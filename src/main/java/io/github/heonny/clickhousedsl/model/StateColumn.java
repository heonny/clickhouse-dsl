package io.github.heonny.clickhousedsl.model;

/**
 * Column subtype for stored aggregate-state values.
 *
 * @param <T> merged value type
 */
public final class StateColumn<T> extends Column<AggregateState<T>> {

    private final Class<T> valueType;

    StateColumn(Table table, Identifier name, Class<T> valueType) {
        super(table, name, aggregateStateClass());
        this.valueType = valueType;
    }

    /**
     * Returns the merged value type associated with the stored state.
     *
     * @return merged value type
     */
    public Class<T> valueType() {
        return valueType;
    }

    @SuppressWarnings("unchecked")
    private static <T> Class<AggregateState<T>> aggregateStateClass() {
        return (Class<AggregateState<T>>) (Class<?>) AggregateState.class;
    }
}
