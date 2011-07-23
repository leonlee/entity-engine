package org.ofbiz.core.entity.jdbc.interceptors.connection;

import org.apache.commons.dbcp.DelegatingConnection;
import org.ofbiz.core.entity.config.ConnectionPoolInfo;
import org.ofbiz.core.entity.jdbc.SQLInterceptorSupport;

import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.NClob;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLXML;
import java.sql.Struct;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A class to track information about {@link Connection}s that come from connection pool.  It also will
 * invoke {@link SQLConnectionInterceptor}s with information about the Connection as it is used.
 */
public class ConnectionTracker
{
    private static final ConnectionPoolInfo UNKNOWN_CONNECTION_POOL_INFO = new ConnectionPoolInfo(-1, -1, -1, -1, -1, -1, null, -1L, -1L);

    private final ConnectionPoolInfo connectionPoolInfo;
    private final AtomicInteger borrowedCount = new AtomicInteger(0);

    public ConnectionTracker()
    {
        this(UNKNOWN_CONNECTION_POOL_INFO);
    }

    public ConnectionTracker(ConnectionPoolInfo connectionPoolInfo)
    {
        this.connectionPoolInfo = connectionPoolInfo != null ? connectionPoolInfo : UNKNOWN_CONNECTION_POOL_INFO;
    }

    /**
     * Called to track the connection as it is pulled from the underlying connection pool.
     *
     * @param helperName        the OfBiz helper name
     * @param getConnectionCall a callable that returns a connection
     *
     * @return the connection that was returned by the callable
     */
    public Connection trackConnection(final String helperName, final Callable<java.sql.Connection> getConnectionCall)
    {
        try
        {
            long then = System.nanoTime();
            Connection connection = getConnectionCall.call();
            return informInterceptor(helperName, connection, System.nanoTime() - then);

        } catch (Exception e)
        {
            throw new RuntimeException("Unable to obtain a connection from the underlying connection pool", e);
        }
    }

    private Connection informInterceptor(String helperName, Connection connection, long timeTakenNanos)
    {
        // connections can be null so we have to handle that.   Its unlikely but the code path can make it so.
        if (connection == null)
        {
            return null;
        }
        final int count = borrowedCount.incrementAndGet();

        final SQLConnectionInterceptor sqlConnectionInterceptor = SQLInterceptorSupport.getNonNullSQLConnectionInterceptor(helperName);
        sqlConnectionInterceptor.onConnectionReplaced(connection, new ConnectionPoolStateImpl(timeTakenNanos, count, connectionPoolInfo));

        // We wrap the connection to that we can know when the conneciton is closed and hence returned to the pool.
        return wrapConnection(connection, sqlConnectionInterceptor);
    }

    private Connection wrapConnection(final Connection connection, final SQLConnectionInterceptor sqlConnectionInterceptor)
    {
        return new DelegatingConnection(connection)
        {
            @Override
            public void close() throws SQLException
            {
                super.close();
                final int count = borrowedCount.decrementAndGet();
                sqlConnectionInterceptor.onConnectionReplaced(connection, new ConnectionPoolStateImpl(0, count, connectionPoolInfo));
            }

            //
            // 1.6 methods that are not in 1.5 delegating version
            //
            public Clob createClob() throws SQLException
            {
                return connection.createClob();
            }

            public Blob createBlob() throws SQLException
            {
                return connection.createBlob();
            }

            public NClob createNClob() throws SQLException
            {
                return connection.createNClob();
            }

            public SQLXML createSQLXML() throws SQLException
            {
                return connection.createSQLXML();
            }

            public boolean isValid(int timeout) throws SQLException
            {
                return connection.isValid(timeout);
            }

            public void setClientInfo(String name, String value) throws SQLClientInfoException
            {
                connection.setClientInfo(name, value);
            }

            public void setClientInfo(Properties properties) throws SQLClientInfoException
            {
                connection.setClientInfo(properties);
            }

            public String getClientInfo(String name) throws SQLException
            {
                return connection.getClientInfo(name);
            }

            public Properties getClientInfo() throws SQLException
            {
                return connection.getClientInfo();
            }

            public Array createArrayOf(String typeName, Object[] elements) throws SQLException
            {
                return connection.createArrayOf(typeName, elements);
            }

            public Struct createStruct(String typeName, Object[] attributes) throws SQLException
            {
                return connection.createStruct(typeName, attributes);
            }

            public <T> T unwrap(Class<T> iface) throws SQLException
            {
                return connection.unwrap(iface);
            }

            public boolean isWrapperFor(Class<?> iface) throws SQLException
            {
                return connection.isWrapperFor(iface);
            }
        };
    }

    private static class ConnectionPoolStateImpl implements ConnectionPoolState
    {
        private final long timeToBorrowNanos;
        private final int borrowCount;
        private final ConnectionPoolInfo connectionPoolInfo;

        private ConnectionPoolStateImpl(long timeToBorrowNanos, int borrowCount, ConnectionPoolInfo connectionPoolInfo)
        {
            this.timeToBorrowNanos = timeToBorrowNanos;
            this.borrowCount = borrowCount;
            this.connectionPoolInfo = connectionPoolInfo;
        }

        public long getTimeToBorrowNanos()
        {
            return timeToBorrowNanos;
        }

        public int getBorrowedCount()
        {
            return borrowCount;
        }

        public ConnectionPoolInfo getConnectionPoolInfo()
        {
            return connectionPoolInfo;
        }
    }

}
