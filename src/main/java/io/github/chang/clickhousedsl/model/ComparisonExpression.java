package io.github.chang.clickhousedsl.model;

import java.util.Objects;

public final class ComparisonExpression implements Expression<Boolean> {

    private final Expression<?> left;
    private final ComparisonOperator operator;
    private final Expression<?> right;

    ComparisonExpression(Expression<?> left, ComparisonOperator operator, Expression<?> right) {
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
        return left.render(context) + " " + operator.sql() + " " + right.render(context);
    }
}
