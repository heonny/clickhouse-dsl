package io.github.heonny.clickhousedsl.model;

public enum UnionType {
    DISTINCT("UNION"),
    ALL("UNION ALL");

    private final String sql;

    UnionType(String sql) {
        this.sql = sql;
    }

    public String sql() {
        return sql;
    }
}
