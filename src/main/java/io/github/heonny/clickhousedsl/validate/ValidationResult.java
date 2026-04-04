package io.github.heonny.clickhousedsl.validate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ValidationResult {

    private final List<ValidationError> errors = new ArrayList<>();

    public void add(String code, String message) {
        errors.add(new ValidationError(code, message));
    }

    public boolean valid() {
        return errors.isEmpty();
    }

    public List<ValidationError> errors() {
        return Collections.unmodifiableList(errors);
    }
}
