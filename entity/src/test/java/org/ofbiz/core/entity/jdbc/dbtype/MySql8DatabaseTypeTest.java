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
    MySql8DatabaseType mySql8DatabaseType;

    @Mock
    Connection connection;

    @Mock
    DatabaseMetaData databaseMetaData;

    @Before
    public void setUp() throws SQLException {
        // when
        when(databaseMetaData.getSQLKeywords()).thenReturn("test, some, col, someColumn");
        when(connection.getMetaData()).thenReturn(databaseMetaData);
        mySql8DatabaseType.initialize(connection);
    }

    @Test
    public void shouldEscapeWithDoubleQuotesCharacters() throws SQLException {
        String someColumn = mySql8DatabaseType.escapeColumnName("someColumn");
        assertEquals("\"someColumn\"", someColumn);
    }

    @Test
    public void shouldNotEscapeWithDoubleQuotes() {
        String other = mySql8DatabaseType.escapeColumnName("Other");
        assertEquals("Other", other);
    }
}
