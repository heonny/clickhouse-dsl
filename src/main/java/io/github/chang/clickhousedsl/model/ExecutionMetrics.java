package io.github.chang.clickhousedsl.model;

public final class ExecutionMetrics {

    private final Long maxMemoryUsageBytes;
    private final Integer usedThreads;

    public ExecutionMetrics(Long maxMemoryUsageBytes, Integer usedThreads) {
        if (maxMemoryUsageBytes != null && maxMemoryUsageBytes < 0L) {
            throw new IllegalArgumentException("maxMemoryUsageBytes must be zero or positive");
        }
        if (usedThreads != null && usedThreads <= 0) {
            throw new IllegalArgumentException("usedThreads must be positive");
        }
        this.maxMemoryUsageBytes = maxMemoryUsageBytes;
        this.usedThreads = usedThreads;
    }

    public Long maxMemoryUsageBytes() {
        return maxMemoryUsageBytes;
    }

    public Integer usedThreads() {
        return usedThreads;
    }
}
