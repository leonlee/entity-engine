package org.ofbiz.core.entity.jdbc;

import org.hamcrest.Matchers;
import org.hamcrest.collection.IsIterableWithSize;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.ofbiz.core.entity.ConnectionProvider;
import org.ofbiz.core.entity.config.DatasourceInfo;
import org.ofbiz.core.entity.jdbc.dbtype.DatabaseType;
import org.ofbiz.core.entity.jdbc.dbtype.DatabaseTypeFactory;
import org.ofbiz.core.entity.model.ModelEntity;
import org.ofbiz.core.entity.model.ModelField;
import org.ofbiz.core.entity.model.ModelFieldType;
import org.ofbiz.core.entity.model.ModelFieldTypeReader;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@RunWith (MockitoJUnitRunner.class)
public class TestDatabaseUtilFieldModifications
{
    public static final String PROMOTED_TYPE_MATCH = "has different type definition and has been changed from";
    private static String MOCK_TABLE_NAME = "BOOKS";
    private static String MOCK_COLUMN_NAME = "AUTHOR";

    @Mock
    private ModelFieldTypeReader modelFieldTypeReader;
    @Mock
    private ModelFieldType modelFieldType;
    @Mock
    private DatasourceInfo datasourceInfo;
    @Mock
    private ConnectionProvider connectionProvider;
    @Mock
    private ModelEntity modelEntity;
    @Mock
    private ModelField modelField;
    @Mock
    private Connection connection;
    @Mock
    private Statement statement;

    private Collection<String> messages;

    private DatabaseUtil.ColumnCheckInfo columnInfo;

    // tested class.
    private DatabaseUtil databaseUtil;


