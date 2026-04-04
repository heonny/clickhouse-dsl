package io.github.heonny.clickhousedsl.explain;

import io.github.heonny.clickhousedsl.model.RenderedQuery;
import io.github.heonny.clickhousedsl.render.ClickHouseRenderer;

/**
 * Renderer for {@link ExplainQuery}.
 */
public final class ExplainRenderer {

    private final ClickHouseRenderer queryRenderer = new ClickHouseRenderer();

    /**
     * Renders an explain wrapper to SQL plus parameters.
     *
     * @param explainQuery explain request
     * @return rendered explain query
     */
    public RenderedQuery render(ExplainQuery explainQuery) {
        RenderedQuery renderedQuery = queryRenderer.render(explainQuery.query());
        return new RenderedQuery(
            "EXPLAIN " + explainQuery.type().sql() + " " + renderedQuery.sql(),
            renderedQuery.parameters()
        );
    }
}
