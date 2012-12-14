package org.ofbiz.core.entity.jdbc;

import com.google.common.collect.ImmutableSet;
import org.junit.Test;
import org.ofbiz.core.entity.ConnectionProvider;
import org.ofbiz.core.entity.GenericEntityException;
import org.ofbiz.core.entity.config.DatasourceInfo;
import org.ofbiz.core.entity.model.ModelEntity;
import org.ofbiz.core.entity.model.ModelField;
import org.ofbiz.core.entity.model.ModelIndex;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

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
        ModelEntity modelEntity = new ModelEntity("testable", Collections.<DatabaseUtil.ColumnCheckInfo>emptyList(), null);
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
            public TreeSet<String> getTableNames(final Collection<String> messages) {
                return tables;
            }

            @Override
            public Map<String, List<ColumnCheckInfo>> getColumnInfo(final Set<String> tableNames, final Collection<String> messages) {
                return columnInfo;
            }

            @Override
            void checkFieldType(ModelEntity entity, ModelField field, ColumnCheckInfo ccInfo, Collection<String> messages,
					boolean promote, boolean widen)
            {
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

        when(dbData.getIndexInfo(anyString(), anyString(), eq("t1"), anyBoolean(), anyBoolean())).thenReturn(t1IndexRs);
        when(dbData.getIndexInfo(anyString(), anyString(), eq("t2"), anyBoolean(), anyBoolean())).thenReturn(t2IndexRs);
        when(dbData.getIndexInfo(anyString(), anyString(), eq("t3"), anyBoolean(), anyBoolean())).thenReturn(t3IndexRs);

        DatabaseUtil du = new DatabaseUtil("Santa's Helper", null, null, new MyConnectionProvider(connection));

        ArrayList<String> messages = new ArrayList<String>();
        HashSet<String> tableNames = new HashSet<String>(Arrays.asList("t1", "t2", "t3"));

        // the call to the production method
        final Map<String, Set<String>> indexInfo = du.getIndexInfo(tableNames, messages);

        // the assertions...
        verify(dbData, times(3)).getIndexInfo(anyString(), anyString(), anyString(), eq(false), anyBoolean());
        assertTrue(indexInfo.entrySet().size() == 2);
        assertTrue("unexpected error messages", messages.isEmpty());
        final TreeSet<String> t1Indexes = new TreeSet<String>();
        t1Indexes.addAll(Arrays.asList("T1_INDEX1", "T1_INDEX2"));
        assertEquals(t1Indexes, indexInfo.get("t1"));
        final TreeSet<String> t2Indexes = new TreeSet<String>();
        t2Indexes.add("T2_INDEX");
        assertEquals(t2Indexes, indexInfo.get("t2"));
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

        when(dbData.getIndexInfo(anyString(), anyString(), eq("t1"), anyBoolean(), anyBoolean())).thenReturn(t1IndexRs);
        when(dbData.getIndexInfo(anyString(), anyString(), eq("t2"), anyBoolean(), anyBoolean())).thenReturn(t2IndexRs);
        when(dbData.getIndexInfo(anyString(), anyString(), eq("t3"), anyBoolean(), anyBoolean())).thenReturn(t3IndexRs);

        DatabaseUtil du = new DatabaseUtil("Santa's Helper", null, null, new MyConnectionProvider(connection));

        ArrayList<String> messages = new ArrayList<String>();
        HashSet<String> tableNames = new HashSet<String>(Arrays.asList("t1", "t2", "t3"));

        // the call to the production method
        final Map<String, Set<String>> indexInfo = du.getIndexInfo(tableNames, messages, true);

        // the assertions...
        verify(dbData, times(3)).getIndexInfo(anyString(), anyString(), anyString(), eq(false), anyBoolean());
        assertTrue(indexInfo.entrySet().size() == 2);
        assertTrue("unexpected error messages", messages.isEmpty());
        final TreeSet<String> t1Indexes = new TreeSet<String>();
        t1Indexes.addAll(Arrays.asList("T1_INDEX1", "T1_INDEX2"));
        assertEquals(t1Indexes, indexInfo.get("t1"));
        final TreeSet<String> t2Indexes = new TreeSet<String>();
        t2Indexes.addAll(Arrays.asList("T2_INDEX", "T2_INDEX_UNIQUE"));
        assertEquals(t2Indexes, indexInfo.get("t2"));
    }

    /**
     * Test JRA-28526 ensure that tables are fetched from data base with the same schema
     * as they are created. Problem was occurring when no schema was provided for mssql or postgres database.
     *
     */
    @Test
    public void testGetTables() throws Exception
    {
        //with
        final Connection connection = mock(Connection.class);
        final DatabaseMetaData dbData = mock(DatabaseMetaData.class);
        when(connection.getMetaData()).thenReturn(dbData);
        when(dbData.supportsSchemasInTableDefinitions()).thenReturn(Boolean.TRUE);
        when(dbData.getUserName()).thenReturn("user");
        final ResultSet rightResultSet = mock(ResultSet.class);
        final String[] types = {"TABLE", "VIEW", "ALIAS", "SYNONYM"};

        when(dbData.getTables(null,null,null, types)).thenReturn(rightResultSet);

        //use this as to be sure that no schema user is used
        final ResultSet emptyResult = mock(ResultSet.class);
        when(dbData.getTables(null,"user",null, types)).thenReturn(emptyResult);
        final DatasourceInfo dataSourceInfo = mock(DatasourceInfo.class);
        final DatabaseUtil du = new DatabaseUtil("Santa's Helper", null, dataSourceInfo, new MyConnectionProvider(connection));

        when(rightResultSet.next()).thenReturn(Boolean.TRUE, Boolean.TRUE, Boolean.FALSE);
        when(rightResultSet.getString("TABLE_NAME")).thenReturn("table1", "table2");
        when(rightResultSet.getString("TABLE_TYPE")).thenReturn("TABLE");
        final Collection<String> messages = new ArrayList<String>();

        //invoke
        final TreeSet<String> tableNames = du.getTableNames(messages);

        //then
        assertEquals(ImmutableSet.of("TABLE1", "TABLE2"),tableNames);

    }
    /**
     * Test JRA-28526 ensure that tables are fetched from data base with the same schema
     * as they are created. For the Oracle db it is the user schema name
     *
     */
    @Test
    public void testGetTablesForOracle() throws Exception
    {
        //with
        final Connection connection = mock(Connection.class);
        final DatabaseMetaData dbData = mock(DatabaseMetaData.class);
        when(connection.getMetaData()).thenReturn(dbData);
        when(dbData.supportsSchemasInTableDefinitions()).thenReturn(Boolean.TRUE);
        when(dbData.getUserName()).thenReturn("user");
        final ResultSet rightResultSet = mock(ResultSet.class);
        final String[] types = {"TABLE", "VIEW", "ALIAS", "SYNONYM"};

        final ResultSet emptyResult = mock(ResultSet.class);
        //use this as to be sure that user as schema is used
        when(dbData.getTables(null,null,null, types)).thenReturn(emptyResult);

        when(dbData.getTables(null,"user",null, types)).thenReturn(rightResultSet);
        final DatasourceInfo dataSourceInfo = mock(DatasourceInfo.class);
        when(dbData.getDatabaseProductName()).thenReturn("Oracle");

        final DatabaseUtil du = new DatabaseUtil("Santa's Helper", null, dataSourceInfo, new MyConnectionProvider(connection));

        when(rightResultSet.next()).thenReturn(Boolean.TRUE, Boolean.TRUE, Boolean.FALSE);
        when(rightResultSet.getString("TABLE_NAME")).thenReturn("table1", "table2");
        when(rightResultSet.getString("TABLE_TYPE")).thenReturn("TABLE");
        final Collection<String> messages = new ArrayList<String>();

        //invoke
        final TreeSet<String> tableNames = du.getTableNames(messages);

        //then
        assertEquals(ImmutableSet.of("TABLE1", "TABLE2"),tableNames);

    }
    @Test
    public void testMissingIndices() {

        // record requests for index info
        final AtomicReference<Set<String>> tableNamesReceived = new AtomicReference<Set<String>>();
        final AtomicBoolean includeUniqueReceived = new AtomicBoolean();

        final Map<String, Set<String>> indexInfo = new HashMap<String, Set<String>>();
        indexInfo.put("e1", Collections.<String>emptySet());
        indexInfo.put("e2", quickSet("E2I2"));
        indexInfo.put("e3", Collections.<String>emptySet());

        // record requests to create indexes
        final HashMap<String, Set<String>> creationCalls = new HashMap<String, Set<String>>();

        DatabaseUtil du = new DatabaseUtil(null, null, null, null) {
            @Override
            Map<String, Set<String>> getIndexInfo(final Set<String> tableNames, final Collection<String> messages, final boolean includeUnique) {
                tableNamesReceived.set(tableNames);
                if (includeUniqueReceived.get()) {
                    fail("unexpected second call (breaks test invariant)");
                }
                includeUniqueReceived.set(includeUnique);
                return indexInfo;
            }

            @Override
            public String createDeclaredIndex(ModelEntity entity, ModelIndex modelIndex) {
                final String name = entity.getEntityName();
                if (!creationCalls.containsKey(name)) {
                    creationCalls.put(name, new HashSet<String>());
                }
                creationCalls.get(name).add(modelIndex.getName());
                // report a failure on this one
                if (modelIndex.getName().equals("e2i1")) {
                    return "NO CAN DO";
                }
                return null;
            }
        };
        ArrayList<String> messages = new ArrayList<String>();
        HashMap<String, ModelEntity> modelEntities = new HashMap<String, ModelEntity>();

        {
            // entity e1 has one index
            ModelEntity e1 = createSimpleModelEntity("e1", "e1f1", "e1f2");
            ModelIndex e1i1 = new ModelIndex();
            e1i1.setName("e1i1");
            e1i1.addIndexField("e1f1");
            e1i1.setUnique(false);
            e1.addIndex(e1i1);
            modelEntities.put("e1", e1);
        }

        {
            // entity e2 has two indexes, one with two fields
            ModelEntity e2 = createSimpleModelEntity("e2", "e2f1", "e2f2", "e2f3");
            ModelIndex e2i1 = new ModelIndex();
            e2i1.setName("e2i1");
            e2i1.addIndexField("e2f1");
            e2i1.setUnique(true);
            e2.addIndex(e2i1);
            ModelIndex e2i2 = new ModelIndex();
            e2i2.setName("e2i2");
            e2i2.addIndexField("e2f2");
            e2i2.addIndexField("e2f3");
            e2i1.setUnique(false);
            e2.addIndex(e2i2);
            modelEntities.put("e2", e2);
        }

        // enity e3 has no indexes
        ModelEntity e3 = createSimpleModelEntity("e3", "e3f1", "e3f2");
        modelEntities.put("e3", e3);

        // production call!
        du.createMissingIndices(modelEntities, messages);

        // assert index info calls all received
        assertTrue(tableNamesReceived.get().containsAll(Arrays.asList("e1", "e2", "e3")));
        assertTrue(tableNamesReceived.get().size() == 3);
        assertTrue(includeUniqueReceived.get());

        // assert calls to create missing indexes were correct
        assertEquals("expected one call to add e1i1 for entity e1", quickSet("e1i1"), creationCalls.get("e1"));
        assertEquals("expected one call to add e2i1 for entity e2", quickSet("e2i1"), creationCalls.get("e2"));

        // assert failure message recieved
        assertTrue(messages.contains("NO CAN DO"));
        assertTrue(messages.contains("Could not create missing indices for entity \"e2\""));
        assertTrue(messages.size() == 2);

    }

    private static <T> Set<T> quickSet(T... members) {
        HashSet<T> s = new HashSet<T>();
        s.addAll(Arrays.asList(members));
        return s;
    }

    @Test
    public void testError() {
        DatabaseUtil du = new DatabaseUtil(null, null, null, null);
        du.error("mesg", null);

        ArrayList<String> expected = new ArrayList<String>();
        expected.add("mesg");
        ArrayList<String> in = new ArrayList<String>();
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
        ArrayList<String> in = new ArrayList<String>();
        du.important("mesg", in);
        assertEquals(expected, in);
        du.important("mesg2", in);
        expected.add("mesg2");
        assertEquals(expected, in);
    }

    @Test
    public void testWarn() {
        DatabaseUtil du = new DatabaseUtil(null, null, null, null);
        du.warn("mesg", null);

        ArrayList<String> expected = new ArrayList<String>();
        expected.add("mesg");
        ArrayList<String> in = new ArrayList<String>();
        du.warn("mesg", in);
        assertEquals(expected, in);
        du.warn("mesg2", in);
        expected.add("mesg2");
        assertEquals(expected, in);
    }

    @Test
    public void testVerbose() {
        DatabaseUtil du = new DatabaseUtil(null, null, null, null);
        du.verbose("mesg", null);

        ArrayList<String> expected = new ArrayList<String>();
        expected.add("mesg");
        ArrayList<String> in = new ArrayList<String>();
        du.verbose("mesg", in);
        assertEquals(expected, in);
        du.verbose("mesg2", in);
        expected.add("mesg2");
        assertEquals(expected, in);
    }


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
        cci.isNullable = Boolean.TRUE;
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
