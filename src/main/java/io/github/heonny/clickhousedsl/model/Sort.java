package io.github.heonny.clickhousedsl.model;

import java.util.Objects;

/**
 * Sort item used in {@code ORDER BY} or window ordering.
 */
public final class Sort {

    private final Expression<?> expression;
    private final SortDirection direction;

    /**
     * Creates a sort descriptor.
     *
     * @param expression sorted expression
     * @param direction sort direction
     */
    public Sort(Expression<?> expression, SortDirection direction) {
        this.expression = Objects.requireNonNull(expression, "expression");
        this.direction = Objects.requireNonNull(direction, "direction");
    }

    /**
     * Returns the sorted expression.
     *
     * @return sorted expression
     */
    public Expression<?> expression() {
        return expression;
    }

    /**
     * Returns the sort direction.
     *
     * @return sort direction
     */
    public SortDirection direction() {
        return direction;
    }
}
