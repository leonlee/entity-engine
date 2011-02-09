/*
 * $Id: ModelViewEntity.java,v 1.2 2005/06/24 02:41:03 amazkovoi Exp $
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

import org.ofbiz.core.entity.jdbc.SqlJdbcUtil;
import org.ofbiz.core.util.Debug;
import org.ofbiz.core.util.UtilTimer;
import org.ofbiz.core.util.UtilValidate;
import org.ofbiz.core.util.UtilXml;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.util.*;

/**
 * This class extends ModelEntity and provides additional information appropriate to view entities
 *
 * @author     <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 * @author     <a href="mailto:peterm@miraculum.com">Peter Moon</a>
 * @version    $Revision: 1.2 $
 * @since      2.0
 */
public class ModelViewEntity extends ModelEntity {
    public static final String module = ModelViewEntity.class.getName();

    /** Contains member-entity alias name definitions: key is alias, value is ModelMemberEntity */
    protected Map<String, ModelMemberEntity> memberModelMemberEntities = new HashMap<String, ModelMemberEntity>();

    /** A list of all ModelMemberEntity entries; this is mainly used to preserve the original order of member entities from the XML file */
    protected List<ModelMemberEntity> allModelMemberEntities = new LinkedList<ModelMemberEntity>();

    /** Contains member-entity ModelEntities: key is alias, value is ModelEntity; populated with fields */
    protected Map<String, ModelEntity> memberModelEntities = null;

    /** List of aliases with information in addition to what is in the standard field list */
    protected List<ModelAlias> aliases = new ArrayList<ModelAlias>();

    /** List of view links to define how entities are connected (or "joined") */
    protected List<ModelViewLink> viewLinks = new ArrayList<ModelViewLink>();

    /** A List of the Field objects for the View Entity, one for each GROUP BY field */
    protected List<ModelField> groupBys = new ArrayList<ModelField>();

    public ModelViewEntity(ModelReader reader, Element entityElement, Element docElement, UtilTimer utilTimer, Hashtable<String, String> docElementValues) {
        this.modelReader = reader;

        if (utilTimer != null) utilTimer.timerString("  createModelViewEntity: before general/basic info");
        this.populateBasicInfo(entityElement, docElement, docElementValues);

        if (utilTimer != null) utilTimer.timerString("  createModelViewEntity: before \"member-entity\"s");
        NodeList membEntList = entityElement.getElementsByTagName("member-entity");

        for (int i = 0; i < membEntList.getLength(); i++) {
            Element membEnt = (Element) membEntList.item(i);
            String alias = UtilXml.checkEmpty(membEnt.getAttribute("entity-alias"));
            String name = UtilXml.checkEmpty(membEnt.getAttribute("entity-name"));

            if (name.length() <= 0 || alias.length() <= 0) {
                Debug.logError("[new ModelViewEntity] entity-alias or entity-name missing on member-entity element of the view-entity " + this.entityName, module);
            } else {
                ModelMemberEntity modelMemberEntity = new ModelMemberEntity(alias, name);

                this.addMemberModelMemberEntity(modelMemberEntity);
            }
        }

        // when reading aliases, just read them into the alias list, there will be a pass
        // after loading all entities to go back and fill in all of the ModelField entries
        if (utilTimer != null) utilTimer.timerString("  createModelViewEntity: before aliases");
        NodeList aliasList = entityElement.getElementsByTagName("alias");

        for (int i = 0; i < aliasList.getLength(); i++) {
            Element aliasElement = (Element) aliasList.item(i);
            ModelViewEntity.ModelAlias alias = new ModelAlias(aliasElement);

            this.aliases.add(alias);
        }

        NodeList viewLinkList = entityElement.getElementsByTagName("view-link");

        for (int i = 0; i < viewLinkList.getLength(); i++) {
            Element viewLinkElement = (Element) viewLinkList.item(i);
            ModelViewLink viewLink = new ModelViewLink(viewLinkElement);

            this.addViewLink(viewLink);
        }

        if (utilTimer != null) utilTimer.timerString("  createModelEntity: before relations");
        this.populateRelated(reader, entityElement);

        // before finishing, make sure the table name is null, this should help bring up errors early...
        this.tableName = null;
    }

