package org.ofbiz.core.entity.jdbc.dbtype;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TestHsqlDatabaseType
{
    private DatabaseType databaseType;

    @Before
    public void setUp() {
        databaseType = new HsqlDatabaseType();
    }

    @Test
    public void hsqlShouldNotModifySqlForUpdate() {
        // Set up
        final String sql = "select foo from bar";

        // Invoke
        final String sqlForUpdate = databaseType.selectForUpdate(sql);

        // Check
        assertEquals("HSQL does not support SELECT ... FOR UPDATE", sql, sqlForUpdate);
    }
}
