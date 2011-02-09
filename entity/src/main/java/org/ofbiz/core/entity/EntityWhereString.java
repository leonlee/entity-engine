/*
 * $Id: EntityWhereString.java,v 1.1 2005/04/01 05:58:03 sfarquhar Exp $
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

import java.util.List;

/**
 * <p>Encapsulates SQL expressions used for where clause snippets. 
 *  NOTE: This is UNSAFE and BREAKS the idea behind the Entity Engine where
 *  you avoid directly specifying SQL. So, KEEP IT MINIMAL and preferrably replace
 *  it when the feature you are getting at is implemented in a more automatic way for you.</p>
 *
 * <p>By minimal I mean use this in conjunction with other EntityConditions like the
 *  EntityExpr, EntityExprList and EntityFieldMap objects which more cleanly 
 *  encapsulate where conditions and don't require you to directly write SQL.</p>
 *
 * @author     <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 * @version    $Revision: 1.1 $
 * @since      2.0
 */
public class EntityWhereString extends EntityCondition {

    protected String sqlString;

    protected EntityWhereString() {}

    public EntityWhereString(String sqlString) {
        this.sqlString = sqlString;
    }

    public String makeWhereString(ModelEntity modelEntity, List<? super EntityConditionParam> entityConditionParams) {
        return sqlString;
    }

    public void checkCondition(ModelEntity modelEntity) throws GenericModelException {// no nothing, this is always assumed to be fine... could do funky SQL syntax checking, but hey this is a HACK anyway
    }

    public String toString() {
        return "[WhereString::" + this.sqlString + "]";
    }
}
