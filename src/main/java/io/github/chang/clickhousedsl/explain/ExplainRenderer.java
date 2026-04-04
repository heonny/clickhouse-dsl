package io.github.chang.clickhousedsl.explain;

import io.github.chang.clickhousedsl.model.RenderedQuery;
import io.github.chang.clickhousedsl.render.ClickHouseRenderer;

public final class ExplainRenderer {

    private final ClickHouseRenderer queryRenderer = new ClickHouseRenderer();

    public RenderedQuery render(ExplainQuery explainQuery) {
        RenderedQuery renderedQuery = queryRenderer.render(explainQuery.query());
        return new RenderedQuery(
            "EXPLAIN " + explainQuery.type().sql() + " " + renderedQuery.sql(),
            renderedQuery.parameters()
        );
    }
}
