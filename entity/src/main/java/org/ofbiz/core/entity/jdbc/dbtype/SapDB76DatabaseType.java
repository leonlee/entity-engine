package org.ofbiz.core.entity.jdbc.dbtype;

import org.ofbiz.core.util.Debug;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.DatabaseMetaData;

public class SapDB76DatabaseType extends AbstractDatabaseType
{
    public SapDB76DatabaseType() {
        super("SAP DB Version 7.6 or greater", "sapdb", new String[]{"SAP DB"});
    }

    public boolean matchesConnection(Connection con) throws SQLException {
        return productNameMatches(con) && versionGreaterThanOrEqual(con, 7, 6);
    }

    public String getSchemaName(Connection con)
    {
        try
        {
            DatabaseMetaData metaData = con.getMetaData();

            return metaData.getUserName().toUpperCase();
        }
        catch (SQLException e)
        {
            Debug.logError(e, "Exception occured while trying to find the schema name for the database connection to a DB2 database");
            return null;
        }
    }
}
