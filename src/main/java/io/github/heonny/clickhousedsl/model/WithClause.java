package io.github.heonny.clickhousedsl.model;

import java.util.Objects;

/**
 * Common table expression entry for a {@code WITH alias AS (...)} clause.
 */
public final class WithClause {

    private final Identifier alias;
    private final Query query;

    /**
     * Creates a with clause.
     *
     * @param alias CTE alias
     * @param query bound query
     */
    public WithClause(Identifier alias, Query query) {
        this.alias = Objects.requireNonNull(alias, "alias");
        this.query = Objects.requireNonNull(query, "query");
    }

    /**
     * Convenience factory from a raw alias string.
     *
     * @param alias CTE alias
     * @param query bound query
     * @return with clause
     */
    public static WithClause of(String alias, Query query) {
        return new WithClause(Identifier.of(alias), query);
    }

    /**
     * Returns the alias exposed by the CTE.
     *
     * @return alias identifier
     */
    public Identifier alias() {
        return alias;
    }

    /**
     * Returns the query bound to the alias.
     *
     * @return CTE query
     */
    public Query query() {
        return query;
    }
}
