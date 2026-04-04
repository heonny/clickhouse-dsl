package io.github.heonny.clickhousedsl.model;

import java.util.List;

/**
 * Column subtype used for ClickHouse array-typed columns.
 *
 * @param <E> element type stored inside the array
 */
public final class ArrayColumn<E> extends Column<List<E>> {

    private final Class<E> elementType;

    ArrayColumn(Table table, Identifier name, Class<E> elementType) {
        super(table, name, listClass());
        this.elementType = elementType;
    }

    /**
     * Returns the array element type declared by the caller.
     *
     * @return element type
     */
    public Class<E> elementType() {
        return elementType;
    }

    @SuppressWarnings("unchecked")
    private static <E> Class<List<E>> listClass() {
        return (Class<List<E>>) (Class<?>) List.class;
    }
}
