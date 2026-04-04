package io.github.heonny.clickhousedsl.explain;

import io.github.heonny.clickhousedsl.model.Query;
import java.util.Objects;

/**
 * Wrapper that pairs a query with a requested EXPLAIN mode.
 */
public final class ExplainQuery {

    private final ExplainType type;
    private final Query query;

    /**
     * Creates an explain query wrapper.
     *
     * @param type explain mode
     * @param query wrapped query
     */
    public ExplainQuery(ExplainType type, Query query) {
        this.type = Objects.requireNonNull(type, "type");
        this.query = Objects.requireNonNull(query, "query");
    }

    /**
     * Returns the requested explain mode.
     *
     * @return explain mode
     */
    public ExplainType type() {
        return type;
    }

    /**
     * Returns the query to explain.
     *
     * @return wrapped query
     */
    public Query query() {
        return query;
    }
}
