/*
 * $Id: UtilMisc.java,v 1.1 2005/04/01 05:58:06 sfarquhar Exp $
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * UtilMisc - Misc Utility Functions
 *
 * @author <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 * @author <a href="mailto:jaz@ofbiz.org">Andy Zeneski</a>
 * @version $Revision: 1.1 $
 * @since 2.0
 */
public class UtilMisc {

    /**
     * Get an iterator from a collection, returning null if collection is null
     *
     * @param col The collection to be turned in to an iterator
     * @return The resulting Iterator
     */
    public static <T> Iterator<T> toIterator(Collection<T> col) {
        if (col == null)
            return null;
        else
            return col.iterator();
    }

    /**
     * Create a map from passed nameX, valueX parameters
     *
     * @return The resulting Map
     */
    public static Map<String, Object> toMap(String name1, Object value1) {
        return new UtilMisc.SimpleMap(name1, value1);

        /* Map fields = new HashMap();
         fields.put(name1, value1);
         return fields;*/
    }

    /**
     * Create a map from passed nameX, valueX parameters
     *
     * @return The resulting Map
     */
    public static Map<String, Object> toMap(String name1, Object value1, String name2, Object value2) {
        return new UtilMisc.SimpleMap(name1, value1, name2, value2);

        /* Map fields = new HashMap();
         fields.put(name1, value1);
         fields.put(name2, value2);
         return fields;*/
    }

    /**
     * Create a map from passed nameX, valueX parameters
     *
     * @return The resulting Map
     */
    public static Map<String, Object> toMap(String name1, Object value1, String name2, Object value2, String name3, Object value3) {
        return new UtilMisc.SimpleMap(name1, value1, name2, value2, name3, value3);

        /* Map fields = new HashMap();
         fields.put(name1, value1);
         fields.put(name2, value2);
         fields.put(name3, value3);
         return fields;*/
    }

    /**
     * Create a map from passed nameX, valueX parameters
     *
     * @return The resulting Map
     */
    public static Map<String, Object> toMap(String name1, Object value1, String name2, Object value2, String name3,
                                            Object value3, String name4, Object value4) {
        return new UtilMisc.SimpleMap(name1, value1, name2, value2, name3, value3, name4, value4);

        /* Map fields = new HashMap();
         fields.put(name1, value1);
         fields.put(name2, value2);
         fields.put(name3, value3);
         fields.put(name4, value4);
         return fields;*/
    }

    /**
     * Create a map from passed nameX, valueX parameters
     *
     * @return The resulting Map
     */
    public static Map<String, Object> toMap(String name1, Object value1, String name2, Object value2, String name3, Object value3,
                                            String name4, Object value4, String name5, Object value5) {
        Map<String, Object> fields = new HashMap<String, Object>();

        fields.put(name1, value1);
        fields.put(name2, value2);
        fields.put(name3, value3);
        fields.put(name4, value4);
        fields.put(name5, value5);
        return fields;
    }

    /**
     * Create a map from passed nameX, valueX parameters
     *
     * @return The resulting Map
     */
    public static Map<String, Object> toMap(String name1, Object value1, String name2, Object value2, String name3, Object value3,
                                            String name4, Object value4, String name5, Object value5, String name6, Object value6) {
        Map<String, Object> fields = new HashMap<String, Object>();

        fields.put(name1, value1);
        fields.put(name2, value2);
        fields.put(name3, value3);
        fields.put(name4, value4);
        fields.put(name5, value5);
        fields.put(name6, value6);
        return fields;
    }

    /**
     * Sort a List of Maps by specified consistent keys.
     *
     * @param listOfMaps List of Map objects to sort.
     * @param sortKeys   List of Map keys to sort by.
     * @return a new List of sorted Maps.
     */
    public static <K, V> List<Map<K, V>> sortMaps(List<Map<K, V>> listOfMaps, List<? extends K> sortKeys) {
        if (listOfMaps == null || sortKeys == null)
            return null;
        List<Map<K, V>> toSort = new LinkedList<Map<K, V>>(listOfMaps);
        try {
            MapComparator<K> mc = new MapComparator<K>(sortKeys);
            Collections.sort(toSort, mc);
        } catch (Exception e) {
            Debug.logError(e, "Problems sorting list of maps; returning null.");
            return null;
        }
        return toSort;
    }

    /**
     * Create a list from passed objX parameters
     *
     * @return The resulting List
     */
    public static <T> List<T> toList(T obj1) {
        List<T> list = new ArrayList<T>(1);

        list.add(obj1);
        return list;
    }

