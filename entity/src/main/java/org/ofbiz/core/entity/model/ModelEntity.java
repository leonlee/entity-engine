/*
 * $Id: ModelEntity.java,v 1.3 2006/03/07 01:08:05 hbarney Exp $
 *
 * Copyright (c) 2001, 2002 The Open For Business Project - www.ofbiz.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 * CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT
 * OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 * THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.ofbiz.core.entity.model;

import org.ofbiz.core.entity.config.DatasourceInfo;
import org.ofbiz.core.entity.config.EntityConfigUtil;
import org.ofbiz.core.entity.jdbc.DatabaseUtil;
import org.ofbiz.core.util.Debug;
import org.ofbiz.core.util.UtilTimer;
import org.ofbiz.core.util.UtilXml;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Generic Entity - Entity model class
 *
 * @author <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 * @author <a href="mailto:jaz@ofbiz.org">Andy Zeneski</a>
 * @version $Revision: 1.3 $
 * @since 2.0
 */
public class ModelEntity implements Comparable<ModelEntity> {

    public static final String module = ModelEntity.class.getName();

    /**
     * The name of the time stamp field for locking/syncronization
     */
    public static final String STAMP_FIELD = "lastUpdatedStamp";

    /**
     * The ModelReader that created this Entity
     */
    protected ModelReader modelReader = null;

    /**
     * The entity-name of the Entity
     */
    protected String entityName = "";

    /**
     * The table-name of the Entity
     */
    protected String tableName = "";

    /**
     * The package-name of the Entity
     */
    protected String packageName = "";

    /**
     * The entity-name of the Entity that this Entity is dependent on, if empty then no dependency
     */
    protected String dependentOn = "";

    // Strings to go in the comment header.
    /**
     * The title for documentation purposes
     */
    protected String title = "";

    /**
     * The description for documentation purposes
     */
    protected String description = "";

    /**
     * The copyright for documentation purposes
     */
    protected String copyright = "";

    /**
     * The author for documentation purposes
     */
    protected String author = "";

    /**
     * The version for documentation purposes
     */
    protected String version = "";

    /**
     * A List of the Field objects for the Entity
     */
    protected List<ModelField> fields = new ArrayList<ModelField>();
    // Eagerly build the fields map to avoid - JRA-5507
    protected Map<String, ModelField> fieldsMap = new HashMap<String, ModelField>();

    /**
     * A List of the Field objects for the Entity, one for each Primary Key
     */
    protected List<ModelField> pks = new ArrayList<ModelField>();

    /**
     * A List of the Field objects for the Entity, one for each NON Primary Key
     */
    protected List<ModelField> nopks = new ArrayList<ModelField>();

    /**
     * relations defining relationships between this entity and other entities
     */
    protected List<ModelRelation> relations = new ArrayList<ModelRelation>();

    /**
     * indexes on fields/columns in this entity
     */
    protected List<ModelIndex> indexes = new ArrayList<ModelIndex>();

    /**
     * function based indexes in this entity
     */
    protected List<ModelFunctionBasedIndex> functionBasedIndexes = new ArrayList<>();

    /**
     * An indicator to specify if this entity requires locking for updates
     */
    protected boolean doLock = false;

    /**
     * An indicator to specify if this entity is never cached.
     * If true causes the delegator to not clear caches on write and to not get
     * from cache on read showing a warning messages to that effect
     */
    protected boolean neverCache = false;

    // ===== CONSTRUCTORS =====

    /**
     * Default Constructor
     */
    public ModelEntity() {
    }

