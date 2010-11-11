/*
 * $Id: ModelUtil.java,v 1.1 2005/04/01 05:58:03 sfarquhar Exp $
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
package org.ofbiz.core.entity.model;

import java.io.*;

import org.ofbiz.core.util.*;

/**
 * Generic Entity - General Utilities
 *
 * @author     <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 * @version    $Revision: 1.1 $
 * @since      2.0
 */
public class ModelUtil {

    /** 
     * Changes the first letter of the passed String to upper case.
     * @param string The passed String
     * @return A String with an upper case first letter
     */
    public static String upperFirstChar(String string) {
        if (string == null) return null;
        if (string.length() <= 1) return string.toLowerCase();
        StringBuffer sb = new StringBuffer(string);

        sb.setCharAt(0, Character.toUpperCase(sb.charAt(0)));
        return sb.toString();
    }

    /** 
     * Changes the first letter of the passed String to lower case.
     *
     * @param string The passed String
     * @return A String with a lower case first letter
     */
    public static String lowerFirstChar(String string) {
        if (string == null) return null;
        if (string.length() <= 1) return string.toLowerCase();
        StringBuffer sb = new StringBuffer(string);

        sb.setCharAt(0, Character.toLowerCase(sb.charAt(0)));
        return sb.toString();
    }

    /** Converts a database name to a Java class name.
     * The naming conventions used to allow for this are as follows: a database name (table or 
     * column) is in all capital letters, and the words are separated by an underscore 
     * (for example: NEAT_ENTITY_NAME or RANDOM_FIELD_NAME); a Java name (ejb or field) is in all 
     * lower case letters, except the letter at the beginning of each word (for example: 
     * NeatEntityName or RandomFieldName). The convention of using a capital letter at 
     * the beginning of a class name in Java, or a lower-case letter for the beginning of a 
     * variable name in Java is also used along with the Java name convention above.
     * @param columnName The database name
     * @return The Java class name
     */
    public static String dbNameToClassName(String columnName) {
        return upperFirstChar(dbNameToVarName(columnName));
    }

    /** Converts a database name to a Java variable name.
     * The naming conventions used to allow for this are as follows: a database name (table or 
     * column) is in all capital letters, and the words are separated by an underscore 
     * (for example: NEAT_ENTITY_NAME or RANDOM_FIELD_NAME); a Java name (ejb or field) is in all 
     * lower case letters, except the letter at the beginning of each word (for example: 
     * NeatEntityName or RandomFieldName). The convention of using a capital letter at 
     * the beginning of a class name in Java, or a lower-case letter for the beginning of a 
     * variable name in Java is also used along with the Java name convention above.
     * @param columnName The database name
     * @return The Java variable name
     */
    public static String dbNameToVarName(String columnName) {
        if (columnName == null) return null;

        StringBuffer fieldName = new StringBuffer(columnName.length());

        boolean toUpper = false;
        for (int i=0; i < columnName.length(); i++) {
            char ch = columnName.charAt(i);
            if (ch == '_') {
                toUpper = true;
            } else if (toUpper == true) {
                fieldName.append(Character.toUpperCase(ch));
                toUpper = false;
            } else {
                fieldName.append(Character.toLowerCase(ch));
            }
        }

        return fieldName.toString();
    }

    /** 
     * Converts a Java variable name to a database name.
     * The naming conventions used to allow for this are as follows: a database name (table or 
     * column) is in all capital letters, and the words are separated by an underscore 
     * (for example: NEAT_ENTITY_NAME or RANDOM_FIELD_NAME); a Java name (ejb or field) is in all 
     * lower case letters, except the letter at the beginning of each word (for example: 
     * NeatEntityName or RandomFieldName). The convention of using a capital letter at 
     * the beginning of a class name in Java, or a lower-case letter for the beginning of a 
     * variable name in Java is also used along with the Java name convention above.
     * @param javaName The Java variable name
     * @return The database name
     */
    public static String javaNameToDbName(String javaName) {
        if (javaName == null) return null;
        if (javaName.length() <= 0) return "";
        StringBuffer dbName = new StringBuffer();

        dbName.append(Character.toUpperCase(javaName.charAt(0)));
        int namePos = 1;

        while (namePos < javaName.length()) {
            char curChar = javaName.charAt(namePos);

            if (Character.isUpperCase(curChar)) dbName.append('_');
            dbName.append(Character.toUpperCase(curChar));
            namePos++;
        }

        return dbName.toString();
    }

    /** 
     * Converts a package name to a path by replacing all '.' characters with the File.separatorChar character. 
     *  Is therefore platform independent.
     * @param The package name.
     * @return The path name corresponding to the specified package name.
     */
    public static String packageToPath(String packageName) {
        // just replace all of the '.' characters with the folder separater character
        return packageName.replace('.', File.separatorChar);
    }

    /** 
     * Replaces all occurances of oldString in mainString with newString
     * @param mainString The original string
     * @param oldString The string to replace
     * @param newString The string to insert in place of the old
     * @return mainString with all occurances of oldString replaced by newString
     */
    public static String replaceString(String mainString, String oldString, String newString) {
        return StringUtil.replaceString(mainString, oldString, newString);
    }

    public static String induceFieldType(String sqlTypeName, int length, int precision, ModelFieldTypeReader fieldTypeReader) {
        if (sqlTypeName == null) return "invalid";

        if (sqlTypeName.equals("VARCHAR") || sqlTypeName.equals("VARCHAR2")) {
            if (length <= 10) return "very-short";
            if (length <= 60) return "short-varchar";
            if (length <= 255) return "long-varchar";
            if (length <= 4000) return "very-long";
            return "invalid";
        } else if (sqlTypeName.equals("TEXT")) {
            return "very-long";
        } else if (sqlTypeName.equals("DECIMAL") || sqlTypeName.equals("NUMERIC")) {
            if (length > 18) return "invalid";
            if (precision == 0) return "numeric";
            if (precision <= 2) return "currency-amount";
            if (precision <= 6) return "floating-point";
            return "invalid";
        } else if (sqlTypeName.equals("BLOB") || sqlTypeName.equals("OID")) {
            return "blob";
        } else if (sqlTypeName.equals("DATETIME") || sqlTypeName.equals("TIMESTAMP")) {
            return "date-time";
        } else if (sqlTypeName.equals("DATE")) {
            return "date";
        } else if (sqlTypeName.equals("TIME")) {
            return "time";
        } else if (sqlTypeName.equals("CHAR") && length == 1) {
            return "indicator";
        } else {
            return "invalid";
        }
    }
}
