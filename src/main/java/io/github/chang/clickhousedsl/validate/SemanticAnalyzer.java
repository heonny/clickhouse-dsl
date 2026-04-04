package io.github.chang.clickhousedsl.validate;

import io.github.chang.clickhousedsl.model.Expression;
import io.github.chang.clickhousedsl.model.Join;
import io.github.chang.clickhousedsl.model.Query;
import io.github.chang.clickhousedsl.model.SetOperation;
import java.util.List;

public final class SemanticAnalyzer {

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

        if (hasAggregate && hasPlain && query.groupBy().isEmpty()) {
            result.add("GROUP_BY_REQUIRED", "Aggregate and non-aggregate selections require GROUP BY");
        }

        if (!query.groupBy().isEmpty()) {
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
