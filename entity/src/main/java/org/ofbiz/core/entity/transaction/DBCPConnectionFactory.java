/*
 * $Id: DBCPConnectionFactory.java,v 1.1 2005/04/01 05:58:03 sfarquhar Exp $
 *
 * <p>Copyright (c) 2001 The Open For Business Project - www.ofbiz.org
 *
 * <p>Permission is hereby granted, free of charge, to any person obtaining a
 *  copy of this software and associated documentation files (the "Software"),
 *  to deal in the Software without restriction, including without limitation
 *  the rights to use, copy, modify, merge, publish, distribute, sublicense,
 *  and/or sell copies of the Software, and to permit persons to whom the
 *  Software is furnished to do so, subject to the following conditions:
 *
 * <p>The above copyright notice and this permission notice shall be included
 *  in all copies or substantial portions of the Software.
 *
 * <p>THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 *  OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 *  IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 *  CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT
 *  OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 *  THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.ofbiz.core.entity.transaction;

import com.atlassian.util.concurrent.CopyOnWriteMap;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.dbcp.BasicDataSourceFactory;
import org.apache.commons.dbcp.ManagedBasicDataSourceFactory;
import org.apache.log4j.Logger;
import org.ofbiz.core.entity.GenericEntityException;
import org.ofbiz.core.entity.config.ConnectionPoolInfo;
import org.ofbiz.core.entity.config.JdbcDatasourceInfo;
import org.ofbiz.core.entity.jdbc.interceptors.connection.ConnectionTracker;
import org.ofbiz.core.util.Debug;
import org.ofbiz.core.util.StringUtil;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Callable;
import javax.sql.DataSource;

import static org.ofbiz.core.entity.util.PropertyUtils.copyOf;
import static org.ofbiz.core.util.UtilValidate.isNotEmpty;

/**
 * DBCP ConnectionFactory - central source for JDBC connections from DBCP
 *
 * This is currently non transactional as DBCP doesn't seem to support transactional datasources yet (DBCP 1.0).
 *
 * @author <a href="mailto:mike@atlassian.com">Mike Cannon-Brookes</a>
 * @version 1.0
 * Created on Dec 18, 2001, 5:03 PM
 */
public class DBCPConnectionFactory {
    private static final Logger log = Logger.getLogger(DBCPConnectionFactory.class);
    private static final String DBCP_PROPERTIES = "dbcp.properties";
    protected static final Map<String, BasicDataSource> dsCache = CopyOnWriteMap.newHashMap();
    protected static final Map<String, ConnectionTracker> trackerCache = CopyOnWriteMap.newHashMap();
    private static final String PROP_JMX = "jmx";

