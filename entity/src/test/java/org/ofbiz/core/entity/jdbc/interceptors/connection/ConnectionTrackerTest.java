package org.ofbiz.core.entity.jdbc.interceptors.connection;

import org.junit.Before;
import org.junit.Test;
import org.ofbiz.core.entity.jdbc.MockConnection;

import java.sql.Connection;
import java.util.concurrent.Callable;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class ConnectionTrackerTest
{
    MockConnection mockConnection;
    private ConnectionTracker connectionTracker;

    @Before
    public void setUp() throws Exception
    {
    }

    @Test
    public void testConnectionTracking() throws Exception
    {
        mockConnection = new MockConnection();

        connectionTracker = new ConnectionTracker();
        Connection wrappedConnection = connectionTracker.trackConnection("someHelperName", connectionCallable());
        assertThat(wrappedConnection instanceof ConnectionWithSQLInterceptor, equalTo(true));

        // this should cause the underlying connection to be closed
        wrappedConnection.close();

        assertThat(mockConnection.isClosed(), equalTo(true));

    }

    private Callable<Connection> connectionCallable()
    {
        return new Callable<Connection>()
        {
            @Override
            public Connection call() throws Exception
            {
                return mockConnection;
            }
        };
    }
}