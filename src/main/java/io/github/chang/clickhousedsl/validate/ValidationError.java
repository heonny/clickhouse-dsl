package io.github.chang.clickhousedsl.validate;

import java.util.Objects;

public final class ValidationError {

    private final String code;
    private final String message;

    public ValidationError(String code, String message) {
        this.code = Objects.requireNonNull(code, "code");
        this.message = Objects.requireNonNull(message, "message");
    }

    public String code() {
        return code;
    }

    public String message() {
        return message;
    }
}
