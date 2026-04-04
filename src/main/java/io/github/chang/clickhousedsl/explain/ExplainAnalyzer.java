package io.github.chang.clickhousedsl.explain;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ExplainAnalyzer {

    public ExplainResult analyze(ExplainType type, String rawOutput) {
        String normalized = rawOutput.toLowerCase(Locale.ROOT);
        boolean readsFromStorage = normalized.contains("readfromstorage") || normalized.contains("read from");
        boolean hasFilter = normalized.contains("filter");
        boolean hasPrewhere = normalized.contains("prewhere");
        boolean hasJoin = normalized.contains("join");
        boolean hasAggregation = normalized.contains("aggregat") || normalized.contains("group by");
        boolean hasSorting = normalized.contains("sorting") || normalized.contains("order by");

        List<String> notes = new ArrayList<>();
        if (readsFromStorage) {
            notes.add("Plan reads from storage.");
        }
        if (hasPrewhere) {
            notes.add("PREWHERE is present, early filtering may reduce scanned rows.");
        } else if (hasFilter) {
            notes.add("Filter detected without PREWHERE optimization.");
        }
        if (hasJoin) {
            notes.add("Join stage detected, inspect join cardinality and key types.");
        }
        if (hasAggregation) {
            notes.add("Aggregation stage detected, verify grouping cardinality.");
        }
        if (hasSorting) {
            notes.add("Sorting stage detected, inspect ordering cost.");
        }
        if (notes.isEmpty()) {
            notes.add("No known plan hints were detected.");
        }

        return new ExplainResult(
            type,
            rawOutput,
            new ExplainSummary(readsFromStorage, hasFilter, hasPrewhere, hasJoin, hasAggregation, hasSorting, notes)
        );
    }
}
