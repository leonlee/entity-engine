/*
 * $Id: ModelRelation.java,v 1.1 2005/04/01 05:58:04 sfarquhar Exp $
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
import java.util.Iterator;
import java.util.List;

/**
 * Generic Entity - Relation model class
 *
 * @author <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 * @version $Revision: 1.1 $
 * @since 2.0
 */
public class ModelRelation {

    /**
     * the title, gives a name/description to the relation
     */
    protected String title;

    /**
     * the type: either "one" or "many" or "one-nofk"
     */
    protected String type;

    /**
     * the name of the related entity
     */
    protected String relEntityName;

    /**
     * the name to use for a database foreign key, if applies
     */
    protected String fkName;

    /**
     * keyMaps defining how to lookup the relatedTable using columns from this table
     */
    protected List<ModelKeyMap> keyMaps = new ArrayList<ModelKeyMap>();

    /**
     * the main entity of this relation
     */
    protected ModelEntity mainEntity = null;

    /**
     * Default Constructor
     */
    public ModelRelation() {
        title = "";
        type = "";
        relEntityName = "";
        fkName = "";
    }

    /**
     * XML Constructor
     */
    public ModelRelation(ModelEntity mainEntity, Element relationElement) {
        this.mainEntity = mainEntity;

        this.type = UtilXml.checkEmpty(relationElement.getAttribute("type"));
        this.title = UtilXml.checkEmpty(relationElement.getAttribute("title"));
        this.relEntityName = UtilXml.checkEmpty(relationElement.getAttribute("rel-entity-name"));
        this.fkName = UtilXml.checkEmpty(relationElement.getAttribute("fk-name"));

        NodeList keyMapList = relationElement.getElementsByTagName("key-map");
        for (int i = 0; i < keyMapList.getLength(); i++) {
            Element keyMapElement = (Element) keyMapList.item(i);

            if (keyMapElement.getParentNode() == relationElement) {
                ModelKeyMap keyMap = new ModelKeyMap(keyMapElement);

                if (keyMap != null) {
                    this.keyMaps.add(keyMap);
                }
            }
        }
    }

    /**
     * the title, gives a name/description to the relation
     */
    public String getTitle() {
        return this.title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * the type: either "one" or "many" or "one-nofk"
     */
    public String getType() {
        return this.type;
    }

    public void setType(String type) {
        this.type = type;
    }

    /**
     * the name of the related entity
     */
    public String getRelEntityName() {
        return this.relEntityName;
    }

    public void setRelEntityName(String relEntityName) {
        this.relEntityName = relEntityName;
    }

    public String getFkName() {
        return this.fkName;
    }

    public void setFkName(String fkName) {
        this.fkName = fkName;
    }

    /**
     * the main entity of this relation
     */
    public ModelEntity getMainEntity() {
        return this.mainEntity;
    }

    public void setMainEntity(ModelEntity mainEntity) {
        this.mainEntity = mainEntity;
    }

    /**
     * keyMaps defining how to lookup the relatedTable using columns from this table
     */
    public Iterator<ModelKeyMap> getKeyMapsIterator() {
        return this.keyMaps.iterator();
    }

    public int getKeyMapsSize() {
        return this.keyMaps.size();
    }

    public ModelKeyMap getKeyMap(int index) {
        return this.keyMaps.get(index);
    }

    public void addKeyMap(ModelKeyMap keyMap) {
        this.keyMaps.add(keyMap);
    }

    public ModelKeyMap removeKeyMap(int index) {
        return this.keyMaps.remove(index);
    }

    /**
     * Find a KeyMap with the specified fieldName
     */
    public ModelKeyMap findKeyMap(String fieldName) {
        for (ModelKeyMap keyMap : keyMaps) {
            if (keyMap.fieldName.equals(fieldName)) return keyMap;
        }
        return null;
    }

    /**
     * Find a KeyMap with the specified relFieldName
     */
    public ModelKeyMap findKeyMapByRelated(String relFieldName) {
        for (ModelKeyMap keyMap : keyMaps) {
            if (keyMap.relFieldName.equals(relFieldName))
                return keyMap;
        }
        return null;
    }

    public String keyMapString(String separator, String afterLast) {
        String returnString = "";

        if (keyMaps.size() < 1) {
            return "";
        }

        int i = 0;

        for (; i < keyMaps.size() - 1; i++) {
            returnString = returnString + keyMaps.get(i).fieldName + separator;
        }
        returnString = returnString + keyMaps.get(i).fieldName + afterLast;
        return returnString;
    }

    public String keyMapUpperString(String separator, String afterLast) {
/*
        String returnString = "";

        if (keyMaps.size() < 1) {
            return "";
        }

        int i = 0;

        for (; i < keyMaps.size() - 1; i++) {
            returnString = returnString + ModelUtil.upperFirstChar(((ModelKeyMap) keyMaps.get(i)).fieldName) + separator;
        }
        returnString = returnString + ModelUtil.upperFirstChar(((ModelKeyMap) keyMaps.get(i)).fieldName) + afterLast;
        return returnString;
*/
        if (keyMaps.size() < 1)
            return "";

        StringBuilder returnString = new StringBuilder(keyMaps.size() * 10);
        int i = 0;
        while (true) {
            ModelKeyMap kmap = keyMaps.get(i);
            returnString.append(ModelUtil.upperFirstChar(kmap.fieldName));

            i++;
            if (i >= keyMaps.size()) {
                returnString.append(afterLast);
                break;
            }

            returnString.append(separator);
        }

        return returnString.toString();
    }

    public String keyMapRelatedUpperString(String separator, String afterLast) {
/*
        String returnString = "";

        if (keyMaps.size() < 1) {
            return "";
        }

        int i = 0;

        for (; i < keyMaps.size() - 1; i++) {
            returnString = returnString + ModelUtil.upperFirstChar(((ModelKeyMap) keyMaps.get(i)).relFieldName) + separator;
        }
        returnString = returnString + ModelUtil.upperFirstChar(((ModelKeyMap) keyMaps.get(i)).relFieldName) + afterLast;
        return returnString;
*/
        if (keyMaps.size() < 1)
            return "";

        StringBuilder returnString = new StringBuilder(keyMaps.size() * 10);
        int i = 0;
        while (true) {
            ModelKeyMap kmap = keyMaps.get(i);
            returnString.append(ModelUtil.upperFirstChar(kmap.relFieldName));

            i++;
            if (i >= keyMaps.size()) {
                returnString.append(afterLast);
                break;
            }

            returnString.append(separator);
        }

        return returnString.toString();
    }

    /*
     public String keyMapColumnString(String separator, String afterLast) {
     String returnString = "";
     if(keyMaps.size() < 1) { return ""; }

     int i = 0;
     for(; i < keyMaps.size() - 1; i++) {
     returnString = returnString + ((ModelKeyMap)keyMaps.get(i)).colName + separator;
     }
     returnString = returnString + ((ModelKeyMap)keyMaps.get(i)).colName + afterLast;
     return returnString;
     }
     */

    /*
     public String keyMapRelatedColumnString(String separator, String afterLast) {
     String returnString = "";
     if(keyMaps.size() < 1) { return ""; }

     int i = 0;
     for(; i < keyMaps.size() - 1; i++) {
     returnString = returnString + ((ModelKeyMap)keyMaps.get(i)).relColName + separator;
     }
     returnString = returnString + ((ModelKeyMap)keyMaps.get(i)).relColName + afterLast;
     return returnString;
     }
     */
}
