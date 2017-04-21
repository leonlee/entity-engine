package org.ofbiz.core.entity.jdbc.dbtype;

import java.sql.Connection;
import java.sql.SQLException;

public class PostgresDatabaseType extends AbstractPostgresDatabaseType {
    protected PostgresDatabaseType() {
        super("Postgres 7.1 and earlier", "postgres");
    }

    public boolean matchesConnection(Connection con) throws SQLException {
        return productNameMatches(con) && postgresVersionLessThanOrEqual(con, 7, 1);
    }

    public String getSchemaName(Connection con) {
        return null;
    }

}