    public static Connection getConnection(String helperName, JdbcDatasourceInfo jdbcDatasource) throws SQLException, GenericEntityException
    {
        // the DataSource implementation
        BasicDataSource dataSource = dsCache.get(helperName);
        if (dataSource != null) {
            return trackConnection(helperName,dataSource);
        }

        try
        {
            synchronized (DBCPConnectionFactory.class) {
                //try again inside the synch just in case someone when through while we were waiting
                dataSource = dsCache.get(helperName);
                if (dataSource != null) {
                    return trackConnection(helperName, dataSource);
                }

                // Sets the connection properties. At least 'user' and 'password' should be set.
                Properties info = jdbcDatasource.getConnectionProperties() != null ? copyOf(jdbcDatasource.getConnectionProperties()) : new Properties();
                if (jdbcDatasource.getUsername() != null) { info.setProperty("user", jdbcDatasource.getUsername()); }
                if (jdbcDatasource.getPassword() != null) { info.setProperty("password", jdbcDatasource.getPassword()); }

                // Use the BasicDataSourceFactory so we can use all the DBCP properties as per http://commons.apache.org/dbcp/configuration.html
                dataSource = createDataSource();
                dataSource.setDriverClassLoader(Thread.currentThread().getContextClassLoader());
                dataSource.setDriverClassName(jdbcDatasource.getDriverClassName());
                dataSource.setUrl(jdbcDatasource.getUri());
                dataSource.setConnectionProperties(toString(info));
                
                if (isNotEmpty(jdbcDatasource.getIsolationLevel()))
                {
                    dataSource.setDefaultTransactionIsolation(TransactionIsolations.fromString(jdbcDatasource.getIsolationLevel()));
                }

                // set connection pool attributes
                ConnectionPoolInfo poolInfo = jdbcDatasource.getConnectionPoolInfo();
                if (poolInfo != null)
                {
                    dataSource.setMaxActive(poolInfo.getMaxSize());
                    dataSource.setMaxWait(poolInfo.getMaxWait());
                    if (isNotEmpty(poolInfo.getValidationQuery()))
                    {
                        dataSource.setTestOnBorrow(true);
                        dataSource.setValidationQuery(poolInfo.getValidationQuery());
                    }
                    if (poolInfo.getMinEvictableTimeMillis() != null)
                    {
                        dataSource.setMinEvictableIdleTimeMillis(poolInfo.getMinEvictableTimeMillis());
                    }
                    if (poolInfo.getTimeBetweenEvictionRunsMillis() != null)
                    {
                        dataSource.setTimeBetweenEvictionRunsMillis(poolInfo.getTimeBetweenEvictionRunsMillis());
                    }
                }

                dataSource.setLogWriter(Debug.getPrintWriter());

                dsCache.put(helperName, dataSource);
                trackerCache.put(helperName,new ConnectionTracker(poolInfo));

                return trackConnection(helperName, dataSource);
            }
        } catch (Exception e) {
            Debug.logError(e, "Error getting datasource via DBCP: " + jdbcDatasource);
        }

        return null;
    }

    private static BasicDataSource createDataSource() throws Exception
    {
        Properties dbcpProperties = loadDbcpProperties();
        if (dbcpProperties.containsKey(PROP_JMX) && Boolean.valueOf(dbcpProperties.getProperty(PROP_JMX)))
        {
            return (BasicDataSource) ManagedBasicDataSourceFactory.createDataSource(dbcpProperties);
        }

        return (BasicDataSource) BasicDataSourceFactory.createDataSource(dbcpProperties);
    }

    private static String toString(Properties properties)
    {
        List<String> props = new ArrayList<String>();
        for (String key : properties.stringPropertyNames())
        {
            props.add(key + "=" + properties.getProperty(key));
        }

        return StringUtil.join(props, ";");
    }

    private static Properties loadDbcpProperties()
    {
        Properties dbcpProperties = new Properties();

        // load everything in c3p0.properties
        InputStream fileProperties = DBCPConnectionFactory.class.getResourceAsStream("/" + DBCP_PROPERTIES);
        if (fileProperties != null)
        {
            try
            {
                dbcpProperties.load(fileProperties);
            }
            catch (IOException e)
            {
                log.error("Error loading " + DBCP_PROPERTIES, e);
            }
        }

        // also look at all dbcp.* system properties
        Properties systemProperties = System.getProperties();
        for (String systemProp : systemProperties.stringPropertyNames())
        {
            final String prefix = "dbcp.";
            if (systemProp.startsWith(prefix))
            {
                dbcpProperties.setProperty(systemProp.substring(prefix.length()), System.getProperty(systemProp));
            }
        }

        return dbcpProperties;
    }

    private static Connection trackConnection(final String helperName, final DataSource dataSource)
    {
        ConnectionTracker connectionTracker = trackerCache.get(helperName);
        return connectionTracker.trackConnection(helperName, new Callable<Connection>()
        {
            public Connection call() throws Exception
            {
                return dataSource.getConnection();
            }
        });
    }

    /**
     * Shuts down and removes a datasource, if it exists
     *
     * @param helperName The name of the datasource to remove
     */
    public synchronized static void removeDatasource(String helperName)
    {
        BasicDataSource dataSource = dsCache.get(helperName);
        if (dataSource != null)
        {
            try
            {
                dataSource.close();
            }
            catch (Exception e)
            {
                Debug.logError(e, "Error closing connection pool in DBCP");
            }
            dsCache.remove(helperName);
        }
        trackerCache.remove(helperName);
    }
}
