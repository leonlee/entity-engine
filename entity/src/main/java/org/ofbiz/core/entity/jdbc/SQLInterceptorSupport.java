package org.ofbiz.core.entity.jdbc;

import org.ofbiz.core.entity.jdbc.interceptors.NoopSQLInterceptorFactory;
import org.ofbiz.core.entity.jdbc.interceptors.SQLInterceptor;
import org.ofbiz.core.entity.jdbc.interceptors.SQLInterceptorFactory;
import org.ofbiz.core.entity.util.ClassLoaderUtils;
import org.ofbiz.core.util.Debug;

import java.util.Properties;

/**
 * This will read "ofbiz-database.properties" and look for a "sqlinterceptor.factory.class" key.  It will then try and
 * instantiate a {@link org.ofbiz.core.entity.jdbc.interceptors.SQLInterceptorFactory} from this class name.
 * <p/>
 * If all this fails it will use a NO-OP interceptor factory and hence do nothing.
 */
class SQLInterceptorSupport
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
     * @param ofbizHelperName the name of the {@link org.ofbiz.core.entity.GenericHelper} in playl
     * @return a NON NULL {@link org.ofbiz.core.entity.jdbc.interceptors.SQLInterceptor}
     */
    static SQLInterceptor getNonNullSQLInterceptor(String ofbizHelperName)
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

    private static SQLInterceptorFactory loadInterceptorFactoryClass()
    {
        SQLInterceptorFactory interceptorFactory = NoopSQLInterceptorFactory.NOOP_INTERCEPTOR_FACTORY;

        String className = CONFIGURATION.getProperty(INTERCEPTOR_FACTORY_CLASS_NAME_KEY);
        if (className != null)
        {
            try
            {
                Class interceptorFactoryClass = ClassLoaderUtils.loadClass(className, SQLInterceptorSupport.class);
                if (SQLInterceptorFactory.class.isAssignableFrom(interceptorFactoryClass))
                {
                    // create a new instance
                    try
                    {
                        interceptorFactory = (SQLInterceptorFactory) interceptorFactoryClass.newInstance();
                    }
                    catch (InstantiationException e)
                    {
                        Debug.logError(e, "Unable to load SQLInterceptorFactory class. " + className);
                    }
                    catch (IllegalAccessException e)
                    {
                        Debug.logError(e, "Unable to load SQLInterceptorFactory class. " + className);
                    }
                }
            }
            catch (ClassNotFoundException e)
            {
                Debug.logError(e, "Unable to load SQLInterceptorFactory class. " + className);
            }
        }
        return interceptorFactory;
    }
}
