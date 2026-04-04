package io.github.heonny.clickhousedsl.executor.jdbc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.heonny.clickhousedsl.api.ClickHouseDsl;
import io.github.heonny.clickhousedsl.executor.QueryExecutionException;
import io.github.heonny.clickhousedsl.model.Query;
import io.github.heonny.clickhousedsl.model.Table;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;

class JdbcQueryFetcherTest {

    @Test
    void rejectsNullConstructorArguments() {
        assertThatThrownBy(() -> new JdbcQueryFetcher(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("dataSource");
    }

    @Test
    void rejectsInvalidTimeout() {
        DataSource dataSource = dataSourceStub(null, null);

        assertThatThrownBy(() -> new JdbcQueryFetcher(dataSource, 0))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("queryTimeoutSeconds");
    }

    @Test
    void validatesNullInputs() {
        JdbcQueryFetcher fetcher = new JdbcQueryFetcher(dataSourceStub(null, null));

        assertThatThrownBy(() -> fetcher.fetch(null, (resultSet, rowNum) -> "x"))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("query");
        Table users = Table.of("users");
        var name = users.column("name", String.class);
        Query query = ClickHouseDsl.select(name).from(users).build();
        assertThatThrownBy(() -> fetcher.fetch(query, null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("rowMapper");
    }

    @Test
    void fetchesRowsAndMapsInOrder() {
        List<String> calls = new ArrayList<>();
        AtomicBoolean connectionClosed = new AtomicBoolean(false);
        AtomicBoolean statementClosed = new AtomicBoolean(false);
        AtomicBoolean resultSetClosed = new AtomicBoolean(false);

        ResultSet resultSet = resultSetProxy(List.of("alice", "bob"), resultSetClosed);
        PreparedStatement statement = preparedStatementProxyForQuery(calls, statementClosed, resultSet);
        DataSource dataSource = dataSourceStub(connectionProxy(statement, calls, connectionClosed), null);
        JdbcQueryFetcher fetcher = new JdbcQueryFetcher(dataSource, 5);

        Table users = Table.of("users");
        var name = users.column("name", String.class);
        Query query = ClickHouseDsl.select(name)
            .from(users)
            .where(name.eq("KR"))
            .build();

        List<String> rows = fetcher.fetch(query, (rs, rowNum) -> rowNum + ":" + rs.getString(1));

        assertThat(rows).containsExactly("0:alice", "1:bob");
        assertThat(calls).containsExactly(
            "prepareStatement:SELECT `users`.`name` FROM `users` WHERE `users`.`name` = ?",
            "setQueryTimeout:5",
            "setObject:1=KR",
            "executeQuery"
        );
        assertThat(connectionClosed).isTrue();
        assertThat(statementClosed).isTrue();
        assertThat(resultSetClosed).isTrue();
        assertThat(fetcher.queryTimeoutSeconds()).isEqualTo(5);
        assertThat(fetcher.dataSource()).isSameAs(dataSource);
    }

    @Test
    void wrapsSqlFailure() {
        SQLException failure = new SQLException("fetch boom");
        DataSource dataSource = dataSourceStub(null, failure);
        JdbcQueryFetcher fetcher = new JdbcQueryFetcher(dataSource);

        Table users = Table.of("users");
        var name = users.column("name", String.class);
        Query query = ClickHouseDsl.select(name).from(users).build();

        assertThatThrownBy(() -> fetcher.fetch(query, (rs, rowNum) -> rs.getString(1)))
            .isInstanceOf(QueryExecutionException.class)
            .hasMessageContaining("JDBC query fetch failed.");
    }

    private static DataSource dataSourceStub(Connection connection, SQLException failure) {
        return (DataSource) Proxy.newProxyInstance(
            JdbcQueryFetcherTest.class.getClassLoader(),
            new Class<?>[]{DataSource.class},
            (proxy, method, args) -> {
                if ("getConnection".equals(method.getName())) {
                    if (failure != null) {
                        throw failure;
                    }
                    return connection;
                }
                return defaultValue(method.getReturnType());
            }
        );
    }

    private static Connection connectionProxy(PreparedStatement preparedStatement, List<String> calls, AtomicBoolean closed) {
        return (Connection) Proxy.newProxyInstance(
            JdbcQueryFetcherTest.class.getClassLoader(),
            new Class<?>[]{Connection.class},
            (proxy, method, args) -> {
                if ("prepareStatement".equals(method.getName())) {
                    calls.add("prepareStatement:" + args[0]);
                    return preparedStatement;
                }
                if ("close".equals(method.getName())) {
                    closed.set(true);
                    return null;
                }
                return defaultValue(method.getReturnType());
            }
        );
    }

    private static PreparedStatement preparedStatementProxyForQuery(List<String> calls, AtomicBoolean closed, ResultSet resultSet) {
        return (PreparedStatement) Proxy.newProxyInstance(
            JdbcQueryFetcherTest.class.getClassLoader(),
            new Class<?>[]{PreparedStatement.class},
            (proxy, method, args) -> {
                if ("setQueryTimeout".equals(method.getName())) {
                    calls.add("setQueryTimeout:" + args[0]);
                    return null;
                }
                if ("setObject".equals(method.getName())) {
                    calls.add("setObject:" + args[0] + "=" + args[1]);
                    return null;
                }
                if ("executeQuery".equals(method.getName())) {
                    calls.add("executeQuery");
                    return resultSet;
                }
                if ("close".equals(method.getName())) {
                    closed.set(true);
                    return null;
                }
                return defaultValue(method.getReturnType());
            }
        );
    }

    private static ResultSet resultSetProxy(List<String> values, AtomicBoolean closed) {
        return (ResultSet) Proxy.newProxyInstance(
            JdbcQueryFetcherTest.class.getClassLoader(),
            new Class<?>[]{ResultSet.class},
            new java.lang.reflect.InvocationHandler() {
                private int index = -1;

                @Override
                public Object invoke(Object proxy, java.lang.reflect.Method method, Object[] args) {
                    switch (method.getName()) {
                        case "next":
                            index++;
                            return index < values.size();
                        case "getString":
                            return values.get(index);
                        case "close":
                            closed.set(true);
                            return null;
                        default:
                            return defaultValue(method.getReturnType());
                    }
                }
            }
        );
    }

    private static Object defaultValue(Class<?> returnType) {
        if (returnType == Void.TYPE) {
            return null;
        }
        if (returnType == Boolean.TYPE) {
            return false;
        }
        if (returnType == Integer.TYPE) {
            return 0;
        }
        if (returnType == Long.TYPE) {
            return 0L;
        }
        if (returnType == Double.TYPE) {
            return 0d;
        }
        if (returnType == Float.TYPE) {
            return 0f;
        }
        if (returnType == Short.TYPE) {
            return (short) 0;
        }
        if (returnType == Byte.TYPE) {
            return (byte) 0;
        }
        if (returnType == Character.TYPE) {
            return '\0';
        }
        return null;
    }
}