    /**
     * XML Constructor
     */
    public ModelEntity(ModelReader reader, Element entityElement, Element docElement, UtilTimer utilTimer, Hashtable<String, String> docElementValues) {
        this.modelReader = reader;

        if (utilTimer != null) utilTimer.timerString("  createModelEntity: before general/basic info");
        this.populateBasicInfo(entityElement, docElement, docElementValues);

        if (utilTimer != null) utilTimer.timerString("  createModelEntity: before fields");
        NodeList fieldList = entityElement.getElementsByTagName("field");

        for (int i = 0; i < fieldList.getLength(); i++) {
            ModelField field = reader.createModelField((Element) fieldList.item(i), docElement, docElementValues);

            if (field != null) {
                this.fields.add(field);
                // Eagerly build the fields map to avoid - JRA-5507
                this.fieldsMap.put(field.name, field);
            }
        }

        if (utilTimer != null) utilTimer.timerString("  createModelEntity: before prim-keys");
        NodeList pkList = entityElement.getElementsByTagName("prim-key");

        for (int i = 0; i < pkList.getLength(); i++) {
            ModelField field = reader.findModelField(this, ((Element) pkList.item(i)).getAttribute("field"));

            if (field != null) {
                this.pks.add(field);
                field.isPk = true;
            } else {
                Debug.logError("[ModelReader.createModelEntity] ERROR: Could not find field \"" +
                        ((Element) pkList.item(i)).getAttribute("field") + "\" specified in a prim-key", module);
            }
        }

        // now that we have the pks and the fields, make the nopks vector
        this.nopks = new ArrayList<ModelField>();
        for (ModelField field : this.fields) {
            if (!field.isPk) this.nopks.add(field);
        }

        if (utilTimer != null) utilTimer.timerString("  createModelEntity: before relations");
        this.populateRelated(reader, entityElement);
        this.populateIndexes(entityElement);
        this.populateFunctionBasedIndexes(entityElement);
    }

    /**
     * DB Names Constructor
     */
    public ModelEntity(String tableName, List<DatabaseUtil.ColumnCheckInfo> colList, ModelFieldTypeReader modelFieldTypeReader) {
        this.tableName = tableName.toUpperCase();
        this.entityName = ModelUtil.dbNameToClassName(this.tableName);

        for (DatabaseUtil.ColumnCheckInfo ccInfo : colList) {
            ModelField newField = new ModelField(ccInfo, modelFieldTypeReader);

            this.fields.add(newField);
            // Eagerly build the fields map to avoid - JRA-5507
            fieldsMap.put(newField.name, newField);
        }
        this.updatePkLists();
    }

    protected void populateBasicInfo(Element entityElement, Element docElement, Hashtable<String, String> docElementValues) {
        this.entityName = UtilXml.checkEmpty(entityElement.getAttribute("entity-name"));
        this.tableName = UtilXml.checkEmpty(entityElement.getAttribute("table-name"), ModelUtil.javaNameToDbName(this.entityName));
        this.packageName = UtilXml.checkEmpty(entityElement.getAttribute("package-name"));
        this.dependentOn = UtilXml.checkEmpty(entityElement.getAttribute("dependent-on"));
        this.doLock = UtilXml.checkBoolean(entityElement.getAttribute("enable-lock"), false);
        this.neverCache = UtilXml.checkBoolean(entityElement.getAttribute("never-cache"), false);

        if (docElementValues == null) {
            this.title = UtilXml.checkEmpty(entityElement.getAttribute("title"), UtilXml.childElementValue(docElement, "title"), "None");
            this.description = UtilXml.checkEmpty(UtilXml.childElementValue(entityElement, "description"), UtilXml.childElementValue(docElement, "description"), "None");
            this.copyright = UtilXml.checkEmpty(entityElement.getAttribute("copyright"), UtilXml.childElementValue(docElement, "copyright"), "Copyright (c) 2001 The Open For Business Project - www.ofbiz.org");
            this.author = UtilXml.checkEmpty(entityElement.getAttribute("author"), UtilXml.childElementValue(docElement, "author"), "None");
            this.version = UtilXml.checkEmpty(entityElement.getAttribute("version"), UtilXml.childElementValue(docElement, "version"), "1.0");
        } else {
            if (!docElementValues.containsKey("title"))
                docElementValues.put("title", UtilXml.childElementValue(docElement, "title"));
            if (!docElementValues.containsKey("description"))
                docElementValues.put("description", UtilXml.childElementValue(docElement, "description"));
            if (!docElementValues.containsKey("copyright"))
                docElementValues.put("copyright", UtilXml.childElementValue(docElement, "copyright"));
            if (!docElementValues.containsKey("author"))
                docElementValues.put("author", UtilXml.childElementValue(docElement, "author"));
            if (!docElementValues.containsKey("version"))
                docElementValues.put("version", UtilXml.childElementValue(docElement, "version"));
            this.title = UtilXml.checkEmpty(entityElement.getAttribute("title"), docElementValues.get("title"), "None");
            this.description = UtilXml.checkEmpty(UtilXml.childElementValue(entityElement, "description"), docElementValues.get("description"), "None");
            this.copyright = UtilXml.checkEmpty(entityElement.getAttribute("copyright"), docElementValues.get("copyright"), "Copyright (c) 2001 The Open For Business Project - www.ofbiz.org");
            this.author = UtilXml.checkEmpty(entityElement.getAttribute("author"), docElementValues.get("author"), "None");
            this.version = UtilXml.checkEmpty(entityElement.getAttribute("version"), docElementValues.get("version"), "1.0");
        }
    }

