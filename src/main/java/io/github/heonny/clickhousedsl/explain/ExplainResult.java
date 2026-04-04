package io.github.heonny.clickhousedsl.explain;

import java.util.Objects;

public final class ExplainResult {

    private final ExplainType type;
    private final String raw;
    private final ExplainSummary summary;

    public ExplainResult(ExplainType type, String raw, ExplainSummary summary) {
        this.type = Objects.requireNonNull(type, "type");
        this.raw = Objects.requireNonNull(raw, "raw");
        this.summary = Objects.requireNonNull(summary, "summary");
    }

    public ExplainType type() {
        return type;
    }

    public String raw() {
        return raw;
    }

    public ExplainSummary summary() {
        return summary;
    }
}
