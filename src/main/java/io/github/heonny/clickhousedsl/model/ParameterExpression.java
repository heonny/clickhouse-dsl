package io.github.heonny.clickhousedsl.model;

import java.util.Objects;

/**
 * Placeholder-backed runtime parameter.
 *
 * @param <T> parameter type
 */
public final class ParameterExpression<T> implements Expression<T> {

    private final T value;
    private final Class<T> type;

    ParameterExpression(T value, Class<T> type) {
        this.value = value;
        this.type = Objects.requireNonNull(type, "type");
    }

    /**
     * Returns the stored runtime value.
     *
     * @return parameter value
     */
    public T value() {
        return value;
    }

    @Override
    public Class<T> type() {
        return type;
    }

    @Override
    public String render(RenderContext context) {
        return context.addParameter(value);
    }
}