    /**
     * Create a list from passed objX parameters
     *
     * @return The resulting List
     */
    public static <T> List<T> toList(T obj1, T obj2) {
        List<T> list = new ArrayList<T>(2);

        list.add(obj1);
        list.add(obj2);
        return list;
    }

    /**
     * Create a list from passed objX parameters
     *
     * @return The resulting List
     */
    public static <T> List<T> toList(T obj1, T obj2, T obj3) {
        List<T> list = new ArrayList<T>(3);

        list.add(obj1);
        list.add(obj2);
        list.add(obj3);
        return list;
    }

    /**
     * Create a list from passed objX parameters
     *
     * @return The resulting List
     */
    public static <T> List<T> toList(T obj1, T obj2, T obj3, T obj4) {
        List<T> list = new ArrayList<T>(4);

        list.add(obj1);
        list.add(obj2);
        list.add(obj3);
        list.add(obj4);
        return list;
    }

    /**
     * Create a list from passed objX parameters
     *
     * @return The resulting List
     */
    public static <T> List<T> toList(T obj1, T obj2, T obj3, T obj4, T obj5) {
        List<T> list = new ArrayList<T>(5);

        list.add(obj1);
        list.add(obj2);
        list.add(obj3);
        list.add(obj4);
        list.add(obj5);
        return list;
    }

    /**
     * Create a list from passed objX parameters
     *
     * @return The resulting List
     */
    public static <T> List<T> toList(T obj1, T obj2, T obj3, T obj4, T obj5, T obj6) {
        List<T> list = new ArrayList<T>(6);

        list.add(obj1);
        list.add(obj2);
        list.add(obj3);
        list.add(obj4);
        list.add(obj5);
        list.add(obj6);
        return list;
    }

    public static <T> List<T> toList(Collection<T> collection) {
        if (collection == null) return null;
        if (collection instanceof List) {
            return (List<T>) collection;
        } else {
            return new ArrayList<T>(collection);
        }
    }

    /**
     * Parse a locale string Locale object
     *
     * @param localeString The locale string (en_US)
     * @return Locale The new Locale object
     */
    public static Locale parseLocale(String localeString) {
        if (localeString == null || localeString.length() == 0)
            return null;

        List<String> splitList = StringUtil.split(localeString, "_");
        if (splitList.size() != 2)
            return null;

        String language = splitList.get(0);
        String country = splitList.get(1);

        return new Locale(language, country);
    }

    /**
     * This is meant to be very quick to create and use for small sized maps, perfect for how we usually use UtilMisc.toMap
     */
    protected static class SimpleMap implements Map<String, Object>, java.io.Serializable {
        protected Map<String, Object> realMapIfNeeded = null;

        String[] names;
        Object[] values;

        public SimpleMap() {
            names = new String[0];
            values = new Object[0];
        }

        public SimpleMap(String name1, Object value1) {
            names = new String[1];
            values = new Object[1];
            this.names[0] = name1;
            this.values[0] = value1;
        }

        public SimpleMap(String name1, Object value1, String name2, Object value2) {
            names = new String[2];
            values = new Object[2];
            this.names[0] = name1;
            this.values[0] = value1;
            this.names[1] = name2;
            this.values[1] = value2;
        }

        public SimpleMap(String name1, Object value1, String name2, Object value2, String name3, Object value3) {
            names = new String[3];
            values = new Object[3];
            this.names[0] = name1;
            this.values[0] = value1;
            this.names[1] = name2;
            this.values[1] = value2;
            this.names[2] = name3;
            this.values[2] = value3;
        }

        public SimpleMap(String name1, Object value1, String name2, Object value2, String name3, Object value3, String name4, Object value4) {
            names = new String[4];
            values = new Object[4];
            this.names[0] = name1;
            this.values[0] = value1;
            this.names[1] = name2;
            this.values[1] = value2;
            this.names[2] = name3;
            this.values[2] = value3;
            this.names[3] = name4;
            this.values[3] = value4;
        }

        protected void makeRealMap() {
            realMapIfNeeded = new HashMap<String, Object>();
            for (int i = 0; i < names.length; i++) {
                realMapIfNeeded.put(names[i], values[i]);
            }
            this.names = null;
            this.values = null;
        }

        public void clear() {
            if (realMapIfNeeded != null) {
                realMapIfNeeded.clear();
            } else {
                realMapIfNeeded = new HashMap<String, Object>();
                names = null;
                values = null;
            }
        }

