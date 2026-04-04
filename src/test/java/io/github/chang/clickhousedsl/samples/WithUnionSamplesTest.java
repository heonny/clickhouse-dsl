package io.github.chang.clickhousedsl.samples;

import static io.github.chang.clickhousedsl.api.ClickHouseDsl.select;
import static io.github.chang.clickhousedsl.api.ClickHouseDsl.with;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.chang.clickhousedsl.model.Query;
import io.github.chang.clickhousedsl.model.RenderedQuery;
import io.github.chang.clickhousedsl.model.Table;
import io.github.chang.clickhousedsl.render.ClickHouseRenderer;
import org.junit.jupiter.api.Test;

class WithUnionSamplesTest {

    private final ClickHouseRenderer renderer = new ClickHouseRenderer();

    @Test
    void renders_cte_and_union_all_sample() {
        Table rawUsers = Table.of("raw_users");
        Table activeUsers = Table.of("active_users");
        Table archivedUsers = Table.of("archived_users");

        Query activeUserCte = select(rawUsers.column("name", String.class))
            .from(rawUsers)
            .where(rawUsers.column("name", String.class).eq("alice"))
            .build();

        Query archivedQuery = select(archivedUsers.column("name", String.class))
            .from(archivedUsers)
            .where(archivedUsers.column("name", String.class).eq("bob"))
            .build();

        Query merged = select(activeUsers.column("name", String.class))
            .with(with("active_users", activeUserCte))
            .from(activeUsers)
            .unionAll(archivedQuery)
            .build();

        RenderedQuery rendered = renderer.render(merged);

        assertThat(rendered.sql()).isEqualTo(
            "WITH `active_users` AS (SELECT `raw_users`.`name` FROM `raw_users` WHERE `raw_users`.`name` = ?) " +
                "SELECT `active_users`.`name` FROM `active_users` UNION ALL " +
                "SELECT `archived_users`.`name` FROM `archived_users` WHERE `archived_users`.`name` = ?"
        );
        assertThat(rendered.parameters()).containsExactly("alice", "bob");
    }
}
