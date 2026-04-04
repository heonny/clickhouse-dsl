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
        RenderContext context = new RenderContext();
        StringBuilder sql = new StringBuilder();
        renderQuery(sql, query, context);
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
        ClickHouseDsl.validateOrThrow(query);
        return render(query);
    }

    private void renderQuery(StringBuilder sql, Query query, RenderContext context) {
        if (!query.withClauses().isEmpty()) {
            sql.append("WITH ");
            Iterator<WithClause> iterator = query.withClauses().iterator();
            while (iterator.hasNext()) {
                WithClause withClause = iterator.next();
                sql.append(withClause.alias().sql()).append(" AS (");
                renderQueryBody(sql, withClause.query(), context);
                sql.append(")");
                if (iterator.hasNext()) {
                    sql.append(", ");
                }
            }
            sql.append(' ');
        }

        // The primary query body is rendered before any UNION / UNION ALL branches.
        renderQueryBody(sql, query, context);
        for (SetOperation setOperation : query.setOperations()) {
            sql.append(' ').append(setOperation.type().sql()).append(' ');
            renderQueryBody(sql, setOperation.query(), context);
        }
    }

    private void renderQueryBody(StringBuilder sql, Query query, RenderContext context) {
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

    private void appendExpressions(StringBuilder builder, Iterable<? extends Expression<?>> expressions, RenderContext context) {
        Iterator<? extends Expression<?>> iterator = expressions.iterator();
        while (iterator.hasNext()) {
            builder.append(iterator.next().render(context));
            if (iterator.hasNext()) {
                builder.append(", ");
            }
        }
    }
}
