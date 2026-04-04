package io.github.heonny.clickhousedsl.executor;

import io.github.heonny.clickhousedsl.api.ClickHouseDsl;
import io.github.heonny.clickhousedsl.model.Query;
import io.github.heonny.clickhousedsl.model.QueryExecutionReport;
import io.github.heonny.clickhousedsl.model.RenderedQuery;
import java.util.Objects;

/**
 * Validation-first executor wrapper for running typed queries safely.
 *
 * <p>This class is intentionally small. It enforces the invariant that transport implementations
 * never receive an invalid query tree. Query validation and SQL rendering happen before the
 * delegate transport is invoked.
 */
public final class SafeQueryExecutor {

    private final RenderedQueryExecutor delegate;

    /**
     * Creates a safe executor wrapper.
     *
     * @param delegate transport-specific rendered-query executor
     */
    public SafeQueryExecutor(RenderedQueryExecutor delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    /**
     * Validates, renders, and executes a query.
     *
     * @param query typed query to execute
     * @return execution report returned by the delegate transport
     */
    public QueryExecutionReport execute(Query query) {
        RenderedQuery renderedQuery = ClickHouseDsl.renderValidatedQuery(Objects.requireNonNull(query, "query"));
        return delegate.execute(renderedQuery);
    }

    /**
     * Returns the underlying rendered-query executor.
     *
     * @return delegate transport
     */
    public RenderedQueryExecutor delegate() {
        return delegate;
    }
}
