package io.github.heonny.clickhousedsl.validate;

import static io.github.heonny.clickhousedsl.api.ClickHouseDsl.count;
import static io.github.heonny.clickhousedsl.api.ClickHouseDsl.function;
import static io.github.heonny.clickhousedsl.api.ClickHouseDsl.literal;
import static io.github.heonny.clickhousedsl.api.ClickHouseDsl.param;
import static io.github.heonny.clickhousedsl.api.ClickHouseDsl.ref;
import static io.github.heonny.clickhousedsl.api.ClickHouseDsl.rowNumber;
import static io.github.heonny.clickhousedsl.api.ClickHouseDsl.select;
import static io.github.heonny.clickhousedsl.api.ClickHouseDsl.window;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.heonny.clickhousedsl.model.Expression;
import io.github.heonny.clickhousedsl.model.Query;
import io.github.heonny.clickhousedsl.model.Table;
import org.junit.jupiter.api.Test;

class SemanticAnalyzerCoverageTest {

    private final SemanticAnalyzer analyzer = new SemanticAnalyzer();

    @Test
    void detectsAggregateHiddenInsideAliasedFunctionTree() {
        Table users = Table.of("users");
        var userId = users.column("id", Long.class);
        Expression<Long> hiddenAggregate = function("identity", Long.class, count()).as("agg_alias");

        Query query = select(userId)
            .from(users)
            .prewhere(userId.gt(hiddenAggregate))
            .build();

        assertThat(analyzer.validate(query).errors())
            .extracting(ValidationError::code)
            .containsExactly("AGGREGATE_NOT_ALLOWED_IN_PREWHERE");
    }

    @Test
    void detectsAggregateInsideLogicalAndArithmeticTrees() {
        Table users = Table.of("users");
        var age = users.column("age", Integer.class);
        Expression<Boolean> aggregateInArithmetic = function(
            "isNotNull",
            Boolean.class,
            io.github.heonny.clickhousedsl.model.Expressions.divide(
                param(10, Integer.class),
                function("identity", Integer.class, io.github.heonny.clickhousedsl.model.Expressions.sum(param(1, Integer.class)))
            )
        );
        Expression<Boolean> whereExpression = age.gt(param(18, Integer.class)).or(aggregateInArithmetic);

        Query query = select(age)
            .from(users)
            .where(whereExpression)
            .build();

        assertThat(analyzer.validate(query).errors())
            .extracting(ValidationError::code)
            .containsExactly("AGGREGATE_NOT_ALLOWED_IN_WHERE");
    }

    @Test
    void detectsWindowFunctionHiddenInsideAliasedFunctionTree() {
        Table users = Table.of("users").as("u");
        var userId = users.column("id", Long.class);
        var age = users.column("age", Integer.class);
        Expression<Long> hiddenWindow = function(
            "identity",
            Long.class,
            rowNumber(window().partitionBy(userId).orderBy(age.desc()))
        ).as("row_num_alias");

        Query query = select(userId)
            .from(users)
            .where(userId.gt(hiddenWindow))
            .build();

        assertThat(analyzer.validate(query).errors())
            .extracting(ValidationError::code)
            .containsExactly("WINDOW_FUNCTION_NOT_ALLOWED_IN_WHERE");
    }

    @Test
    void detectsWindowFunctionInsideLogicalAndArithmeticTrees() {
        Table users = Table.of("users").as("u");
        var userId = users.column("id", Long.class);
        var age = users.column("age", Integer.class);
        Expression<Boolean> windowInArithmetic = function(
            "isNotNull",
            Boolean.class,
            io.github.heonny.clickhousedsl.model.Expressions.divide(
                param(10, Integer.class),
                function("identity", Long.class, rowNumber(window().partitionBy(userId).orderBy(age.desc())))
            )
        );
        Expression<Boolean> prewhereExpression = userId.gt(param(1L, Long.class)).or(windowInArithmetic);

        Query query = select(userId)
            .from(users)
            .prewhere(prewhereExpression)
            .build();

        assertThat(analyzer.validate(query).errors())
            .extracting(ValidationError::code)
            .containsExactly("WINDOW_FUNCTION_NOT_ALLOWED_IN_PREWHERE");
    }

    @Test
    void allowsGroupedReferencesAndLiteralOnlyHavingExpressions() {
        Table users = Table.of("users").as("u");
        Expression<Integer> groupedReference = ref("age_alias", Integer.class);
        Query query = new Query(
            java.util.List.of(groupedReference),
            java.util.List.of(),
            users,
            java.util.List.of(),
            java.util.List.of(),
            null,
            null,
            null,
            java.util.List.of(groupedReference),
            groupedReference.eq(param(18, Integer.class)).and(function("equals", Boolean.class, literal(1, Integer.class), param(1, Integer.class))),
            java.util.List.of(),
            null,
            java.util.List.of(),
            java.util.List.of()
        );

        assertThat(analyzer.validate(query).errors()).isEmpty();
    }

    @Test
    void detectsUngroupedReferenceOnRightSideOfHavingComparison() {
        Table users = Table.of("users").as("u");
        var age = users.column("age", Integer.class);
        var score = users.column("score", Integer.class);

        Query query = select(age)
            .from(users)
            .groupBy(age)
            .having(age.eq(score))
            .build();

        assertThat(analyzer.validate(query).errors())
            .extracting(ValidationError::code)
            .containsExactly("HAVING_EXPRESSION_NOT_GROUPED");
    }

    @Test
    void detectsUngroupedExpressionsInsideLogicalArithmeticAndFunctionHavingTrees() {
        Table users = Table.of("users").as("u");
        var age = users.column("age", Integer.class);
        var score = users.column("score", Integer.class);
        var level = users.column("level", Integer.class);
        Expression<Boolean> logicalHaving = age.eq(param(18, Integer.class)).and(level.eq(param(1, Integer.class)));
        Expression<Boolean> arithmeticHaving = io.github.heonny.clickhousedsl.model.Expressions
            .divide(age, score)
            .gt(param(1.0d, Double.class));
        Expression<Boolean> functionHaving = function("identity", Boolean.class, score.as("score_alias")).eq(literal(true, Boolean.class));

        Query logicalQuery = select(age)
            .from(users)
            .groupBy(age)
            .having(logicalHaving)
            .build();
        Query arithmeticQuery = select(age)
            .from(users)
            .groupBy(age)
            .having(arithmeticHaving)
            .build();
        Query functionQuery = select(age)
            .from(users)
            .groupBy(age)
            .having(functionHaving)
            .build();

        assertThat(analyzer.validate(logicalQuery).errors())
            .extracting(ValidationError::code)
            .containsExactly("HAVING_EXPRESSION_NOT_GROUPED");
        assertThat(analyzer.validate(arithmeticQuery).errors())
            .extracting(ValidationError::code)
            .containsExactly("HAVING_EXPRESSION_NOT_GROUPED");
        assertThat(analyzer.validate(functionQuery).errors())
            .extracting(ValidationError::code)
            .containsExactly("HAVING_EXPRESSION_NOT_GROUPED");
    }

    @Test
    void treatsAggregateArgumentsInsideHavingFunctionsAsGroupedSafe() {
        Table users = Table.of("users");
        var userId = users.column("id", Long.class);

        Query query = select(userId, count())
            .from(users)
            .groupBy(userId)
            .having(function("isNotNull", Boolean.class, count()))
            .build();

        assertThat(analyzer.validate(query).errors()).isEmpty();
    }
}
