package io.github.heonny.clickhousedsl.validate;

import java.util.List;
import java.util.Objects;

/**
 * Exception thrown when a query fails semantic validation in fail-fast mode.
 */
public final class QueryValidationException extends IllegalArgumentException {

    /** Full validation result preserved for callers that need structured inspection. */
    private final ValidationResult validationResult;

    /**
     * Creates a validation exception.
     *
     * @param validationResult failed validation result
     */
    public QueryValidationException(ValidationResult validationResult) {
        super(buildMessage(validationResult));
        this.validationResult = Objects.requireNonNull(validationResult, "validationResult");
    }

    /**
     * Returns the full validation result that caused the exception.
     *
     * @return validation result
     */
    public ValidationResult validationResult() {
        return validationResult;
    }

    private static String buildMessage(ValidationResult validationResult) {
        Objects.requireNonNull(validationResult, "validationResult");
        List<ValidationError> errors = validationResult.errors();
        if (errors.isEmpty()) {
            return "Query validation failed.";
        }
        ValidationError firstError = errors.get(0);
        String baseMessage = "Query validation failed with "
            + errors.size()
            + " error(s). First error ["
            + firstError.code()
            + "] in "
            + firstError.clause()
            + ": "
            + firstError.message();
        if (firstError.detail() == null || firstError.detail().isBlank()) {
            return baseMessage;
        }
        return baseMessage + " Detail: " + firstError.detail();
    }
}
