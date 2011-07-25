package org.ofbiz.core.entity.jdbc.interceptors.connection;

import org.ofbiz.core.entity.jdbc.interceptors.SQLInterceptor;

import java.sql.Connection;

/**
 * A {@link Connection} implementation that has a {@link org.ofbiz.core.entity.jdbc.interceptors.SQLInterceptor}  attached
 * to it
 */
public interface ConnectionWithSQLInterceptor extends Connection
{
    /**
     * @return the interceptor that was created and attached to this connection as it was handed out of a pool.  It MUST be non null!
     */
    SQLInterceptor getNonNullSQLInterceptor();
}
