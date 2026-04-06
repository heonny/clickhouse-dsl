package io.github.heonny.clickhousedsl.render;

import static io.github.heonny.clickhousedsl.api.ClickHouseDsl.count;
import static io.github.heonny.clickhousedsl.api.ClickHouseDsl.maxMemoryUsage;
import static io.github.heonny.clickhousedsl.api.ClickHouseDsl.maxThreads;
import static io.github.heonny.clickhousedsl.api.ClickHouseDsl.select;
import static io.github.heonny.clickhousedsl.api.ClickHouseDsl.useUncompressedCache;
import static io.github.heonny.clickhousedsl.api.ClickHouseDsl.with;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.heonny.clickhousedsl.model.Query;
import io.github.heonny.clickhousedsl.model.RenderedQuery;
import io.github.heonny.clickhousedsl.model.Table;
import org.junit.jupiter.api.Test;

class ClickHouseRendererPrettyTest {

    private final ClickHouseRenderer renderer = new ClickHouseRenderer();

    @Test
    void keepsCompactRenderingAsDefault() {
        Table users = Table.of("users").as("u");
        var name = users.column("name", String.class);

        Query query = select(name)
            .from(users)
            .where(name.eq("alice"))
            .build();

        assertThat(renderer.render(query).sql())
            .isEqualTo("SELECT `u`.`name` FROM `users` AS `u` WHERE `u`.`name` = ?");
    }

    @Test
    void rendersPrettyMultiLineSqlWithoutChangingParameterOrder() {
        Table users = Table.of("analytics.users").as("u").finalTable();
        var userName = users.column("name", String.class);
        var age = users.column("age", Integer.class);

        Query query = select(userName, count())
            .from(users)
            .prewhere(age.gt(18))
            .where(userName.eq("alice"))
            .groupBy(userName)
            .orderBy(userName.asc())
            .limit(10)
            .settings(maxThreads(4), maxMemoryUsage(268_435_456L), useUncompressedCache(true))
            .build();

        RenderedQuery rendered = renderer.render(query, RenderOptions.pretty());

        assertThat(rendered.sql()).isEqualTo(
            "SELECT\n" +
                "  `u`.`name`,\n" +
                "  count()\n" +
                "FROM `analytics`.`users` FINAL AS `u`\n" +
                "PREWHERE `u`.`age` > ?\n" +
                "WHERE `u`.`name` = ?\n" +
                "GROUP BY\n" +
                "  `u`.`name`\n" +
                "ORDER BY\n" +
                "  `u`.`name` ASC\n" +
                "LIMIT ?\n" +
                "SETTINGS\n" +
                "  `max_threads` = ?,\n" +
                "  `max_memory_usage` = ?,\n" +
                "  `use_uncompressed_cache` = ?"
        );
        assertThat(rendered.parameters()).containsExactly(18, "alice", 10, 4, 268_435_456L, 1);
    }

    @Test
    void rendersPrettyWithWithUnionAndArrayJoinBranches() {
        Table raw = Table.of("raw_users").as("r");
        Table archived = Table.of("archived_users").as("a");
        Table outer = Table.of("merged_users").as("m");
        var rawName = raw.column("name", String.class);
        var archivedName = archived.column("name", String.class);
        var mergedName = outer.column("name", String.class);
        var tags = outer.arrayColumn("tags", String.class);

        Query active = select(rawName)
            .from(raw)
            .where(rawName.eq("alice"))
            .build();
        Query archivedQuery = select(archivedName)
            .from(archived)
            .where(archivedName.eq("bob"))
            .build();
        Query merged = select(mergedName)
            .with(with("user_pool", active))
            .from(outer)
            .arrayJoin(tags)
            .unionAll(archivedQuery)
            .build();

        RenderedQuery rendered = renderer.render(merged, RenderOptions.pretty());

        assertThat(rendered.sql()).contains(
            "WITH\n",
            "  `user_pool` AS (\n",
            "ARRAY JOIN\n",
            "UNION ALL\n"
        );
        assertThat(rendered.parameters()).containsExactly("alice", "bob");
    }
}
