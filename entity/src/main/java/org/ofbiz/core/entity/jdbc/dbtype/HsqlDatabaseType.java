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
    protected String getChangeColumnTypeStructure() {
        return CHANGE_COLUMN_TYPE_CLAUSE_STRUCTURE_STANDARD_MODIFY;
    }

    @Override
    public String getDropIndexStructure() {
        return DROP_INDEX_SCHEMA_DOT_INDEX;
    }

    @Override
    public String getSimpleSelectSqlSyntax(boolean clusterMode) {
        return STANDARD_SELECT_SYNTAX;
    }

}
