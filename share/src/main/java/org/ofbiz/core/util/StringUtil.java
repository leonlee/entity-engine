/*
 * $Id: StringUtil.java,v 1.1 2005/04/01 05:58:06 sfarquhar Exp $
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

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.*;

/**
 * Misc String Utility Functions
 *
 * @author     <a href="mailto:jaz@ofbiz.org">Andy Zeneski</a>
 * @version    $Revision: 1.1 $
 * @since      2.0
 */
public class StringUtil {

    /** 
     * Replaces all occurances of oldString in mainString with newString
     * @param mainString The original string
     * @param oldString The string to replace
     * @param newString The string to insert in place of the old
     * @return mainString with all occurances of oldString replaced by newString
     */
    public static String replaceString(String mainString, String oldString, String newString) {
        if (mainString == null) {
            return null;
        }
        if (oldString == null || oldString.length() == 0) {
            return mainString;
        }
        if (newString == null) {
            newString = "";
        }

        int i = mainString.lastIndexOf(oldString);

        if (i < 0) return mainString;

        StringBuilder mainSb = new StringBuilder(mainString);

        while (i >= 0) {
            mainSb.replace(i, i + oldString.length(), newString);
            i = mainString.lastIndexOf(oldString, i - 1);
        }
        return mainSb.toString();
    }

    /**
     * Creates a single string from a List of strings seperated by a delimiter.
     * @param list a list of strings to join
     * @param delim the delimiter character(s) to use. (null value will join with no delimiter)
     * @return a String of all values in the list seperated by the delimiter
     * @deprecated Use {@code com.google.common.base.Joiner} instead
     */
    public static String join(List<String> list, String delim) {
        if (list == null || list.size() < 1)
            return null;
        StringBuilder buf = new StringBuilder();
        Iterator<String> i = list.iterator();

        while (i.hasNext()) {
            buf.append(i.next());
            if (i.hasNext())
                buf.append(delim);
        }
        return buf.toString();
    }

    /**
     * Splits a String on a delimiter into a List of Strings.
     * @param str the String to split
     * @param delim the delimiter character(s) to join on (null will split on whitespace)
     * @return a list of Strings
     */
    public static List<String> split(String str, String delim) {
        List<String> splitList = null;
        StringTokenizer st = null;

        if (str == null)
            return splitList;

        if (delim != null)
            st = new StringTokenizer(str, delim);
        else
            st = new StringTokenizer(str);

        if (st != null && st.hasMoreTokens()) {
            splitList = new ArrayList<String>();

            while (st.hasMoreTokens())
                splitList.add(st.nextToken());
        }
        return splitList;
    }

    /**
     * Encloses each of a List of Strings in quotes.
     * @param list List of String(s) to quote.
     */
    public static List<String> quoteStrList(List<String> list) {
        List<String> tmpList = list;

        list = new ArrayList<String>();

        for (String aTmpList : tmpList) {
            String str = aTmpList;

            str = "'" + str + "''";
            list.add(str);
        }
        return list;
    }

    /**
     * Creates a Map from an encoded name/value pair string
     * @param str The string to decode and format
     * @return a Map of name/value pairs
     */
    public static Map<String, String> strToMap(String str) {
        if (str == null) return null;
        Map<String, String> decodedMap = new HashMap<String, String>();
        List<String> elements = split(str, "|");

        for (String element : elements) {
            List<String> e = split(element, "=");

            if (e.size() != 2)
                continue;
            String name = e.get(0);
            String value = e.get(1);

            decodedMap.put(URLDecoder.decode(name), URLDecoder.decode(value));
        }
        return decodedMap;
    }

    /**
     * Creates an encoded String from a Map of name/value pairs (MUST BE STRINGS!)
     * @param map The Map of name/value pairs
     * @return String The encoded String
     */
    public static String mapToStr(Map<?, ?> map) {
        if (map == null) return null;
        StringBuilder buf = new StringBuilder();
        Set<?> keySet = map.keySet();
        Iterator<?> i = keySet.iterator();
        boolean first = true;

        while (i.hasNext()) {
            Object key = i.next();
            Object value = map.get(key);

            if (!(key instanceof String) || !(value instanceof String))
                continue;
            String encodedName = URLEncoder.encode((String) key);
            String encodedValue = URLEncoder.encode((String) value);

            if (first)
                first = false;
            else
                buf.append("|");

            buf.append(encodedName);
            buf.append("=");
            buf.append(encodedValue);
        }
        return buf.toString();
    }

    /** Make sure the string starts with a forward slash but does not end with one; converts back-slashes to forward-slashes; if in String is null or empty, returns zero length string. */
    public static String cleanUpPathPrefix(String prefix) {
        if (prefix == null || prefix.length() == 0) return "";

        StringBuilder cppBuff = new StringBuilder(prefix.replace('\\', '/'));

        if (cppBuff.charAt(0) != '/') {
            cppBuff.insert(0, '/');
        }
        if (cppBuff.charAt(cppBuff.length() - 1) == '/') {
            cppBuff.deleteCharAt(cppBuff.length() - 1);
        }
        return cppBuff.toString();
    }
    
    /** Removes all spaces from a string */
    public static String removeSpaces(String str) {
        StringBuilder newString = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) != ' ')
                newString.append(str.charAt(i));
        }
        return newString.toString();        
    }
}
