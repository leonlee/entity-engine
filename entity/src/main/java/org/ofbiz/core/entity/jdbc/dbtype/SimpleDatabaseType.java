package org.ofbiz.core.entity.jdbc.dbtype;

import java.sql.Connection;
import java.sql.SQLException;

public class SimpleDatabaseType extends AbstractDatabaseType {
    protected SimpleDatabaseType(String name, String fieldTypeName, String[] productNamePrefix, int constraintNameClipLength) {
        super(name, fieldTypeName, productNamePrefix, constraintNameClipLength);
    }

    protected SimpleDatabaseType(String name, String fieldTypeName, String[] productNamePrefix) {
        super(name, fieldTypeName, productNamePrefix);
    }
    /**
     * This implementation of the method only checks the productName returned by the driver
     * against a list of potential product name prefixes.
     */
    public boolean matchesConnection(Connection con) throws SQLException {
        return productNameMatches(con);
    }
}
