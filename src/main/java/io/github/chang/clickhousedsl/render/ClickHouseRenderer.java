package io.github.chang.clickhousedsl.render;

import io.github.chang.clickhousedsl.model.Expression;
import io.github.chang.clickhousedsl.model.Join;
import io.github.chang.clickhousedsl.model.Query;
import io.github.chang.clickhousedsl.model.RenderContext;
import io.github.chang.clickhousedsl.model.RenderedQuery;
import io.github.chang.clickhousedsl.model.Setting;
import io.github.chang.clickhousedsl.model.Sort;
import java.util.Iterator;

public final class ClickHouseRenderer {

    public RenderedQuery render(Query query) {
        RenderContext context = new RenderContext();
        StringBuilder sql = new StringBuilder("SELECT ");
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
                sql.append(setting.name().sql()).append(" = ").append(context.addParameter(setting.value()));
                if (iterator.hasNext()) {
                    sql.append(", ");
                }
            }
        }
        return new RenderedQuery(sql.toString(), context.parameters());
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
