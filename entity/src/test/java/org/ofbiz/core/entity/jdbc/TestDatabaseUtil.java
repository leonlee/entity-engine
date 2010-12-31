package org.ofbiz.core.entity.jdbc;

import org.junit.Test;
import org.ofbiz.core.entity.ConnectionProvider;
import org.ofbiz.core.entity.GenericEntityException;
import org.ofbiz.core.entity.config.DatasourceInfo;
import org.ofbiz.core.entity.model.ModelEntity;
import org.ofbiz.core.entity.model.ModelField;
import org.ofbiz.core.entity.model.ModelIndex;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static junit.framework.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit test for {@link org.ofbiz.core.entity.jdbc.DatabaseUtil}.
 * <p/>
 * Baby steps...
 */
public class TestDatabaseUtil {
    @Test
    public void testCreateDeclaredIndicesNone() {
        DatabaseUtil du = new DatabaseUtil("Santa's Little Helper", null, null, null);
        ModelEntity modelEntity = new ModelEntity();
        final String mesg = du.createDeclaredIndices(modelEntity);
        assertNull("unexpected error", mesg);
    }

    @Test
    public void testCreateDeclaredIndicesBasic() throws Exception {
        final Connection connection = mock(Connection.class);
        final Statement statement = mock(Statement.class);
        when(connection.createStatement()).thenReturn(statement);
        DatabaseUtil du = new DatabaseUtil("Santa's Helper", null, null, new MyConnectionProvider(connection));
        ModelEntity modelEntity = new ModelEntity("testable", Collections.emptyList(), null);
        final ModelField modelField = new ModelField();
        modelField.setColName("nicecolumn");
        modelField.setName("fieldname");
        modelEntity.addField(modelField);
        final ModelIndex modelIndex = new ModelIndex();
        modelIndex.addIndexField("fieldname");
        modelEntity.addIndex(modelIndex);
        final String mesg = du.createDeclaredIndices(modelEntity);
        assertNull("unexpected error", mesg);
        verify(statement).executeUpdate("CREATE INDEX  ON TESTABLE (nicecolumn)");
    }

    /**
     * Mild test for this monster method.
     *
     * @throws Exception
     */
    @Test
    public void testCheckDb() throws Exception {
        final TreeSet<String> tables = new TreeSet<String>();
        tables.add("BOOKS");
        tables.add("AUTHORS");

        final Map<String, List<DatabaseUtil.ColumnCheckInfo>> columnInfo = new HashMap<String, List<DatabaseUtil.ColumnCheckInfo>>();
        DatabaseUtil.ColumnCheckInfo a1 = createCcInfo("AUTHORS", "NAME");
        DatabaseUtil.ColumnCheckInfo a2 = createCcInfo("AUTHORS", "AGE");

        DatabaseUtil.ColumnCheckInfo b1 = createCcInfo("BOOKS", "TITLE");
        DatabaseUtil.ColumnCheckInfo b2 = createCcInfo("BOOKS", "AUTHOR");

        columnInfo.put("AUTHORS", new ArrayList<DatabaseUtil.ColumnCheckInfo>(Arrays.asList(a1, a2)));
        columnInfo.put("BOOKS", new ArrayList<DatabaseUtil.ColumnCheckInfo>(Arrays.asList(b1, b2)));

        final Connection connection = mock(Connection.class);
        final DatabaseMetaData dbData = mock(DatabaseMetaData.class);
        when(dbData.supportsSchemasInTableDefinitions()).thenReturn(false);
        when(connection.getMetaData()).thenReturn(dbData);

        final Statement statement = mock(Statement.class);
        when(connection.createStatement()).thenReturn(statement);

        DatasourceInfo dsi = mock(DatasourceInfo.class);
        when(dsi.isUseFks()).thenReturn(false);
        when(dsi.isUseFkIndices()).thenReturn(false);
        when(dsi.isUseIndices()).thenReturn(true);

        DatabaseUtil du = new DatabaseUtil("Santa's Helper", null, dsi, new MyConnectionProvider(connection)) {
            @Override
            public TreeSet getTableNames(final Collection messages) {
                return tables;
            }

            @Override
            public Map getColumnInfo(final Set tableNames, final Collection messages) {
                return columnInfo;
            }

            @Override
            void checkFieldType(final ModelEntity entity, final ModelField field, final ColumnCheckInfo ccInfo, final Collection messages) {
                // do nothing... we're not testing field types here
            }
        };

        Map<String, ModelEntity> entities = new HashMap<String, ModelEntity>();
        entities.put("AUTHORS", createSimpleModelEntity("AUTHORS", "NAME", "AGE"));
        // deliberately missing title to produce one error message
        entities.put("BOOKS", createSimpleModelEntity("BOOKS", "AUTHOR"));
        ArrayList<String> messages = new ArrayList<String>();
        du.checkDb(entities, messages, true);
        assertEquals(4, messages.size());
        assertTrue(messages.get(0).matches(".*Checking #.*"));
        assertTrue(messages.get(1).matches(".*Checking #.*"));
        assertEquals("Column \"TITLE\" of table \"BOOKS\" of entity \"BOOKS\" exists in the database but has no corresponding field", messages.get(2));
        assertEquals("Entity \"BOOKS\" has 1 fields but table \"BOOKS\" has 2 columns.", messages.get(3));
    }

