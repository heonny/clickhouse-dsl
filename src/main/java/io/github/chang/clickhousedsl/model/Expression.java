package io.github.chang.clickhousedsl.model;

public interface Expression<T> {

    Class<T> type();

    String render(RenderContext context);

    default boolean aggregate() {
        return false;
    }

    default AliasedExpression<T> as(String alias) {
        return new AliasedExpression<>(this, Identifier.of(alias));
    }
}
