package io.github.heonny.clickhousedsl.explain;

import io.github.heonny.clickhousedsl.model.Query;
import java.util.Objects;

public final class ExplainQuery {

    private final ExplainType type;
    private final Query query;

    public ExplainQuery(ExplainType type, Query query) {
        this.type = Objects.requireNonNull(type, "type");
        this.query = Objects.requireNonNull(query, "query");
    }

    public ExplainType type() {
        return type;
    }

    public Query query() {
        return query;
    }
}
