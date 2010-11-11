/*
 * $Id: EntityFieldMap.java,v 1.1 2005/04/01 05:58:03 sfarquhar Exp $
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

import java.util.*;

import org.ofbiz.core.entity.model.*;
import org.ofbiz.core.entity.jdbc.*;

/**
 * Encapsulates simple expressions used for specifying queries
 *
 * @author     <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 * @version    $Revision: 1.1 $
 * @since      2.0
 */
public class EntityFieldMap extends EntityCondition {

    protected Map fieldMap;
    protected EntityOperator operator;

    protected EntityFieldMap() {}

    public EntityFieldMap(Map fieldMap, EntityOperator operator) {
        this.fieldMap = fieldMap;
        this.operator = operator;
    }

    public EntityOperator getOperator() {
        return this.operator;
    }

    public Object getField(String name) {
        return this.fieldMap.get(name);
    }
    
    public boolean containsField(String name) {
        return this.fieldMap.containsKey(name);
    }
    
    public Iterator getFieldKeyIterator() {
        return this.fieldMap.keySet().iterator();
    }
    
    public Iterator getFieldEntryIterator() {
        return this.fieldMap.entrySet().iterator();
    }
    
    public String makeWhereString(ModelEntity modelEntity, List entityConditionParams) {
        // if (Debug.verboseOn()) Debug.logVerbose("makeWhereString for entity " + modelEntity.getEntityName());
        List whereFields = new ArrayList();

        if (fieldMap != null && fieldMap.size() > 0) {
            for (int fi = 0; fi < modelEntity.getFieldsSize(); fi++) {
                ModelField curField = modelEntity.getField(fi);

                if (fieldMap.containsKey(curField.getName())) {
                    whereFields.add(curField);
                }
            }
        }
        return SqlJdbcUtil.makeWhereStringFromFields(whereFields, fieldMap, operator.getCode(), entityConditionParams);
    }

    public void checkCondition(ModelEntity modelEntity) throws GenericModelException {
        // if (Debug.verboseOn()) Debug.logVerbose("checkCondition for entity " + modelEntity.getEntityName());
        // make sure that all fields in the Map are valid
        if (fieldMap != null && !modelEntity.areFields(fieldMap.keySet())) {
            throw new GenericModelException("At least one of the passed fields is not valid: " + fieldMap.keySet().toString());
        }
    }

    public String toString() {
        return "[FieldMap::" + operator + "::" + fieldMap + "]";
    }
}
