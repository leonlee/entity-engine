/*
 * $Id: ModelReader.java,v 1.1 2005/04/01 05:58:03 sfarquhar Exp $
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

import java.util.*;
import org.w3c.dom.*;

import org.ofbiz.core.config.*;
import org.ofbiz.core.util.*;
import org.ofbiz.core.entity.*;
import org.ofbiz.core.entity.config.*;

/**
 * Generic Entity - Entity Definition Reader
 *
 * @author     <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 * @author     <a href="mailto:jaz@ofbiz.org">Andy Zeneski</a>
 * @version    $Revision: 1.1 $
 * @since      2.0
 */
public class ModelReader {

    public static final String module = ModelReader.class.getName();
    public static UtilCache readers = new UtilCache("entity.ModelReader", 0, 0);

    protected Map entityCache = null;

    protected int numEntities = 0;
    protected int numViewEntities = 0;
    protected int numFields = 0;
    protected int numRelations = 0;

    protected String modelName;

    /** collection of filenames for entity definitions */
    protected Collection entityResourceHandlers;

    /** contains a collection of entity names for each ResourceHandler, populated as they are loaded */
    protected Map resourceHandlerEntities;

    /** for each entity contains a map to the ResourceHandler that the entity came from */
    protected Map entityResourceHandlerMap;

    public static ModelReader getModelReader(String delegatorName) throws GenericEntityException {
        EntityConfigUtil.DelegatorInfo delegatorInfo = EntityConfigUtil.getInstance().getDelegatorInfo(delegatorName);

        if (delegatorInfo == null) {
            throw new GenericEntityConfException("Could not find a delegator with the name " + delegatorName);
        }

        String tempModelName = delegatorInfo.entityModelReader;
        ModelReader reader = (ModelReader) readers.get(tempModelName);

        if (reader == null) { // don't want to block here
            synchronized (ModelReader.class) {
                // must check if null again as one of the blocked threads can still enter
                reader = (ModelReader) readers.get(tempModelName);
                if (reader == null) {
                    reader = new ModelReader(tempModelName);
                    // preload caches...
                    reader.getEntityCache();
                    readers.put(tempModelName, reader);
                }
            }
        }
        return reader;
    }

    public ModelReader(String modelName) throws GenericEntityException {
        this.modelName = modelName;
        entityResourceHandlers = new LinkedList();
        resourceHandlerEntities = new HashMap();
        entityResourceHandlerMap = new HashMap();

        EntityConfigUtil.EntityModelReaderInfo entityModelReaderInfo = EntityConfigUtil.getInstance().getEntityModelReaderInfo(modelName);

        if (entityModelReaderInfo == null) {
            throw new GenericEntityConfException("Cound not find an entity-model-reader with the name " + modelName);
        }

        List resourceElements = entityModelReaderInfo.resourceElements;
        Iterator resIter = resourceElements.iterator();

        while (resIter.hasNext()) {
            Element resourceElement = (Element) resIter.next();
            ResourceHandler handler = new ResourceHandler(EntityConfigUtil.ENTITY_ENGINE_XML_FILENAME, resourceElement);

            entityResourceHandlers.add(handler);
        }
    }