        public boolean containsKey(Object obj) {
            if (realMapIfNeeded != null) {
                return realMapIfNeeded.containsKey(obj);
            } else {
                for (String name : names) {
                    if (obj == null && name == null) return true;
                    if (name != null && name.equals(obj)) return true;
                }
                return false;
            }
        }

        public boolean containsValue(Object obj) {
            if (realMapIfNeeded != null) {
                return realMapIfNeeded.containsValue(obj);
            } else {
                for (int i = 0; i < names.length; i++) {
                    if (obj == null && values[i] == null) return true;
                    if (values[i] != null && values[i].equals(obj)) return true;
                }
                return false;
            }
        }

        public java.util.Set<Map.Entry<String, Object>> entrySet() {
            if (realMapIfNeeded != null) {
                return realMapIfNeeded.entrySet();
            } else {
                this.makeRealMap();
                return realMapIfNeeded.entrySet();
            }
        }

        public Object get(Object obj) {
            if (realMapIfNeeded != null) {
                return realMapIfNeeded.get(obj);
            } else {
                for (int i = 0; i < names.length; i++) {
                    if (obj == null && names[i] == null) return values[i];
                    if (names[i] != null && names[i].equals(obj)) return values[i];
                }
                return null;
            }
        }

        public boolean isEmpty() {
            if (realMapIfNeeded != null) {
                return realMapIfNeeded.isEmpty();
            } else {
                if (this.names.length == 0) return true;
                return false;
            }
        }

        public java.util.Set<String> keySet() {
            if (realMapIfNeeded != null) {
                return realMapIfNeeded.keySet();
            } else {
                this.makeRealMap();
                return realMapIfNeeded.keySet();
            }
        }

        public Object put(String obj, Object obj1) {
            if (realMapIfNeeded != null) {
                return realMapIfNeeded.put(obj, obj1);
            } else {
                this.makeRealMap();
                return realMapIfNeeded.put(obj, obj1);
            }
        }

        public void putAll(java.util.Map<? extends String, ?> map) {
            if (realMapIfNeeded != null) {
                realMapIfNeeded.putAll(map);
            } else {
                this.makeRealMap();
                realMapIfNeeded.putAll(map);
            }
        }

        public Object remove(Object obj) {
            if (realMapIfNeeded != null) {
                return realMapIfNeeded.remove(obj);
            } else {
                this.makeRealMap();
                return realMapIfNeeded.remove(obj);
            }
        }

        public int size() {
            if (realMapIfNeeded != null) {
                return realMapIfNeeded.size();
            } else {
                return this.names.length;
            }
        }

        public java.util.Collection<Object> values() {
            if (realMapIfNeeded != null) {
                return realMapIfNeeded.values();
            } else {
                this.makeRealMap();
                return realMapIfNeeded.values();
            }
        }

        public String toString() {
            if (realMapIfNeeded != null) {
                return realMapIfNeeded.toString();
            } else {
                StringBuilder outString = new StringBuilder("{");
                for (int i = 0; i < names.length; i++) {
                    if (i > 0) outString.append(',');
                    outString.append('{');
                    outString.append(names[i]);
                    outString.append(',');
                    outString.append(values[i]);
                    outString.append('}');
                }
                outString.append('}');
                return outString.toString();
            }
        }

        public int hashCode() {
            if (realMapIfNeeded != null) {
                return realMapIfNeeded.hashCode();
            } else {
                int hashCode = 0;
                for (int i = 0; i < names.length; i++) {
                    //note that this calculation is done based on the calc specified in the Java java.util.Map interface
                    int tempNum = (names[i] == null ? 0 : names[i].hashCode()) ^
                            (values[i] == null ? 0 : values[i].hashCode());
                    hashCode += tempNum;
                }
                return hashCode;
            }
        }

        public boolean equals(Object obj) {
            if (realMapIfNeeded != null) {
                return realMapIfNeeded.equals(obj);
            } else {
                Map<?, ?> mapObj = (Map<?, ?>) obj;

                //first check the size
                if (mapObj.size() != names.length) return false;

                //okay, same size, now check each entry
                for (int i = 0; i < names.length; i++) {
                    //first check the name
                    if (!mapObj.containsKey(names[i])) return false;

                    //if that passes, check the value
                    Object mapValue = mapObj.get(names[i]);
                    if (mapValue == null) {
                        if (values[i] != null) return false;
                    } else {
                        if (!mapValue.equals(values[i])) return false;
                    }
                }

                return true;
            }
        }
    }
}
