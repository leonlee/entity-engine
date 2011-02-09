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

import java.sql.Timestamp;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * MapComparator.java
 * 
 * @author     <a href="mailto:jaz@ofbiz.org">Andy Zeneski</a>
 * @created    Oct 14, 2002
 * @version    2.0
 */
public class MapComparator<K> implements Comparator<Map<K, ?>> {

    private List<? extends K> keys;

    /**
     * Method MapComparator.
     * @param keys List of Map keys to sort on
     */
    public MapComparator(List<? extends K> keys) {
        this.keys = keys;
    }

    public int compare(Map<K, ?> map1, Map<K, ?> map2) {

        if (keys == null || keys.size() < 1)
            throw new IllegalArgumentException("No sort fields defined");

        for (K key : keys) {
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
                        Date d1 = (Date) o1;
                        Date d2 = (Date) o2;
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

    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object obj) {
        return obj.equals(this);
    }
    
    private boolean testValue(Map<K, ?> map, K key) {
        if (!map.containsKey(key))
            return false;
        if (map.get(key) == null)
            return false;
        return true;
    }    
}
