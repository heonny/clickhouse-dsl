package io.github.heonny.clickhousedsl.model;

/**
 * Join types currently modeled by the DSL.
 */
public enum JoinType {
    /** Inner join. */
    INNER("INNER JOIN"),
    /** Left outer join. */
    LEFT("LEFT JOIN");

    private final String sql;

    JoinType(String sql) {
        this.sql = sql;
    }

    /**
     * Returns the SQL token for this join type.
     *
     * @return SQL fragment
     */
    public String sql() {
        return sql;
    }
}
