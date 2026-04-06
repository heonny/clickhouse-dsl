package io.github.heonny.clickhousedsl.render;

import io.github.heonny.clickhousedsl.api.ClickHouseDsl;
import io.github.heonny.clickhousedsl.model.Expression;
import io.github.heonny.clickhousedsl.model.Join;
import io.github.heonny.clickhousedsl.model.Query;
import io.github.heonny.clickhousedsl.model.RenderContext;
import io.github.heonny.clickhousedsl.model.RenderedQuery;
import io.github.heonny.clickhousedsl.model.SetOperation;
import io.github.heonny.clickhousedsl.model.Setting;
import io.github.heonny.clickhousedsl.model.Sort;
import io.github.heonny.clickhousedsl.model.WithClause;
import java.util.Iterator;
import java.util.Objects;

/**
 * Renders immutable query objects into ClickHouse SQL plus positional parameters.
 *
 * <p>The renderer walks the query in SQL clause order and delegates expression rendering to each
 * node. Placeholder values are accumulated in {@link RenderContext} so SQL text and parameter order
 * stay aligned.
 */
public final class ClickHouseRenderer {

    /**
     * Renders a query into a SQL string and ordered parameter list.
     *
     * @param query query to render
     * @return rendered SQL plus bound parameters
     */
    public RenderedQuery render(Query query) {
        return render(query, RenderOptions.compact());
    }

    /**
     * Renders a query into a SQL string and ordered parameter list using explicit render options.
     *
     * @param query query to render
     * @param options render options
     * @return rendered SQL plus bound parameters
     */
    public RenderedQuery render(Query query, RenderOptions options) {
        Objects.requireNonNull(query, "query");
        Objects.requireNonNull(options, "options");
        RenderContext context = new RenderContext();
        StringBuilder sql = new StringBuilder();
        if (options.prettyPrint()) {
            renderQueryPretty(sql, query, context);
        } else {
            renderQueryCompact(sql, query, context);
        }
        return new RenderedQuery(sql.toString(), context.parameters());
    }

    /**
     * Validates a query before rendering it.
     *
     * <p>This is the safest render entry point for integration boundaries where invalid queries
     * should fail immediately instead of leaking deeper into the calling application.
     *
     * @param query query to validate and render
     * @return rendered SQL plus bound parameters
     */
    public RenderedQuery renderValidated(Query query) {
        return renderValidated(query, RenderOptions.compact());
    }

    /**
     * Validates a query before rendering it with explicit render options.
     *
     * @param query query to validate and render
     * @param options render options
     * @return rendered SQL plus bound parameters
     */
    public RenderedQuery renderValidated(Query query, RenderOptions options) {
        ClickHouseDsl.validateOrThrow(query);
        return render(query, options);
    }

    private void renderQueryCompact(StringBuilder sql, Query query, RenderContext context) {
        if (!query.withClauses().isEmpty()) {
            sql.append("WITH ");
            Iterator<WithClause> iterator = query.withClauses().iterator();
            while (iterator.hasNext()) {
                WithClause withClause = iterator.next();
                sql.append(withClause.alias().sql()).append(" AS (");
                renderQueryBodyCompact(sql, withClause.query(), context);
                sql.append(")");
                if (iterator.hasNext()) {
                    sql.append(", ");
                }
            }
            sql.append(' ');
        }

        // The primary query body is rendered before any UNION / UNION ALL branches.
        renderQueryBodyCompact(sql, query, context);
        for (SetOperation setOperation : query.setOperations()) {
            sql.append(' ').append(setOperation.type().sql()).append(' ');
            renderQueryBodyCompact(sql, setOperation.query(), context);
        }
    }

    private void renderQueryBodyCompact(StringBuilder sql, Query query, RenderContext context) {
        sql.append("SELECT ");
        appendExpressions(sql, query.selections(), context);
        sql.append(" FROM ").append(query.from().renderFromClause());
        for (Join join : query.joins()) {
            sql.append(' ')
                .append(join.type().sql())
                .append(' ')
                .append(join.table().renderFromClause())
                .append(" ON ")
                .append(join.leftKey().render(context))
                .append(" = ")
                .append(join.rightKey().render(context));
        }

        if (!query.arrayJoins().isEmpty()) {
            sql.append(" ARRAY JOIN ");
            appendExpressions(sql, query.arrayJoins(), context);
        }
        if (query.sampleRatio() != null) {
            sql.append(" SAMPLE ").append(context.addParameter(query.sampleRatio()));
        }
        if (query.prewhere() != null) {
            sql.append(" PREWHERE ").append(query.prewhere().render(context));
        }
        if (query.where() != null) {
            sql.append(" WHERE ").append(query.where().render(context));
        }
        if (!query.groupBy().isEmpty()) {
            sql.append(" GROUP BY ");
            appendExpressions(sql, query.groupBy(), context);
        }
        if (query.having() != null) {
            sql.append(" HAVING ").append(query.having().render(context));
        }
        if (!query.orderBy().isEmpty()) {
            sql.append(" ORDER BY ");
            Iterator<Sort> iterator = query.orderBy().iterator();
            while (iterator.hasNext()) {
                Sort sort = iterator.next();
                sql.append(sort.expression().render(context)).append(' ').append(sort.direction().name());
                if (iterator.hasNext()) {
                    sql.append(", ");
                }
            }
        }
        if (query.limit() != null) {
            sql.append(" LIMIT ").append(context.addParameter(query.limit()));
        }
        if (!query.settings().isEmpty()) {
            sql.append(" SETTINGS ");
            Iterator<Setting> iterator = query.settings().iterator();
            while (iterator.hasNext()) {
                Setting setting = iterator.next();
                // Settings are rendered as placeholders too so operational values follow the same
                // binding path as regular query parameters.
                sql.append(setting.name().sql()).append(" = ").append(context.addParameter(setting.value()));
                if (iterator.hasNext()) {
                    sql.append(", ");
                }
            }
        }
    }

