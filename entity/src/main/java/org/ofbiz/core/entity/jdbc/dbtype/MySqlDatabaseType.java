package org.ofbiz.core.entity.jdbc.dbtype;

public class MySqlDatabaseType extends SimpleDatabaseType
{
    public MySqlDatabaseType()
    {
        super("MySQL", "mysql", new String[]{"MySQL"});
    }

    @Override
    protected String getChangeColumnTypeStructure()
    {
        return CHANGE_COLUMN_TYPE_CLAUSE_STRUCTURE_STANDARD_ALTER_COLUMN;
    }
}
