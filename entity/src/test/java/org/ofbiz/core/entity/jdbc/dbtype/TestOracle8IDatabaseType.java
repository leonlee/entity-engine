package org.ofbiz.core.entity.jdbc.dbtype;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TestOracle8IDatabaseType  extends AbstractDatabaseTypeTest
{
    protected DatabaseType databaseType;

    @Before
    public void setUp()
    {
        databaseType = getDatabaseType("Oracle 8i");
    }

    @Test
    public void oracleShouldModifySqlForUpdate() {
        // Set up
        final String sql = "select foo from bar";

        // Invoke
        final String sqlForUpdate = databaseType.selectForUpdate(sql);

        // Check
        assertEquals(sql + " FOR UPDATE", sqlForUpdate);
    }
}
