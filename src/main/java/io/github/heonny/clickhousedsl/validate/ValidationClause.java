package io.github.heonny.clickhousedsl.validate;

/**
 * Stable query clause identifier attached to validation failures.
 *
 * <p>This gives callers a small amount of structured context without exposing internal analyzer
 * implementation details.
 */
public enum ValidationClause {
    /** Validation related to the {@code SELECT} list or grouping semantics. */
    SELECT,
    /** Validation related to the {@code PREWHERE} clause. */
    PREWHERE,
    /** Validation related to the {@code WHERE} clause. */
    WHERE,
    /** Validation related to the {@code GROUP BY} clause. */
    GROUP_BY,
    /** Validation related to explicit JOIN declarations. */
    JOIN,
    /** Validation related to the {@code HAVING} clause. */
    HAVING,
    /** Validation related to {@code UNION} or {@code UNION ALL}. */
    UNION,
    /** Validation related to ClickHouse {@code SETTINGS}. */
    SETTINGS,
    /** Validation related to ClickHouse {@code ARRAY JOIN}. */
    ARRAY_JOIN
}
