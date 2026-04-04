package io.github.heonny.clickhousedsl.validate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Mutable accumulator used by semantic validation.
 */
public final class ValidationResult {

    private final List<ValidationError> errors = new ArrayList<>();

    /**
     * Adds a validation error.
     *
     * @param code stable error code
     * @param message human-readable message
     */
    public void add(String code, String message) {
        errors.add(new ValidationError(code, message));
    }

    /**
     * Returns whether no validation errors were recorded.
     *
     * @return {@code true} when the result is valid
     */
    public boolean valid() {
        return errors.isEmpty();
    }

    /**
     * Returns the collected validation errors.
     *
     * @return immutable validation error list
     */
    public List<ValidationError> errors() {
        return Collections.unmodifiableList(errors);
    }
}
