package io.github.heonny.clickhousedsl.explain;

import java.util.Objects;

/**
 * Result of explain analysis.
 */
public final class ExplainResult {

    private final ExplainType type;
    private final String raw;
    private final ExplainSummary summary;

    /**
     * Creates an explain result.
     *
     * @param type explain mode
     * @param raw raw explain output
     * @param summary summarized explain output
     */
    public ExplainResult(ExplainType type, String raw, ExplainSummary summary) {
        this.type = Objects.requireNonNull(type, "type");
        this.raw = Objects.requireNonNull(raw, "raw");
        this.summary = Objects.requireNonNull(summary, "summary");
    }

    /**
     * Returns the explain mode.
     *
     * @return explain mode
     */
    public ExplainType type() {
        return type;
    }

    /**
     * Returns the original raw explain text.
     *
     * @return raw explain output
     */
    public String raw() {
        return raw;
    }

    /**
     * Returns the summarized view of the explain output.
     *
     * @return explain summary
     */
    public ExplainSummary summary() {
        return summary;
    }
}
