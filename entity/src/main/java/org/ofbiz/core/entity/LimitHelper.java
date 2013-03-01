package org.ofbiz.core.entity;

import org.ofbiz.core.entity.model.ModelField;

import java.util.List;

/**
 * Amends the passed in SQL string to provide limit clause
 *
 * @since v1.0.24
 */
public class LimitHelper {
    
    private final String fieldTypeName;
    private final int SELECT_OFFSET = 7;


    public LimitHelper(String fieldTypeName) {
        this.fieldTypeName = fieldTypeName;
    }

    public String addLimitClause(String sql, List<ModelField> selectFields, int maxResults) {
         return addLimitClause(sql, selectFields, 0, maxResults);
    }

    public String addLimitClause(String sql, List<ModelField> selectFields, int offset,  int maxResults) {
        StringBuilder sqlBuilder = new StringBuilder(sql);
        // LIMIT clause
       
        if (maxResults > 0 ) {
            // mysql and postgres append LIMIT
            if (fieldTypeName.equals("mysql") || fieldTypeName.startsWith("postgres")) {
                sqlBuilder.append(" LIMIT ");
                sqlBuilder.append(maxResults);
                if (offset > 0) {
                    sqlBuilder.append(" OFFSET ");
                    sqlBuilder.append(offset);
                }
            }
            // if it's HSQLDB insert the top or limit
            if (fieldTypeName.equals("hsql")) {
                if (offset >0) {
                    sqlBuilder.insert(SELECT_OFFSET, "LIMIT "+ offset + " " + maxResults + " ");
                } else {
                    sqlBuilder = new StringBuilder(buildTopClause(sql, maxResults));
                }
            }
            // if it's SQL Server insert the top
            if ( fieldTypeName.equals("mssql")) {
                sqlBuilder = new StringBuilder(buildTopClause(sql, maxResults));
                if (offset > 0) {
                    sqlBuilder.append(" EXCEPT ");
                    sqlBuilder.append(buildTopClause(sql, offset));
                }
            }
            // if Oracle lets get a subquery going...
            if (fieldTypeName.startsWith("oracle")) {
                sqlBuilder = new StringBuilder(buildOracleClause(selectFields, maxResults, sql));
                if (offset > 0) {
                    sqlBuilder.append(" MINUS ");
                    sqlBuilder.append(buildOracleClause(selectFields, offset, sql));
                }
            }
        }
        return sqlBuilder.toString();
    }

    private String buildOracleClause(List<ModelField> selectFields, int maxResults, String sql) {
        final StringBuilder sqlBuilder = new StringBuilder(sql);
        sqlBuilder.insert(0, getOracleParentClause(selectFields));
        sqlBuilder.append(") WHERE ROWNUM <= ");
        sqlBuilder.append(maxResults);
        return sqlBuilder.toString();
    }

    private String buildTopClause(String sql, int maxResults) {
        StringBuilder sqlBuilder = new StringBuilder(sql);
        sqlBuilder.insert(SELECT_OFFSET, "TOP "+ maxResults + " ");
        return sqlBuilder.toString();
    }

    private String getOracleParentClause(List<ModelField> modelFields) {
        StringBuilder sqlBuilder = new StringBuilder("SELECT ");
        if (modelFields.isEmpty()) {
            sqlBuilder.append("*");
        } else {
            int i = 0;
            for (; i < modelFields.size() - 1; i++) {
                sqlBuilder.append(stripTableName(modelFields.get(i).getColName()));
                sqlBuilder.append(",");
            }
            sqlBuilder.append(stripTableName(modelFields.get(i).getColName()));
        }
        sqlBuilder.append(" FROM (");
        return sqlBuilder.toString();
    }
    
    private String stripTableName(String columnName) {
        return columnName.replaceAll(".*\\.","");
    }
        
}
