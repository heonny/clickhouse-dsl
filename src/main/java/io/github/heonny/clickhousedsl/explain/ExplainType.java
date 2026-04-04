package io.github.heonny.clickhousedsl.explain;

public enum ExplainType {
    AST("AST"),
    PLAN("PLAN"),
    PIPELINE("PIPELINE"),
    SYNTAX("SYNTAX");

    private final String sql;

    ExplainType(String sql) {
        this.sql = sql;
    }

    public String sql() {
        return sql;
    }
}
