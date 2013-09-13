/*
 * $Id: EntityOperator.java,v 1.3 2005/05/27 06:39:03 amazkovoi Exp $
 *
 *  Copyright (c) 2002 The Open For Business Project - www.ofbiz.org
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a
 *  copy of this software and associated documentation files (the "Software"),
 *  to deal in the Software without restriction, including without limitation
 *  the rights to use, copy, modify, merge, publish, distribute, sublicense,
 *  and/or sell copies of the Software, and to permit persons to whom the
 *  Software is furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included
 *  in all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 *  OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 *  IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 *  CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT
 *  OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 *  THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package org.ofbiz.core.entity;


/**
 * Encapsulates operations between entities and entity fields. This is a immutable class.
 *
 *@author     <a href='mailto:chris_maurer@altavista.com'>Chris Maurer</a>
 *@author     <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 *@author     <a href="mailto:jaz@ofbiz.org">Andy Zeneski</a>
 *@created    Nov 5, 2001
 *@version    1.0
 */
public class EntityOperator implements java.io.Serializable {

    public static final int ID_EQUALS = 1;
    public static final int ID_NOT_EQUAL = 2;
    public static final int ID_LESS_THAN = 3;
    public static final int ID_GREATER_THAN = 4;
    public static final int ID_LESS_THAN_EQUAL_TO = 5;
    public static final int ID_GREATER_THAN_EQUAL_TO = 6;
    public static final int ID_IN = 7;
    public static final int ID_BETWEEN = 8;
    public static final int ID_NOT = 9;
    public static final int ID_AND = 10;
    public static final int ID_OR = 11;
    public static final int ID_LIKE = 12;

    public static final EntityOperator EQUALS = new EntityOperator(ID_EQUALS, "=")
    {
        public boolean compare(Object o1, Object o2)
        {
            return ((o1 == null && o2 == null) || (o1 != null && o1.equals(o2)));
        }
    };
    public static final EntityOperator NOT_EQUAL = new EntityOperator(ID_NOT_EQUAL, "<>")
    {
        public boolean compare(Object o1, Object o2)
        {
            return !((o1 == null && o2 == null) || (o1 != null && o1.equals(o2)));
        }
    };
    public static final EntityOperator LESS_THAN = new EntityOperator(ID_LESS_THAN, "<")
    {
        public boolean compare(Object o1, Object o2)
        {
            Comparable c1 = null;
            if (o1 != null && o1 instanceof Comparable<?>)
            {
                c1 = (Comparable) o1;
                return c1.compareTo(o1) < 0;
            }
            else
                return false;
        }
    };
    public static final EntityOperator GREATER_THAN = new EntityOperator(ID_GREATER_THAN, ">")
    {
        public boolean compare(Object o1, Object o2)
        {
            Comparable c1 = null;
            if (o1 != null && o1 instanceof Comparable)
                c1 = (Comparable) o1;

            return c1.compareTo(o1) > 0;
        }
    };
    public static final EntityOperator LESS_THAN_EQUAL_TO = new EntityOperator(ID_LESS_THAN_EQUAL_TO, "<=")
    {
        public boolean compare(Object o1, Object o2)
        {
            return !(GREATER_THAN.compare(o1,o2));
        }
    };
    public static final EntityOperator GREATER_THAN_EQUAL_TO = new EntityOperator(ID_GREATER_THAN_EQUAL_TO, ">=")
    {
        public boolean compare(Object o1, Object o2)
        {
            return !(LESS_THAN.compare(o1,o2));
        }
    };
    public static final EntityOperator IN = new EntityOperator(ID_IN, "IN");
    public static final EntityOperator BETWEEN = new EntityOperator(ID_BETWEEN, "BETWEEN");
    public static final EntityOperator NOT = new EntityOperator(ID_NOT, "NOT");
    public static final EntityOperator AND = new EntityOperator(ID_AND, "AND");
    public static final EntityOperator OR = new EntityOperator(ID_OR, "OR");
    public static final EntityOperator LIKE = new EntityOperator(ID_LIKE, "LIKE")
    {
        public boolean compare(Object o1, Object o2)
        {
            if (o1 == null)
            {
                if (o2 == null)
                {
                    // Should not happen very often as using the like operator to compare nulls is not a good way to go
                    return true;
                }
                else
                {
                    return false;
                }
            }
            else
            {
                if (o2 == null)
                {
                    return false;
                }
                else
                {
                    String s1 = (String) o1;
                    String s2 = (String) o2;
                    // Not a good way of doing it as it ignores the '%' inside the match.
                    // Should do for now :(
                    return s1.contains(s2);
                }
            }
        }
    };

    private int idInt;
    private String codeString;

    public EntityOperator(int id, String code) {
        idInt = id;
        codeString = code;
    }

    public String getCode() {
        if (codeString == null)
            return "null";
        else
            return codeString;
    }

    public int getId() {
        return idInt;
    }

    public String toString() {
        return codeString;
    }

    public boolean compare(Object o1, Object o2)
    {
        throw new UnsupportedOperationException("This class does not implement compare as is has no meaning out side of sql");
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof EntityOperator)) {
            return false;
        }
        final EntityOperator otherOper = (EntityOperator) obj;
        return this.idInt == otherOper.getId();
    }
}
