package org.ofbiz.core.entity;

import org.ofbiz.core.entity.model.ModelField;

import java.util.List;

/**
 * Amends the passed in SQL string to provide limit clause
 *
 * @since v1.0.24
 */
public class LimitHelper
{

    private final String databaseTypeName;
    private final int SELECT_OFFSET = 7;
    private final String SUBQUERY_VARIABLE = "sq_";

    public LimitHelper(String databaseTypeName)
    {
        this.databaseTypeName = databaseTypeName;
    }

    public String addLimitClause(String sql, List<ModelField> selectFields, int maxResults)
    {
        return addLimitClause(sql, selectFields, 0, maxResults);
    }

    public String addLimitClause(final String sql, final List<ModelField> selectFields, final int offset, final int maxResults)
    {
        StringBuilder sqlBuilder = new StringBuilder();
        final int limit = maxResults + offset;
        if (offset < 0)
        {
            throw new IllegalArgumentException(String.format("Offset %d is invalid, it  must be a valid non-negative integer.", offset));
        }
        if (sql.indexOf("ORDER BY") < 0)
        {
            throw new IllegalArgumentException(String.format("The SQL %s is invalid it does not have an ORDER BY clause.", sql));
        }
        if (maxResults > 0)
        {
            // mysql and postgres append LIMIT, which is shorthand for return this many rows, so substitute with maxResults.
            if (databaseTypeName.equals("mysql") || databaseTypeName.startsWith("postgres") || databaseTypeName.startsWith("h2"))
            {
                sqlBuilder.append(sql);
                sqlBuilder.append(" LIMIT ");
                sqlBuilder.append(maxResults);
                if (offset > 0)
                {
                    sqlBuilder.append(" OFFSET ");
                    sqlBuilder.append(offset);
                }
            }
            // if it's HSQLDB insert the limit
            else if (databaseTypeName.equals("hsql"))
            {
                if (offset > 0)
                {
                    sqlBuilder.append(sql);
                    sqlBuilder.insert(SELECT_OFFSET, "LIMIT " + offset + " " + maxResults + " ");
                }
                else
                {
                    sqlBuilder = new StringBuilder(buildTopClause(sql, maxResults));
                }
            }
            // if it's SQL Server build the SQL Server clause
            else if (databaseTypeName.equals("mssql"))
            {
               return new MSSQLClauseBuilder(selectFields, offset, limit, sql).buildSqlClause();
            }
            // if Oracle lets get a subquery going...
            else if (databaseTypeName.startsWith("oracle"))
            {
                 return new OracleClauseBuilder(selectFields, offset, limit, sql).buildSqlClause();
            }
            else
            {
                throw new IllegalArgumentException(String.format("The database type %s is not a supported database type.", databaseTypeName));
            }
        }
        else
        {
            sqlBuilder.append(sql);
        }
        return sqlBuilder.toString();
    }

    private String buildTopClause(String sql, int maxResults)
    {
        StringBuilder sqlBuilder = new StringBuilder(sql);
        sqlBuilder.insert(SELECT_OFFSET, "TOP " + maxResults + " ");
        return sqlBuilder.toString();
    }

    private String getParentClause(List<ModelField> modelFields, boolean useSubQueryVariable)
    {
        StringBuilder sqlBuilder = new StringBuilder("SELECT ");
        if (modelFields.isEmpty())
        {
            if (useSubQueryVariable)
            {
                sqlBuilder.append(SUBQUERY_VARIABLE);
                sqlBuilder.append(".");
            }
            sqlBuilder.append("*");
        }
        else
        {
            int i = 0;
            for (; i < modelFields.size() - 1; i++)
            {
                sqlBuilder.append(createQualifiedColumnName(modelFields.get(i).getColName(), useSubQueryVariable));
                sqlBuilder.append(",");
            }
            sqlBuilder.append(createQualifiedColumnName(modelFields.get(i).getColName(), useSubQueryVariable));
        }
        sqlBuilder.append(" FROM (");
        return sqlBuilder.toString();
    }

