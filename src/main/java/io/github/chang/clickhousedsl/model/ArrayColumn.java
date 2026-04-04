package io.github.chang.clickhousedsl.model;

import java.util.List;

public final class ArrayColumn<E> extends Column<List<E>> {

    private final Class<E> elementType;

    ArrayColumn(Table table, Identifier name, Class<E> elementType) {
        super(table, name, listClass());
        this.elementType = elementType;
    }

    public Class<E> elementType() {
        return elementType;
    }

    @SuppressWarnings("unchecked")
    private static <E> Class<List<E>> listClass() {
        return (Class<List<E>>) (Class<?>) List.class;
    }
}
