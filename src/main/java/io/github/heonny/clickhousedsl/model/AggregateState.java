package io.github.heonny.clickhousedsl.model;

/**
 * Marker type representing a ClickHouse aggregate state value.
 *
 * @param <T> merged value type produced when the state is finalized
 */
public final class AggregateState<T> {
    private AggregateState() {
    }
}
