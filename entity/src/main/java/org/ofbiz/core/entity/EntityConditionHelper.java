package org.ofbiz.core.entity;

import com.google.common.base.Function;
import com.google.common.base.Predicate;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class EntityConditionHelper {

    /**
     * Traverses input EntityCondition and checks of predicate is true for each leaf EntityExpr (that does not contain
     * sub-conditions)
     *
     * @param input     EntityCondition to be checked
     * @param predicate to be applied to each leaf EntityExpr
     * @return false if at least for one EntityExpr predicate was not true
     */
    public static boolean predicateTrueForEachLeafExpression(final EntityCondition input, final Predicate<EntityExpr> predicate) {
        boolean result = true;
        if (input instanceof EntityExpr) {
            final EntityExpr expr = (EntityExpr) input;
            if (expr.getLhs() instanceof EntityCondition) {
                result = predicateTrueForEachLeafExpression((EntityCondition) expr.getLhs(), predicate) && predicateTrueForEachLeafExpression((EntityCondition) expr.getRhs(), predicate);
            } else {
                result = predicate.apply(expr);
            }
        } else if (input instanceof EntityConditionList) {
            final Iterator<? extends EntityCondition> conditionIterator = ((EntityConditionList) input).getConditionIterator();
            while (conditionIterator.hasNext()) {
                result &= predicateTrueForEachLeafExpression(conditionIterator.next(), predicate);
            }
        } else if (input instanceof EntityExprList) {
            final EntityExprList exprList = (EntityExprList) input;
            final Iterator<? extends EntityExpr> exprIterator = exprList.getExprIterator();
            while (exprIterator.hasNext()) {
                result &= predicateTrueForEachLeafExpression(exprIterator.next(), predicate);
            }
        }
        return result;
    }

    /**
     * Traverses input EntityCondition and each leaf EntityExpr (that does not contain sub-conditions) is transformed
     * with function
     *
     * @param input    EntityCondition to be transformed
     * @param function to be applied to each leaf EntityExpr
     * @return rewritten EntityCondition of the same structure except for leaf expressions transformed by function
     */
    public static EntityCondition transformCondition(final EntityCondition input, final Function<EntityExpr, EntityCondition> function) {
        if (input instanceof EntityExpr) {
            final EntityExpr expr = (EntityExpr) input;
            if (expr.getLhs() instanceof EntityCondition) {
                return new EntityExpr(transformCondition((EntityCondition) expr.getLhs(), function), expr.getOperator(), transformCondition((EntityCondition) expr.getRhs(), function));
            } else {
                return function.apply(expr);
            }
        } else if (input instanceof EntityConditionList) {
            final EntityConditionList conditionList = (EntityConditionList) input;
            final Iterator<? extends EntityCondition> conditionIterator = conditionList.getConditionIterator();
            final List<EntityCondition> result = new ArrayList<EntityCondition>(conditionList.getConditionListSize());
            while (conditionIterator.hasNext()) {
                result.add(transformCondition(conditionIterator.next(), function));
            }
            return new EntityConditionList(result, conditionList.getOperator());
        } else if (input instanceof EntityExprList) {
            final EntityExprList exprList = (EntityExprList) input;
            final Iterator<? extends EntityExpr> exprIterator = exprList.getExprIterator();
            final List<EntityCondition> result = new ArrayList<EntityCondition>(exprList.getExprListSize());
            while (exprIterator.hasNext()) {
                result.add(transformCondition(exprIterator.next(), function));
            }
            return new EntityConditionList(result, exprList.getOperator());
        } else {
            return input;
        }
    }
}
