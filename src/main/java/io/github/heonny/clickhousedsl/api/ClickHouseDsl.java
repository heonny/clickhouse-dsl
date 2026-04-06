package io.github.heonny.clickhousedsl.api;

import io.github.heonny.clickhousedsl.model.Expression;
import io.github.heonny.clickhousedsl.model.Expressions;
import io.github.heonny.clickhousedsl.explain.ExplainAnalyzer;
import io.github.heonny.clickhousedsl.explain.ExplainQuery;
import io.github.heonny.clickhousedsl.explain.ExplainRenderer;
import io.github.heonny.clickhousedsl.explain.ExplainResult;
import io.github.heonny.clickhousedsl.explain.ExplainType;
import io.github.heonny.clickhousedsl.model.AggregateExpression;
import io.github.heonny.clickhousedsl.model.AggregateStateExpression;
import io.github.heonny.clickhousedsl.model.BinaryArithmeticExpression;
import io.github.heonny.clickhousedsl.model.Query;
import io.github.heonny.clickhousedsl.model.RenderedQuery;
import io.github.heonny.clickhousedsl.model.Setting;
import io.github.heonny.clickhousedsl.model.Sort;
import io.github.heonny.clickhousedsl.model.Table;
import io.github.heonny.clickhousedsl.model.WindowFunctionExpression;
import io.github.heonny.clickhousedsl.model.WindowSpec;
import io.github.heonny.clickhousedsl.model.WithClause;
import io.github.heonny.clickhousedsl.render.ClickHouseRenderer;
import io.github.heonny.clickhousedsl.render.RenderOptions;
import io.github.heonny.clickhousedsl.validate.SemanticAnalyzer;
import io.github.heonny.clickhousedsl.validate.QueryValidationException;
import io.github.heonny.clickhousedsl.validate.ValidationResult;
import java.util.Objects;

/**
 * Entry point for building type-aware ClickHouse queries.
 *
 * <p>This class exposes the public DSL surface area:
 *
 * <ul>
 *   <li>query construction via step interfaces
 *   <li>expression and parameter factories
 *   <li>ClickHouse-specific settings helpers
 *   <li>SQL rendering
 *   <li>semantic validation
 *   <li>EXPLAIN request and analysis helpers
 * </ul>
 *
 * <p>The DSL intentionally splits responsibility:
 *
 * <ul>
 *   <li>compile-time guardrails handle ordering and a subset of type-safe composition
 *   <li>{@link SemanticAnalyzer} handles rules that are too dynamic or expensive for Java's type system
 * </ul>
 */
public final class ClickHouseDsl {

    private ClickHouseDsl() {
    }

    /**
     * Starts a {@code SELECT} query.
     *
     * <p>At least one selection is required. The returned step forces callers to continue with a
     * valid query shape instead of assembling raw SQL fragments.
     *
     * @param selections expressions that should appear in the {@code SELECT} list
     * @return the first DSL step for building a query
     */
    public static SelectStep select(Expression<?>... selections) {
        return new QueryBuilder(selections);
    }

    /**
     * Creates a parameter expression rendered as a placeholder.
     *
     * <p>The concrete value is stored in the render context and is not interpolated into the SQL
     * string directly. This is the default path for user-provided values.
     *
     * @param value runtime value to bind
     * @param type declared Java type of the parameter
     * @param <T> parameter type
     * @return placeholder-backed expression
     */
    public static <T> Expression<T> param(T value, Class<T> type) {
        return Expressions.param(value, type);
    }

    /**
     * Creates a literal expression.
     *
     * <p>This is useful for constant function arguments such as precision, timezone, or numeric
     * toggles that should be part of the expression tree.
     *
     * @param value literal value
     * @param type declared Java type of the literal
     * @param <T> literal type
     * @return literal expression
     */
    public static <T> Expression<T> literal(T value, Class<T> type) {
        return Expressions.literal(value, type);
    }

    /**
     * Creates {@code count()}.
     *
     * @return aggregate expression representing {@code count()}
     */
    public static AggregateExpression<Long> count() {
        return Expressions.count();
    }

