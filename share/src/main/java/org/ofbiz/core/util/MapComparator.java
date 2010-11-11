/*
 * $Id: MapComparator.java,v 1.1 2005/04/01 05:58:06 sfarquhar Exp $
 *
 *  Copyright (c) 2001, 2002 The Open For Business Project - www.ofbiz.org
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
 
package org.ofbiz.core.util;

import java.sql.*;
import java.util.*;

import org.ofbiz.core.util.*;

/**
 * MapComparator.java
 * 
 * @author     <a href="mailto:jaz@ofbiz.org">Andy Zeneski</a>
 * @created    Oct 14, 2002
 * @version    2.0
 */
public class MapComparator implements Comparator {

    private List keys;

    /**
     * Method MapComparator.
     * @param keys List of Map keys to sort on
     */
    public MapComparator(List keys) {
        this.keys = keys;
    }

    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object obj) {
        return obj.equals(this);
    }

    /**
     * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
     */
    public int compare(Object obj1, Object obj2) {
        Map map1, map2;
        try {
            map1 = (Map) obj1;
            map2 = (Map) obj2;
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("Objects not from the Map interface");
        }

        if (keys == null || keys.size() < 1)
            throw new IllegalArgumentException("No sort fields defined");

        Iterator i = keys.iterator();
        while (i.hasNext()) {
            Object key = i.next();
            if (testValue(map1, key) && !testValue(map2, key))
                return -1;
            if (!testValue(map1, key) && testValue(map2, key))
                return 1;
            if (!testValue(map1, key) && !testValue(map2, key))
                continue;

            Object o1 = map1.get(key);
            Object o2 = map2.get(key);
            try {
                if (!o1.equals(o2)) {
                    if (ObjectType.instanceOf(o1, "java.lang.String")) {
                        String s1 = (String) o1;
                        String s2 = (String) o2;
                        if (!s1.equals(s2))
                            return s1.compareTo(s2);
                    } else if (ObjectType.instanceOf(o1, "java.lang.Integer")) {
                        Integer i1 = (Integer) o1;
                        Integer i2 = (Integer) o2;
                        if (!i1.equals(i2))
                            return i1.compareTo(i2);
                    } else if (ObjectType.instanceOf(o1, "java.lang.Double")) {
                        Double d1 = (Double) o1;
                        Double d2 = (Double) o2;
                        if (!d1.equals(d2))
                            return d1.compareTo(d2);
                    } else if (ObjectType.instanceOf(o1, "java.lang.Float")) {
                        Float f1 = (Float) o1;
                        Float f2 = (Float) o2;
                        if (!f1.equals(f2))
                            return f1.compareTo(f2);
                    } else if (ObjectType.instanceOf(o1, "java.sql.Timestamp")) {
                        Timestamp t1 = (Timestamp) o1;
                        Timestamp t2 = (Timestamp) o2;
                        if (!t1.equals(t2))
                            return t1.compareTo(t2);
                    } else if (ObjectType.instanceOf(o1, "java.util.Date")) {
                        java.util.Date d1 = (java.util.Date) o1;
                        java.util.Date d2 = (java.util.Date) o2;
                        if (!d1.equals(d2))
                            return d1.compareTo(d2);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e.getMessage());
            }
        }
        return 0;
    }
    
    private boolean testValue(Map map, Object key) {
        if (!map.containsKey(key))
            return false;
        if (map.get(key) == null)
            return false;
        return true;
    }    
}
