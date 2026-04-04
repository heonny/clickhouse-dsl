package io.github.heonny.clickhousedsl.model;

import java.util.Objects;
import java.util.function.Function;

/**
 * Expression representing {@code <function> OVER (...)}.
 *
 * @param <T> result type
 */
public final class WindowFunctionExpression<T> implements Expression<T> {

    private final Function<RenderContext, String> baseRenderer;
    private final WindowSpec windowSpec;
    private final Class<T> type;

    WindowFunctionExpression(Function<RenderContext, String> baseRenderer, WindowSpec windowSpec, Class<T> type) {
        this.baseRenderer = Objects.requireNonNull(baseRenderer, "baseRenderer");
        this.windowSpec = Objects.requireNonNull(windowSpec, "windowSpec");
        this.type = Objects.requireNonNull(type, "type");
    }

    @Override
    public Class<T> type() {
        return type;
    }

    /**
     * Returns the window specification used for rendering.
     *
     * @return window spec
     */
    public WindowSpec windowSpec() {
        return windowSpec;
    }

    @Override
    public String render(RenderContext context) {
        return baseRenderer.apply(context) + " OVER (" + windowSpec.render(context) + ")";
    }
}
