package org.ofbiz.core.entity.transaction;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ofbiz.core.entity.config.ConnectionPoolInfo;
import org.ofbiz.core.entity.config.JdbcDatasourceInfo;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import java.sql.Connection;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;


@RunWith(PowerMockRunner.class)
@PrepareForTest({HikariCPConnectionFactory.class})
public class HikariCPConnectionFactoryTest {

    @Test
    public void driverProblemTestShouldReturnTrueIfErrorStacktraceContainsIsValidAndValidationQueryIsNull() {
        HikariDataSource dataSource = mock(HikariDataSource.class);
        AbstractMethodError methodError = mock(AbstractMethodError.class);

        when(dataSource.getConnectionTestQuery()).thenReturn(null);
        when(methodError.getStackTrace()).thenReturn(new StackTraceElement[]{new StackTraceElement("HikariCPConnectionFactory", "isValid", "someFile", 10)});

        assertEquals(true, DBCPConnectionFactory.checkIfProblemMayBeCausedByIsValidMethod(dataSource.getConnectionTestQuery(), methodError));
    }

    @Test
    public void driverProblemTestShouldReturnFalseIfErrorStacktraceDoeNotContainsIsValidAndValidationQueryIsNull() {
        HikariDataSource dataSource = mock(HikariDataSource.class);
        AbstractMethodError methodError = mock(AbstractMethodError.class);

        when(dataSource.getConnectionTestQuery()).thenReturn(null);
        when(methodError.getStackTrace()).thenReturn(new StackTraceElement[]{new StackTraceElement("HikariCPConnectionFactory", "isNotValid", "someFile", 10)});

        assertEquals(false, HikariCPConnectionFactory.checkIfProblemMayBeCausedByIsValidMethod(dataSource.getConnectionTestQuery(), methodError));
    }

    @Test
    public void driverProblemTestShouldReturnFalseIfValidationQueryIsNotEmpty() {
        HikariDataSource dataSource = mock(HikariDataSource.class);
        AbstractMethodError methodError = mock(AbstractMethodError.class);

        when(dataSource.getConnectionTestQuery()).thenReturn("select 1");
        when(methodError.getStackTrace()).thenReturn(new StackTraceElement[]{new StackTraceElement("HikariCPConnectionFactory", "isValid", "someFile", 10)});

        assertEquals(false, HikariCPConnectionFactory.checkIfProblemMayBeCausedByIsValidMethod(dataSource.getConnectionTestQuery(), methodError));
    }

    @Test
    public void willGetConnectionsFromPool() throws Exception {

        final ConnectionPoolInfo connectionPoolInfo = getConnectionPoolInfo();
        final JdbcDatasourceInfo jdbcDataSourceInfo = getJdbcDatasourceInfo(connectionPoolInfo);

        HikariConfig config = mock(HikariConfig.class);
        HikariDataSource datasource = mock(HikariDataSource.class);
        HikariConfigFactory configFactory = mock(HikariConfigFactory.class);
        HikariDatasourceFactory datasourceFactory = mock(HikariDatasourceFactory.class);
        Connection connection = mock(Connection.class);

        Whitebox.setInternalState(HikariCPConnectionFactory.class, "hikariConfigFactory", configFactory);
        Whitebox.setInternalState(HikariCPConnectionFactory.class, "hikariDatasourceFactory", datasourceFactory);

        when(configFactory.getHikariConfig("hikari.properties")).thenReturn(config);
        when(datasourceFactory.createHikariDatasource(config)).thenReturn(datasource);
        when(datasource.getConnection()).thenReturn(connection);

        // Get an initial connection.
        Connection connection1 = HikariCPConnectionFactory.getConnection("hikari", jdbcDataSourceInfo);

        // Get a second connection.
        Connection connection2 = HikariCPConnectionFactory.getConnection("hikari", jdbcDataSourceInfo);

        // Pool configuration should only happen once, i.e. that second connection won't trigger a recreate.
        verify(config, times(1)).setDriverClassName("org.h2.Driver");
        verify(config, times(1)).setJdbcUrl("jdbc:mysql://localhost:3306/bananas");
        verify(config, times(1)).setUsername("sa");
        verify(config, times(1)).setPassword("");
        verify(config, times(1)).setCatalog(null);
        verify(config, times(1)).setMinimumIdle(5);
        verify(config, times(1)).setConnectionTestQuery("select 1");
        verify(config, times(1)).setMaximumPoolSize(20);
        verify(config, times(1)).setValidationTimeout(5 * 1000);
        verify(config, times(1)).setTransactionIsolation("TRANSACTION_READ_COMMITTED");
        verify(config, times(1)).setRegisterMbeans(true);
        verify(configFactory, times(1)).getHikariConfig("hikari.properties");
        verify(datasourceFactory, times(1)).createHikariDatasource(config);
    }

    private JdbcDatasourceInfo getJdbcDatasourceInfo(ConnectionPoolInfo connectionPoolInfo) {
        return new JdbcDatasourceInfo(
                "jdbc:mysql://localhost:3306/bananas", "org.h2.Driver", "sa", "",
                "TRANSACTION_READ_COMMITTED", new Properties(), connectionPoolInfo);
    }

    private ConnectionPoolInfo getConnectionPoolInfo() {
        return ConnectionPoolInfo.builder()
                .setPoolInitialSize(10).setPoolMinSize(5).setPoolMaxSize(20)
                .setValidationQuery("select 1").setValidationQueryTimeout(5).build();
    }
}