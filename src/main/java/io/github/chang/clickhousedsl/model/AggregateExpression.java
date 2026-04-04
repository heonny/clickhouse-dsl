package io.github.chang.clickhousedsl.model;

import java.util.Objects;
import java.util.function.Function;

public final class AggregateExpression<T> implements Expression<T> {

    private final Function<RenderContext, String> renderer;
    private final Class<T> type;

    AggregateExpression(String sql, Class<T> type) {
        this(context -> sql, type);
    }

    AggregateExpression(Function<RenderContext, String> renderer, Class<T> type) {
        this.renderer = Objects.requireNonNull(renderer, "renderer");
        this.type = Objects.requireNonNull(type, "type");
    }

    @Override
    public Class<T> type() {
        return type;
    }

    @Override
    public String render(RenderContext context) {
        return renderer.apply(context);
    }

    @Override
    public boolean aggregate() {
        return true;
    }

    public ComparisonExpression eq(Expression<T> other) {
        return new ComparisonExpression(this, ComparisonOperator.EQ, other);
    }

    public ComparisonExpression gt(Expression<T> other) {
        return new ComparisonExpression(this, ComparisonOperator.GT, other);
    }

    public WindowFunctionExpression<T> over(WindowSpec windowSpec) {
        return new WindowFunctionExpression<>(this::render, windowSpec, type);
    }
}
