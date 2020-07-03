package org.ofbiz.core.entity.jdbc.sql.escape;

import com.google.common.collect.Sets;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.ofbiz.core.entity.config.DatasourceInfo;
import org.ofbiz.core.entity.jdbc.dbtype.DatabaseType;

import java.sql.DatabaseMetaData;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SqlEscapeHelperTest {

    DatasourceInfo datasourceInfo;
    SqlEscapeHelper sqlEscapeHelper;
    DatabaseType databaseType;
    DatabaseMetaData databaseMetaData;

    private static final Set<String> RESERVED_KEYWORDS = Sets.newHashSet("ADMIN", "Secondary", "LEAD", "SOME", "SELECT");

    @Before
    public void setUp() {
        datasourceInfo = mock(DatasourceInfo.class);
        databaseType = spy(DatabaseType.class);
        databaseMetaData = mock(DatabaseMetaData.class);

        when(datasourceInfo.getDatabaseTypeFromJDBCConnection()).thenReturn(databaseType);
        when(databaseType.getReservedKeywords()).thenReturn(RESERVED_KEYWORDS);

        sqlEscapeHelper = new SqlEscapeHelper(datasourceInfo);
    }

    @Test
    public void shouldEscapeForMySQL8() {
        assertEquals("TEST", sqlEscapeHelper.escapeColumn("TEST"));
        assertEquals("\"SECONDARY\"", sqlEscapeHelper.escapeColumn("SECONDARY"));
        assertEquals("\"LEAD\"", sqlEscapeHelper.escapeColumn("LEAD"));
        assertEquals("\"ADMIN\"", sqlEscapeHelper.escapeColumn("ADMIN"));
    }
}
