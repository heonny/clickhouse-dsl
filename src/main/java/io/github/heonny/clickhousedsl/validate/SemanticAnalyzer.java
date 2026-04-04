package io.github.heonny.clickhousedsl.validate;

import io.github.heonny.clickhousedsl.model.Expression;
import io.github.heonny.clickhousedsl.model.Join;
import io.github.heonny.clickhousedsl.model.Query;
import io.github.heonny.clickhousedsl.model.SetOperation;
import java.util.List;

/**
 * Applies semantic validation rules that are not fully expressible in the Java type system.
 *
 * <p>The analyzer is intentionally conservative. It catches high-value structural errors such as:
 *
 * <ul>
 *   <li>aggregate misuse without {@code GROUP BY}
 *   <li>join key type mismatch
 *   <li>{@code HAVING} without grouping
 *   <li>{@code UNION} shape mismatch
 *   <li>{@code ARRAY JOIN} on non-array expressions
 * </ul>
 */
public final class SemanticAnalyzer {

    /**
     * Validates a query and accumulates all detected semantic errors.
     *
     * @param query query to validate
     * @return validation result
     */
    public ValidationResult validate(Query query) {
        ValidationResult result = new ValidationResult();
        validateAggregateUsage(query, result);
        validateJoinKeys(query, result);
        validateHavingClause(query, result);
        validateSetOperations(query, result);
        validateArrayJoinUsage(query, result);
        return result;
    }

    private void validateAggregateUsage(Query query, ValidationResult result) {
        boolean hasAggregate = query.selections().stream().anyMatch(Expression::aggregate);
        boolean hasPlain = query.selections().stream().anyMatch(expression -> !expression.aggregate());

        // Mixed aggregate and plain selections are only legal when the plain expressions are
        // grouped explicitly.
        if (hasAggregate && hasPlain && query.groupBy().isEmpty()) {
            result.add("GROUP_BY_REQUIRED", "Aggregate and non-aggregate selections require GROUP BY");
        }

        if (!query.groupBy().isEmpty()) {
            // Once GROUP BY exists, every non-aggregate projection must appear in the grouping set.
            query.selections().stream()
                .filter(expression -> !expression.aggregate())
                .filter(expression -> !query.groupBy().contains(expression))
                .findFirst()
                .ifPresent(expression -> result.add("GROUP_BY_MISMATCH", "Non-aggregate selection must appear in GROUP BY"));
        }
    }

    private void validateJoinKeys(Query query, ValidationResult result) {
        for (Join join : query.joins()) {
            if (!join.leftKey().type().equals(join.rightKey().type())) {
                result.add("JOIN_KEY_TYPE_MISMATCH", "Join key types must match");
            }
        }
    }

    private void validateHavingClause(Query query, ValidationResult result) {
        if (query.having() != null && query.groupBy().isEmpty()) {
            result.add("HAVING_REQUIRES_GROUP_BY", "HAVING requires GROUP BY");
        }
    }

    private void validateSetOperations(Query query, ValidationResult result) {
        for (SetOperation operation : query.setOperations()) {
            Query right = operation.query();
            if (query.selections().size() != right.selections().size()) {
                result.add("UNION_SELECTION_COUNT_MISMATCH", "UNION queries must select the same number of columns");
                continue;
            }
            // ClickHouse aligns UNION columns by ordinal position, not by alias.
            for (int i = 0; i < query.selections().size(); i++) {
                if (!query.selections().get(i).type().equals(right.selections().get(i).type())) {
                    result.add("UNION_SELECTION_TYPE_MISMATCH", "UNION selection types must match by position");
                    break;
                }
            }
        }
    }

    private void validateArrayJoinUsage(Query query, ValidationResult result) {
        for (Expression<?> expression : query.arrayJoins()) {
            if (!List.class.isAssignableFrom(expression.type())) {
                result.add("ARRAY_JOIN_REQUIRES_ARRAY_TYPE", "ARRAY JOIN requires an array-typed expression");
            }
        }
    }
}
