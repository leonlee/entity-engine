package org.ofbiz.core.entity.jdbc.interceptors.connection;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;

import org.ofbiz.core.util.Debug;

/**
 * Wraps a {@link SQLConnectionInterceptor} such that the callbacks are guaranteed not to throw
 * runtime exceptions or linkage errors.
 *
 * @since v1.0.67 / v1.1.7
 */
public class SafeDelegatingSqlConnectionInterceptor implements SQLConnectionInterceptor
{
    private static final String MODULE = SafeDelegatingSqlConnectionInterceptor.class.getName();

    private final SQLConnectionInterceptor delegate;

    public SafeDelegatingSqlConnectionInterceptor(SQLConnectionInterceptor delegate)
    {
        this.delegate = delegate;
    }

    @Override
    public void onConnectionTaken(Connection connection, ConnectionPoolState connectionPoolState)
    {
        try
        {
            delegate.onConnectionTaken(connection, connectionPoolState);
        }
        catch (RuntimeException | LinkageError e)
        {
            logError(e, "onConnectionTaken", connection, connectionPoolState);
        }
    }

    @Override
    public void onConnectionReplaced(Connection connection, ConnectionPoolState connectionPoolState)
    {
        try
        {
            delegate.onConnectionReplaced(connection, connectionPoolState);
        }
        catch (RuntimeException | LinkageError e)
        {
            logError(e, "onConnectionReplaced", connection, connectionPoolState);
        }
    }

    @Override
    public void beforeExecution(String sqlString, List<String> parameterValues, Statement statement)
    {
        try
        {
            delegate.beforeExecution(sqlString, parameterValues, statement);
        }
        catch (RuntimeException | LinkageError e)
        {
            logError(e, "beforeExecution", sqlString, parameterValues, statement);
        }
    }

    @Override
    public void afterSuccessfulExecution(String sqlString, List<String> parameterValues, Statement statement, ResultSet resultSet, int rowsUpdated)
    {
        try
        {
            delegate.afterSuccessfulExecution(sqlString, parameterValues, statement, resultSet, rowsUpdated);
        }
        catch (RuntimeException | LinkageError e)
        {
            logError(e, "afterSuccessfulExecution", sqlString, parameterValues, statement, resultSet, rowsUpdated);
        }
    }

    @Override
    public void onException(String sqlString, List<String> parameterValues, Statement statement, SQLException sqlException)
    {
        try
        {
            delegate.onException(sqlString, parameterValues, statement, sqlException);
        }
        catch (RuntimeException | LinkageError e)
        {
            logError(e, "onException", sqlString, parameterValues, statement, sqlException);
        }
    }



    private static void logError(Throwable e, String fn, Object... args)
    {
        Debug.logError(e, "Unexpected exception from SQL connection interceptor callback: " + fn + '(' +
                Arrays.deepToString(args) + ')', MODULE);
    }
}
