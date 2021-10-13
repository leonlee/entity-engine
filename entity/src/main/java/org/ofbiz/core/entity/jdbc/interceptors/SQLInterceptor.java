package org.ofbiz.core.entity.jdbc.interceptors;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/**
 * The {@link SQLInterceptor} is called before and after SQL is to be executed in OFBIZ.
 */
public interface SQLInterceptor {
    /**
     * This is called before an JDBC query is run.
     *
     * @param sqlString       thw SQL string in play
     * @param parameterValues this list of the Java parameters passed to this statement.  This is a simple
     *                        String.valueOf() on these parameters
     * @param statement       the JDBC {@link java.sql.Statement} in play
     * @deprecated please use {@link #beforeExecution(String, List, List, Statement)}
     */
    void beforeExecution(String sqlString, List<String> parameterValues, Statement statement);

    /**
     * This is called before an JDBC query is run.
     *
     * @param sqlString               thw SQL string in play
     * @param parameterValues         this list of the Java parameters passed to this statement.  This is a simple
     *                                String.valueOf() on these parameters
     * @param parameterValuesForBatch this list of list of the Java parameters passed to this statement for batch processing. This is a simple
     *                                String.valueOf() on these parameters. In case of non batch operation this will be an empty list.
     * @param statement               the JDBC {@link java.sql.Statement} in play
     */
    default void beforeExecution(String sqlString, List<String> parameterValues, List<List<String>> parameterValuesForBatch, Statement statement) {
        beforeExecution(sqlString, parameterValues, statement);
    }

    /**
     * This is called after a successful (ie no {@link java.sql.SQLException} generated) JDBC query is run.
     * <p/>
     * If this method runs then by design the {@link #onException(String, java.util.List, java.sql.Statement,
     * java.sql.SQLException)} will NOT run.
     *
     * @param sqlString       the SQL string in play
     * @param parameterValues this list of the Java parameters passed to this statement.  This is a simple
     *                        String.valueOf() on these parameters
     * @param statement       the JDBC {@link java.sql.Statement} in play
     * @param resultSet       a JDBC {@link java.sql.ResultSet}.  In the case of an update, this will be NULL.
     * @param rowsUpdated     the number of rows updated.  In the case of a SELECT, this will be -1
     * @deprecated please use {@link #afterSuccessfulExecution(String, List, List, Statement, ResultSet, int, int[])}
     */
    void afterSuccessfulExecution(String sqlString, List<String> parameterValues, Statement statement, ResultSet resultSet, final int rowsUpdated);

    /**
     * This is called after a successful (ie no {@link java.sql.SQLException} generated) JDBC query is run.
     * <p/>
     * If this method runs then by design the {@link #onException(String, List, List, Statement, SQLException)} will NOT run.
     *
     * @param sqlString               the SQL string in play
     * @param parameterValues         this list of the Java parameters passed to this statement. This is a simple
     *                                String.valueOf() on these parameters
     * @param parameterValuesForBatch this list of list of the Java parameters passed to this statement for batch processing. This is a simple
     *                                String.valueOf() on these parameters. In case of non batch operation this will be an empty list.
     * @param statement               the JDBC {@link java.sql.Statement} in play
     * @param resultSet               a JDBC {@link java.sql.ResultSet}.  In the case of an update, this will be NULL.
     * @param rowsUpdated             the number of rows updated.  In the case of a SELECT, this will be -1
     * @param rowsUpdatedByBatch      the number of rows updated by batch operation. In case of non batch operation it will be an empty array.
     */
    default void afterSuccessfulExecution(String sqlString, List<String> parameterValues, List<List<String>> parameterValuesForBatch, Statement statement, ResultSet resultSet, final int rowsUpdated, final int[] rowsUpdatedByBatch) {
        afterSuccessfulExecution(sqlString, parameterValues, statement, resultSet, rowsUpdated);
    }

    /**
     * This is called if an {@link java.sql.SQLException} is thrown during the JDBC query
     * <p/>
     * If this method runs then by design the {@link #afterSuccessfulExecution(String, java.util.List,
     * java.sql.Statement, java.sql.ResultSet, int)} will have NOT run.
     *
     * @param sqlString       thw SQL string in play
     * @param parameterValues this list of the Java parameters passed to this statement.  This is a simple
     *                        String.valueOf() on these parameters
     * @param statement       the JDBC {@link java.sql.Statement} in play
     * @param sqlException    the exception that occurred
     * @deprecated please use {@link #onException(String, List, List, Statement, SQLException)}
     */
    void onException(String sqlString, List<String> parameterValues, Statement statement, final SQLException sqlException);

    /**
     * This is called if an {@link java.sql.SQLException} is thrown during the JDBC query
     * <p/>
     * If this method runs then by design the {@link #afterSuccessfulExecution(String, List, List, Statement, ResultSet, int, int[])}  will have NOT run.
     *
     * @param sqlString               thw SQL string in play
     * @param parameterValues         this list of the Java parameters passed to this statement.  This is a simple
     *                                String.valueOf() on these parameters
     * @param parameterValuesForBatch this list of list of the Java parameters passed to this statement for batch processing. This is a simple
     *                                String.valueOf() on these parameters. For non batched operations this will be an empty list.
     * @param statement               the JDBC {@link java.sql.Statement} in play
     * @param sqlException            the exception that occurred
     */
    default void onException(String sqlString, List<String> parameterValues, List<List<String>> parameterValuesForBatch, Statement statement, final SQLException sqlException) {
        onException(sqlString, parameterValues, statement, sqlException);
    }
}
