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
     * @param clause stable query clause identifier
     * @param message human-readable message
     * @param detail optional additional detail
     */
    public void add(String code, ValidationClause clause, String message, String detail) {
        errors.add(new ValidationError(code, clause, message, detail));
    }

    /**
     * Adds a validation error using a stable code definition.
     *
     * @param code validation code
     */
    public void add(ValidationCode code) {
        add(code, null);
    }

    /**
     * Adds a validation error using a stable code definition plus optional detail.
     *
     * @param code validation code
     * @param detail optional additional detail
     */
    public void add(ValidationCode code, String detail) {
        errors.add(new ValidationError(code.name(), code.clause(), code.defaultMessage(), detail));
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

    /**
     * Throws a structured exception when validation failed.
     *
     * @throws QueryValidationException when the result is invalid
     */
    public void throwIfInvalid() {
        if (!valid()) {
            throw new QueryValidationException(this);
        }
    }
}
