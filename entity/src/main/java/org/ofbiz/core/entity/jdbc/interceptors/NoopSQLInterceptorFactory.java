package org.ofbiz.core.entity.jdbc.interceptors;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/**
 * A NO-OP implementation of {@link SQLInterceptorFactory}
 */
public class NoopSQLInterceptorFactory implements SQLInterceptorFactory
{
    public static final SQLInterceptorFactory NOOP_INTERCEPTOR_FACTORY = new NoopSQLInterceptorFactory();
    public static final SQLInterceptor NOOP_INTERCEPTOR = new NoopSQLInterceptor();

    private NoopSQLInterceptorFactory()
    {
    }

    public SQLInterceptor newSQLInterceptor(final String ofbizHelperName)
    {
        return NOOP_INTERCEPTOR;
    }


    /**
     * A NO-OP implementation of {@link SQLInterceptor}
     */
    private static class NoopSQLInterceptor implements SQLInterceptor
    {

        private NoopSQLInterceptor()
        {
        }


        public void beforeExecution(final String sqlString, final List<String> parameterValues, final Statement statement)
        {
        }

        public void afterSuccessfulExecution(final String sqlString, final List<String> parameterValues, final Statement statement, final ResultSet resultSet, final int rowsUpdated)
        {
        }

        public void onException(final String sqlString, final List<String> parameterValues, final Statement statement, final SQLException sqlException)
        {
        }
    }
}
