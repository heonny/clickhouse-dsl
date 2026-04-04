package io.github.heonny.clickhousedsl.model;

import java.util.Objects;

public class Column<T> implements Expression<T> {

    private final Table table;
    private final Identifier name;
    private final Class<T> type;

    Column(Table table, Identifier name, Class<T> type) {
        this.table = Objects.requireNonNull(table, "table");
        this.name = Objects.requireNonNull(name, "name");
        this.type = Objects.requireNonNull(type, "type");
    }

    @Override
    public Class<T> type() {
        return type;
    }

    @Override
    public String render(RenderContext context) {
        return table.renderReference() + "." + name.sql();
    }

    public ComparisonExpression eq(T value) {
        return eq(Expressions.param(value, type));
    }

    public ComparisonExpression eq(Expression<T> other) {
        return new ComparisonExpression(this, ComparisonOperator.EQ, other);
    }

    public ComparisonExpression gt(T value) {
        return gt(Expressions.param(value, type));
    }

    public ComparisonExpression gt(Expression<T> other) {
        return new ComparisonExpression(this, ComparisonOperator.GT, other);
    }

    public ComparisonExpression lt(T value) {
        return lt(Expressions.param(value, type));
    }

    public ComparisonExpression lt(Expression<T> other) {
        return new ComparisonExpression(this, ComparisonOperator.LT, other);
    }

    public ComparisonExpression gte(T value) {
        return gte(Expressions.param(value, type));
    }

    public ComparisonExpression gte(Expression<T> other) {
        return new ComparisonExpression(this, ComparisonOperator.GTE, other);
    }

    public ComparisonExpression lte(T value) {
        return lte(Expressions.param(value, type));
    }

    public ComparisonExpression lte(Expression<T> other) {
        return new ComparisonExpression(this, ComparisonOperator.LTE, other);
    }

    public Sort asc() {
        return new Sort(this, SortDirection.ASC);
    }

    public Sort desc() {
        return new Sort(this, SortDirection.DESC);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Column<?> column)) {
            return false;
        }
        return table.equals(column.table) && name.equals(column.name) && type.equals(column.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(table, name, type);
    }
}
