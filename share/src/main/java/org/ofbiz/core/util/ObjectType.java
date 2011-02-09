/*
 * $Id: ObjectType.java,v 1.1 2005/04/01 05:58:06 sfarquhar Exp $
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

import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Utilities for analyzing and converting Object types in Java - takes advantage of a lot of reflection and other stuff
 *
 * @author     <a href="mailto:jaz@ofbiz.org">Andy Zeneski</a>
 * @author     <a href="mailto:jonesde@ofbiz.org">David E. Jones</a> 
 * @author     <a href="mailto:gielen@aixcept.de">Rene Gielen</a> 
 * @version    $Revision: 1.1 $
 * @since      2.0
 */
public class ObjectType {
    
    public static final String module = ObjectType.class.getName();

    protected static Map<String, Class<?>> classCache = new HashMap<String, Class<?>>();

    public static final String LANG_PACKAGE = "java.lang."; // We will test both the raw value and this + raw value
    public static final String SQL_PACKAGE = "java.sql.";   // We will test both the raw value and this + raw value

    /** 
     * Loads a class with the current thread's context classloader
     * @param className The name of the class to load
     */
    public static Class<?> loadClass(String className) throws ClassNotFoundException {
        // small block to speed things up by putting using preloaded classes for common objects, this turns out to help quite a bit...
        Class<?> theClass = CachedClassLoader.globalClassNameClassMap.get(className);

        if (theClass != null) return theClass;

        return loadClass(className, null);
    }

    /** 
     * Loads a class with the current thread's context classloader
     * @param className The name of the class to load
     */
    public static Class<?> loadClass(String className, ClassLoader loader) throws ClassNotFoundException {
        // small block to speed things up by putting using preloaded classes for common objects, this turns out to help quite a bit...
        Class<?> theClass = CachedClassLoader.globalClassNameClassMap.get(className);

        if (theClass != null) return theClass;

        if (loader == null) loader = Thread.currentThread().getContextClassLoader();

        try {
            theClass = loader.loadClass(className);
        } catch (Exception e) {
            theClass = classCache.get(className);
            if (theClass == null) {
                synchronized (ObjectType.class) {
                    theClass = classCache.get(className);
                    if (theClass == null) {
                        theClass = Class.forName(className);
                        if (theClass != null) {
                            if (Debug.verboseOn()) Debug.logVerbose("Loaded Class: " + theClass.getName(), module);
                            classCache.put(className, theClass);
                        }
                    }
                }
            }
        }

        return theClass;
    }

    /** 
     * Returns an instance of the specified class
     * @param className Name of the class to instantiate
     */
    public static Object getInstance(String className) throws ClassNotFoundException,
            InstantiationException, IllegalAccessException {
        Class<?> c = loadClass(className);
        Object o = c.newInstance();

        if (Debug.verboseOn()) Debug.logVerbose("Instantiated object: " + o.toString(), module);
        return o;
    }

    /** 
     * Tests if an object properly implements the specified interface
     * @param obj Object to test
     * @param interfaceName Name of the interface to test against
     */
    public static boolean interfaceOf(Object obj, String interfaceName) throws ClassNotFoundException {
        Class<?> interfaceClass = loadClass(interfaceName);

        return interfaceOf(obj, interfaceClass);
    }

    /** 
     * Tests if an object properly implements the specified interface
     * @param obj Object to test
     * @param interfaceObject to test against
     */
    public static boolean interfaceOf(Object obj, Object interfaceObject) {
        Class<?> interfaceClass = interfaceObject.getClass();

        return interfaceOf(obj, interfaceClass);
    }

    /** 
     * Tests if an object properly implements the specified interface
     * @param obj Object to test
     * @param interfaceClass Class to test against
     */
    public static boolean interfaceOf(Object obj, Class<?> interfaceClass) {
        Class<?> objectClass = obj.getClass();

        while (objectClass != null) {
            Class<?>[] ifaces = objectClass.getInterfaces();
            for (Class<?> iface : ifaces) {
                if (iface == interfaceClass) return true;
            }
            objectClass = objectClass.getSuperclass();
        }
        return false;
    }

