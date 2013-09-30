package org.ofbiz.core.entity.jdbc.dbtype;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.ofbiz.core.entity.jdbc.dbtype.DatabaseTypeFactory.MSSQL;

public class TestMsSqlDatabaseType {

    private DatabaseType databaseType;

    @Before
    public void setUp() {
        this.databaseType = MSSQL;
    }

    @Test
    public void shouldNotUseLockingHintsWhenNoWhereClauseExists() {
        assertSqlUnchangedForUpdate(
                "select LastName from Person where CompanyFK = ? union select Surname from User where CompanyFK = ?");
    }

    @Test
    public void shouldNotUseLockingHintsWhenMultipleWhereClausesExist() {
        assertSqlUnchangedForUpdate("select count(*) from Issue");
    }

    private void assertSqlUnchangedForUpdate(final String sql) {
        assertEquals(sql, databaseType.selectForUpdate(sql));
    }

    // See http://technet.microsoft.com/en-us/library/ms187373.aspx for explanation of SQL Server locking hints
    @Test
    public void shouldUseLockingHintsInsteadOfForUpdateKeywords() {
        // Invoke
        final String sqlForUpdate = databaseType.selectForUpdate("SELECT Foo FROM Bar WHERE Baz = ? ORDER BY 1 DESC");

        // Check
        assertEquals("SELECT Foo FROM Bar WITH (UPDLOCK) WHERE Baz = ? ORDER BY 1 DESC", sqlForUpdate);
    }
}
