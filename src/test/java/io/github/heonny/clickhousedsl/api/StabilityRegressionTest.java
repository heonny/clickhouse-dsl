package io.github.heonny.clickhousedsl.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.heonny.clickhousedsl.model.Identifier;
import io.github.heonny.clickhousedsl.model.Query;
import io.github.heonny.clickhousedsl.model.RenderedQuery;
import io.github.heonny.clickhousedsl.model.Table;
import io.github.heonny.clickhousedsl.render.ClickHouseRenderer;
import java.util.List;
import org.junit.jupiter.api.Test;

class StabilityRegressionTest {

    private final ClickHouseRenderer renderer = new ClickHouseRenderer();

    @Test
    void identifierRenderingRemainsStableAcrossSafeNames() {
        List<String> identifiers = List.of(
            "users",
            "analytics.users",
            "events_2026",
            "mv.daily_rollup",
            "tenant_01.metrics"
        );

        for (String identifierValue : identifiers) {
            Identifier identifier = Identifier.of(identifierValue);
            assertThat(identifier.sql()).startsWith("`");
            assertThat(identifier.value()).isEqualTo(identifierValue);
        }
    }

    @Test
    void identifierRejectsUnsafeShapesAcrossCommonBadInputs() {
        List<String> invalidIdentifiers = List.of(
            "users;drop",
            "bad-name",
            "bad name",
            "users/*",
            "users--comment"
        );

        for (String invalidIdentifier : invalidIdentifiers) {
            assertThatThrownBy(() -> Identifier.of(invalidIdentifier))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsafe identifier");
        }
    }

    @Test
    void rendererProducesDeterministicSqlAndParameterOrderForDynamicBranches() {
        Table events = Table.of("events").as("e");
        var tenantId = events.column("tenant_id", Long.class);
        var country = events.column("country", String.class);
        var score = events.column("score", Integer.class);

        Query query = ClickHouseDsl.select(tenantId, country)
            .from(events)
            .where(
                tenantId.eq(7L)
                    .and(score.gt(100))
                    .and(country.eq("KR"))
            )
            .limit(20)
            .build();

        RenderedQuery first = renderer.render(query);
        RenderedQuery second = renderer.render(query);

        assertThat(first.sql()).isEqualTo(second.sql());
        assertThat(first.parameters()).containsExactly(7L, 100, "KR", 20);
        assertThat(second.parameters()).containsExactly(7L, 100, "KR", 20);
    }

    @Test
    void validatedRendererRemainsStableForRepeatedCalls() {
        Table users = Table.of("users");
        var userName = users.column("name", String.class);

        Query query = ClickHouseDsl.select(userName)
            .from(users)
            .where(userName.eq("alice"))
            .build();

        String first = ClickHouseDsl.renderValidated(query);
        String second = ClickHouseDsl.renderValidated(query);

        assertThat(first).isEqualTo(second);
        assertThat(first).isEqualTo("SELECT `users`.`name` FROM `users` WHERE `users`.`name` = ?");
    }
}
