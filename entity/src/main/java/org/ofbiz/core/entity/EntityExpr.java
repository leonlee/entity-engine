/*
 * $Id: EntityExpr.java,v 1.1 2005/04/01 05:58:03 sfarquhar Exp $
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

import org.ofbiz.core.entity.jdbc.sql.escape.SqlEscapeHelper;
import org.ofbiz.core.entity.model.ModelEntity;
import org.ofbiz.core.entity.model.ModelField;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Encapsulates simple expressions used for specifying queries
 *
 * @author <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 * @version $Revision: 1.1 $
 * @since 2.0
 */
public class EntityExpr extends EntityCondition {

    private Object lhs;
    private boolean leftUpper = false;
    private EntityOperator operator;
    private Object rhs;
    private boolean rightUpper = false;

    protected EntityExpr() {
        super(null);
    }

    public EntityExpr(String lhs, EntityOperator operator, Object rhs, SqlEscapeHelper sqlEscapeHelper) {
        super(sqlEscapeHelper);
        if (lhs == null) {
            throw new IllegalArgumentException("The field name cannot be null");
        }
        if (operator == null) {
            throw new IllegalArgumentException("The operator argument cannot be null");
        }

        this.lhs = lhs;
        this.operator = operator;
        this.rhs = rhs;
    }

    public EntityExpr(String lhs, boolean leftUpper, EntityOperator operator, Object rhs, boolean rightUpper, SqlEscapeHelper sqlEscapeHelper) {
        this(lhs, operator, rhs, sqlEscapeHelper);
        this.leftUpper = leftUpper;
        this.rightUpper = rightUpper;
    }

    public EntityExpr(EntityCondition lhs, EntityOperator operator, EntityCondition rhs, SqlEscapeHelper sqlEscapeHelper) {
        super(sqlEscapeHelper);
        if (lhs == null) {
            throw new IllegalArgumentException("The left EntityCondition argument cannot be null");
        }
        if (rhs == null) {
            throw new IllegalArgumentException("The right EntityCondition argument cannot be null");
        }
        if (operator == null) {
            throw new IllegalArgumentException("The operator argument cannot be null");
        }

        this.lhs = lhs;
        this.operator = operator;
        this.rhs = rhs;
    }

    public void setLUpper(boolean upper) {
        leftUpper = upper;
    }

    public boolean isLUpper() {
        return leftUpper;
    }

    public boolean isRUpper() {
        return rightUpper;
    }

    public void setRUpper(boolean upper) {
        rightUpper = upper;
    }

    public Object getLhs() {
        return lhs;
    }

    public EntityOperator getOperator() {
        return operator;
    }

    public Object getRhs() {
        return rhs;
    }

