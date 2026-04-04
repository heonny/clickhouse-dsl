package io.github.heonny.clickhousedsl.executor.jdbc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.heonny.clickhousedsl.executor.QueryExecutionException;
import io.github.heonny.clickhousedsl.model.RenderedQuery;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;

class JdbcRenderedQueryExecutorTest {

    @Test
    void rejectsNullDataSource() {
        assertThatThrownBy(() -> new JdbcRenderedQueryExecutor(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("dataSource");
    }

    @Test
    void rejectsNonPositiveQueryTimeout() {
        DataSource dataSource = dataSourceStub(null, null);

        assertThatThrownBy(() -> new JdbcRenderedQueryExecutor(dataSource, 0))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("queryTimeoutSeconds");
    }

    @Test
    void bindsParametersInOrderAndExecutesStatement() {
        List<String> calls = new ArrayList<>();
        AtomicBoolean connectionClosed = new AtomicBoolean(false);
        AtomicBoolean statementClosed = new AtomicBoolean(false);

        DataSource dataSource = dataSourceStub(
            connectionProxy(preparedStatementProxy(calls, statementClosed), calls, connectionClosed),
            null
        );
        JdbcRenderedQueryExecutor executor = new JdbcRenderedQueryExecutor(dataSource, 15);
        RenderedQuery renderedQuery = new RenderedQuery(
            "SELECT * FROM users WHERE id = ? AND country = ?",
            List.of(7L, "KR")
        );

        executor.execute(renderedQuery);

        assertThat(calls).containsExactly(
            "prepareStatement:SELECT * FROM users WHERE id = ? AND country = ?",
            "setQueryTimeout:15",
            "setObject:1=7",
            "setObject:2=KR",
            "execute"
        );
        assertThat(connectionClosed).isTrue();
        assertThat(statementClosed).isTrue();
        assertThat(executor.queryTimeoutSeconds()).isEqualTo(15);
        assertThat(executor.dataSource()).isSameAs(dataSource);
    }

    @Test
    void wrapsSqlExceptionWithRenderedQueryContext() {
        SQLException failure = new SQLException("boom");
        DataSource dataSource = dataSourceStub(null, failure);
        JdbcRenderedQueryExecutor executor = new JdbcRenderedQueryExecutor(dataSource);
        RenderedQuery renderedQuery = new RenderedQuery("SELECT 1", List.of());

        assertThatThrownBy(() -> executor.execute(renderedQuery))
            .isInstanceOf(QueryExecutionException.class)
            .hasMessageContaining("JDBC query execution failed.")
            .satisfies(throwable -> {
                QueryExecutionException exception = (QueryExecutionException) throwable;
                assertThat(exception.getCause()).isSameAs(failure);
                assertThat(exception.renderedQuery()).isSameAs(renderedQuery);
            });
    }

    @Test
    void rejectsNullRenderedQuery() {
        JdbcRenderedQueryExecutor executor = new JdbcRenderedQueryExecutor(dataSourceStub(null, new SQLException("unused")));

        assertThatThrownBy(() -> executor.execute(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("renderedQuery");
    }

    private static DataSource dataSourceStub(Connection connection, SQLException failure) {
        return (DataSource) Proxy.newProxyInstance(
            JdbcRenderedQueryExecutorTest.class.getClassLoader(),
            new Class<?>[]{DataSource.class},
            (proxy, method, args) -> {
                if ("getConnection".equals(method.getName())) {
                    if (failure != null) {
                        throw failure;
                    }
                    return connection;
                }
                if ("toString".equals(method.getName())) {
                    return "DataSourceStub";
                }
                return defaultValue(method.getReturnType());
            }
        );
    }

    private static Connection connectionProxy(PreparedStatement preparedStatement, List<String> calls, AtomicBoolean closed) {
        return (Connection) Proxy.newProxyInstance(
            JdbcRenderedQueryExecutorTest.class.getClassLoader(),
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
                if ("toString".equals(method.getName())) {
                    return "ConnectionStub";
                }
                return defaultValue(method.getReturnType());
            }
        );
    }

    private static PreparedStatement preparedStatementProxy(List<String> calls, AtomicBoolean closed) {
        return (PreparedStatement) Proxy.newProxyInstance(
            JdbcRenderedQueryExecutorTest.class.getClassLoader(),
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
                if ("execute".equals(method.getName())) {
                    calls.add("execute");
                    return true;
                }
                if ("close".equals(method.getName())) {
                    closed.set(true);
                    return null;
                }
                if ("toString".equals(method.getName())) {
                    return "PreparedStatementStub";
                }
                return defaultValue(method.getReturnType());
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
