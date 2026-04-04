package io.github.heonny.clickhousedsl.explain;

/**
 * ClickHouse EXPLAIN modes currently modeled by the DSL.
 */
public enum ExplainType {
    /** Explain the AST representation. */
    AST("AST"),
    /** Explain the logical or physical plan shape. */
    PLAN("PLAN"),
    /** Explain the execution pipeline. */
    PIPELINE("PIPELINE"),
    /** Explain the parsed syntax form. */
    SYNTAX("SYNTAX");

    private final String sql;

    ExplainType(String sql) {
        this.sql = sql;
    }

    /**
     * Returns the SQL token used after the {@code EXPLAIN} keyword.
     *
     * @return explain SQL token
     */
    public String sql() {
        return sql;
    }
}
