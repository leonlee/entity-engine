package org.ofbiz.core.entity.jdbc.interceptors.connection;

import org.ofbiz.core.entity.jdbc.interceptors.SQLInterceptor;

import java.sql.Connection;

/**
 * An interceptor that knows about when connections are taken from the underlying connection pool
 */
public interface SQLConnectionInterceptor extends SQLInterceptor
{
    /**
     * Called when a connection is taken from the underlying pool
     *
     * @param connection the connection that has been borrowed
     * @param connectionPoolState info about the connection pool in at the time the connection was taken
     */
    void onConnectionTaken(Connection connection, ConnectionPoolState connectionPoolState);

    /**
     * Called when a connection is returned to the underlying pool
     *
     * @param connection the connection that has been returned
     * @param connectionPoolState info about the connection pool in at the time the connection was taken
     */
    void onConnectionReplaced(Connection connection, ConnectionPoolState connectionPoolState);
}
