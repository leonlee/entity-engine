package org.ofbiz.core.entity.jdbc;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.junit.Test;
import org.mockito.InOrder;
import org.ofbiz.core.entity.ConnectionProvider;
import org.ofbiz.core.entity.GenericEntityException;
import org.ofbiz.core.entity.config.DatasourceInfo;
import org.ofbiz.core.entity.jdbc.alternative.ShouldNotRunIndexAlternativeAction;
import org.ofbiz.core.entity.jdbc.alternative.SuspendingAlternativeAction;
import org.ofbiz.core.entity.jdbc.dbtype.DatabaseType;
import org.ofbiz.core.entity.jdbc.dbtype.DatabaseTypeFactory;
import org.ofbiz.core.entity.jdbc.dbtype.MySqlDatabaseType;
import org.ofbiz.core.entity.jdbc.sql.escape.SqlEscapeHelper;
import org.ofbiz.core.entity.model.FunctionDefinitionBuilder;
import org.ofbiz.core.entity.model.ModelEntity;
import org.ofbiz.core.entity.model.ModelField;
import org.ofbiz.core.entity.model.ModelFieldType;
import org.ofbiz.core.entity.model.ModelFieldTypeReader;
import org.ofbiz.core.entity.model.ModelFunctionBasedIndex;
import org.ofbiz.core.entity.model.ModelIndex;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Collections.emptyList;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link org.ofbiz.core.entity.jdbc.DatabaseUtil}.
 * <p>
 * Baby steps...
 */
public class TestDatabaseUtil {
    @Test
    public void testCreateDeclaredIndicesNone() {
        DatabaseUtil du = new DatabaseUtil("Santa's Little Helper", null, null, null, null);
        ModelEntity modelEntity = new ModelEntity();
        final String mesg = du.createDeclaredIndices(modelEntity);
        assertNull("unexpected error", mesg);
    }

