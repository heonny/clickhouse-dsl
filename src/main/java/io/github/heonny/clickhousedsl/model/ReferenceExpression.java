package io.github.heonny.clickhousedsl.model;

import java.util.Objects;

public final class ReferenceExpression<T> implements Expression<T> {

    private final Identifier identifier;
    private final Class<T> type;

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

    public Sort asc() {
        return new Sort(this, SortDirection.ASC);
    }

    public Sort desc() {
        return new Sort(this, SortDirection.DESC);
    }
}
