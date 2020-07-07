package org.ofbiz.core.entity;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.ofbiz.core.entity.jdbc.sql.escape.SqlEscapeHelper;
import org.ofbiz.core.entity.model.ModelEntity;
import org.ofbiz.core.entity.model.ModelField;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.ofbiz.core.entity.EntityOperator.IN;

@RunWith(MockitoJUnitRunner.class)
public class EntityExprTest {

    @Test
    public void shouldMakeWhereInString() {

        final String FIELD_NAME = "name";

        ModelEntity modelEntity = mock(ModelEntity.class);
        ModelField modelField = mock(ModelField.class);
        when(modelField.getColName()).thenReturn("NAME");
        when(modelEntity.getField(FIELD_NAME)).thenReturn(modelField);

        List<String> params = new ArrayList<>();
        params.add("john");
        params.add("adam");
        params.add("stacy");

        SqlEscapeHelper sqlEscapeHelper = mock(SqlEscapeHelper.class);
        when(sqlEscapeHelper.escapeColumn("NAME")).thenReturn("`NAME`");

        EntityExpr entityExpr = new EntityExpr(FIELD_NAME, IN, params, sqlEscapeHelper);
        entityExpr.sqlEscapeHelper = sqlEscapeHelper;

        List<EntityConditionParam> entityConditionParams = new ArrayList<>();

        String result = entityExpr.makeWhereString(modelEntity, entityConditionParams);
        System.out.println(entityConditionParams);
        assertEquals("`NAME` IN (?, ?, ?) ", result);
    }
}
