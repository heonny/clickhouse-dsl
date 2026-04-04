package io.github.heonny.clickhousedsl.model;

import java.util.Objects;

public final class Setting {

    private final Identifier name;
    private final Object value;

    private Setting(Identifier name, Object value) {
        this.name = Objects.requireNonNull(name, "name");
        this.value = value;
    }

    public static Setting of(String name, Object value) {
        return new Setting(Identifier.of(name), value);
    }

    public Identifier name() {
        return name;
    }

    public Object value() {
        return value;
    }
}
