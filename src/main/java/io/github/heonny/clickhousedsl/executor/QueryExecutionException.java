package io.github.heonny.clickhousedsl.executor;

import io.github.heonny.clickhousedsl.model.RenderedQuery;
import java.util.Objects;

/**
 * Transport-side query execution failure.
 *
 * <p>This exception is intentionally separate from semantic validation failures so callers can
 * distinguish "invalid query shape" from "valid query but failed during execution".
 */
public final class QueryExecutionException extends RuntimeException {

    /** Rendered query snapshot captured at the execution boundary. */
    private final RenderedQuery renderedQuery;

    /**
     * Creates a query execution exception.
     *
     * @param message failure summary safe for logs and application errors
     * @param cause underlying transport exception
     * @param renderedQuery rendered query snapshot that was being executed
     */
    public QueryExecutionException(String message, Throwable cause, RenderedQuery renderedQuery) {
        super(Objects.requireNonNull(message, "message"), Objects.requireNonNull(cause, "cause"));
        this.renderedQuery = Objects.requireNonNull(renderedQuery, "renderedQuery");
    }

    /**
     * Returns the rendered query that failed during execution.
     *
     * @return rendered query
     */
    public RenderedQuery renderedQuery() {
        return renderedQuery;
    }
}
