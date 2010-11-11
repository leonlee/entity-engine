/*
 * $Id: EntityUtil.java,v 1.1 2005/04/01 05:58:02 sfarquhar Exp $
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


import java.util.*;
import org.ofbiz.core.util.*;
import org.ofbiz.core.entity.model.*;


/**
 * Helper methods when dealing with Entities, especially ones that follow certain conventions
 *
 *@author     Eric Pabst
 *@created    Tue Aug 07 01:10:32 MDT 2001
 *@version    1.0
 */
public class EntityUtil {

    public static GenericValue getFirst(List values) {
        if ((values != null) && (values.size() > 0)) {
            return (GenericValue) values.iterator().next();
        } else {
            return null;
        }
    }

    public static GenericValue getOnly(List values) {
        if (values != null) {
            final int size = values.size();
            if (size <= 0) {
                return null;
            }
            if (size == 1) {
                return (GenericValue) values.iterator().next();
            } else {
                throw new IllegalArgumentException("Passed List had more than one value.");
            }
        } else {
            return null;
        }
    }

    /**
     *returns the values that are currently active.
     *
     *@param datedValues GenericValue's that have "fromDate" and "thruDate" fields
     *@return List of GenericValue's that are currently active
     */
    public static List filterByDate(List datedValues) {
        return filterByDate(datedValues, UtilDateTime.nowTimestamp(), "fromDate", "thruDate", true);
    }

    /**
     *returns the values that are currently active.
     *
     *@param datedValues GenericValue's that have "fromDate" and "thruDate" fields
     *@param allAreSame Specifies whether all values in the List are of the same entity; this can help speed things up a fair amount since we only have to see if the from and thru date fields are valid once
     *@return List of GenericValue's that are currently active
     */
    public static List filterByDate(List datedValues, boolean allAreSame) {
        return filterByDate(datedValues, UtilDateTime.nowTimestamp(), "fromDate", "thruDate", allAreSame);
    }

    /**
     *returns the values that are active at the moment.
     *
     *@param datedValues GenericValue's that have "fromDate" and "thruDate" fields
     *@param moment the moment in question
     *@return List of GenericValue's that are active at the moment
     */
    public static List filterByDate(List datedValues, java.util.Date moment) {
        return filterByDate(datedValues, new java.sql.Timestamp(moment.getTime()), "fromDate", "thruDate", true);
    }

    /**
     *returns the values that are active at the moment.
     *
     *@param datedValues GenericValue's that have "fromDate" and "thruDate" fields
     *@param moment the moment in question
     *@return List of GenericValue's that are active at the moment
     */
    public static List filterByDate(List datedValues, java.sql.Timestamp moment) {
        return filterByDate(datedValues, moment, "fromDate", "thruDate", true);
    }

    /**
     *returns the values that are active at the moment.
     *
     *@param datedValues GenericValue's that have "fromDate" and "thruDate" fields
     *@param moment the moment in question
     *@param allAreSame Specifies whether all values in the List are of the same entity; this can help speed things up a fair amount since we only have to see if the from and thru date fields are valid once
     *@return List of GenericValue's that are active at the moment
     */
    public static List filterByDate(List datedValues, java.sql.Timestamp moment, String fromDateName, String thruDateName, boolean allAreSame) {
        if (datedValues == null) return null;
        if (moment == null) return datedValues;
        if (fromDateName == null) throw new IllegalArgumentException("You must specify the name of the fromDate field to use this method");
        if (thruDateName == null) throw new IllegalArgumentException("You must specify the name of the thruDate field to use this method");

        List result = new LinkedList();
        Iterator iter = datedValues.iterator();

        if (allAreSame) {
            ModelField fromDateField = null;
            ModelField thruDateField = null;

            if (iter.hasNext()) {
                GenericValue datedValue = (GenericValue) iter.next();

                fromDateField = datedValue.getModelEntity().getField(fromDateName);
                if (fromDateField == null) throw new IllegalArgumentException("\"" + fromDateName + "\" is not a field of " + datedValue.getEntityName());
                thruDateField = datedValue.getModelEntity().getField(thruDateName);
                if (fromDateField == null) throw new IllegalArgumentException("\"" + thruDateName + "\" is not a field of " + datedValue.getEntityName());

                java.sql.Timestamp fromDate = (java.sql.Timestamp) datedValue.dangerousGetNoCheckButFast(fromDateField);
                java.sql.Timestamp thruDate = (java.sql.Timestamp) datedValue.dangerousGetNoCheckButFast(thruDateField);

                if ((thruDate == null || thruDate.after(moment)) && (fromDate == null || fromDate.before(moment))) {
                    result.add(datedValue);
                }// else not active at moment
            }
            while (iter.hasNext()) {
                GenericValue datedValue = (GenericValue) iter.next();
                java.sql.Timestamp fromDate = (java.sql.Timestamp) datedValue.dangerousGetNoCheckButFast(fromDateField);
                java.sql.Timestamp thruDate = (java.sql.Timestamp) datedValue.dangerousGetNoCheckButFast(thruDateField);

                if ((thruDate == null || thruDate.after(moment)) && (fromDate == null || fromDate.before(moment))) {
                    result.add(datedValue);
                }// else not active at moment
            }
        } else {
            // if not all values are known to be of the same entity, must check each one...
            while (iter.hasNext()) {
                GenericValue datedValue = (GenericValue) iter.next();
                java.sql.Timestamp fromDate = datedValue.getTimestamp(fromDateName);
                java.sql.Timestamp thruDate = datedValue.getTimestamp(thruDateName);

                if ((thruDate == null || thruDate.after(moment)) && (fromDate == null || fromDate.before(moment))) {
                    result.add(datedValue);
                }// else not active at moment
            }
        }

        return result;
    }

