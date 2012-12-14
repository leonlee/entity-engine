package org.ofbiz.core.entity.jdbc.dbtype;

import org.ofbiz.core.util.Debug;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

public class DB2DatabaseType extends SimpleDatabaseType
{
    public DB2DatabaseType() {
         super("DB2", "db2", new String[]{"DB2", "QDB2"}, 15);
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

    /*
       not a supported DB, but this is how it would look like:

    @Override
    protected String getChangeColumnTypeStructure()
    {
        return "ALTER TABLE {0} ALTER COLUMN {1} SET DATA TYPE {2}";
    }
    */
}
