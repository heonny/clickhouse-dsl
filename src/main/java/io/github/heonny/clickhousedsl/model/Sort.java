package io.github.heonny.clickhousedsl.model;

import java.util.Objects;

public final class Sort {

    private final Expression<?> expression;
    private final SortDirection direction;

    public Sort(Expression<?> expression, SortDirection direction) {
        this.expression = Objects.requireNonNull(expression, "expression");
        this.direction = Objects.requireNonNull(direction, "direction");
    }

    public Expression<?> expression() {
        return expression;
    }

    public SortDirection direction() {
        return direction;
    }
}
