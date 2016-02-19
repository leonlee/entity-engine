package org.ofbiz.core.entity.jdbc;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.ofbiz.core.entity.config.DatasourceInfo;
import org.ofbiz.core.entity.model.ModelEntity;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Test whether tables are created and queried for existence with the same schema
 * The incoherent behaviour make data base upgrade impossible See JRA-28526
 */
@RunWith(MockitoJUnitRunner.class)
public class TestDataBaseUtilsCoherentWithModelEntity {
    @Mock(answer = Answers.RETURNS_MOCKS)
    private DatasourceInfo datasourceInfo;

    @Mock(answer = Answers.RETURNS_MOCKS)
    private DatabaseMetaData databaseMetadata;


    @Test
    public void testTablesAreCreatedAndQueriedWithTheSameSchemaWhenNoSchemaProvided() throws SQLException {
        final GetTableNameAndSchema tableNameAndSchema = new GetTableNameAndSchema(null).invoke();
        assertNull(tableNameAndSchema.getLookupSchemaName());
        assertEquals("table", tableNameAndSchema.getTableNameFromModelEntity());

    }

    @Test
    public void testTablesAreCreatedAndQueriedWithTheSameSchemaWhenSchemaIsProvided() throws SQLException {
        final GetTableNameAndSchema tableNameAndSchema = new GetTableNameAndSchema("dbo").invoke();

        assertEquals("dbo", tableNameAndSchema.getLookupSchemaName());
        assertTrue("Table name should contains schema", tableNameAndSchema.getTableNameFromModelEntity().contains("dbo."));
    }

    @Test
    public void testTablesAreCreatedAndQueriedWithTheSameSchemaWhenSchemaIsBlank() throws SQLException {
        final GetTableNameAndSchema tableNameAndSchema = new GetTableNameAndSchema("").invoke();
        assertNull(tableNameAndSchema.getLookupSchemaName());
        assertEquals("table", tableNameAndSchema.getTableNameFromModelEntity());
    }


    private class GetTableNameAndSchema {
        private final String schemaName;
        private String tableNameFromModelEntity;
        private String lookupSchemaName;

        public GetTableNameAndSchema(String schemaName) {
            this.schemaName = schemaName;
        }

        public String getTableNameFromModelEntity() {
            return tableNameFromModelEntity;
        }

        public String getLookupSchemaName() {
            return lookupSchemaName;
        }

        public GetTableNameAndSchema invoke() throws SQLException {
            when(datasourceInfo.getSchemaName()).thenReturn(schemaName);
            when(databaseMetadata.getUserName()).thenReturn("someUser");
            final ModelEntity modelEntity = new ModelEntity();
            modelEntity.setTableName("table");
            when(databaseMetadata.supportsSchemasInTableDefinitions()).thenReturn(Boolean.TRUE);
            tableNameFromModelEntity = modelEntity.getTableName(datasourceInfo);
            lookupSchemaName = DatabaseUtil.getSchemaPattern(databaseMetadata, schemaName);
            return this;
        }
    }
}
