package io.github.heonny.clickhousedsl.validate;

/**
 * Stable machine-readable semantic validation code.
 */
public enum ValidationCode {
    /** Mixed aggregate and non-aggregate projections without grouping. */
    GROUP_BY_REQUIRED(
        ValidationClause.SELECT,
        "Aggregate and non-aggregate selections cannot be mixed without GROUP BY."
    ),
    /** Non-aggregate selections missing from the grouping set. */
    GROUP_BY_MISMATCH(
        ValidationClause.SELECT,
        "Every non-aggregate selection must also appear in GROUP BY."
    ),
    /** Aggregate expressions are not allowed inside PREWHERE. */
    AGGREGATE_NOT_ALLOWED_IN_PREWHERE(
        ValidationClause.PREWHERE,
        "PREWHERE cannot contain aggregate expressions."
    ),
    /** Aggregate expressions are not allowed inside WHERE. */
    AGGREGATE_NOT_ALLOWED_IN_WHERE(
        ValidationClause.WHERE,
        "WHERE cannot contain aggregate expressions."
    ),
    /** Window functions are not allowed inside PREWHERE. */
    WINDOW_FUNCTION_NOT_ALLOWED_IN_PREWHERE(
        ValidationClause.PREWHERE,
        "PREWHERE cannot contain window functions."
    ),
    /** Window functions are not allowed inside WHERE. */
    WINDOW_FUNCTION_NOT_ALLOWED_IN_WHERE(
        ValidationClause.WHERE,
        "WHERE cannot contain window functions."
    ),
    /** Join key types do not align across both sides of the join. */
    JOIN_KEY_TYPE_MISMATCH(
        ValidationClause.JOIN,
        "Join key types must match on both sides of the JOIN."
    ),
    /** HAVING was declared without GROUP BY. */
    HAVING_REQUIRES_GROUP_BY(
        ValidationClause.HAVING,
        "HAVING cannot be used without GROUP BY."
    ),
    /** Window functions are not allowed inside HAVING. */
    WINDOW_FUNCTION_NOT_ALLOWED_IN_HAVING(
        ValidationClause.HAVING,
        "HAVING cannot contain window functions."
    ),
    /** HAVING references a plain expression that is not part of the grouping set. */
    HAVING_EXPRESSION_NOT_GROUPED(
        ValidationClause.HAVING,
        "HAVING may reference plain expressions only when they also appear in GROUP BY."
    ),
    /** Window functions are not allowed inside GROUP BY. */
    WINDOW_FUNCTION_NOT_ALLOWED_IN_GROUP_BY(
        ValidationClause.GROUP_BY,
        "GROUP BY cannot contain window functions."
    ),
    /** UNION branches expose different numbers of selections. */
    UNION_SELECTION_COUNT_MISMATCH(
        ValidationClause.UNION,
        "UNION queries must select the same number of expressions."
    ),
    /** UNION branches expose incompatible types at the same ordinal position. */
    UNION_SELECTION_TYPE_MISMATCH(
        ValidationClause.UNION,
        "UNION queries must align selection types by ordinal position."
    ),
    /** The same setting name was declared more than once. */
    DUPLICATE_SETTING_NAME(
        ValidationClause.SETTINGS,
        "Each ClickHouse setting name may appear only once per query."
    ),
    /** ARRAY JOIN received a non-array expression. */
    ARRAY_JOIN_REQUIRES_ARRAY_TYPE(
        ValidationClause.ARRAY_JOIN,
        "ARRAY JOIN requires an array-typed expression."
    );

    private final ValidationClause clause;
    private final String defaultMessage;

    ValidationCode(ValidationClause clause, String defaultMessage) {
        this.clause = clause;
        this.defaultMessage = defaultMessage;
    }

    /**
     * Returns the query clause associated with this code.
     *
     * @return validation clause
     */
    public ValidationClause clause() {
        return clause;
    }

    /**
     * Returns the default human-readable message.
     *
     * @return default message
     */
    public String defaultMessage() {
        return defaultMessage;
    }
}
