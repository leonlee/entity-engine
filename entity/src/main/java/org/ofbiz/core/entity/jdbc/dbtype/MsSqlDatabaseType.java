package org.ofbiz.core.entity.jdbc.dbtype;

public class MsSqlDatabaseType extends SimpleDatabaseType
{
    private static final String WHERE_KEYWORD = "WHERE";

    public MsSqlDatabaseType()
    {
        super("MS SQL", "mssql", new String[]{"Microsoft SQL Server"});
    }

    @Override
    protected String getChangeColumnTypeStructure()
    {
        return CHANGE_COLUMN_TYPE_CLAUSE_STRUCTURE_STANDARD_ALTER_COLUMN;
    }

    @Override
    public String selectForUpdate(final String sql)
    {
        final int whereIndex = sql.toUpperCase().indexOf(WHERE_KEYWORD);
        if (whereIndex < 0) {
            // No WHERE keyword => selecting all rows from table, so probably not selecting for update
            return sql;
        }
        final int lastWhereIndex = sql.toUpperCase().lastIndexOf(WHERE_KEYWORD);
        if (whereIndex != lastWhereIndex) {
            // More than one WHERE keyword => must be a union, so probably not selecting for update
            return sql;
        }
        final String whereKeyword = sql.substring(whereIndex, whereIndex + WHERE_KEYWORD.length());
        return sql.replace(whereKeyword, "WITH (REPEATABLEREAD, UPDLOCK) " + whereKeyword);
    }
}
