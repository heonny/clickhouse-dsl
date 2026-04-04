package io.github.heonny.clickhousedsl.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Immutable query object produced by the DSL builder.
 *
 * <p>This object is the main boundary between query construction and later phases such as rendering,
 * validation, explain generation, and future execution.
 */
public final class Query {

    private final List<Expression<?>> selections;
    private final List<WithClause> withClauses;
    private final Table from;
    private final List<Join> joins;
    private final List<Expression<?>> arrayJoins;
    private final Double sampleRatio;
    private final Expression<Boolean> prewhere;
    private final Expression<Boolean> where;
    private final List<Expression<?>> groupBy;
    private final Expression<Boolean> having;
    private final List<Sort> orderBy;
    private final Integer limit;
    private final List<Setting> settings;
    private final List<SetOperation> setOperations;

    /**
     * Creates an immutable query snapshot from the builder state.
     *
     * <p>All list inputs are defensively copied and wrapped as unmodifiable collections so downstream
     * phases can safely assume a stable structure.
     *
     * @param selections select expressions
     * @param withClauses CTE clauses
     * @param from main source table
     * @param joins join descriptors
     * @param arrayJoins array-join expressions
     * @param sampleRatio optional sample ratio
     * @param prewhere optional prewhere predicate
     * @param where optional where predicate
     * @param groupBy group by expressions
     * @param having optional having predicate
     * @param orderBy order by expressions
     * @param limit optional limit
     * @param settings ClickHouse settings
     * @param setOperations union-like operations
     */
    public Query(
        List<Expression<?>> selections,
        List<WithClause> withClauses,
        Table from,
        List<Join> joins,
        List<Expression<?>> arrayJoins,
        Double sampleRatio,
        Expression<Boolean> prewhere,
        Expression<Boolean> where,
        List<Expression<?>> groupBy,
        Expression<Boolean> having,
        List<Sort> orderBy,
        Integer limit,
        List<Setting> settings,
        List<SetOperation> setOperations
    ) {
        this.selections = Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNull(selections, "selections")));
        this.withClauses = Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNull(withClauses, "withClauses")));
        this.from = Objects.requireNonNull(from, "from");
        this.joins = Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNull(joins, "joins")));
        this.arrayJoins = Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNull(arrayJoins, "arrayJoins")));
        this.sampleRatio = sampleRatio;
        this.prewhere = prewhere;
        this.where = where;
        this.groupBy = Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNull(groupBy, "groupBy")));
        this.having = having;
        this.orderBy = Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNull(orderBy, "orderBy")));
        this.limit = limit;
        this.settings = Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNull(settings, "settings")));
        this.setOperations = Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNull(setOperations, "setOperations")));
    }

    /**
     * Returns the select expressions.
     *
     * @return select expressions
     */
    public List<Expression<?>> selections() {
        return selections;
    }

    /**
     * Returns the CTE clauses.
     *
     * @return with clauses
     */
    public List<WithClause> withClauses() {
        return withClauses;
    }

    /**
     * Returns the main source table.
     *
     * @return source table
     */
    public Table from() {
        return from;
    }

    /**
     * Returns the join descriptors.
     *
     * @return join list
     */
    public List<Join> joins() {
        return joins;
    }

    /**
     * Returns the array-join expressions.
     *
     * @return array-join expressions
     */
    public List<Expression<?>> arrayJoins() {
        return arrayJoins;
    }

    /**
     * Returns the optional sample ratio.
     *
     * @return sample ratio or {@code null}
     */
    public Double sampleRatio() {
        return sampleRatio;
    }

    /**
     * Returns the optional PREWHERE predicate.
     *
     * @return prewhere predicate or {@code null}
     */
    public Expression<Boolean> prewhere() {
        return prewhere;
    }

    /**
     * Returns the optional WHERE predicate.
     *
     * @return where predicate or {@code null}
     */
    public Expression<Boolean> where() {
        return where;
    }

    /**
     * Returns the grouping expressions.
     *
     * @return grouping expressions
     */
    public List<Expression<?>> groupBy() {
        return groupBy;
    }

    /**
     * Returns the optional HAVING predicate.
     *
     * @return having predicate or {@code null}
     */
    public Expression<Boolean> having() {
        return having;
    }

    /**
     * Returns the order-by list.
     *
     * @return order-by descriptors
     */
    public List<Sort> orderBy() {
        return orderBy;
    }

    /**
     * Returns the optional limit.
     *
     * @return limit or {@code null}
     */
    public Integer limit() {
        return limit;
    }

    /**
     * Returns the query settings.
     *
     * @return settings list
     */
    public List<Setting> settings() {
        return settings;
    }

    /**
     * Returns the set operations attached to the primary query.
     *
     * @return set operations
     */
    public List<SetOperation> setOperations() {
        return setOperations;
    }
}
