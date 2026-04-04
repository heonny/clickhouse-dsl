package io.github.heonny.clickhousedsl.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Immutable window specification used by window functions.
 */
public final class WindowSpec {

    private final List<Expression<?>> partitionBy;
    private final List<Sort> orderBy;

    private WindowSpec(List<Expression<?>> partitionBy, List<Sort> orderBy) {
        this.partitionBy = Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNull(partitionBy, "partitionBy")));
        this.orderBy = Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNull(orderBy, "orderBy")));
    }

    /**
     * Creates an empty window specification.
     *
     * @return empty spec
     */
    public static WindowSpec empty() {
        return new WindowSpec(List.of(), List.of());
    }

    /**
     * Returns a copy with additional partition expressions.
     *
     * @param expressions partition expressions
     * @return updated window spec
     */
    public WindowSpec partitionBy(Expression<?>... expressions) {
        Objects.requireNonNull(expressions, "expressions");
        List<Expression<?>> next = new ArrayList<>(partitionBy);
        Collections.addAll(next, expressions);
        for (Expression<?> expression : next) {
            Objects.requireNonNull(expression, "expressions must not contain null");
        }
        return new WindowSpec(next, orderBy);
    }

    /**
     * Returns a copy with additional sort expressions.
     *
     * @param sorts sort descriptors
     * @return updated window spec
     */
    public WindowSpec orderBy(Sort... sorts) {
        Objects.requireNonNull(sorts, "sorts");
        List<Sort> next = new ArrayList<>(orderBy);
        Collections.addAll(next, sorts);
        for (Sort sort : next) {
            Objects.requireNonNull(sort, "sorts must not contain null");
        }
        return new WindowSpec(partitionBy, next);
    }

    /**
     * Returns the partition expressions used by the window.
     *
     * @return window partitions
     */
    public List<Expression<?>> partitionBy() {
        return partitionBy;
    }

    /**
     * Returns the order-by descriptors used by the window.
     *
     * @return window ordering
     */
    public List<Sort> orderBy() {
        return orderBy;
    }

    /**
     * Renders the window specification body without the {@code OVER} keyword.
     *
     * @param context render context
     * @return rendered window specification
     */
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
