package io.github.chang.clickhousedsl.api;

import static io.github.chang.clickhousedsl.api.ClickHouseDsl.count;
import static io.github.chang.clickhousedsl.api.ClickHouseDsl.maxThreads;
import static io.github.chang.clickhousedsl.api.ClickHouseDsl.useUncompressedCache;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.chang.clickhousedsl.model.Join;
import io.github.chang.clickhousedsl.model.JoinType;
import io.github.chang.clickhousedsl.model.Query;
import io.github.chang.clickhousedsl.model.RenderedQuery;
import io.github.chang.clickhousedsl.model.Table;
import io.github.chang.clickhousedsl.render.ClickHouseRenderer;
import io.github.chang.clickhousedsl.validate.SemanticAnalyzer;
import org.junit.jupiter.api.Test;

class ClickHouseDslTest {

    private final ClickHouseRenderer renderer = new ClickHouseRenderer();
    private final SemanticAnalyzer analyzer = new SemanticAnalyzer();

    @Test
    void rendersQueryWithClickHouseSpecificClausesAndParameters() {
        Table users = Table.of("analytics.users").as("u").finalTable();
        Table events = Table.of("analytics.events").as("e");

        var userId = users.column("id", Long.class);
        var userName = users.column("name", String.class);
        var age = users.column("age", Integer.class);
        var tags = users.column("tags", String.class);
        var eventUserId = events.column("user_id", Long.class);

        Query query = ClickHouseDsl.select(userName, count())
            .from(users)
            .innerJoin(events).on(userId, eventUserId)
            .arrayJoin(tags)
            .sample(0.5d)
            .prewhere(age.gt(18))
            .where(userName.eq("alice"))
            .groupBy(userName)
            .having(count().gt(ClickHouseDsl.param(1L, Long.class)))
            .orderBy(userName.asc())
            .limit(10)
            .settings(maxThreads(4), useUncompressedCache(true))
            .build();

        RenderedQuery rendered = renderer.render(query);

        assertThat(rendered.sql()).isEqualTo(
            "SELECT `u`.`name`, count() FROM `analytics`.`users` FINAL AS `u` " +
                "INNER JOIN `analytics`.`events` AS `e` ON `u`.`id` = `e`.`user_id` " +
                "ARRAY JOIN `u`.`tags` SAMPLE ? PREWHERE `u`.`age` > ? WHERE `u`.`name` = ? " +
                "GROUP BY `u`.`name` HAVING count() > ? ORDER BY `u`.`name` ASC LIMIT ? " +
                "SETTINGS `max_threads` = ?, `use_uncompressed_cache` = ?"
        );
        assertThat(rendered.parameters()).containsExactly(0.5d, 18, "alice", 1L, 10, 4, 1);
    }

    @Test
    void rejectsUnsafeIdentifiers() {
        assertThatThrownBy(() -> Table.of("users;drop"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unsafe identifier");
    }

    @Test
    void requiresFromBeforeBuild() {
        Table users = Table.of("users");
        var userName = users.column("name", String.class);
        QueryBuilder builder = new QueryBuilder(userName);

        assertThatThrownBy(builder::build)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("FROM is required");
    }

    @Test
    void requiresJoinToBeCompleted() {
        Table users = Table.of("users");
        Table events = Table.of("events");
        var userName = users.column("name", String.class);
        QueryBuilder builder = new QueryBuilder(userName);
        builder.from(users);
        builder.innerJoin(events);

        assertThatThrownBy(builder::build)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Join must be completed");
    }

    @Test
    void validatesAggregateUsageWithoutGroupBy() {
        Table users = Table.of("users");
        var userName = users.column("name", String.class);

        Query query = ClickHouseDsl.select(userName, count())
            .from(users)
            .build();

        assertThat(analyzer.validate(query).errors())
            .extracting(error -> error.code())
            .containsExactly("GROUP_BY_REQUIRED");
    }

    @Test
    void validatesHavingRequiresGroupBy() {
        Table users = Table.of("users");
        var age = users.column("age", Integer.class);

        Query query = new Query(
            java.util.List.of(count()),
            users,
            java.util.List.of(),
            java.util.List.of(),
            null,
            null,
            null,
            java.util.List.of(),
            age.gt(10),
            java.util.List.of(),
            null,
            java.util.List.of()
        );

        assertThat(analyzer.validate(query).errors())
            .extracting(error -> error.code())
            .containsExactly("HAVING_REQUIRES_GROUP_BY");
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test
    void validatesJoinKeyTypeMismatchWhenBypassingGenericSafety() {
        Table users = Table.of("users");
        Table events = Table.of("events");
        var userId = users.column("id", Long.class);
        var eventCode = events.column("event_code", String.class);

        Query query = new Query(
            java.util.List.of(userId),
            users,
            java.util.List.of(new Join(JoinType.INNER, events, (io.github.chang.clickhousedsl.model.Expression) userId, (io.github.chang.clickhousedsl.model.Expression) eventCode)),
            java.util.List.of(),
            null,
            null,
            null,
            java.util.List.of(),
            null,
            java.util.List.of(),
            null,
            java.util.List.of()
        );

        assertThat(analyzer.validate(query).errors())
            .extracting(error -> error.code())
            .containsExactly("JOIN_KEY_TYPE_MISMATCH");
    }

    @Test
    void enforcesSampleAndLimitBounds() {
        Table users = Table.of("users");
        var userName = users.column("name", String.class);

        assertThatThrownBy(() -> ClickHouseDsl.select(userName).from(users).sample(0.0d))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Sample ratio");

        assertThatThrownBy(() -> ClickHouseDsl.select(userName).from(users).limit(0))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Limit");
    }

    @Test
    void validatesGroupByMismatch() {
        Table users = Table.of("users");
        var userName = users.column("name", String.class);
        var country = users.column("country", String.class);

        Query query = ClickHouseDsl.select(userName, count())
            .from(users)
            .groupBy(country)
            .build();

        assertThat(analyzer.validate(query).errors())
            .extracting(error -> error.code())
            .containsExactly("GROUP_BY_MISMATCH");
    }

    @Test
    void rejectsEmptySelectionList() {
        assertThatThrownBy(() -> new QueryBuilder(new io.github.chang.clickhousedsl.model.Expression<?>[0]))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("At least one selection");
    }
}
