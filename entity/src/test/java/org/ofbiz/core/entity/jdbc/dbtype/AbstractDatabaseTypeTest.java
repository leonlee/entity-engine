package org.ofbiz.core.entity.jdbc.dbtype;

import java.util.Collection;

public class AbstractDatabaseTypeTest
{
    protected DatabaseType getDatabaseType(final String typeName)
    {
        final Collection<DatabaseType> databaseTypes = DatabaseTypeFactory.DATABASE_TYPES;
        for (DatabaseType databaseType : databaseTypes)
        {
            if (databaseType.getName().equals(typeName))
            {
                return databaseType;
            }
        }
        return null;
    }
}
