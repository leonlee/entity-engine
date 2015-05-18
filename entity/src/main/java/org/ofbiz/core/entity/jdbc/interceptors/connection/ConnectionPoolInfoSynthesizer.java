package org.ofbiz.core.entity.jdbc.interceptors.connection;


import org.apache.commons.dbcp2.BasicDataSource;
import org.ofbiz.core.entity.config.ConnectionPoolInfo;


import java.lang.reflect.InvocationTargetException;

/**
 * Synthesizes {@link org.ofbiz.core.entity.config.ConnectionPoolInfo}s when there are not know at compile time
 */
public class ConnectionPoolInfoSynthesizer
{
    /**
     * This methods understands that there can be DBCP data sources and it knows how to generate {@link ConnectionPoolInfo} from that
     * under lying data source.
     *
     * @param ds the
     * @return ConnectionPoolInfo
     */
    public static ConnectionPoolInfo synthesizeConnectionPoolInfo(final javax.sql.DataSource ds)
    {
        if (ds instanceof BasicDataSource)
        {
            return copyBasicDataSource((BasicDataSource) ds);
        }
        //
        // Tomcat in its infinite wisdom renames the package structure of BasicDataSource without actually changing it
        // so we have to use reflection to get this to happen at runtime
        //
        else if ("org.apache.commons.dbcp2.BasicDataSource".equals(ds.getClass().getName()))
        {
            return reflectDataSource((BasicDataSource) ds);
        }
        else
        {
            return ConnectionTracker.UNKNOWN_CONNECTION_POOL_INFO;
        }
    }

    private static ConnectionPoolInfo copyBasicDataSource(BasicDataSource bds)
    {
        return new ConnectionPoolInfo(
                bds.getMaxTotal(), bds.getMinIdle(), (long) bds.getMaxWaitMillis(),
                -1, -1,
                -1, -1,
                bds.getValidationQuery(),
                -1L, -1L); //todo fix eviction
    }

    private static ConnectionPoolInfo reflectDataSource(BasicDataSource ds)
    {
        return new ConnectionPoolInfo(
                getInt(ds, "getMaxActive"), getInt(ds, "getMinIdle"), getLong(ds, "getMaxWait"),
                -1, -1,
                -1, -1,
                getStr(ds, "getValidationQuery"),
                getLong(ds, "getMinEvictableIdleTimeMillis"), getLong(ds, "getTimeBetweenEvictionRunsMillis")
        );
    }

    private static Integer getInt(BasicDataSource ds, String methodName)
    {
        try
        {
            Object val = ds.getClass().getMethod(methodName).invoke(ds);
            if (val instanceof Long)
            {
                return ((Long) val).intValue();
            }
            return (Integer) val;
        }
        catch (IllegalAccessException e)
        {
            return null;
        }
        catch (InvocationTargetException e)
        {
            return null;
        }
        catch (NoSuchMethodException e)
        {
            return null;
        }
    }

    private static Long getLong(BasicDataSource ds, String methodName)
    {
        try
        {
            Object val = ds.getClass().getMethod(methodName).invoke(ds);
            if (val instanceof Integer)
            {
                return ((Integer) val).longValue();
            }
            return (Long) val;
        }
        catch (IllegalAccessException e)
        {
            return null;
        }
        catch (InvocationTargetException e)
        {
            return null;
        }
        catch (NoSuchMethodException e)
        {
            return null;
        }
    }

    private static String getStr(BasicDataSource ds, String methodName)
    {
        try
        {
            return (String) ds.getClass().getMethod(methodName).invoke(ds);
        }
        catch (IllegalAccessException e)
        {
            return null;
        }
        catch (InvocationTargetException e)
        {
            return null;
        }
        catch (NoSuchMethodException e)
        {
            return null;
        }
    }
}
