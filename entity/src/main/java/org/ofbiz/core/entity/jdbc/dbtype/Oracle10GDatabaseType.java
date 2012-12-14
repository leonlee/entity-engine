package org.ofbiz.core.entity.jdbc.dbtype;

import java.sql.Connection;
import java.sql.SQLException;

class Oracle10GDatabaseType extends AbstractDatabaseType {
    public Oracle10GDatabaseType() {
        super("Oracle 9i and 10g", "oracle10g", new String[]{"ORACLE"});
    }

    public boolean matchesConnection(Connection con) throws SQLException {
        return productNameMatches(con) && versionGreaterThanOrEqual(con, 9, 0);
    }

    @Override
    protected String getChangeColumnTypeStructure()
    {
        return CHANGE_COLUMN_TYPE_CLAUSE_STRUCTURE_STANDARD_MODIFY;
    }
}
