package io.github.heonny.clickhousedsl.api;

import static io.github.heonny.clickhousedsl.api.ClickHouseDsl.allOf;
import static io.github.heonny.clickhousedsl.api.ClickHouseDsl.count;
import static io.github.heonny.clickhousedsl.api.ClickHouseDsl.maxMemoryUsage;
import static io.github.heonny.clickhousedsl.api.ClickHouseDsl.maxThreads;
import static io.github.heonny.clickhousedsl.api.ClickHouseDsl.param;
import static io.github.heonny.clickhousedsl.api.ClickHouseDsl.render;
import static io.github.heonny.clickhousedsl.api.ClickHouseDsl.rowNumber;
import static io.github.heonny.clickhousedsl.api.ClickHouseDsl.select;
import static io.github.heonny.clickhousedsl.api.ClickHouseDsl.sum;
import static io.github.heonny.clickhousedsl.api.ClickHouseDsl.useUncompressedCache;
import static io.github.heonny.clickhousedsl.api.ClickHouseDsl.window;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.heonny.clickhousedsl.model.Query;
import io.github.heonny.clickhousedsl.model.RenderedQuery;
import io.github.heonny.clickhousedsl.model.Table;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.junit.jupiter.api.Test;

class ReadmeExampleTest {

    @Test
    void gettingStartedExampleRendersForExistingExecutionLayers() {
        Table users = Table.of("analytics.users").as("u");
        var userName = users.column("name", String.class);
        var appId = users.column("app_id", Long.class);

        Query query = select(userName, count())
            .from(users)
            .where(appId.eq(param(7L, Long.class)))
            .groupBy(userName)
            .build();

        RenderedQuery rendered = ClickHouseDsl.renderValidatedQuery(query);

        assertThat(rendered.sql())
            .isEqualTo("SELECT `u`.`name`, count() FROM `analytics`.`users` AS `u` WHERE `u`.`app_id` = ? GROUP BY `u`.`name`");
        assertThat(rendered.parameters()).containsExactly(7L);
    }

    @Test
    void safeUsageExampleKeepsExecutionOutsideTheDsl() throws SQLException {
        Table users = Table.of("analytics.users").as("u");
        var userName = users.column("name", String.class);

        Query query = select(userName)
            .from(users)
            .where(userName.eq("alice"))
            .build();

        RenderedQuery rendered = ClickHouseDsl.renderValidatedQuery(query);

        PreparedStatement statement = preparedStatementRecorder();
        bind(statement, rendered);

        assertThat(rendered.debugSql())
            .isEqualTo("SELECT `u`.`name` FROM `analytics`.`users` AS `u` WHERE `u`.`name` = 'alice'");
    }

    @Test
    void quickExampleStaysStable() {
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
                sum(score).over(window().partitionBy(userName).orderBy(age.asc()))
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

    @Test
    void dynamicConditionExampleUsesNullSafeHelper() {
        Table sessions = Table.of("user_sessions").as("s");
        var sessionId = sessions.column("session_id", String.class);
        var appId = sessions.column("app_id", Long.class);
        var country = sessions.column("country", String.class);
        var duration = sessions.column("duration_ms", Long.class);

        Query query = select(sessionId, country, duration)
            .from(sessions)
            .whereIfPresent(allOf(
                appId.eq(param(7L, Long.class)),
                duration.gt(param(1000L, Long.class)),
                null,
                country.eq(param("KR", String.class))
            ))
            .orderBy(duration.desc(), sessionId.asc())
            .limit(20)
            .build();

        RenderedQuery rendered = ClickHouseDsl.renderValidatedQuery(query);

        assertThat(rendered.sql()).isEqualTo(
            "SELECT `s`.`session_id`, `s`.`country`, `s`.`duration_ms` FROM `user_sessions` AS `s` " +
                "WHERE ((`s`.`app_id` = ? AND `s`.`duration_ms` > ?) AND `s`.`country` = ?) " +
                "ORDER BY `s`.`duration_ms` DESC, `s`.`session_id` ASC LIMIT ?"
        );
        assertThat(rendered.parameters()).containsExactly(7L, 1000L, "KR", 20);
    }

    private static void bind(PreparedStatement statement, RenderedQuery rendered) throws SQLException {
        for (int index = 0; index < rendered.parameters().size(); index++) {
            statement.setObject(index + 1, rendered.parameters().get(index));
        }
    }

    private static PreparedStatement preparedStatementRecorder() {
        return (PreparedStatement) java.lang.reflect.Proxy.newProxyInstance(
            ReadmeExampleTest.class.getClassLoader(),
            new Class<?>[]{PreparedStatement.class},
            (proxy, method, args) -> defaultValue(method.getReturnType())
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
