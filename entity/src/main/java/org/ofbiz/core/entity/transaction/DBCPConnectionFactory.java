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
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.commons.dbcp2.BasicDataSourceFactory;
import org.ofbiz.core.entity.GenericEntityException;
import org.ofbiz.core.entity.config.ConnectionPoolInfo;
import org.ofbiz.core.entity.config.JdbcDatasourceInfo;
import org.ofbiz.core.entity.jdbc.interceptors.connection.ConnectionTracker;
import org.ofbiz.core.util.Debug;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Callable;

import static org.ofbiz.core.entity.util.PropertyUtils.copyOf;
import static org.ofbiz.core.util.UtilValidate.isNotEmpty;

/**
 * DBCP ConnectionFactory - central source for JDBC connections from DBCP
 *
 * This is currently non transactional as DBCP doesn't seem to support transactional datasources yet (DBCP 1.0).
 *
 * @author <a href="mailto:mike@atlassian.com">Mike Cannon-Brookes</a>
 * @version 1.0
 *          Created on Dec 18, 2001, 5:03 PM
 */
public class DBCPConnectionFactory {
    private static final Logger log = LoggerFactory.getLogger(DBCPConnectionFactory.class);

    protected static final Map<String, BasicDataSource> dsCache = CopyOnWriteMap.newHashMap();
    protected static final Map<String, ConnectionTracker> trackerCache = CopyOnWriteMap.newHashMap();

    private static final String PROP_JMX = "jmx";
    private static final String DBCP_PROPERTIES = "dbcp.properties";
    private static final String PROP_MBEANNAME = "mbeanName";
    public static final String VALIDATION_QUERY = "select 1";

    public static Connection getConnection(String helperName, JdbcDatasourceInfo jdbcDatasource) throws SQLException, GenericEntityException {
        // the DataSource implementation
        BasicDataSource dataSource = dsCache.get(helperName);
        if (dataSource != null) {
            return trackConnection(helperName, dataSource);
        }

        try {
            synchronized (DBCPConnectionFactory.class) {
                //try again inside the synch just in case someone when through while we were waiting
                dataSource = dsCache.get(helperName);
                if (dataSource != null) {
                    return trackConnection(helperName, dataSource);
                }

                // Sets the connection properties. At least 'user' and 'password' should be set.
                final Properties info = jdbcDatasource.getConnectionProperties() != null ? copyOf(jdbcDatasource.getConnectionProperties()) : new Properties();

                // Use the BasicDataSourceFactory so we can use all the DBCP properties as per http://commons.apache.org/dbcp/configuration.html
                dataSource = createDataSource(jdbcDatasource);
                dataSource.setConnectionProperties(toString(info));
                // set connection pool attributes
                final ConnectionPoolInfo poolInfo = jdbcDatasource.getConnectionPoolInfo();
                initConnectionPoolSettings(dataSource, poolInfo);

                dataSource.setLogWriter(Debug.getPrintWriter());

                dsCache.put(helperName, dataSource);
                trackerCache.put(helperName, new ConnectionTracker(poolInfo));

                return trackConnection(helperName, dataSource);
            }
        } catch (Exception e) {
            Debug.logError(e, "Error getting datasource via DBCP: " + jdbcDatasource);
        } catch (AbstractMethodError err) {
            if (checkIfProblemMayBeCausedByIsValidMethod(dataSource, err)) {

                unregisterDatasourceFromJmx(dataSource);

                log.warn("*********************************************** IMPORTANT  ***********************************************");
                log.warn("                                                                                                          ");
                log.warn("  We found that you may experience problems with database connectivity because your database driver       ");
                log.warn("  is not fully JDBC 4 compatible. As a workaround of this problem a validation query was added to your    ");
                log.warn("  runtime configuration. Please add a line with validation query:                                       \n");
                log.warn("                          <validation-query>select 1</validation-query>                                 \n");
                log.warn("  to your JIRA_HOME/dbconfig.xml or update your database driver to version which fully supports JDBC 4.   ");
                log.warn("  More information about this problem can be found here: https://jira.atlassian.com/browse/JRA-59768      ");
                log.warn("                                                                                                          ");
                log.warn("**********************************************************************************************************");

                return getConnection(helperName, getUpdatedJdbcDatasource(jdbcDatasource));
            }
        }

        return null;
    }

