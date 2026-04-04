package io.github.heonny.clickhousedsl.executor;

import io.github.heonny.clickhousedsl.model.QueryExecutionReport;
import io.github.heonny.clickhousedsl.model.RenderedQuery;

/**
 * Minimal transport boundary for executing an already-rendered query.
 *
 * <p>Implementations can map this to JDBC, HTTP, or any other ClickHouse transport without
 * coupling transport details to the DSL and validation layers.
 */
@FunctionalInterface
public interface RenderedQueryExecutor {

    /**
     * Executes a rendered query through the underlying transport.
     *
     * @param renderedQuery SQL plus ordered parameters
     * @return execution report
     */
    QueryExecutionReport execute(RenderedQuery renderedQuery);
}
