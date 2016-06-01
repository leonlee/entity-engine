package org.ofbiz.core.entity.model;

import org.ofbiz.core.entity.jdbc.dbtype.DatabaseType;
import org.ofbiz.core.entity.jdbc.dbtype.DatabaseTypeFactory;

import javax.annotation.Nullable;
import java.util.List;

/**
 *
 *  Base class for creating a function definition.  The main purpose is to return
 *  different function definitions based on database type, for instance the substr function
 *  is different in each of our supported databases.
 */
public abstract class FunctionDefinitionBuilder {

    private final String virtualColumn;
    private final String type;
    protected final List<String> columns;
    protected final String argsList;

    /**
     *
     * @param virtualColumn  the name of any virtual column to be created, must be provided, but only used in MySql and SQLServer
     * @param type   the model type of the virtual column, this should be the same as the function return type and is something like {@code "long-varchar"}.
     * @param columns a list of columns that the function operates on, must contain at least one column.
     * @param argsList an optional comma separated string of arguments to be provided to the function.
     */
    public FunctionDefinitionBuilder(String virtualColumn, String type, List<String> columns, @Nullable String argsList) {
        this.virtualColumn = virtualColumn;
        this.type = type;
        this.columns = columns;
        this.argsList = argsList;
    }

    @Nullable
    public String getVirtualColumn(DatabaseType dbType) {
        if (supportsFunctionBasedIndices(dbType)) {
            return virtualColumn;
        }
        return null;
    }

    public String getType() {
        return type;
    }

    public abstract String getFunctionDefinition(DatabaseType databaseType);

    public boolean supportsFunctionBasedIndices(DatabaseType dbType) {
        return DatabaseTypeFactory.ORACLE_10G == dbType || DatabaseTypeFactory.ORACLE_8I == dbType
                || DatabaseTypeFactory.POSTGRES  == dbType|| DatabaseTypeFactory.POSTGRES_7_2 == dbType
                || DatabaseTypeFactory.POSTGRES_7_3 == dbType;
    }

}
