package org.ofbiz.core.entity.jdbc;

import org.junit.Test;
import org.mockito.Mock;
import org.ofbiz.core.entity.jdbc.sql.escape.SqlEscapeHelper;
import org.ofbiz.core.entity.model.ModelField;

import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.ofbiz.core.entity.jdbc.SqlJdbcUtil.isBoolean;
import static org.ofbiz.core.entity.jdbc.SqlJdbcUtil.makeWhereStringFromFields;

public class TestSqlJdbcUtil {

    @Mock
    private SqlEscapeHelper sqlEscapeHelper;

    @Test
    public void makeWhereStringFromFieldsShouldReturnEmptyStringForEmptyListOfModelFields() {
        // Set up
        final Map<String, ?> fieldConditions = singletonMap("foo", "bar");
        final List<ModelField> modelFields = emptyList();

        // Invoke
        final String whereString = makeWhereStringFromFields(modelFields, fieldConditions, "anyOperator", sqlEscapeHelper);

        // Check
        assertEquals("", whereString);
    }

    @Test
    public void makeWhereStringFromFieldsShouldReturnEmptyStringForNullListOfModelFields() {
        // Set up
        final Map<String, ?> fieldConditions = singletonMap("foo", "bar");

        // Invoke
        final String whereString = makeWhereStringFromFields(null, fieldConditions, "anyOperator", sqlEscapeHelper);

        // Check
        assertEquals("", whereString);
    }

    @Test
    public void makeWhereStringShouldGenerateEqualsClauseForNonNullValue() {
        // Set up
        final String columnName = "myColumn";
        final String fieldName = "myField";
        final ModelField mockField = getMockModelField(fieldName, columnName);
        final Map<String, ?> fieldValues = singletonMap(fieldName, "any non-null value");

        // Invoke
        final String whereString = makeWhereStringFromFields(singletonList(mockField), fieldValues, "anyOperator", sqlEscapeHelper);

        // Check
        assertEquals(columnName + "=?", whereString);
    }

    private ModelField getMockModelField(final String fieldName, final String columnName) {
        final ModelField modelField = mock(ModelField.class);
        when(modelField.getColName()).thenReturn(columnName);
        when(modelField.getName()).thenReturn(fieldName);
        return modelField;
    }

    @Test
    public void makeWhereStringShouldGenerateIsNullClauseForNullValue() {
        // Set up
        final String columnName = "myColumn";
        final ModelField mockField = getMockModelField("myField", columnName);
        final Map<String, ?> fieldValues = emptyMap();

        // Invoke
        final String whereString = makeWhereStringFromFields(singletonList(mockField), fieldValues, "anyOperator", sqlEscapeHelper);

        // Check
        assertEquals(columnName + " IS NULL", whereString);
    }

    @Test
    public void makeWhereStringShouldUseProvidedOperatorBetweenMultipleFields() {
        // Set up
        final String columnName1 = "myColumn1";
        final String fieldName1 = "myField1";
        final ModelField mockField1 = getMockModelField(fieldName1, columnName1);

        final String columnName2 = "myColumn2";
        final String fieldName2 = "myField2";
        final ModelField mockField2 = getMockModelField(fieldName2, columnName2);

        final Map<String, ?> fieldValues = singletonMap(fieldName1, "any non-null value");

        // Invoke
        final String whereString = makeWhereStringFromFields(asList(mockField1, mockField2), fieldValues, "OR", sqlEscapeHelper);

        // Check
        assertEquals("myColumn1=? OR myColumn2 IS NULL", whereString);
    }

    @Test
    public void booleanFieldShouldBeReportedAsBoolean() {
        assertTrue(isBoolean(Boolean.class.getName()));
    }

    @Test
    public void nonBooleanFieldShouldNotBeReportedAsBoolean() {
        assertFalse(isBoolean(String.class.getName()));
    }
}
