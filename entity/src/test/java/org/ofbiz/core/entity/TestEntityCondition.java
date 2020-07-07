package org.ofbiz.core.entity;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.mockito.Mock;
import org.ofbiz.core.entity.jdbc.sql.escape.SqlEscapeHelper;
import org.ofbiz.core.entity.model.ModelEntity;
import org.ofbiz.core.entity.model.ModelField;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class TestEntityCondition {

    @Mock
    private SqlEscapeHelper sqlEscapeHelper;

    @Test
    public void testEntityConditionListParameterCount() {
        EntityConditionList ec = new EntityConditionList(ImmutableList.of(new MockEntityCondition(3), new MockEntityCondition(2)), EntityOperator.AND, sqlEscapeHelper);
        assertEquals("Wrong parameter count.", 5, ec.getParameterCount(null));
    }

    @Test
    public void testEntityExprParameterCountForEntityConditions() {
        EntityExpr ec = new EntityExpr(new MockEntityCondition(2), EntityOperator.AND, new MockEntityCondition(5), sqlEscapeHelper);
        assertEquals("Wrong parameter count.", 7, ec.getParameterCount(null));
    }

    @Test
    public void testEntityExprParameterCountForInQueryCollection() {
        EntityExpr ec = new EntityExpr("myField", EntityOperator.IN, ImmutableList.of("one", "two", "three"), sqlEscapeHelper);
        ModelEntity modelEntity = new ModelEntity();
        ModelField modelField = new ModelField();
        modelField.setName("myField");
        modelEntity.addField(modelField);
        assertEquals("Wrong parameter count.", 3, ec.getParameterCount(modelEntity));
    }

    @Test
    public void testEntityExprParameterCountForInQueryWhere() {
        EntityExpr ec = new EntityExpr("myField", EntityOperator.IN, new EntityWhereString("blah", sqlEscapeHelper), sqlEscapeHelper);
        ModelEntity modelEntity = new ModelEntity();
        ModelField modelField = new ModelField();
        modelField.setName("myField");
        modelEntity.addField(modelField);
        assertEquals("Wrong parameter count.", 0, ec.getParameterCount(modelEntity));
    }

    @Test
    public void testEntityExprParameterCountForInQueryLiteral() {
        EntityExpr ec = new EntityExpr("myField", EntityOperator.IN, "something", sqlEscapeHelper);
        ModelEntity modelEntity = new ModelEntity();
        ModelField modelField = new ModelField();
        modelField.setName("myField");
        modelEntity.addField(modelField);
        assertEquals("Wrong parameter count.", 1, ec.getParameterCount(modelEntity));
    }

    @Test
    public void testEntityExprListParameterCount() {
        EntityExprList ec = new EntityExprList(ImmutableList.of(
                new EntityExpr("myField", EntityOperator.IN, ImmutableList.of("one", "two"), sqlEscapeHelper),
                new EntityExpr("myField", EntityOperator.IN, ImmutableList.of("three", "four", "five"), sqlEscapeHelper)
        ), EntityOperator.AND, sqlEscapeHelper);
        ModelEntity modelEntity = new ModelEntity();
        ModelField modelField = new ModelField();
        modelField.setName("myField");
        modelEntity.addField(modelField);
        assertEquals("Wrong parameter count.", 5, ec.getParameterCount(modelEntity));
    }

    @Test
    public void testEntityFieldMapParameterCount() {
        EntityFieldMap ec = new EntityFieldMap(ImmutableMap.of(
                "myField", "one",
                "myField2", "two"
        ), EntityOperator.OR, sqlEscapeHelper);
        ModelEntity modelEntity = new ModelEntity();
        ModelField modelField = new ModelField();
        modelField.setName("myField");
        modelEntity.addField(modelField);
        ModelField modelField2 = new ModelField();
        modelField2.setName("myField2");
        modelEntity.addField(modelField2);
        assertEquals("Wrong parameter count.", 2, ec.getParameterCount(modelEntity));
    }

    @Test
    public void testEntityFieldMapParameterCountWithNulls() {
        EntityFieldMap ec = new EntityFieldMap(Collections.singletonMap("myField", null), EntityOperator.AND, sqlEscapeHelper);
        ModelEntity modelEntity = new ModelEntity();
        ModelField modelField = new ModelField();
        modelField.setName("myField");
        modelEntity.addField(modelField);
        assertEquals("Wrong parameter count.", 0, ec.getParameterCount(modelEntity));
    }

    /**
     * Fake entity condition implementation that has a specific parameter count.
     */
    private static class MockEntityCondition extends EntityCondition {
        private final int parameterCount;

        public MockEntityCondition(int parameterCount) {
            super(null);
            this.parameterCount = parameterCount;
        }

        @Override
        public int getParameterCount(ModelEntity modelEntity) {
            return parameterCount;
        }

        @Override
        public void checkCondition(ModelEntity modelEntity) throws GenericModelException {
            throw new UnsupportedOperationException();
        }

        @Override
        public String makeWhereString(ModelEntity modelEntity, List<? super EntityConditionParam> entityConditionParams) {
            throw new UnsupportedOperationException();
        }
    }
}
