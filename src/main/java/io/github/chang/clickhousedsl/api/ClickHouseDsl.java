package io.github.chang.clickhousedsl.api;

import io.github.chang.clickhousedsl.model.Expression;
import io.github.chang.clickhousedsl.model.Expressions;
import io.github.chang.clickhousedsl.explain.ExplainAnalyzer;
import io.github.chang.clickhousedsl.explain.ExplainQuery;
import io.github.chang.clickhousedsl.explain.ExplainRenderer;
import io.github.chang.clickhousedsl.explain.ExplainResult;
import io.github.chang.clickhousedsl.explain.ExplainType;
import io.github.chang.clickhousedsl.model.AggregateExpression;
import io.github.chang.clickhousedsl.model.AggregateStateExpression;
import io.github.chang.clickhousedsl.model.BinaryArithmeticExpression;
import io.github.chang.clickhousedsl.model.Query;
import io.github.chang.clickhousedsl.model.Setting;
import io.github.chang.clickhousedsl.model.Sort;
import io.github.chang.clickhousedsl.model.Table;
import io.github.chang.clickhousedsl.model.WindowFunctionExpression;
import io.github.chang.clickhousedsl.model.WindowSpec;
import io.github.chang.clickhousedsl.model.WithClause;
import io.github.chang.clickhousedsl.render.ClickHouseRenderer;
import io.github.chang.clickhousedsl.validate.SemanticAnalyzer;
import io.github.chang.clickhousedsl.validate.ValidationResult;

public final class ClickHouseDsl {

    private ClickHouseDsl() {
    }

    public static SelectStep select(Expression<?>... selections) {
        return new QueryBuilder(selections);
    }

    public static <T> Expression<T> param(T value, Class<T> type) {
        return Expressions.param(value, type);
    }

    public static <T> Expression<T> literal(T value, Class<T> type) {
        return Expressions.literal(value, type);
    }

    public static AggregateExpression<Long> count() {
        return Expressions.count();
    }

    public static <N extends Number> AggregateStateExpression<N> sumState(Expression<N> expression) {
        return Expressions.sumState(expression);
    }

    public static <N extends Number> AggregateExpression<N> sumMerge(AggregateStateExpression<N> expression) {
        return Expressions.sumMerge(expression);
    }

    public static <N extends Number> AggregateExpression<N> sumMerge(io.github.chang.clickhousedsl.model.Expression<io.github.chang.clickhousedsl.model.AggregateState<N>> expression, Class<N> valueType) {
        return Expressions.sumMerge(expression, valueType);
    }

    public static AggregateExpression<Long> countMerge(Expression<io.github.chang.clickhousedsl.model.AggregateState<Long>> expression) {
        return Expressions.countMerge(expression);
    }

    public static AggregateExpression<Long> countIfMerge(Expression<io.github.chang.clickhousedsl.model.AggregateState<Long>> expression) {
        return Expressions.countIfMerge(expression);
    }

    public static AggregateExpression<Long> uniqMerge(Expression<io.github.chang.clickhousedsl.model.AggregateState<Long>> expression) {
        return Expressions.uniqMerge(expression);
    }

    public static <T> io.github.chang.clickhousedsl.model.FunctionExpression<T> function(String name, Class<T> type, Expression<?>... arguments) {
        return Expressions.function(name, type, arguments);
    }

    public static <T> io.github.chang.clickhousedsl.model.FunctionExpression<T> aggregateFunction(String name, Class<T> type, Expression<?>... arguments) {
        return Expressions.aggregateFunction(name, type, arguments);
    }

    public static BinaryArithmeticExpression<Double> divide(Expression<?> left, Expression<?> right) {
        return Expressions.divide(left, right);
    }

    public static <T> io.github.chang.clickhousedsl.model.ReferenceExpression<T> ref(String identifier, Class<T> type) {
        return Expressions.ref(identifier, type);
    }

    public static WindowSpec window() {
        return WindowSpec.empty();
    }

    public static WindowFunctionExpression<Long> rowNumber(WindowSpec windowSpec) {
        return Expressions.rowNumber(windowSpec);
    }

    public static ExplainQuery explain(ExplainType type, Query query) {
        return new ExplainQuery(type, query);
    }

    public static Setting maxThreads(int value) {
        return Setting.of("max_threads", value);
    }

    public static Setting useUncompressedCache(boolean enabled) {
        return Setting.of("use_uncompressed_cache", enabled ? 1 : 0);
    }

    public static WithClause with(String alias, Query query) {
        return WithClause.of(alias, query);
    }

    public static String render(Query query) {
        return new ClickHouseRenderer().render(query).sql();
    }

    public static String render(ExplainQuery explainQuery) {
        return new ExplainRenderer().render(explainQuery).sql();
    }

    public static ValidationResult analyze(Query query) {
        return new SemanticAnalyzer().validate(query);
    }

    public static ExplainResult analyze(ExplainType type, String rawExplainOutput) {
        return new ExplainAnalyzer().analyze(type, rawExplainOutput);
    }

    public interface SelectStep {
        QueryStep with(WithClause... clauses);
        QueryStep from(Table table);
    }

    public interface QueryStep extends BuildStep {
        QueryStep from(Table table);
        QueryStep with(WithClause... clauses);
        QueryStep prewhere(Expression<Boolean> expression);
        QueryStep where(Expression<Boolean> expression);
        JoinOnStep innerJoin(Table table);
        JoinOnStep leftJoin(Table table);
        QueryStep arrayJoin(Expression<?>... expressions);
        QueryStep sample(double ratio);
        GroupedQueryStep groupBy(Expression<?>... expressions);
        QueryStep orderBy(Sort... sorts);
        QueryStep limit(int limit);
        QueryStep settings(Setting... settings);
        QueryStep union(Query query);
        QueryStep unionAll(Query query);
    }

    public interface GroupedQueryStep extends QueryStep {
        GroupedQueryStep having(Expression<Boolean> expression);
    }

    public interface JoinOnStep {
        <T> QueryStep on(Expression<T> left, Expression<T> right);
    }

    public interface BuildStep {
        Query build();
    }
}
