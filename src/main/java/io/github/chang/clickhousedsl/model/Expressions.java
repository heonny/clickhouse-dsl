package io.github.chang.clickhousedsl.model;

public final class Expressions {

    private Expressions() {
    }

    public static <T> ParameterExpression<T> param(T value, Class<T> type) {
        return new ParameterExpression<>(value, type);
    }

    public static AggregateExpression<Long> count() {
        return new AggregateExpression<>("count()", Long.class);
    }

    public static <N extends Number> AggregateExpression<N> sum(Expression<N> expression) {
        return new AggregateExpression<>("sum(" + expression.render(new RenderContext()) + ")", expression.type());
    }
}