    public String makeWhereString(ModelEntity modelEntity, List<? super EntityConditionParam> entityConditionParams) {
        // if (Debug.verboseOn()) Debug.logVerbose("makeWhereString for entity " + modelEntity.getEntityName());
        StringBuilder whereStringBuilder = new StringBuilder();

        if (lhs instanceof String) {
            ModelField field = modelEntity.getField((String) this.getLhs());

            if (field != null) {
                if (this.getRhs() == null) {
                    whereStringBuilder.append(sqlEscapeHelper.escapeColumn(field.getColName()));
                    if (EntityOperator.NOT_EQUAL.equals(this.getOperator())) {
                        whereStringBuilder.append(" IS NOT NULL ");
                    } else {
                        whereStringBuilder.append(" IS NULL ");
                    }
                } else {
                    if (this.isLUpper()) {
                        whereStringBuilder.append("UPPER(" + sqlEscapeHelper.escapeColumn(field.getColName()) + ")");
                    } else {
                        whereStringBuilder.append(sqlEscapeHelper.escapeColumn(field.getColName()));
                    }
                    whereStringBuilder.append(' ');
                    whereStringBuilder.append(this.getOperator().toString());
                    whereStringBuilder.append(' ');

                    // treat the IN operator as a special case, especially with a Collection rhs
                    if (EntityOperator.IN.equals(this.getOperator())) {
                        whereStringBuilder.append('(');

                        if (rhs instanceof Collection) {
                            Iterator<?> rhsIter = ((Collection<?>) rhs).iterator();

                            while (rhsIter.hasNext()) {
                                Object inObj = rhsIter.next();

                                whereStringBuilder.append('?');
                                if (rhsIter.hasNext()) {
                                    whereStringBuilder.append(", ");
                                }

                                if (this.isRUpper()) {
                                    if (inObj instanceof String) {
                                        inObj = ((String) inObj).toUpperCase();
                                    }
                                }
                                entityConditionParams.add(new EntityConditionParam(field, inObj));
                            }
                        } else if (rhs instanceof EntityWhereString) {
                            whereStringBuilder.append(" " + ((EntityWhereString) rhs).makeWhereString(modelEntity, Collections.emptyList()) + " ");
                        } else {
                            whereStringBuilder.append(" ? ");

                            if (this.isRUpper()) {
                                if (rhs instanceof String) {
                                    rhs = ((String) rhs).toUpperCase();
                                }
                            }
                            entityConditionParams.add(new EntityConditionParam(field, rhs));
                        }

                        whereStringBuilder.append(") ");
                    } else {
                        whereStringBuilder.append(" ? ");

                        if (this.isRUpper()) {
                            if (rhs instanceof String) {
                                rhs = ((String) rhs).toUpperCase();
                            }
                        }
                        entityConditionParams.add(new EntityConditionParam(field, rhs));
                    }
                }
            } else {
                throw new IllegalArgumentException("ModelField with field name " + this.getLhs() + " not found");
            }
        } else if (lhs instanceof EntityCondition) {
            // then rhs MUST also be an EntityCondition
            whereStringBuilder.append('(');
            whereStringBuilder.append(((EntityCondition) lhs).makeWhereString(modelEntity, entityConditionParams));
            whereStringBuilder.append(") ");
            whereStringBuilder.append(this.getOperator().toString());
            whereStringBuilder.append(" (");
            whereStringBuilder.append(((EntityCondition) rhs).makeWhereString(modelEntity, entityConditionParams));
            whereStringBuilder.append(')');
        }
        return whereStringBuilder.toString();
    }

    public void checkCondition(ModelEntity modelEntity) throws GenericModelException {
        // if (Debug.verboseOn()) Debug.logVerbose("checkCondition for entity " + modelEntity.getEntityName());
        if (lhs instanceof String) {
            if (modelEntity.getField((String) lhs) == null) {
                throw new GenericModelException("Field with name " + lhs + " not found in the " + modelEntity.getEntityName() + " Entity");
            }
        } else if (lhs instanceof EntityCondition) {
            ((EntityCondition) lhs).checkCondition(modelEntity);
            ((EntityCondition) rhs).checkCondition(modelEntity);
        }
    }

    @Override
    public int getParameterCount(ModelEntity modelEntity) {
        int parameterCount = 0;
        if (lhs instanceof String) {
            ModelField field = modelEntity.getField((String) this.getLhs());

            if (field != null) {
                if (this.getRhs() != null) {
                    // treat the IN operator as a special case, especially with a Collection rhs
                    if (EntityOperator.IN.equals(this.getOperator())) {
                        if (rhs instanceof Collection) {
                            parameterCount += ((Collection<?>) rhs).size();
                        } else if (rhs instanceof EntityWhereString) {
                            parameterCount += ((EntityWhereString) rhs).getParameterCount(modelEntity);
                        } else {
                            parameterCount++;
                        }
                    } else {
                        parameterCount++;
                    }
                }
            } else {
                throw new IllegalArgumentException("ModelField with field name " + this.getLhs() + " not found");
            }
        } else if (lhs instanceof EntityCondition) {
            // then rhs MUST also be an EntityCondition
            EntityCondition lhsCondition = (EntityCondition) lhs;
            EntityCondition rhsCondition = (EntityCondition) rhs;
            parameterCount += lhsCondition.getParameterCount(modelEntity);
            parameterCount += rhsCondition.getParameterCount(modelEntity);
        }
        return parameterCount;
    }

    public String toString() {
        return "[Expr::" + lhs + "::" + operator + "::" + rhs + "]";
    }
}