    public Map<String, ModelMemberEntity> getMemberModelMemberEntities() {
        return this.memberModelMemberEntities;
    }

    public List<ModelMemberEntity> getAllModelMemberEntities() {
        return this.allModelMemberEntities;
    }

    public ModelMemberEntity getMemberModelMemberEntity(String alias) {
        return this.memberModelMemberEntities.get(alias);
    }

    public ModelEntity getMemberModelEntity(String alias) {
        if (this.memberModelEntities == null) {
            this.memberModelEntities = new HashMap<String, ModelEntity>();
            populateFields(this.getModelReader().entityCache);
        }
        return this.memberModelEntities.get(alias);
    }

    public void addMemberModelMemberEntity(ModelMemberEntity modelMemberEntity) {
        this.memberModelMemberEntities.put(modelMemberEntity.getEntityAlias(), modelMemberEntity);
        this.allModelMemberEntities.add(modelMemberEntity);
    }

    public void removeMemberModelMemberEntity(String alias) {
        ModelMemberEntity modelMemberEntity = this.memberModelMemberEntities.remove(alias);

        if (modelMemberEntity == null) return;
        this.allModelMemberEntities.remove(modelMemberEntity);
    }

    /** List of aliases with information in addition to what is in the standard field list */
    public ModelAlias getAlias(int index) {
        return this.aliases.get(index);
    }

    public int getAliasesSize() {
        return this.aliases.size();
    }

    public Iterator<ModelAlias> getAliasesIterator() {
        return this.aliases.iterator();
    }

    public List<ModelAlias> getAliasesCopy() {
        return new ArrayList<ModelAlias>(this.aliases);
    }

    public List<ModelField> getGroupBysCopy() {
        return new ArrayList<ModelField>(this.groupBys);
    }

    /** List of view links to define how entities are connected (or "joined") */
    public ModelViewLink getViewLink(int index) {
        return this.viewLinks.get(index);
    }

    public int getViewLinksSize() {
        return this.viewLinks.size();
    }

    public Iterator<ModelViewLink> getViewLinksIterator() {
        return this.viewLinks.iterator();
    }

    public List<ModelViewLink> getViewLinksCopy() {
        return new ArrayList<ModelViewLink>(this.viewLinks);
    }

    public void addViewLink(ModelViewLink viewLink) {
        this.viewLinks.add(viewLink);
    }

