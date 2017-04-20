package org.ofbiz.core.entity.jdbc.dbtype;

import java.sql.Connection;
import java.sql.SQLException;

public class Hsql233DatabaseType extends AbstractHsqlDatabaseType {

    public Hsql233DatabaseType() {
        super("HSQL 2.3.3 and later");
    }

    @Override
    public boolean matchesConnection(Connection con) throws SQLException {
        return productNameMatches(con) &&
                hsqlVersionGreaterThanOrEqual(con, 2, 3, 3);
    }

    @Override
    public String getSimpleSelectSqlSyntax(boolean clusterMode) {
        if (clusterMode) {
            return STANDARD_SELECT_FOR_UPDATE_SYNTAX;
        } else {
            return STANDARD_SELECT_SYNTAX;
        }
    }

}
