package org.ofbiz.core.entity.jdbc.sql.escape;

import org.ofbiz.core.entity.config.DatasourceInfo;
import org.ofbiz.core.entity.jdbc.dbtype.DatabaseType;

public class SqlEscapeHelper {

    protected DatabaseType databaseType;

    public SqlEscapeHelper(DatasourceInfo datasourceInfo) {
        this.databaseType = datasourceInfo.getDatabaseTypeFromJDBCConnection();
    }

    private SqlEscapeHelper() {
    }

    public String escapeColumn(String columnName) {
        return databaseType.escapeColumnName(columnName);
    }
}
