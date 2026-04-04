package io.github.heonny.clickhousedsl.model;

import java.util.Objects;

/**
 * Inline literal expression.
 *
 * @param <T> literal type
 */
public final class LiteralExpression<T> implements Expression<T> {

    private final T value;
    private final Class<T> type;

    /**
     * Creates a literal expression.
     *
     * @param value literal value
     * @param type literal type
     */
    public LiteralExpression(T value, Class<T> type) {
        this.value = value;
        this.type = Objects.requireNonNull(type, "type");
    }

    @Override
    public Class<T> type() {
        return type;
    }

    @Override
    public String render(RenderContext context) {
        // Strings and characters are escaped in-place because literals render directly into SQL.
        if (value == null) {
            return "NULL";
        }
        if (value instanceof String string) {
            return "'" + string.replace("'", "''") + "'";
        }
        if (value instanceof Character character) {
            return "'" + character.toString().replace("'", "''") + "'";
        }
        if (value instanceof Boolean bool) {
            return bool ? "1" : "0";
        }
        return String.valueOf(value);
    }
}
