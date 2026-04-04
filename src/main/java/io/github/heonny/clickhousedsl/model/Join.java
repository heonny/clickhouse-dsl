package io.github.heonny.clickhousedsl.model;

import java.util.Objects;

public final class Join {

    private final JoinType type;
    private final Table table;
    private final Expression<?> leftKey;
    private final Expression<?> rightKey;

    public Join(JoinType type, Table table, Expression<?> leftKey, Expression<?> rightKey) {
        this.type = Objects.requireNonNull(type, "type");
        this.table = Objects.requireNonNull(table, "table");
        this.leftKey = Objects.requireNonNull(leftKey, "leftKey");
        this.rightKey = Objects.requireNonNull(rightKey, "rightKey");
    }

    public JoinType type() {
        return type;
    }

    public Table table() {
        return table;
    }

    public Expression<?> leftKey() {
        return leftKey;
    }

    public Expression<?> rightKey() {
        return rightKey;
    }
}