    /**
     *returns the values that match the values in fields
     *
     *@param values List of GenericValues
     *@param fields the field-name/value pairs that must match
     *@return List of GenericValue's that match the values in fields
     */
    public static List filterByAnd(List values, Map fields) {
        if (values == null) return null;

        List result = null;

        if (fields == null || fields.size() == 0) {
            result = new ArrayList(values);
        } else {
            result = new ArrayList(values.size());
            Iterator iter = values.iterator();

            while (iter.hasNext()) {
                GenericValue value = (GenericValue) iter.next();

                if (value.matchesFields(fields)) {
                    result.add(value);
                }// else did not match
            }
        }
        return result;
    }

    /**
     *returns the values that match the exprs in list
     *
     *@param values List of GenericValues
     *@param exprs the expressions that must validate to true
     *@return List of GenericValue's that match the values in fields
     */
    public static List filterByAnd(List values, List exprs) {
        if (values == null) return null;
        if (exprs == null || exprs.size() == 0) {
            // no constraints... oh well
            return values;
        }

        List result = new ArrayList();
        Iterator iter = values.iterator();

        while (iter.hasNext()) {
            GenericValue value = (GenericValue) iter.next();
            Iterator exprIter = exprs.iterator();
            boolean include = true;

            while (exprIter.hasNext()) {
                EntityExpr expr = (EntityExpr) exprIter.next();
                Object lhs = value.get((String) expr.getLhs());
                Object rhs = expr.getRhs();

                int operatorId = expr.getOperator().getId();
                switch (operatorId) {
                    case EntityOperator.ID_EQUALS:
                        // if the field named by lhs is not equal to rhs value, constraint fails
                        if (lhs == null) {
                            if (rhs != null) {
                                include = false;
                            }
                        } else if (!lhs.equals(rhs)) {
                            include = false;
                        }
                        break;
                    case EntityOperator.ID_NOT_EQUAL:
                        if (lhs == null) {
                            if (rhs == null) {
                                include = false;
                            }
                        } else if (lhs.equals(rhs)) {
                            include = false;
                        }
                        break;
                    case EntityOperator.ID_GREATER_THAN:
                        if (lhs == null) {
                            if (rhs == null) {
                                include = false;
                            }
                        } else if (((Comparable) lhs).compareTo(rhs) <= 0) {
                            include = false;
                        }
                        break;
                    case EntityOperator.ID_GREATER_THAN_EQUAL_TO:
                        if (lhs == null) {
                            if (rhs != null) {
                                include = false;
                            }
                        } else if (((Comparable) lhs).compareTo(rhs) < 0) {
                            include = false;
                        }
                        break;
                    case EntityOperator.ID_LESS_THAN:
                        if (lhs == null) {
                            if (rhs == null) {
                                include = false;
                            }
                        } else if (((Comparable) lhs).compareTo(rhs) >= 0) {
                            include = false;
                        }
                        break;
                    case EntityOperator.ID_LESS_THAN_EQUAL_TO:
                        if (lhs == null) {
                            if (rhs != null) {
                                include = false;
                            }
                        } else if (((Comparable) lhs).compareTo(rhs) > 0) {
                            include = false;
                        }
                        break;
                    /*
                    case EntityOperator.ID_LIKE:
                        if (lhs == null) {
                            if (rhs != null) {
                                include = false;
                            }
                        } else if (lhs instanceof String && rhs instanceof String) {
                            //see if the lhs value is like the rhs value, rhs will have the pattern characters in it...
                            include = false;
                        }
                        break;
                     */
                    default:
                        throw new IllegalArgumentException("The " + expr.getOperator().getCode() + " with id " + expr.getOperator().getId() + " operator is not yet supported by filterByAnd");
                }
                if (!include) break;
            }
            if (include) {
                result.add(value);
            }
        }
        return result;
    }

