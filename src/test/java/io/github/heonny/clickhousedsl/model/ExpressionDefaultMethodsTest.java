package io.github.heonny.clickhousedsl.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ExpressionDefaultMethodsTest {

    @Test
    void defaultComparisonHelpersCreateExpectedComparisons() {
        TestExpression<Integer> expression = new TestExpression<>("lhs", Integer.class);
        TestExpression<Integer> other = new TestExpression<>("rhs", Integer.class);

        ComparisonExpression eqValue = expression.eq(7);
        ComparisonExpression eqExpression = expression.eq(other);
        ComparisonExpression gtValue = expression.gt(8);
        ComparisonExpression gtExpression = expression.gt(other);
        ComparisonExpression ltValue = expression.lt(9);
        ComparisonExpression ltExpression = expression.lt(other);
        ComparisonExpression gteValue = expression.gte(10);
        ComparisonExpression gteExpression = expression.gte(other);
        ComparisonExpression lteValue = expression.lte(11);
        ComparisonExpression lteExpression = expression.lte(other);

        assertThat(expression.aggregate()).isFalse();
        assertThat(expression.as("alias").render(new RenderContext())).isEqualTo("lhs AS `alias`");

        assertThat(eqValue.render(new RenderContext())).isEqualTo("lhs = ?");
        assertThat(eqExpression.render(new RenderContext())).isEqualTo("lhs = rhs");
        assertThat(gtValue.render(new RenderContext())).isEqualTo("lhs > ?");
        assertThat(gtExpression.render(new RenderContext())).isEqualTo("lhs > rhs");
        assertThat(ltValue.render(new RenderContext())).isEqualTo("lhs < ?");
        assertThat(ltExpression.render(new RenderContext())).isEqualTo("lhs < rhs");
        assertThat(gteValue.render(new RenderContext())).isEqualTo("lhs >= ?");
        assertThat(gteExpression.render(new RenderContext())).isEqualTo("lhs >= rhs");
        assertThat(lteValue.render(new RenderContext())).isEqualTo("lhs <= ?");
        assertThat(lteExpression.render(new RenderContext())).isEqualTo("lhs <= rhs");
    }

    private static final class TestExpression<T> implements Expression<T> {
        private final String sql;
        private final Class<T> type;

        private TestExpression(String sql, Class<T> type) {
            this.sql = sql;
            this.type = type;
        }

        @Override
        public Class<T> type() {
            return type;
        }

        @Override
        public String render(RenderContext context) {
            return sql;
        }
    }
}