    /** 
     * Tests if an object is an instance of or a sub-class of the parent
     * @param obj Object to test
     * @param parentName Name of the parent class to test against
     */
    public static boolean isOrSubOf(Object obj, String parentName) throws ClassNotFoundException {
        Class<?> parentClass = loadClass(parentName);

        return isOrSubOf(obj, parentClass);
    }

    /** 
     * Tests if an object is an instance of or a sub-class of the parent
     * @param obj Object to test
     * @param parentObject Object to test against
     */
    public static boolean isOrSubOf(Object obj, Object parentObject) {
        Class<?> parentClass = parentObject.getClass();

        return isOrSubOf(obj, parentClass);
    }

    /** 
     * Tests if an object is an instance of or a sub-class of the parent
     * @param obj Object to test
     * @param parentClass Class to test against
     */
    public static boolean isOrSubOf(Object obj, Class<?> parentClass) {
        Class<?> objectClass = obj.getClass();

        while (objectClass != null) {
            if (objectClass == parentClass) return true;
            objectClass = objectClass.getSuperclass();
        }
        return false;
    }

    /** 
     * Tests if an object is an instance of a sub-class of or properly implements an interface
     * @param obj Object to test
     * @param typeObject Object to test against
     */
    public static boolean instanceOf(Object obj, Object typeObject) {
        Class<?> typeClass = typeObject.getClass();

        return instanceOf(obj, typeClass);
    }

    /** 
     * Tests if an object is an instance of a sub-class of or properly implements an interface
     * @param obj Object to test
     * @param typeObject Object to test against
     */
    public static boolean instanceOf(Object obj, String typeName) {
        return instanceOf(obj, typeName, null);
    }

    /** 
     * Tests if an object is an instance of a sub-class of or properly implements an interface
     * @param obj Object to test
     * @param typeObject Object to test against
     */
    public static boolean instanceOf(Object obj, String typeName, ClassLoader loader) {
        Class<?> infoClass = null;

        try {
            infoClass = ObjectType.loadClass(typeName, loader);
        } catch (SecurityException se1) {
            throw new IllegalArgumentException("Problems with classloader: security exception (" +
                    se1.getMessage() + ")");
        } catch (ClassNotFoundException e1) {
            try {
                infoClass = ObjectType.loadClass(LANG_PACKAGE + typeName, loader);
            } catch (SecurityException se2) {
                throw new IllegalArgumentException("Problems with classloader: security exception (" +
                        se2.getMessage() + ")");
            } catch (ClassNotFoundException e2) {
                try {
                    infoClass = ObjectType.loadClass(SQL_PACKAGE + typeName, loader);
                } catch (SecurityException se3) {
                    throw new IllegalArgumentException("Problems with classloader: security exception (" +
                            se3.getMessage() + ")");
                } catch (ClassNotFoundException e3) {
                    throw new IllegalArgumentException("Cannot find and load the class of type: " + typeName +
                            " or of type: " + LANG_PACKAGE + typeName + " or of type: " + SQL_PACKAGE + typeName +
                            ":  (" + e3.getMessage() + ")");
                }
            }
        }

        if (infoClass == null)
            throw new IllegalArgumentException("Illegal type found in info map (could not load class for specified type)");

        return instanceOf(obj, infoClass);
    }

    /** 
     * Tests if an object is an instance of a sub-class of or properly implements an interface
     * @param obj Object to test
     * @param typeClass Class to test against
     */
    public static boolean instanceOf(Object obj, Class<?> typeClass) {
        if (obj == null) return true;
        Class<?> objectClass = obj.getClass();

        if (typeClass.isInterface()) {
            return interfaceOf(obj, typeClass);
        } else {
            return isOrSubOf(obj, typeClass);
        }
    }

