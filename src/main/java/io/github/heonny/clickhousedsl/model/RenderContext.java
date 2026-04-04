package io.github.heonny.clickhousedsl.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class RenderContext {

    private final List<Object> parameters = new ArrayList<>();

    public String addParameter(Object value) {
        parameters.add(value);
        return "?";
    }

    public List<Object> parameters() {
        return Collections.unmodifiableList(parameters);
    }
}
