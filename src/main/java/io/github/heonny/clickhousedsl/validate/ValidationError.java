package io.github.heonny.clickhousedsl.validate;

import java.util.Objects;

/**
 * Single semantic validation error.
 */
public final class ValidationError {

    private final String code;
    private final String message;

    /**
     * Creates a validation error.
     *
     * @param code stable machine-readable error code
     * @param message human-readable message
     */
    public ValidationError(String code, String message) {
        this.code = Objects.requireNonNull(code, "code");
        this.message = Objects.requireNonNull(message, "message");
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
     * Returns the human-readable error message.
     *
     * @return error message
     */
    public String message() {
        return message;
    }
}