    public void populateFields(Map<String, ModelEntity> entityCache) {
        if (this.memberModelEntities == null) {
            this.memberModelEntities = new HashMap<String, ModelEntity>();
        }

        for (Map.Entry<String, ModelMemberEntity> entry : memberModelMemberEntities.entrySet()) {
            ModelMemberEntity modelMemberEntity = entry.getValue();
            String aliasedEntityName = modelMemberEntity.getEntityName();
            ModelEntity aliasedEntity = entityCache.get(aliasedEntityName);

            if (aliasedEntity == null) {
                Debug.logError("[ModelViewEntity.populateFields] ERROR: could not find ModelEntity for entity name: " +
                        aliasedEntityName);
                continue;
            }
            memberModelEntities.put(entry.getKey(), aliasedEntity);
        }

        for (ModelAlias alias : aliases) {
            ModelMemberEntity modelMemberEntity = memberModelMemberEntities.get(alias.entityAlias);

            if (modelMemberEntity == null) {
                Debug.logError("No member entity with alias " + alias.entityAlias + " found in view-entity " + this.getEntityName() + "; this view-entity will NOT be usable...");
            }
            String aliasedEntityName = modelMemberEntity.getEntityName();
            ModelEntity aliasedEntity = entityCache.get(aliasedEntityName);

            if (aliasedEntity == null) {
                Debug.logError("[ModelViewEntity.populateFields] ERROR: could not find ModelEntity for entity name: " +
                        aliasedEntityName);
                continue;
            }

            ModelField aliasedField = aliasedEntity.getField(alias.field);

            if (aliasedField == null) {
                Debug.logError("[ModelViewEntity.populateFields] ERROR: could not find ModelField for field name \"" +
                        alias.field + "\" on entity with name: " + aliasedEntityName);
                continue;
            }

            ModelField field = new ModelField();

            field.name = alias.name;
            if (alias.isPk != null) {
                field.isPk = alias.isPk;
            } else {
                field.isPk = aliasedField.isPk;
            }

            this.fields.add(field);
            // Ensure the fields map gets populated (Fix JRA-7080 -  which was actually caused by
            // fixing 5507).
            fieldsMap.put(field.name, field);
            if (field.isPk) {
                this.pks.add(field);
            } else {
                this.nopks.add(field);
            }

            // if this is a groupBy field, add it to the groupBys list
            if (alias.groupBy) {
                this.groupBys.add(field);
            }

            // show a warning if function is specified and groupBy is true
            if (UtilValidate.isNotEmpty(alias.function) && alias.groupBy) {
                Debug.logWarning("The view-entity alias with name=" + alias.name + " has a function value and is specified as a group-by field; this may be an error, but is not necessarily.");
            }

            if ("count".equals(alias.function) || "count-distinct".equals(alias.function)) {
                // if we have a "count" function we have to change the type
                field.type = "numeric";
            } else {
                field.type = aliasedField.type;
            }

            if (UtilValidate.isNotEmpty(alias.function)) {
                if ("min".equals(alias.function)) {
                    field.colName = "MIN(" + alias.entityAlias + "." + SqlJdbcUtil.filterColName(aliasedField.colName) + ")";
                } else if ("max".equals(alias.function)) {
                    field.colName = "MAX(" + alias.entityAlias + "." + SqlJdbcUtil.filterColName(aliasedField.colName) + ")";
                } else if ("sum".equals(alias.function)) {
                    field.colName = "SUM(" + alias.entityAlias + "." + SqlJdbcUtil.filterColName(aliasedField.colName) + ")";
                } else if ("avg".equals(alias.function)) {
                    field.colName = "AVG(" + alias.entityAlias + "." + SqlJdbcUtil.filterColName(aliasedField.colName) + ")";
                } else if ("count".equals(alias.function)) {
                    field.colName = "COUNT(" + alias.entityAlias + "." + SqlJdbcUtil.filterColName(aliasedField.colName) + ")";
                } else if ("count-distinct".equals(alias.function)) {
                    field.colName = "COUNT(DISTINCT " + alias.entityAlias + "." + SqlJdbcUtil.filterColName(aliasedField.colName) + ")";
                } else if ("upper".equals(alias.function)) {
                    field.colName = "UPPER(" + alias.entityAlias + "." + SqlJdbcUtil.filterColName(aliasedField.colName) + ")";
                } else if ("lower".equals(alias.function)) {
                    field.colName = "LOWER(" + alias.entityAlias + "." + SqlJdbcUtil.filterColName(aliasedField.colName) + ")";
                } else {
                    Debug.logWarning("Specified alias function [" + alias.function + "] not valid; must be: min, max, sum, avg, count or count-distinct; using a column name with no function function");
                    field.colName = alias.entityAlias + "." + SqlJdbcUtil.filterColName(aliasedField.colName);
                }
            } else {
                field.colName = alias.entityAlias + "." + SqlJdbcUtil.filterColName(aliasedField.colName);
            }

            field.validators = aliasedField.validators;
        }
    }

    public static class ModelMemberEntity {
        protected String entityAlias = "";
        protected String entityName = "";

        public ModelMemberEntity(String entityAlias, String entityName) {
            this.entityAlias = entityAlias;
            this.entityName = entityName;
        }