    /**
     * Creates {@code sum(...)}.
     *
     * @param expression numeric expression to aggregate
     * @param <N> numeric value type
     * @return aggregate expression
     */
    public static <N extends Number> AggregateExpression<N> sum(Expression<N> expression) {
        return Expressions.sum(expression);
    }

    /**
     * Creates {@code sumState(...)} for state-table workflows.
     *
     * @param expression numeric expression to aggregate into a state value
     * @param <N> numeric value type
     * @return aggregate state expression
     */
    public static <N extends Number> AggregateStateExpression<N> sumState(Expression<N> expression) {
        return Expressions.sumState(expression);
    }

    /**
     * Creates {@code sumMerge(...)} from a previously created state expression.
     *
     * @param expression aggregate state expression
     * @param <N> merged numeric value type
     * @return merged aggregate expression
     */
    public static <N extends Number> AggregateExpression<N> sumMerge(AggregateStateExpression<N> expression) {
        return Expressions.sumMerge(expression);
    }

    /**
     * Creates {@code sumMerge(...)} from a state-typed column or reference.
     *
     * <p>This overload is useful when the state comes from a materialized view or rollup table and
     * the DSL does not hold the original state-producing expression.
     *
     * @param expression state-typed expression
     * @param valueType merged numeric value type
     * @param <N> merged numeric value type
     * @return merged aggregate expression
     */
    public static <N extends Number> AggregateExpression<N> sumMerge(io.github.heonny.clickhousedsl.model.Expression<io.github.heonny.clickhousedsl.model.AggregateState<N>> expression, Class<N> valueType) {
        return Expressions.sumMerge(expression, valueType);
    }

    /**
     * Creates {@code countMerge(...)}.
     *
     * @param expression state expression produced by a count-state aggregation
     * @return merged aggregate expression
     */
    public static AggregateExpression<Long> countMerge(Expression<io.github.heonny.clickhousedsl.model.AggregateState<Long>> expression) {
        return Expressions.countMerge(expression);
    }

    /**
     * Creates {@code countIfMerge(...)}.
     *
     * @param expression state expression produced by a conditional count-state aggregation
     * @return merged aggregate expression
     */
    public static AggregateExpression<Long> countIfMerge(Expression<io.github.heonny.clickhousedsl.model.AggregateState<Long>> expression) {
        return Expressions.countIfMerge(expression);
    }

    /**
     * Creates {@code uniqMerge(...)}.
     *
     * @param expression state expression produced by a uniqueness aggregate
     * @return merged aggregate expression
     */
    public static AggregateExpression<Long> uniqMerge(Expression<io.github.heonny.clickhousedsl.model.AggregateState<Long>> expression) {
        return Expressions.uniqMerge(expression);
    }

    /**
     * Creates a generic function call.
     *
     * <p>Use this for ClickHouse functions that are not yet modeled as dedicated typed helpers.
     *
     * @param name ClickHouse function name
     * @param type Java type of the function result
     * @param arguments function arguments
     * @param <T> result type
     * @return function expression
     */
    public static <T> io.github.heonny.clickhousedsl.model.FunctionExpression<T> function(String name, Class<T> type, Expression<?>... arguments) {
        return Expressions.function(name, type, arguments);
    }

    /**
     * Creates a function expression explicitly marked as aggregate.
     *
     * @param name ClickHouse aggregate function name
     * @param type Java type of the aggregate result
     * @param arguments function arguments
     * @param <T> result type
     * @return aggregate function expression
     */
    public static <T> io.github.heonny.clickhousedsl.model.FunctionExpression<T> aggregateFunction(String name, Class<T> type, Expression<?>... arguments) {
        return Expressions.aggregateFunction(name, type, arguments);
    }

    /**
     * Creates a division expression.
     *
     * @param left left operand
     * @param right right operand
     * @return arithmetic expression
     */
    public static BinaryArithmeticExpression<Double> divide(Expression<?> left, Expression<?> right) {
        return Expressions.divide(left, right);
    }

