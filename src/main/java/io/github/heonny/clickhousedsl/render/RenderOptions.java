package io.github.heonny.clickhousedsl.render;

/**
 * Immutable render options for SQL output formatting.
 */
public final class RenderOptions {

    private static final RenderOptions COMPACT = new RenderOptions(false);
    private static final RenderOptions PRETTY = new RenderOptions(true);

    private final boolean pretty;

    private RenderOptions(boolean pretty) {
        this.pretty = pretty;
    }

    /**
     * Returns compact single-line rendering options.
     *
     * @return compact render options
     */
    public static RenderOptions compact() {
        return COMPACT;
    }

    /**
     * Returns multi-line rendering options for debugging and readability.
     *
     * @return pretty render options
     */
    public static RenderOptions pretty() {
        return PRETTY;
    }

    /**
     * Returns whether pretty multi-line formatting is enabled.
     *
     * @return {@code true} when pretty formatting is enabled
     */
    public boolean prettyPrint() {
        return pretty;
    }
}
