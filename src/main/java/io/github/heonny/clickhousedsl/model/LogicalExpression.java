package io.github.heonny.clickhousedsl.model;

import java.util.Objects;

/**
 * Boolean composition of two boolean expressions.
 */
public final class LogicalExpression implements Expression<Boolean> {

    private final Expression<Boolean> left;
    private final LogicalOperator operator;
    private final Expression<Boolean> right;

    LogicalExpression(Expression<Boolean> left, LogicalOperator operator, Expression<Boolean> right) {
        this.left = Objects.requireNonNull(left, "left");
        this.operator = Objects.requireNonNull(operator, "operator");
        this.right = Objects.requireNonNull(right, "right");
    }

    /**
     * Combines this expression with another boolean expression using {@code AND}.
     *
     * @param other right-hand boolean expression
     * @return logical expression
     */
    public LogicalExpression and(Expression<Boolean> other) {
        return new LogicalExpression(this, LogicalOperator.AND, other);
    }

    /**
     * Combines this expression with another boolean expression using {@code OR}.
     *
     * @param other right-hand boolean expression
     * @return logical expression
     */
    public LogicalExpression or(Expression<Boolean> other) {
        return new LogicalExpression(this, LogicalOperator.OR, other);
    }

    /**
     * Returns the left-hand boolean expression.
     *
     * @return left-hand boolean expression
     */
    public Expression<Boolean> left() {
        return left;
    }

    /**
     * Returns the right-hand boolean expression.
     *
     * @return right-hand boolean expression
     */
    public Expression<Boolean> right() {
        return right;
    }

    @Override
    public Class<Boolean> type() {
        return Boolean.class;
    }

    @Override
    public String render(RenderContext context) {
        // Parentheses preserve explicit boolean grouping across nested compositions.
        return "(" + left.render(context) + " " + operator.name() + " " + right.render(context) + ")";
    }
}
