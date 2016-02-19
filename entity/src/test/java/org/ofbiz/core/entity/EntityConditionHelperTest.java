package org.ofbiz.core.entity;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class EntityConditionHelperTest {

    public static final EntityCondition EMPTY_FIELD_MAP = new EntityFieldMap(Collections.<String, Object>emptyMap(), EntityOperator.BETWEEN);

    @Mock
    Predicate<EntityExpr> predicate;
    @Mock
    EntityExpr entityExpr;
    @Mock
    EntityExpr entityExprTransformed;
    @Mock
    EntityExpr entityExpr2;
    @Mock
    EntityExpr entityExprTransformed2;
    @Mock
    Function<EntityExpr, EntityCondition> function;

    @Before
    public void setUp() throws Exception {
        Mockito.when(function.apply(entityExpr)).thenReturn(entityExprTransformed);
        Mockito.when(function.apply(entityExpr2)).thenReturn(entityExprTransformed2);
    }

    @Test
    public void testChecksSingleExpr() throws Exception {
        assertThat(EntityConditionHelper.predicateTrueForEachLeafExpression(entityExpr, predicate), equalTo(false));

        verify(predicate).apply(entityExpr);
    }

    @Test
    public void testTransformsSingleExpr() throws Exception {
        assertThat(EntityConditionHelper.transformCondition(entityExpr, function), equalTo((EntityCondition) entityExprTransformed));
    }

    @Test
    public void testChecksLeafsOfExpressionsFirstFalse() throws Exception {
        EntityExpr expression = new EntityExpr(entityExpr, EntityOperator.LESS_THAN, entityExpr2);

        assertThat(EntityConditionHelper.predicateTrueForEachLeafExpression(expression, predicate), equalTo(false));

        verify(predicate).apply(entityExpr);
    }

    @Test
    public void testChecksLeafsOfExpressionsSecondFalse() throws Exception {
        EntityExpr expression = new EntityExpr(entityExpr, EntityOperator.LESS_THAN, entityExpr2);
        Mockito.when(predicate.apply(entityExpr)).thenReturn(true);

        assertThat(EntityConditionHelper.predicateTrueForEachLeafExpression(expression, predicate), equalTo(false));

        verify(predicate).apply(entityExpr);
        verify(predicate).apply(entityExpr2);
    }

    @Test
    public void testChecksLeafsOfExpressionsBothTrue() throws Exception {
        EntityExpr expression = new EntityExpr(entityExpr, EntityOperator.LESS_THAN, entityExpr2);
        Mockito.when(predicate.apply(entityExpr)).thenReturn(true);
        Mockito.when(predicate.apply(entityExpr2)).thenReturn(true);

        assertThat(EntityConditionHelper.predicateTrueForEachLeafExpression(expression, predicate), equalTo(true));

        verify(predicate).apply(entityExpr);
        verify(predicate).apply(entityExpr2);
    }

    @Test
    public void testChecksLeafsOfExprListFirstFalse() throws Exception {
        EntityExprList expressionList = new EntityExprList(ImmutableList.of(entityExpr, entityExpr2), EntityOperator.LESS_THAN);

        assertThat(EntityConditionHelper.predicateTrueForEachLeafExpression(expressionList, predicate), equalTo(false));

        verify(predicate).apply(entityExpr);
    }

    @Test
    public void testChecksLeafsOfExprListSecondFalse() throws Exception {
        EntityExprList expressionList = new EntityExprList(ImmutableList.of(entityExpr, entityExpr2), EntityOperator.LESS_THAN);
        Mockito.when(predicate.apply(entityExpr)).thenReturn(true);

        assertThat(EntityConditionHelper.predicateTrueForEachLeafExpression(expressionList, predicate), equalTo(false));

        verify(predicate).apply(entityExpr);
        verify(predicate).apply(entityExpr2);
    }

    @Test
    public void testChecksLeafsOfExprListBothTrue() throws Exception {
        EntityExprList expressionList = new EntityExprList(ImmutableList.of(entityExpr, entityExpr2), EntityOperator.LESS_THAN);
        Mockito.when(predicate.apply(entityExpr)).thenReturn(true);
        Mockito.when(predicate.apply(entityExpr2)).thenReturn(true);

        assertThat(EntityConditionHelper.predicateTrueForEachLeafExpression(expressionList, predicate), equalTo(true));

        verify(predicate).apply(entityExpr);
        verify(predicate).apply(entityExpr2);
    }

    @Test
    public void testChecksLeafsOfConditionListFirstFalse() throws Exception {
        EntityConditionList conditionList = new EntityConditionList(ImmutableList.of(entityExpr, entityExpr2, EMPTY_FIELD_MAP), EntityOperator.LESS_THAN);

        assertThat(EntityConditionHelper.predicateTrueForEachLeafExpression(conditionList, predicate), equalTo(false));

        verify(predicate).apply(entityExpr);
    }

    @Test
    public void testChecksLeafsOfConditionListSecondFalse() throws Exception {
        EntityConditionList conditionList = new EntityConditionList(ImmutableList.of(entityExpr, EMPTY_FIELD_MAP, entityExpr2), EntityOperator.LESS_THAN);
        Mockito.when(predicate.apply(entityExpr)).thenReturn(true);

        assertThat(EntityConditionHelper.predicateTrueForEachLeafExpression(conditionList, predicate), equalTo(false));

        verify(predicate).apply(entityExpr);
        verify(predicate).apply(entityExpr2);
    }

    @Test
    public void testChecksLeafsOfConditionListBothTrue() throws Exception {
        EntityConditionList conditionList = new EntityConditionList(ImmutableList.of(EMPTY_FIELD_MAP, entityExpr, entityExpr2), EntityOperator.LESS_THAN);
        Mockito.when(predicate.apply(entityExpr)).thenReturn(true);
        Mockito.when(predicate.apply(entityExpr2)).thenReturn(true);

        assertThat(EntityConditionHelper.predicateTrueForEachLeafExpression(conditionList, predicate), equalTo(true));

        verify(predicate).apply(entityExpr);
        verify(predicate).apply(entityExpr2);
    }

    @Test
    public void testTransformsLeafsOfExpressions() throws Exception {
        EntityExpr expression = new EntityExpr(entityExpr, EntityOperator.LESS_THAN, entityExpr2);

        EntityCondition result = EntityConditionHelper.transformCondition(expression, function);

        verify(function).apply(entityExpr);
        verify(function).apply(entityExpr2);

        assertThat(result, instanceOf(EntityExpr.class));
        assertThat(((EntityExpr) result).getOperator(), equalTo(EntityOperator.LESS_THAN));
        assertThat((EntityExpr) ((EntityExpr) result).getLhs(), equalTo(entityExprTransformed));
        assertThat((EntityExpr) ((EntityExpr) result).getRhs(), equalTo(entityExprTransformed2));
    }

    @Test
    public void testTransformsLeafsOfExprList() throws Exception {
        EntityExprList expressionList = new EntityExprList(ImmutableList.of(entityExpr, entityExpr2), EntityOperator.LESS_THAN);

        EntityCondition result = EntityConditionHelper.transformCondition(expressionList, function);

        verify(function).apply(entityExpr);
        verify(function).apply(entityExpr2);

        assertThat(result, instanceOf(EntityConditionList.class));
        assertThat(((EntityConditionList) result).getOperator(), equalTo(EntityOperator.LESS_THAN));
        assertThat(ImmutableList.copyOf(((EntityConditionList) result).getConditionIterator()), Matchers.<EntityCondition>contains(entityExprTransformed, entityExprTransformed2));
    }

    @Test
    public void testTransformsLeafsOfConditionList() throws Exception {
        EntityConditionList conditionList = new EntityConditionList(ImmutableList.of(entityExpr, entityExpr2, EMPTY_FIELD_MAP), EntityOperator.LESS_THAN);

        EntityCondition result = EntityConditionHelper.transformCondition(conditionList, function);

        verify(function).apply(entityExpr);
        verify(function).apply(entityExpr2);

        assertThat(result, instanceOf(EntityConditionList.class));
        assertThat(((EntityConditionList) result).getOperator(), equalTo(EntityOperator.LESS_THAN));
        assertThat(ImmutableList.copyOf(((EntityConditionList) result).getConditionIterator()), Matchers.<EntityCondition>contains(entityExprTransformed, entityExprTransformed2, EMPTY_FIELD_MAP));
    }
}
