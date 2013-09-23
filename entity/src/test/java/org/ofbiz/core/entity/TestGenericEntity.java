package org.ofbiz.core.entity;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.ofbiz.core.entity.model.ModelEntity;
import org.ofbiz.core.entity.model.ModelField;
import org.ofbiz.core.entity.model.ModelFieldType;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestGenericEntity {

    @Mock private GenericDelegator mockGenericDelegator;
    @Mock private ModelEntity mockModelEntity;
    private GenericEntity entity;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        entity = new GenericEntity(mockGenericDelegator, mockModelEntity);
    }

    @Test
    public void setFieldsShouldDoNothingIfGivenNullMap() {
        // Set up
        final String fieldName = "myField";
        final String fieldValue = "myValue";
        entity.fields.put(fieldName, fieldValue);

        // Invoke
        entity.setFields(null);

        // Check
        assertEquals(fieldValue, entity.fields.get(fieldName));
    }

    @Test
    public void setFieldsShouldIgnoreFieldsNotIncludedInMap() {
        // Set up
        final String modifiedField = "modifiedField";
        final String unmodifiedField = "unmodifiedField";
        final String modifiedValue = "modifiedValue";
        final String unmodifiedValue = "unmodifiedValue";
        entity.fields.put(modifiedField, "previousValue");
        entity.fields.put(unmodifiedField, unmodifiedValue);
        setUpValidField(modifiedField);

        // Invoke
        entity.setFields(singletonMap(modifiedField, modifiedValue));

        // Check
        assertEquals(modifiedValue, entity.fields.get(modifiedField));
        assertEquals(unmodifiedValue, entity.fields.get(unmodifiedField));
    }

    @Test(expected = IllegalArgumentException.class)
    public void setShouldNotAllowUnknownFields() {
        // Invoke
        entity.set("unknown field", "any value");
    }

    @Test
    public void settingNullValueWithSetIfNullFalseShouldReturnOriginalValueAndLeaveEntityUnchanged() {
        // Set up
        final String unmodifiedField = "unmodifiedField";
        final String previousValue = "previousValue";
        setUpValidField(unmodifiedField);
        entity.fields.put(unmodifiedField, previousValue);

        // Invoke
        final Object returnedValue = entity.set(unmodifiedField, null, false);

        // Check
        assertEquals(previousValue, returnedValue);
        assertEquals(previousValue, entity.fields.get(unmodifiedField));
    }

    @Test
    public void settingNullValueWithSetIfNullTrueShouldReturnOriginalValueAndUpdateEntity() {
        // Set up
        final String modifiedField = "modifiedField";
        final String previousValue = "previousValue";
        setUpValidField(modifiedField);
        entity.fields.put(modifiedField, previousValue);

        // Invoke
        final Object returnedValue = entity.set(modifiedField, null, true);

        // Check
        assertEquals(previousValue, returnedValue);
        assertNull(entity.fields.get(modifiedField));
    }

    private ModelField setUpValidField(final String name) {
        final ModelField mockField = mock(ModelField.class);
        when(mockModelEntity.getField(name)).thenReturn(mockField);
        return mockField;
    }

    @Test
    public void settingBooleanFieldToBooleanTrueShouldSetFieldToTrue() throws Exception {
        assertSetFieldToBooleanValue(TRUE, TRUE, Boolean.class);
    }

    @Test
    public void settingBooleanFieldToBooleanFalseShouldSetFieldToFalse() throws Exception {
        assertSetFieldToBooleanValue(FALSE, FALSE, Boolean.class);
    }

    @Test
    public void settingNonBooleanFieldToBooleanTrueShouldSetFieldToY() throws Exception {
        assertSetFieldToBooleanValue(TRUE, "Y", String.class);
    }

    @Test
    public void settingNonBooleanFieldToBooleanFalseShouldSetFieldToN() throws Exception {
        assertSetFieldToBooleanValue(FALSE, "N", String.class);
    }

    private void assertSetFieldToBooleanValue(
            final Boolean newBooleanValue, final Object expectedFieldValue, final Class<?> fieldJavaType)
        throws Exception
    {
        // Set up
        final String modifiedField = "modifiedField";
        final String previousValue = "previousValue";
        final ModelField mockField = setUpValidField(modifiedField);
        final String fieldType = "theFieldType";
        when(mockField.getType()).thenReturn(fieldType);
        final ModelFieldType mockModelFieldType = mock(ModelFieldType.class);
        when(mockModelFieldType.getJavaType()).thenReturn(fieldJavaType.getName());
        when(mockGenericDelegator.getEntityFieldType(mockModelEntity, fieldType)).thenReturn(mockModelFieldType);
        entity.fields.put(modifiedField, previousValue);

        // Invoke
        final Object returnedValue = entity.set(modifiedField, newBooleanValue, false);

        // Check
        assertEquals(previousValue, returnedValue);
        assertEquals(expectedFieldValue, entity.fields.get(modifiedField));
    }
}
