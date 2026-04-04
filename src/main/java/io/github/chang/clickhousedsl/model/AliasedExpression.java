package io.github.chang.clickhousedsl.model;

import java.util.Objects;

public final class AliasedExpression<T> implements Expression<T> {

    private final Expression<T> delegate;
    private final Identifier alias;

    public AliasedExpression(Expression<T> delegate, Identifier alias) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.alias = Objects.requireNonNull(alias, "alias");
    }

    public Expression<T> delegate() {
        return delegate;
    }

    public Identifier alias() {
        return alias;
    }

    @Override
    public Class<T> type() {
        return delegate.type();
    }

    @Override
    public String render(RenderContext context) {
        return delegate.render(context) + " AS " + alias.sql();
    }

    @Override
    public boolean aggregate() {
        return delegate.aggregate();
    }
}
