package org.ofbiz.core.entity.jdbc.dbtype;

import org.ofbiz.core.entity.jdbc.dbtype.AbstractDatabaseType;

import java.sql.Connection;
import java.sql.SQLException;

class Oracle10GDatabaseType extends AbstractDatabaseType {
    public Oracle10GDatabaseType() {
        super("Oracle 9i and 10g", "oracle10g", new String[]{"ORACLE"});
    }

    public boolean matchesConnection(Connection con) throws SQLException {
        return productNameMatches(con) && versionGreaterThanOrEqual(con, 9, 0);
    }

}
