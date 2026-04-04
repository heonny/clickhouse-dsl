package io.github.heonny.clickhousedsl.executor;

import static io.github.heonny.clickhousedsl.api.ClickHouseDsl.count;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.heonny.clickhousedsl.api.ClickHouseDsl;
import io.github.heonny.clickhousedsl.model.ExecutionMetrics;
import io.github.heonny.clickhousedsl.model.Query;
import io.github.heonny.clickhousedsl.model.QueryExecutionReport;
import io.github.heonny.clickhousedsl.model.RenderedQuery;
import io.github.heonny.clickhousedsl.model.Table;
import io.github.heonny.clickhousedsl.validate.QueryValidationException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

class SafeQueryExecutorTest {

    @Test
    void rejectsNullDelegate() {
        assertThatThrownBy(() -> new SafeQueryExecutor(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("delegate");
    }

    @Test
    void rejectsNullQuery() {
        SafeQueryExecutor executor = new SafeQueryExecutor(renderedQuery -> new QueryExecutionReport(
            renderedQuery,
            new ExecutionMetrics(null, null)
        ));

        assertThatThrownBy(() -> executor.execute(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("query");
    }

    @Test
    void doesNotInvokeDelegateWhenValidationFails() {
        AtomicBoolean invoked = new AtomicBoolean(false);
        SafeQueryExecutor executor = new SafeQueryExecutor(renderedQuery -> {
            invoked.set(true);
            return new QueryExecutionReport(renderedQuery, new ExecutionMetrics(null, null));
        });

        Table users = Table.of("users");
        var userName = users.column("name", String.class);
        Query invalidQuery = ClickHouseDsl.select(userName, count())
            .from(users)
            .build();

        assertThatThrownBy(() -> executor.execute(invalidQuery))
            .isInstanceOf(QueryValidationException.class)
            .hasMessageContaining("GROUP_BY_REQUIRED");
        assertThat(invoked).isFalse();
    }

    @Test
    void validatesAndDelegatesForValidQueries() {
        AtomicBoolean invoked = new AtomicBoolean(false);
        SafeQueryExecutor executor = new SafeQueryExecutor(renderedQuery -> {
            invoked.set(true);
            return new QueryExecutionReport(renderedQuery, new ExecutionMetrics(1024L, 2));
        });

        Table users = Table.of("users");
        var userName = users.column("name", String.class);
        Query query = ClickHouseDsl.select(userName)
            .from(users)
            .where(userName.eq("alice"))
            .build();

        QueryExecutionReport report = executor.execute(query);

        assertThat(invoked).isTrue();
        assertThat(report.renderedQuery().sql()).isEqualTo("SELECT `users`.`name` FROM `users` WHERE `users`.`name` = ?");
        assertThat(report.renderedQuery().parameters()).containsExactly("alice");
        assertThat(report.metrics().maxMemoryUsageBytes()).isEqualTo(1024L);
        assertThat(report.metrics().usedThreads()).isEqualTo(2);
    }

    @Test
    void exposesDelegate() {
        RenderedQueryExecutor delegate = renderedQuery -> new QueryExecutionReport(
            new RenderedQuery(renderedQuery.sql(), renderedQuery.parameters()),
            new ExecutionMetrics(null, null)
        );
        SafeQueryExecutor executor = new SafeQueryExecutor(delegate);

        assertThat(executor.delegate()).isSameAs(delegate);
    }
}
