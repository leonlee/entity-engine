package org.ofbiz.core.entity.jdbc.dbtype;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MsSqlDatabaseTypeTest {

    @InjectMocks
    MsSqlDatabaseType msSqlDatabaseType;

    @Mock
    Connection connection;

    @Mock
    DatabaseMetaData databaseMetaData;

    @Before
    public void setUp() throws SQLException {
        // when
        when(databaseMetaData.getSQLKeywords()).thenReturn("test, some, col, someColumn");
        when(connection.getMetaData()).thenReturn(databaseMetaData);
        msSqlDatabaseType.initialize(connection);
    }

    @Test
    public void shouldEscapeWithBracketsCharacters() throws SQLException {
        String someColumn = msSqlDatabaseType.escapeColumnName("someColumn");
        assertEquals("[someColumn]", someColumn);
    }

    @Test
    public void shouldNotEscapeWithBrackets() {
        String other = msSqlDatabaseType.escapeColumnName("Other");
        assertEquals("Other", other);
    }
}
