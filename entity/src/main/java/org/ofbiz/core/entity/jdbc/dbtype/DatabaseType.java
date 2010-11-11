package org.ofbiz.core.entity.jdbc.dbtype;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Interface representing the different types of databases.  If you implement this interface,
 * you should register your implementation with DatabaseTypeFactory.
 *
 * @see DatabaseTypeFactory
 * @see DatabaseTypeFactory#registerDatabaseType(DatabaseType)
 */
public interface DatabaseType {

    String getName();

    String getFieldTypeName();

    String getSchemaName(Connection con);

    int getConstraintNameClipLength();

    boolean matchesConnection(Connection con) throws SQLException;

}