    /**
     * Creates a typed reference expression.
     *
     * <p>This is commonly used for alias-based references in {@code GROUP BY}, {@code ORDER BY},
     * or other projections where the identifier should be treated as an already-declared symbol.
     *
     * @param identifier reference name
     * @param type declared Java type of the reference
     * @param <T> reference type
     * @return reference expression
     */
    public static <T> io.github.heonny.clickhousedsl.model.ReferenceExpression<T> ref(String identifier, Class<T> type) {
        return Expressions.ref(identifier, type);
    }

    /**
     * Creates an empty window specification.
     *
     * @return empty window spec ready for partition/order decoration
     */
    public static WindowSpec window() {
        return WindowSpec.empty();
    }

    /**
     * Creates {@code rowNumber() OVER (...)}.
     *
     * @param windowSpec window specification
     * @return window function expression
     */
    public static WindowFunctionExpression<Long> rowNumber(WindowSpec windowSpec) {
        return Expressions.rowNumber(windowSpec);
    }

    /**
     * Creates an EXPLAIN request wrapper around a query.
     *
     * @param type explain mode such as {@code PLAN} or {@code PIPELINE}
     * @param query query to explain
     * @return explain query wrapper
     */
    public static ExplainQuery explain(ExplainType type, Query query) {
        return new ExplainQuery(type, query);
    }

    /**
     * Creates the {@code max_threads} setting.
     *
     * @param value positive thread count limit
     * @return setting object
     * @throws IllegalArgumentException when {@code value <= 0}
     */
    public static Setting maxThreads(int value) {
        if (value <= 0) {
            throw new IllegalArgumentException("max_threads must be positive");
        }
        return Setting.of("max_threads", value);
    }

    /**
     * Creates the {@code max_memory_usage} setting.
     *
     * @param bytes positive byte limit
     * @return setting object
     * @throws IllegalArgumentException when {@code bytes <= 0}
     */
    public static Setting maxMemoryUsage(long bytes) {
        if (bytes <= 0L) {
            throw new IllegalArgumentException("max_memory_usage must be positive");
        }
        return Setting.of("max_memory_usage", bytes);
    }

    /**
     * Creates the {@code use_uncompressed_cache} setting.
     *
     * @param enabled whether ClickHouse should enable the uncompressed cache
     * @return setting object encoded in ClickHouse-friendly numeric form
     */
    public static Setting useUncompressedCache(boolean enabled) {
        return Setting.of("use_uncompressed_cache", enabled ? 1 : 0);
    }

    /**
     * Creates a {@code WITH alias AS (...)} clause.
     *
     * @param alias symbolic name exposed to the outer query
     * @param query subquery bound to the alias
     * @return with clause
     */
    public static WithClause with(String alias, Query query) {
        return WithClause.of(alias, query);
    }

    /**
     * Renders a query to SQL only.
     *
     * <p>Use the renderer directly if you also need the parameter list.
     *
     * @param query query to render
     * @return SQL string with placeholders
     */
    public static String render(Query query) {
        return new ClickHouseRenderer().render(query).sql();
    }

    /**
     * Renders a query to SQL using explicit render options.
     *
     * @param query query to render
     * @param options render options
     * @return SQL string
     */
    public static String render(Query query, RenderOptions options) {
        return new ClickHouseRenderer().render(query, options).sql();
    }

    /**
     * Validates a query before rendering and returns SQL only.
     *
     * @param query query to validate and render
     * @return SQL string with placeholders
     * @throws QueryValidationException when semantic validation fails
     */
    public static String renderValidated(Query query) {
        return new ClickHouseRenderer().renderValidated(query).sql();
    }

    /**
     * Validates a query before rendering it with explicit render options.
     *
     * @param query query to validate and render
     * @param options render options
     * @return SQL string
     * @throws QueryValidationException when semantic validation fails
     */
    public static String renderValidated(Query query, RenderOptions options) {
        return new ClickHouseRenderer().renderValidated(query, options).sql();
    }

