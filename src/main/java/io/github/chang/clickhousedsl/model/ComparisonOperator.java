package io.github.chang.clickhousedsl.model;

public enum ComparisonOperator {
    EQ("="),
    GT(">"),
    LT("<");

    private final String sql;

    ComparisonOperator(String sql) {
        this.sql = sql;
    }

    public String sql() {
        return sql;
    }
}