    /**
     *returns the values in the order specified
     *
     *@param values List of GenericValues
     *@param order The fields of the named entity to order the query by;
     *      optionally add a " ASC" for ascending or " DESC" for descending
     *@return List of GenericValue's in the proper order
     */
    public static List orderBy(List values, List orderBy) {
        if (values == null) return null;
        if (values.size() == 0) return UtilMisc.toList(values);

        List result = new ArrayList(values);

        Collections.sort(result, new OrderByComparator(orderBy));
        return result;
    }

    public static List getRelated(String relationName, List values) throws GenericEntityException {
        if (values == null) return null;

        List result = new ArrayList();
        Iterator iter = values.iterator();

        while (iter.hasNext()) {
            result.addAll(((GenericValue) iter.next()).getRelated(relationName));
        }
        return result;
    }

    public static List getRelatedCache(String relationName, List values) throws GenericEntityException {
        if (values == null) return null;

        List result = new ArrayList();
        Iterator iter = values.iterator();

        while (iter.hasNext()) {
            result.addAll(((GenericValue) iter.next()).getRelatedCache(relationName));
        }
        return result;
    }

    public static List getRelatedByAnd(String relationName, Map fields, List values) throws GenericEntityException {
        if (values == null) return null;

        List result = new ArrayList();
        Iterator iter = values.iterator();

        while (iter.hasNext()) {
            result.addAll(((GenericValue) iter.next()).getRelatedByAnd(relationName, fields));
        }
        return result;
    }

    static class OrderByComparator implements Comparator {

        private String field;
        private ModelField modelField = null;
        private boolean descending;
        private Comparator next = null;

        OrderByComparator(List orderBy) {
            this(orderBy, 0);
        }

        private OrderByComparator(List orderBy, int startIndex) {
            if (orderBy == null) throw new IllegalArgumentException("orderBy may not be empty");
            if (startIndex >= orderBy.size()) throw new IllegalArgumentException("startIndex may not be greater than or equal to orderBy size");
            String fieldAndDirection = (String) orderBy.get(startIndex);
            String upper = fieldAndDirection.trim().toUpperCase();

            if (upper.endsWith(" DESC")) {
                this.descending = true;
                this.field = fieldAndDirection.substring(0, fieldAndDirection.length() - 5);
            } else if (upper.endsWith(" ASC")) {
                this.descending = false;
                this.field = fieldAndDirection.substring(0, fieldAndDirection.length() - 4);
            } else {
                this.descending = false;
                this.field = fieldAndDirection;
            }
            if (startIndex + 1 < orderBy.size()) {
                this.next = new OrderByComparator(orderBy, startIndex + 1);
            }// else keep null
        }

        public int compare(java.lang.Object obj, java.lang.Object obj1) {
            int result = compareAsc((GenericEntity) obj, (GenericEntity) obj1);

            if (descending && result != 0) {
                result = -result;
            }
            if ((result == 0) && (next != null)) {
                return next.compare(obj, obj1);
            } else {
                return result;
            }
        }

        private int compareAsc(GenericEntity obj, GenericEntity obj2) {
            if (this.modelField == null) {
                this.modelField = obj.getModelEntity().getField(field);
                if (this.modelField == null) {
                    throw new IllegalArgumentException("The field " + field + " could not be found in the entity " + obj.getEntityName());
                }
            }
            Object value = obj.dangerousGetNoCheckButFast(this.modelField);
            Object value2 = obj2.dangerousGetNoCheckButFast(this.modelField);

            // null is defined as the largest possible value
            if (value == null) return value2 == null ? 0 : 1;
            if (value2 == null) return value == null ? 0 : -1;
            int result = ((Comparable) value).compareTo(value2);

            // if (Debug.infoOn()) Debug.logInfo("[OrderByComparator.compareAsc] Result is " + result + " for [" + value + "] and [" + value2 + "]");
            return result;
        }

        public boolean equals(java.lang.Object obj) {
            if ((obj != null) && (obj instanceof OrderByComparator)) {
                OrderByComparator that = (OrderByComparator) obj;

                return this.field.equals(that.field) && (this.descending == that.descending)
                    && UtilValidate.areEqual(this.next, that.next);
            } else {
                return false;
            }
        }
    }
}