    private static void unregisterDatasourceFromJmx(BasicDataSource dataSource) {
        final MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        try {
            mbs.unregisterMBean(ObjectName.getInstance(dataSource.getJmxName()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @VisibleForTesting
    static JdbcDatasourceInfo getUpdatedJdbcDatasource(JdbcDatasourceInfo jdbcDatasource) {

        final ConnectionPoolInfo cpi = jdbcDatasource.getConnectionPoolInfo();
        final ConnectionPoolInfo newConnectionPoolInfo = new ConnectionPoolInfo(cpi.getMaxSize(), cpi.getMinSize(), cpi.getMaxWait(), cpi.getSleepTime(), cpi.getLifeTime(), cpi.getDeadLockMaxWait(),
                cpi.getDeadLockRetryWait(), VALIDATION_QUERY, cpi.getMinEvictableTimeMillis(), cpi.getTimeBetweenEvictionRunsMillis());

        return new JdbcDatasourceInfo(jdbcDatasource.getUri(), jdbcDatasource.getDriverClassName(), jdbcDatasource.getUsername(), jdbcDatasource.getPassword(), jdbcDatasource.getIsolationLevel(),
                jdbcDatasource.getConnectionProperties(), newConnectionPoolInfo);
    }

    @VisibleForTesting
    static boolean checkIfProblemMayBeCausedByIsValidMethod(final BasicDataSource dataSource, final AbstractMethodError error) {
        final String validationQuery = dataSource.getValidationQuery();
        if (validationQuery == null || validationQuery.isEmpty()) {
            final List<StackTraceElement> stackTraceElements = Lists.newArrayList(error.getStackTrace());
            return stackTraceElements.stream().anyMatch(stackTraceElement -> stackTraceElement.getMethodName().contains("isValid"));
        }
        return false;
    }

    private static void initConnectionPoolSettings(final BasicDataSource dataSource, final ConnectionPoolInfo poolInfo) {
        if (poolInfo == null) {
            return;
        }

        dataSource.setMaxTotal(poolInfo.getMaxSize());
        dataSource.setMinIdle(poolInfo.getMinSize());
        dataSource.setMaxIdle(poolInfo.getMaxIdle());
        dataSource.setMaxWaitMillis(poolInfo.getMaxWait());
        dataSource.setDefaultCatalog(poolInfo.getDefaultCatalog());

        if (poolInfo.getInitialSize() != null) {
            dataSource.setInitialSize(poolInfo.getInitialSize());
        }

        if (isNotEmpty(poolInfo.getValidationQuery())) {
            // testOnBorrow defaults to true when this is set, but can still be forced to false
            dataSource.setTestOnBorrow(poolInfo.getTestOnBorrow() == null || poolInfo.getTestOnBorrow());
            if (poolInfo.getTestOnReturn() != null) {
                dataSource.setTestOnReturn(poolInfo.getTestOnReturn());
            }
            if (poolInfo.getTestWhileIdle() != null) {
                dataSource.setTestWhileIdle(poolInfo.getTestWhileIdle());
            }
            dataSource.setValidationQuery(poolInfo.getValidationQuery());
            if (poolInfo.getValidationQueryTimeout() != null) {
                dataSource.setValidationQueryTimeout(poolInfo.getValidationQueryTimeout());
            }
        }

        if (poolInfo.getPoolPreparedStatements() != null) {
            dataSource.setPoolPreparedStatements(poolInfo.getPoolPreparedStatements());
            if (dataSource.isPoolPreparedStatements() && poolInfo.getMaxOpenPreparedStatements() != null) {
                dataSource.setMaxOpenPreparedStatements(poolInfo.getMaxOpenPreparedStatements());
            }
        }

        if (poolInfo.getRemoveAbandonedOnBorrow() != null) {
            dataSource.setRemoveAbandonedOnBorrow(poolInfo.getRemoveAbandonedOnBorrow());
        }

        if (poolInfo.getRemoveAbandonedOnMaintanance() != null) {
            dataSource.setRemoveAbandonedOnMaintenance(poolInfo.getRemoveAbandonedOnMaintanance());
        }

        if (poolInfo.getRemoveAbandonedTimeout() != null) {
            dataSource.setRemoveAbandonedTimeout(poolInfo.getRemoveAbandonedTimeout());
        }

        if (poolInfo.getMinEvictableTimeMillis() != null) {
            dataSource.setMinEvictableIdleTimeMillis(poolInfo.getMinEvictableTimeMillis());
        }

        if (poolInfo.getNumTestsPerEvictionRun() != null) {
            dataSource.setNumTestsPerEvictionRun(poolInfo.getNumTestsPerEvictionRun());
        }

        if (poolInfo.getTimeBetweenEvictionRunsMillis() != null) {
            dataSource.setTimeBetweenEvictionRunsMillis(poolInfo.getTimeBetweenEvictionRunsMillis());
        }

    }

    private static BasicDataSource createDataSource(JdbcDatasourceInfo jdbcDatasource) throws Exception {
        final Properties dbcpProperties = loadDbcpProperties();

        final BasicDataSource dataSource = BasicDataSourceFactory.createDataSource(dbcpProperties);
        dataSource.setDriverClassLoader(Thread.currentThread().getContextClassLoader());
        dataSource.setDriverClassName(jdbcDatasource.getDriverClassName());
        dataSource.setUrl(jdbcDatasource.getUri());
        dataSource.setUsername(jdbcDatasource.getUsername());
        dataSource.setPassword(jdbcDatasource.getPassword());

        if (isNotEmpty(jdbcDatasource.getIsolationLevel())) {
            dataSource.setDefaultTransactionIsolation(TransactionIsolations.fromString(jdbcDatasource.getIsolationLevel()));
        }

        if (dbcpProperties.containsKey(PROP_JMX) && Boolean.valueOf(dbcpProperties.getProperty(PROP_JMX))) {
            dataSource.setJmxName(ObjectName.getInstance(dbcpProperties.getProperty(PROP_MBEANNAME)).getCanonicalName());
        }

        return dataSource;
    }

    private static String toString(Properties properties) {
        List<String> props = new ArrayList<String>();
        for (String key : properties.stringPropertyNames()) {
            props.add(key + "=" + properties.getProperty(key));
        }

        return Joiner.on(';').skipNulls().join(props);
    }

    private static Properties loadDbcpProperties() {
        Properties dbcpProperties = new Properties();

        // load everything in c3p0.properties
        InputStream fileProperties = DBCPConnectionFactory.class.getResourceAsStream("/" + DBCP_PROPERTIES);
        if (fileProperties != null) {
            try {
                dbcpProperties.load(fileProperties);
            } catch (IOException e) {
                log.error("Error loading " + DBCP_PROPERTIES, e);
            }
        }

        // also look at all dbcp.* system properties
        Properties systemProperties = System.getProperties();
        for (String systemProp : systemProperties.stringPropertyNames()) {
            final String prefix = "dbcp.";
            if (systemProp.startsWith(prefix)) {
                dbcpProperties.setProperty(systemProp.substring(prefix.length()), System.getProperty(systemProp));
            }
        }

        return dbcpProperties;
    }

    private static Connection trackConnection(final String helperName, final DataSource dataSource) {
        ConnectionTracker connectionTracker = trackerCache.get(helperName);
        return connectionTracker.trackConnection(helperName, new Callable<Connection>() {
            public Connection call() throws Exception {
                return dataSource.getConnection();
            }
        });
    }

    /**
     * Shuts down and removes a datasource, if it exists
     *
     * @param helperName The name of the datasource to remove
     */
    public synchronized static void removeDatasource(String helperName) {
        BasicDataSource dataSource = dsCache.get(helperName);
        if (dataSource != null) {
            try {
                dataSource.close();
                unregisterMBeanIfPresent();
            } catch (Exception e) {
                Debug.logError(e, "Error closing connection pool in DBCP");
            }


            dsCache.remove(helperName);
        }
        trackerCache.remove(helperName);
    }

    private static void unregisterMBeanIfPresent() {
        // We want to make sure mBean will be unregistered
        final Properties dbcpProperties = loadDbcpProperties();
        if (dbcpProperties.containsKey(PROP_JMX) && Boolean.valueOf(dbcpProperties.getProperty(PROP_JMX))) {
            final String mBeanName = dbcpProperties.getProperty(PROP_MBEANNAME);
            try {
                final ObjectName objectName = ObjectName.getInstance(dbcpProperties.getProperty(PROP_MBEANNAME));
                final MBeanServer platformMBeanServer = ManagementFactory.getPlatformMBeanServer();
                if (platformMBeanServer.isRegistered(objectName)) {
                    platformMBeanServer.unregisterMBean(objectName);
                }
            } catch (Exception e) {
                log.error("Exception un-registering MBean data source " + mBeanName, e);
            }
        }
    }
}