    private void renderQueryPretty(StringBuilder sql, Query query, RenderContext context) {
        if (!query.withClauses().isEmpty()) {
            sql.append("WITH\n");
            Iterator<WithClause> iterator = query.withClauses().iterator();
            while (iterator.hasNext()) {
                WithClause withClause = iterator.next();
                sql.append("  ").append(withClause.alias().sql()).append(" AS (\n");
                StringBuilder nested = new StringBuilder();
                renderQueryBodyPretty(nested, withClause.query(), context);
                sql.append(indent(nested.toString(), 4)).append('\n');
                sql.append("  )");
                if (iterator.hasNext()) {
                    sql.append(",\n");
                } else {
                    sql.append('\n');
                }
            }
        }

        renderQueryBodyPretty(sql, query, context);
        for (SetOperation setOperation : query.setOperations()) {
            sql.append('\n').append(setOperation.type().sql()).append('\n');
            renderQueryBodyPretty(sql, setOperation.query(), context);
        }
    }

    private void renderQueryBodyPretty(StringBuilder sql, Query query, RenderContext context) {
        sql.append("SELECT\n");
        appendExpressionsPretty(sql, query.selections(), context);
        sql.append('\n').append("FROM ").append(query.from().renderFromClause());
        for (Join join : query.joins()) {
            sql.append('\n')
                .append(join.type().sql())
                .append(' ')
                .append(join.table().renderFromClause())
                .append(" ON ")
                .append(join.leftKey().render(context))
                .append(" = ")
                .append(join.rightKey().render(context));
        }

        if (!query.arrayJoins().isEmpty()) {
            sql.append('\n').append("ARRAY JOIN\n");
            appendExpressionsPretty(sql, query.arrayJoins(), context);
        }
        if (query.sampleRatio() != null) {
            sql.append('\n').append("SAMPLE ").append(context.addParameter(query.sampleRatio()));
        }
        if (query.prewhere() != null) {
            sql.append('\n').append("PREWHERE ").append(query.prewhere().render(context));
        }
        if (query.where() != null) {
            sql.append('\n').append("WHERE ").append(query.where().render(context));
        }
        if (!query.groupBy().isEmpty()) {
            sql.append('\n').append("GROUP BY\n");
            appendExpressionsPretty(sql, query.groupBy(), context);
        }
        if (query.having() != null) {
            sql.append('\n').append("HAVING ").append(query.having().render(context));
        }
        if (!query.orderBy().isEmpty()) {
            sql.append('\n').append("ORDER BY\n");
            appendSortsPretty(sql, query.orderBy(), context);
        }
        if (query.limit() != null) {
            sql.append('\n').append("LIMIT ").append(context.addParameter(query.limit()));
        }
        if (!query.settings().isEmpty()) {
            sql.append('\n').append("SETTINGS\n");
            appendSettingsPretty(sql, query.settings(), context);
        }
    }

    private void appendExpressions(StringBuilder builder, Iterable<? extends Expression<?>> expressions, RenderContext context) {
        Iterator<? extends Expression<?>> iterator = expressions.iterator();
        while (iterator.hasNext()) {
            builder.append(iterator.next().render(context));
            if (iterator.hasNext()) {
                builder.append(", ");
            }
        }
    }

    private void appendExpressionsPretty(StringBuilder builder, Iterable<? extends Expression<?>> expressions, RenderContext context) {
        Iterator<? extends Expression<?>> iterator = expressions.iterator();
        while (iterator.hasNext()) {
            builder.append("  ").append(iterator.next().render(context));
            if (iterator.hasNext()) {
                builder.append(",\n");
            }
        }
    }

    private void appendSortsPretty(StringBuilder builder, Iterable<Sort> sorts, RenderContext context) {
        Iterator<Sort> iterator = sorts.iterator();
        while (iterator.hasNext()) {
            Sort sort = iterator.next();
            builder.append("  ").append(sort.expression().render(context)).append(' ').append(sort.direction().name());
            if (iterator.hasNext()) {
                builder.append(",\n");
            }
        }
    }

    private void appendSettingsPretty(StringBuilder builder, Iterable<Setting> settings, RenderContext context) {
        Iterator<Setting> iterator = settings.iterator();
        while (iterator.hasNext()) {
            Setting setting = iterator.next();
            builder.append("  ").append(setting.name().sql()).append(" = ").append(context.addParameter(setting.value()));
            if (iterator.hasNext()) {
                builder.append(",\n");
            }
        }
    }

    private String indent(String value, int spaces) {
        String prefix = " ".repeat(spaces);
        return prefix + value.replace("\n", "\n" + prefix);
    }
}
