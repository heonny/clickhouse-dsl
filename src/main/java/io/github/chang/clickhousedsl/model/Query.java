package io.github.chang.clickhousedsl.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

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

    public List<Expression<?>> selections() {
        return selections;
    }

    public List<WithClause> withClauses() {
        return withClauses;
    }

    public Table from() {
        return from;
    }

    public List<Join> joins() {
        return joins;
    }

    public List<Expression<?>> arrayJoins() {
        return arrayJoins;
    }

    public Double sampleRatio() {
        return sampleRatio;
    }

    public Expression<Boolean> prewhere() {
        return prewhere;
    }

    public Expression<Boolean> where() {
        return where;
    }

    public List<Expression<?>> groupBy() {
        return groupBy;
    }

    public Expression<Boolean> having() {
        return having;
    }

    public List<Sort> orderBy() {
        return orderBy;
    }

    public Integer limit() {
        return limit;
    }

    public List<Setting> settings() {
        return settings;
    }

    public List<SetOperation> setOperations() {
        return setOperations;
    }
}
