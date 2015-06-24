package org.ofbiz.core.entity.jdbc;

import org.ofbiz.core.entity.jdbc.interceptors.NoopSQLInterceptorFactory;
import org.ofbiz.core.entity.jdbc.interceptors.SQLInterceptor;
import org.ofbiz.core.entity.jdbc.interceptors.SQLInterceptorFactory;
import org.ofbiz.core.entity.jdbc.interceptors.connection.ConnectionPoolState;
import org.ofbiz.core.entity.jdbc.interceptors.connection.SQLConnectionInterceptor;
import org.ofbiz.core.entity.jdbc.interceptors.connection.SafeDelegatingSqlConnectionInterceptor;
import org.ofbiz.core.entity.util.ClassLoaderUtils;
import org.ofbiz.core.util.Debug;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Properties;

/**
 * This will read "ofbiz-database.properties" and look for a "sqlinterceptor.factory.class" key.  It will then try and
 * instantiate a {@link org.ofbiz.core.entity.jdbc.interceptors.SQLInterceptorFactory} from this class name.
 * <p/>
 * If all this fails it will use a NO-OP interceptor factory and hence do nothing.
 */
public class SQLInterceptorSupport
{
    /**
     * The name of the ofbiz-database.properties key for {@link org.ofbiz.core.entity.jdbc.interceptors.SQLInterceptorFactory}
     */
    public static final String INTERCEPTOR_FACTORY_CLASS_NAME_KEY = "sqlinterceptor.factory.class";

    private static final Properties CONFIGURATION;
    private static final SQLInterceptorFactory interceptorFactory;

    static
    {
        CONFIGURATION = new Properties();
        try
        {
            CONFIGURATION.load(ClassLoaderUtils.getResourceAsStream("ofbiz-database.properties", SQLInterceptorSupport.class));
        }
        catch (Exception e)
        {
            Debug.logError("Unable to find ofbiz-database.properties file. Using default values for ofbiz configuration.");
        }
        interceptorFactory = loadInterceptorFactoryClass();
    }

    /**
     * This will return a NON NULL SQLInterceptor.  If the {@link org.ofbiz.core.entity.jdbc.interceptors.SQLInterceptorFactory}
     * provided returns null, then a default NO-OP {@link org.ofbiz.core.entity.jdbc.interceptors.SQLInterceptor} will
     * be returned.
     *
     * @param ofbizHelperName the name of the {@link org.ofbiz.core.entity.GenericHelper} in play
     * @return a NON NULL {@link org.ofbiz.core.entity.jdbc.interceptors.SQLInterceptor}
     */
    public static SQLInterceptor getNonNullSQLInterceptor(String ofbizHelperName)
    {
        if (interceptorFactory == null)
        {
            throw new IllegalStateException("How can this happen? interceptorFactory must be non null by design");
        }
        SQLInterceptor sqlInterceptor = interceptorFactory.newSQLInterceptor(ofbizHelperName);
        if (sqlInterceptor == null)
        {
            sqlInterceptor = NoopSQLInterceptorFactory.NOOP_INTERCEPTOR;
        }
        return sqlInterceptor;
    }

    /**
     * This will return a NON NULL {@link SQLConnectionInterceptor}.  If the {@link org.ofbiz.core.entity.jdbc.interceptors.SQLInterceptorFactory}
     * provided returns null, then a default NO-OP {@link SQLConnectionInterceptor} will
     * be returned.
     *
     * @param ofbizHelperName the name of the {@link org.ofbiz.core.entity.GenericHelper} in playl
     * @return a NON NULL {@link SQLConnectionInterceptor}
     */
    public static SQLConnectionInterceptor getNonNullSQLConnectionInterceptor(String ofbizHelperName)
    {
        return new SafeDelegatingSqlConnectionInterceptor(getNonNullDelegate(ofbizHelperName));
    }

    private static SQLConnectionInterceptor getNonNullDelegate(String ofbizHelperName)
    {
        final SQLInterceptor sqlInterceptor = getNonNullSQLInterceptor(ofbizHelperName);
        if (sqlInterceptor instanceof SQLConnectionInterceptor) {
            return (SQLConnectionInterceptor) sqlInterceptor;
        }
        return new DelegatingNoOpSQLConnectionInterceptor(sqlInterceptor);
    }

    private static SQLInterceptorFactory loadInterceptorFactoryClass()
    {
        SQLInterceptorFactory interceptorFactory = NoopSQLInterceptorFactory.NOOP_INTERCEPTOR_FACTORY;

        String className = CONFIGURATION.getProperty(INTERCEPTOR_FACTORY_CLASS_NAME_KEY);
        if (className != null)
        {
            try
            {
                Class<?> interceptorFactoryClass = ClassLoaderUtils.loadClass(className, SQLInterceptorSupport.class);
                if (SQLInterceptorFactory.class.isAssignableFrom(interceptorFactoryClass))
                {
                    // create a new instance
                    interceptorFactory = (SQLInterceptorFactory) interceptorFactoryClass.newInstance();
                }
            }
            catch (ClassNotFoundException | InstantiationException | IllegalAccessException e)
            {
                Debug.logError(e, "Unable to load SQLInterceptorFactory class. " + className);
            }
        }
        return interceptorFactory;
    }


    /**
     * This allows us to bridge back to the SQLInterceptor that does stuff but when it isnt a new SQLConnectionInterceptor
     */
    private static class DelegatingNoOpSQLConnectionInterceptor implements SQLConnectionInterceptor {

        private final SQLInterceptor sqlInterceptor;

        public DelegatingNoOpSQLConnectionInterceptor(SQLInterceptor delegate)
        {
            this.sqlInterceptor = delegate;
        }

        public void onConnectionTaken(Connection connection, ConnectionPoolState connectionPoolState)
        {
        }

        public void onConnectionReplaced(Connection connection, ConnectionPoolState connectionPoolState)
        {
        }

        public void beforeExecution(String sqlString, List<String> parameterValues, Statement statement)
        {
            sqlInterceptor.beforeExecution(sqlString, parameterValues, statement);
        }

        public void afterSuccessfulExecution(String sqlString, List<String> parameterValues, Statement statement, ResultSet resultSet, int rowsUpdated)
        {
            sqlInterceptor.afterSuccessfulExecution(sqlString, parameterValues, statement, resultSet, rowsUpdated);
        }

        public void onException(String sqlString, List<String> parameterValues, Statement statement, SQLException sqlException)
        {
            sqlInterceptor.onException(sqlString, parameterValues, statement, sqlException);
        }
    }
}
