package io.github.chang.clickhousedsl.model;

import java.util.Objects;

public final class WithClause {

    private final Identifier alias;
    private final Query query;

    public WithClause(Identifier alias, Query query) {
        this.alias = Objects.requireNonNull(alias, "alias");
        this.query = Objects.requireNonNull(query, "query");
    }

    public static WithClause of(String alias, Query query) {
        return new WithClause(Identifier.of(alias), query);
    }

    public Identifier alias() {
        return alias;
    }

    public Query query() {
        return query;
    }
}
