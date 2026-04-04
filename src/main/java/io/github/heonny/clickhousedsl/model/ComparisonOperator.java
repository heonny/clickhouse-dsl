package io.github.heonny.clickhousedsl.model;

public enum ComparisonOperator {
    EQ("="),
    GT(">"),
    LT("<"),
    GTE(">="),
    LTE("<=");

    private final String sql;

    ComparisonOperator(String sql) {
        this.sql = sql;
    }

    public String sql() {
        return sql;
    }
}
