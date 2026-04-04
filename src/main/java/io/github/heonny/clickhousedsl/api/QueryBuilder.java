package io.github.heonny.clickhousedsl.api;

import io.github.heonny.clickhousedsl.model.Expression;
import io.github.heonny.clickhousedsl.model.Join;
import io.github.heonny.clickhousedsl.model.JoinType;
import io.github.heonny.clickhousedsl.model.Query;
import io.github.heonny.clickhousedsl.model.SetOperation;
import io.github.heonny.clickhousedsl.model.Setting;
import io.github.heonny.clickhousedsl.model.Sort;
import io.github.heonny.clickhousedsl.model.Table;
import io.github.heonny.clickhousedsl.model.UnionType;
import io.github.heonny.clickhousedsl.model.WithClause;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Mutable implementation behind the step-based public DSL.
 *
 * <p>The builder intentionally keeps state local until {@link #build()} is called. At that point
 * it materializes an immutable {@link Query}. This class is package-private so callers are forced
 * to go through {@link ClickHouseDsl}.
 */
final class QueryBuilder implements ClickHouseDsl.SelectStep, ClickHouseDsl.QueryStep, ClickHouseDsl.GroupedQueryStep, ClickHouseDsl.JoinOnStep {

    private final List<Expression<?>> selections;
    private final List<WithClause> withClauses = new ArrayList<>();
    private final List<Join> joins = new ArrayList<>();
    private final List<Expression<?>> arrayJoins = new ArrayList<>();
    private final List<Expression<?>> groupBy = new ArrayList<>();
    private final List<Sort> orderBy = new ArrayList<>();
    private final List<Setting> settings = new ArrayList<>();
    private final List<SetOperation> setOperations = new ArrayList<>();

    private Table from;
    private Double sampleRatio;
    private Expression<Boolean> prewhere;
    private Expression<Boolean> where;
    private Expression<Boolean> having;
    private Integer limit;
    private JoinType pendingJoinType;
    private Table pendingJoinTable;

    QueryBuilder(Expression<?>... selections) {
        this.selections = requireNonNullElements("selections", selections);
        if (this.selections.isEmpty()) {
            throw new IllegalArgumentException("At least one selection is required");
        }
    }

    @Override
    public ClickHouseDsl.QueryStep from(Table table) {
        this.from = Objects.requireNonNull(table, "table");
        return this;
    }

    @Override
    public ClickHouseDsl.QueryStep with(WithClause... clauses) {
        withClauses.addAll(requireNonNullElements("clauses", clauses));
        return this;
    }

    @Override
    public ClickHouseDsl.QueryStep prewhere(Expression<Boolean> expression) {
        this.prewhere = Objects.requireNonNull(expression, "expression");
        return this;
    }

    @Override
    public ClickHouseDsl.QueryStep where(Expression<Boolean> expression) {
        this.where = Objects.requireNonNull(expression, "expression");
        return this;
    }

    @Override
    public ClickHouseDsl.JoinOnStep innerJoin(Table table) {
        this.pendingJoinType = JoinType.INNER;
        this.pendingJoinTable = Objects.requireNonNull(table, "table");
        return this;
    }

    @Override
    public ClickHouseDsl.JoinOnStep leftJoin(Table table) {
        this.pendingJoinType = JoinType.LEFT;
        this.pendingJoinTable = Objects.requireNonNull(table, "table");
        return this;
    }

    @Override
    public <T> ClickHouseDsl.QueryStep on(Expression<T> left, Expression<T> right) {
        // JOIN is stored only when both the join kind and target table were declared first.
        joins.add(new Join(pendingJoinType, pendingJoinTable, left, right));
        pendingJoinType = null;
        pendingJoinTable = null;
        return this;
    }

    @Override
    public ClickHouseDsl.QueryStep arrayJoin(Expression<?>... expressions) {
        arrayJoins.addAll(requireNonNullElements("expressions", expressions));
        return this;
    }

    @Override
    public ClickHouseDsl.QueryStep sample(double ratio) {
        if (ratio <= 0.0d || ratio > 1.0d) {
            throw new IllegalArgumentException("Sample ratio must be in (0, 1]");
        }
        this.sampleRatio = ratio;
        return this;
    }

    @Override
    public ClickHouseDsl.GroupedQueryStep groupBy(Expression<?>... expressions) {
        groupBy.addAll(requireNonNullElements("expressions", expressions));
        return this;
    }

    @Override
    public ClickHouseDsl.GroupedQueryStep having(Expression<Boolean> expression) {
        this.having = Objects.requireNonNull(expression, "expression");
        return this;
    }

    @Override
    public ClickHouseDsl.QueryStep orderBy(Sort... sorts) {
        orderBy.addAll(requireNonNullElements("sorts", sorts));
        return this;
    }

    @Override
    public ClickHouseDsl.QueryStep limit(int limit) {
        if (limit <= 0) {
            throw new IllegalArgumentException("Limit must be positive");
        }
        this.limit = limit;
        return this;
    }

    @Override
    public ClickHouseDsl.QueryStep settings(Setting... settings) {
        this.settings.addAll(requireNonNullElements("settings", settings));
        return this;
    }

    @Override
    public ClickHouseDsl.QueryStep union(Query query) {
        setOperations.add(new SetOperation(UnionType.DISTINCT, Objects.requireNonNull(query, "query")));
        return this;
    }

    @Override
    public ClickHouseDsl.QueryStep unionAll(Query query) {
        setOperations.add(new SetOperation(UnionType.ALL, Objects.requireNonNull(query, "query")));
        return this;
    }

    @Override
    public Query build() {
        if (from == null) {
            throw new IllegalStateException("FROM is required");
        }
        // A dangling JOIN without ON is a partially built query and must fail fast here.
        if (pendingJoinType != null || pendingJoinTable != null) {
            throw new IllegalStateException("Join must be completed with ON");
        }
        return new Query(
            selections,
            withClauses,
            from,
            joins,
            arrayJoins,
            sampleRatio,
            prewhere,
            where,
            groupBy,
            having,
            orderBy,
            limit,
            settings,
            setOperations
        );
    }

    private static <T> List<T> requireNonNullElements(String label, T[] values) {
        Objects.requireNonNull(values, label);
        List<T> list = Arrays.asList(values);
        for (T value : list) {
            Objects.requireNonNull(value, label + " must not contain null");
        }
        return list;
    }
}