    @Before
    public void setUp() throws Exception
    {
        databaseUtil = new DatabaseUtil("field-modification-tests", modelFieldTypeReader, datasourceInfo, connectionProvider);

        when(modelEntity.getTableName(any(DatasourceInfo.class))).thenReturn(MOCK_TABLE_NAME);
        when(connectionProvider.getConnection(any(String.class))).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);
        when(modelFieldTypeReader.getModelFieldType(any(String.class))).thenReturn(modelFieldType);
        when(modelField.getColName()).thenReturn(MOCK_COLUMN_NAME);
        columnInfo = new DatabaseUtil.ColumnCheckInfo();
        columnInfo.columnName = MOCK_COLUMN_NAME;
        messages = new ArrayList<String>();
    }

    @Test
    public void testWideningFieldsInOracle() throws Exception
    {
        mockDataBase(DatabaseTypeFactory.ORACLE_10G);

        // mock existing SQL type:
        columnInfo.columnSize = 10;
        columnInfo.typeName = "VARCHAR";

        // mock desired type:
        when(modelFieldType.getSqlType()).thenReturn("VARCHAR(20)");

        databaseUtil.checkFieldType(modelEntity, modelField, columnInfo, messages, false, true);

        // update should be performed:
        verify(statement).executeUpdate(any(String.class));
        Assert.assertThat(messages, Matchers.<String>hasItem(containsString(PROMOTED_TYPE_MATCH)));
    }

    @Test
    public void testUsingWideningFlag() throws Exception
    {
        // mock existing SQL type:
        columnInfo.columnSize = 10;
        columnInfo.typeName = "VARCHAR";

        // mock desired type:
        when(modelFieldType.getSqlType()).thenReturn("VARCHAR(20)");

        databaseUtil.checkFieldType(modelEntity, modelField, columnInfo, messages, true, false);

        // update should not be performed:
        verify(statement, never()).executeUpdate(any(String.class));
        Assert.assertThat(messages, Matchers.<String>hasItem(containsString("but is defined to have a column size of")));
    }

    @Test
    public void testNotShorteningFields() throws Exception
    {
        // mock existing SQL type:
        columnInfo.columnSize = 20;
        columnInfo.typeName = "VARCHAR";

        // mock desired type:
        when(modelFieldType.getSqlType()).thenReturn("VARCHAR(10)");

        databaseUtil.checkFieldType(modelEntity, modelField, columnInfo, messages, true, true);

        // update should not be performed:
        verify(statement, never()).executeUpdate(any(String.class));
        Assert.assertThat(messages, Matchers.<String>hasItem(containsString("but is defined to have a column size of")));
    }

    @Test
    public void testPromotingType() throws Exception
    {
        mockDataBase(DatabaseTypeFactory.HSQL);

        // mock existing SQL type:
        columnInfo.columnSize = 20;
        columnInfo.typeName = "VARCHAR";

        // mock desired type:
        when(modelFieldType.getSqlType()).thenReturn("NVARCHAR(20)");

        databaseUtil.checkFieldType(modelEntity, modelField, columnInfo, messages, true, false);

        // update should be performed:
        verify(statement).executeUpdate(any(String.class));
        Assert.assertThat(messages, Matchers.<String>hasItem(containsString("has been promoted")));
    }

    @Test
    public void testPromotingOracleType() throws Exception
    {
        mockDataBase(DatabaseTypeFactory.ORACLE_10G);

        // mock existing SQL type:
        columnInfo.columnSize = 40;
        columnInfo.typeName = "VARCHAR2";

        // mock desired type:
        when(modelFieldType.getSqlType()).thenReturn("NVARCHAR2(40)");

        databaseUtil.checkFieldType(modelEntity, modelField, columnInfo, messages, true, false);

        // update should be performed:
        verify(statement).executeUpdate(any(String.class));
        Assert.assertThat(messages, Matchers.<String>hasItem(Matchers.containsString("has been promoted")));
    }

    @Test
    public void testPromotingOracleExtension() throws Exception
    {
        mockDataBase(DatabaseTypeFactory.ORACLE_10G);

        // mock existing SQL type:
        columnInfo.columnSize = 40;
        columnInfo.maxSizeInBytes = 40;
        columnInfo.typeName = "VARCHAR2";

        // mock desired type:
        when(modelFieldType.getSqlType()).thenReturn("VARCHAR2(40 CHAR)");

        databaseUtil.checkFieldType(modelEntity, modelField, columnInfo, messages, false, true);

        // update should be performed:
        verify(statement).executeUpdate(any(String.class));
        Assert.assertThat(messages, Matchers.<String>hasItem(Matchers.containsString(PROMOTED_TYPE_MATCH)));
    }

    @Test
    public void shouldNotPromoteExtensionWhenDbNotOracle() throws Exception
    {
        mockDataBase(DatabaseTypeFactory.MSSQL);

        // mock existing SQL type:
        columnInfo.columnSize = 40;
        columnInfo.maxSizeInBytes = 40;
        columnInfo.typeName = "VARCHAR2";

        // mock desired type:
        when(modelFieldType.getSqlType()).thenReturn("VARCHAR2(40 CHAR)");

        databaseUtil.checkFieldType(modelEntity, modelField, columnInfo, messages, false, true);

        // update should be performed:
        verify(statement, never()).executeUpdate(any(String.class));
        Assert.assertThat(messages, IsIterableWithSize.<String>iterableWithSize(0));
    }

    private void mockDataBase(DatabaseType databaseType)
    {
        when(datasourceInfo.getDatabaseTypeFromJDBCConnection(connection)).thenReturn(databaseType);
        when(datasourceInfo.getDatabaseTypeFromJDBCConnection()).thenReturn(databaseType);
    }

    @Test
    public void testDetectOracleUnicodeFields() throws Exception
    {
        mockDataBase(DatabaseTypeFactory.ORACLE_10G);

        // mock existing SQL type:
        columnInfo.columnSize = 40;
        columnInfo.maxSizeInBytes = 160;
        columnInfo.typeName = "VARCHAR2";

        // mock desired type:
        when(modelFieldType.getSqlType()).thenReturn("VARCHAR2(40 CHAR)");

        databaseUtil.checkFieldType(modelEntity, modelField, columnInfo, messages, false, true);

        // update should not be performed:
        verify(statement, never()).executeUpdate(any(String.class));
        // no information on widening should be raised (field should be considered correct in DB):
        Assert.assertThat(messages, Matchers.not(Matchers.<String>hasItem(Matchers.containsString("but is defined to have a column size of"))));
        Assert.assertThat(messages, Matchers.not(Matchers.<String>hasItem(Matchers.containsString("has been widened"))));
        Assert.assertThat(messages, Matchers.not(Matchers.<String>hasItem(Matchers.containsString("Could not widen column"))));
    }


    @Test
    public void testNotPromotingTypeInReverse() throws Exception
    {
        // mock existing SQL type:
        columnInfo.columnSize = 20;
        columnInfo.typeName = "NVARCHAR";

        // mock desired type:
        when(modelFieldType.getSqlType()).thenReturn("VARCHAR(20)");

        databaseUtil.checkFieldType(modelEntity, modelField, columnInfo, messages, true, true);

        // update should not be performed:
        verify(statement, never()).executeUpdate(any(String.class));
        Assert.assertThat(messages, Matchers.<String>hasItem(containsString("but is defined as type")));
    }


    @Test
    public void testUsingPromotingFlag() throws Exception
    {
        // mock existing SQL type:
        columnInfo.columnSize = 20;
        columnInfo.typeName = "VARCHAR";

        // mock desired type:
        when(modelFieldType.getSqlType()).thenReturn("NVARCHAR(20)");

        databaseUtil.checkFieldType(modelEntity, modelField, columnInfo, messages, false, true);

        // update should not be performed:
        verify(statement, never()).executeUpdate(any(String.class));
        Assert.assertThat(messages, Matchers.<String>hasItem(containsString("but is defined as type")));
    }

    /**
     * It might be conceivable to ie. widen a field for the customer, but then promote it to something completely
     * different. Like: go from VARCHAR(20) to VARCHAR(40), only to find that in fact it should be NVARCHAR(20). Thus
     * promotion SHOULD make it possible to narrow the size.
     */
    @Test
    public void testPromotingTypeRegardlessOfShortening() throws Exception
    {
        mockDataBase(DatabaseTypeFactory.ORACLE_10G);

        // mock existing SQL type:
        columnInfo.columnSize = 20;
        columnInfo.typeName = "VARCHAR2";

        // mock desired type:
        when(modelFieldType.getSqlType()).thenReturn("NVARCHAR2(15)");

        databaseUtil.checkFieldType(modelEntity, modelField, columnInfo, messages, true, false);

        // update should be performed:
        verify(statement).executeUpdate(any(String.class));
        Assert.assertThat(messages, Matchers.<String>hasItem(containsString("has been promoted")));
    }

    @Test
    public void testSqlStatementCompositionOracle() throws Exception
    {
        testSqlStatementCompositionForDBWithModify(DatabaseTypeFactory.ORACLE_10G);
    }

    @Test
    public void testSqlStatementCompositionHsqldb() throws Exception
    {

        testSqlStatementCompositionForDBWithModify(DatabaseTypeFactory.HSQL);
    }

    private void testSqlStatementCompositionForDBWithModify(DatabaseType databaseType) throws SQLException
    {
        mockDataBase(databaseType);

        // mock existing SQL type:
        columnInfo.columnSize = 45;
        columnInfo.typeName = "VARCHAR";

        // mock desired type:
        when(modelFieldType.getSqlType()).thenReturn("NVARCHAR(123)");

        databaseUtil.checkFieldType(modelEntity, modelField, columnInfo, messages, true, true);

        // update should be performed:
        verify(statement).executeUpdate("ALTER TABLE BOOKS MODIFY AUTHOR NVARCHAR(123)");
    }

    @Test
    public void testSqlStatementCompositionMssql() throws Exception
    {
        testSqlStatementCompositionWithAlterColumnForDB(DatabaseTypeFactory.MSSQL);
    }

    @Test
    public void testSqlStatementCompositionMysql() throws Exception
    {
        testSqlStatementCompositionForDBWithModify(DatabaseTypeFactory.MYSQL);
    }

    private void testSqlStatementCompositionWithAlterColumnForDB(DatabaseType databaseType) throws SQLException
    {
        mockDataBase(databaseType);
        // mock existing SQL type:
        columnInfo.columnSize = 45;
        columnInfo.typeName = "VARCHAR";

        // mock desired type:
        when(modelFieldType.getSqlType()).thenReturn("VARCHAR(222)");

        databaseUtil.checkFieldType(modelEntity, modelField, columnInfo, messages, true, true);

        // update should be performed:
        verify(statement).executeUpdate("ALTER TABLE BOOKS ALTER COLUMN AUTHOR VARCHAR(222)");
    }

    @Test
    public void testSqlStatementCompositionPostgresql() throws Exception
    {
        mockDataBase(DatabaseTypeFactory.POSTGRES_7_3);

        // mock existing SQL type:
        columnInfo.columnSize = 234;
        columnInfo.typeName = "VARCHAR";

        // mock desired type:
        when(modelFieldType.getSqlType()).thenReturn("NVARCHAR(456)");

        databaseUtil.checkFieldType(modelEntity, modelField, columnInfo, messages, true, true);

        // update should be performed:
        verify(statement).executeUpdate("ALTER TABLE BOOKS ALTER COLUMN AUTHOR TYPE NVARCHAR(456)");
    }

    @Test
    public void testMessagesWhenWideningIsDisabled() throws Exception
    {
        // pick an outdated DB type:
        mockDataBase(DatabaseTypeFactory.POSTGRES_7_2);

        // mock existing SQL type:
        columnInfo.columnSize = 234;
        columnInfo.typeName = "VARCHAR";

        // mock desired type:
        when(modelFieldType.getSqlType()).thenReturn("NVARCHAR(456)");

        databaseUtil.checkFieldType(modelEntity, modelField, columnInfo, messages, true, false);

        // update should not be performed:
        verify(statement, never()).executeUpdate(any(String.class));
        Assert.assertThat(messages, Matchers.<String>hasItem(containsString("Changing of column type is not supported")));

        // check also widening:
        columnInfo.typeName = "NVARCHAR";
        messages.clear();

        databaseUtil.checkFieldType(modelEntity, modelField, columnInfo, messages, false, true);

        // update should not be performed:
        verify(statement, never()).executeUpdate(any(String.class));
        Assert.assertThat(messages, Matchers.<String>hasItem(containsString("Changing of column type is not supported")));

    }

}