    @Test
    public void testGetIndexInfo() throws SQLException {
        final Connection connection = mock(Connection.class);
        final DatabaseMetaData dbData = mock(DatabaseMetaData.class);
        when(connection.getMetaData()).thenReturn(dbData);

        MockResultSet t1IndexRs = new MockResultSet();
        // t1 has two indexes, t2 has one non unique index and t3 has no indexes
        t1IndexRs.addRow(createIndexMetadataRow("t1_index1", "t1", false));
        t1IndexRs.addRow(createIndexMetadataRow("t1_index2", "t1", false));
        MockResultSet t2IndexRs = new MockResultSet();
        t2IndexRs.addRow(createIndexMetadataRow("t2_index", "t2", false));
        t2IndexRs.addRow(createIndexMetadataRow("t2_index_unique", "t2", true));
        MockResultSet t3IndexRs = new MockResultSet();

        when(dbData.getIndexInfo(anyString(), anyString(), eq("T1"), anyBoolean(), anyBoolean())).thenReturn(t1IndexRs);
        when(dbData.getIndexInfo(anyString(), anyString(), eq("T2"), anyBoolean(), anyBoolean())).thenReturn(t2IndexRs);
        when(dbData.getIndexInfo(anyString(), anyString(), eq("T3"), anyBoolean(), anyBoolean())).thenReturn(t3IndexRs);

        DatabaseUtil du = new DatabaseUtil("Santa's Helper", null, null, new MyConnectionProvider(connection));

        ArrayList messages = new ArrayList();
        HashSet<String> tableNames = new HashSet<String>(Arrays.asList("T1", "T2", "T3"));

        // the call to the production method
        final Map indexInfo = du.getIndexInfo(tableNames, messages);

        // the assertions...
        verify(dbData, times(3)).getIndexInfo(anyString(),anyString(), anyString(), eq(false), anyBoolean());
        assertTrue(indexInfo.entrySet().size() == 2);
        assertTrue("unexpected error messages", messages.isEmpty());
        final TreeSet<String> t1Indexes = new TreeSet<String>();
        t1Indexes.addAll(Arrays.asList("T1_INDEX1", "T1_INDEX2"));
        assertEquals(t1Indexes, indexInfo.get("T1"));
        final TreeSet<String> t2Indexes = new TreeSet<String>();
        t2Indexes.add("T2_INDEX");
        assertEquals(t2Indexes, indexInfo.get("T2"));
    }

    /**
     * Tests unique indexes. Primary key indexes are unique but foreign key indexes are not. Originally these two
     * were the only index types supported by the ofbiz index creation code. The unique flag was used to distinguish
     * between them. Since adding support for arbitrary indexes which may or may not be unique, we test to check that
     * both are detected.
     *
     * @throws Exception
     */
    @Test
    public void testGetIndexInfoUnique() throws Exception {
        final Connection connection = mock(Connection.class);
        final DatabaseMetaData dbData = mock(DatabaseMetaData.class);
        when(connection.getMetaData()).thenReturn(dbData);

        MockResultSet t1IndexRs = new MockResultSet();
        // t1 has two indexes, t2 has one non unique index and t3 has no indexes
        t1IndexRs.addRow(createIndexMetadataRow("t1_index1", "t1", false));
        t1IndexRs.addRow(createIndexMetadataRow("t1_index2", "t1", false));
        MockResultSet t2IndexRs = new MockResultSet();
        t2IndexRs.addRow(createIndexMetadataRow("t2_index", "t2", false));
        t2IndexRs.addRow(createIndexMetadataRow("t2_index_unique", "t2", true));
        MockResultSet t3IndexRs = new MockResultSet();

        when(dbData.getIndexInfo(anyString(), anyString(), eq("T1"), anyBoolean(), anyBoolean())).thenReturn(t1IndexRs);
        when(dbData.getIndexInfo(anyString(), anyString(), eq("T2"), anyBoolean(), anyBoolean())).thenReturn(t2IndexRs);
        when(dbData.getIndexInfo(anyString(), anyString(), eq("T3"), anyBoolean(), anyBoolean())).thenReturn(t3IndexRs);

        DatabaseUtil du = new DatabaseUtil("Santa's Helper", null, null, new MyConnectionProvider(connection));

        ArrayList messages = new ArrayList();
        HashSet<String> tableNames = new HashSet<String>(Arrays.asList("T1", "T2", "T3"));

        // the call to the production method
        final Map indexInfo = du.getIndexInfo(tableNames, messages, true);

        // the assertions...
        verify(dbData, times(3)).getIndexInfo(anyString(),anyString(), anyString(), eq(true), anyBoolean());
        assertTrue(indexInfo.entrySet().size() == 2);
        assertTrue("unexpected error messages", messages.isEmpty());
        final TreeSet<String> t1Indexes = new TreeSet<String>();
        t1Indexes.addAll(Arrays.asList("T1_INDEX1", "T1_INDEX2"));
        assertEquals(t1Indexes, indexInfo.get("T1"));
        final TreeSet<String> t2Indexes = new TreeSet<String>();
        t2Indexes.addAll(Arrays.asList("T2_INDEX", "T2_INDEX_UNIQUE"));
        assertEquals(t2Indexes, indexInfo.get("T2"));
    }

