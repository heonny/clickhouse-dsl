package io.github.heonny.clickhousedsl.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Internal formatter for debug-only SQL interpolation.
 */
final class DebugSqlFormatter {

    private static final DateTimeFormatter LOCAL_DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter LOCAL_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private DebugSqlFormatter() {
    }

    static String format(String sql, List<Object> parameters) {
        StringBuilder result = new StringBuilder(sql.length() + (parameters.size() * 8));
        int parameterIndex = 0;
        boolean inStringLiteral = false;

        for (int index = 0; index < sql.length(); index++) {
            char current = sql.charAt(index);

            if (current == '\'') {
                result.append(current);
                if (inStringLiteral && index + 1 < sql.length() && sql.charAt(index + 1) == '\'') {
                    result.append(sql.charAt(index + 1));
                    index++;
                    continue;
                }
                inStringLiteral = !inStringLiteral;
                continue;
            }

            if (current == '?' && !inStringLiteral) {
                if (parameterIndex >= parameters.size()) {
                    throw new IllegalStateException("Not enough parameters to render debug SQL");
                }
                result.append(formatValue(parameters.get(parameterIndex++)));
                continue;
            }

            result.append(current);
        }

        if (parameterIndex != parameters.size()) {
            throw new IllegalStateException("Too many parameters to render debug SQL");
        }

        return result.toString();
    }

    private static String formatValue(Object value) {
        if (value == null) {
            return "NULL";
        }
        if (value instanceof String string) {
            return quote(string);
        }
        if (value instanceof Character character) {
            return quote(character.toString());
        }
        if (value instanceof Boolean bool) {
            return bool ? "1" : "0";
        }
        if (value instanceof Number) {
            return String.valueOf(value);
        }
        if (value instanceof LocalDate localDate) {
            return quote(LOCAL_DATE_FORMATTER.format(localDate));
        }
        if (value instanceof LocalDateTime localDateTime) {
            return quote(LOCAL_DATE_TIME_FORMATTER.format(localDateTime));
        }
        return quote(String.valueOf(value));
    }

    private static String quote(String value) {
        return "'" + value.replace("'", "''") + "'";
    }
}