    public Map getEntityCache() throws GenericEntityException {
        if (entityCache == null) { // don't want to block here
            synchronized (ModelReader.class) {
                // must check if null again as one of the blocked threads can still enter
                if (entityCache == null) { // now it's safe
                    numEntities = 0;
                    numViewEntities = 0;
                    numFields = 0;
                    numRelations = 0;

                    entityCache = new HashMap();
                    List tempViewEntityList = new LinkedList();

                    UtilTimer utilTimer = new UtilTimer();

                    Iterator rhIter = entityResourceHandlers.iterator();

                    while (rhIter.hasNext()) {
                        ResourceHandler entityResourceHandler = (ResourceHandler) rhIter.next();

                        // utilTimer.timerString("Before getDocument in file " + entityFileName);
                        Document document = null;

                        try {
                            document = entityResourceHandler.getDocument();
                        } catch (GenericConfigException e) {
                            throw new GenericEntityConfException("Error getting document from resource handler", e);
                        }
                        if (document == null) {
                            Debug.logError("Could not get document for " + entityResourceHandler.toString());
                            entityCache = null;
                            return null;
                        }

                        Hashtable docElementValues = null;

                        docElementValues = new Hashtable();

                        // utilTimer.timerString("Before getDocumentElement in " + entityResourceHandler.toString());
                        Element docElement = document.getDocumentElement();

                        if (docElement == null) {
                            entityCache = null;
                            return null;
                        }
                        docElement.normalize();
                        Node curChild = docElement.getFirstChild();

                        int i = 0;

                        if (curChild != null) {
                            utilTimer.timerString("Before start of entity loop in " + entityResourceHandler.toString());
                            do {
                                boolean isEntity = "entity".equals(curChild.getNodeName());
                                boolean isViewEntity = "view-entity".equals(curChild.getNodeName());

                                if ((isEntity || isViewEntity) && curChild.getNodeType() == Node.ELEMENT_NODE) {
                                    i++;
                                    Element curEntity = (Element) curChild;
                                    String entityName = UtilXml.checkEmpty(curEntity.getAttribute("entity-name"));

                                    // add entityName to appropriate resourceHandlerEntities collection
                                    Collection resourceHandlerEntityNames = (Collection) resourceHandlerEntities.get(entityResourceHandler);

                                    if (resourceHandlerEntityNames == null) {
                                        resourceHandlerEntityNames = new LinkedList();
                                        resourceHandlerEntities.put(entityResourceHandler, resourceHandlerEntityNames);
                                    }
                                    resourceHandlerEntityNames.add(entityName);

                                    // check to see if entity with same name has already been read
                                    if (entityCache.containsKey(entityName)) {
                                        Debug.logWarning("WARNING: Entity " + entityName +
                                            " is defined more than once, most recent will over-write " +
                                            "previous definition(s)", module);
                                        Debug.logWarning("WARNING: Entity " + entityName + " was found in " +
                                            entityResourceHandler + ", but was already defined in " +
                                            entityResourceHandlerMap.get(entityName).toString(), module);
                                    }

                                    // add entityName, entityFileName pair to entityResourceHandlerMap map
                                    entityResourceHandlerMap.put(entityName, entityResourceHandler);

                                    // utilTimer.timerString("  After entityEntityName -- " + i + " --");
                                    // ModelEntity entity = createModelEntity(curEntity, docElement, utilTimer, docElementValues);

                                    ModelEntity entity = null;

                                    if (isEntity) {
                                        entity = createModelEntity(curEntity, docElement, null, docElementValues);
                                    } else {
                                        entity = createModelViewEntity(curEntity, docElement, null, docElementValues);
                                        // put the view entity in a list to get ready for the second pass to populate fields...
                                        tempViewEntityList.add(entity);
                                    }

                                    // utilTimer.timerString("  After createModelEntity -- " + i + " --");
                                    if (entity != null) {
                                        entityCache.put(entityName, entity);
                                        // utilTimer.timerString("  After entityCache.put -- " + i + " --");
                                        if (isEntity) {
                                            if (Debug.verboseOn()) Debug.logVerbose("-- [Entity]: #" + i + ": " + entityName, module);
                                        } else {
                                            if (Debug.verboseOn()) Debug.logVerbose("-- [ViewEntity]: #" + i + ": " + entityName, module);
                                        }
                                    } else {
                                        Debug.logWarning("-- -- ENTITYGEN ERROR:getModelEntity: Could not create " +
                                            "entity for entityName: " + entityName, module);
                                    }

                                }
                            } while ((curChild = curChild.getNextSibling()) != null);
                        } else {
                            Debug.logWarning("No child nodes found.", module);
                        }
                        utilTimer.timerString("Finished " + entityResourceHandler.toString() + " - Total Entities: " + i + " FINISHED");
                    }

                    // do a pass on all of the view entities now that all of the entities have
                    // loaded and populate the fields
                    for (int velInd = 0; velInd < tempViewEntityList.size(); velInd++) {
                        ModelViewEntity curViewEntity = (ModelViewEntity) tempViewEntityList.get(velInd);

                        curViewEntity.populateFields(entityCache);
                    }

                    Debug.log("FINISHED LOADING ENTITIES - ALL FILES; #Entities=" + numEntities + " #ViewEntities=" +
                        numViewEntities + " #Fields=" + numFields + " #Relationships=" + numRelations, module);
                }
            }
        }
        return entityCache;
    }

