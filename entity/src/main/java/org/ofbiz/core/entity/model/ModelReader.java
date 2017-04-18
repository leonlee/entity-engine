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

import org.ofbiz.core.config.GenericConfigException;
import org.ofbiz.core.config.ResourceHandler;
import org.ofbiz.core.entity.GenericEntityConfException;
import org.ofbiz.core.entity.GenericEntityException;
import org.ofbiz.core.entity.GenericModelException;
import org.ofbiz.core.entity.config.EntityConfigUtil;
import org.ofbiz.core.util.Debug;
import org.ofbiz.core.util.UtilCache;
import org.ofbiz.core.util.UtilTimer;
import org.ofbiz.core.util.UtilXml;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Generic Entity - Entity Definition Reader
 *
 * @author <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 * @author <a href="mailto:jaz@ofbiz.org">Andy Zeneski</a>
 * @version $Revision: 1.1 $
 * @since 2.0
 */
public class ModelReader {

    public static final String module = ModelReader.class.getName();
    public static final UtilCache<String, ModelReader> readers = new UtilCache<String, ModelReader>("entity.ModelReader", 0, 0);

    protected Map<String, ModelEntity> entityCache = null;

    protected int numEntities = 0;
    protected int numViewEntities = 0;
    protected int numFields = 0;
    protected int numRelations = 0;

    protected String modelName;

    /**
     * collection of filenames for entity definitions
     */
    protected Collection<ResourceHandler> entityResourceHandlers;

    /**
     * contains a collection of entity names for each ResourceHandler, populated as they are loaded
     */
    protected Map<ResourceHandler, Collection<String>> resourceHandlerEntities;

    /**
     * for each entity contains a map to the ResourceHandler that the entity came from
     */
    protected Map<String, ResourceHandler> entityResourceHandlerMap;

