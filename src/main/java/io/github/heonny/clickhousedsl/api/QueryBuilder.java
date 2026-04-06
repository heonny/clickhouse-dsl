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
 * to go through {@link ClickHouseDsl}. Mutations are synchronized so a shared builder remains
 * linearizable when callers coordinate through multiple threads.
 */
final class QueryBuilder implements ClickHouseDsl.SelectStep, ClickHouseDsl.QueryStep, ClickHouseDsl.GroupedQueryStep {

    private final List<Expression<?>> selections;
    private final List<WithClause> withClauses = new ArrayList<>();
    private final List<Join> joins = new ArrayList<>();
    private final List<Expression<?>> arrayJoins = new ArrayList<>();
    private final List<Expression<?>> groupBy = new ArrayList<>();
    private final List<Sort> orderBy = new ArrayList<>();
    private final List<Setting> settings = new ArrayList<>();
    private final List<SetOperation> setOperations = new ArrayList<>();
    private final Object mutex = new Object();

    private Table from;
    private Double sampleRatio;
    private Expression<Boolean> prewhere;
    private Expression<Boolean> where;
    private Expression<Boolean> having;
    private Integer limit;
    private int pendingJoinSteps;

    QueryBuilder(Expression<?>... selections) {
        this.selections = List.copyOf(requireNonNullElements("selections", selections));
        if (this.selections.isEmpty()) {
            throw new IllegalArgumentException("At least one selection is required");
        }
    }

    @Override
    public ClickHouseDsl.QueryStep from(Table table) {
        synchronized (mutex) {
            this.from = Objects.requireNonNull(table, "table");
        }
        return this;
    }

    @Override
    public ClickHouseDsl.QueryStep with(WithClause... clauses) {
        synchronized (mutex) {
            withClauses.addAll(requireNonNullElements("clauses", clauses));
        }
        return this;
    }

    @Override
    public ClickHouseDsl.QueryStep prewhere(Expression<Boolean> expression) {
        synchronized (mutex) {
            this.prewhere = Objects.requireNonNull(expression, "expression");
        }
        return this;
    }

    @Override
    public ClickHouseDsl.QueryStep where(Expression<Boolean> expression) {
        synchronized (mutex) {
            this.where = Objects.requireNonNull(expression, "expression");
        }
        return this;
    }

    @Override
    public ClickHouseDsl.JoinOnStep innerJoin(Table table) {
        return registerPendingJoin(JoinType.INNER, table);
    }

    @Override
    public ClickHouseDsl.JoinOnStep leftJoin(Table table) {
        return registerPendingJoin(JoinType.LEFT, table);
    }

    @Override
    public ClickHouseDsl.QueryStep arrayJoin(Expression<?>... expressions) {
        synchronized (mutex) {
            arrayJoins.addAll(requireNonNullElements("expressions", expressions));
        }
        return this;
    }

    @Override
    public ClickHouseDsl.QueryStep sample(double ratio) {
        if (ratio <= 0.0d || ratio > 1.0d) {
            throw new IllegalArgumentException("Sample ratio must be in (0, 1]");
        }
        synchronized (mutex) {
            this.sampleRatio = ratio;
        }
        return this;
    }

    @Override
    public ClickHouseDsl.GroupedQueryStep groupBy(Expression<?>... expressions) {
        synchronized (mutex) {
            groupBy.addAll(requireNonNullElements("expressions", expressions));
        }
        return this;
    }

    @Override
    public ClickHouseDsl.GroupedQueryStep having(Expression<Boolean> expression) {
        synchronized (mutex) {
            this.having = Objects.requireNonNull(expression, "expression");
        }
        return this;
    }

    @Override
    public ClickHouseDsl.QueryStep orderBy(Sort... sorts) {
        synchronized (mutex) {
            orderBy.addAll(requireNonNullElements("sorts", sorts));
        }
        return this;
    }

    @Override
    public ClickHouseDsl.QueryStep limit(int limit) {
        if (limit <= 0) {
            throw new IllegalArgumentException("Limit must be positive");
        }
        synchronized (mutex) {
            this.limit = limit;
        }
        return this;
    }

    @Override
    public ClickHouseDsl.QueryStep settings(Setting... settings) {
        synchronized (mutex) {
            this.settings.addAll(requireNonNullElements("settings", settings));
        }
        return this;
    }

    @Override
    public ClickHouseDsl.QueryStep union(Query query) {
        synchronized (mutex) {
            setOperations.add(new SetOperation(UnionType.DISTINCT, Objects.requireNonNull(query, "query")));
        }
        return this;
    }

    @Override
    public ClickHouseDsl.QueryStep unionAll(Query query) {
        synchronized (mutex) {
            setOperations.add(new SetOperation(UnionType.ALL, Objects.requireNonNull(query, "query")));
        }
        return this;
    }

    @Override
    public Query build() {
        synchronized (mutex) {
            if (from == null) {
                throw new IllegalStateException("FROM is required");
            }
            // A dangling JOIN without ON is a partially built query and must fail fast here.
            if (pendingJoinSteps > 0) {
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
    }

    private ClickHouseDsl.JoinOnStep registerPendingJoin(JoinType type, Table table) {
        synchronized (mutex) {
            pendingJoinSteps++;
            return new PendingJoinStep(type, Objects.requireNonNull(table, "table"));
        }
    }

    private static <T> List<T> requireNonNullElements(String label, T[] values) {
        Objects.requireNonNull(values, label);
        List<T> list = Arrays.asList(values);
        for (T value : list) {
            Objects.requireNonNull(value, label + " must not contain null");
        }
        return list;
    }

    private final class PendingJoinStep implements ClickHouseDsl.JoinOnStep {
        private final JoinType type;
        private final Table table;
        private boolean completed;

        private PendingJoinStep(JoinType type, Table table) {
            this.type = type;
            this.table = table;
        }

        @Override
        public <T> ClickHouseDsl.QueryStep on(Expression<T> left, Expression<T> right) {
            synchronized (mutex) {
                if (completed) {
                    throw new IllegalStateException("Join has already been completed");
                }
                joins.add(new Join(type, table, left, right));
                completed = true;
                pendingJoinSteps--;
                return QueryBuilder.this;
            }
        }
    }
}
