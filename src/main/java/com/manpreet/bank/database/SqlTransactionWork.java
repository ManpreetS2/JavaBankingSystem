package com.manpreet.bank.database;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Work executed inside a single JDBC transaction owned by {@link DatabaseManager}.
 */
@FunctionalInterface
public interface SqlTransactionWork<T> {

    T execute(Connection connection) throws SQLException;
}
