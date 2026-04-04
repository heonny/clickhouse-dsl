package io.github.heonny.clickhousedsl.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.heonny.clickhousedsl.api.ClickHouseDsl;
import io.github.heonny.clickhousedsl.explain.ExplainResult;
import io.github.heonny.clickhousedsl.explain.ExplainType;
import io.github.heonny.clickhousedsl.render.ClickHouseRenderer;
import io.github.heonny.clickhousedsl.validate.ValidationError;
import io.github.heonny.clickhousedsl.validate.ValidationClause;
import io.github.heonny.clickhousedsl.validate.ValidationCode;
import io.github.heonny.clickhousedsl.validate.ValidationResult;
import org.junit.jupiter.api.Test;

class ModelCoverageTest {

    @Test
    void identifierSupportsValueEqualityAndSafeSqlRendering() {
        Identifier identifier = Identifier.of("analytics.users");

        assertThat(identifier.value()).isEqualTo("analytics.users");
        assertThat(identifier.sql()).isEqualTo("`analytics`.`users`");
        assertThat(identifier.toString()).isEqualTo("analytics.users");
        assertThat(identifier).isEqualTo(Identifier.of("analytics.users"));
        assertThat(identifier).hasSameHashCodeAs(Identifier.of("analytics.users"));
        assertThat(identifier.equals("analytics.users")).isFalse();
    }

