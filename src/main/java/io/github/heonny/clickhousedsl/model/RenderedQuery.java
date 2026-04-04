package io.github.heonny.clickhousedsl.model;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Render result containing SQL text and positional parameters.
 */
public final class RenderedQuery {

    private final String sql;
    private final List<Object> parameters;

    /**
     * Creates a rendered query snapshot.
     *
     * @param sql rendered SQL
     * @param parameters ordered parameters
     */
    public RenderedQuery(String sql, List<Object> parameters) {
        this.sql = Objects.requireNonNull(sql, "sql");
        this.parameters = Collections.unmodifiableList(Objects.requireNonNull(parameters, "parameters"));
    }

    /**
     * Returns the rendered SQL string.
     *
     * @return SQL string
     */
    public String sql() {
        return sql;
    }

    /**
     * Returns the ordered parameter list.
     *
     * @return ordered parameter list
     */
    public List<Object> parameters() {
        return parameters;
    }
}