    @Test
    public void testCreateDeclaredIndicesBasic() throws Exception {
        final Connection connection = mock(Connection.class);
        final Statement statement = mock(Statement.class);
        when(connection.createStatement()).thenReturn(statement);
        final SqlEscapeHelper sqlEscapeHelper = mock(SqlEscapeHelper.class);
        when(sqlEscapeHelper.escapeColumn(anyString())).thenAnswer(i -> i.getArgument(0));
        DatabaseUtil du = new DatabaseUtil("Santa's Helper", null, null, new MyConnectionProvider(connection), sqlEscapeHelper);
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

    @Test
    public void testCreateFunctionBasedIndicesSupportedDB() throws Exception {
        final Connection connection = mock(Connection.class);
        final Statement statement = mock(Statement.class);
        when(connection.createStatement()).thenReturn(statement);
        final DatasourceInfo datasourceInfo = mock(DatasourceInfo.class);
        when(datasourceInfo.getDatabaseTypeFromJDBCConnection(connection)).thenReturn(DatabaseTypeFactory.POSTGRES);
        DatabaseUtil du = new DatabaseUtil("Santa's Helper", null, datasourceInfo, new MyConnectionProvider(connection), null);
        ModelEntity modelEntity = new ModelEntity("testable", Collections.<DatabaseUtil.ColumnCheckInfo>emptyList(), null);
        final ModelField modelField = new ModelField();
        modelField.setColName("nicecolumn");
        modelField.setName("fieldname");
        modelEntity.addField(modelField);
        final FunctionDefinitionBuilder builder = createFunctionDefinitionBuilder(true);
        final ModelFunctionBasedIndex modelFBIndex = new ModelFunctionBasedIndex(modelEntity, "fbindex", true, builder);
        modelEntity.addFunctionBasedIndex(modelFBIndex);
        final String mesg = du.createFunctionBasedIndices(modelEntity);
        assertNull("unexpected error", mesg);
        verify(statement).executeUpdate("CREATE UNIQUE INDEX fbindex ON TESTABLE (lower(fieldname))");
    }

    private FunctionDefinitionBuilder createFunctionDefinitionBuilder(boolean supportsFunctionIndexes) {
        final FunctionDefinitionBuilder builder = mock(FunctionDefinitionBuilder.class);
        when(builder.getFunctionDefinition(any(DatabaseType.class))).thenReturn("lower(fieldname)");
        when(builder.supportsFunctionBasedIndices(any(DatabaseType.class))).thenReturn(supportsFunctionIndexes);
        return builder;
    }

    @Test
    public void testCreateFunctionBasedIndicesUnsupportedDB() throws Exception {
        final Connection connection = mock(Connection.class);
        final Statement statement = mock(Statement.class);
        when(connection.createStatement()).thenReturn(statement);
        final DatasourceInfo datasourceInfo = mock(DatasourceInfo.class);
        final SqlEscapeHelper sqlEscapeHelper = mock(SqlEscapeHelper.class);
        when(datasourceInfo.getDatabaseTypeFromJDBCConnection(connection)).thenReturn(DatabaseTypeFactory.MSSQL);
        final ModelFieldType modelFieldType = mock(ModelFieldType.class);
        when(modelFieldType.getSqlType()).thenReturn("VARCHAR");
        final ModelFieldTypeReader modelFieldTypeReader = mock(ModelFieldTypeReader.class);
        when(modelFieldTypeReader.getModelFieldType("long-varchar")).thenReturn(modelFieldType);
        DatabaseUtil du = new DatabaseUtil("Santa's Helper", modelFieldTypeReader, datasourceInfo, new MyConnectionProvider(connection), sqlEscapeHelper);
        ModelEntity modelEntity = new ModelEntity("testable", Collections.<DatabaseUtil.ColumnCheckInfo>emptyList(), null);
        final ModelField modelField = new ModelField();
        modelField.setColName("nicecolumn");
        modelField.setName("fieldname");
        modelEntity.addField(modelField);
        final FunctionDefinitionBuilder builder = createFunctionDefinitionBuilder(false);
        when(builder.getVirtualColumn(any(DatabaseType.class))).thenReturn("lower_vname");
        when(sqlEscapeHelper.escapeColumn(anyString())).thenAnswer(i -> i.getArguments()[0]);
        when(builder.getType()).thenReturn("long-varchar");
        final ModelFunctionBasedIndex modelFBIndex = new ModelFunctionBasedIndex(modelEntity, "fbindex", false, builder);
        modelEntity.addFunctionBasedIndex(modelFBIndex);
        final String mesg = du.createFunctionBasedIndices(modelEntity);
        assertNull("unexpected error", mesg);
        verify(statement).executeUpdate("ALTER TABLE TESTABLE ADD lower_vname VARCHAR AS (lower(fieldname))");
        verify(statement).executeUpdate("CREATE INDEX fbindex ON TESTABLE (lower_vname)");
    }

    @Test
    public void testGetIndexInfoUsesUpperCaseForOracleHsqlAndH2() throws Exception {
        final List<DatabaseType> databaseTypes = ImmutableList.of(
                DatabaseTypeFactory.ORACLE_8I,
                DatabaseTypeFactory.ORACLE_10G,
                DatabaseTypeFactory.HSQL,
                DatabaseTypeFactory.HSQL_2_3_3,
                DatabaseTypeFactory.H2);
        final DatabaseUtil du = new DatabaseUtil("Santa's Helper", null, null, null, null);
        for (DatabaseType databaseType : databaseTypes) {
            final DatabaseMetaData metaData = mock(DatabaseMetaData.class);
            final ResultSet rs = mock(ResultSet.class);
            when(metaData.getIndexInfo(null, "schema", "NAME", false, true)).thenReturn(rs);

            assertThat(du.getIndexInfo(metaData, databaseType, "schema", "Name", null), is(rs));

            verifyZeroInteractions(rs);
        }
    }

    @Test
    public void testGetIndexInfoUsesOriginalCaseForMysqlAndMssql() throws Exception {
        final List<DatabaseType> databaseTypes = ImmutableList.of(
                DatabaseTypeFactory.MYSQL,
                DatabaseTypeFactory.MSSQL);
        final DatabaseUtil du = new DatabaseUtil("Santa's Helper", null, null, null, null);
        for (DatabaseType databaseType : databaseTypes) {
            final DatabaseMetaData metaData = mock(DatabaseMetaData.class);
            final ResultSet rs = mock(ResultSet.class);
            when(metaData.getIndexInfo(null, "schema", "Name", false, true)).thenReturn(rs);

            assertThat(du.getIndexInfo(metaData, databaseType, "schema", "Name", null), is(rs));

            verifyZeroInteractions(rs);
        }
    }

    @Test
    public void testGetIndexInfoUsesOriginalCaseForPostgresAsLongAsItHasResults() throws Exception {
        final List<DatabaseType> databaseTypes = ImmutableList.of(
                DatabaseTypeFactory.POSTGRES,
                DatabaseTypeFactory.POSTGRES_7_2,
                DatabaseTypeFactory.POSTGRES_7_3);
        final DatabaseUtil du = new DatabaseUtil("Santa's Helper", null, null, null, null);
        for (DatabaseType databaseType : databaseTypes) {
            final DatabaseMetaData metaData = mock(DatabaseMetaData.class);
            final ResultSet rs = mock(ResultSet.class);
            when(metaData.getIndexInfo(null, "schema", "Name", false, true)).thenReturn(rs);
            when(rs.next()).thenReturn(true);

            assertThat(du.getIndexInfo(metaData, databaseType, "schema", "Name", null), is(rs));

            final InOrder inOrder = inOrder(rs);
            inOrder.verify(rs).next();
            inOrder.verify(rs).beforeFirst();
            inOrder.verifyNoMoreInteractions();
        }
    }

    @Test
    public void testGetIndexInfoUsesLowerCaseForPostgresIfTheOriginalCaseFails() throws Exception {
        final List<DatabaseType> databaseTypes = ImmutableList.of(
                DatabaseTypeFactory.POSTGRES,
                DatabaseTypeFactory.POSTGRES_7_2,
                DatabaseTypeFactory.POSTGRES_7_3);
        final DatabaseUtil du = new DatabaseUtil("Santa's Helper", null, null, null, null);
        for (DatabaseType databaseType : databaseTypes) {
            final DatabaseMetaData metaData = mock(DatabaseMetaData.class);
            final ResultSet rs1 = mock(ResultSet.class);
            final ResultSet rs2 = mock(ResultSet.class);
            when(metaData.getIndexInfo(null, "schema", "Name", false, true)).thenReturn(rs1);
            when(metaData.getIndexInfo(null, "schema", "name", false, true)).thenReturn(rs2);

            assertThat(du.getIndexInfo(metaData, databaseType, "schema", "Name", null), is(rs2));

            final InOrder inOrder = inOrder(rs1, rs2);
            inOrder.verify(rs1).next();
            inOrder.verify(rs1).close();
            inOrder.verifyNoMoreInteractions();
        }
    }

    /**
     * Mild test for this monster method.
     *
     * @throws Exception
     */
    @Test
    public void testCheckDb() throws Exception {
        final TreeSet<String> tables = new TreeSet<>();
        tables.add("BOOKS");
        tables.add("AUTHORS");

        final Map<String, List<DatabaseUtil.ColumnCheckInfo>> columnInfo = new HashMap<>();
        DatabaseUtil.ColumnCheckInfo a1 = createCcInfo("AUTHORS", "NAME");
        DatabaseUtil.ColumnCheckInfo a2 = createCcInfo("AUTHORS", "AGE");

        DatabaseUtil.ColumnCheckInfo b1 = createCcInfo("BOOKS", "TITLE");
        DatabaseUtil.ColumnCheckInfo b2 = createCcInfo("BOOKS", "AUTHOR");

        columnInfo.put("AUTHORS", new ArrayList<>(Arrays.asList(a1, a2)));
        columnInfo.put("BOOKS", new ArrayList<>(Arrays.asList(b1, b2)));

        final Connection connection = mock(Connection.class);
        final DatabaseMetaData dbData = mock(DatabaseMetaData.class);
        when(dbData.supportsSchemasInTableDefinitions()).thenReturn(false);
        when(connection.getMetaData()).thenReturn(dbData);

        final Statement statement = mock(Statement.class);
        when(connection.createStatement()).thenReturn(statement);

        DatasourceInfo dsi = mock(DatasourceInfo.class);
        final SqlEscapeHelper sqlEscapeHelper = mock(SqlEscapeHelper.class);

        when(dsi.isUseFks()).thenReturn(false);
        when(dsi.isUseFkIndices()).thenReturn(false);
        when(dsi.isUseIndices()).thenReturn(true);

        DatabaseUtil du = new DatabaseUtil("Santa's Helper", null, dsi, new MyConnectionProvider(connection), sqlEscapeHelper) {
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
                                boolean promote, boolean widen) {
                // do nothing... we're not testing field types here
            }
        };

        Map<String, ModelEntity> entities = new HashMap<>();
        entities.put("AUTHORS", createSimpleModelEntity("AUTHORS", "NAME", "AGE"));
        // deliberately missing title to produce one error message
        entities.put("BOOKS", createSimpleModelEntity("BOOKS", "AUTHOR"));
        List<String> messages = new ArrayList<>();
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
        final DatasourceInfo datasourceInfo = mock(DatasourceInfo.class);
        when(connection.getMetaData()).thenReturn(dbData);

        MockResultSet t1IndexRs = new MockResultSet();
        // t1 has two indexes, t2 has one non unique index and t3 has no indexes
        t1IndexRs.addRow(createIndexMetadataRow("t1_index1", "t1", false));
        t1IndexRs.addRow(createIndexMetadataRow("t1_index2", "t1", false));
        MockResultSet t2IndexRs = new MockResultSet();
        t2IndexRs.addRow(createIndexMetadataRow("t2_index", "t2", false));
        t2IndexRs.addRow(createIndexMetadataRow("t2_index_unique", "t2", true));
        MockResultSet t3IndexRs = new MockResultSet();

        when(dbData.getIndexInfo(any(), anyString(), eq("t1"), anyBoolean(), anyBoolean())).thenReturn(t1IndexRs);
        when(dbData.getIndexInfo(any(), anyString(), eq("t2"), anyBoolean(), anyBoolean())).thenReturn(t2IndexRs);
        when(dbData.getIndexInfo(any(), anyString(), eq("t3"), anyBoolean(), anyBoolean())).thenReturn(t3IndexRs);
        when(dbData.supportsSchemasInTableDefinitions()).thenReturn(true);
        when(datasourceInfo.getSchemaName()).thenReturn("jira");

        DatabaseUtil du = new DatabaseUtil("Santa's Helper", null, datasourceInfo, new MyConnectionProvider(connection), null);

        List<String> messages = new ArrayList<>();
        Set<String> tableNames = ImmutableSet.of("t1", "t2", "t3");

        // the call to the production method
        final Map<String, Set<String>> indexInfo = du.getIndexInfo(tableNames, messages);

        // the assertions...
        verify(dbData, times(3)).getIndexInfo(eq(null), anyString(), anyString(), eq(false), anyBoolean());
        assertThat(indexInfo.keySet(), containsInAnyOrder("t1", "t2", "t3"));
        assertThat(indexInfo.get("t1"), contains("T1_INDEX1", "T1_INDEX2"));
        assertThat(indexInfo.get("t2"), contains("T2_INDEX"));
        assertThat(indexInfo.get("t3"), hasSize(0));
        assertTrue("unexpected error messages", messages.isEmpty());
        final Set<String> t1Indexes = new TreeSet<>();
        t1Indexes.addAll(Arrays.asList("T1_INDEX1", "T1_INDEX2"));
        assertEquals(t1Indexes, indexInfo.get("t1"));
        final Set<String> t2Indexes = new TreeSet<>();
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
        final DatasourceInfo datasourceInfo = mock(DatasourceInfo.class);
        when(connection.getMetaData()).thenReturn(dbData);

        MockResultSet t1IndexRs = new MockResultSet();
        // t1 has two indexes, t2 has one non unique index and t3 has no indexes
        t1IndexRs.addRow(createIndexMetadataRow("t1_index1", "t1", false));
        t1IndexRs.addRow(createIndexMetadataRow("t1_index2", "t1", false));
        MockResultSet t2IndexRs = new MockResultSet();
        t2IndexRs.addRow(createIndexMetadataRow("t2_index", "t2", false));
        t2IndexRs.addRow(createIndexMetadataRow("t2_index_unique", "t2", true));
        MockResultSet t3IndexRs = new MockResultSet();

        when(dbData.getIndexInfo(any(), anyString(), eq("t1"), anyBoolean(), anyBoolean())).thenReturn(t1IndexRs);
        when(dbData.getIndexInfo(any(), anyString(), eq("t2"), anyBoolean(), anyBoolean())).thenReturn(t2IndexRs);
        when(dbData.getIndexInfo(any(), anyString(), eq("t3"), anyBoolean(), anyBoolean())).thenReturn(t3IndexRs);
        when(dbData.supportsSchemasInTableDefinitions()).thenReturn(true);
        when(datasourceInfo.getSchemaName()).thenReturn("jira");

        DatabaseUtil du = new DatabaseUtil("Santa's Helper", null, datasourceInfo, new MyConnectionProvider(connection), null);

        List<String> messages = new ArrayList<>();
        Set<String> tableNames = ImmutableSet.of("t1", "t2", "t3");

        // the call to the production method
        final Map<String, Set<String>> indexInfo = du.getIndexInfo(tableNames, messages, true);

        // the assertions...
        verify(dbData, times(3)).getIndexInfo(eq(null), eq("jira"), anyString(), eq(false), anyBoolean());
        assertThat(indexInfo.entrySet(), hasSize(3));
        assertThat(indexInfo.get("t1"), hasSize(2));
        assertThat(indexInfo.get("t2"), hasSize(2));
        assertThat(indexInfo.get("t3"), hasSize(0));
        assertThat("unexpected error messages", messages, hasSize(0));
        final Set<String> t1Indexes = new TreeSet<String>();
        t1Indexes.addAll(Arrays.asList("T1_INDEX1", "T1_INDEX2"));
        assertEquals(t1Indexes, indexInfo.get("t1"));
        final Set<String> t2Indexes = new TreeSet<String>();
        t2Indexes.addAll(Arrays.asList("T2_INDEX", "T2_INDEX_UNIQUE"));
        assertEquals(t2Indexes, indexInfo.get("t2"));
    }

