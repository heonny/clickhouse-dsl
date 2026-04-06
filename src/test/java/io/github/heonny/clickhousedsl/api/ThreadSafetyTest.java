package io.github.heonny.clickhousedsl.api;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.heonny.clickhousedsl.model.Join;
import io.github.heonny.clickhousedsl.model.JoinType;
import io.github.heonny.clickhousedsl.model.Query;
import io.github.heonny.clickhousedsl.model.RenderedQuery;
import io.github.heonny.clickhousedsl.model.Table;
import io.github.heonny.clickhousedsl.render.ClickHouseRenderer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class ThreadSafetyTest {

    @Test
    void sharedQueryBuilderKeepsJoinPairsConsistentAcrossThreads() throws Exception {
        Table users = Table.of("users").as("u");
        Table orders = Table.of("orders").as("o");
        Table events = Table.of("events").as("e");
        var userId = users.column("id", Long.class);
        var orderUserId = orders.column("user_id", Long.class);
        var eventUserId = events.column("user_id", Long.class);

        for (int attempt = 0; attempt < 100; attempt++) {
            QueryBuilder builder = new QueryBuilder(userId);
            builder.from(users);
            CyclicBarrier barrier = new CyclicBarrier(2);
            ExecutorService executor = Executors.newFixedThreadPool(2);
            try {
                Future<?> leftFuture = executor.submit(() -> {
                    ClickHouseDsl.JoinOnStep pendingJoin = builder.leftJoin(orders);
                    barrier.await();
                    pendingJoin.on(userId, orderUserId);
                    return null;
                });
                Future<?> innerFuture = executor.submit(() -> {
                    ClickHouseDsl.JoinOnStep pendingJoin = builder.innerJoin(events);
                    barrier.await();
                    pendingJoin.on(userId, eventUserId);
                    return null;
                });
                leftFuture.get(5, TimeUnit.SECONDS);
                innerFuture.get(5, TimeUnit.SECONDS);
            } finally {
                executor.shutdownNow();
            }

            Query built = builder.build();
            assertThat(built.joins()).hasSize(2);
            assertThat(hasJoin(built.joins(), JoinType.LEFT, orders, orderUserId)).isTrue();
            assertThat(hasJoin(built.joins(), JoinType.INNER, events, eventUserId)).isTrue();
        }
    }

    @Test
    void immutableQueryRendersConsistentlyAcrossThreads() throws Exception {
        ClickHouseRenderer renderer = new ClickHouseRenderer();
        Table users = Table.of("users").as("u");
        var id = users.column("id", Long.class);
        var country = users.column("country", String.class);
        Query query = ClickHouseDsl.select(id)
            .from(users)
            .where(country.eq("KR"))
            .orderBy(id.desc())
            .limit(5)
            .build();

        int workerCount = 8;
        int taskCount = 48;
        ExecutorService executor = Executors.newFixedThreadPool(workerCount);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<RenderedQuery>> futures = new ArrayList<>();

        try {
            for (int index = 0; index < taskCount; index++) {
                futures.add(executor.submit(() -> {
                    start.await(5, TimeUnit.SECONDS);
                    return renderer.render(query);
                }));
            }

            start.countDown();

            List<RenderedQuery> renderedQueries = new ArrayList<>();
            for (Future<RenderedQuery> future : futures) {
                renderedQueries.add(future.get(5, TimeUnit.SECONDS));
            }

            assertThat(renderedQueries)
                .extracting(RenderedQuery::sql)
                .containsOnly("SELECT `u`.`id` FROM `users` AS `u` WHERE `u`.`country` = ? ORDER BY `u`.`id` DESC LIMIT ?");
            assertThat(renderedQueries)
                .extracting(RenderedQuery::parameters)
                .containsOnly(List.of("KR", 5));
        } finally {
            executor.shutdownNow();
        }
    }

    private static boolean hasJoin(List<Join> joins, JoinType type, Table table, Object rightKey) {
        return joins.stream().anyMatch(join ->
            join.type() == type
                && join.table().equals(table)
                && join.rightKey().equals(rightKey)
        );
    }
}
