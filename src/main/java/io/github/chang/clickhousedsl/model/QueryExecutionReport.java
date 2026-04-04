package io.github.chang.clickhousedsl.model;

import java.util.Objects;

public final class QueryExecutionReport {

    private final RenderedQuery renderedQuery;
    private final ExecutionMetrics metrics;

    public QueryExecutionReport(RenderedQuery renderedQuery, ExecutionMetrics metrics) {
        this.renderedQuery = Objects.requireNonNull(renderedQuery, "renderedQuery");
        this.metrics = Objects.requireNonNull(metrics, "metrics");
    }

    public RenderedQuery renderedQuery() {
        return renderedQuery;
    }

    public ExecutionMetrics metrics() {
        return metrics;
    }
}
