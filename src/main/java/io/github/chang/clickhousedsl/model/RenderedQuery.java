package io.github.chang.clickhousedsl.model;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class RenderedQuery {

    private final String sql;
    private final List<Object> parameters;

    public RenderedQuery(String sql, List<Object> parameters) {
        this.sql = Objects.requireNonNull(sql, "sql");
        this.parameters = Collections.unmodifiableList(Objects.requireNonNull(parameters, "parameters"));
    }

    public String sql() {
        return sql;
    }

    public List<Object> parameters() {
        return parameters;
    }
}
