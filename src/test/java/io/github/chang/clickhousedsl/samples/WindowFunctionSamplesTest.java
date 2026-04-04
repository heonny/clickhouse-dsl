package io.github.chang.clickhousedsl.samples;

import static io.github.chang.clickhousedsl.api.ClickHouseDsl.rowNumber;
import static io.github.chang.clickhousedsl.api.ClickHouseDsl.select;
import static io.github.chang.clickhousedsl.api.ClickHouseDsl.window;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.chang.clickhousedsl.model.Query;
import io.github.chang.clickhousedsl.model.RenderedQuery;
import io.github.chang.clickhousedsl.model.Table;
import io.github.chang.clickhousedsl.render.ClickHouseRenderer;
import org.junit.jupiter.api.Test;

class WindowFunctionSamplesTest {

    private final ClickHouseRenderer renderer = new ClickHouseRenderer();

    @Test
    void renders_window_ranking_and_running_total_sample() {
        Table sessions = Table.of("user_sessions").as("s");
        var userId = sessions.column("user_id", String.class);
        var startedAt = sessions.column("started_at", Long.class);
        var duration = sessions.column("duration_ms", Integer.class);

        Query query = select(
                userId,
                rowNumber(window().partitionBy(userId).orderBy(startedAt.desc())).as("sessionRank"),
                io.github.chang.clickhousedsl.model.Expressions.sum(duration)
                    .over(window().partitionBy(userId).orderBy(startedAt.asc()))
                    .as("runningDuration")
            )
            .from(sessions)
            .build();

        RenderedQuery rendered = renderer.render(query);

        assertThat(rendered.sql()).isEqualTo(
            "SELECT `s`.`user_id`, rowNumber() OVER (PARTITION BY `s`.`user_id` ORDER BY `s`.`started_at` DESC) AS `sessionRank`, " +
                "sum(`s`.`duration_ms`) OVER (PARTITION BY `s`.`user_id` ORDER BY `s`.`started_at` ASC) AS `runningDuration` " +
                "FROM `user_sessions` AS `s`"
        );
    }
}
