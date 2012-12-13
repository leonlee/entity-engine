package org.ofbiz.core.entity.jdbc;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.ofbiz.core.entity.ConnectionProvider;
import org.ofbiz.core.entity.config.DatasourceInfo;
import org.ofbiz.core.entity.model.ModelEntity;
import org.ofbiz.core.entity.model.ModelField;
import org.ofbiz.core.entity.model.ModelFieldType;
import org.ofbiz.core.entity.model.ModelFieldTypeReader;

import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@RunWith (MockitoJUnitRunner.class)
public class TestDatabaseUtilFieldModifications
{
    private static String MOCK_TABLE_NAME = "MOCK_TABLE_NAME";
    private static String MOCK_COLUMN_NAME = "MOCK_COLUMN_NAME";

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
    public void testWideningFields() throws Exception
    {
        // mock existing SQL type:
        columnInfo.columnSize = 10;
        columnInfo.typeName = "VARCHAR";

        // mock desired type:
        when(modelFieldType.getSqlType()).thenReturn("VARCHAR(20)");

        databaseUtil.checkFieldType(modelEntity, modelField, columnInfo, messages, false, true);

        // update should be performed:
        verify(statement).executeUpdate(any(String.class));
        Assert.assertThat(messages, Matchers.contains(Matchers.containsString("has been widened")));
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
        Assert.assertThat(messages, Matchers.contains(Matchers.containsString("but is defined to have a column size of")));
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
        Assert.assertThat(messages, Matchers.contains(Matchers.containsString("but is defined to have a column size of")));
    }

    @Test
    public void testPromotingType() throws Exception
    {
        // mock existing SQL type:
        columnInfo.columnSize = 20;
        columnInfo.typeName = "VARCHAR";

        // mock desired type:
        when(modelFieldType.getSqlType()).thenReturn("NVARCHAR(20)");

        databaseUtil.checkFieldType(modelEntity, modelField, columnInfo, messages, true, false);

        // update should be performed:
        verify(statement).executeUpdate(any(String.class));
        Assert.assertThat(messages, Matchers.contains(Matchers.containsString("has been promoted")));
    }

    @Test
    public void testPromotingOracleType() throws Exception
    {
        // mock existing SQL type:
        columnInfo.columnSize = 40;
        columnInfo.typeName = "VARCHAR2";

        // mock desired type:
        when(modelFieldType.getSqlType()).thenReturn("NVARCHAR2(40)");

        databaseUtil.checkFieldType(modelEntity, modelField, columnInfo, messages, true, false);

        // update should be performed:
        verify(statement).executeUpdate(any(String.class));
        Assert.assertThat(messages, Matchers.contains(Matchers.containsString("has been promoted")));
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
        Assert.assertThat(messages, Matchers.contains(Matchers.containsString("but is defined as type")));
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
        Assert.assertThat(messages, Matchers.contains(Matchers.containsString("but is defined as type")));
    }

    /**
     * It might be conceivable to ie. widen a field for the customer, but then promote it to something completely
     * different. Like: go from VARCHAR(20) to VARCHAR(40), only to find that in fact it should be NVARCHAR(20).
     * Thus promotion SHOULD make it possible to narrow the size.
     */
    @Test
    public void testPromotingTypeRegardlessOfShortening() throws Exception
    {
        // mock existing SQL type:
        columnInfo.columnSize = 20;
        columnInfo.typeName = "VARCHAR";

        // mock desired type:
        when(modelFieldType.getSqlType()).thenReturn("NVARCHAR(15)");

        databaseUtil.checkFieldType(modelEntity, modelField, columnInfo, messages, true, false);

        // update should be performed:
        verify(statement).executeUpdate(any(String.class));
        Assert.assertThat(messages, Matchers.contains(Matchers.containsString("has been promoted")));
    }

    @Test
    public void testSqlStatementComposition() throws Exception
    {
        // mock existing SQL type:
        columnInfo.columnSize = 45;
        columnInfo.typeName = "VARCHAR";

        // mock desired type:
        when(modelFieldType.getSqlType()).thenReturn("NVARCHAR(123)");

        databaseUtil.checkFieldType(modelEntity, modelField, columnInfo, messages, true, true);

        // update should be performed:
        verify(statement).executeUpdate("ALTER TABLE " + MOCK_TABLE_NAME + " MODIFY " + MOCK_COLUMN_NAME + " NVARCHAR(123)");
    }
}