    @Test
    void identifierRejectsUnsafePart() {
        assertThatThrownBy(() -> Identifier.of("bad-name"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unsafe identifier");
    }

    @Test
    void tableColumnAndPojoAccessorsWork() {
        Table users = Table.of("users");
        Table aliased = users.as("u");
        Table finalized = aliased.finalTable();
        Column<Long> id = finalized.column("id", Long.class);
        ArrayColumn<String> tags = finalized.arrayColumn("tags", String.class);
        StateColumn<Long> state = finalized.stateColumn("count_state", Long.class);

        assertThat(users.name()).isEqualTo(Identifier.of("users"));
        assertThat(users.alias()).isNull();
        assertThat(users.finalModifier()).isFalse();
        assertThat(aliased.renderReference()).isEqualTo("`u`");
        assertThat(finalized.renderFromClause()).isEqualTo("`users` FINAL AS `u`");
        assertThat(id.type()).isEqualTo(Long.class);
        assertThat(id.render(new RenderContext())).isEqualTo("`u`.`id`");
        assertThat(tags.type()).isEqualTo((Class<?>) java.util.List.class);
        assertThat(tags.elementType()).isEqualTo(String.class);
        assertThat(tags.render(new RenderContext())).isEqualTo("`u`.`tags`");
        assertThat(state.valueType()).isEqualTo(Long.class);
        assertThat(state.render(new RenderContext())).isEqualTo("`u`.`count_state`");
        assertThat(id.asc().direction()).isEqualTo(SortDirection.ASC);
        assertThat(id.desc().direction()).isEqualTo(SortDirection.DESC);
        assertThat(users).isEqualTo(Table.of("users"));
        assertThat(users.equals(aliased)).isFalse();
        assertThat(users.equals("users")).isFalse();
        assertThat(users.hashCode()).isEqualTo(Table.of("users").hashCode());
        assertThat(id).isEqualTo(finalized.column("id", Long.class));
        assertThat(id.hashCode()).isEqualTo(finalized.column("id", Long.class).hashCode());
    }

    @Test
    void parameterAndRenderedQueryExposeStoredValues() {
        ParameterExpression<String> parameter = new ParameterExpression<>("alice", String.class);
        RenderContext context = new RenderContext();
        String placeholder = parameter.render(context);
        RenderedQuery renderedQuery = new RenderedQuery("SELECT ?", context.parameters());
        ExecutionMetrics metrics = new ExecutionMetrics(1024L, 4);
        QueryExecutionReport report = new QueryExecutionReport(renderedQuery, metrics);

        assertThat(parameter.value()).isEqualTo("alice");
        assertThat(parameter.type()).isEqualTo(String.class);
        assertThat(placeholder).isEqualTo("?");
        assertThat(context.parameters()).containsExactly("alice");
        assertThat(renderedQuery.sql()).isEqualTo("SELECT ?");
        assertThat(renderedQuery.parameters()).containsExactly("alice");
        assertThat(metrics.maxMemoryUsageBytes()).isEqualTo(1024L);
        assertThat(metrics.usedThreads()).isEqualTo(4);
        assertThat(report.renderedQuery()).isEqualTo(renderedQuery);
        assertThat(report.metrics()).isEqualTo(metrics);
    }

    @Test
    void comparisonAndLogicalExpressionsRenderAsExpected() {
        Table users = Table.of("users").as("u");
        Column<Integer> age = users.column("age", Integer.class);
        Column<Integer> score = users.column("score", Integer.class);

        ComparisonExpression eq = age.eq(18);
        ComparisonExpression gt = age.gt(score);
        ComparisonExpression lt = age.lt(65);
        LogicalExpression logical = eq.and(gt).or(lt);

        assertThat(eq.type()).isEqualTo(Boolean.class);
        assertThat(eq.left()).isEqualTo(age);
        assertThat(eq.right()).isInstanceOf(ParameterExpression.class);
        assertThat(eq.render(new RenderContext())).isEqualTo("`u`.`age` = ?");
        assertThat(gt.render(new RenderContext())).isEqualTo("`u`.`age` > `u`.`score`");
        assertThat(logical.type()).isEqualTo(Boolean.class);
        assertThat(logical.left()).isInstanceOf(LogicalExpression.class);
        assertThat(logical.right()).isEqualTo(lt);
        assertThat(logical.render(new RenderContext())).isEqualTo("((`u`.`age` = ? AND `u`.`age` > `u`.`score`) OR `u`.`age` < ?)");
    }

    @Test
    void expressionDefaultHelpersCoverAliasAndComparisonVariants() {
        Table users = Table.of("users").as("u");
        Column<Integer> age = users.column("age", Integer.class);
        ReferenceExpression<Integer> aliasRef = Expressions.ref("age_alias", Integer.class);

        AliasedExpression<Integer> aliased = age.as("age_alias");
        ComparisonExpression eq = age.eq(aliasRef);
        ComparisonExpression gt = age.gt(Expressions.literal(10, Integer.class));
        ComparisonExpression lt = age.lt(aliasRef);
        ComparisonExpression gte = age.gte(18);
        ComparisonExpression lte = age.lte(Expressions.param(65, Integer.class));

        assertThat(aliased.alias()).isEqualTo(Identifier.of("age_alias"));
        assertThat(aliased.delegate()).isEqualTo(age);
        assertThat(aliased.type()).isEqualTo(Integer.class);
        assertThat(aliased.aggregate()).isFalse();
        assertThat(aliased.render(new RenderContext())).isEqualTo("`u`.`age` AS `age_alias`");
        assertThat(eq.render(new RenderContext())).isEqualTo("`u`.`age` = `age_alias`");
        assertThat(gt.render(new RenderContext())).isEqualTo("`u`.`age` > 10");
        assertThat(lt.render(new RenderContext())).isEqualTo("`u`.`age` < `age_alias`");
        assertThat(gte.render(new RenderContext())).isEqualTo("`u`.`age` >= ?");
        assertThat(lte.render(new RenderContext())).isEqualTo("`u`.`age` <= ?");
    }

    @Test
    void aggregateAndUtilityFactoriesRemainAccessible() {
        AggregateExpression<Long> count = ClickHouseDsl.count();
        Expression<Long> parameter = ClickHouseDsl.param(3L, Long.class);
        AggregateExpression<Integer> summed = Expressions.sum(Expressions.param(7, Integer.class));
        AggregateStateExpression<Integer> state = ClickHouseDsl.sumState(Expressions.param(7, Integer.class));
        AggregateExpression<Integer> merged = ClickHouseDsl.sumMerge(state);
        FunctionExpression<Integer> scalarFunction = Expressions.function("toUInt8", Integer.class, Expressions.param(1, Integer.class));
        BinaryArithmeticExpression<Double> division = Expressions.divide(Expressions.param(10, Integer.class), Expressions.param(2, Integer.class));

        assertThat(count.aggregate()).isTrue();
        assertThat(count.type()).isEqualTo(Long.class);
        assertThat(count.eq(parameter).render(new RenderContext())).isEqualTo("count() = ?");
        assertThat(count.gt(parameter).render(new RenderContext())).isEqualTo("count() > ?");
        assertThat(summed.aggregate()).isTrue();
        assertThat(summed.type()).isEqualTo(Integer.class);
        assertThat(summed.render(new RenderContext())).startsWith("sum(");
        assertThat(state.aggregate()).isTrue();
        assertThat(state.valueType()).isEqualTo(Integer.class);
        assertThat(state.render(new RenderContext())).isEqualTo("sumState(?)");
        assertThat(merged.aggregate()).isTrue();
        assertThat(merged.type()).isEqualTo(Integer.class);
        assertThat(merged.render(new RenderContext())).isEqualTo("sumMerge(sumState(?))");
        assertThat(scalarFunction.arguments()).hasSize(1);
        assertThat(scalarFunction.render(new RenderContext())).isEqualTo("toUInt8(?)");
        assertThat(division.left()).isInstanceOf(ParameterExpression.class);
        assertThat(division.right()).isInstanceOf(ParameterExpression.class);
        assertThat(division.render(new RenderContext())).isEqualTo("? / ?");
        assertThatThrownBy(() -> FunctionExpression.of("toUInt8", Integer.class, false, new Expression<?>[]{null}))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("arguments");
    }

    @Test
    void literalExpressionCoversNullStringCharacterBooleanAndNumberRendering() {
        assertThat(Expressions.literal(null, String.class).render(new RenderContext())).isEqualTo("NULL");
        assertThat(Expressions.literal("O'Reilly", String.class).render(new RenderContext())).isEqualTo("'O''Reilly'");
        assertThat(Expressions.literal('\'', Character.class).render(new RenderContext())).isEqualTo("''''");
        assertThat(Expressions.literal(true, Boolean.class).render(new RenderContext())).isEqualTo("1");
        assertThat(Expressions.literal(false, Boolean.class).render(new RenderContext())).isEqualTo("0");
        assertThat(Expressions.literal(42, Integer.class).render(new RenderContext())).isEqualTo("42");
    }

    @Test
    void clickHouseDslHelpersAndLeftJoinPathAreCovered() {
        Table users = Table.of("users").as("u");
        Table events = Table.of("events").as("e");
        Column<Long> userId = users.column("id", Long.class);
        Column<Long> eventUserId = events.column("user_id", Long.class);
        Query query = ClickHouseDsl.select(userId)
            .from(users)
            .leftJoin(events).on(userId, eventUserId)
            .settings(ClickHouseDsl.maxThreads(2), ClickHouseDsl.maxMemoryUsage(4096L), ClickHouseDsl.useUncompressedCache(false))
            .build();

        String sql = ClickHouseDsl.render(query);
        ValidationResult result = ClickHouseDsl.analyze(query);

        assertThat(sql).isEqualTo(
            "SELECT `u`.`id` FROM `users` AS `u` LEFT JOIN `events` AS `e` ON `u`.`id` = `e`.`user_id` " +
                "SETTINGS `max_threads` = ?, `max_memory_usage` = ?, `use_uncompressed_cache` = ?"
        );
        assertThat(result.valid()).isTrue();
        assertThat(result.errors()).isEmpty();
    }

    @Test
    void validationPojoObjectsExposeMessagesAndValidity() {
        ValidationResult result = new ValidationResult();
        result.add("CODE", ValidationClause.SELECT, "message", "detail");
        ValidationError error = result.errors().get(0);

        assertThat(result.valid()).isFalse();
        assertThat(error.code()).isEqualTo("CODE");
        assertThat(error.clause()).isEqualTo(ValidationClause.SELECT);
        assertThat(error.message()).isEqualTo("message");
        assertThat(error.detail()).isEqualTo("detail");
        assertThat(error.toString()).contains("CODE", "SELECT", "message", "detail");
    }

    @Test
    void validationResultSupportsStableCodeAndThrowIfInvalid() {
        ValidationResult result = new ValidationResult();
        result.add(ValidationCode.GROUP_BY_REQUIRED, "extra detail");

        assertThat(result.errors()).hasSize(1);
        assertThat(result.errors().get(0).code()).isEqualTo("GROUP_BY_REQUIRED");
        assertThat(result.errors().get(0).clause()).isEqualTo(ValidationClause.SELECT);
        assertThat(result.errors().get(0).message()).contains("GROUP BY");
        assertThat(result.errors().get(0).detail()).isEqualTo("extra detail");
        assertThatThrownBy(result::throwIfInvalid)
            .isInstanceOf(io.github.heonny.clickhousedsl.validate.QueryValidationException.class)
            .hasMessageContaining("GROUP_BY_REQUIRED");
        try {
            result.throwIfInvalid();
        } catch (io.github.heonny.clickhousedsl.validate.QueryValidationException exception) {
            assertThat(exception.validationResult()).isSameAs(result);
        }
    }

    @Test
    void queryAndJoinPojoAccessorsExposeState() {
        Table users = Table.of("users");
        Table events = Table.of("events");
        Column<Long> userId = users.column("id", Long.class);
        Join join = new Join(JoinType.LEFT, events, userId, userId);
        Query query = new Query(
            java.util.List.of(userId),
            java.util.List.of(),
            users,
            java.util.List.of(join),
            java.util.List.of(userId),
            0.3d,
            userId.eq(1L),
            userId.eq(2L),
            java.util.List.of(userId),
            userId.eq(3L),
            java.util.List.of(userId.asc()),
            5,
            java.util.List.of(Setting.of("max_threads", 2)),
            java.util.List.of()
        );

        assertThat(join.type()).isEqualTo(JoinType.LEFT);
        assertThat(join.table()).isEqualTo(events);
        assertThat(join.leftKey()).isEqualTo(userId);
        assertThat(join.rightKey()).isEqualTo(userId);
        assertThat(query.selections()).hasSize(1);
        assertThat(query.withClauses()).isEmpty();
        assertThat(query.from()).isEqualTo(users);
        assertThat(query.joins()).hasSize(1);
        assertThat(query.arrayJoins()).hasSize(1);
        assertThat(query.sampleRatio()).isEqualTo(0.3d);
        assertThat(query.prewhere()).isNotNull();
        assertThat(query.where()).isNotNull();
        assertThat(query.groupBy()).hasSize(1);
        assertThat(query.having()).isNotNull();
        assertThat(query.orderBy()).hasSize(1);
        assertThat(query.limit()).isEqualTo(5);
        assertThat(query.settings()).hasSize(1);
        assertThat(query.setOperations()).isEmpty();
        assertThat(Setting.of("max_threads", 2).name()).isEqualTo(Identifier.of("max_threads"));
        assertThat(Setting.of("max_threads", 2).value()).isEqualTo(2);
    }

    @Test
    void executionMetricsRejectInvalidValues() {
        assertThatThrownBy(() -> new ExecutionMetrics(-1L, 2))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("maxMemoryUsageBytes");

        assertThatThrownBy(() -> new ExecutionMetrics(1L, 0))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("usedThreads");
    }

    @Test
    void withClauseAndSetOperationExposeState() {
        Table users = Table.of("users");
        Column<String> name = users.column("name", String.class);
        Query subQuery = ClickHouseDsl.select(name).from(users).build();
        WithClause withClause = WithClause.of("user_view", subQuery);
        SetOperation setOperation = new SetOperation(UnionType.ALL, subQuery);

        assertThat(withClause.alias()).isEqualTo(Identifier.of("user_view"));
        assertThat(withClause.query()).isEqualTo(subQuery);
        assertThat(setOperation.type()).isEqualTo(UnionType.ALL);
        assertThat(setOperation.query()).isEqualTo(subQuery);
        assertThat(UnionType.DISTINCT.sql()).isEqualTo("UNION");
        assertThat(UnionType.ALL.sql()).isEqualTo("UNION ALL");
    }

    @Test
    void windowSpecAndWindowFunctionExposeState() {
        Table users = Table.of("users").as("u");
        Column<String> name = users.column("name", String.class);
        Column<Integer> age = users.column("age", Integer.class);

        WindowSpec spec = ClickHouseDsl.window().partitionBy(name).orderBy(age.desc());
        WindowFunctionExpression<Long> rowNumber = ClickHouseDsl.rowNumber(spec);

        assertThat(spec.partitionBy()).containsExactly(name);
        assertThat(spec.orderBy()).hasSize(1);
        assertThat(spec.render(new RenderContext())).isEqualTo("PARTITION BY `u`.`name` ORDER BY `u`.`age` DESC");
        assertThat(rowNumber.type()).isEqualTo(Long.class);
        assertThat(rowNumber.windowSpec()).isEqualTo(spec);
        assertThat(rowNumber.render(new RenderContext())).isEqualTo("rowNumber() OVER (PARTITION BY `u`.`name` ORDER BY `u`.`age` DESC)");
        assertThatThrownBy(() -> ClickHouseDsl.window().partitionBy(new Expression<?>[]{null}))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("expressions");
        assertThatThrownBy(() -> ClickHouseDsl.window().orderBy(new Sort[]{null}))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("sorts");
    }

    @Test
    void explainModelsAndAnalyzerExposeState() {
        Table users = Table.of("users");
        Column<String> name = users.column("name", String.class);
        Query query = ClickHouseDsl.select(name).from(users).build();

        var explainQuery = ClickHouseDsl.explain(ExplainType.PIPELINE, query);
        ExplainResult result = ClickHouseDsl.analyze(
            ExplainType.PLAN,
            """
            ReadFromStorage
            Prewhere
            Filter
            Join
            Aggregating
            Sorting
            """
        );

        assertThat(explainQuery.type()).isEqualTo(ExplainType.PIPELINE);
        assertThat(explainQuery.query()).isEqualTo(query);
        assertThat(ExplainType.AST.sql()).isEqualTo("AST");
        assertThat(result.type()).isEqualTo(ExplainType.PLAN);
        assertThat(result.raw()).contains("ReadFromStorage");
        assertThat(result.summary().readsFromStorage()).isTrue();
        assertThat(result.summary().hasFilter()).isTrue();
        assertThat(result.summary().hasPrewhere()).isTrue();
        assertThat(result.summary().hasJoin()).isTrue();
        assertThat(result.summary().hasAggregation()).isTrue();
        assertThat(result.summary().hasSorting()).isTrue();
        assertThat(result.summary().notes()).isNotEmpty();
    }

    @Test
    void rendererHandlesMinimalQueryWithoutOptionalClauses() {
        Table users = Table.of("users");
        Column<String> name = users.column("name", String.class);
        RenderedQuery rendered = new ClickHouseRenderer().render(
            ClickHouseDsl.select(name)
                .from(users)
                .build()
        );

        assertThat(rendered.sql()).isEqualTo("SELECT `users`.`name` FROM `users`");
        assertThat(rendered.parameters()).isEmpty();
    }
}
