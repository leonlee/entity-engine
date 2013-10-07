package org.ofbiz.core.entity.jdbc.dbtype;

public class MsSqlDatabaseType extends SimpleDatabaseType
{
    public MsSqlDatabaseType()
    {
        super("MS SQL", "mssql", new String[]{"Microsoft SQL Server"});
    }

    @Override
    protected String getChangeColumnTypeStructure()
    {
        return CHANGE_COLUMN_TYPE_CLAUSE_STRUCTURE_STANDARD_ALTER_COLUMN;
    }
}
