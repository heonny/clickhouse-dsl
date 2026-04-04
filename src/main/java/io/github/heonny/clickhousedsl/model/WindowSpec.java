package io.github.heonny.clickhousedsl.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class WindowSpec {

    private final List<Expression<?>> partitionBy;
    private final List<Sort> orderBy;

    private WindowSpec(List<Expression<?>> partitionBy, List<Sort> orderBy) {
        this.partitionBy = Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNull(partitionBy, "partitionBy")));
        this.orderBy = Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNull(orderBy, "orderBy")));
    }

    public static WindowSpec empty() {
        return new WindowSpec(List.of(), List.of());
    }

    public WindowSpec partitionBy(Expression<?>... expressions) {
        List<Expression<?>> next = new ArrayList<>(partitionBy);
        Collections.addAll(next, expressions);
        return new WindowSpec(next, orderBy);
    }

    public WindowSpec orderBy(Sort... sorts) {
        List<Sort> next = new ArrayList<>(orderBy);
        Collections.addAll(next, sorts);
        return new WindowSpec(partitionBy, next);
    }

    public List<Expression<?>> partitionBy() {
        return partitionBy;
    }

    public List<Sort> orderBy() {
        return orderBy;
    }

    public String render(RenderContext context) {
        StringBuilder builder = new StringBuilder();
        if (!partitionBy.isEmpty()) {
            builder.append("PARTITION BY ");
            appendExpressions(builder, partitionBy, context);
        }
        if (!orderBy.isEmpty()) {
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append("ORDER BY ");
            for (int i = 0; i < orderBy.size(); i++) {
                Sort sort = orderBy.get(i);
                builder.append(sort.expression().render(context)).append(' ').append(sort.direction().name());
                if (i + 1 < orderBy.size()) {
                    builder.append(", ");
                }
            }
        }
        return builder.toString();
    }

    private void appendExpressions(StringBuilder builder, List<Expression<?>> expressions, RenderContext context) {
        for (int i = 0; i < expressions.size(); i++) {
            builder.append(expressions.get(i).render(context));
            if (i + 1 < expressions.size()) {
                builder.append(", ");
            }
        }
    }
}
