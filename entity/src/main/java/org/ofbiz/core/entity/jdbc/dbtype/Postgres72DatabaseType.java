package org.ofbiz.core.entity.jdbc.dbtype;

import org.ofbiz.core.entity.jdbc.dbtype.AbstractDatabaseType;

import java.sql.Connection;
import java.sql.SQLException;

class Postgres72DatabaseType extends AbstractPostgresDatabaseType {
    public Postgres72DatabaseType() {
        super("PostGres 7.2", "postgres72");
    }

    public boolean matchesConnection(Connection con) throws SQLException {
        return productNameMatches(con) &&
               postgresVersionGreaterThanOrEqual(con, 7, 2) &&
               postgresVersionLessThanOrEqual(con, 7, 2);
    }

    public String getSchemaName(Connection con) {
        return "";
    }

}
