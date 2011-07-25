package org.ofbiz.core.entity.jdbc.interceptors.connection;

import org.ofbiz.core.entity.config.ConnectionPoolInfo;
import org.ofbiz.core.entity.jdbc.SQLInterceptorSupport;
import org.ofbiz.core.entity.jdbc.interceptors.SQLInterceptor;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A class to track information about {@link Connection}s that come from connection pool.  It also will
 * invoke {@link SQLConnectionInterceptor}s with information about the Connection as it is used.
 */
public class ConnectionTracker
{
    /**
     * A symbolic constant for the the ConnectionPoolInfo is now known
     */
    static final ConnectionPoolInfo UNKNOWN_CONNECTION_POOL_INFO = new ConnectionPoolInfo(-1, -1, -1L, -1, -1, -1, -1, null, -1L, -1L);

    private final ConnectionPoolInfo connectionPoolInfo;
    private final AtomicInteger borrowedCount = new AtomicInteger(0);

    public ConnectionTracker()
    {
        this(UNKNOWN_CONNECTION_POOL_INFO);
    }

    /**
     * This allows you to have static information about the underlying connection pool.
     *
     * @param connectionPoolInfo the static information about the connection pool
     */
    public ConnectionTracker(final ConnectionPoolInfo connectionPoolInfo)
    {
        this.connectionPoolInfo = connectionPoolInfo != null ? connectionPoolInfo : UNKNOWN_CONNECTION_POOL_INFO;
    }

    /**
     * Called to track the connection as it is pulled from the underlying connection pool.
     *
     * @param helperName         the OfBiz helper name
     * @param getConnectionCall  a callable that returns a connection
     * @return the connection that was returned by the callable
     */
    public Connection trackConnection(final String helperName, final Callable<java.sql.Connection> getConnectionCall)
    {
    try
        {
            long then = System.nanoTime();
            Connection connection = getConnectionCall.call();
            return informInterceptor(helperName, connection, connectionPoolInfo, System.nanoTime() - then);

        }
        catch (Exception e)
        {
            throw new RuntimeException("Unable to obtain a connection from the underlying connection pool", e);
        }
    }

    private Connection informInterceptor(final String helperName, final Connection connection, final ConnectionPoolInfo connectionPoolInfo, final long timeTakenNanos)
    {
        // connections can be null so we have to handle that.   Its unlikely but the code path can make it so.
        if (connection == null)
        {
            return null;
        }
        final int count = borrowedCount.incrementAndGet();

        final SQLConnectionInterceptor sqlConnectionInterceptor = SQLInterceptorSupport.getNonNullSQLConnectionInterceptor(helperName);
        sqlConnectionInterceptor.onConnectionReplaced(connection, new ConnectionPoolStateImpl(timeTakenNanos, count, connectionPoolInfo));

        // We wrap the connection to that we can know when the connection is closed and hence returned to the pool.
        return new ConnectionWithSQLInterceptorImpl(connection, connectionPoolInfo, sqlConnectionInterceptor);
    }

    private class ConnectionWithSQLInterceptorImpl extends DelegatingConnection implements ConnectionWithSQLInterceptor
    {
        private final SQLConnectionInterceptor sqlConnectionInterceptor;
        private final ConnectionPoolInfo connectionPoolInfo;

        public ConnectionWithSQLInterceptorImpl(final Connection delegate, ConnectionPoolInfo connectionPoolInfo, final SQLConnectionInterceptor sqlConnectionInterceptor)
        {
            super(delegate);
            this.sqlConnectionInterceptor = sqlConnectionInterceptor;
            this.connectionPoolInfo = connectionPoolInfo;
        }

        @Override
        public void close() throws SQLException
        {
            super.close();
            final int count = borrowedCount.decrementAndGet();
            sqlConnectionInterceptor.onConnectionReplaced(getDelegate(), new ConnectionPoolStateImpl(0, count, connectionPoolInfo));
        }

        public SQLInterceptor getNonNullSQLInterceptor()
        {
            return sqlConnectionInterceptor;
        }
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

        public long getTimeToBorrow()
        {
            return timeToBorrowNanos / 1000000L;
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
