package org.ofbiz.core.entity.transaction;


import org.apache.commons.dbcp2.BasicDataSource;
import org.junit.Test;
import org.ofbiz.core.entity.config.ConnectionPoolInfo;
import org.ofbiz.core.entity.config.JdbcDatasourceInfo;

import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DBCPConnectionFactoryTest {

    @Test
    public void driverProblemTestShouldReturnTrueIfErrorStacktraceContainsIsValidAndValidationQueryIsNull() {
        BasicDataSource dataSource = mock(BasicDataSource.class);
        AbstractMethodError methodError = mock(AbstractMethodError.class);

        when(dataSource.getValidationQuery()).thenReturn(null);
        when(methodError.getStackTrace()).thenReturn(new StackTraceElement[]{new StackTraceElement("DBCPConnectionFactory", "isValid", "someFile", 10)});

        assertEquals(true, DBCPConnectionFactory.checkIfProblemMayBeCausedByIsValidMethod(dataSource, methodError));
    }

    @Test
    public void driverProblemTestShouldReturnFalseIfErrorStacktraceDoeNotContainsIsValidAndValidationQueryIsNull() {
        BasicDataSource dataSource = mock(BasicDataSource.class);
        AbstractMethodError methodError = mock(AbstractMethodError.class);

        when(dataSource.getValidationQuery()).thenReturn(null);
        when(methodError.getStackTrace()).thenReturn(new StackTraceElement[]{new StackTraceElement("DBCPConnectionFactory", "isNotValid", "someFile", 10)});

        assertEquals(false, DBCPConnectionFactory.checkIfProblemMayBeCausedByIsValidMethod(dataSource, methodError));
    }

    @Test
    public void driverProblemTestShouldReturnFalseIfValidationQueryIsNotEmpty() {
        BasicDataSource dataSource = mock(BasicDataSource.class);
        AbstractMethodError methodError = mock(AbstractMethodError.class);

        when(dataSource.getValidationQuery()).thenReturn("select 1");
        when(methodError.getStackTrace()).thenReturn(new StackTraceElement[]{new StackTraceElement("DBCPConnectionFactory", "isValid", "someFile", 10)});

        assertEquals(false, DBCPConnectionFactory.checkIfProblemMayBeCausedByIsValidMethod(dataSource, methodError));
    }

    @Test
    public void getUpdatedJdbcDatasourceShouldReturnInstanceWithAddedValidationQueryAndAllOtherParamsShouldBeTheSame () {
        final JdbcDatasourceInfo dsi = mock(JdbcDatasourceInfo.class);
        final ConnectionPoolInfo cpi = mock(ConnectionPoolInfo.class);

        final Properties props = new Properties();
        props.setProperty("test","test");

        when(cpi.getMaxSize()).thenReturn(1);
        when(cpi.getMinSize()).thenReturn(2);
        when(cpi.getMaxWait()).thenReturn(3L);
        when(cpi.getSleepTime()).thenReturn(4L);
        when(cpi.getLifeTime()).thenReturn(5L);
        when(cpi.getDeadLockMaxWait()).thenReturn(6L);
        when(cpi.getDeadLockRetryWait()).thenReturn(7L);
        when(cpi.getMinEvictableTimeMillis()).thenReturn(8L);
        when(cpi.getTimeBetweenEvictionRunsMillis()).thenReturn(9L);

        when(dsi.getConnectionPoolInfo()).thenReturn(cpi);
        when(dsi.getConnectionProperties()).thenReturn(props);
        when(dsi.getDriverClassName()).thenReturn("driverClassName");
        when(dsi.getIsolationLevel()).thenReturn("isolationLevel");
        when(dsi.getUsername()).thenReturn("username");
        when(dsi.getPassword()).thenReturn("password");
        when(dsi.getUri()).thenReturn("uri");

        final JdbcDatasourceInfo result = DBCPConnectionFactory.getUpdatedJdbcDatasource(dsi);

        assertEquals(props, result.getConnectionProperties());
        assertEquals("driverClassName", result.getDriverClassName());
        assertEquals("isolationLevel", result.getIsolationLevel());
        assertEquals("password", result.getPassword());
        assertEquals("username", result.getUsername());
        assertEquals("uri", result.getUri());

        assertEquals(1, result.getConnectionPoolInfo().getMaxSize());
        assertEquals(2, result.getConnectionPoolInfo().getMinSize());
        assertEquals(3L, result.getConnectionPoolInfo().getMaxWait());
        assertEquals(4L, result.getConnectionPoolInfo().getSleepTime());
        assertEquals(5L, result.getConnectionPoolInfo().getLifeTime());
        assertEquals(6L, result.getConnectionPoolInfo().getDeadLockMaxWait());
        assertEquals(7L, result.getConnectionPoolInfo().getDeadLockRetryWait());
        assertEquals(8L, result.getConnectionPoolInfo().getMinEvictableTimeMillis().longValue());
        assertEquals(9L, result.getConnectionPoolInfo().getTimeBetweenEvictionRunsMillis().longValue());
        assertEquals("select 1", result.getConnectionPoolInfo().getValidationQuery());
    }
}