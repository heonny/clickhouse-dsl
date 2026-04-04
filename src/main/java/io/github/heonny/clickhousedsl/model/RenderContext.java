package io.github.heonny.clickhousedsl.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Shared rendering context that owns parameter collection.
 */
public final class RenderContext {

    private final List<Object> parameters = new ArrayList<>();

    /**
     * Adds a parameter and returns the placeholder token.
     *
     * @param value parameter value
     * @return placeholder token
     */
    public String addParameter(Object value) {
        parameters.add(value);
        return "?";
    }

    /**
     * Returns collected parameters in insertion order.
     *
     * @return immutable parameter list
     */
    public List<Object> parameters() {
        return Collections.unmodifiableList(parameters);
    }
}