    /**
     * Test JRA-28526 ensure that tables are fetched from data base with the same schema
     * as they are created. Problem was occurring when no schema was provided for mssql or postgres database.
     */
    @Test
    public void testGetTables() throws Exception {
        //with
        final Connection connection = mock(Connection.class);
        final DatabaseMetaData dbData = mock(DatabaseMetaData.class);
        when(connection.getMetaData()).thenReturn(dbData);
        when(dbData.supportsSchemasInTableDefinitions()).thenReturn(Boolean.TRUE);
        when(dbData.getUserName()).thenReturn("user");
        final ResultSet rightResultSet = mock(ResultSet.class);
        final String[] types = {"BASE TABLE", "TABLE", "VIEW", "ALIAS", "SYNONYM"};

        when(dbData.getTables(null, null, null, types)).thenReturn(rightResultSet);

        //use this as to be sure that no schema user is used
        final ResultSet emptyResult = mock(ResultSet.class);
        when(dbData.getTables(null, "user", null, types)).thenReturn(emptyResult);
        final DatasourceInfo dataSourceInfo = mock(DatasourceInfo.class);
        final SqlEscapeHelper sqlEscapeHelper = mock(SqlEscapeHelper.class);
        final DatabaseUtil du = new DatabaseUtil("Santa's Helper", null, dataSourceInfo, new MyConnectionProvider(connection), sqlEscapeHelper);

        when(rightResultSet.next()).thenReturn(Boolean.TRUE, Boolean.TRUE, Boolean.FALSE);
        when(rightResultSet.getString("TABLE_NAME")).thenReturn("table1", "table2");
        when(rightResultSet.getString("TABLE_TYPE")).thenReturn("TABLE");
        final Collection<String> messages = new ArrayList<>();

        //invoke
        final Set<String> tableNames = du.getTableNames(messages);

        //then
        assertEquals(ImmutableSet.of("TABLE1", "TABLE2"), tableNames);

    }

