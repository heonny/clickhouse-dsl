package io.github.heonny.clickhousedsl.model;

import java.util.Objects;

/**
 * Boolean comparison between two expressions.
 */
public final class ComparisonExpression implements Expression<Boolean> {

    private final Expression<?> left;
    private final ComparisonOperator operator;
    private final Expression<?> right;

    ComparisonExpression(Expression<?> left, ComparisonOperator operator, Expression<?> right) {
        this.left = Objects.requireNonNull(left, "left");
        this.operator = Objects.requireNonNull(operator, "operator");
        this.right = Objects.requireNonNull(right, "right");
    }

    /**
     * Combines this comparison with another boolean expression using {@code AND}.
     *
     * @param other right-hand boolean expression
     * @return logical expression
     */
    public LogicalExpression and(Expression<Boolean> other) {
        return new LogicalExpression(this, LogicalOperator.AND, other);
    }

    /**
     * Combines this comparison with another boolean expression using {@code OR}.
     *
     * @param other right-hand boolean expression
     * @return logical expression
     */
    public LogicalExpression or(Expression<Boolean> other) {
        return new LogicalExpression(this, LogicalOperator.OR, other);
    }

    /**
     * Returns the left-hand expression.
     *
     * @return left-hand expression
     */
    public Expression<?> left() {
        return left;
    }

    /**
     * Returns the right-hand expression.
     *
     * @return right-hand expression
     */
    public Expression<?> right() {
        return right;
    }

    @Override
    public Class<Boolean> type() {
        return Boolean.class;
    }

    @Override
    public String render(RenderContext context) {
        return left.render(context) + " " + operator.sql() + " " + right.render(context);
    }
}
