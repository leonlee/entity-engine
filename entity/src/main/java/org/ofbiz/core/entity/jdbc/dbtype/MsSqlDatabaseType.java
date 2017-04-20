package org.ofbiz.core.entity.jdbc.dbtype;

public class MsSqlDatabaseType extends SimpleDatabaseType {
    public MsSqlDatabaseType() {
        super("MS SQL", "mssql", new String[]{"Microsoft SQL Server"});
    }

    @Override
    protected String getChangeColumnTypeStructure() {
        return CHANGE_COLUMN_TYPE_CLAUSE_STRUCTURE_STANDARD_ALTER_COLUMN;
    }

    @Override
    public String getSimpleSelectSqlSyntax(boolean clusterMode) {
        if (clusterMode) {
            return "SELECT {0} FROM {1} WITH (UPDLOCK,ROWLOCK) WHERE {2}";
        } else {
            return STANDARD_SELECT_SYNTAX;
        }
    }

}
