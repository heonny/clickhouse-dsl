package io.github.chang.clickhousedsl.model;

public enum JoinType {
    INNER("INNER JOIN"),
    LEFT("LEFT JOIN");

    private final String sql;

    JoinType(String sql) {
        this.sql = sql;
    }

    public String sql() {
        return sql;
    }
}
