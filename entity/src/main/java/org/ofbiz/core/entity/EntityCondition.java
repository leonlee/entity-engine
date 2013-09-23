/*
 * $Id: EntityCondition.java,v 1.1 2005/04/01 05:58:03 sfarquhar Exp $
 *
 * <p>Copyright (c) 2001 The Open For Business Project - www.ofbiz.org
 *
 * <p>Permission is hereby granted, free of charge, to any person obtaining a
 *  copy of this software and associated documentation files (the "Software"),
 *  to deal in the Software without restriction, including without limitation
 *  the rights to use, copy, modify, merge, publish, distribute, sublicense,
 *  and/or sell copies of the Software, and to permit persons to whom the
 *  Software is furnished to do so, subject to the following conditions:
 *
 * <p>The above copyright notice and this permission notice shall be included
 *  in all copies or substantial portions of the Software.
 *
 * <p>THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 *  OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 *  IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 *  CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT
 *  OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 *  THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.ofbiz.core.entity;

import org.ofbiz.core.entity.model.ModelEntity;

import java.io.Serializable;
import java.util.List;

/**
 * Represents the conditions to be used to constrain a query
 * <br>An EntityCondition can represent various type of constraints, including:
 * <ul>
 *  <li>EntityConditionList: a list of EntityConditions, combined with the operator specified
 *  <li>EntityExpr: for simple expressions or expressions that combine EntityConditions
 *  <li>EntityExprList: a list of EntityExprs, combined with the operator specified
 *  <li>EntityFieldMap: a map of fields where the field (key) equals the value, combined with the operator specified
 * </ul>
 * These can be used in various combinations using the EntityExpr object.
 *
 *@author     <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 *@since    July 12, 2002
 *@version    1.0
 */
public abstract class EntityCondition implements Serializable {

    /**
     * Creates a string for use in a WHERE clause, based on this condition.
     *
     * @param modelEntity the entity being queried for (required)
     * @param entityConditionParams a non-null list to which this method will add any required bind values
     * @return a non-null string
     */
    public abstract String makeWhereString(
            ModelEntity modelEntity, List<? super EntityConditionParam> entityConditionParams);

    /**
     * Checks this condition against the given entity.
     *
     * @param modelEntity the entity being queried for (required)
     * @throws GenericModelException if the condition is not met
     */
    public abstract void checkCondition(ModelEntity modelEntity) throws GenericModelException;
}
