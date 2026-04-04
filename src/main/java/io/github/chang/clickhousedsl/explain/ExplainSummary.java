package io.github.chang.clickhousedsl.explain;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class ExplainSummary {

    private final boolean readsFromStorage;
    private final boolean hasFilter;
    private final boolean hasPrewhere;
    private final boolean hasJoin;
    private final boolean hasAggregation;
    private final boolean hasSorting;
    private final List<String> notes;

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

    public boolean readsFromStorage() {
        return readsFromStorage;
    }

    public boolean hasFilter() {
        return hasFilter;
    }

    public boolean hasPrewhere() {
        return hasPrewhere;
    }

    public boolean hasJoin() {
        return hasJoin;
    }

    public boolean hasAggregation() {
        return hasAggregation;
    }

    public boolean hasSorting() {
        return hasSorting;
    }

    public List<String> notes() {
        return notes;
    }
}
