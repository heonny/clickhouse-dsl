package io.github.chang.clickhousedsl.model;

public final class StateColumn<T> extends Column<AggregateState<T>> {

    private final Class<T> valueType;

    StateColumn(Table table, Identifier name, Class<T> valueType) {
        super(table, name, aggregateStateClass());
        this.valueType = valueType;
    }

    public Class<T> valueType() {
        return valueType;
    }

    @SuppressWarnings("unchecked")
    private static <T> Class<AggregateState<T>> aggregateStateClass() {
        return (Class<AggregateState<T>>) (Class<?>) AggregateState.class;
    }
}