    /**
     * Test JRA-28526 ensure that tables are fetched from data base with the same schema
     * as they are created. For the Oracle db it is the user schema name
     */
    @Test
    public void testGetTablesForOracle() throws Exception {
        //with
        final Connection connection = mock(Connection.class);
        final DatabaseMetaData dbData = mock(DatabaseMetaData.class);
        when(connection.getMetaData()).thenReturn(dbData);
        when(dbData.supportsSchemasInTableDefinitions()).thenReturn(Boolean.TRUE);
        when(dbData.getUserName()).thenReturn("user");
        final ResultSet rightResultSet = mock(ResultSet.class);
        final String[] types = {"BASE TABLE", "TABLE", "VIEW", "ALIAS", "SYNONYM"};

        final ResultSet emptyResult = mock(ResultSet.class);
        //use this as to be sure that user as schema is used
        when(dbData.getTables(null, null, null, types)).thenReturn(emptyResult);

        when(dbData.getTables(null, "user", null, types)).thenReturn(rightResultSet);
        final DatasourceInfo dataSourceInfo = mock(DatasourceInfo.class);
        when(dbData.getDatabaseProductName()).thenReturn("Oracle");
        final SqlEscapeHelper sqlEscapeHelper = mock(SqlEscapeHelper.class);

        final DatabaseUtil du = new DatabaseUtil("Santa's Helper", null, dataSourceInfo, new MyConnectionProvider(connection), sqlEscapeHelper);
        when(rightResultSet.next()).thenReturn(Boolean.TRUE, Boolean.TRUE, Boolean.FALSE);
        when(rightResultSet.getString("TABLE_NAME")).thenReturn("table1", "table2");
        when(rightResultSet.getString("TABLE_TYPE")).thenReturn("TABLE");
        final Collection<String> messages = new ArrayList<>();

        //invoke
        final Set<String> tableNames = du.getTableNames(messages);

        //then
        assertEquals(ImmutableSet.of("TABLE1", "TABLE2"), tableNames);

    }

