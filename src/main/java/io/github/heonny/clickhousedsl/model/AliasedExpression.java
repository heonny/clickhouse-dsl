package io.github.heonny.clickhousedsl.model;

import java.util.Objects;

/**
 * Projection wrapper that renders an expression with an alias.
 *
 * @param <T> delegate expression type
 */
public final class AliasedExpression<T> implements Expression<T> {

    private final Expression<T> delegate;
    private final Identifier alias;

    /**
     * Creates an aliased expression.
     *
     * @param delegate underlying expression
     * @param alias SQL alias
     */
    public AliasedExpression(Expression<T> delegate, Identifier alias) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.alias = Objects.requireNonNull(alias, "alias");
    }

    /**
     * Returns the wrapped expression.
     *
     * @return delegate expression
     */
    public Expression<T> delegate() {
        return delegate;
    }

    /**
     * Returns the SQL alias.
     *
     * @return alias identifier
     */
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
