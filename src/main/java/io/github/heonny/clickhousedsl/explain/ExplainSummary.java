package io.github.heonny.clickhousedsl.explain;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Heuristic summary extracted from raw EXPLAIN output.
 */
public final class ExplainSummary {

    private final boolean readsFromStorage;
    private final boolean hasFilter;
    private final boolean hasPrewhere;
    private final boolean hasJoin;
    private final boolean hasAggregation;
    private final boolean hasSorting;
    private final List<String> notes;

    /**
     * Creates an explain summary.
     *
     * @param readsFromStorage whether a storage read was detected
     * @param hasFilter whether a filter stage was detected
     * @param hasPrewhere whether a prewhere stage was detected
     * @param hasJoin whether a join stage was detected
     * @param hasAggregation whether an aggregation stage was detected
     * @param hasSorting whether a sorting stage was detected
     * @param notes human-readable notes
     */
    public ExplainSummary(
        boolean readsFromStorage,
        boolean hasFilter,
        boolean hasPrewhere,
        boolean hasJoin,
        boolean hasAggregation,
        boolean hasSorting,
        List<String> notes
    ) {
        this.readsFromStorage = readsFromStorage;
        this.hasFilter = hasFilter;
        this.hasPrewhere = hasPrewhere;
        this.hasJoin = hasJoin;
        this.hasAggregation = hasAggregation;
        this.hasSorting = hasSorting;
        this.notes = Collections.unmodifiableList(Objects.requireNonNull(notes, "notes"));
    }

    /**
     * Returns whether the plan appears to read from storage.
     *
     * @return {@code true} when storage access was detected
     */
    public boolean readsFromStorage() {
        return readsFromStorage;
    }

    /**
     * Returns whether a filter stage was detected.
     *
     * @return {@code true} when a filter stage was detected
     */
    public boolean hasFilter() {
        return hasFilter;
    }

    /**
     * Returns whether a PREWHERE stage was detected.
     *
     * @return {@code true} when a PREWHERE stage was detected
     */
    public boolean hasPrewhere() {
        return hasPrewhere;
    }

    /**
     * Returns whether a join stage was detected.
     *
     * @return {@code true} when a join stage was detected
     */
    public boolean hasJoin() {
        return hasJoin;
    }

    /**
     * Returns whether an aggregation stage was detected.
     *
     * @return {@code true} when an aggregation stage was detected
     */
    public boolean hasAggregation() {
        return hasAggregation;
    }

    /**
     * Returns whether a sorting stage was detected.
     *
     * @return {@code true} when a sorting stage was detected
     */
    public boolean hasSorting() {
        return hasSorting;
    }

    /**
     * Returns human-readable notes derived from the raw explain text.
     *
     * @return immutable note list
     */
    public List<String> notes() {
        return notes;
    }
}