    protected void populateRelated(ModelReader reader, Element entityElement) {
        NodeList relationList = entityElement.getElementsByTagName("relation");
        for (int i = 0; i < relationList.getLength(); i++) {
            Element relationElement = (Element) relationList.item(i);
            if (relationElement.getParentNode() == entityElement) {
                ModelRelation relation = reader.createRelation(this, relationElement);
                if (relation != null) this.relations.add(relation);
            }
        }
    }

    protected void populateIndexes(Element entityElement) {
        NodeList indexList = entityElement.getElementsByTagName("index");
        for (int i = 0; i < indexList.getLength(); i++) {
            Element indexElement = (Element) indexList.item(i);
            if (indexElement.getParentNode() == entityElement) {
                ModelIndex index = new ModelIndex(this, indexElement);
                this.indexes.add(index);
            }
        }
    }

    protected void populateFunctionBasedIndexes(Element entityElement) {
        NodeList indexList = entityElement.getElementsByTagName("function-based-index");
        for (int i = 0; i < indexList.getLength(); i++) {
            Element indexElement = (Element) indexList.item(i);
            if (indexElement.getParentNode() == entityElement) {
                ModelFunctionBasedIndex index = new ModelFunctionBasedIndex(this, indexElement);
                this.functionBasedIndexes.add(index);
            }
        }
    }

    // ===== GETTERS/SETTERS =====

    public ModelReader getModelReader() {
        return modelReader;
    }

    /**
     * The entity-name of the Entity
     */
    public String getEntityName() {
        return this.entityName;
    }

    public void setEntityName(String entityName) {
        this.entityName = entityName;
    }

    /**
     * The plain table-name of the Entity without a schema name prefix
     */
    public String getPlainTableName() {
        return this.tableName;
    }

    /**
     * The table-name of the Entity including a Schema name if specified in the datasource config
     */
    public String getTableName(String helperName) {
        return getTableName(EntityConfigUtil.getInstance().getDatasourceInfo(helperName));
    }

