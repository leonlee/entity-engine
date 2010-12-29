package org.ofbiz.core.entity.jdbc;

import static junit.framework.Assert.*;
import static org.mockito.Mockito.*;

import org.junit.Test;
import org.ofbiz.core.entity.ConnectionProvider;
import org.ofbiz.core.entity.GenericEntityException;
import org.ofbiz.core.entity.model.ModelEntity;
import org.ofbiz.core.entity.model.ModelField;
import org.ofbiz.core.entity.model.ModelIndex;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;

/**
 * Unit test for {@link org.ofbiz.core.entity.jdbc.DatabaseUtil}.
 *
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
        DatabaseUtil du = new DatabaseUtil("Santa's Little Helper", null, null, new ConnectionProvider() {
            public Connection getConnection(final String name) throws SQLException, GenericEntityException {
                return connection;
            }
        });
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
}
