package io.github.chang.clickhousedsl.model;

import java.util.Objects;

public final class BinaryArithmeticExpression<T> implements Expression<T> {

    private final Expression<?> left;
    private final String operator;
    private final Expression<?> right;
    private final Class<T> type;
    private final boolean aggregate;

    public BinaryArithmeticExpression(Expression<?> left, String operator, Expression<?> right, Class<T> type, boolean aggregate) {
        this.left = Objects.requireNonNull(left, "left");
        this.operator = Objects.requireNonNull(operator, "operator");
        this.right = Objects.requireNonNull(right, "right");
        this.type = Objects.requireNonNull(type, "type");
        this.aggregate = aggregate;
    }

    @Override
    public Class<T> type() {
        return type;
    }

    @Override
    public String render(RenderContext context) {
        return left.render(context) + " " + operator + " " + right.render(context);
    }

    @Override
    public boolean aggregate() {
        return aggregate;
    }
}
