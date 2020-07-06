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
public class MySql8DatabaseTypeTest {

    @InjectMocks
    MySqlDatabaseType mySqlDatabaseType;

    @Mock
    Connection connection;

    @Mock
    DatabaseMetaData databaseMetaData;

    @Before
    public void setUp() throws SQLException {
        // when
        when(databaseMetaData.getSQLKeywords()).thenReturn("test, some, col, someColumn");
        when(connection.getMetaData()).thenReturn(databaseMetaData);
        mySqlDatabaseType.initialize(connection);
    }

    @Test
    public void shouldEscapeWithDoubleQuotesCharacters() throws SQLException {
        String someColumn = mySqlDatabaseType.escapeColumnName("someColumn");
        assertEquals("`someColumn`", someColumn);
    }

    @Test
    public void shouldEscapeWithDoubleQuotesCharactersCaseInsensitive() throws SQLException {
        String someColumn = mySqlDatabaseType.escapeColumnName("COL");
        assertEquals("`COL`", someColumn);
    }

    @Test
    public void shouldNotEscapeWithDoubleQuotes() {
        String other = mySqlDatabaseType.escapeColumnName("Other");
        assertEquals("Other", other);
    }
}
