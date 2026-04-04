package io.github.heonny.clickhousedsl.model;

import java.util.Objects;

/**
 * Execution-oriented wrapper combining rendered SQL and execution metrics.
 */
public final class QueryExecutionReport {

    private final RenderedQuery renderedQuery;
    private final ExecutionMetrics metrics;

    /**
     * Creates a query execution report.
     *
     * @param renderedQuery rendered query snapshot
     * @param metrics execution metrics
     */
    public QueryExecutionReport(RenderedQuery renderedQuery, ExecutionMetrics metrics) {
        this.renderedQuery = Objects.requireNonNull(renderedQuery, "renderedQuery");
        this.metrics = Objects.requireNonNull(metrics, "metrics");
    }

    /**
     * Returns the rendered query snapshot.
     *
     * @return rendered query
     */
    public RenderedQuery renderedQuery() {
        return renderedQuery;
    }

    /**
     * Returns execution metrics, if available.
     *
     * @return execution metrics
     */
    public ExecutionMetrics metrics() {
        return metrics;
    }
}
