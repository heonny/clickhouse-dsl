package io.github.heonny.clickhousedsl.model;

/**
 * Supported SQL set operation kinds.
 */
public enum UnionType {
    /** Distinct union. */
    DISTINCT("UNION"),
    /** Bag union. */
    ALL("UNION ALL");

    private final String sql;

    UnionType(String sql) {
        this.sql = sql;
    }

    /**
     * Returns the SQL fragment used when rendering the set operation.
     *
     * @return SQL fragment
     */
    public String sql() {
        return sql;
    }
}
