package io.github.heonny.clickhousedsl.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class FunctionExpression<T> implements Expression<T> {

    private final String name;
    private final List<Expression<?>> arguments;
    private final Class<T> type;
    private final boolean aggregate;

    public FunctionExpression(String name, List<Expression<?>> arguments, Class<T> type, boolean aggregate) {
        this.name = Objects.requireNonNull(name, "name");
        this.arguments = List.copyOf(Objects.requireNonNull(arguments, "arguments"));
        this.type = Objects.requireNonNull(type, "type");
        this.aggregate = aggregate;
    }

    @Override
    public Class<T> type() {
        return type;
    }

    @Override
    public String render(RenderContext context) {
        StringBuilder builder = new StringBuilder(name).append('(');
        for (int i = 0; i < arguments.size(); i++) {
            builder.append(arguments.get(i).render(context));
            if (i + 1 < arguments.size()) {
                builder.append(", ");
            }
        }
        builder.append(')');
        return builder.toString();
    }

    @Override
    public boolean aggregate() {
        return aggregate;
    }

    public static <T> FunctionExpression<T> of(String name, Class<T> type, boolean aggregate, Expression<?>... arguments) {
        List<Expression<?>> items = new ArrayList<>();
        java.util.Collections.addAll(items, arguments);
        return new FunctionExpression<>(name, items, type, aggregate);
    }
}
