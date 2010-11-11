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

}
