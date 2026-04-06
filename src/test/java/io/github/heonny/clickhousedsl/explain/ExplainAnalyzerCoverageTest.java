package io.github.heonny.clickhousedsl.explain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ExplainAnalyzerCoverageTest {

    private final ExplainAnalyzer analyzer = new ExplainAnalyzer();

    @Test
    void detectsReadFromStorageGroupByAndOrderByVariants() {
        ExplainResult result = analyzer.analyze(
            ExplainType.PLAN,
            "Read From storage\nGROUP BY key\nORDER BY created_at"
        );

        assertThat(result.summary().readsFromStorage()).isTrue();
        assertThat(result.summary().hasAggregation()).isTrue();
        assertThat(result.summary().hasSorting()).isTrue();
        assertThat(result.summary().notes())
            .contains("Plan reads from storage.")
            .contains("Aggregation stage detected, verify grouping cardinality.")
            .contains("Sorting stage detected, inspect ordering cost.");
    }

    @Test
    void fallsBackToGenericNoHintsMessageWhenNothingMatches() {
        ExplainResult result = analyzer.analyze(ExplainType.PLAN, "Projection node only");

        assertThat(result.summary().readsFromStorage()).isFalse();
        assertThat(result.summary().hasFilter()).isFalse();
        assertThat(result.summary().hasPrewhere()).isFalse();
        assertThat(result.summary().hasJoin()).isFalse();
        assertThat(result.summary().hasAggregation()).isFalse();
        assertThat(result.summary().hasSorting()).isFalse();
        assertThat(result.summary().notes()).containsExactly("No known plan hints were detected.");
    }
}
