package io.github.heonny.clickhousedsl.model;

import java.util.Objects;

/**
 * Immutable join descriptor.
 */
public final class Join {

    private final JoinType type;
    private final Table table;
    private final Expression<?> leftKey;
    private final Expression<?> rightKey;

    /**
     * Creates a join descriptor.
     *
     * @param type join type
     * @param table joined table
     * @param leftKey left join key
     * @param rightKey right join key
     */
    public Join(JoinType type, Table table, Expression<?> leftKey, Expression<?> rightKey) {
        this.type = Objects.requireNonNull(type, "type");
        this.table = Objects.requireNonNull(table, "table");
        this.leftKey = Objects.requireNonNull(leftKey, "leftKey");
        this.rightKey = Objects.requireNonNull(rightKey, "rightKey");
    }

    /**
     * Returns the join type.
     *
     * @return join type
     */
    public JoinType type() {
        return type;
    }

    /**
     * Returns the joined table.
     *
     * @return joined table
     */
    public Table table() {
        return table;
    }

    /**
     * Returns the left join key.
     *
     * @return left join key
     */
    public Expression<?> leftKey() {
        return leftKey;
    }

    /**
     * Returns the right join key.
     *
     * @return right join key
     */
    public Expression<?> rightKey() {
        return rightKey;
    }
}
