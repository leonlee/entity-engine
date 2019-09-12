package org.ofbiz.core.entity.transaction;

import com.atlassian.util.concurrent.CopyOnWriteMap;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.ofbiz.core.entity.config.ConnectionPoolInfo;
import org.ofbiz.core.entity.config.JdbcDatasourceInfo;
import org.ofbiz.core.entity.jdbc.interceptors.connection.ConnectionTracker;
import org.ofbiz.core.util.Debug;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.Map;
import java.util.concurrent.Callable;

import static org.ofbiz.core.util.UtilValidate.isNotEmpty;

/**
 * HikariCP ConnectionFactory - central source for JDBC connections from the Hikari connection pool implementation.
 */
public class HikariCPConnectionFactory extends AbstractConnectionFactory {

    private static final String HIKARI_PROPERTIES = "hikari.properties";
    private static final Map<String, HikariDataSource> dsCache = CopyOnWriteMap.newHashMap();
    private static final Map<String, ConnectionTracker> trackerCache = CopyOnWriteMap.newHashMap();

    private static HikariConfigFactory hikariConfigFactory = new HikariConfigFactory();
    private static HikariDatasourceFactory hikariDatasourceFactory = new HikariDatasourceFactory();

    public static Connection getConnection(final String helperName, final JdbcDatasourceInfo jdbcDatasource) {

        HikariDataSource dataSource = dsCache.get(helperName);
        if (dataSource != null) {
            return trackConnection(helperName, dataSource);
        }

        try {
            synchronized (HikariCPConnectionFactory.class) {
                // Try again inside the block just in case someone when through while we were waiting.
                dataSource = dsCache.get(helperName);
                if (dataSource != null) {
                    return trackConnection(helperName, dataSource);
                }


                // Create the hikari pool config, attempting to load external properties if present.
                final HikariConfig config = hikariConfigFactory.getHikariConfig(HIKARI_PROPERTIES);

                // However, properties in the connection pool info will override anything set by the properties file.
                dataSource = createDataSource(config, jdbcDatasource);
                dataSource.setLogWriter(Debug.getPrintWriter());
                dsCache.put(helperName, dataSource);
                trackerCache.put(helperName, new ConnectionTracker(jdbcDatasource.getConnectionPoolInfo()));

                return trackConnection(helperName, dataSource);
            }
        } catch (Exception e) {
            Debug.logError(e, "Error getting datasource via HikariCP: " + jdbcDatasource);
        } catch (AbstractMethodError err) {
            if (checkIfProblemMayBeCausedByIsValidMethod(dataSource.getConnectionTestQuery(), err)) {
                unregisterDatasourceFromJmx(dataSource.getPoolName());
                logType4DriverWarning();
            }
        }

        return null;
    }

    public static void setHikariConfigFactory(final HikariConfigFactory factory) {
        hikariConfigFactory = factory;
    }

    public static void setHikariDatasourceFactory(final HikariDatasourceFactory factory) {
        hikariDatasourceFactory = factory;
    }

    private static HikariDataSource createDataSource(final HikariConfig config, final JdbcDatasourceInfo datasourceInfo) {
        final ConnectionPoolInfo connectionPoolInfo = datasourceInfo.getConnectionPoolInfo();
        config.setDriverClassName(datasourceInfo.getDriverClassName());
        config.setJdbcUrl(datasourceInfo.getUri());
        config.setUsername(datasourceInfo.getUsername());
        config.setPassword(datasourceInfo.getPassword());
        config.setCatalog(connectionPoolInfo.getDefaultCatalog());
        config.setMinimumIdle(connectionPoolInfo.getMinSize());
        config.setConnectionTestQuery(connectionPoolInfo.getValidationQuery());
        config.setMaximumPoolSize(connectionPoolInfo.getMaxSize());

        // The connection pool info expects this timeout to be set in seconds, Hikari expects it in milliseconds.
        Integer timeoutInSeconds = connectionPoolInfo.getValidationQueryTimeout();
        if (timeoutInSeconds != null) {
            config.setValidationTimeout(timeoutInSeconds * 1000);
        }

        if (isNotEmpty(datasourceInfo.getIsolationLevel())) {
            config.setTransactionIsolation(datasourceInfo.getIsolationLevel());
        }
        config.setRegisterMbeans(true);

        return hikariDatasourceFactory.createHikariDatasource(config);
    }

    private static Connection trackConnection(final String helperName, final DataSource dataSource) {
        final ConnectionTracker connectionTracker = trackerCache.get(helperName);
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
    public synchronized static void removeDatasource(final String helperName) {
        final HikariDataSource dataSource = dsCache.get(helperName);
        if (dataSource != null) {
            try {
                dataSource.close();
                unregisterMBean(dataSource.getPoolName());
            } catch (Exception e) {
                Debug.logError(e, "Error closing connection pool in HikariCP");
            }

            dsCache.remove(helperName);
        }
        trackerCache.remove(helperName);
    }
}
