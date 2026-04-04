package io.github.chang.clickhousedsl.api;

import static io.github.chang.clickhousedsl.api.ClickHouseDsl.count;
import static io.github.chang.clickhousedsl.api.ClickHouseDsl.maxMemoryUsage;
import static io.github.chang.clickhousedsl.api.ClickHouseDsl.maxThreads;
import static io.github.chang.clickhousedsl.api.ClickHouseDsl.param;
import static io.github.chang.clickhousedsl.api.ClickHouseDsl.render;
import static io.github.chang.clickhousedsl.api.ClickHouseDsl.rowNumber;
import static io.github.chang.clickhousedsl.api.ClickHouseDsl.select;
import static io.github.chang.clickhousedsl.api.ClickHouseDsl.useUncompressedCache;
import static io.github.chang.clickhousedsl.api.ClickHouseDsl.window;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.chang.clickhousedsl.model.Query;
import io.github.chang.clickhousedsl.model.Table;
import org.junit.jupiter.api.Test;

class ReadmeExampleTest {

    @Test
    void readmeQueryExampleStaysStable() {
        Table users = Table.of("analytics.users").as("u").finalTable();
        Table events = Table.of("analytics.events").as("e");

        var userId = users.column("id", Long.class);
        var userName = users.column("name", String.class);
        var age = users.column("age", Integer.class);
        var score = users.column("score", Integer.class);
        var tags = users.arrayColumn("tags", String.class);
        var eventUserId = events.column("user_id", Long.class);

        Query query = select(
                userName,
                count(),
                rowNumber(window().partitionBy(userName).orderBy(age.desc())),
                io.github.chang.clickhousedsl.model.Expressions.sum(score)
                    .over(window().partitionBy(userName).orderBy(age.asc()))
            )
            .from(users)
            .innerJoin(events).on(userId, eventUserId)
            .arrayJoin(tags)
            .prewhere(age.gt(18))
            .where(userName.eq("alice"))
            .groupBy(userName)
            .having(count().gt(param(1L, Long.class)))
            .limit(100)
            .settings(maxThreads(4), maxMemoryUsage(268_435_456L), useUncompressedCache(true))
            .build();

        assertThat(render(query)).isEqualTo(
            "SELECT `u`.`name`, count(), rowNumber() OVER (PARTITION BY `u`.`name` ORDER BY `u`.`age` DESC), " +
                "sum(`u`.`score`) OVER (PARTITION BY `u`.`name` ORDER BY `u`.`age` ASC) " +
                "FROM `analytics`.`users` FINAL AS `u` " +
                "INNER JOIN `analytics`.`events` AS `e` ON `u`.`id` = `e`.`user_id` " +
                "ARRAY JOIN `u`.`tags` PREWHERE `u`.`age` > ? WHERE `u`.`name` = ? " +
                "GROUP BY `u`.`name` HAVING count() > ? LIMIT ? " +
                "SETTINGS `max_threads` = ?, `max_memory_usage` = ?, `use_uncompressed_cache` = ?"
        );
    }
}