    /** rebuilds the resourceHandlerEntities Map of Collections based on the current
     *  entityResourceHandlerMap Map, must be done whenever a manual change is made to the
     *  entityResourceHandlerMap Map after the initial load to make them consistent again.
     */
    public void rebuildResourceHandlerEntities() {
        resourceHandlerEntities = new HashMap();
        Iterator entityResourceIter = entityResourceHandlerMap.entrySet().iterator();

        while (entityResourceIter.hasNext()) {
            Map.Entry entry = (Map.Entry) entityResourceIter.next();
            // add entityName to appropriate resourceHandlerEntities collection
            Collection resourceHandlerEntityNames = (Collection) resourceHandlerEntities.get(entry.getValue());

            if (resourceHandlerEntityNames == null) {
                resourceHandlerEntityNames = new LinkedList();
                resourceHandlerEntities.put(entry.getValue(), resourceHandlerEntityNames);
            }
            resourceHandlerEntityNames.add(entry.getKey());
        }
    }

    public Iterator getResourceHandlerEntitiesKeyIterator() {
        if (resourceHandlerEntities == null) return null;
        return resourceHandlerEntities.keySet().iterator();
    }

    public Collection getResourceHandlerEntities(ResourceHandler resourceHandler) {
        if (resourceHandlerEntities == null) return null;
        return (Collection) resourceHandlerEntities.get(resourceHandler);
    }

    public void addEntityToResourceHandler(String entityName, String loaderName, String location) {
        entityResourceHandlerMap.put(entityName, new ResourceHandler(EntityConfigUtil.ENTITY_ENGINE_XML_FILENAME, loaderName, location));
    }

    public ResourceHandler getEntityResourceHandler(String entityName) {
        return (ResourceHandler) entityResourceHandlerMap.get(entityName);
    }

    /** Gets an Entity object based on a definition from the specified XML Entity descriptor file.
     * @param entityName The entityName of the Entity definition to use.
     * @return An Entity object describing the specified entity of the specified descriptor file.
     */
    public ModelEntity getModelEntity(String entityName) throws GenericEntityException {
        Map ec = getEntityCache();

        if (ec == null) {
            throw new GenericEntityConfException("ERROR: Unable to load Entity Cache");
        }

        ModelEntity modelEntity = (ModelEntity) ec.get(entityName);

        if (modelEntity == null) {
            throw new GenericModelException("Could not find definition for entity name " + entityName);
        }
        return modelEntity;
    }

    /** Creates a Iterator with the entityName of each Entity defined in the specified XML Entity Descriptor file.
     * @return A Iterator of entityName Strings
     */
    public Iterator getEntityNamesIterator() throws GenericEntityException {
        Collection collection = getEntityNames();

        if (collection != null) {
            return collection.iterator();
        } else {
            return null;
        }
    }

    /** Creates a Collection with the entityName of each Entity defined in the specified XML Entity Descriptor file.
     * @return A Collection of entityName Strings
     */
    public Collection getEntityNames() throws GenericEntityException {
        Map ec = getEntityCache();

        if (ec == null) {
            throw new GenericEntityConfException("ERROR: Unable to load Entity Cache");
        }
        return ec.keySet();
    }

    ModelEntity createModelEntity(Element entityElement, Element docElement, UtilTimer utilTimer, Hashtable docElementValues) {
        if (entityElement == null) return null;
        this.numEntities++;
        ModelEntity entity = new ModelEntity(this, entityElement, docElement, utilTimer, docElementValues);

        return entity;
    }

    ModelEntity createModelViewEntity(Element entityElement, Element docElement, UtilTimer utilTimer, Hashtable docElementValues) {
        if (entityElement == null) return null;
        this.numViewEntities++;
        ModelViewEntity entity = new ModelViewEntity(this, entityElement, docElement, utilTimer, docElementValues);

        return entity;
    }

    public ModelRelation createRelation(ModelEntity entity, Element relationElement) {
        this.numRelations++;
        ModelRelation relation = new ModelRelation(entity, relationElement);

        return relation;
    }

    public ModelField findModelField(ModelEntity entity, String fieldName) {
        for (int i = 0; i < entity.fields.size(); i++) {
            ModelField field = (ModelField) entity.fields.get(i);

            if (field.name.compareTo(fieldName) == 0) {
                return field;
            }
        }
        return null;
    }

    public ModelField createModelField(Element fieldElement, Element docElement, Hashtable docElementValues) {
        if (fieldElement == null) {
            return null;
        }

        this.numFields++;
        ModelField field = new ModelField(fieldElement);

        return field;
    }
}
