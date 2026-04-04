package io.github.chang.clickhousedsl.api;

import static io.github.chang.clickhousedsl.api.ClickHouseDsl.aggregateFunction;
import static io.github.chang.clickhousedsl.api.ClickHouseDsl.count;
import static io.github.chang.clickhousedsl.api.ClickHouseDsl.function;
import static io.github.chang.clickhousedsl.api.ClickHouseDsl.literal;
import static io.github.chang.clickhousedsl.api.ClickHouseDsl.param;
import static io.github.chang.clickhousedsl.api.ClickHouseDsl.ref;
import static io.github.chang.clickhousedsl.api.ClickHouseDsl.select;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.chang.clickhousedsl.model.Query;
import io.github.chang.clickhousedsl.model.RenderedQuery;
import io.github.chang.clickhousedsl.model.Table;
import io.github.chang.clickhousedsl.render.ClickHouseRenderer;
import org.junit.jupiter.api.Test;

class SampleQuerySnapshotTest {

    private final ClickHouseRenderer renderer = new ClickHouseRenderer();

    @Test
    void matchesSampleAnalyticsErrorSummaryQuery() {
        Table errors = Table.of("app_errors");

        var appId = errors.column("app_id", Long.class);
        var timestamp = errors.column("event_time", Long.class);
        var message = errors.column("event_message", String.class);

        Query query = select(
                count().as("count"),
                function("toUInt8", Integer.class, literal(0, Integer.class)).as("isOther"),
                message.as("message")
            )
            .from(errors)
            .where(
                appId.eq(param(100L, Long.class))
                    .and(timestamp.gte(function("toDateTime64", Long.class, param("2026-04-01T00:00:00", String.class), literal(3, Integer.class), literal("UTC", String.class))))
                    .and(timestamp.lt(
                        function(
                            "toDateTime64",
                            Long.class,
                            function("addDays", String.class, function("toDate", String.class, param("2026-04-02", String.class)), literal(1, Integer.class)),
                            literal(3, Integer.class),
                            literal("UTC", String.class)
                        )
                    ))
            )
            .groupBy(ref("message", String.class))
            .orderBy(ref("count", Long.class).desc(), ref("message", String.class).asc())
            .limit(50)
            .build();

        RenderedQuery rendered = renderer.render(query);

        assertThat(rendered.sql()).isEqualTo(
            "SELECT count() AS `count`, toUInt8(0) AS `isOther`, `app_errors`.`event_message` AS `message` " +
                "FROM `app_errors` " +
                "WHERE ((`app_errors`.`app_id` = ? AND `app_errors`.`event_time` >= toDateTime64(?, 3, 'UTC')) " +
                "AND `app_errors`.`event_time` < toDateTime64(addDays(toDate(?), 1), 3, 'UTC')) " +
                "GROUP BY `message` ORDER BY `count` DESC, `message` ASC LIMIT ?"
        );
        assertThat(rendered.parameters()).containsExactly(100L, "2026-04-01T00:00:00", "2026-04-02", 50);
    }
}