    /**
     * The table-name of the Entity including a Schema name if specified in the datasource config
     */
    public String getTableName(DatasourceInfo datasourceInfo) {
        if (datasourceInfo != null && datasourceInfo.getSchemaName() != null && datasourceInfo.getSchemaName().length() > 0) {
            return datasourceInfo.getSchemaName() + "." + this.tableName;
        } else {
            return this.tableName;
        }
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    /**
     * The package-name of the Entity
     */
    public String getPackageName() {
        return this.packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    /**
     * The entity-name of the Entity that this Entity is dependent on, if empty then no dependency
     */
    public String getDependentOn() {
        return this.dependentOn;
    }

    public void setDependentOn(String dependentOn) {
        this.dependentOn = dependentOn;
    }

    // Strings to go in the comment header.

    /**
     * The title for documentation purposes
     */
    public String getTitle() {
        return this.title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * The description for documentation purposes
     */
    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * The copyright for documentation purposes
     */
    public String getCopyright() {
        return this.copyright;
    }

    public void setCopyright(String copyright) {
        this.copyright = copyright;
    }

    /**
     * The author for documentation purposes
     */
    public String getAuthor() {
        return this.author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    /**
     * The version for documentation purposes
     */
    public String getVersion() {
        return this.version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    /**
     * An indicator to specify if this entity is never cached.
     * If true causes the delegator to not clear caches on write and to not get
     * from cache on read showing a warning messages to that effect
     */
    public boolean getNeverCache() {
        return this.neverCache;
    }

    public void setNeverCache(boolean neverCache) {
        this.neverCache = neverCache;
    }

    /**
     * An indicator to specify if this entity requires locking for updates
     */
    public boolean getDoLock() {
        return this.doLock;
    }

    public void setDoLock(boolean doLock) {
        this.doLock = doLock;
    }

    public boolean lock() {
        if (doLock && isField(STAMP_FIELD)) {
            return true;
        } else {
            doLock = false;
            return false;
        }
    }

    public void updatePkLists() {
        pks = new ArrayList<ModelField>();
        nopks = new ArrayList<ModelField>();
        for (ModelField field : fields) {
            if (field.isPk)
                pks.add(field);
            else
                nopks.add(field);
        }
    }

    public boolean isField(String fieldName) {
        if (fieldName == null) return false;
        for (ModelField field : fields) {
            if (field.name.equals(fieldName)) return true;
        }
        return false;
    }

    public boolean areFields(Collection<String> fieldNames) {
        if (fieldNames == null) return false;

        for (String fieldName : fieldNames) {
            if (!isField(fieldName)) return false;
        }
        return true;
    }

    public int getPksSize() {
        return this.pks.size();
    }

    public ModelField getPk(int index) {
        return this.pks.get(index);
    }

    public Iterator<ModelField> getPksIterator() {
        return this.pks.iterator();
    }

    public List<ModelField> getPksCopy() {
        return new ArrayList<ModelField>(this.pks);
    }

    public int getNopksSize() {
        return this.nopks.size();
    }

    public ModelField getNopk(int index) {
        return this.nopks.get(index);
    }

    public Iterator<ModelField> getNopksIterator() {
        return this.nopks.iterator();
    }

    public List<ModelField> getNopksCopy() {
        return new ArrayList<ModelField>(this.nopks);
    }

    public int getFieldsSize() {
        return this.fields.size();
    }

    public ModelField getField(int index) {
        return this.fields.get(index);
    }

    public Iterator<ModelField> getFieldsIterator() {
        return this.fields.iterator();
    }

    public List<ModelField> getFieldsCopy() {
        return new ArrayList<ModelField>(this.fields);
    }

    public ModelField getField(String fieldName) {
        if (fieldName == null) return null;
        if (fieldsMap == null) {
            fieldsMap = new HashMap<String, ModelField>(fields.size());

            for (ModelField field : fields) {
                fieldsMap.put(field.name, field);
            }
        }
        return fieldsMap.get(fieldName);
    }

    public void addField(ModelField field) {
        if (field == null) return;
        this.fields.add(field);
        this.fieldsMap.put(field.name, field);

        if (field.isPk) {
            pks.add(field);
        } else {
            nopks.add(field);
        }
    }

    public ModelField removeField(int index) {
        ModelField field = null;

        field = fields.remove(index);
        if (field == null) return null;

        this.fieldsMap.remove(field.name);
        if (field.isPk) {
            pks.remove(field);
        } else {
            nopks.remove(field);
        }
        return field;
    }

    public ModelField removeField(String fieldName) {
        if (fieldName == null) return null;
        ModelField field = null;

        for (int i = 0; i < fields.size(); i++) {
            field = fields.get(i);
            if (field.name.equals(fieldName)) {
                fields.remove(i);
                fieldsMap.remove(field.name);
                if (field.isPk) {
                    pks.remove(field);
                } else {
                    nopks.remove(field);
                }
            }
            field = null;
        }
        return field;
    }

    public List<String> getAllFieldNames() {
        return getFieldNamesFromFieldVector(fields);
    }

    public List<String> getPkFieldNames() {
        return getFieldNamesFromFieldVector(pks);
    }

    public List<String> getNoPkFieldNames() {
        return getFieldNamesFromFieldVector(nopks);
    }

    public List<String> getFieldNamesFromFieldVector(List<ModelField> modelFields) {
        List<String> nameList = new ArrayList<String>(modelFields.size());

        if (modelFields == null || modelFields.size() <= 0) return nameList;
        for (ModelField field : modelFields) {
            nameList.add(field.name);
        }
        return nameList;
    }

    public int getRelationsSize() {
        return this.relations.size();
    }

    public ModelRelation getRelation(int index) {
        return this.relations.get(index);
    }

    public Iterator<ModelRelation> getRelationsIterator() {
        return this.relations.iterator();
    }

    public ModelRelation getRelation(String relationName) {
        if (relationName == null) return null;
        for (ModelRelation relation : relations) {
            if (relationName.equals(relation.title + relation.relEntityName)) return relation;
        }
        return null;
    }

    public void addRelation(ModelRelation relation) {
        this.relations.add(relation);
    }

    public ModelRelation removeRelation(int index) {
        return this.relations.remove(index);
    }

    public int getIndexesSize() {
        return this.indexes.size();
    }

    public ModelIndex getIndex(int index) {
        return this.indexes.get(index);
    }

    public Iterator<ModelIndex> getIndexesIterator() {
        return this.indexes.iterator();
    }

    public ModelIndex getIndex(String indexName) {
        if (indexName == null) return null;
        for (ModelIndex index : indexes) {
            if (indexName.equals(index.getName())) return index;
        }
        return null;
    }

    public void addIndex(ModelIndex index) {
        this.indexes.add(index);
    }

    public ModelIndex removeIndex(int index) {
        return this.indexes.remove(index);
    }

    public Iterator<ModelFunctionBasedIndex> getFunctionBasedIndexesIterator() {
        return this.functionBasedIndexes.iterator();
    }

    public void addFunctionBasedIndex(ModelFunctionBasedIndex fbindex) {
        this.functionBasedIndexes.add(fbindex);
    }

    public String nameString(List<ModelField> flds) {
        return nameString(flds, ", ", "");
    }

    public String nameString(List<ModelField> flds, String separator, String afterLast) {
        StringBuilder returnString = new StringBuilder();

        if (flds.size() < 1) {
            return "";
        }

        int i = 0;

        for (; i < flds.size() - 1; i++) {
            returnString.append(flds.get(i).name);
            returnString.append(separator);
        }
        returnString.append(flds.get(i).name);
        returnString.append(afterLast);
        return returnString.toString();
    }

    public String typeNameString(List<ModelField> flds) {
        StringBuilder returnString = new StringBuilder();

        if (flds.size() < 1) {
            return "";
        }

        int i = 0;

        for (; i < flds.size() - 1; i++) {
            ModelField curField = flds.get(i);
            returnString.append(curField.type);
            returnString.append(" ");
            returnString.append(curField.name);
            returnString.append(", ");
        }
        ModelField curField = flds.get(i);
        returnString.append(curField.type);
        returnString.append(" ");
        returnString.append(curField.name);
        return returnString.toString();
    }

    public String fieldNameString() {
        return fieldNameString(", ", "");
    }

    public String fieldNameString(String separator, String afterLast) {
        return nameString(fields, separator, afterLast);
    }

    public String fieldTypeNameString() {
        return typeNameString(fields);
    }

    public String primKeyClassNameString() {
        return typeNameString(pks);
    }

    public String pkNameString() {
        return pkNameString(", ", "");
    }

    public String pkNameString(String separator, String afterLast) {
        return nameString(pks, separator, afterLast);
    }

    public String nonPkNullList() {
        return fieldsStringList(fields, "null", ", ", false, true);
    }

    public String fieldsStringList(List<ModelField> flds, String eachString, String separator) {
        return fieldsStringList(flds, eachString, separator, false, false);
    }

    public String fieldsStringList(List<ModelField> flds, String eachString, String separator, boolean appendIndex) {
        return fieldsStringList(flds, eachString, separator, appendIndex, false);
    }

    public String fieldsStringList(List<ModelField> flds, String eachString, String separator, boolean appendIndex, boolean onlyNonPK) {
        StringBuilder returnString = new StringBuilder();

        if (flds.size() < 1) {
            return "";
        }

        int i = 0;

        for (; i < flds.size(); i++) {
            if (onlyNonPK && flds.get(i).isPk) continue;
            returnString.append(eachString);
            if (appendIndex) returnString.append(i + 1);
            if (i < flds.size() - 1) returnString.append(separator);
        }
        return returnString.toString();
    }

    public String colNameString(List<ModelField> flds) {
        return colNameString(flds, ", ", "");
    }

    public String colNameString(List<ModelField> flds, String separator, String afterLast) {
        StringBuilder returnString = new StringBuilder();

        if (flds.size() < 1) {
            return "";
        }

        int i = 0;

        for (; i < flds.size() - 1; i++) {
            returnString.append(flds.get(i).getSafeColName());
            returnString.append(separator);
        }
        returnString.append(flds.get(i).getSafeColName());
        returnString.append(afterLast);
        return returnString.toString();
    }

    public String classNameString(List<ModelField> flds) {
        return classNameString(flds, ", ", "");
    }

    public String classNameString(List<ModelField> flds, String separator, String afterLast) {
        StringBuilder returnString = new StringBuilder();

        if (flds.size() < 1) {
            return "";
        }

        int i = 0;

        for (; i < flds.size() - 1; i++) {
            returnString.append(ModelUtil.upperFirstChar(flds.get(i).name));
            returnString.append(separator);
        }
        returnString.append(ModelUtil.upperFirstChar(flds.get(i).name));
        returnString.append(afterLast);
        return returnString.toString();
    }

    public String finderQueryString(List<ModelField> flds) {
        StringBuilder returnString = new StringBuilder();

        if (flds.size() < 1) {
            return "";
        }
        int i = 0;

        for (; i < flds.size() - 1; i++) {
            returnString.append(flds.get(i).getSafeColName());
            returnString.append(" like {");
            returnString.append(i);
            returnString.append("} AND ");
        }
        returnString.append(flds.get(i).getSafeColName());
        returnString.append(" like {");
        returnString.append(i);
        returnString.append("}");
        return returnString.toString();
    }

    public String httpArgList(List<ModelField> flds) {
        StringBuilder returnString = new StringBuilder();

        if (flds.size() < 1) {
            return "";
        }
        int i = 0;

        for (; i < flds.size() - 1; i++) {
            returnString.append("\"");
            returnString.append(tableName);
            returnString.append("_");
            returnString.append(flds.get(i).getSafeColName());
            returnString.append("=\" + ");
            returnString.append(flds.get(i).name);
            returnString.append(" + \"&\" + ");
        }
        returnString.append("\"");
        returnString.append(tableName);
        returnString.append("_");
        returnString.append(flds.get(i).getSafeColName());
        returnString.append("=\" + ");
        returnString.append(flds.get(i).name);
        return returnString.toString();
    }

    public String httpArgListFromClass(List<ModelField> flds) {
        StringBuilder returnString = new StringBuilder();

        if (flds.size() < 1) {
            return "";
        }

        int i = 0;

        for (; i < flds.size() - 1; i++) {
            returnString.append("\"");
            returnString.append(tableName);
            returnString.append("_");
            returnString.append(flds.get(i).getSafeColName());
            returnString.append("=\" + ");
            returnString.append(ModelUtil.lowerFirstChar(entityName));
            returnString.append(".get");
            returnString.append(ModelUtil.upperFirstChar(flds.get(i).name));
            returnString.append("() + \"&\" + ");
        }
        returnString.append("\"");
        returnString.append(tableName);
        returnString.append("_");
        returnString.append(flds.get(i).getSafeColName());
        returnString.append("=\" + ");
        returnString.append(ModelUtil.lowerFirstChar(entityName));
        returnString.append(".get");
        returnString.append(ModelUtil.upperFirstChar(flds.get(i).name));
        returnString.append("()");
        return returnString.toString();
    }

    public String httpArgListFromClass(List<ModelField> flds, String entityNameSuffix) {
        StringBuilder returnString = new StringBuilder();

        if (flds.size() < 1) {
            return "";
        }

        int i = 0;

        for (; i < flds.size() - 1; i++) {
            returnString.append("\"");
            returnString.append(tableName);
            returnString.append("_");
            returnString.append(flds.get(i).getSafeColName());
            returnString.append("=\" + ");
            returnString.append(ModelUtil.lowerFirstChar(entityName));
            returnString.append(entityNameSuffix);
            returnString.append(".get");
            returnString.append(ModelUtil.upperFirstChar(flds.get(i).name));
            returnString.append("() + \"&\" + ");
        }
        returnString.append("\"");
        returnString.append(tableName);
        returnString.append("_");
        returnString.append(flds.get(i).getSafeColName());
        returnString.append("=\" + ");
        returnString.append(ModelUtil.lowerFirstChar(entityName));
        returnString.append(entityNameSuffix);
        returnString.append(".get");
        returnString.append(ModelUtil.upperFirstChar(flds.get(i).name));
        returnString.append("()");
        return returnString.toString();
    }

    public String httpRelationArgList(List<ModelField> flds, ModelRelation relation) {
        StringBuilder returnString = new StringBuilder();

        if (flds.size() < 1) {
            return "";
        }

        int i = 0;

        for (; i < flds.size() - 1; i++) {
            ModelKeyMap keyMap = relation.findKeyMapByRelated(flds.get(i).name);

            if (keyMap != null) {
                returnString.append("\"");
                returnString.append(tableName);
                returnString.append("_");
                returnString.append(flds.get(i).getSafeColName());
                returnString.append("=\" + ");
                returnString.append(ModelUtil.lowerFirstChar(relation.mainEntity.entityName));
                returnString.append(".get");
                returnString.append(ModelUtil.upperFirstChar(keyMap.fieldName));
                returnString.append("() + \"&\" + ");
            } else {
                Debug.logWarning("-- -- ENTITYGEN ERROR:httpRelationArgList: Related Key in Key Map not found for name: " + flds.get(i).name + " related entity: " + relation.relEntityName + " main entity: " + relation.mainEntity.entityName + " type: " + relation.type);
            }
        }
        ModelKeyMap keyMap = relation.findKeyMapByRelated(flds.get(i).name);

        if (keyMap != null) {
            returnString.append("\"");
            returnString.append(tableName);
            returnString.append("_");
            returnString.append(flds.get(i).getSafeColName());
            returnString.append("=\" + ");
            returnString.append(ModelUtil.lowerFirstChar(relation.mainEntity.entityName));
            returnString.append(".get");
            returnString.append(ModelUtil.upperFirstChar(keyMap.fieldName));
            returnString.append("()");
        } else {
            Debug.logWarning("-- -- ENTITYGEN ERROR:httpRelationArgList: Related Key in Key Map not found for name: " + flds.get(i).name + " related entity: " + relation.relEntityName + " main entity: " + relation.mainEntity.entityName + " type: " + relation.type);
        }
        return returnString.toString();
    }

    /*
     public String httpRelationArgList(ModelRelation relation) {
     String returnString = "";
     if(relation.keyMaps.size() < 1) { return ""; }

     int i = 0;
     for(; i < relation.keyMaps.size() - 1; i++) {
     ModelKeyMap keyMap = (ModelKeyMap)relation.keyMaps.get(i);
     if(keyMap != null)
     returnString = returnString + "\"" + tableName + "_" + keyMap.relColName + "=\" + " + ModelUtil.lowerFirstChar(relation.mainEntity.entityName) + ".get" + ModelUtil.upperFirstChar(keyMap.fieldName) + "() + \"&\" + ";
     }
     ModelKeyMap keyMap = (ModelKeyMap)relation.keyMaps.get(i);
     returnString = returnString + "\"" + tableName + "_" + keyMap.relColName + "=\" + " + ModelUtil.lowerFirstChar(relation.mainEntity.entityName) + ".get" + ModelUtil.upperFirstChar(keyMap.fieldName) + "()";
     return returnString;
     }
     */
    public String typeNameStringRelatedNoMapped(List<ModelField> flds, ModelRelation relation) {
        StringBuilder returnString = new StringBuilder();

        if (flds.size() < 1) {
            return "";
        }

        int i = 0;

        if (relation.findKeyMapByRelated(flds.get(i).name) == null) {
            returnString.append(flds.get(i).type);
            returnString.append(" ");
            returnString.append(flds.get(i).name);
        }
        i++;
        for (; i < flds.size(); i++) {
            if (relation.findKeyMapByRelated(flds.get(i).name) == null) {
                if (returnString.length() > 0) returnString.append(", ");
                returnString.append(flds.get(i).type);
                returnString.append(" ");
                returnString.append(flds.get(i).name);
            }
        }
        return returnString.toString();
    }

    public String typeNameStringRelatedAndMain(List<ModelField> flds, ModelRelation relation) {
        StringBuilder returnString = new StringBuilder();

        if (flds.size() < 1) {
            return "";
        }

        int i = 0;

        for (; i < flds.size() - 1; i++) {
            ModelKeyMap keyMap = relation.findKeyMapByRelated(flds.get(i).name);

            if (keyMap != null) {
                returnString.append(keyMap.fieldName);
                returnString.append(", ");
            } else {
                returnString.append(flds.get(i).name);
                returnString.append(", ");
            }
        }
        ModelKeyMap keyMap = relation.findKeyMapByRelated(flds.get(i).name);

        if (keyMap != null) returnString.append(keyMap.fieldName);
        else returnString.append(flds.get(i).name);
        return returnString.toString();
    }

    public int compareTo(ModelEntity obj) {

        /* This DOESN'T WORK, so forget it... using two passes
         //sort list by fk dependencies

         if (this.getEntityName().equals(otherModelEntity.getEntityName())) {
         return 0;
         }

         //look through relations for dependencies from this entity to the other
         Iterator relationsIter = this.getRelationsIterator();
         while (relationsIter.hasNext()) {
         ModelRelation modelRelation = (ModelRelation) relationsIter.next();

         if ("one".equals(modelRelation.getType()) && modelRelation.getRelEntityName().equals(otherModelEntity.getEntityName())) {
         //this entity is dependent on the other entity, so put that entity earlier in the list
         return -1;
         }
         }

         //look through relations for dependencies from the other to this entity
         Iterator otherRelationsIter = otherModelEntity.getRelationsIterator();
         while (otherRelationsIter.hasNext()) {
         ModelRelation modelRelation = (ModelRelation) otherRelationsIter.next();

         if ("one".equals(modelRelation.getType()) && modelRelation.getRelEntityName().equals(this.getEntityName())) {
         //the other entity is dependent on this entity, so put that entity later in the list
         return 1;
         }
         }

         return 0;
         */

        return this.getEntityName().compareTo(obj.getEntityName());
    }
}

