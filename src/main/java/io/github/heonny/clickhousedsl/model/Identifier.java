package io.github.heonny.clickhousedsl.model;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Validated SQL identifier wrapper.
 *
 * <p>Only simple safe identifier parts are accepted so callers cannot inject raw SQL through table,
 * column, alias, or setting names.
 */
public final class Identifier {

    private static final Pattern PART_PATTERN = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");

    private final String value;

    private Identifier(String value) {
        this.value = value;
    }

    /**
     * Creates a validated identifier.
     *
     * @param value identifier, optionally dot-qualified
     * @return validated identifier
     */
    public static Identifier of(String value) {
        Objects.requireNonNull(value, "value");
        List<String> parts = Arrays.asList(value.split("\\."));
        if (parts.isEmpty()) {
            throw new IllegalArgumentException("Identifier must not be empty");
        }
        for (String part : parts) {
            if (!PART_PATTERN.matcher(part).matches()) {
                throw new IllegalArgumentException("Unsafe identifier: " + value);
            }
        }
        return new Identifier(value);
    }

    /**
     * Renders the identifier using backtick quoting for each part.
     *
     * @return quoted SQL identifier
     */
    public String sql() {
        String[] parts = value.split("\\.");
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                builder.append('.');
            }
            builder.append('`').append(parts[i]).append('`');
        }
        return builder.toString();
    }

    /**
     * Returns the original validated identifier value.
     *
     * @return raw identifier value
     */
    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Identifier identifier)) {
            return false;
        }
        return value.equals(identifier.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return value;
    }
}
