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

    String STANDARD_SELECT_SYNTAX = "SELECT {0} FROM {1} WHERE {2}";

    String getName();

    String getFieldTypeName();

    /**
     * Returns the name of the connected schema.
     *
     * @param con the connection for which to return the schema
     * @return null if a schema name is not available
     */
    String getSchemaName(Connection con);

    int getConstraintNameClipLength();

    boolean matchesConnection(Connection con) throws SQLException;

    /**
     * Builds a DB-specific SQL statement to update a column's type.
     *
     * @param tableName     the name of the table to be changed.
     * @param columnName    the name of the column to be changed.
     * @param targetSqlType the target SQL type of the column.
     * @return the SQL text to change the column, or {@code null}, if not supported.
     */
    String getChangeColumnTypeSQL(String tableName, String columnName, String targetSqlType);

    /**
     * Builds a DB-specific SQL statement for dropping index.
     *
     * @param schemaName the of the schema containing table.
     * @param tableName  the name of the table to be changed.
     * @param indexName  the name of the index to be dropped.
     * @return the SQL text to drop the index.
     */
    String getDropIndexSQL(String schemaName, String tableName, String indexName);

    /**
     * Based on the cluster mode parameter, and the capabilities of the database, return the appropriate SELECT
     * statement for usage.
     */
    default String getSimpleSelectSqlSyntax(boolean clusterMode) {
        return STANDARD_SELECT_SYNTAX;
    }
}
