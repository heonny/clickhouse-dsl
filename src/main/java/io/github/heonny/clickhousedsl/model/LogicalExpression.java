package io.github.heonny.clickhousedsl.model;

import java.util.Objects;

public final class LogicalExpression implements Expression<Boolean> {

    private final Expression<Boolean> left;
    private final LogicalOperator operator;
    private final Expression<Boolean> right;

    LogicalExpression(Expression<Boolean> left, LogicalOperator operator, Expression<Boolean> right) {
        this.left = Objects.requireNonNull(left, "left");
        this.operator = Objects.requireNonNull(operator, "operator");
        this.right = Objects.requireNonNull(right, "right");
    }

    public LogicalExpression and(Expression<Boolean> other) {
        return new LogicalExpression(this, LogicalOperator.AND, other);
    }

    public LogicalExpression or(Expression<Boolean> other) {
        return new LogicalExpression(this, LogicalOperator.OR, other);
    }

    @Override
    public Class<Boolean> type() {
        return Boolean.class;
    }

    @Override
    public String render(RenderContext context) {
        return "(" + left.render(context) + " " + operator.name() + " " + right.render(context) + ")";
    }
}
