package org.ofbiz.core.entity.jdbc.dbtype;

public class MySql8DatabaseType extends SimpleDatabaseType {
    public MySql8DatabaseType() {
        super("MySQL8", "mysql8", new String[]{"MySQL8"});
    }

    @Override
    protected String getChangeColumnTypeStructure() {
        return CHANGE_COLUMN_TYPE_CLAUSE_STRUCTURE_STANDARD_MODIFY;
    }

    @Override
    public String getDropIndexStructure() {
        return ALTER_TABLE_DROP_INDEX;
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
