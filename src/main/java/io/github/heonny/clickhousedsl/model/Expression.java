package io.github.heonny.clickhousedsl.model;

/**
 * Base contract for all typed nodes that can appear inside a query.
 *
 * <p>An expression knows:
 *
 * <ul>
 *   <li>its Java-visible type
 *   <li>how to render itself into SQL using a shared render context
 *   <li>whether it should be treated as an aggregate expression
 * </ul>
 *
 * <p>Default helper methods provide the most common comparison operators while preserving type
 * relationships in Java code.
 *
 * @param <T> Java type represented by the expression
 */
public interface Expression<T> {

    /**
     * Returns the Java type associated with this expression.
     *
     * @return expression type
     */
    Class<T> type();

    /**
     * Renders the expression into SQL using the supplied context.
     *
     * <p>The render context owns placeholder ordering and collected parameter values.
     *
     * @param context shared render context
     * @return SQL fragment
     */
    String render(RenderContext context);

    /**
     * Indicates whether the expression behaves as an aggregate expression for semantic validation.
     *
     * @return {@code true} when the expression is aggregate-aware
     */
    default boolean aggregate() {
        return false;
    }

    /**
     * Attaches a projection alias.
     *
     * @param alias SQL alias
     * @return aliased expression
     */
    default AliasedExpression<T> as(String alias) {
        return new AliasedExpression<>(this, Identifier.of(alias));
    }

    /**
     * Creates {@code this = ?}.
     *
     * @param value runtime value to bind
     * @return comparison expression
     */
    default ComparisonExpression eq(T value) {
        return eq(Expressions.param(value, type()));
    }

    /**
     * Creates {@code this = other}.
     *
     * @param other right-hand expression
     * @return comparison expression
     */
    default ComparisonExpression eq(Expression<T> other) {
        return new ComparisonExpression(this, ComparisonOperator.EQ, other);
    }

    /**
     * Creates {@code this > ?}.
     *
     * @param value runtime value to bind
     * @return comparison expression
     */
    default ComparisonExpression gt(T value) {
        return gt(Expressions.param(value, type()));
    }

    /**
     * Creates {@code this > other}.
     *
     * @param other right-hand expression
     * @return comparison expression
     */
    default ComparisonExpression gt(Expression<T> other) {
        return new ComparisonExpression(this, ComparisonOperator.GT, other);
    }

    /**
     * Creates {@code this < ?}.
     *
     * @param value runtime value to bind
     * @return comparison expression
     */
    default ComparisonExpression lt(T value) {
        return lt(Expressions.param(value, type()));
    }

    /**
     * Creates {@code this < other}.
     *
     * @param other right-hand expression
     * @return comparison expression
     */
    default ComparisonExpression lt(Expression<T> other) {
        return new ComparisonExpression(this, ComparisonOperator.LT, other);
    }

    /**
     * Creates {@code this >= ?}.
     *
     * @param value runtime value to bind
     * @return comparison expression
     */
    default ComparisonExpression gte(T value) {
        return gte(Expressions.param(value, type()));
    }

    /**
     * Creates {@code this >= other}.
     *
     * @param other right-hand expression
     * @return comparison expression
     */
    default ComparisonExpression gte(Expression<T> other) {
        return new ComparisonExpression(this, ComparisonOperator.GTE, other);
    }

    /**
     * Creates {@code this <= ?}.
     *
     * @param value runtime value to bind
     * @return comparison expression
     */
    default ComparisonExpression lte(T value) {
        return lte(Expressions.param(value, type()));
    }

    /**
     * Creates {@code this <= other}.
     *
     * @param other right-hand expression
     * @return comparison expression
     */
    default ComparisonExpression lte(Expression<T> other) {
        return new ComparisonExpression(this, ComparisonOperator.LTE, other);
    }
}
