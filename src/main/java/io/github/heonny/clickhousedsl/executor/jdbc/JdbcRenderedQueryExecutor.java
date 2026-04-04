package io.github.heonny.clickhousedsl.executor.jdbc;

import io.github.heonny.clickhousedsl.executor.QueryExecutionException;
import io.github.heonny.clickhousedsl.executor.RenderedQueryExecutor;
import io.github.heonny.clickhousedsl.model.ExecutionMetrics;
import io.github.heonny.clickhousedsl.model.QueryExecutionReport;
import io.github.heonny.clickhousedsl.model.RenderedQuery;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Objects;
import javax.sql.DataSource;

/**
 * Minimal JDBC transport for executing a rendered query.
 *
 * <p>This implementation is intentionally narrow. It prepares the SQL, binds positional parameters
 * in order, optionally applies a query timeout, and returns a basic execution report without row
 * materialization.
 */
public final class JdbcRenderedQueryExecutor implements RenderedQueryExecutor {

    private final DataSource dataSource;
    private final Integer queryTimeoutSeconds;

    /**
     * Creates a JDBC executor without a query timeout override.
     *
     * @param dataSource JDBC data source
     */
    public JdbcRenderedQueryExecutor(DataSource dataSource) {
        this(dataSource, null);
    }

    /**
     * Creates a JDBC executor with an optional query timeout.
     *
     * @param dataSource JDBC data source
     * @param queryTimeoutSeconds positive timeout in seconds, or {@code null}
     */
    public JdbcRenderedQueryExecutor(DataSource dataSource, Integer queryTimeoutSeconds) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
        if (queryTimeoutSeconds != null && queryTimeoutSeconds <= 0) {
            throw new IllegalArgumentException("queryTimeoutSeconds must be positive");
        }
        this.queryTimeoutSeconds = queryTimeoutSeconds;
    }

    /**
     * Executes a rendered query through JDBC.
     *
     * @param renderedQuery SQL plus positional parameters
     * @return execution report without transport-specific metrics
     * @throws QueryExecutionException when JDBC execution fails
     */
    @Override
    public QueryExecutionReport execute(RenderedQuery renderedQuery) {
        Objects.requireNonNull(renderedQuery, "renderedQuery");

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(renderedQuery.sql())) {
            if (queryTimeoutSeconds != null) {
                statement.setQueryTimeout(queryTimeoutSeconds);
            }
            bindParameters(statement, renderedQuery);
            statement.execute();
            return new QueryExecutionReport(renderedQuery, new ExecutionMetrics(null, null));
        } catch (SQLException exception) {
            throw new QueryExecutionException(
                "JDBC query execution failed.",
                exception,
                renderedQuery
            );
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
