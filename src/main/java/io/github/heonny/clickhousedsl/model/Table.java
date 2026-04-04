package io.github.heonny.clickhousedsl.model;

import java.util.Objects;

/**
 * Immutable table reference used by the DSL.
 *
 * <p>A table stores:
 *
 * <ul>
 *   <li>the physical table identifier
 *   <li>an optional query-local alias
 *   <li>whether the ClickHouse {@code FINAL} modifier is enabled
 * </ul>
 */
public final class Table {

    private final Identifier name;
    private final Identifier alias;
    private final boolean finalModifier;

    private Table(Identifier name, Identifier alias, boolean finalModifier) {
        this.name = name;
        this.alias = alias;
        this.finalModifier = finalModifier;
    }

    /**
     * Creates a table reference from a validated identifier.
     *
     * @param name table name, optionally schema-qualified
     * @return table reference
     */
    public static Table of(String name) {
        return new Table(Identifier.of(name), null, false);
    }

    /**
     * Returns a copy of this table with an alias.
     *
     * @param alias query-local alias
     * @return aliased table reference
     */
    public Table as(String alias) {
        return new Table(name, Identifier.of(alias), finalModifier);
    }

    /**
     * Returns a copy of this table with the ClickHouse {@code FINAL} modifier enabled.
     *
     * @return finalized table reference
     */
    public Table finalTable() {
        return new Table(name, alias, true);
    }

    /**
     * Creates a typed column reference.
     *
     * @param columnName column name
     * @param type column type
     * @param <T> column type
     * @return column reference
     */
    public <T> Column<T> column(String columnName, Class<T> type) {
        return new Column<>(this, Identifier.of(columnName), type);
    }

    /**
     * Creates an array column reference.
     *
     * @param columnName column name
     * @param elementType array element type
     * @param <E> element type
     * @return array column reference
     */
    public <E> ArrayColumn<E> arrayColumn(String columnName, Class<E> elementType) {
        return new ArrayColumn<>(this, Identifier.of(columnName), elementType);
    }

    /**
     * Creates an aggregate-state column reference.
     *
     * @param columnName column name
     * @param valueType merged value type
     * @param <T> merged value type
     * @return state column reference
     */
    public <T> StateColumn<T> stateColumn(String columnName, Class<T> valueType) {
        return new StateColumn<>(this, Identifier.of(columnName), valueType);
    }

    /**
     * Returns the physical table identifier.
     *
     * @return table identifier
     */
    public Identifier name() {
        return name;
    }

    /**
     * Returns the optional alias identifier.
     *
     * @return alias identifier or {@code null}
     */
    public Identifier alias() {
        return alias;
    }

    /**
     * Returns whether the FINAL modifier is enabled.
     *
     * @return {@code true} when FINAL is enabled
     */
    public boolean finalModifier() {
        return finalModifier;
    }

    /**
     * Returns the identifier used when the table is referenced from expressions.
     *
     * @return reference SQL fragment
     */
    public String renderReference() {
        return alias != null ? alias.sql() : name.sql();
    }

    /**
     * Returns the table fragment used in the FROM or JOIN clause.
     *
     * @return FROM/JOIN SQL fragment
     */
    public String renderFromClause() {
        StringBuilder builder = new StringBuilder(name.sql());
        // FINAL must appear before the alias in ClickHouse's FROM clause.
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