    /**
     * Validates a query before rendering and returns SQL plus parameters.
     *
     * @param query query to validate and render
     * @return rendered query snapshot
     * @throws QueryValidationException when semantic validation fails
     */
    public static RenderedQuery renderValidatedQuery(Query query) {
        return new ClickHouseRenderer().renderValidated(query);
    }

    /**
     * Validates a query before rendering it with explicit render options.
     *
     * @param query query to validate and render
     * @param options render options
     * @return rendered query snapshot
     * @throws QueryValidationException when semantic validation fails
     */
    public static RenderedQuery renderValidatedQuery(Query query, RenderOptions options) {
        return new ClickHouseRenderer().renderValidated(query, options);
    }

    /**
     * Combines conditions with {@code AND}, skipping {@code null} inputs.
     *
     * @param expressions boolean expressions, optionally containing {@code null}
     * @return combined expression, a single expression, or {@code null} when no inputs remain
     */
    public static Expression<Boolean> allOf(Expression<Boolean>... expressions) {
        return combineConditions(true, expressions);
    }

    /**
     * Combines conditions with {@code OR}, skipping {@code null} inputs.
     *
     * @param expressions boolean expressions, optionally containing {@code null}
     * @return combined expression, a single expression, or {@code null} when no inputs remain
     */
    public static Expression<Boolean> anyOf(Expression<Boolean>... expressions) {
        return combineConditions(false, expressions);
    }

    /**
     * Renders an explain query to SQL only.
     *
     * @param explainQuery explain request
     * @return SQL string with placeholders
     */
    public static String render(ExplainQuery explainQuery) {
        return new ExplainRenderer().render(explainQuery).sql();
    }

    /**
     * Runs semantic validation rules against a built query.
     *
     * @param query query to validate
     * @return validation result containing zero or more semantic errors
     */
    public static ValidationResult analyze(Query query) {
        return new SemanticAnalyzer().validate(query);
    }

    /**
     * Validates a query and throws a structured exception on the first invalid result set.
     *
     * <p>This is useful at integration boundaries where callers want fail-fast behavior instead of
     * manually inspecting a {@link ValidationResult}.
     *
     * @param query query to validate
     * @return the same query when validation succeeds
     * @throws QueryValidationException when semantic validation fails
     */
    public static Query validateOrThrow(Query query) {
        ValidationResult validationResult = analyze(query);
        validationResult.throwIfInvalid();
        return query;
    }

    /**
     * Analyzes raw EXPLAIN output into a structured summary.
     *
     * @param type explain mode used to produce the raw output
     * @param rawExplainOutput raw explain text returned by ClickHouse
     * @return parsed and summarized explain result
     */
    public static ExplainResult analyze(ExplainType type, String rawExplainOutput) {
        return new ExplainAnalyzer().analyze(type, rawExplainOutput);
    }

    /**
     * First DSL step after {@code select(...)}.
     */
    public interface SelectStep {
        /**
         * Adds one or more CTE clauses before the main {@code FROM}.
         *
         * @param clauses CTE clauses
         * @return next query-building step
         */
        QueryStep with(WithClause... clauses);

        /**
         * Declares the main source table.
         *
         * @param table source table
         * @return next query-building step
         */
        QueryStep from(Table table);
    }

    /**
     * Main mutable builder surface after {@code FROM} has been declared.
     */
    public interface QueryStep extends BuildStep {
        /**
         * Replaces the main source table.
         *
         * @param table source table
         * @return current query step
         */
        QueryStep from(Table table);

        /**
         * Adds one or more CTE clauses.
         *
         * @param clauses CTE clauses
         * @return current query step
         */
        QueryStep with(WithClause... clauses);

        /**
         * Adds a ClickHouse {@code PREWHERE} predicate.
         *
         * @param expression boolean predicate
         * @return current query step
         */
        QueryStep prewhere(Expression<Boolean> expression);

        /**
         * Adds a ClickHouse {@code PREWHERE} predicate when the expression is not {@code null}.
         *
         * @param expression optional boolean predicate
         * @return current query step
         */
        default QueryStep prewhereIfPresent(Expression<Boolean> expression) {
            return expression == null ? this : prewhere(expression);
        }