        public String getEntityAlias() {
            return this.entityAlias;
        }

        public String getEntityName() {
            return this.entityName;
        }
    }


    public static class ModelAlias {
        protected String entityAlias = "";
        protected String name = "";
        protected String field = "";
        // this is a Boolean object for a tri-state: null, true or false
        protected Boolean isPk = null;
        protected boolean groupBy = false;
        // is specified this alias is a calculated value; can be: min, max, sum, avg, count, count-distinct
        protected String function = null;

        protected ModelAlias() {}

        public ModelAlias(Element aliasElement) {
            this.entityAlias = UtilXml.checkEmpty(aliasElement.getAttribute("entity-alias"));
            this.name = UtilXml.checkEmpty(aliasElement.getAttribute("name"));
            this.field = UtilXml.checkEmpty(aliasElement.getAttribute("field"), this.name);
            String primKeyValue = UtilXml.checkEmpty(aliasElement.getAttribute("prim-key"));

            if (UtilValidate.isNotEmpty(primKeyValue)) {
                this.isPk = "true".equals(primKeyValue);
            } else {
                this.isPk = null;
            }
            this.groupBy = "true".equals(UtilXml.checkEmpty(aliasElement.getAttribute("group-by")));
            this.function = UtilXml.checkEmpty(aliasElement.getAttribute("function"));
        }

        public ModelAlias(String entityAlias, String name, String field, Boolean isPk, boolean groupBy, String function) {
            this.entityAlias = entityAlias;
            this.name = name;
            this.field = field;
            this.isPk = isPk;
            this.groupBy = groupBy;
            this.function = function;
        }

        public String getEntityAlias() {
            return this.entityAlias;
        }

        public String getName() {
            return this.name;
        }

        public String getField() {
            return this.field;
        }

        public Boolean getIsPk() {
            return this.isPk;
        }

        public boolean getGroupBy() {
            return this.groupBy;
        }

        public String getFunction() {
            return this.function;
        }
    }


    public static class ModelViewLink {
        protected String entityAlias = "";
        protected String relEntityAlias = "";
        protected boolean relOptional = false;
        protected List<ModelKeyMap> keyMaps = new ArrayList<ModelKeyMap>();

        protected ModelViewLink() {}

        public ModelViewLink(Element viewLinkElement) {
            this.entityAlias = UtilXml.checkEmpty(viewLinkElement.getAttribute("entity-alias"));
            this.relEntityAlias = UtilXml.checkEmpty(viewLinkElement.getAttribute("rel-entity-alias"));
            // if anything but true will be false; ie defaults to false
            this.relOptional = "true".equals(viewLinkElement.getAttribute("rel-optional"));

            NodeList keyMapList = viewLinkElement.getElementsByTagName("key-map");

            for (int j = 0; j < keyMapList.getLength(); j++) {
                Element keyMapElement = (Element) keyMapList.item(j);
                ModelKeyMap keyMap = new ModelKeyMap(keyMapElement);

                if (keyMap != null) keyMaps.add(keyMap);
            }
        }

        public ModelViewLink(String entityAlias, String relEntityAlias, List<? extends ModelKeyMap> keyMaps) {
            this.entityAlias = entityAlias;
            this.relEntityAlias = relEntityAlias;
            this.keyMaps.addAll(keyMaps);
        }

        public String getEntityAlias() {
            return this.entityAlias;
        }

        public String getRelEntityAlias() {
            return this.relEntityAlias;
        }

        public boolean isRelOptional() {
            return this.relOptional;
        }

        public ModelKeyMap getKeyMap(int index) {
            return this.keyMaps.get(index);
        }

        public int getKeyMapsSize() {
            return this.keyMaps.size();
        }

        public Iterator<ModelKeyMap> getKeyMapsIterator() {
            return this.keyMaps.iterator();
        }

        public List<ModelKeyMap> getKeyMapsCopy() {
            return new ArrayList<ModelKeyMap>(this.keyMaps);
        }
    }
}
