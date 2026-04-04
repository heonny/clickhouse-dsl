package io.github.heonny.clickhousedsl.samples.basic;

import static io.github.heonny.clickhousedsl.api.ClickHouseDsl.param;
import static io.github.heonny.clickhousedsl.api.ClickHouseDsl.select;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.heonny.clickhousedsl.model.LogicalExpression;
import io.github.heonny.clickhousedsl.model.Query;
import io.github.heonny.clickhousedsl.model.Table;
import io.github.heonny.clickhousedsl.render.ClickHouseRenderer;
import org.junit.jupiter.api.Test;

class DynamicQuerySamplesTest {

    private final ClickHouseRenderer renderer = new ClickHouseRenderer();

    @Test
    void builds_dynamic_list_query_without_string_concatenation() {
        Table sessions = Table.of("user_sessions").as("s");
        var sessionId = sessions.column("session_id", String.class);
        var appId = sessions.column("app_id", Long.class);
        var country = sessions.column("country", String.class);
        var duration = sessions.column("duration_ms", Long.class);

        LogicalExpression predicate = appId.eq(param(7L, Long.class))
            .and(duration.gt(param(1000L, Long.class)));
        predicate = predicate.and(country.eq(param("KR", String.class)));

        Query query = select(sessionId, country, duration)
            .from(sessions)
            .where(predicate)
            .orderBy(duration.desc(), sessionId.asc())
            .limit(20)
            .build();

        assertThat(renderer.render(query).sql()).isEqualTo(
            "SELECT `s`.`session_id`, `s`.`country`, `s`.`duration_ms` FROM `user_sessions` AS `s` " +
                "WHERE ((`s`.`app_id` = ? AND `s`.`duration_ms` > ?) AND `s`.`country` = ?) " +
                "ORDER BY `s`.`duration_ms` DESC, `s`.`session_id` ASC LIMIT ?"
        );
        assertThat(renderer.render(query).parameters()).containsExactly(7L, 1000L, "KR", 20);
    }
}
