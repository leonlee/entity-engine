package org.ofbiz.core.entity.jdbc.dbtype;

public class MySqlDatabaseType extends SimpleDatabaseType {
    public MySqlDatabaseType() {
        super("MySQL", "mysql", new String[]{"MySQL"});
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

    @Override
    public Character getStartEscapeCharacter() {
        return ESCAPE_CHARACTER_BACK_TICK;
    }

    @Override
    public Character getEndEscapeCharacter() {
        return ESCAPE_CHARACTER_BACK_TICK;
    }
}
