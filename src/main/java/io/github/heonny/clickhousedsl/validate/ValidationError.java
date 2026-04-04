package io.github.heonny.clickhousedsl.validate;

import java.util.Objects;

/**
 * Single semantic validation error.
 */
public final class ValidationError {

    private final String code;
    private final ValidationClause clause;
    private final String message;
    private final String detail;

    /**
     * Creates a validation error.
     *
     * @param code stable machine-readable error code
     * @param clause stable query clause identifier
     * @param message human-readable message
     * @param detail optional additional detail for logs or debugging
     */
    public ValidationError(String code, ValidationClause clause, String message, String detail) {
        this.code = Objects.requireNonNull(code, "code");
        this.clause = Objects.requireNonNull(clause, "clause");
        this.message = Objects.requireNonNull(message, "message");
        this.detail = detail;
    }

    /**
     * Returns the stable error code.
     *
     * @return error code
     */
    public String code() {
        return code;
    }

    /**
     * Returns the query clause associated with the validation failure.
     *
     * @return validation clause
     */
    public ValidationClause clause() {
        return clause;
    }

    /**
     * Returns the human-readable error message.
     *
     * @return error message
     */
    public String message() {
        return message;
    }

    /**
     * Returns optional additional detail about the failure.
     *
     * @return detail or {@code null}
     */
    public String detail() {
        return detail;
    }

    @Override
    public String toString() {
        return code + "[" + clause + "]: " + message + (detail == null ? "" : " (" + detail + ")");
    }
}