    @Test
    public void testMissingIndices() {

        // record requests for index info
        final AtomicReference<Set<String>> tableNamesReceived = new AtomicReference<>();
        final AtomicBoolean includeUniqueReceived = new AtomicBoolean();

        final Map<String, Set<String>> indexInfo = new HashMap<>();
        indexInfo.put("e1", Collections.<String>emptySet());
        indexInfo.put("e2", quickSet("E2I2"));
        indexInfo.put("e3", Collections.<String>emptySet());

        // record requests to create indexes
        final Map<String, Set<String>> creationCalls = new HashMap<>();

        DatabaseUtil du = new DatabaseUtil(null, null, null, mock(ConnectionProvider.class), null) {
            @Override
            public Map<String, Set<String>> getIndexInfo(final Set<String> tableNames, final Collection<String> messages, final boolean includeUnique) {
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
        List<String> messages = new ArrayList<>();
        Map<String, ModelEntity> modelEntities = new HashMap<>();

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
        assertThat(tableNamesReceived.get(), containsInAnyOrder("e1", "e2", "e3"));
        assertTrue(includeUniqueReceived.get());

        // assert calls to create missing indexes were correct
        assertEquals("expected one call to add e1i1 for entity e1", quickSet("e1i1"), creationCalls.get("e1"));
        assertEquals("expected one call to add e2i1 for entity e2", quickSet("e2i1"), creationCalls.get("e2"));

        // assert failure message recieved
        assertThat(messages, contains("Could not create missing indices for entity \"e2\"", "NO CAN DO"));
    }

    private static <T> Set<T> quickSet(T... members) {
        Set<T> s = new HashSet<>();
        s.addAll(Arrays.asList(members));
        return s;
    }

    @Test
    public void testError() {
        DatabaseUtil du = new DatabaseUtil(null, null, null, null, null);
        du.error("mesg", null);

        List<String> expected = new ArrayList<>();
        expected.add("mesg");
        Collection<String> in = new ArrayList<>();
        du.error("mesg", in);
        assertEquals(expected, in);
        du.error("mesg2", in);
        expected.add("mesg2");
        assertEquals(expected, in);
    }

    @Test
    public void testImportant() {
        DatabaseUtil du = new DatabaseUtil(null, null, null, null, null);
        du.important("mesg", null);

        List<String> expected = new ArrayList<>();
        expected.add("mesg");
        Collection<String> in = new ArrayList<>();
        du.important("mesg", in);
        assertEquals(expected, in);
        du.important("mesg2", in);
        expected.add("mesg2");
        assertEquals(expected, in);
    }

    @Test
    public void testWarn() {
        DatabaseUtil du = new DatabaseUtil(null, null, null, null, null);
        du.warn("mesg", null);

        List<String> expected = new ArrayList<>();
        expected.add("mesg");
        Collection<String> in = new ArrayList<>();
        du.warn("mesg", in);
        assertEquals(expected, in);
        du.warn("mesg2", in);
        expected.add("mesg2");
        assertEquals(expected, in);
    }

    @Test
    public void testVerbose() {
        DatabaseUtil du = new DatabaseUtil(null, null, null, null, null);
        du.verbose("mesg", null);

        List<String> expected = new ArrayList<>();
        expected.add("mesg");
        Collection<String> in = new ArrayList<>();
        du.verbose("mesg", in);
        assertEquals(expected, in);
        du.verbose("mesg2", in);
        expected.add("mesg2");
        assertEquals(expected, in);
    }

    private final DatabaseUtil TEST_ALTERNATIVE_FUNCTION_DB_UTIL = new DatabaseUtil(null, null, null, null, null) {
        @Override
        public String makeIndexClause(ModelEntity entity, ModelIndex modelIndex) {
            return null;
        }

        @Override
        public Connection getConnection() throws SQLException, GenericEntityException {
            return mock(Connection.class, RETURNS_DEEP_STUBS);
        }

        protected String executeStatement(Connection connection, String sql) {
            return "executed";
        }
    };

    @Test
    public void testAlternateFunctionShouldStopExecution() {
        DatabaseUtil databaseUtil = TEST_ALTERNATIVE_FUNCTION_DB_UTIL;
        ModelIndex modelIndex = new ModelIndex();
        modelIndex.addAlternativeAction(new ShouldNotRunIndexAlternativeAction());
        modelIndex.addAlternativeAction(new SuspendingAlternativeAction());


        assertEquals(databaseUtil.createDeclaredIndex(null, modelIndex), SuspendingAlternativeAction.ACTION_EXECUTED_DUMMY_ERROR);
    }

    @Test
    public void testAlternateFunctionShouldContinueExecution() {
        DatabaseUtil databaseUtil = TEST_ALTERNATIVE_FUNCTION_DB_UTIL;
        ModelIndex modelIndex = new ModelIndex();
        modelIndex.addAlternativeAction(new ShouldNotRunIndexAlternativeAction());
        modelIndex.addAlternativeAction(new ShouldNotRunIndexAlternativeAction());

        //Null means no errors and normal execution.
        assertNull(databaseUtil.createDeclaredIndex(null, modelIndex));
    }

    @Test(expected = IllegalStateException.class)
    public void twoRunnableAlternativeActionsShouldThrowException() {
        DatabaseUtil databaseUtil = TEST_ALTERNATIVE_FUNCTION_DB_UTIL;
        ModelIndex modelIndex = new ModelIndex();
        modelIndex.addAlternativeAction(new SuspendingAlternativeAction());
        modelIndex.addAlternativeAction(new SuspendingAlternativeAction());

        databaseUtil.createDeclaredIndex(null, modelIndex);
    }

    @Test
    public void testShouldCreateTableWithEscapedColumnName() throws SQLException {

        Connection connection = mock(Connection.class, RETURNS_DEEP_STUBS);
        Statement statement = mock(Statement.class);

        when(connection.createStatement()).thenReturn(statement);
        DatabaseMetaData metaData = mock(DatabaseMetaData.class);
        when(metaData.getSQLKeywords()).thenReturn("LEAD,GRANT,OF,OTHER");
        when(connection.getMetaData()).thenReturn(metaData);

        DatasourceInfo datasourceInfo = mock(DatasourceInfo.class);
        DatabaseType databaseType = new MySqlDatabaseType();
        databaseType.initialize(connection);
        when(datasourceInfo.getDatabaseTypeFromJDBCConnection()).thenReturn(databaseType);
        SqlEscapeHelper sqlEscapeHelper = new SqlEscapeHelper(datasourceInfo);

        ModelFieldTypeReader modelFieldTypeReader = mock(ModelFieldTypeReader.class);
        ModelFieldType modelFieldType = mock(ModelFieldType.class);
        when(modelFieldType.getJavaType()).thenReturn("String");
        when(modelFieldType.getSqlType()).thenReturn("VARCHAR2(255)");
        when(modelFieldType.getType()).thenReturn("VARCHAR");
        when(modelFieldTypeReader.getModelFieldType(any())).thenReturn(modelFieldType);

        List<ModelField> modelFields = new ArrayList<>();
        ModelField modelFieldId = new ModelField("id", "long-varchar", "id", false, emptyList());
        modelFields.add(modelFieldId);
        modelFields.add(new ModelField("lead", "long-varchar", "lead", false, emptyList()));
        modelFields.add(new ModelField("other", "long-varchar", "other", false, emptyList()));
        modelFields.add(new ModelField("author", "long-varchar", "author", false, emptyList()));

        ModelEntity modelEntity = mock(ModelEntity.class);
        when(modelEntity.getPksCopy()).thenReturn(Collections.singletonList(modelFieldId));
        when(modelEntity.getTableName(eq(datasourceInfo))).thenReturn("PROJECT");
        when(modelEntity.getFieldsSize()).thenReturn(modelFields.size());
        when(modelEntity.getPlainTableName()).thenReturn("PROJECT");
        when(modelEntity.getField(anyInt())).thenAnswer(i -> modelFields.get(i.getArgument(0)));
        when(modelEntity.colNameString(anyList(), eq(sqlEscapeHelper))).thenCallRealMethod();
        when(modelEntity.colNameString(anyList(), eq(", "), eq(""), eq(sqlEscapeHelper))).thenCallRealMethod();

        DatabaseUtil databaseUtil = new DatabaseUtil(null, modelFieldTypeReader, datasourceInfo, null, sqlEscapeHelper) {
            @Override
            public Connection getConnection() {
                return connection;
            }
        };

        String result = databaseUtil.createTable(modelEntity, null, false, false, 1, "name_constraint", false);

        assertNull(result);
        verify(statement, times(1)).executeUpdate("CREATE TABLE PROJECT (id VARCHAR2(255), `lead` VARCHAR2(255), `other` VARCHAR2(255), author VARCHAR2(255),  PRIMARY KEY (id))");
        verify(statement, times(1)).close();
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
        Map<String, Object> row = new HashMap<>();
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
