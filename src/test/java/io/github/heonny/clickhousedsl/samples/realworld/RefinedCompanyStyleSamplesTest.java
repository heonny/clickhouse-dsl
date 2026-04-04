package io.github.heonny.clickhousedsl.samples.realworld;

import static io.github.heonny.clickhousedsl.api.ClickHouseDsl.countIfMerge;
import static io.github.heonny.clickhousedsl.api.ClickHouseDsl.countMerge;
import static io.github.heonny.clickhousedsl.api.ClickHouseDsl.divide;
import static io.github.heonny.clickhousedsl.api.ClickHouseDsl.function;
import static io.github.heonny.clickhousedsl.api.ClickHouseDsl.literal;
import static io.github.heonny.clickhousedsl.api.ClickHouseDsl.param;
import static io.github.heonny.clickhousedsl.api.ClickHouseDsl.select;
import static io.github.heonny.clickhousedsl.api.ClickHouseDsl.sumMerge;
import static io.github.heonny.clickhousedsl.api.ClickHouseDsl.uniqMerge;
import static io.github.heonny.clickhousedsl.api.ClickHouseDsl.with;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.heonny.clickhousedsl.model.Query;
import io.github.heonny.clickhousedsl.model.RenderedQuery;
import io.github.heonny.clickhousedsl.model.Table;
import io.github.heonny.clickhousedsl.render.ClickHouseRenderer;
import org.junit.jupiter.api.Test;

class RefinedCompanyStyleSamplesTest {

    private final ClickHouseRenderer renderer = new ClickHouseRenderer();

    @Test
    void renders_refined_api_performance_style_sample() {
        Table rollup = Table.of("api_metrics_rollup");

        var appId = rollup.column("app_id", Long.class);
        var endpoint = rollup.column("endpoint", String.class);
        var totalCountState = rollup.stateColumn("total_count_state", Long.class);
        var successCountState = rollup.stateColumn("success_count_state", Long.class);
        var errorCountState = rollup.stateColumn("error_count_state", Long.class);
        var userUniqState = rollup.stateColumn("user_uniq_state", Long.class);
        var durationSumState = rollup.stateColumn("duration_sum_state", Integer.class);
        var durationCountState = rollup.stateColumn("duration_count_state", Long.class);

        Query aggregated = select(
                endpoint.as("endpointKey"),
                endpoint.as("endpointFull"),
                countMerge(totalCountState).as("totalCount"),
                countMerge(successCountState).as("successCount"),
                countMerge(errorCountState).as("errorCount"),
                function("toFloat64", Double.class, uniqMerge(userUniqState)).as("userCount"),
                divide(
                    sumMerge(durationSumState, Integer.class),
                    function("nullIf", Long.class, countIfMerge(durationCountState), literal(0L, Long.class))
                ).as("responseTime")
            )
            .from(rollup)
            .where(appId.eq(param(7L, Long.class)).and(endpoint.gt(literal("", String.class))))
            .groupBy(endpoint)
            .build();

        Table aggregatedTable = Table.of("aggregated");
        var aggregatedEndpoint = aggregatedTable.column("endpointKey", String.class);
        var aggregatedResponseTime = aggregatedTable.column("responseTime", Double.class);

        Query filtered = select(
                aggregatedEndpoint,
                aggregatedTable.column("endpointFull", String.class),
                aggregatedTable.column("totalCount", Long.class),
                aggregatedTable.column("successCount", Long.class),
                aggregatedTable.column("errorCount", Long.class),
                aggregatedTable.column("userCount", Double.class),
                aggregatedResponseTime
            )
            .from(aggregatedTable)
            .where(
                function("positionCaseInsensitive", Integer.class, aggregatedEndpoint, param("checkout", String.class)).gt(literal(0, Integer.class))
                    .and(aggregatedResponseTime.gte(param(120.0d, Double.class)))
            )
            .build();

        Query finalQuery = select(
                Table.of("filtered").column("endpointKey", String.class).as("normalized"),
                Table.of("filtered").column("endpointFull", String.class).as("full"),
                Table.of("filtered").column("totalCount", Long.class).as("count"),
                Table.of("filtered").column("successCount", Long.class).as("successCount"),
                Table.of("filtered").column("errorCount", Long.class).as("errorCount"),
                Table.of("filtered").column("userCount", Double.class).as("userCount"),
                Table.of("filtered").column("responseTime", Double.class).as("responseTime")
            )
            .with(with("aggregated", aggregated), with("filtered", filtered))
            .from(Table.of("filtered"))
            .orderBy(Table.of("filtered").column("totalCount", Long.class).desc(), Table.of("filtered").column("endpointKey", String.class).asc())
            .limit(30)
            .build();

        RenderedQuery rendered = renderer.render(finalQuery);

        assertThat(rendered.sql()).contains("WITH `aggregated` AS (SELECT");
        assertThat(rendered.sql()).contains("countMerge(`api_metrics_rollup`.`total_count_state`) AS `totalCount`");
        assertThat(rendered.sql()).contains("toFloat64(uniqMerge(`api_metrics_rollup`.`user_uniq_state`)) AS `userCount`");
        assertThat(rendered.sql()).contains("sumMerge(`api_metrics_rollup`.`duration_sum_state`) / nullIf(countIfMerge(`api_metrics_rollup`.`duration_count_state`), 0) AS `responseTime`");
        assertThat(rendered.sql()).contains("WITH `aggregated` AS");
        assertThat(rendered.sql()).contains("`filtered`.`endpointKey` AS `normalized`");
        assertThat(rendered.sql()).contains("ORDER BY `filtered`.`totalCount` DESC, `filtered`.`endpointKey` ASC LIMIT ?");
        assertThat(rendered.parameters()).containsExactly(7L, "checkout", 120.0d, 30);
    }
}
