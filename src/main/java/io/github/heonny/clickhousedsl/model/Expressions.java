package io.github.heonny.clickhousedsl.model;

public final class Expressions {

    private Expressions() {
    }

    public static <T> ParameterExpression<T> param(T value, Class<T> type) {
        return new ParameterExpression<>(value, type);
    }

    public static <T> LiteralExpression<T> literal(T value, Class<T> type) {
        return new LiteralExpression<>(value, type);
    }

    public static AggregateExpression<Long> count() {
        return new AggregateExpression<>("count()", Long.class);
    }

    public static <N extends Number> AggregateExpression<N> sum(Expression<N> expression) {
        return new AggregateExpression<>(context -> "sum(" + expression.render(context) + ")", expression.type());
    }

    public static <N extends Number> AggregateStateExpression<N> sumState(Expression<N> expression) {
        return new AggregateStateExpression<>(context -> "sumState(" + expression.render(context) + ")", expression.type());
    }

    public static <N extends Number> AggregateExpression<N> sumMerge(AggregateStateExpression<N> expression) {
        return new AggregateExpression<>(context -> "sumMerge(" + expression.render(context) + ")", expression.valueType());
    }

    public static AggregateExpression<Long> countMerge(Expression<AggregateState<Long>> expression) {
        return new AggregateExpression<>(context -> "countMerge(" + expression.render(context) + ")", Long.class);
    }

    public static AggregateExpression<Long> countIfMerge(Expression<AggregateState<Long>> expression) {
        return new AggregateExpression<>(context -> "countIfMerge(" + expression.render(context) + ")", Long.class);
    }

    public static AggregateExpression<Long> uniqMerge(Expression<AggregateState<Long>> expression) {
        return new AggregateExpression<>(context -> "uniqMerge(" + expression.render(context) + ")", Long.class);
    }

    public static <N extends Number> AggregateExpression<N> sumMerge(Expression<AggregateState<N>> expression, Class<N> valueType) {
        return new AggregateExpression<>(context -> "sumMerge(" + expression.render(context) + ")", valueType);
    }

    public static <T> FunctionExpression<T> function(String name, Class<T> type, Expression<?>... arguments) {
        return FunctionExpression.of(name, type, false, arguments);
    }

    public static <T> FunctionExpression<T> aggregateFunction(String name, Class<T> type, Expression<?>... arguments) {
        return FunctionExpression.of(name, type, true, arguments);
    }

    public static BinaryArithmeticExpression<Double> divide(Expression<?> left, Expression<?> right) {
        return new BinaryArithmeticExpression<>(left, "/", right, Double.class, left.aggregate() || right.aggregate());
    }

    public static <T> ReferenceExpression<T> ref(String identifier, Class<T> type) {
        return new ReferenceExpression<>(identifier, type);
    }

    public static WindowFunctionExpression<Long> rowNumber(WindowSpec windowSpec) {
        return new WindowFunctionExpression<>(context -> "rowNumber()", windowSpec, Long.class);
    }
}
