package io.github.heonny.clickhousedsl.model;

import java.util.Objects;

public final class SetOperation {

    private final UnionType type;
    private final Query query;

    public SetOperation(UnionType type, Query query) {
        this.type = Objects.requireNonNull(type, "type");
        this.query = Objects.requireNonNull(query, "query");
    }

    public UnionType type() {
        return type;
    }

    public Query query() {
        return query;
    }
}
