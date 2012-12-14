package org.ofbiz.core.entity.jdbc.dbtype;

public class HsqlDatabaseType extends SimpleDatabaseType
{
    public HsqlDatabaseType()
    {
        super("HSQL", "hsql", new String[]{"HSQL Database Engine"});
    }

    @Override
    protected String getChangeColumnTypeStructure()
    {
        return CHANGE_COLUMN_TYPE_CLAUSE_STRUCTURE_STANDARD_MODIFY;
    }
}
