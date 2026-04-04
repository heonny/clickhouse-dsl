package io.github.heonny.clickhousedsl.validate;

import io.github.heonny.clickhousedsl.model.Expression;
import io.github.heonny.clickhousedsl.model.BinaryArithmeticExpression;
import io.github.heonny.clickhousedsl.model.Column;
import io.github.heonny.clickhousedsl.model.ComparisonExpression;
import io.github.heonny.clickhousedsl.model.FunctionExpression;
import io.github.heonny.clickhousedsl.model.Join;
import io.github.heonny.clickhousedsl.model.LogicalExpression;
import io.github.heonny.clickhousedsl.model.ParameterExpression;
import io.github.heonny.clickhousedsl.model.Query;
import io.github.heonny.clickhousedsl.model.ReferenceExpression;
import io.github.heonny.clickhousedsl.model.SetOperation;
import io.github.heonny.clickhousedsl.model.Setting;
import io.github.heonny.clickhousedsl.model.LiteralExpression;
import io.github.heonny.clickhousedsl.model.AliasedExpression;
import io.github.heonny.clickhousedsl.model.WindowFunctionExpression;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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
        validateFilterClauses(query, result);
        validateJoinKeys(query, result);
        validateHavingClause(query, result);
        validateSetOperations(query, result);
        validateArrayJoinUsage(query, result);
        validateDuplicateSettings(query, result);
        return result;
    }

    private void validateFilterClauses(Query query, ValidationResult result) {
        if (query.prewhere() != null && containsAggregate(query.prewhere())) {
            result.add(
                ValidationCode.AGGREGATE_NOT_ALLOWED_IN_PREWHERE,
                "PREWHERE expression contains an aggregate-aware subtree."
            );
        }
        if (query.prewhere() != null && containsWindowFunction(query.prewhere())) {
            result.add(
                ValidationCode.WINDOW_FUNCTION_NOT_ALLOWED_IN_PREWHERE,
                "PREWHERE expression contains a window function subtree."
            );
        }
        if (query.where() != null && containsAggregate(query.where())) {
            result.add(
                ValidationCode.AGGREGATE_NOT_ALLOWED_IN_WHERE,
                "WHERE expression contains an aggregate-aware subtree."
            );
        }
        if (query.where() != null && containsWindowFunction(query.where())) {
            result.add(
                ValidationCode.WINDOW_FUNCTION_NOT_ALLOWED_IN_WHERE,
                "WHERE expression contains a window function subtree."
            );
        }
        for (Expression<?> expression : query.groupBy()) {
            if (containsWindowFunction(expression)) {
                result.add(
                    ValidationCode.WINDOW_FUNCTION_NOT_ALLOWED_IN_GROUP_BY,
                    "GROUP BY expression contains a window function subtree."
                );
                break;
            }
        }
    }

    private void validateAggregateUsage(Query query, ValidationResult result) {
        boolean hasAggregate = query.selections().stream().anyMatch(Expression::aggregate);
        boolean hasPlain = query.selections().stream().anyMatch(expression -> !expression.aggregate());

        // Mixed aggregate and plain selections are only legal when the plain expressions are
        // grouped explicitly.
        if (hasAggregate && hasPlain && query.groupBy().isEmpty()) {
            result.add(
                ValidationCode.GROUP_BY_REQUIRED,
                "Found at least one aggregate selection and at least one plain selection in SELECT."
            );
        }

        if (!query.groupBy().isEmpty()) {
            // Once GROUP BY exists, every non-aggregate projection must appear in the grouping set.
            query.selections().stream()
                .filter(expression -> !expression.aggregate())
                .filter(expression -> !query.groupBy().contains(expression))
                .findFirst()
                .ifPresent(expression -> result.add(
                    ValidationCode.GROUP_BY_MISMATCH,
                    "Offending selection type: " + expression.type().getSimpleName()
                ));
        }
    }

    private void validateJoinKeys(Query query, ValidationResult result) {
        for (Join join : query.joins()) {
            if (!join.leftKey().type().equals(join.rightKey().type())) {
                result.add(
                    ValidationCode.JOIN_KEY_TYPE_MISMATCH,
                    "Left type: " + join.leftKey().type().getSimpleName()
                        + ", right type: " + join.rightKey().type().getSimpleName()
                );
            }
        }
    }

    private void validateHavingClause(Query query, ValidationResult result) {
        if (query.having() != null && query.groupBy().isEmpty()) {
            result.add(
                ValidationCode.HAVING_REQUIRES_GROUP_BY,
                "HAVING was provided without any GROUP BY expressions."
            );
            return;
        }

        if (query.having() != null) {
            if (containsWindowFunction(query.having())) {
                result.add(
                    ValidationCode.WINDOW_FUNCTION_NOT_ALLOWED_IN_HAVING,
                    "HAVING expression contains a window function subtree."
                );
                return;
            }
            Optional<Expression<?>> offendingExpression = findUngroupedPlainExpression(query.having(), query.groupBy());
            offendingExpression.ifPresent(expression -> result.add(
                ValidationCode.HAVING_EXPRESSION_NOT_GROUPED,
                "Plain HAVING expression type: " + expression.type().getSimpleName()
            ));
        }
    }

    private void validateSetOperations(Query query, ValidationResult result) {
        for (SetOperation operation : query.setOperations()) {
            Query right = operation.query();
            if (query.selections().size() != right.selections().size()) {
                result.add(
                    ValidationCode.UNION_SELECTION_COUNT_MISMATCH,
                    "Left count: " + query.selections().size() + ", right count: " + right.selections().size()
                );
                continue;
            }
            // ClickHouse aligns UNION columns by ordinal position, not by alias.
            for (int i = 0; i < query.selections().size(); i++) {
                if (!query.selections().get(i).type().equals(right.selections().get(i).type())) {
                    result.add(
                        ValidationCode.UNION_SELECTION_TYPE_MISMATCH,
                        "Position " + i
                            + ": left type=" + query.selections().get(i).type().getSimpleName()
                            + ", right type=" + right.selections().get(i).type().getSimpleName()
                    );
                    break;
                }
            }
        }
    }

    private void validateArrayJoinUsage(Query query, ValidationResult result) {
        for (Expression<?> expression : query.arrayJoins()) {
            if (!List.class.isAssignableFrom(expression.type())) {
                result.add(
                    ValidationCode.ARRAY_JOIN_REQUIRES_ARRAY_TYPE,
                    "Offending expression type: " + expression.type().getSimpleName()
                );
            }
        }
    }

    private void validateDuplicateSettings(Query query, ValidationResult result) {
        Set<String> seenSettingNames = new HashSet<>();
        for (Setting setting : query.settings()) {
            String settingName = setting.name().value();
            if (!seenSettingNames.add(settingName)) {
                result.add(
                    ValidationCode.DUPLICATE_SETTING_NAME,
                    "Duplicate setting name: " + settingName
                );
            }
        }
    }

    private boolean containsAggregate(Expression<?> expression) {
        if (expression.aggregate()) {
            return true;
        }
        if (expression instanceof AliasedExpression<?> aliasedExpression) {
            return containsAggregate(aliasedExpression.delegate());
        }
        if (expression instanceof ComparisonExpression comparisonExpression) {
            return containsAggregate(comparisonExpression.left()) || containsAggregate(comparisonExpression.right());
        }
        if (expression instanceof LogicalExpression logicalExpression) {
            return containsAggregate(logicalExpression.left()) || containsAggregate(logicalExpression.right());
        }
        if (expression instanceof BinaryArithmeticExpression<?> arithmeticExpression) {
            return containsAggregate(arithmeticExpression.left()) || containsAggregate(arithmeticExpression.right());
        }
        if (expression instanceof FunctionExpression<?> functionExpression) {
            for (Expression<?> argument : functionExpression.arguments()) {
                if (containsAggregate(argument)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean containsWindowFunction(Expression<?> expression) {
        if (expression instanceof WindowFunctionExpression<?>) {
            return true;
        }
        if (expression instanceof AliasedExpression<?> aliasedExpression) {
            return containsWindowFunction(aliasedExpression.delegate());
        }
        if (expression instanceof ComparisonExpression comparisonExpression) {
            return containsWindowFunction(comparisonExpression.left()) || containsWindowFunction(comparisonExpression.right());
        }
        if (expression instanceof LogicalExpression logicalExpression) {
            return containsWindowFunction(logicalExpression.left()) || containsWindowFunction(logicalExpression.right());
        }
        if (expression instanceof BinaryArithmeticExpression<?> arithmeticExpression) {
            return containsWindowFunction(arithmeticExpression.left()) || containsWindowFunction(arithmeticExpression.right());
        }
        if (expression instanceof FunctionExpression<?> functionExpression) {
            for (Expression<?> argument : functionExpression.arguments()) {
                if (containsWindowFunction(argument)) {
                    return true;
                }
            }
        }
        return false;
    }

    private Optional<Expression<?>> findUngroupedPlainExpression(Expression<?> expression, List<Expression<?>> groupByExpressions) {
        if (expression.aggregate()) {
            return Optional.empty();
        }
        if (expression instanceof AliasedExpression<?> aliasedExpression) {
            return findUngroupedPlainExpression(aliasedExpression.delegate(), groupByExpressions);
        }
        if (expression instanceof Column<?> || expression instanceof ReferenceExpression<?>) {
            return groupByExpressions.contains(expression) ? Optional.empty() : Optional.of(expression);
        }
        if (expression instanceof LiteralExpression<?> || expression instanceof ParameterExpression<?>) {
            return Optional.empty();
        }
        if (expression instanceof ComparisonExpression comparisonExpression) {
            Optional<Expression<?>> left = findUngroupedPlainExpression(comparisonExpression.left(), groupByExpressions);
            if (left.isPresent()) {
                return left;
            }
            return findUngroupedPlainExpression(comparisonExpression.right(), groupByExpressions);
        }
        if (expression instanceof LogicalExpression logicalExpression) {
            Optional<Expression<?>> left = findUngroupedPlainExpression(logicalExpression.left(), groupByExpressions);
            if (left.isPresent()) {
                return left;
            }
            return findUngroupedPlainExpression(logicalExpression.right(), groupByExpressions);
        }
        if (expression instanceof BinaryArithmeticExpression<?> arithmeticExpression) {
            Optional<Expression<?>> left = findUngroupedPlainExpression(arithmeticExpression.left(), groupByExpressions);
            if (left.isPresent()) {
                return left;
            }
            return findUngroupedPlainExpression(arithmeticExpression.right(), groupByExpressions);
        }
        if (expression instanceof FunctionExpression<?> functionExpression) {
            for (Expression<?> argument : functionExpression.arguments()) {
                Optional<Expression<?>> offending = findUngroupedPlainExpression(argument, groupByExpressions);
                if (offending.isPresent()) {
                    return offending;
                }
            }
        }
        return Optional.empty();
    }
}
