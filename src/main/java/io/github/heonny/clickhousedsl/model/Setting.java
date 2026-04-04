package io.github.heonny.clickhousedsl.model;

import java.util.Objects;

/**
 * ClickHouse {@code SETTINGS} entry.
 */
public final class Setting {

    private final Identifier name;
    private final Object value;

    private Setting(Identifier name, Object value) {
        this.name = Objects.requireNonNull(name, "name");
        this.value = Objects.requireNonNull(value, "value");
    }

    /**
     * Creates a setting from a validated identifier and value.
     *
     * @param name setting name
     * @param value setting value
     * @return setting
     */
    public static Setting of(String name, Object value) {
        return new Setting(Identifier.of(name), value);
    }

    /**
     * Returns the setting identifier.
     *
     * @return setting identifier
     */
    public Identifier name() {
        return name;
    }

    /**
     * Returns the setting value.
     *
     * @return setting value
     */
    public Object value() {
        return value;
    }
}
