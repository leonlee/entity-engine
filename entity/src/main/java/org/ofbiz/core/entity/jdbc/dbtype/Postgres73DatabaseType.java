package org.ofbiz.core.entity.jdbc.dbtype;

import java.sql.Connection;
import java.sql.SQLException;

public class Postgres73DatabaseType extends AbstractPostgresDatabaseType
{
    public Postgres73DatabaseType() {
        super("PostGres 7.3 and higher", "postgres72");
    }

    public boolean matchesConnection(Connection con) throws SQLException {
        return productNameMatches(con) &&
               postgresVersionGreaterThanOrEqual(con, 7, 3);
    }

    public String getSchemaName(Connection con) {
        return "public";
    }

    @Override
    protected String getChangeColumnTypeStructure()
    {
        return "ALTER TABLE {0} ALTER COLUMN {1} TYPE {2}";
    }

    @Override
    public String getDropIndexStructure()
    {
        return DROP_INDEX_SCHEMA_DOT_INDEX;
    }
}