    @Test
    public void testError() {
        DatabaseUtil du = new DatabaseUtil(null, null, null, null);
        du.error("mesg", null);

        ArrayList<String> expected = new ArrayList<String>();
        expected.add("mesg");
        ArrayList in = new ArrayList();
        du.error("mesg", in);
        assertEquals(expected, in);
        du.error("mesg2", in);
        expected.add("mesg2");
        assertEquals(expected, in);        
    }
    @Test
    public void testImportant() {
        DatabaseUtil du = new DatabaseUtil(null, null, null, null);
        du.important("mesg", null);

        ArrayList<String> expected = new ArrayList<String>();
        expected.add("mesg");
        ArrayList in = new ArrayList();
        du.important("mesg", in);
        assertEquals(expected, in);
        du.important("mesg2", in);
        expected.add("mesg2");
        assertEquals(expected, in);    }
    @Test
    public void testWarn() {
        DatabaseUtil du = new DatabaseUtil(null, null, null, null);
        du.warn("mesg", null);

        ArrayList<String> expected = new ArrayList<String>();
        expected.add("mesg");
        ArrayList in = new ArrayList();
        du.warn("mesg", in);
        assertEquals(expected, in);
        du.warn("mesg2", in);
        expected.add("mesg2");
        assertEquals(expected, in);    }
    @Test
    public void testVerbose() {
        DatabaseUtil du = new DatabaseUtil(null, null, null, null);
        du.verbose("mesg", null);

        ArrayList<String> expected = new ArrayList<String>();
        expected.add("mesg");
        ArrayList in = new ArrayList();
        du.verbose("mesg", in);
        assertEquals(expected, in);
        du.verbose("mesg2", in);
        expected.add("mesg2");
        assertEquals(expected, in);    }

//    @Test
//    public void testMissingIndices() {
//        fail("TODO: check conditions under which the missing indices are created");
//        fail("TODO: look at TODOs about index creation");
//    }

    private ModelEntity createSimpleModelEntity(String tableAndEntityName, String... fields) {
        ModelEntity authorModelEntity = new ModelEntity();
        authorModelEntity.setEntityName(tableAndEntityName);
        authorModelEntity.setTableName(tableAndEntityName);
        for (String field : fields) {
            ModelField modelField = new ModelField();
            modelField.setType("longish-varchar");
            modelField.setName(field);
            modelField.setColName(field);
            authorModelEntity.addField(modelField);
        }
        return authorModelEntity;
    }

    private Map<String, Object> createIndexMetadataRow(final String indexName, final String tableName, boolean unique) {
        Map<String, Object> row = new HashMap<String, Object>();
        row.put("TYPE", DatabaseMetaData.tableIndexClustered);
        row.put("INDEX_NAME", indexName);
        row.put("TABLE_NAME", tableName);
        row.put("NON_UNIQUE", !unique);
        return row;
    }

    private DatabaseUtil.ColumnCheckInfo createCcInfo(String tableName, String colName) {
        DatabaseUtil.ColumnCheckInfo cci = new DatabaseUtil.ColumnCheckInfo();
        cci.columnName = colName;
        cci.columnSize = 55;
        cci.decimalDigits = 0;
        cci.isNullable = "YES";
        cci.tableName = tableName;
        return cci;
    }

    private static class MyConnectionProvider implements ConnectionProvider {
        private final Connection connection;

        public MyConnectionProvider(final Connection connection) {
            this.connection = connection;
        }

        public Connection getConnection(final String name) throws SQLException, GenericEntityException {
            return connection;
        }
    }
}
