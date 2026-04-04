package io.github.heonny.clickhousedsl.model;

import java.util.Objects;

/**
 * Binary arithmetic expression such as division between two expressions.
 *
 * @param <T> result type
 */
public final class BinaryArithmeticExpression<T> implements Expression<T> {

    private final Expression<?> left;
    private final String operator;
    private final Expression<?> right;
    private final Class<T> type;
    private final boolean aggregate;

    /**
     * Creates a binary arithmetic expression.
     *
     * @param left left operand
     * @param operator SQL operator
     * @param right right operand
     * @param type result type
     * @param aggregate whether either side is aggregate-aware
     */
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

    /**
     * Returns the left operand.
     *
     * @return left operand
     */
    public Expression<?> left() {
        return left;
    }

    /**
     * Returns the right operand.
     *
     * @return right operand
     */
    public Expression<?> right() {
        return right;
    }
}
