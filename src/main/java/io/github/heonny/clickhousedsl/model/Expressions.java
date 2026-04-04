package io.github.heonny.clickhousedsl.model;

/**
 * Internal expression factory collection used by the public DSL.
 */
public final class Expressions {

    private Expressions() {
    }

    /**
     * Creates a placeholder-backed parameter expression.
     *
     * @param value runtime parameter value
     * @param type parameter type
     * @param <T> parameter type
     * @return parameter expression
     */
    public static <T> ParameterExpression<T> param(T value, Class<T> type) {
        return new ParameterExpression<>(value, type);
    }

    /**
     * Creates an inline literal expression.
     *
     * @param value literal value
     * @param type literal type
     * @param <T> literal type
     * @return literal expression
     */
    public static <T> LiteralExpression<T> literal(T value, Class<T> type) {
        return new LiteralExpression<>(value, type);
    }

    /**
     * Creates {@code count()}.
     *
     * @return aggregate expression
     */
    public static AggregateExpression<Long> count() {
        return new AggregateExpression<>("count()", Long.class);
    }

    /**
     * Creates {@code sum(...)}.
     *
     * @param expression numeric expression
     * @param <N> numeric result type
     * @return aggregate expression
     */
    public static <N extends Number> AggregateExpression<N> sum(Expression<N> expression) {
        return new AggregateExpression<>(context -> "sum(" + expression.render(context) + ")", expression.type());
    }

    /**
     * Creates {@code sumState(...)}.
     *
     * @param expression numeric expression
     * @param <N> merged numeric type
     * @return aggregate state expression
     */
    public static <N extends Number> AggregateStateExpression<N> sumState(Expression<N> expression) {
        return new AggregateStateExpression<>(context -> "sumState(" + expression.render(context) + ")", expression.type());
    }

    /**
     * Creates {@code sumMerge(...)} from a state expression.
     *
     * @param expression state expression
     * @param <N> merged numeric type
     * @return aggregate expression
     */
    public static <N extends Number> AggregateExpression<N> sumMerge(AggregateStateExpression<N> expression) {
        return new AggregateExpression<>(context -> "sumMerge(" + expression.render(context) + ")", expression.valueType());
    }

    /**
     * Creates {@code countMerge(...)}.
     *
     * @param expression count-state expression
     * @return aggregate expression
     */
    public static AggregateExpression<Long> countMerge(Expression<AggregateState<Long>> expression) {
        return new AggregateExpression<>(context -> "countMerge(" + expression.render(context) + ")", Long.class);
    }

    /**
     * Creates {@code countIfMerge(...)}.
     *
     * @param expression conditional count-state expression
     * @return aggregate expression
     */
    public static AggregateExpression<Long> countIfMerge(Expression<AggregateState<Long>> expression) {
        return new AggregateExpression<>(context -> "countIfMerge(" + expression.render(context) + ")", Long.class);
    }

    /**
     * Creates {@code uniqMerge(...)}.
     *
     * @param expression uniqueness-state expression
     * @return aggregate expression
     */
    public static AggregateExpression<Long> uniqMerge(Expression<AggregateState<Long>> expression) {
        return new AggregateExpression<>(context -> "uniqMerge(" + expression.render(context) + ")", Long.class);
    }

    /**
     * Creates {@code sumMerge(...)} from a state-typed expression.
     *
     * @param expression state expression
     * @param valueType merged numeric type
     * @param <N> merged numeric type
     * @return aggregate expression
     */
    public static <N extends Number> AggregateExpression<N> sumMerge(Expression<AggregateState<N>> expression, Class<N> valueType) {
        return new AggregateExpression<>(context -> "sumMerge(" + expression.render(context) + ")", valueType);
    }

    /**
     * Creates a generic scalar function expression.
     *
     * @param name function name
     * @param type result type
     * @param arguments function arguments
     * @param <T> result type
     * @return function expression
     */
    public static <T> FunctionExpression<T> function(String name, Class<T> type, Expression<?>... arguments) {
        return FunctionExpression.of(name, type, false, arguments);
    }

    /**
     * Creates a generic aggregate function expression.
     *
     * @param name function name
     * @param type result type
     * @param arguments function arguments
     * @param <T> result type
     * @return aggregate function expression
     */
    public static <T> FunctionExpression<T> aggregateFunction(String name, Class<T> type, Expression<?>... arguments) {
        return FunctionExpression.of(name, type, true, arguments);
    }

    /**
     * Creates a division expression.
     *
     * @param left left operand
     * @param right right operand
     * @return arithmetic expression
     */
    public static BinaryArithmeticExpression<Double> divide(Expression<?> left, Expression<?> right) {
        return new BinaryArithmeticExpression<>(left, "/", right, Double.class, left.aggregate() || right.aggregate());
    }

    /**
     * Creates a typed alias/reference expression.
     *
     * @param identifier identifier or alias
     * @param type reference type
     * @param <T> reference type
     * @return reference expression
     */
    public static <T> ReferenceExpression<T> ref(String identifier, Class<T> type) {
        return new ReferenceExpression<>(identifier, type);
    }

    /**
     * Creates {@code rowNumber() OVER (...)}.
     *
     * @param windowSpec window specification
     * @return window function expression
     */
    public static WindowFunctionExpression<Long> rowNumber(WindowSpec windowSpec) {
        return new WindowFunctionExpression<>(context -> "rowNumber()", windowSpec, Long.class);
    }
}
