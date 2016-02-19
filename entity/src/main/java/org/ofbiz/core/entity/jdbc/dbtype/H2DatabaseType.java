package org.ofbiz.core.entity.jdbc.dbtype;

public class H2DatabaseType extends SimpleDatabaseType {
    public H2DatabaseType() {
        super("H2", "h2", new String[]{"H2"});
    }

    @Override
    protected String getChangeColumnTypeStructure() {
        return CHANGE_COLUMN_TYPE_CLAUSE_STRUCTURE_STANDARD_ALTER_COLUMN;
    }

    @Override
    public String getDropIndexStructure() {
        return DROP_INDEX_SCHEMA_DOT_INDEX;
    }
}
