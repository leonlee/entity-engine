package org.ofbiz.core.entity.jdbc.interceptors;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/**
 * The {@link SQLInterceptor} is called before and after SQL is to be executed in OFBIZ.
 */
public interface SQLInterceptor
{
    /**
     * This is called before an JDBC query is run.
     *
     * @param sqlString       thw SQL string in play
     * @param parameterValues this list of the Java parameters passed to this statement.  This is a simple
     *                        String.valueOf() on these parameters
     * @param statement       the JDBC {@link java.sql.Statement} in play
     */
    void beforeExecution(String sqlString, List<String> parameterValues, Statement statement);

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
     */
    void afterSuccessfulExecution(String sqlString, List<String> parameterValues, Statement statement, ResultSet resultSet, final int rowsUpdated);

    /**
     * This is called if an {@link java.sql.SQLException} is thrown during the JDBC query
     * <p/>
     * If this method runs then by design the {@link #afterSuccessfulExecution(String, java.util.List,
     * java.sql.Statement, java.sql.ResultSet, int)} (String, java.util.List, java.sql.Statement,
     * java.sql.SQLException)} will have NOT run.
     *
     * @param sqlString       thw SQL string in play
     * @param parameterValues this list of the Java parameters passed to this statement.  This is a simple
     *                        String.valueOf() on these parameters
     * @param statement       the JDBC {@link java.sql.Statement} in play
     * @param sqlException    the exception that occurred
     */
    void onException(String sqlString, List<String> parameterValues, Statement statement, final SQLException sqlException);

}
