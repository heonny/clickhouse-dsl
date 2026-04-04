package io.github.heonny.clickhousedsl.model;

/**
 * Execution-side metrics that may be populated by a future executor implementation.
 */
public final class ExecutionMetrics {

    private final Long maxMemoryUsageBytes;
    private final Integer usedThreads;

    /**
     * Creates execution metrics.
     *
     * @param maxMemoryUsageBytes observed maximum memory usage in bytes, if known
     * @param usedThreads observed thread usage, if known
     */
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

    /**
     * Returns the observed maximum memory usage in bytes.
     *
     * @return max memory usage or {@code null} when not available
     */
    public Long maxMemoryUsageBytes() {
        return maxMemoryUsageBytes;
    }

    /**
     * Returns the observed thread usage.
     *
     * @return used threads or {@code null} when not available
     */
    public Integer usedThreads() {
        return usedThreads;
    }
}
