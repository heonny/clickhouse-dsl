package io.github.chang.clickhousedsl.samples.advanced;

import static io.github.chang.clickhousedsl.api.ClickHouseDsl.countIfMerge;
import static io.github.chang.clickhousedsl.api.ClickHouseDsl.countMerge;
import static io.github.chang.clickhousedsl.api.ClickHouseDsl.select;
import static io.github.chang.clickhousedsl.api.ClickHouseDsl.sumMerge;
import static io.github.chang.clickhousedsl.api.ClickHouseDsl.uniqMerge;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.chang.clickhousedsl.model.Query;
import io.github.chang.clickhousedsl.model.RenderedQuery;
import io.github.chang.clickhousedsl.model.Table;
import io.github.chang.clickhousedsl.render.ClickHouseRenderer;
import org.junit.jupiter.api.Test;

class AggregateStateSamplesTest {

    private final ClickHouseRenderer renderer = new ClickHouseRenderer();

    @Test
    void renders_aggregate_state_merge_sample() {
        Table metrics = Table.of("api_rollup");

        var endpoint = metrics.column("endpoint", String.class);
        var totalCountState = metrics.stateColumn("total_count_state", Long.class);
        var slowCountState = metrics.stateColumn("slow_count_state", Long.class);
        var userUniqState = metrics.stateColumn("user_uniq_state", Long.class);
        var durationSumState = metrics.stateColumn("duration_sum_state", Integer.class);

        Query query = select(
                endpoint,
                countMerge(totalCountState).as("totalCount"),
                countIfMerge(slowCountState).as("slowCount"),
                uniqMerge(userUniqState).as("userCount"),
                sumMerge(durationSumState, Integer.class).as("durationSum")
            )
            .from(metrics)
            .groupBy(endpoint)
            .build();

        RenderedQuery rendered = renderer.render(query);

        assertThat(rendered.sql()).isEqualTo(
            "SELECT `api_rollup`.`endpoint`, countMerge(`api_rollup`.`total_count_state`) AS `totalCount`, " +
                "countIfMerge(`api_rollup`.`slow_count_state`) AS `slowCount`, uniqMerge(`api_rollup`.`user_uniq_state`) AS `userCount`, " +
                "sumMerge(`api_rollup`.`duration_sum_state`) AS `durationSum` FROM `api_rollup` GROUP BY `api_rollup`.`endpoint`"
        );
    }
}
