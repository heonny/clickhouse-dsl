package io.github.chang.clickhousedsl.model;

import java.util.Objects;
import java.util.function.Function;

public final class AggregateStateExpression<T> implements Expression<AggregateState<T>> {

    private final Function<RenderContext, String> renderer;
    private final Class<T> valueType;

    AggregateStateExpression(Function<RenderContext, String> renderer, Class<T> valueType) {
        this.renderer = Objects.requireNonNull(renderer, "renderer");
        this.valueType = Objects.requireNonNull(valueType, "valueType");
    }

    @Override
    @SuppressWarnings("unchecked")
    public Class<AggregateState<T>> type() {
        return (Class<AggregateState<T>>) (Class<?>) AggregateState.class;
    }

    public Class<T> valueType() {
        return valueType;
    }

    @Override
    public String render(RenderContext context) {
        return renderer.apply(context);
    }

    @Override
    public boolean aggregate() {
        return true;
    }
}
