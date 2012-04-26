/*
 * $Id: EntityConditionList.java,v 1.1 2005/04/01 05:58:03 sfarquhar Exp $
 *
 * Copyright (c) 2001, 2002 The Open For Business Project - www.ofbiz.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 * CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT
 * OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 * THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */
package org.ofbiz.core.entity;

import org.ofbiz.core.entity.model.ModelEntity;

import java.util.Iterator;
import java.util.List;

/**
 * Encapsulates a list of EntityConditions to be used as a single EntityCondition combined as specified
 *
 * @author     <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 * @version    $Revision: 1.1 $
 * @since      2.0
 */
public class EntityConditionList extends EntityCondition {

    protected List<? extends EntityCondition> conditionList;
    protected EntityOperator operator;

    protected EntityConditionList() {}

    public EntityConditionList(List<? extends EntityCondition> conditionList, EntityOperator operator) {
        this.conditionList = conditionList;
        this.operator = operator;
    }

    public EntityOperator getOperator() {
        return this.operator;
    }

    public EntityCondition getCondition(int index) {
        return this.conditionList.get(index);
    }
    
    public int getConditionListSize() {
        return this.conditionList.size();
    }
    
    public Iterator<? extends EntityCondition> getConditionIterator() {
        return this.conditionList.iterator();
    }
    
    public String makeWhereString(ModelEntity modelEntity, List<? super EntityConditionParam> entityConditionParams) {
        // if (Debug.verboseOn()) Debug.logVerbose("makeWhereString for entity " + modelEntity.getEntityName());
        StringBuilder whereStringBuilder = new StringBuilder();

        if (conditionList != null && conditionList.size() > 0) {
            for (int i = 0; i < conditionList.size(); i++) {
                EntityCondition condition = conditionList.get(i);

                whereStringBuilder.append('(');
                whereStringBuilder.append(condition.makeWhereString(modelEntity, entityConditionParams));
                whereStringBuilder.append(')');
                if (i < conditionList.size() - 1) {
                    whereStringBuilder.append(' ');
                    whereStringBuilder.append(operator.getCode());
                    whereStringBuilder.append(' ');
                }
            }
        }
        return whereStringBuilder.toString();
    }

    public void checkCondition(ModelEntity modelEntity) throws GenericModelException {
        // if (Debug.verboseOn()) Debug.logVerbose("checkCondition for entity " + modelEntity.getEntityName());

        for (EntityCondition condition : conditionList) {

            condition.checkCondition(modelEntity);
        }
    }

    public String toString() {
        StringBuilder toStringBuilder = new StringBuilder();

        toStringBuilder.append("[conditionList::");
        if (conditionList != null && conditionList.size() > 0) {
            for (int i = 0; i < conditionList.size(); i++) {
                EntityCondition condition = conditionList.get(i);

                toStringBuilder.append(condition.toString());
                if (i > 0) toStringBuilder.append("::");
            }
        }
        toStringBuilder.append(']');
        return toStringBuilder.toString();
    }
}
