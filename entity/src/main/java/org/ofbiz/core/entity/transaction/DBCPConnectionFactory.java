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
import org.apache.commons.dbcp.ConnectionFactory;
import org.apache.commons.dbcp.DriverManagerConnectionFactory;
import org.apache.commons.dbcp.PoolableConnectionFactory;
import org.apache.commons.dbcp.PoolingDataSource;
import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.ofbiz.core.entity.GenericEntityException;
import org.ofbiz.core.entity.config.ConnectionPoolInfo;
import org.ofbiz.core.entity.config.JdbcDatasourceInfo;
import org.ofbiz.core.entity.jdbc.interceptors.connection.ConnectionTracker;
import org.ofbiz.core.util.Debug;

import java.sql.Connection;
import java.sql.SQLException;
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

    protected static Map<String, DataSource> dsCache = CopyOnWriteMap.newHashMap();
    protected static Map<String, ObjectPool> connectionPoolCache = CopyOnWriteMap.newHashMap();
    protected static Map<String, ConnectionTracker> trackerCache = CopyOnWriteMap.newHashMap();

    public static Connection getConnection(String helperName, JdbcDatasourceInfo jdbcDatasource) throws SQLException, GenericEntityException {
        // the PooledDataSource implementation
        DataSource dataSource = dsCache.get(helperName);

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

                // First, we'll need a ObjectPool that serves as the actual pool of connections.
                GenericObjectPool connectionPool = new GenericObjectPool(null);
                connectionPoolCache.put(helperName, connectionPool);

                // Next, we'll create a ConnectionFactory that the pool will use to create Connections.
                ClassLoader loader = Thread.currentThread().getContextClassLoader();
                loader.loadClass(jdbcDatasource.getDriverClassName());

                // Sets the connection properties. At least 'user' and 'password' should be set.
                Properties info = jdbcDatasource.getConnectionProperties() != null ? copyOf(jdbcDatasource.getConnectionProperties()) : new Properties();
                if (jdbcDatasource.getUsername() != null) { info.setProperty("user", jdbcDatasource.getUsername()); }
                if (jdbcDatasource.getPassword() != null) { info.setProperty("password", jdbcDatasource.getPassword()); }
                
                ConnectionFactory connectionFactory = new DriverManagerConnectionFactory(jdbcDatasource.getUri(), info);

                // Now we'll create the PoolableConnectionFactory, which wraps
                // the "real" Connections created by the ConnectionFactory with
                // the classes that implement the pooling functionality.
                PoolableConnectionFactory poolableConnectionFactory = new PoolableConnectionFactory(connectionFactory, connectionPool, null, null, false, true);
                if (isNotEmpty(jdbcDatasource.getIsolationLevel()))
                {
                    poolableConnectionFactory.setDefaultTransactionIsolation(TransactionIsolations.fromString(jdbcDatasource.getIsolationLevel()));
                }

                // set connection pool attributes
                ConnectionPoolInfo poolInfo = jdbcDatasource.getConnectionPoolInfo();
                if (poolInfo != null)
                {
                    connectionPool.setMaxActive(poolInfo.getMaxSize());
                    connectionPool.setMaxWait(poolInfo.getMaxWait());
                    if (isNotEmpty(poolInfo.getValidationQuery()))
                    {
                        connectionPool.setTestOnBorrow(true);
                        poolableConnectionFactory.setValidationQuery(poolInfo.getValidationQuery());
                    }
                    if (poolInfo.getMinEvictableTimeMillis() != null)
                    {
                        connectionPool.setMinEvictableIdleTimeMillis(poolInfo.getMinEvictableTimeMillis());
                    }
                    if (poolInfo.getTimeBetweenEvictionRunsMillis() != null)
                    {
                        connectionPool.setTimeBetweenEvictionRunsMillis(poolInfo.getTimeBetweenEvictionRunsMillis());
                    }

                }

                // Finally, we create the PoolingDriver itself,
                // passing in the object pool we created.
                dataSource = new PoolingDataSource(connectionPool);

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
        DataSource dataSource = dsCache.get(helperName);
        if (dataSource != null)
        {
            ObjectPool connectionPool = connectionPoolCache.get(helperName);
            if (connectionPool != null)
            {
                try
                {
                    connectionPool.close();
                }
                catch (Exception e)
                {
                    Debug.logError(e, "Error closing connection pool in DBCP");
                }
                connectionPoolCache.remove(helperName);
            }
            dsCache.remove(helperName);
        }
        trackerCache.remove(helperName);
    }
}