    private String createQualifiedColumnName(final String colName, boolean useSubQueryVariable)
    {
        final String subQueryPrefix = useSubQueryVariable ? SUBQUERY_VARIABLE + "." : "";
        return subQueryPrefix + stripTableName(colName);
    }

    private String stripTableName(String columnName)
    {
        return columnName.replaceAll(".*\\.", "");
    }


    private class OracleClauseBuilder
    {

        private final List<ModelField> selectFields;
        private final int offset;
        private final int limit;
        private final String sql;

        public OracleClauseBuilder(final List<ModelField> selectFields, final int offset, final int limit, final String sql)
        {

            this.selectFields = selectFields;
            this.offset = offset;
            this.limit = limit;
            this.sql = sql;
        }

        public String buildSqlClause()
        {
            String subQuery = buildlimitSqlClause();
            if (offset > 0)
            {
                subQuery = surroundSubQueryWithOffset(subQuery);
            }
            return subQuery;
        }

        private String buildlimitSqlClause()
        {
            final StringBuilder sqlBuilder = new StringBuilder(sql);
            sqlBuilder.insert(0, getParentClause(selectFields, true));
            sqlBuilder.append(") ");
            sqlBuilder.append(SUBQUERY_VARIABLE);
            sqlBuilder.append(" WHERE ROWNUM <= ");
            sqlBuilder.append(limit);
            return sqlBuilder.toString();
        }

        private String surroundSubQueryWithOffset(final String subQuery)
        {
            StringBuilder fullQueryBuilder = new StringBuilder(appendRownumInQuery(subQuery));
            fullQueryBuilder.insert(0, getParentClause(selectFields, false));
            fullQueryBuilder.append(") WHERE rnum > ");
            fullQueryBuilder.append(offset);
            return fullQueryBuilder.toString();
        }

        private String appendRownumInQuery(final String subQuery)
        {
            final StringBuilder builder = new StringBuilder(subQuery);
            int insertPosition = subQuery.indexOf("FROM") - 1;
            if (insertPosition > 0)
            {
                builder.insert(insertPosition, ",ROWNUM rnum");
            }
            return builder.toString();
        }
    }

    private class MSSQLClauseBuilder
    {
        private final List<ModelField> selectFields;
        private final int offset;
        private final int limit;
        private final String sql;
        private final static String ROW_NUMBER_FUNCTION = ", ROW_NUMBER() OVER (";

        public MSSQLClauseBuilder(final List<ModelField> selectFields, final int offset, final int limit, final String sql)
        {

            this.selectFields = selectFields;
            this.offset = offset;
            this.limit = limit;
            this.sql = sql;
        }

        public String buildSqlClause()
        {
            String subQuery = buildlimitSqlClause();
            if (offset > 0)
            {
                subQuery = appendOffset(subQuery);
            }
            return subQuery;
        }

        private String buildlimitSqlClause()
        {
            final StringBuilder sqlBuilder = new StringBuilder(moveOrderByClause(sql));
            sqlBuilder.insert(0, getParentClause(selectFields, true));
            sqlBuilder.append(") ");
            sqlBuilder.append(SUBQUERY_VARIABLE);
            sqlBuilder.append(" WHERE ");
            sqlBuilder.append(SUBQUERY_VARIABLE);
            sqlBuilder.append(".rnum <= ");
            sqlBuilder.append(limit);
            return sqlBuilder.toString();
        }

        private String moveOrderByClause(final String sql)
        {
            final StringBuilder builder = new StringBuilder();
            final int orderByPosition =  sql.indexOf("ORDER BY");
            builder.append(sql.substring(0, orderByPosition - 1));
            final String orderBy = sql.substring(orderByPosition);
            int insertPosition = sql.indexOf("FROM") - 1;
            if (insertPosition > 0)
            {
                builder.insert(insertPosition, ROW_NUMBER_FUNCTION + orderBy + ") rnum");
            }
            return builder.toString();
        }

        private String appendOffset(final String subQuery)
        {
            final StringBuilder builder = new StringBuilder(subQuery);
            builder.append(" AND ");
            builder.append(SUBQUERY_VARIABLE);
            builder.append(".rnum > ");
            builder.append(offset);
            return builder.toString();
        }
    }
}
