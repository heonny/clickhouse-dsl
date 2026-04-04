package io.github.heonny.clickhousedsl.model;

/**
 * Comparison operators supported by the DSL.
 */
public enum ComparisonOperator {
    /** Equality operator. */
    EQ("="),
    /** Greater-than operator. */
    GT(">"),
    /** Less-than operator. */
    LT("<"),
    /** Greater-than-or-equal operator. */
    GTE(">="),
    /** Less-than-or-equal operator. */
    LTE("<=");

    private final String sql;

    ComparisonOperator(String sql) {
        this.sql = sql;
    }

    /**
     * Returns the SQL token used for rendering.
     *
     * @return SQL operator
     */
    public String sql() {
        return sql;
    }
}
