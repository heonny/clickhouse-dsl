package io.github.heonny.clickhousedsl.api;

import static io.github.heonny.clickhousedsl.api.ClickHouseDsl.count;
import static io.github.heonny.clickhousedsl.api.ClickHouseDsl.maxMemoryUsage;
import static io.github.heonny.clickhousedsl.api.ClickHouseDsl.maxThreads;
import static io.github.heonny.clickhousedsl.api.ClickHouseDsl.rowNumber;
import static io.github.heonny.clickhousedsl.api.ClickHouseDsl.useUncompressedCache;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.heonny.clickhousedsl.explain.ExplainType;
import io.github.heonny.clickhousedsl.model.Expression;
import io.github.heonny.clickhousedsl.model.Join;
import io.github.heonny.clickhousedsl.model.JoinType;
import io.github.heonny.clickhousedsl.model.Query;
import io.github.heonny.clickhousedsl.model.RenderedQuery;
import io.github.heonny.clickhousedsl.model.Table;
import io.github.heonny.clickhousedsl.render.ClickHouseRenderer;
import io.github.heonny.clickhousedsl.render.RenderOptions;
import io.github.heonny.clickhousedsl.validate.SemanticAnalyzer;
import io.github.heonny.clickhousedsl.validate.ValidationClause;
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
            .settings(maxThreads(4), maxMemoryUsage(268_435_456L), useUncompressedCache(true))
            .build();

        RenderedQuery rendered = renderer.render(query);

        assertThat(rendered.sql()).isEqualTo(
            "SELECT `u`.`name`, count() FROM `analytics`.`users` FINAL AS `u` " +
                "INNER JOIN `analytics`.`events` AS `e` ON `u`.`id` = `e`.`user_id` " +
                "ARRAY JOIN `u`.`tags` SAMPLE ? PREWHERE `u`.`age` > ? WHERE `u`.`name` = ? " +
                "GROUP BY `u`.`name` HAVING count() > ? ORDER BY `u`.`name` ASC LIMIT ? " +
                "SETTINGS `max_threads` = ?, `max_memory_usage` = ?, `use_uncompressed_cache` = ?"
        );
        assertThat(rendered.parameters()).containsExactly(0.5d, 18, "alice", 1L, 10, 4, 268_435_456L, 1);
    }

    @Test
    void rejectsUnsafeIdentifiers() {
        assertThatThrownBy(() -> Table.of("users;drop"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unsafe identifier");
    }

    @Test
    void validatesPositiveSettingBounds() {
        assertThatThrownBy(() -> maxThreads(0))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("max_threads");

        assertThatThrownBy(() -> maxMemoryUsage(0L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("max_memory_usage");
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
        assertThat(analyzer.validate(query).errors())
            .extracting(error -> error.clause())
            .containsExactly(ValidationClause.SELECT);
    }

    @Test
    void validatesHavingRequiresGroupBy() {
        Table users = Table.of("users");
        var age = users.column("age", Integer.class);

        Query query = new Query(
            java.util.List.of(count()),
            java.util.List.of(),
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
            java.util.List.of(),
            java.util.List.of()
        );

        assertThat(analyzer.validate(query).errors())
            .extracting(error -> error.code())
            .containsExactly("HAVING_REQUIRES_GROUP_BY");
        assertThat(analyzer.validate(query).errors().get(0).detail())
            .contains("HAVING");
    }

    @Test
    void validatesAggregateNotAllowedInWhere() {
        Table users = Table.of("users");
        var userName = users.column("name", String.class);

        Query query = ClickHouseDsl.select(userName)
            .from(users)
            .where(count().gt(ClickHouseDsl.param(1L, Long.class)))
            .build();

        assertThat(analyzer.validate(query).errors())
            .extracting(error -> error.code())
            .containsExactly("AGGREGATE_NOT_ALLOWED_IN_WHERE");
        assertThat(analyzer.validate(query).errors().get(0).clause())
            .isEqualTo(ValidationClause.WHERE);
    }

    @Test
    void validatesAggregateNotAllowedInPrewhere() {
        Table users = Table.of("users");
        var userName = users.column("name", String.class);

        Query query = ClickHouseDsl.select(userName)
            .from(users)
            .prewhere(count().gt(ClickHouseDsl.param(1L, Long.class)))
            .build();

        assertThat(analyzer.validate(query).errors())
            .extracting(error -> error.code())
            .containsExactly("AGGREGATE_NOT_ALLOWED_IN_PREWHERE");
        assertThat(analyzer.validate(query).errors().get(0).clause())
            .isEqualTo(ValidationClause.PREWHERE);
    }

    @Test
    void validatesWindowFunctionNotAllowedInWhere() {
        Table users = Table.of("users");
        var userName = users.column("name", String.class);
        var age = users.column("age", Integer.class);

        Query query = ClickHouseDsl.select(userName)
            .from(users)
            .where(rowNumber(ClickHouseDsl.window().partitionBy(userName).orderBy(age.desc())).gt(ClickHouseDsl.param(1L, Long.class)))
            .build();

        assertThat(analyzer.validate(query).errors())
            .extracting(error -> error.code())
            .containsExactly("WINDOW_FUNCTION_NOT_ALLOWED_IN_WHERE");
    }

    @Test
    void validatesWindowFunctionNotAllowedInPrewhere() {
        Table users = Table.of("users");
        var userName = users.column("name", String.class);
        var age = users.column("age", Integer.class);

        Query query = ClickHouseDsl.select(userName)
            .from(users)
            .prewhere(rowNumber(ClickHouseDsl.window().partitionBy(userName).orderBy(age.desc())).gt(ClickHouseDsl.param(1L, Long.class)))
            .build();

        assertThat(analyzer.validate(query).errors())
            .extracting(error -> error.code())
            .containsExactly("WINDOW_FUNCTION_NOT_ALLOWED_IN_PREWHERE");
    }

    @Test
    void combinesOptionalConditionsAndSkipsNullEntries() {
        Table users = Table.of("users");
        var age = users.column("age", Integer.class);
        var country = users.column("country", String.class);

        Expression<Boolean> predicate = ClickHouseDsl.allOf(
            age.gt(18),
            null,
            country.eq("KR")
        );

        Query query = ClickHouseDsl.select(country)
            .from(users)
            .whereIfPresent(predicate)
            .build();

        assertThat(renderer.render(query).sql())
            .isEqualTo("SELECT `users`.`country` FROM `users` WHERE (`users`.`age` > ? AND `users`.`country` = ?)");
        assertThat(renderer.render(query).parameters()).containsExactly(18, "KR");
    }

    @Test
    void anyOfReturnsNullWhenNoConditionIsPresent() {
        Table users = Table.of("users");
        var country = users.column("country", String.class);

        Query query = ClickHouseDsl.select(country)
            .from(users)
            .whereIfPresent(ClickHouseDsl.anyOf(null, null))
            .build();

        assertThat(renderer.render(query).sql()).isEqualTo("SELECT `users`.`country` FROM `users`");
    }

    @Test
    void exposesPrettyRenderOverloadsThroughFacade() {
        Table users = Table.of("users");
        var country = users.column("country", String.class);

        Query query = ClickHouseDsl.select(country)
            .from(users)
            .where(country.eq("KR"))
            .build();

        assertThat(ClickHouseDsl.render(query, RenderOptions.pretty())).isEqualTo(
            "SELECT\n" +
                "  `users`.`country`\n" +
                "FROM `users`\n" +
                "WHERE `users`.`country` = ?"
        );
    }

    @Test
    void facadeCoversAdditionalRenderAndConditionHelperBranches() {
        Table users = Table.of("users");
        var country = users.column("country", String.class);
        var age = users.column("age", Integer.class);

        Query query = ClickHouseDsl.select(country)
            .from(users)
            .prewhereIfPresent(country.eq("KR"))
            .whereIfPresent(ClickHouseDsl.anyOf(age.gt(18), null))
            .build();

        assertThat(ClickHouseDsl.renderValidated(query)).isEqualTo(
            "SELECT `users`.`country` FROM `users` PREWHERE `users`.`country` = ? WHERE `users`.`age` > ?"
        );
        assertThat(ClickHouseDsl.renderValidatedQuery(query, RenderOptions.pretty()).parameters())
            .containsExactly("KR", 18);
        assertThatThrownBy(() -> ClickHouseDsl.allOf((Expression<Boolean>[]) null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("expressions");
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
            java.util.List.of(),
            users,
            java.util.List.of(new Join(JoinType.INNER, events, (io.github.heonny.clickhousedsl.model.Expression) userId, (io.github.heonny.clickhousedsl.model.Expression) eventCode)),
            java.util.List.of(),
            null,
            null,
            null,
            java.util.List.of(),
            null,
            java.util.List.of(),
            null,
            java.util.List.of(),
            java.util.List.of()
        );

        assertThat(analyzer.validate(query).errors())
            .extracting(error -> error.code())
            .containsExactly("JOIN_KEY_TYPE_MISMATCH");
        assertThat(analyzer.validate(query).errors().get(0).detail())
            .contains("Long", "String");
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
        assertThat(analyzer.validate(query).errors().get(0).message())
            .contains("GROUP BY");
    }

    @Test
    void validatesHavingPlainExpressionMustBeGrouped() {
        Table users = Table.of("users");
        var userName = users.column("name", String.class);
        var country = users.column("country", String.class);

        Query query = ClickHouseDsl.select(country, count())
            .from(users)
            .groupBy(country)
            .having(userName.eq("alice"))
            .build();

        assertThat(analyzer.validate(query).errors())
            .extracting(error -> error.code())
            .containsExactly("HAVING_EXPRESSION_NOT_GROUPED");
        assertThat(analyzer.validate(query).errors().get(0).detail())
            .contains("String");
    }

    @Test
    void validatesWindowFunctionNotAllowedInHaving() {
        Table users = Table.of("users");
        var userName = users.column("name", String.class);
        var age = users.column("age", Integer.class);

        Query query = ClickHouseDsl.select(userName, count())
            .from(users)
            .groupBy(userName)
            .having(rowNumber(ClickHouseDsl.window().partitionBy(userName).orderBy(age.desc())).gt(ClickHouseDsl.param(1L, Long.class)))
            .build();

        assertThat(analyzer.validate(query).errors())
            .extracting(error -> error.code())
            .containsExactly("WINDOW_FUNCTION_NOT_ALLOWED_IN_HAVING");
    }

    @Test
    void validatesWindowFunctionNotAllowedInGroupBy() {
        Table users = Table.of("users");
        var userName = users.column("name", String.class);
        var age = users.column("age", Integer.class);

        Query query = ClickHouseDsl.select(userName)
            .from(users)
            .groupBy(rowNumber(ClickHouseDsl.window().partitionBy(userName).orderBy(age.desc())))
            .build();

        assertThat(analyzer.validate(query).errors())
            .extracting(error -> error.code())
            .containsExactly("GROUP_BY_MISMATCH", "WINDOW_FUNCTION_NOT_ALLOWED_IN_GROUP_BY");
    }

    @Test
    void rejectsEmptySelectionList() {
        assertThatThrownBy(() -> new QueryBuilder(new io.github.heonny.clickhousedsl.model.Expression<?>[0]))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("At least one selection");
    }

    @Test
    void rendersWithAndUnionAllQueriesInOrder() {
        Table rawUsers = Table.of("raw_users");
        Table users = Table.of("active_users");
        Table archivedUsers = Table.of("archived_users");
        var rawName = rawUsers.column("name", String.class);
        var activeName = users.column("name", String.class);
        var archivedName = archivedUsers.column("name", String.class);

        Query cte = ClickHouseDsl.select(rawName)
            .from(rawUsers)
            .where(rawName.eq("alice"))
            .build();

        Query archivedQuery = ClickHouseDsl.select(archivedName)
            .from(archivedUsers)
            .where(archivedName.eq("bob"))
            .build();

        Query query = ClickHouseDsl.select(activeName)
            .with(ClickHouseDsl.with("active_users", cte))
            .from(users)
            .unionAll(archivedQuery)
            .build();

        RenderedQuery rendered = renderer.render(query);

        assertThat(rendered.sql()).isEqualTo(
            "WITH `active_users` AS (SELECT `raw_users`.`name` FROM `raw_users` WHERE `raw_users`.`name` = ?) " +
                "SELECT `active_users`.`name` FROM `active_users` UNION ALL " +
                "SELECT `archived_users`.`name` FROM `archived_users` WHERE `archived_users`.`name` = ?"
        );
        assertThat(rendered.parameters()).containsExactly("alice", "bob");
    }

    @Test
    void validatesUnionSelectionCountMismatch() {
        Table users = Table.of("users");
        var userName = users.column("name", String.class);
        var userAge = users.column("age", Integer.class);

        Query right = ClickHouseDsl.select(userName, userAge)
            .from(users)
            .build();

        Query left = ClickHouseDsl.select(userName)
            .from(users)
            .union(right)
            .build();

        assertThat(analyzer.validate(left).errors())
            .extracting(error -> error.code())
            .containsExactly("UNION_SELECTION_COUNT_MISMATCH");
        assertThat(analyzer.validate(left).errors().get(0).detail())
            .contains("Left count", "right count");
    }

    @Test
    void validatesUnionSelectionTypeMismatch() {
        Table users = Table.of("users");
        var userName = users.column("name", String.class);
        var userAge = users.column("age", Integer.class);

        Query right = ClickHouseDsl.select(userAge)
            .from(users)
            .build();

        Query left = ClickHouseDsl.select(userName)
            .from(users)
            .union(right)
            .build();

        assertThat(analyzer.validate(left).errors())
            .extracting(error -> error.code())
            .containsExactly("UNION_SELECTION_TYPE_MISMATCH");
        assertThat(analyzer.validate(left).errors().get(0).detail())
            .contains("Position 0");
    }

    @Test
    void validatesArrayJoinRequiresArrayTypedExpression() {
        Table users = Table.of("users");
        var userName = users.column("name", String.class);

        Query query = ClickHouseDsl.select(userName)
            .from(users)
            .arrayJoin(userName)
            .build();

        assertThat(analyzer.validate(query).errors())
            .extracting(error -> error.code())
            .containsExactly("ARRAY_JOIN_REQUIRES_ARRAY_TYPE");
        assertThat(analyzer.validate(query).errors().get(0).clause())
            .isEqualTo(ValidationClause.ARRAY_JOIN);
    }

    @Test
    void validatesDuplicateSettingNames() {
        Table users = Table.of("users");
        var userName = users.column("name", String.class);

        Query query = ClickHouseDsl.select(userName)
            .from(users)
            .settings(maxThreads(4), io.github.heonny.clickhousedsl.model.Setting.of("max_threads", 8))
            .build();

        assertThat(analyzer.validate(query).errors())
            .extracting(error -> error.code())
            .containsExactly("DUPLICATE_SETTING_NAME");
        assertThat(analyzer.validate(query).errors().get(0).clause())
            .isEqualTo(ValidationClause.SETTINGS);
        assertThat(analyzer.validate(query).errors().get(0).detail())
            .contains("max_threads");
    }

    @Test
    void validateOrThrowReturnsQueryWhenValidationSucceeds() {
        Table users = Table.of("users");
        var userName = users.column("name", String.class);

        Query query = ClickHouseDsl.select(userName)
            .from(users)
            .build();

        assertThat(ClickHouseDsl.validateOrThrow(query)).isSameAs(query);
    }

    @Test
    void validateOrThrowUsesStructuredExceptionMessage() {
        Table users = Table.of("users");
        var userName = users.column("name", String.class);

        Query query = ClickHouseDsl.select(userName, count())
            .from(users)
            .build();

        assertThatThrownBy(() -> ClickHouseDsl.validateOrThrow(query))
            .isInstanceOf(io.github.heonny.clickhousedsl.validate.QueryValidationException.class)
            .hasMessageContaining("GROUP_BY_REQUIRED")
            .hasMessageContaining("SELECT")
            .hasMessageContaining("Aggregate and non-aggregate selections");
    }

    @Test
    void renderValidatedFailsFastForInvalidQuery() {
        Table users = Table.of("users");
        var userName = users.column("name", String.class);

        Query query = ClickHouseDsl.select(userName, count())
            .from(users)
            .build();

        assertThatThrownBy(() -> ClickHouseDsl.renderValidated(query))
            .isInstanceOf(io.github.heonny.clickhousedsl.validate.QueryValidationException.class)
            .hasMessageContaining("GROUP_BY_REQUIRED");
    }

    @Test
    void renderValidatedQueryReturnsSqlAndParametersForValidQuery() {
        Table users = Table.of("users");
        var userName = users.column("name", String.class);

        Query query = ClickHouseDsl.select(userName)
            .from(users)
            .where(userName.eq("alice"))
            .build();

        RenderedQuery renderedQuery = ClickHouseDsl.renderValidatedQuery(query);

        assertThat(renderedQuery.sql()).isEqualTo("SELECT `users`.`name` FROM `users` WHERE `users`.`name` = ?");
        assertThat(renderedQuery.parameters()).containsExactly("alice");
    }

    @Test
    void rejectsNullSettingValue() {
        assertThatThrownBy(() -> io.github.heonny.clickhousedsl.model.Setting.of("max_threads", null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("value");
    }

    @Test
    void rejectsNullElementsInVarargDslInputs() {
        Table users = Table.of("users");
        var userName = users.column("name", String.class);

        assertThatThrownBy(() -> ClickHouseDsl.select((io.github.heonny.clickhousedsl.model.Expression<?>[]) new io.github.heonny.clickhousedsl.model.Expression<?>[]{userName, null}))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("selections");

        assertThatThrownBy(() -> ClickHouseDsl.select(userName).from(users).settings(new io.github.heonny.clickhousedsl.model.Setting[]{null}))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("settings");

        assertThatThrownBy(() -> ClickHouseDsl.select(userName).from(users).arrayJoin(new io.github.heonny.clickhousedsl.model.Expression<?>[]{null}))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("expressions");
    }

    @Test
    void rendersWindowFunctionsWithPartitionAndOrder() {
        Table users = Table.of("users").as("u");
        var userName = users.column("name", String.class);
        var age = users.column("age", Integer.class);
        var score = users.column("score", Integer.class);

        Query query = ClickHouseDsl.select(
                userName,
                rowNumber(ClickHouseDsl.window().partitionBy(userName).orderBy(age.desc())),
                io.github.heonny.clickhousedsl.model.Expressions.sum(score).over(ClickHouseDsl.window().partitionBy(userName).orderBy(age.asc()))
            )
            .from(users)
            .build();

        RenderedQuery rendered = renderer.render(query);

        assertThat(rendered.sql()).isEqualTo(
            "SELECT `u`.`name`, rowNumber() OVER (PARTITION BY `u`.`name` ORDER BY `u`.`age` DESC), " +
                "sum(`u`.`score`) OVER (PARTITION BY `u`.`name` ORDER BY `u`.`age` ASC) FROM `users` AS `u`"
        );
        assertThat(rendered.parameters()).isEmpty();
    }

    @Test
    void rendersExplainPlanForQuery() {
        Table users = Table.of("users");
        var userName = users.column("name", String.class);

        Query query = ClickHouseDsl.select(userName)
            .from(users)
            .where(userName.eq("alice"))
            .build();

        String explainSql = ClickHouseDsl.render(ClickHouseDsl.explain(ExplainType.PLAN, query));

        assertThat(explainSql).isEqualTo(
            "EXPLAIN PLAN SELECT `users`.`name` FROM `users` WHERE `users`.`name` = ?"
        );
    }
}