        /**
         * Adds a {@code WHERE} predicate.
         *
         * @param expression boolean predicate
         * @return current query step
         */
        QueryStep where(Expression<Boolean> expression);

        /**
         * Adds a {@code WHERE} predicate when the expression is not {@code null}.
         *
         * @param expression optional boolean predicate
         * @return current query step
         */
        default QueryStep whereIfPresent(Expression<Boolean> expression) {
            return expression == null ? this : where(expression);
        }

        /**
         * Starts an {@code INNER JOIN}.
         *
         * @param table joined table
         * @return join completion step
         */
        JoinOnStep innerJoin(Table table);

        /**
         * Starts a {@code LEFT JOIN}.
         *
         * @param table joined table
         * @return join completion step
         */
        JoinOnStep leftJoin(Table table);

        /**
         * Adds one or more {@code ARRAY JOIN} expressions.
         *
         * @param expressions array-typed expressions
         * @return current query step
         */
        QueryStep arrayJoin(Expression<?>... expressions);

        /**
         * Adds a {@code SAMPLE} ratio.
         *
         * @param ratio ratio in {@code (0, 1]}
         * @return current query step
         */
        QueryStep sample(double ratio);

        /**
         * Adds {@code GROUP BY} expressions.
         *
         * @param expressions grouping expressions
         * @return grouped query step
         */
        GroupedQueryStep groupBy(Expression<?>... expressions);

        /**
         * Adds {@code ORDER BY} items.
         *
         * @param sorts sort descriptors
         * @return current query step
         */
        QueryStep orderBy(Sort... sorts);

        /**
         * Adds a {@code LIMIT}.
         *
         * @param limit positive row limit
         * @return current query step
         */
        QueryStep limit(int limit);

        /**
         * Adds ClickHouse {@code SETTINGS}.
         *
         * @param settings settings to append
         * @return current query step
         */
        QueryStep settings(Setting... settings);

        /**
         * Adds a {@code UNION} branch.
         *
         * @param query right-hand query
         * @return current query step
         */
        QueryStep union(Query query);

        /**
         * Adds a {@code UNION ALL} branch.
         *
         * @param query right-hand query
         * @return current query step
         */
        QueryStep unionAll(Query query);
    }

    /**
     * Specialized step returned after {@code groupBy(...)} so {@code having(...)} can be exposed in
     * a more discoverable place.
     */
    public interface GroupedQueryStep extends QueryStep {
        /**
         * Adds a {@code HAVING} predicate.
         *
         * @param expression boolean predicate
         * @return grouped query step
         */
        GroupedQueryStep having(Expression<Boolean> expression);

        /**
         * Adds a {@code HAVING} predicate when the expression is not {@code null}.
         *
         * @param expression optional boolean predicate
         * @return grouped query step
         */
        default GroupedQueryStep havingIfPresent(Expression<Boolean> expression) {
            return expression == null ? this : having(expression);
        }
    }

    /**
     * Intermediate step used to force {@code JOIN ... ON ...} completion.
     */
    public interface JoinOnStep {
        /**
         * Completes a pending join by providing the join keys.
         *
         * @param left left join key
         * @param right right join key
         * @param <T> join key type
         * @return current query step
         */
        <T> QueryStep on(Expression<T> left, Expression<T> right);
    }

    /**
     * Terminal builder step.
     */
    public interface BuildStep {
        /**
         * Materializes the immutable query object.
         *
         * @return built query
         */
        Query build();
    }

    private static Expression<Boolean> combineConditions(boolean and, Expression<Boolean>[] expressions) {
        Objects.requireNonNull(expressions, "expressions");
        Expression<Boolean> combined = null;
        for (Expression<Boolean> expression : expressions) {
            if (expression == null) {
                continue;
            }
            if (combined == null) {
                combined = expression;
                continue;
            }
            combined = and ? Expressions.and(combined, expression) : Expressions.or(combined, expression);
        }
        return combined;
    }
}
