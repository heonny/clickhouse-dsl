package io.github.heonny.clickhousedsl.model;

import java.util.Objects;

/**
 * Reference to an already-named symbol such as an alias.
 *
 * @param <T> reference type
 */
public final class ReferenceExpression<T> implements Expression<T> {

    private final Identifier identifier;
    private final Class<T> type;

    /**
     * Creates a reference expression.
     *
     * @param identifier identifier or alias
     * @param type reference type
     */
    public ReferenceExpression(String identifier, Class<T> type) {
        this.identifier = Identifier.of(identifier);
        this.type = Objects.requireNonNull(type, "type");
    }

    @Override
    public Class<T> type() {
        return type;
    }

    @Override
    public String render(RenderContext context) {
        return identifier.sql();
    }

    /**
     * Creates an ascending sort using this reference.
     *
     * @return ascending sort descriptor
     */
    public Sort asc() {
        return new Sort(this, SortDirection.ASC);
    }

    /**
     * Creates a descending sort using this reference.
     *
     * @return descending sort descriptor
     */
    public Sort desc() {
        return new Sort(this, SortDirection.DESC);
    }
}
