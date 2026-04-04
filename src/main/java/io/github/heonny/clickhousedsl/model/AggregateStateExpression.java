package io.github.heonny.clickhousedsl.model;

import java.util.Objects;
import java.util.function.Function;

/**
 * Expression representing a ClickHouse aggregate state function such as {@code sumState(...)}.
 *
 * @param <T> merged value type associated with the state
 */
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

    /**
     * Returns the merged value type associated with this state.
     *
     * @return merged value type
     */
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
