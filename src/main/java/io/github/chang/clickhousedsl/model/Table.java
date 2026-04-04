package io.github.chang.clickhousedsl.model;

import java.util.Objects;

public final class Table {

    private final Identifier name;
    private final Identifier alias;
    private final boolean finalModifier;

    private Table(Identifier name, Identifier alias, boolean finalModifier) {
        this.name = name;
        this.alias = alias;
        this.finalModifier = finalModifier;
    }

    public static Table of(String name) {
        return new Table(Identifier.of(name), null, false);
    }

    public Table as(String alias) {
        return new Table(name, Identifier.of(alias), finalModifier);
    }

    public Table finalTable() {
        return new Table(name, alias, true);
    }

    public <T> Column<T> column(String columnName, Class<T> type) {
        return new Column<>(this, Identifier.of(columnName), type);
    }

    public Identifier name() {
        return name;
    }

    public Identifier alias() {
        return alias;
    }

    public boolean finalModifier() {
        return finalModifier;
    }

    public String renderReference() {
        return alias != null ? alias.sql() : name.sql();
    }

    public String renderFromClause() {
        StringBuilder builder = new StringBuilder(name.sql());
        if (finalModifier) {
            builder.append(" FINAL");
        }
        if (alias != null) {
            builder.append(" AS ").append(alias.sql());
        }
        return builder.toString();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Table table)) {
            return false;
        }
        return finalModifier == table.finalModifier
            && name.equals(table.name)
            && Objects.equals(alias, table.alias);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, alias, finalModifier);
    }
}