    public static ModelReader getModelReader(String delegatorName) throws GenericEntityException {
        EntityConfigUtil.DelegatorInfo delegatorInfo = EntityConfigUtil.getInstance().getDelegatorInfo(delegatorName);

        if (delegatorInfo == null) {
            throw new GenericEntityConfException("Could not find a delegator with the name " + delegatorName);
        }

        String tempModelName = delegatorInfo.entityModelReader;
        ModelReader reader = readers.get(tempModelName);

        if (reader == null) { // don't want to block here
            synchronized (ModelReader.class) {
                // must check if null again as one of the blocked threads can still enter
                reader = readers.get(tempModelName);
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
        entityResourceHandlers = new LinkedList<ResourceHandler>();
        resourceHandlerEntities = new HashMap<ResourceHandler, Collection<String>>();
        entityResourceHandlerMap = new HashMap<String, ResourceHandler>();

        EntityConfigUtil.EntityModelReaderInfo entityModelReaderInfo = EntityConfigUtil.getInstance().getEntityModelReaderInfo(modelName);

        if (entityModelReaderInfo == null) {
            throw new GenericEntityConfException("Cound not find an entity-model-reader with the name " + modelName);
        }

        List<Element> resourceElements = entityModelReaderInfo.resourceElements;

        for (Element elem : resourceElements) {
            ResourceHandler handler = new ResourceHandler(EntityConfigUtil.ENTITY_ENGINE_XML_FILENAME, elem);
            entityResourceHandlers.add(handler);
        }
    }

    public Map<String, ModelEntity> getEntityCache() throws GenericEntityException {
        if (entityCache == null) { // don't want to block here
            synchronized (ModelReader.class) {
                // must check if null again as one of the blocked threads can still enter
                if (entityCache == null) { // now it's safe
                    numEntities = 0;
                    numViewEntities = 0;
                    numFields = 0;
                    numRelations = 0;

                    entityCache = new HashMap<String, ModelEntity>();
                    List<ModelViewEntity> tempViewEntityList = new LinkedList<ModelViewEntity>();

                    UtilTimer utilTimer = new UtilTimer();

                    for (ResourceHandler handler : entityResourceHandlers) {

                        // utilTimer.timerString("Before getDocument in file " + entityFileName);
                        Document document = null;

                        try {
                            document = handler.getDocument();
                        } catch (GenericConfigException e) {
                            throw new GenericEntityConfException("Error getting document from resource handler", e);
                        }
                        if (document == null) {
                            Debug.logError("Could not get document for " + handler.toString());
                            entityCache = null;
                            return null;
                        }

                        Hashtable<String, String> docElementValues = new Hashtable<String, String>();

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
                            utilTimer.timerString("Before start of entity loop in " + handler.toString());
                            do {
                                boolean isEntity = "entity".equals(curChild.getNodeName());
                                boolean isViewEntity = "view-entity".equals(curChild.getNodeName());

                                if ((isEntity || isViewEntity) && curChild.getNodeType() == Node.ELEMENT_NODE) {
                                    i++;
                                    Element curEntity = (Element) curChild;
                                    String entityName = UtilXml.checkEmpty(curEntity.getAttribute("entity-name"));

                                    // add entityName to appropriate resourceHandlerEntities collection
                                    Collection<String> resourceHandlerEntityNames = resourceHandlerEntities.get(handler);

                                    if (resourceHandlerEntityNames == null) {
                                        resourceHandlerEntityNames = new LinkedList<String>();
                                        resourceHandlerEntities.put(handler, resourceHandlerEntityNames);
                                    }
                                    resourceHandlerEntityNames.add(entityName);

                                    // check to see if entity with same name has already been read
                                    if (entityCache.containsKey(entityName)) {
                                        Debug.logWarning("WARNING: Entity " + entityName +
                                                " is defined more than once, most recent will over-write " +
                                                "previous definition(s)", module);
                                        Debug.logWarning("WARNING: Entity " + entityName + " was found in " +
                                                handler + ", but was already defined in " +
                                                entityResourceHandlerMap.get(entityName).toString(), module);
                                    }

                                    // add entityName, entityFileName pair to entityResourceHandlerMap map
                                    entityResourceHandlerMap.put(entityName, handler);

                                    // utilTimer.timerString("  After entityEntityName -- " + i + " --");
                                    // ModelEntity entity = createModelEntity(curEntity, docElement, utilTimer, docElementValues);

                                    ModelEntity entity = null;

                                    if (isEntity) {
                                        entity = createModelEntity(curEntity, docElement, null, docElementValues);
                                    } else {
                                        ModelViewEntity mve;
                                        entity = mve = createModelViewEntity(curEntity, docElement, null, docElementValues);
                                        // put the view entity in a list to get ready for the second pass to populate fields...
                                        tempViewEntityList.add(mve);
                                    }

                                    // utilTimer.timerString("  After createModelEntity -- " + i + " --");
                                    if (entity != null) {
                                        entityCache.put(entityName, entity);
                                        // utilTimer.timerString("  After entityCache.put -- " + i + " --");
                                        if (isEntity) {
                                            if (Debug.verboseOn())
                                                Debug.logVerbose("-- [Entity]: #" + i + ": " + entityName, module);
                                        } else {
                                            if (Debug.verboseOn())
                                                Debug.logVerbose("-- [ViewEntity]: #" + i + ": " + entityName, module);
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
                        utilTimer.timerString("Finished " + handler.toString() + " - Total Entities: " + i + " FINISHED");
                    }

                    // do a pass on all of the view entities now that all of the entities have
                    // loaded and populate the fields
                    for (ModelViewEntity curViewEntity : tempViewEntityList) {
                        curViewEntity.populateFields(entityCache);
                    }

                    Debug.log("FINISHED LOADING ENTITIES - ALL FILES; #Entities=" + numEntities + " #ViewEntities=" +
                            numViewEntities + " #Fields=" + numFields + " #Relationships=" + numRelations, module);
                }
            }
        }
        return entityCache;
    }

    /**
     * rebuilds the resourceHandlerEntities Map of Collections based on the current
     * entityResourceHandlerMap Map, must be done whenever a manual change is made to the
     * entityResourceHandlerMap Map after the initial load to make them consistent again.
     */
    public void rebuildResourceHandlerEntities() {
        resourceHandlerEntities = new HashMap<ResourceHandler, Collection<String>>();

        for (Map.Entry<String, ResourceHandler> entry : entityResourceHandlerMap.entrySet()) {
            // add entityName to appropriate resourceHandlerEntities collection
            Collection<String> resourceHandlerEntityNames = resourceHandlerEntities.get(entry.getValue());

            if (resourceHandlerEntityNames == null) {
                resourceHandlerEntityNames = new LinkedList<String>();
                resourceHandlerEntities.put(entry.getValue(), resourceHandlerEntityNames);
            }
            resourceHandlerEntityNames.add(entry.getKey());
        }
    }

    public Iterator<ResourceHandler> getResourceHandlerEntitiesKeyIterator() {
        if (resourceHandlerEntities == null) return null;
        return resourceHandlerEntities.keySet().iterator();
    }

    public Collection<String> getResourceHandlerEntities(ResourceHandler resourceHandler) {
        if (resourceHandlerEntities == null) return null;
        return resourceHandlerEntities.get(resourceHandler);
    }

    public void addEntityToResourceHandler(String entityName, String loaderName, String location) {
        entityResourceHandlerMap.put(entityName, new ResourceHandler(EntityConfigUtil.ENTITY_ENGINE_XML_FILENAME, loaderName, location));
    }

    public ResourceHandler getEntityResourceHandler(String entityName) {
        return entityResourceHandlerMap.get(entityName);
    }

    /**
     * Gets an Entity object based on a definition from the specified XML Entity descriptor file.
     *
     * @param entityName The entityName of the Entity definition to use.
     * @return An Entity object describing the specified entity of the specified descriptor file.
     */
    public ModelEntity getModelEntity(String entityName) throws GenericEntityException {
        Map<String, ModelEntity> ec = getEntityCache();

        if (ec == null) {
            throw new GenericEntityConfException("ERROR: Unable to load Entity Cache");
        }

        ModelEntity modelEntity = ec.get(entityName);

        if (modelEntity == null) {
            throw new GenericModelException("Could not find definition for entity name " + entityName);
        }
        return modelEntity;
    }

    /**
     * Creates a Iterator with the entityName of each Entity defined in the specified XML Entity Descriptor file.
     *
     * @return A Iterator of entityName Strings
     */
    public Iterator<String> getEntityNamesIterator() throws GenericEntityException {
        Collection<String> collection = getEntityNames();

        if (collection != null) {
            return collection.iterator();
        } else {
            return null;
        }
    }

    /**
     * Creates a Collection with the entityName of each Entity defined in the specified XML Entity Descriptor file.
     *
     * @return A Collection of entityName Strings
     */
    public Collection<String> getEntityNames() throws GenericEntityException {
        Map<String, ModelEntity> ec = getEntityCache();

        if (ec == null) {
            throw new GenericEntityConfException("ERROR: Unable to load Entity Cache");
        }
        return ec.keySet();
    }

    ModelEntity createModelEntity(Element entityElement, Element docElement, UtilTimer utilTimer, Hashtable<String, String> docElementValues) {
        if (entityElement == null) return null;
        this.numEntities++;

        return new ModelEntity(this, entityElement, docElement, utilTimer, docElementValues);
    }

    ModelViewEntity createModelViewEntity(Element entityElement, Element docElement, UtilTimer utilTimer, Hashtable<String, String> docElementValues) {
        if (entityElement == null) return null;
        this.numViewEntities++;

        return new ModelViewEntity(this, entityElement, docElement, utilTimer, docElementValues);
    }

    public ModelRelation createRelation(ModelEntity entity, Element relationElement) {
        this.numRelations++;

        return new ModelRelation(entity, relationElement);
    }

    public ModelField findModelField(ModelEntity entity, String fieldName) {
        for (int i = 0; i < entity.fields.size(); i++) {
            ModelField field = entity.fields.get(i);

            if (field.name.compareTo(fieldName) == 0) {
                return field;
            }
        }
        return null;
    }

    public ModelField createModelField(Element fieldElement, Element docElement, Hashtable<String, String> docElementValues) {
        if (fieldElement == null) {
            return null;
        }

        this.numFields++;

        return new ModelField(fieldElement);
    }
}
