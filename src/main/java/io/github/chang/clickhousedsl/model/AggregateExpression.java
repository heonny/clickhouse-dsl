package io.github.chang.clickhousedsl.model;

import java.util.Objects;

public final class AggregateExpression<T> implements Expression<T> {

    private final String sql;
    private final Class<T> type;

    AggregateExpression(String sql, Class<T> type) {
        this.sql = Objects.requireNonNull(sql, "sql");
        this.type = Objects.requireNonNull(type, "type");
    }

    @Override
    public Class<T> type() {
        return type;
    }

    @Override
    public String render(RenderContext context) {
        return sql;
    }

    @Override
    public boolean aggregate() {
        return true;
    }

    public ComparisonExpression eq(Expression<T> other) {
        return new ComparisonExpression(this, ComparisonOperator.EQ, other);
    }

    public ComparisonExpression gt(Expression<T> other) {
        return new ComparisonExpression(this, ComparisonOperator.GT, other);
    }
}
