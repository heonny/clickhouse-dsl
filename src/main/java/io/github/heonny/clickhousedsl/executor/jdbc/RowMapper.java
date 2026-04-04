package io.github.heonny.clickhousedsl.executor.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Maps a single JDBC result row into an application value.
 *
 * @param <T> mapped row type
 */
@FunctionalInterface
public interface RowMapper<T> {

    /**
     * Maps the current row of the supplied {@link ResultSet}.
     *
     * @param resultSet current JDBC result set row
     * @param rowNum zero-based row number
     * @return mapped row value
     * @throws SQLException when JDBC column access fails
     */
    T mapRow(ResultSet resultSet, int rowNum) throws SQLException;
}