    /** 
     * Converts the passed object to the named simple type; supported types
     * include: String, Boolean, Double, Float, Long, Integer, Date (java.sql.Date),
     * Time, Timestamp;
     * @param obj Object to convert
     * @param type Name of type to convert to
     * @param format Optional (can be null) format string for Date, Time, Timestamp
     * @param locale Optional (can be null) Locale for formatting and parsing Double, Float, Long, Integer
     */
    public static Object simpleTypeConvert(Object obj, String type, String format, Locale locale) throws GeneralException {
        if (obj == null)
            return null;

        if ("PlainString".equals(type)) {
            return obj.toString();
        }
        if ("Object".equals(type)) {
            return obj;
        }

        String fromType = null;

        if (obj instanceof java.lang.String) {
            fromType = "String";
            String str = (String) obj;
            if ("String".equals(type) || "java.lang.String".equals(type)) {
                return obj;
            }
            if (str.length() == 0) {
                return null;
            }

            if ("Boolean".equals(type) || "java.lang.Boolean".equals(type)) {
                return str.equalsIgnoreCase("TRUE");
            } else if ("Locale".equals(type) || "java.util.Locale".equals(type)) {
                Locale loc = UtilMisc.parseLocale(str);
                if (loc != null) {
                    return loc;
                } else {
                    throw new GeneralException("Could not convert " + str + " to " + type + ": ");    
                }
            } else if ("Double".equals(type) || "java.lang.Double".equals(type)) {
                try {
                    NumberFormat nf = null;

                    if (locale == null)
                        nf = NumberFormat.getNumberInstance();
                    else
                        nf = NumberFormat.getNumberInstance(locale);
                    Number tempNum = nf.parse(str);

                    return tempNum.doubleValue();
                } catch (ParseException e) {
                    throw new GeneralException("Could not convert " + str + " to " + type + ": ", e);
                }
            } else if ("Float".equals(type) || "java.lang.Float".equals(type)) {
                try {
                    NumberFormat nf = null;

                    if (locale == null)
                        nf = NumberFormat.getNumberInstance();
                    else
                        nf = NumberFormat.getNumberInstance(locale);
                    Number tempNum = nf.parse(str);

                    return tempNum.floatValue();
                } catch (ParseException e) {
                    throw new GeneralException("Could not convert " + str + " to " + type + ": ", e);
                }
            } else if ("Long".equals(type) || "java.lang.Long".equals(type)) {
                try {
                    NumberFormat nf = null;

                    if (locale == null)
                        nf = NumberFormat.getNumberInstance();
                    else
                        nf = NumberFormat.getNumberInstance(locale);
                    nf.setMaximumFractionDigits(0);
                    Number tempNum = nf.parse(str);

                    return tempNum.longValue();
                } catch (ParseException e) {
                    throw new GeneralException("Could not convert " + str + " to " + type + ": ", e);
                }
            } else if ("Integer".equals(type) || "java.lang.Integer".equals(type)) {
                try {
                    NumberFormat nf = null;

                    if (locale == null)
                        nf = NumberFormat.getNumberInstance();
                    else
                        nf = NumberFormat.getNumberInstance(locale);
                    nf.setMaximumFractionDigits(0);
                    Number tempNum = nf.parse(str);

                    return tempNum.intValue();
                } catch (ParseException e) {
                    throw new GeneralException("Could not convert " + str + " to " + type + ": ", e);
                }
            } else if ("Date".equals(type) || "java.sql.Date".equals(type)) {
                if (format == null || format.length() == 0) {
                    try {
                        return java.sql.Date.valueOf(str);
                    } catch (Exception e) {
                        try {
                            DateFormat df = null;
                            if (locale != null) {
                                df = DateFormat.getDateInstance(DateFormat.SHORT, locale);
                            } else {
                                df = DateFormat.getDateInstance(DateFormat.SHORT);
                            }
                            Date fieldDate = df.parse(str);

                            return new java.sql.Date(fieldDate.getTime());
                        } catch (ParseException e1) {
                            throw new GeneralException("Could not convert " + str + " to " + type + ": ", e);
                        }
                    }
                } else {
                    try {
                        SimpleDateFormat sdf = new SimpleDateFormat(format);
                        java.util.Date fieldDate = sdf.parse(str);

                        return new java.sql.Date(fieldDate.getTime());
                    } catch (ParseException e) {
                        throw new GeneralException("Could not convert " + str + " to " + type + ": ", e);
                    }
                }
            } else if ("Time".equals(type) || "java.sql.Time".equals(type)) {
                if (format == null || format.length() == 0) {
                    try {
                        return java.sql.Time.valueOf(str);
                    } catch (Exception e) {
                        try {
                            DateFormat df = null;
                            if (locale != null) {
                                df = DateFormat.getTimeInstance(DateFormat.SHORT, locale);
                            } else {
                                df = DateFormat.getTimeInstance(DateFormat.SHORT);
                            }
                            Date fieldDate = df.parse(str);

                            return new java.sql.Time(fieldDate.getTime());
                        } catch (ParseException e1) {
                            throw new GeneralException("Could not convert " + str + " to " + type + ": ", e);
                        }
                    }
                } else {
                    try {
                        SimpleDateFormat sdf = new SimpleDateFormat(format);
                        java.util.Date fieldDate = sdf.parse(str);

                        return new java.sql.Time(fieldDate.getTime());
                    } catch (ParseException e) {
                        throw new GeneralException("Could not convert " + str + " to " + type + ": ", e);
                    }
                }
            } else if ("Timestamp".equals(type) || "java.sql.Timestamp".equals(type)) {
                if (format == null || format.length() == 0) {
                    try {
                        return java.sql.Timestamp.valueOf(str);
                    } catch (Exception e) {
                        try {
                            DateFormat df = null;
                            if (locale != null) {
                                df = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, locale);
                            } else {
                                df = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
                            }
                            Date fieldDate = df.parse(str);

                            return new java.sql.Timestamp(fieldDate.getTime());
                        } catch (ParseException e1) {
                            throw new GeneralException("Could not convert " + str + " to " + type + ": ", e);
                        }
                    }
                } else {
                    try {
                        SimpleDateFormat sdf = new SimpleDateFormat(format);
                        java.util.Date fieldDate = sdf.parse(str);

                        return new java.sql.Timestamp(fieldDate.getTime());
                    } catch (ParseException e) {
                        throw new GeneralException("Could not convert " + str + " to " + type + ": ", e);
                    }
                }
            } else {
                throw new GeneralException("Conversion from " + fromType + " to " + type + " not currently supported");
            }
        } else if (obj instanceof java.lang.Double) {
            fromType = "Double";
            Double dbl = (Double) obj;

            if ("String".equals(type) || "java.lang.String".equals(type)) {
                NumberFormat nf = null;

                if (locale == null)
                    nf = NumberFormat.getNumberInstance();
                else
                    nf = NumberFormat.getNumberInstance(locale);
                return nf.format(dbl.doubleValue());
            } else if ("Double".equals(type) || "java.lang.Double".equals(type)) {
                return obj;
            } else if ("Float".equals(type) || "java.lang.Float".equals(type)) {
                return dbl.floatValue();
            } else if ("Long".equals(type) || "java.lang.Long".equals(type)) {
                return Math.round(dbl);
            } else if ("Integer".equals(type) || "java.lang.Integer".equals(type)) {
                return (int) Math.round(dbl);
            } else {
                throw new GeneralException("Conversion from " + fromType + " to " + type + " not currently supported");
            }
        } else if (obj instanceof java.lang.Float) {
            fromType = "Float";
            Float flt = (Float) obj;

            if ("String".equals(type)) {
                NumberFormat nf = null;

                if (locale == null)
                    nf = NumberFormat.getNumberInstance();
                else
                    nf = NumberFormat.getNumberInstance(locale);
                return nf.format(flt.doubleValue());
            } else if ("Double".equals(type)) {
                return flt.doubleValue();
            } else if ("Float".equals(type)) {
                return obj;
            } else if ("Long".equals(type)) {
                return Math.round(flt.doubleValue());
            } else if ("Integer".equals(type)) {
                return (int) Math.round(flt.doubleValue());
            } else {
                throw new GeneralException("Conversion from " + fromType + " to " + type + " not currently supported");
            }
        } else if (obj instanceof java.lang.Long) {
            fromType = "Long";
            Long lng = (Long) obj;

            if ("String".equals(type) || "java.lang.String".equals(type)) {
                NumberFormat nf = null;

                if (locale == null)
                    nf = NumberFormat.getNumberInstance();
                else
                    nf = NumberFormat.getNumberInstance(locale);
                return nf.format(lng.longValue());
            } else if ("Double".equals(type) || "java.lang.Double".equals(type)) {
                return lng.doubleValue();
            } else if ("Float".equals(type) || "java.lang.Float".equals(type)) {
                return lng.floatValue();
            } else if ("Long".equals(type) || "java.lang.Long".equals(type)) {
                return obj;
            } else if ("Integer".equals(type) || "java.lang.Integer".equals(type)) {
                return lng.intValue();
            } else {
                throw new GeneralException("Conversion from " + fromType + " to " + type + " not currently supported");
            }
        } else if (obj instanceof java.lang.Integer) {
            fromType = "Integer";
            Integer intgr = (Integer) obj;

            if ("String".equals(type) || "java.lang.String".equals(type)) {
                NumberFormat nf = null;

                if (locale == null)
                    nf = NumberFormat.getNumberInstance();
                else
                    nf = NumberFormat.getNumberInstance(locale);
                return nf.format(intgr.longValue());
            } else if ("Double".equals(type) || "java.lang.Double".equals(type)) {
                return intgr.doubleValue();
            } else if ("Float".equals(type) || "java.lang.Float".equals(type)) {
                return intgr.floatValue();
            } else if ("Long".equals(type) || "java.lang.Long".equals(type)) {
                return intgr.longValue();
            } else if ("Integer".equals(type) || "java.lang.Integer".equals(type)) {
                return obj;
            } else {
                throw new GeneralException("Conversion from " + fromType + " to " + type + " not currently supported");
            }
        } else if (obj instanceof java.sql.Date) {
            fromType = "Date";
            java.sql.Date dte = (java.sql.Date) obj;

            if ("String".equals(type) || "java.lang.String".equals(type)) {
                if (format == null || format.length() == 0) {
                    return dte.toString();
                } else {
                    SimpleDateFormat sdf = new SimpleDateFormat(format);

                    return sdf.format(new java.util.Date(dte.getTime()));
                }
            } else if ("Date".equals(type) || "java.sql.Date".equals(type)) {
                return obj;
            } else if ("Time".equals(type) || "java.sql.Time".equals(type)) {
                throw new GeneralException("Conversion from " + fromType + " to " + type + " not currently supported");
            } else if ("Timestamp".equals(type) || "java.sql.Timestamp".equals(type)) {
                return new java.sql.Timestamp(dte.getTime());
            } else {
                throw new GeneralException("Conversion from " + fromType + " to " + type + " not currently supported");
            }
        } else if (obj instanceof java.sql.Time) {
            fromType = "Time";
            java.sql.Time tme = (java.sql.Time) obj;

            if ("String".equals(type) || "java.lang.String".equals(type)) {
                if (format == null || format.length() == 0) {
                    return tme.toString();
                } else {
                    SimpleDateFormat sdf = new SimpleDateFormat(format);

                    return sdf.format(new java.util.Date(tme.getTime()));
                }
            } else if ("Date".equals(type) || "java.sql.Date".equals(type)) {
                throw new GeneralException("Conversion from " + fromType + " to " + type + " not currently supported");
            } else if ("Time".equals(type) || "java.sql.Time".equals(type)) {
                return obj;
            } else if ("Timestamp".equals(type) || "java.sql.Timestamp".equals(type)) {
                return new java.sql.Timestamp(tme.getTime());
            } else {
                throw new GeneralException("Conversion from " + fromType + " to " + type + " not currently supported");
            }
        } else if (obj instanceof java.sql.Timestamp) {
            fromType = "Timestamp";
            java.sql.Timestamp tme = (java.sql.Timestamp) obj;

            if ("String".equals(type) || "java.lang.String".equals(type)) {
                if (format == null || format.length() == 0) {
                    return tme.toString();
                } else {
                    SimpleDateFormat sdf = new SimpleDateFormat(format);

                    return sdf.format(new java.util.Date(tme.getTime()));
                }
            } else if ("Date".equals(type) || "java.sql.Date".equals(type)) {
                return new java.sql.Date(tme.getTime());
            } else if ("Time".equals(type) || "java.sql.Time".equals(type)) {
                return new java.sql.Time(tme.getTime());
            } else if ("Timestamp".equals(type) || "java.sql.Timestamp".equals(type)) {
                return obj;
            } else {
                throw new GeneralException("Conversion from " + fromType + " to " + type + " not currently supported");
            }
        } else if (obj instanceof java.lang.Boolean) {
            fromType = "Boolean";
            Boolean bol = (Boolean) obj;
            if ("Boolean".equals(type) || "java.lang.Boolean".equals(type)) {
                return bol;
            } else if ("String".equals(type) || "java.lang.String".equals(type)) {
                return bol.toString();
            } else if ("Integer".equals(type) || "java.lang.Integer".equals(type)) {
                if (bol)
                    return 1;
                else
                    return 0;
            } else { 
                throw new GeneralException("Conversion from " + fromType + " to " + type + " not currently supported");
            }
        } else if (obj instanceof java.util.Locale) {
            fromType = "Locale";
            Locale loc = (Locale) obj;
            if ("Locale".equals(type) || "java.util.Locale".equals(type)) {
                return loc;
            } else if ("String".equals(type) || "java.lang.String".equals(type)) {
                return loc.toString();
            } else {
                throw new GeneralException("Conversion from " + fromType + " to " + type + " not currently supported");
            }
        } else {
            throw new GeneralException("Conversion from " + obj.getClass().getName() + " to " + type + " not currently supported");
        }
    }

    public static Boolean doRealCompare(Object value1, Object value2, String operator, String type, String format,
        List<String> messages, Locale locale, ClassLoader loader) {
        boolean verboseOn = Debug.verboseOn();

        if (verboseOn) Debug.logVerbose("Comparing value1: \"" + value1 + "\" " + operator + " value2:\"" + value2 + "\"");

        int result = 0;

        Object convertedValue1 = null;

        try {
            convertedValue1 = ObjectType.simpleTypeConvert(value1, type, format, locale);
        } catch (GeneralException e) {
            messages.add("Could not convert value1 for comparison: " + e.getMessage());
            return null;
        }

        Object convertedValue2 = null;

        if (value2 != null) {
            try {
                convertedValue2 = ObjectType.simpleTypeConvert(value2, type, format, locale);
            } catch (GeneralException e) {
                messages.add("Could not convert value2 for comparison: " + e.getMessage());
                return null;
            }
        }

        if (convertedValue1 == null && !"is-not-empty".equals(operator) && !"is-empty".equals(operator)) {
            if (verboseOn) Debug.logVerbose("Value1 was null, cannot complete comparison");
            return null;
        }
        if (convertedValue2 == null && !"is-not-empty".equals(operator) && !"is-empty".equals(operator)) {
            if (verboseOn) Debug.logVerbose("Value2 was null, cannot complete comparison");
            return null;
        }

        if ("contains".equals(operator)) {
            if (!"String".equals(type) && !"PlainString".equals(type)) {
                messages.add("Error in XML file: cannot do a contains compare with a non-String type");
                return null;
            }

            String str1 = (String) convertedValue1;
            String str2 = (String) convertedValue2;

            if (!str1.contains(str2)) {
                return Boolean.FALSE;
            }
        }

        if ("is-empty".equals(operator)) {
            if (value1 == null)
                return Boolean.TRUE;
            if (value1 instanceof String && ((String) value1).length() == 0)
                return Boolean.TRUE;
            if (value1 instanceof List && ((List<?>) value1).size() == 0)
                return Boolean.TRUE;
            if (value1 instanceof Map && ((Map<?,?>) value1).size() == 0)
                return Boolean.TRUE;
            return Boolean.FALSE;    
        }
        
        if ("is-not-empty".equals(operator)) {
            if (value1 == null)
                return Boolean.FALSE;
            if (value1 instanceof String && ((String) value1).length() == 0)
                return Boolean.FALSE;
            if (value1 instanceof List && ((List<?>) value1).size() == 0)
                return Boolean.FALSE;
            if (value1 instanceof Map && ((Map<?,?>) value1).size() == 0)
                return Boolean.FALSE;
            return Boolean.TRUE;    
        }
        
        if ("String".equals(type) || "PlainString".equals(type)) {
            String str1 = (String) convertedValue1;
            String str2 = (String) convertedValue2;

            if (str1.length() == 0 || str2.length() == 0) {
                return null;
            }
            result = str1.compareTo(str2);
        } else if ("Double".equals(type) || "Float".equals(type) || "Long".equals(type) || "Integer".equals(type)) {
            Number tempNum = (Number) convertedValue1;
            double value1Double = tempNum.doubleValue();

            tempNum = (Number) convertedValue2;
            double value2Double = tempNum.doubleValue();

            if (value1Double < value2Double)
                result = -1;
            else if (value1Double > value2Double)
                result = 1;
            else
                result = 0;
        } else if ("Date".equals(type)) {
            java.sql.Date value1Date = (java.sql.Date) convertedValue1;
            java.sql.Date value2Date = (java.sql.Date) convertedValue2;
            result = value1Date.compareTo(value2Date);
        } else if ("Time".equals(type)) {
            java.sql.Time value1Time = (java.sql.Time) convertedValue1;
            java.sql.Time value2Time = (java.sql.Time) convertedValue2;
            result = value1Time.compareTo(value2Time);
        } else if ("Timestamp".equals(type)) {
            java.sql.Timestamp value1Timestamp = (java.sql.Timestamp) convertedValue1;
            java.sql.Timestamp value2Timestamp = (java.sql.Timestamp) convertedValue2;
            result = value1Timestamp.compareTo(value2Timestamp);
        } else if ("Boolean".equals(type)) {
            Boolean value1Boolean = (Boolean) convertedValue1;
            Boolean value2Boolean = (Boolean) convertedValue2;
            if ("equals".equals(operator)) {
                if ((value1Boolean && value2Boolean) || (!value1Boolean && !value2Boolean))
                    result = 0;
                else
                    result = 1;
            } else if ("not-equals".equals(operator)) {
                if ((!value1Boolean && value2Boolean) || (value1Boolean && !value2Boolean))
                    result = 0;
                else
                    result = 1;
            } else {
                messages.add("Can only compare Booleans using the operators 'equals' or 'not-equals'");
                return null;
            }
        } else if ("Object".equals(type)) {
            if (convertedValue1.equals(convertedValue2)) {
                result = 0;
            } else {
                result = 1;
            }
        } else {
            messages.add("Type \"" + type + "\" specified for compare not supported.");
            return null;
        }

        if (verboseOn) Debug.logVerbose("Got Compare result: " + result + ", operator: " + operator);
        if ("less".equals(operator)) {
            if (result >= 0)
                return Boolean.FALSE;
        } else if ("greater".equals(operator)) {
            if (result <= 0)
                return Boolean.FALSE;
        } else if ("less-equals".equals(operator)) {
            if (result > 0)
                return Boolean.FALSE;
        } else if ("greater-equals".equals(operator)) {
            if (result < 0)
                return Boolean.FALSE;
        } else if ("equals".equals(operator)) {
            if (result != 0)
                return Boolean.FALSE;
        } else if ("not-equals".equals(operator)) {
            if (result == 0)
                return Boolean.FALSE;
        } else {
            messages.add("Specified compare operator \"" + operator + "\" not known.");
            return null;
        }

        if (verboseOn) Debug.logVerbose("Returning true");
        return Boolean.TRUE;
    }
}
