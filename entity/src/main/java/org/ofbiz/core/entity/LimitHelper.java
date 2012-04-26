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


    public LimitHelper(String fieldTypeName) {
        this.fieldTypeName = fieldTypeName;
    }
    
    public String addLimitClause(String sql, List<ModelField> selectFields, int maxResults) {
        StringBuilder sqlBuilder = new StringBuilder(sql);
        // LIMIT clause
       
        if (maxResults > 0 ) {
            // mysql and postgres append LIMIT
            if (fieldTypeName.equals("mysql") || fieldTypeName.startsWith("postgres")) {
                sqlBuilder.append(" LIMIT ");
                sqlBuilder.append(maxResults);
            }
            // if it's SQL Server or HSQLDB insert the top
            if (fieldTypeName.equals("hsql") || fieldTypeName.equals("mssql")) {
                sqlBuilder.insert(7, "TOP "+ maxResults + " ");
            }
            // if Oracle lets get a subquery going...
            if (fieldTypeName.startsWith("oracle")) {
                
                sqlBuilder.insert(0, getOracleParentClause(selectFields));
                sqlBuilder.append(") WHERE ROWNUM <= ");
                sqlBuilder.append(maxResults);
            }
        }
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
