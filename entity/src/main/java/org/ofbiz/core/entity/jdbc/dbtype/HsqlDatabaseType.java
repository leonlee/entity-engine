package org.ofbiz.core.entity.jdbc.dbtype;

import java.sql.Connection;
import java.sql.SQLException;

public class HsqlDatabaseType extends AbstractHsqlDatabaseType {
    public HsqlDatabaseType() {
        super("HSQL 2.3.2 and earlier");
    }

    @Override
    public boolean matchesConnection(Connection con) throws SQLException {
        return productNameMatches(con) &&
                hsqlVersionLessThanOrEqual(con, 2, 3, 2);
    }

    @Override
    public String getSimpleSelectSqlSyntax(boolean clusterMode) {
        return STANDARD_SELECT_SYNTAX;
    }

}
