package io.github.heonny.clickhousedsl.executor.jdbc;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.heonny.clickhousedsl.api.ClickHouseDsl;
import io.github.heonny.clickhousedsl.model.Query;
import io.github.heonny.clickhousedsl.model.Table;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;

class JdbcQueryFetcherConcurrencyTest {

    @Test
    void sharedFetcherUsesIndependentJdbcResourcesPerThread() throws Exception {
        AtomicInteger connectionRequests = new AtomicInteger();
        AtomicInteger preparedStatements = new AtomicInteger();
        AtomicInteger executeQueries = new AtomicInteger();
        AtomicInteger parameterBindings = new AtomicInteger();
        AtomicInteger timeoutApplications = new AtomicInteger();
        AtomicInteger closedConnections = new AtomicInteger();
        AtomicInteger closedStatements = new AtomicInteger();
        AtomicInteger closedResultSets = new AtomicInteger();
        ConcurrentLinkedQueue<String> observedCalls = new ConcurrentLinkedQueue<>();

        DataSource dataSource = concurrentDataSource(
            connectionRequests,
            preparedStatements,
            executeQueries,
            parameterBindings,
            timeoutApplications,
            closedConnections,
            closedStatements,
            closedResultSets,
            observedCalls
        );
        JdbcQueryFetcher fetcher = new JdbcQueryFetcher(dataSource, 3);

        Table users = Table.of("users");
        var country = users.column("country", String.class);
        Query query = ClickHouseDsl.select(country)
            .from(users)
            .where(country.eq("KR"))
            .build();

        int taskCount = 24;
        ExecutorService executor = Executors.newFixedThreadPool(8);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<List<String>>> futures = new ArrayList<>();

        try {
            for (int index = 0; index < taskCount; index++) {
                futures.add(executor.submit(() -> {
                    start.await(5, TimeUnit.SECONDS);
                    return fetcher.fetch(query, (resultSet, rowNum) -> rowNum + ":" + resultSet.getString(1));
                }));
            }

            start.countDown();

            for (Future<List<String>> future : futures) {
                assertThat(future.get(5, TimeUnit.SECONDS)).containsExactly("0:alice", "1:bob");
            }
        } finally {
            executor.shutdownNow();
        }

        assertThat(connectionRequests).hasValue(taskCount);
        assertThat(preparedStatements).hasValue(taskCount);
        assertThat(executeQueries).hasValue(taskCount);
        assertThat(parameterBindings).hasValue(taskCount);
        assertThat(timeoutApplications).hasValue(taskCount);
        assertThat(closedConnections).hasValue(taskCount);
        assertThat(closedStatements).hasValue(taskCount);
        assertThat(closedResultSets).hasValue(taskCount);
        assertThat(observedCalls)
            .allSatisfy(call -> assertThat(call).containsAnyOf("prepareStatement:", "setQueryTimeout:3", "setObject:1=KR", "executeQuery"));
    }

    private static DataSource concurrentDataSource(
        AtomicInteger connectionRequests,
        AtomicInteger preparedStatements,
        AtomicInteger executeQueries,
        AtomicInteger parameterBindings,
        AtomicInteger timeoutApplications,
        AtomicInteger closedConnections,
        AtomicInteger closedStatements,
        AtomicInteger closedResultSets,
        ConcurrentLinkedQueue<String> observedCalls
    ) {
        return (DataSource) Proxy.newProxyInstance(
            JdbcQueryFetcherConcurrencyTest.class.getClassLoader(),
            new Class<?>[]{DataSource.class},
            (proxy, method, args) -> {
                if ("getConnection".equals(method.getName())) {
                    int connectionId = connectionRequests.incrementAndGet();
                    return connectionProxy(
                        connectionId,
                        preparedStatements,
                        executeQueries,
                        parameterBindings,
                        timeoutApplications,
                        closedConnections,
                        closedStatements,
                        closedResultSets,
                        observedCalls
                    );
                }
                return defaultValue(method.getReturnType());
            }
        );
    }

    private static Connection connectionProxy(
        int connectionId,
        AtomicInteger preparedStatements,
        AtomicInteger executeQueries,
        AtomicInteger parameterBindings,
        AtomicInteger timeoutApplications,
        AtomicInteger closedConnections,
        AtomicInteger closedStatements,
        AtomicInteger closedResultSets,
        ConcurrentLinkedQueue<String> observedCalls
    ) {
        return (Connection) Proxy.newProxyInstance(
            JdbcQueryFetcherConcurrencyTest.class.getClassLoader(),
            new Class<?>[]{Connection.class},
            (proxy, method, args) -> {
                if ("prepareStatement".equals(method.getName())) {
                    preparedStatements.incrementAndGet();
                    observedCalls.add("prepareStatement:" + connectionId + ":" + args[0]);
                    return preparedStatementProxy(
                        connectionId,
                        executeQueries,
                        parameterBindings,
                        timeoutApplications,
                        closedStatements,
                        closedResultSets,
                        observedCalls
                    );
                }
                if ("close".equals(method.getName())) {
                    closedConnections.incrementAndGet();
                    return null;
                }
                return defaultValue(method.getReturnType());
            }
        );
    }

    private static PreparedStatement preparedStatementProxy(
        int connectionId,
        AtomicInteger executeQueries,
        AtomicInteger parameterBindings,
        AtomicInteger timeoutApplications,
        AtomicInteger closedStatements,
        AtomicInteger closedResultSets,
        ConcurrentLinkedQueue<String> observedCalls
    ) {
        return (PreparedStatement) Proxy.newProxyInstance(
            JdbcQueryFetcherConcurrencyTest.class.getClassLoader(),
            new Class<?>[]{PreparedStatement.class},
            (proxy, method, args) -> {
                if ("setQueryTimeout".equals(method.getName())) {
                    timeoutApplications.incrementAndGet();
                    observedCalls.add("setQueryTimeout:3");
                    return null;
                }
                if ("setObject".equals(method.getName())) {
                    parameterBindings.incrementAndGet();
                    observedCalls.add("setObject:" + args[0] + "=" + args[1]);
                    return null;
                }
                if ("executeQuery".equals(method.getName())) {
                    executeQueries.incrementAndGet();
                    observedCalls.add("executeQuery:" + connectionId);
                    return resultSetProxy(closedResultSets);
                }
                if ("close".equals(method.getName())) {
                    closedStatements.incrementAndGet();
                    return null;
                }
                return defaultValue(method.getReturnType());
            }
        );
    }

    private static ResultSet resultSetProxy(AtomicInteger closedResultSets) {
        return (ResultSet) Proxy.newProxyInstance(
            JdbcQueryFetcherConcurrencyTest.class.getClassLoader(),
            new Class<?>[]{ResultSet.class},
            new java.lang.reflect.InvocationHandler() {
                private final List<String> values = List.of("alice", "bob");
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
                            closedResultSets.incrementAndGet();
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
