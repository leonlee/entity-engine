package org.ofbiz.core.entity;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Abstraction for acquiring a reference to a {@link java.sql.Connection}.
 */
public interface ConnectionProvider {

    /**
     * Gets the connecion given a name.
     *
     * @param name a name for the connection if required by the implementation.
     * @return a {@link java.sql.Connection}.
     * @throws SQLException on JDBC based failure.
     * @throws GenericEntityException on Ofbiz framework exception.
     */
    public Connection getConnection(String name) throws SQLException, GenericEntityException;
}
