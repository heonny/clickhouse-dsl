package io.github.chang.clickhousedsl.api;

import io.github.chang.clickhousedsl.model.Expression;
import io.github.chang.clickhousedsl.model.Expressions;
import io.github.chang.clickhousedsl.model.AggregateExpression;
import io.github.chang.clickhousedsl.model.Query;
import io.github.chang.clickhousedsl.model.Setting;
import io.github.chang.clickhousedsl.model.Sort;
import io.github.chang.clickhousedsl.model.Table;
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

    public static AggregateExpression<Long> count() {
        return Expressions.count();
    }

    public static Setting maxThreads(int value) {
        return Setting.of("max_threads", value);
    }

    public static Setting useUncompressedCache(boolean enabled) {
        return Setting.of("use_uncompressed_cache", enabled ? 1 : 0);
    }

    public static String render(Query query) {
        return new ClickHouseRenderer().render(query).sql();
    }

    public static ValidationResult analyze(Query query) {
        return new SemanticAnalyzer().validate(query);
    }

    public interface SelectStep {
        QueryStep from(Table table);
    }

    public interface QueryStep extends BuildStep {
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
