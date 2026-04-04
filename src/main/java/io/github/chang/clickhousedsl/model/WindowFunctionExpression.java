package io.github.chang.clickhousedsl.model;

import java.util.Objects;
import java.util.function.Function;

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

    public WindowSpec windowSpec() {
        return windowSpec;
    }

    @Override
    public String render(RenderContext context) {
        return baseRenderer.apply(context) + " OVER (" + windowSpec.render(context) + ")";
    }
}
