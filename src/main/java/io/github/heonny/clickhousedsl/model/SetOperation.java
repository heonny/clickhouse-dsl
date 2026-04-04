package io.github.heonny.clickhousedsl.model;

import java.util.Objects;

/**
 * Immutable set operation descriptor such as {@code UNION} or {@code UNION ALL}.
 */
public final class SetOperation {

    private final UnionType type;
    private final Query query;

    /**
     * Creates a set operation.
     *
     * @param type set operation type
     * @param query right-hand query
     */
    public SetOperation(UnionType type, Query query) {
        this.type = Objects.requireNonNull(type, "type");
        this.query = Objects.requireNonNull(query, "query");
    }

    /**
     * Returns the set operation type.
     *
     * @return set operation type
     */
    public UnionType type() {
        return type;
    }

    /**
     * Returns the right-hand query.
     *
     * @return right-hand query
     */
    public Query query() {
        return query;
    }
}
