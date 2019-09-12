package org.ofbiz.core.entity.config;

import org.ofbiz.core.entity.util.PropertyUtils;

import java.util.Properties;

/**
 * JDBC datasource descriptor
 */
public class JdbcDatasourceInfo {
    private final String uri;
    private final String driverClassName;
    private final String username;
    private final String password;
    private final String isolationLevel;
    private final Properties connectionProperties;
    private final ConnectionPoolInfo connectionPoolInfo;
    private final boolean useHikariConnectionPool;

    public JdbcDatasourceInfo(final String uri, final String driverClassName, final String username, final String password,
                              final String isolationLevel, final Properties connectionProperties, final ConnectionPoolInfo connectionPoolInfo) {
        this.uri = uri;
        this.driverClassName = driverClassName;
        this.username = username;
        this.password = password;
        this.isolationLevel = isolationLevel;
        this.connectionProperties = PropertyUtils.copyOf(connectionProperties);
        this.connectionPoolInfo = connectionPoolInfo;
        this.useHikariConnectionPool = false;
    }

    public JdbcDatasourceInfo(final String uri, final String driverClassName, final String username, final String password,
                              final String isolationLevel, final Properties connectionProperties, final ConnectionPoolInfo connectionPoolInfo,
                              final boolean useHikariConnectionPool) {
        this.uri = uri;
        this.driverClassName = driverClassName;
        this.username = username;
        this.password = password;
        this.isolationLevel = isolationLevel;
        this.connectionProperties = PropertyUtils.copyOf(connectionProperties);
        this.connectionPoolInfo = connectionPoolInfo;
        this.useHikariConnectionPool = useHikariConnectionPool;
    }


    public String getUri() {
        return uri;
    }

    public String getDriverClassName() {
        return driverClassName;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getIsolationLevel() {
        return isolationLevel;
    }

    public Properties getConnectionProperties() {
        return connectionProperties;
    }

    public ConnectionPoolInfo getConnectionPoolInfo() {
        return connectionPoolInfo;
    }

    public boolean useHikariConnectionPool() { return useHikariConnectionPool; }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("JdbcDatasourceInfo");
        sb.append("{uri='").append(uri).append('\'');
        sb.append(", driverClassName='").append(driverClassName).append('\'');
        sb.append(", username='").append(username).append('\'');
        sb.append(", password='").append("********").append('\'');
        sb.append(", isolationLevel='").append(isolationLevel).append('\'');
        sb.append(", connectionProperties=").append(connectionProperties);
        sb.append(", connectionPoolInfo=").append(connectionPoolInfo).append('\'');
        sb.append(", useHikariConnectionPool=").append(useHikariConnectionPool);
        sb.append('}');
        return sb.toString();
    }
}
