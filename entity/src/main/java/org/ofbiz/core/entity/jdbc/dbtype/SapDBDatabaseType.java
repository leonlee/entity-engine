package org.ofbiz.core.entity.jdbc.dbtype;

import java.sql.Connection;
import java.sql.SQLException;

public class SapDBDatabaseType extends AbstractDatabaseType
{
    public SapDBDatabaseType() {
        super("SAP DB version 7.5 or less", "sapdb", new String[]{"SAP DB"});
    }
                                                                                       
    public boolean matchesConnection(Connection con) throws SQLException {
        return productNameMatches(con) && versionLessThanOrEqual(con, 7, 5);
    }
}
