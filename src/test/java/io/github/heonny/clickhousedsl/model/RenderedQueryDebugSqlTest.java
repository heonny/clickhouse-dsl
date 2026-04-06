package io.github.heonny.clickhousedsl.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class RenderedQueryDebugSqlTest {

    @Test
    void interpolatesCommonParameterTypesForDebugging() {
        RenderedQuery renderedQuery = new RenderedQuery(
            "SELECT ?, ?, ?, ?, ?, ?, '?'",
            Arrays.asList(
                "O'Reilly",
                null,
                true,
                42,
                LocalDate.of(2026, 4, 6),
                LocalDateTime.of(2026, 4, 6, 12, 34, 56)
            )
        );

        assertThat(renderedQuery.debugSql()).isEqualTo(
            "SELECT 'O''Reilly', NULL, 1, 42, '2026-04-06', '2026-04-06 12:34:56', '?'"
        );
    }

    @Test
    void rejectsTooFewParametersForDebugRendering() {
        RenderedQuery renderedQuery = new RenderedQuery("SELECT ?, ?", List.of("alice"));

        assertThatThrownBy(renderedQuery::debugSql)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Not enough parameters");
    }

    @Test
    void rejectsTooManyParametersForDebugRendering() {
        RenderedQuery renderedQuery = new RenderedQuery("SELECT 1", List.of("alice"));

        assertThatThrownBy(renderedQuery::debugSql)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Too many parameters");
    }

    @Test
    void preservesEscapedQuotesInsideSqlStringLiterals() {
        RenderedQuery renderedQuery = new RenderedQuery(
            "SELECT 'it''s ?', ?",
            List.of("alice")
        );

        assertThat(renderedQuery.debugSql()).isEqualTo("SELECT 'it''s ?', 'alice'");
    }
}
