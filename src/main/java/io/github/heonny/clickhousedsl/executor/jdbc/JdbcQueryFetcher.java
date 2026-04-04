package io.github.heonny.clickhousedsl.executor.jdbc;

import io.github.heonny.clickhousedsl.api.ClickHouseDsl;
import io.github.heonny.clickhousedsl.executor.QueryExecutionException;
import io.github.heonny.clickhousedsl.model.Query;
import io.github.heonny.clickhousedsl.model.RenderedQuery;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.sql.DataSource;

/**
 * Validation-first JDBC query fetcher with row mapping.
 *
 * <p>This class is intended for read paths where callers need typed rows rather than only an
 * execution report. Query validation still happens before any transport interaction.
 */
public final class JdbcQueryFetcher {

    private final DataSource dataSource;
    private final Integer queryTimeoutSeconds;

    /**
     * Creates a JDBC query fetcher without a query timeout override.
     *
     * @param dataSource JDBC data source
     */
    public JdbcQueryFetcher(DataSource dataSource) {
        this(dataSource, null);
    }

    /**
     * Creates a JDBC query fetcher with an optional query timeout.
     *
     * @param dataSource JDBC data source
     * @param queryTimeoutSeconds positive timeout in seconds, or {@code null}
     */
    public JdbcQueryFetcher(DataSource dataSource, Integer queryTimeoutSeconds) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
        if (queryTimeoutSeconds != null && queryTimeoutSeconds <= 0) {
            throw new IllegalArgumentException("queryTimeoutSeconds must be positive");
        }
        this.queryTimeoutSeconds = queryTimeoutSeconds;
    }

    /**
     * Validates, renders, executes, and maps all rows of a query.
     *
     * @param query typed query
     * @param rowMapper row mapper for the current result shape
     * @param <T> mapped row type
     * @return mapped rows
     */
    public <T> List<T> fetch(Query query, RowMapper<T> rowMapper) {
        Objects.requireNonNull(query, "query");
        Objects.requireNonNull(rowMapper, "rowMapper");

        RenderedQuery renderedQuery = ClickHouseDsl.renderValidatedQuery(query);

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(renderedQuery.sql())) {
            if (queryTimeoutSeconds != null) {
                statement.setQueryTimeout(queryTimeoutSeconds);
            }
            bindParameters(statement, renderedQuery);

            try (ResultSet resultSet = statement.executeQuery()) {
                List<T> rows = new ArrayList<>();
                int rowNum = 0;
                while (resultSet.next()) {
                    rows.add(rowMapper.mapRow(resultSet, rowNum));
                    rowNum++;
                }
                return rows;
            }
        } catch (SQLException exception) {
            throw new QueryExecutionException("JDBC query fetch failed.", exception, renderedQuery);
        }
    }

    /**
     * Returns the configured query timeout in seconds.
     *
     * @return query timeout or {@code null}
     */
    public Integer queryTimeoutSeconds() {
        return queryTimeoutSeconds;
    }

    /**
     * Returns the configured JDBC data source.
     *
     * @return JDBC data source
     */
    public DataSource dataSource() {
        return dataSource;
    }

    private void bindParameters(PreparedStatement statement, RenderedQuery renderedQuery) throws SQLException {
        for (int index = 0; index < renderedQuery.parameters().size(); index++) {
            statement.setObject(index + 1, renderedQuery.parameters().get(index));
        }
    }
}
