/*
 * $Id: ModelFieldType.java,v 1.1 2005/04/01 05:58:03 sfarquhar Exp $
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

import org.ofbiz.core.util.UtilXml;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.List;

/**
 * Generic Entity - FieldType model class
 *
 * @author     <a href="mailto:jonesde@ofbiz.org">David E. Jones</a> 
 * @version    $Revision: 1.1 $
 * @since      2.0
 */
public class ModelFieldType {

    /** The type of the Field */
    protected String type = null;

    /** The java-type of the Field */
    protected String javaType = null;

    /** The sql-type of the Field */
    protected String sqlType = null;

    /** The sql-type-alias of the Field, this is optional */
    protected String sqlTypeAlias = null;

    /** validators to be called when an update is done */
    protected List<String> validators = new ArrayList<String>();

    /** Default Constructor */
    public ModelFieldType() {}

    /** XML Constructor */
    public ModelFieldType(Element fieldTypeElement) {
        this.type = UtilXml.checkEmpty(fieldTypeElement.getAttribute("type"));
        this.javaType = UtilXml.checkEmpty(fieldTypeElement.getAttribute("java-type"));
        this.sqlType = UtilXml.checkEmpty(fieldTypeElement.getAttribute("sql-type"));
        this.sqlTypeAlias = UtilXml.checkEmpty(fieldTypeElement.getAttribute("sql-type-alias"));

        NodeList validateList = fieldTypeElement.getElementsByTagName("validate");

        for (int i = 0; i < validateList.getLength(); i++) {
            Element element = (Element) validateList.item(i);

            this.validators.add(UtilXml.checkEmpty(element.getAttribute("name")));
        }
    }

    /** The type of the Field */
    public String getType() {
        return this.type;
    }

    /** The java-type of the Field */
    public String getJavaType() {
        return this.javaType;
    }

    /** The sql-type of the Field */
    public String getSqlType() {
        return this.sqlType;
    }

    /** The sql-type-alias of the Field */
    public String getSqlTypeAlias() {
        return this.sqlTypeAlias;
    }

    /** validators to be called when an update is done */
    public List<String> getValidators() {
        return this.validators;
    }

    /** A simple function to derive the max length of a String created from the field value, based on the sql-type
     * @return max length of a String representing the Field value
     */
    public int stringLength() {
        if (sqlType.contains("VARCHAR")) {
            if (sqlType.indexOf("(") > 0 && sqlType.indexOf(")") > 0) {
                String length = sqlType.substring(sqlType.indexOf("(") + 1, sqlType.indexOf(")"));

                return Integer.parseInt(length);
            } else {
                return 255;
            }
        } else if (sqlType.contains("CHAR")) {
            if (sqlType.indexOf("(") > 0 && sqlType.indexOf(")") > 0) {
                String length = sqlType.substring(sqlType.indexOf("(") + 1, sqlType.indexOf(")"));

                return Integer.parseInt(length);
            } else {
                return 255;
            }
        } else if (sqlType.contains("TEXT") || sqlType.contains("LONG")) {
            return 5000;
        }
        return 20;
    }
}
