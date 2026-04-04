package io.github.chang.clickhousedsl.model;

public interface Expression<T> {

    Class<T> type();

    String render(RenderContext context);

    default boolean aggregate() {
        return false;
    }

    default AliasedExpression<T> as(String alias) {
        return new AliasedExpression<>(this, Identifier.of(alias));
    }

    default ComparisonExpression eq(T value) {
        return eq(Expressions.param(value, type()));
    }

    default ComparisonExpression eq(Expression<T> other) {
        return new ComparisonExpression(this, ComparisonOperator.EQ, other);
    }

    default ComparisonExpression gt(T value) {
        return gt(Expressions.param(value, type()));
    }

    default ComparisonExpression gt(Expression<T> other) {
        return new ComparisonExpression(this, ComparisonOperator.GT, other);
    }

    default ComparisonExpression lt(T value) {
        return lt(Expressions.param(value, type()));
    }

    default ComparisonExpression lt(Expression<T> other) {
        return new ComparisonExpression(this, ComparisonOperator.LT, other);
    }

    default ComparisonExpression gte(T value) {
        return gte(Expressions.param(value, type()));
    }

    default ComparisonExpression gte(Expression<T> other) {
        return new ComparisonExpression(this, ComparisonOperator.GTE, other);
    }

    default ComparisonExpression lte(T value) {
        return lte(Expressions.param(value, type()));
    }

    default ComparisonExpression lte(Expression<T> other) {
        return new ComparisonExpression(this, ComparisonOperator.LTE, other);
    }
}
