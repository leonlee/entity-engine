package org.ofbiz.core.entity.jdbc.interceptors.connection;

import org.ofbiz.core.entity.config.ConnectionPoolInfo;

/**
 * This holds information about the state of the connection pool at the time a connection was borrowed from the
 * connection pool
 */
public interface ConnectionPoolState
{
    /**
     * @return the time it took to borrow this connection from the pool in nano seconds
     */
    long getTimeToBorrowNanos();

    /**
     * @return the time it took to borrow this connection from the pool in mill seconds
     */
    long getTimeToBorrow();

    /**
     * @return the number of objects that have been borrowed from the pool
     */
    int getBorrowedCount();

    /**
     * @return the static pool infomation, known when the pool was created.
     */
    ConnectionPoolInfo getConnectionPoolInfo();
}
