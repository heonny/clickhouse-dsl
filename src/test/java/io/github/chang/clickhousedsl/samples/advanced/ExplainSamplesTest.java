package io.github.chang.clickhousedsl.samples.advanced;

import static io.github.chang.clickhousedsl.api.ClickHouseDsl.analyze;
import static io.github.chang.clickhousedsl.api.ClickHouseDsl.explain;
import static io.github.chang.clickhousedsl.api.ClickHouseDsl.render;
import static io.github.chang.clickhousedsl.api.ClickHouseDsl.select;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.chang.clickhousedsl.explain.ExplainResult;
import io.github.chang.clickhousedsl.explain.ExplainType;
import io.github.chang.clickhousedsl.model.Query;
import io.github.chang.clickhousedsl.model.Table;
import org.junit.jupiter.api.Test;

class ExplainSamplesTest {

    @Test
    void renders_and_summarizes_explain_plan_sample() {
        Table pageViews = Table.of("page_views");
        var pagePath = pageViews.column("page_path", String.class);

        Query query = select(pagePath)
            .from(pageViews)
            .where(pagePath.eq("/pricing"))
            .build();

        String explainSql = render(explain(ExplainType.PLAN, query));
        ExplainResult result = analyze(
            ExplainType.PLAN,
            """
            ReadFromStorage
            Filter
            Sorting
            """
        );

        assertThat(explainSql).isEqualTo(
            "EXPLAIN PLAN SELECT `page_views`.`page_path` FROM `page_views` WHERE `page_views`.`page_path` = ?"
        );
        assertThat(result.summary().readsFromStorage()).isTrue();
        assertThat(result.summary().hasFilter()).isTrue();
        assertThat(result.summary().hasSorting()).isTrue();
        assertThat(result.summary().notes()).isNotEmpty();
    }
}
