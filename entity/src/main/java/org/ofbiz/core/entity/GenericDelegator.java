/*
 * $Id: GenericDelegator.java,v 1.3 2006/10/16 00:50:13 cmountford Exp $
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
 *
 */
package org.ofbiz.core.entity;

import java.util.*;
import java.net.*;

import com.atlassian.util.concurrent.CopyOnWriteMap;
import org.ofbiz.core.util.*;
import org.ofbiz.core.entity.model.*;
import org.ofbiz.core.entity.config.*;
import org.ofbiz.core.entity.eca.*;

import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Generic Data Source Delegator Class
 *
 * todo The thread safety in here (and everywhere of ofbiz) is crap, improper double check locking, modification of
 * maps while other threads may be reading them, this class is not thread safe at all.
 *
 * @author     <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 * @author     <a href="mailto:chris_maurer@altavista.com">Chris Maurer</a>
 * @author     <a href="mailto:jaz@ofbiz.org">Andy Zeneski</a
 * @version    $Revision: 1.3 $
 * @since      1.0
 */
public class GenericDelegator implements DelegatorInterface {

    private static boolean isLocked = false;

    public static final String module = GenericDelegator.class.getName();

    /** the delegatorCache will now be a HashMap, allowing reload of definitions,
     * but the delegator will always be the same object for the given name */
    protected static Map<String, GenericDelegator> delegatorCache = CopyOnWriteMap.newHashMap();
    protected String delegatorName;
    protected EntityConfigUtil.DelegatorInfo delegatorInfo = null;

    /** set this to true for better performance; set to false to be able to reload definitions at runtime throught the cache manager */
    public final boolean keepLocalReaders = true;
    protected ModelReader modelReader = null;
    protected ModelGroupReader modelGroupReader = null;

    protected UtilCache primaryKeyCache = null;
    protected UtilCache allCache = null;
    protected UtilCache andCache = null;

    // keeps a list of field key sets used in the by and cache, a Set (of Sets of fieldNames) for each entityName
    protected Map andCacheFieldSets = new HashMap();

    protected DistributedCacheClear distributedCacheClear = null;

    protected EntityEcaHandler entityEcaHandler = null;
    public static final String ECA_HANDLER_CLASS_NAME = "org.ofbiz.core.extentity.eca.DelegatorEcaHandler";

    protected SequenceUtil sequencer = null;

    public static GenericDelegator getGenericDelegator(String delegatorName) {
        GenericDelegator delegator = delegatorCache.get(delegatorName);

        if (delegator == null) {
            synchronized (GenericDelegator.class) {
                // must check if null again as one of the blocked threads can still enter
                delegator = delegatorCache.get(delegatorName);
                if (delegator == null) {
                    try {
                        if (isLocked) {
                            delegator = new LockedDatabaseGenericDelegator();
                        } else {
                            delegator = new GenericDelegator(delegatorName);
                        }
                    } catch (GenericEntityException e) {
                        Debug.logError(e, "Error creating delegator", module);
                    }
                    if (delegator != null) {
                        delegatorCache.put(delegatorName, delegator);
                    }
                }
            }
        }
        return delegator;
    }

    public static synchronized void removeGenericDelegator(String delegatorName)
    {
        delegatorCache.remove(delegatorName);
    }

    public static void lock() {
        isLocked = true;
    }

    /** Only allow creation through the factory method */
    protected GenericDelegator() {}

    /** Only allow creation through the factory method */
    protected GenericDelegator(String delegatorName) throws GenericEntityException {
        if (Debug.infoOn()) Debug.logInfo("Creating new Delegator with name \"" + delegatorName + "\".", module);

        this.delegatorName = delegatorName;
        if (keepLocalReaders) {
            modelReader = ModelReader.getModelReader(delegatorName);
            modelGroupReader = ModelGroupReader.getModelGroupReader(delegatorName);
        }

        primaryKeyCache = new UtilCache("entity.FindByPrimaryKey." + delegatorName, 0, 0, true);
        allCache = new UtilCache("entity.FindAll." + delegatorName, 0, 0, true);
        andCache = new UtilCache("entity.FindByAnd." + delegatorName, 0, 0, true);

        // initialize helpers by group
        Iterator groups = UtilMisc.toIterator(getModelGroupReader().getGroupNames());

        while (groups != null && groups.hasNext()) {
            String groupName = (String) groups.next();
            String helperName = this.getGroupHelperName(groupName);

            if (Debug.infoOn()) Debug.logInfo("Delegator \"" + delegatorName + "\" initializing helper \"" +
                    helperName + "\" for entity group \"" + groupName + "\".", module);
            TreeSet helpersDone = new TreeSet();

            if (helperName != null && helperName.length() > 0) {
                // make sure each helper is only loaded once
                if (helpersDone.contains(helperName)) {
                    if (Debug.infoOn()) Debug.logInfo("Helper \"" + helperName + "\" already initialized, not re-initializing.", module);
                    continue;
                }
                helpersDone.add(helperName);
                // pre-load field type defs, the return value is ignored
                ModelFieldTypeReader.getModelFieldTypeReader(helperName);
                // get the helper and if configured, do the datasource check
                GenericHelper helper = GenericHelperFactory.getHelper(helperName);

                EntityConfigUtil.DatasourceInfo datasourceInfo = EntityConfigUtil.getInstance().getDatasourceInfo(helperName);

                if (datasourceInfo.checkOnStart) {
                    if (Debug.infoOn()) Debug.logInfo("Doing database check as requested in entityengine.xml with addMissing=" + datasourceInfo.addMissingOnStart, module);
                    try {
                        helper.checkDataSource(this.getModelEntityMapByGroup(groupName), null, datasourceInfo.addMissingOnStart);
                    } catch (GenericEntityException e) {
                        Debug.logWarning(e.getMessage(), module);
                    }
                }
            }
        }

        //time to do some tricks with manual class loading that resolves circular dependencies, like calling services...
        ClassLoader loader = Thread.currentThread().getContextClassLoader();

        // if useDistributedCacheClear is false do nothing since the
        // distributedCacheClear member field with a null value will cause the
        // dcc code to do nothing
        if (getDelegatorInfo().useDistributedCacheClear) {
            // initialize the distributedCacheClear mechanism
            String distributedCacheClearClassName = getDelegatorInfo().distributedCacheClearClassName;

            try {
                Class dccClass = loader.loadClass(distributedCacheClearClassName);
                this.distributedCacheClear = (DistributedCacheClear) dccClass.newInstance();
                this.distributedCacheClear.setDelegator(this, getDelegatorInfo().distributedCacheClearUserLoginId);
            } catch (ClassNotFoundException e) {
                Debug.logWarning(e, "DistributedCacheClear class with name " + distributedCacheClearClassName + " was not found, distributed cache clearing will be disabled");
            } catch (InstantiationException e) {
                Debug.logWarning(e, "DistributedCacheClear class with name " + distributedCacheClearClassName + " could not be instantiated, distributed cache clearing will be disabled");
            } catch (IllegalAccessException e) {
                Debug.logWarning(e, "DistributedCacheClear class with name " + distributedCacheClearClassName + " could not be accessed (illegal), distributed cache clearing will be disabled");
            } catch (ClassCastException e) {
                Debug.logWarning(e, "DistributedCacheClear class with name " + distributedCacheClearClassName + " does not implement the DistributedCacheClear interface, distributed cache clearing will be disabled");
            }
        }

        // setup the Entity ECA Handler
        try {
            Class eecahClass = loader.loadClass(ECA_HANDLER_CLASS_NAME);
            this.entityEcaHandler = (EntityEcaHandler) eecahClass.newInstance();
            this.entityEcaHandler.setDelegator(this);
        } catch (ClassNotFoundException e) {
            //Debug.logWarning(e, "EntityEcaHandler class with name " + ECA_HANDLER_CLASS_NAME + " was not found, Entity ECA Rules will be disabled");
        } catch (InstantiationException e) {
            Debug.logWarning(e, "EntityEcaHandler class with name " + ECA_HANDLER_CLASS_NAME + " could not be instantiated, Entity ECA Rules will be disabled");
        } catch (IllegalAccessException e) {
            Debug.logWarning(e, "EntityEcaHandler class with name " + ECA_HANDLER_CLASS_NAME + " could not be accessed (illegal), Entity ECA Rules will be disabled");
        } catch (ClassCastException e) {
            Debug.logWarning(e, "EntityEcaHandler class with name " + ECA_HANDLER_CLASS_NAME + " does not implement the EntityEcaHandler interface, Entity ECA Rules will be disabled");
        }
    }

    /** Gets the name of the server configuration that corresponds to this delegator
     * @return server configuration name
     */
    public String getDelegatorName() {
        return this.delegatorName;
    }

    protected EntityConfigUtil.DelegatorInfo getDelegatorInfo() {
        if (delegatorInfo == null) {
            delegatorInfo = EntityConfigUtil.getInstance().getDelegatorInfo(this.delegatorName);
        }
        return delegatorInfo;
    }

    /** Gets the instance of ModelReader that corresponds to this delegator
     *@return ModelReader that corresponds to this delegator
     */
    public ModelReader getModelReader() {
        if (keepLocalReaders) {
            return this.modelReader;
        } else {
            try {
                return ModelReader.getModelReader(delegatorName);
            } catch (GenericEntityException e) {
                Debug.logError(e, "Error loading entity model", module);
                return null;
            }
        }
    }

    /** Gets the instance of ModelGroupReader that corresponds to this delegator
     *@return ModelGroupReader that corresponds to this delegator
     */
    public ModelGroupReader getModelGroupReader() {
        if (keepLocalReaders) {
            return this.modelGroupReader;
        } else {
            try {
                return ModelGroupReader.getModelGroupReader(delegatorName);
            } catch (GenericEntityException e) {
                Debug.logError(e, "Error loading entity group model", module);
                return null;
            }
        }
    }

    /** Gets the instance of ModelEntity that corresponds to this delegator and the specified entityName
     *@param entityName The name of the entity to get
     *@return ModelEntity that corresponds to this delegator and the specified entityName
     */
    public ModelEntity getModelEntity(String entityName) {
        try {
            return getModelReader().getModelEntity(entityName);
        } catch (GenericEntityException e) {
            Debug.logError(e, "Error getting entity definition from model", module);
            return null;
        }
    }

    /** Gets the helper name that corresponds to this delegator and the specified entityName
     *@param entityName The name of the entity to get the helper for
     *@return String with the helper name that corresponds to this delegator and the specified entityName
     */
    public String getEntityGroupName(String entityName) {
        String groupName = getModelGroupReader().getEntityGroupName(entityName);

        return groupName;
    }

    /** Gets a list of entity models that are in a group corresponding to the specified group name
     *@param groupName The name of the group
     *@return List of ModelEntity instances
     */
    public List getModelEntitiesByGroup(String groupName) {
        Iterator enames = UtilMisc.toIterator(getModelGroupReader().getEntityNamesByGroup(groupName));
        List entities = new LinkedList();

        if (enames == null || !enames.hasNext())
            return entities;
        while (enames.hasNext()) {
            String ename = (String) enames.next();
            ModelEntity entity = this.getModelEntity(ename);

            if (entity != null)
                entities.add(entity);
        }
        return entities;
    }

    /** Gets a Map of entity name & entity model pairs that are in the named group
     *@param groupName The name of the group
     *@return Map of entityName String keys and ModelEntity instance values
     */
    public Map getModelEntityMapByGroup(String groupName) {
        Iterator enames = UtilMisc.toIterator(getModelGroupReader().getEntityNamesByGroup(groupName));
        Map entities = new HashMap();

        if (enames == null || !enames.hasNext()) {
            return entities;
        }

        int errorCount = 0;
        while (enames.hasNext()) {
            String ename = (String) enames.next();
            try {
                ModelEntity entity = getModelReader().getModelEntity(ename);
                if (entity != null) {
                    entities.put(entity.getEntityName(), entity);
                } else {
                    throw new IllegalStateException("Programm Error: entity was null with name " + ename);
                }
            } catch (GenericEntityException ex) {
                errorCount++;
                Debug.logError("Entity " + ename + " named in Entity Group with name " + groupName + " are not defined in any Entity Definition file");
            }
        }

        if (errorCount > 0) {
            Debug.logError(errorCount + " entities were named in ModelGroup but not defined in any EntityModel");
        }

        return entities;
    }

    /** Gets the helper name that corresponds to this delegator and the specified entityName
     *@param groupName The name of the group to get the helper name for
     *@return String with the helper name that corresponds to this delegator and the specified entityName
     */
    public String getGroupHelperName(String groupName) {
        EntityConfigUtil.DelegatorInfo delegatorInfo = this.getDelegatorInfo();

        return (String) delegatorInfo.groupMap.get(groupName);
    }

    /** Gets the helper name that corresponds to this delegator and the specified entityName
     *@param entityName The name of the entity to get the helper name for
     *@return String with the helper name that corresponds to this delegator and the specified entityName
     */
    public String getEntityHelperName(String entityName) {
        String groupName = getModelGroupReader().getEntityGroupName(entityName);

        return this.getGroupHelperName(groupName);
    }

    /** Gets the helper name that corresponds to this delegator and the specified entity
     *@param entity The entity to get the helper for
     *@return String with the helper name that corresponds to this delegator and the specified entity
     */
    public String getEntityHelperName(ModelEntity entity) {
        if (entity == null)
            return null;
        return getEntityHelperName(entity.getEntityName());
    }

    /** Gets the an instance of helper that corresponds to this delegator and the specified entityName
     *@param entityName The name of the entity to get the helper for
     *@return GenericHelper that corresponds to this delegator and the specified entityName
     */
    public GenericHelper getEntityHelper(String entityName) throws GenericEntityException {
        String helperName = getEntityHelperName(entityName);

        if (helperName != null && helperName.length() > 0)
            return GenericHelperFactory.getHelper(helperName);
        else
            throw new GenericEntityException("Helper name not found for entity " + entityName);
    }

    /** Gets the an instance of helper that corresponds to this delegator and the specified entity
     *@param entity The entity to get the helper for
     *@return GenericHelper that corresponds to this delegator and the specified entity
     */
    public GenericHelper getEntityHelper(ModelEntity entity) throws GenericEntityException {
        return getEntityHelper(entity.getEntityName());
    }

    /** Gets a field type instance by name from the helper that corresponds to the specified entity
     *@param entity The entity
     *@param type The name of the type
     *@return ModelFieldType instance for the named type from the helper that corresponds to the specified entity
     */
    public ModelFieldType getEntityFieldType(ModelEntity entity, String type) throws GenericEntityException {
        String helperName = getEntityHelperName(entity);

        if (helperName == null || helperName.length() <= 0)
            return null;
        ModelFieldTypeReader modelFieldTypeReader = ModelFieldTypeReader.getModelFieldTypeReader(helperName);

        if (modelFieldTypeReader == null) {
            throw new GenericEntityException("ModelFieldTypeReader not found for entity " + entity.getEntityName() + " with helper name " + helperName);
        }
        return modelFieldTypeReader.getModelFieldType(type);
    }

    /** Gets field type names from the helper that corresponds to the specified entity
     *@param entity The entity
     *@return Collection of field type names from the helper that corresponds to the specified entity
     */
    public Collection getEntityFieldTypeNames(ModelEntity entity) throws GenericEntityException {
        String helperName = getEntityHelperName(entity);

        if (helperName == null || helperName.length() <= 0)
            return null;
        ModelFieldTypeReader modelFieldTypeReader = ModelFieldTypeReader.getModelFieldTypeReader(helperName);

        if (modelFieldTypeReader == null) {
            throw new GenericEntityException("ModelFieldTypeReader not found for entity " + entity.getEntityName() + " with helper name " + helperName);
        }
        return modelFieldTypeReader.getFieldTypeNames();
    }

    /** Creates a Entity in the form of a GenericValue without persisting it */
    public GenericValue makeValue(String entityName, Map fields) {
        ModelEntity entity = this.getModelEntity(entityName);

        if (entity == null) {
            throw new IllegalArgumentException("[GenericDelegator.makeValue] could not find entity for entityName: " + entityName);
        }
        GenericValue value = new GenericValue(entity, fields);

        value.setDelegator(this);
        return value;
    }

    /** Creates a Primary Key in the form of a GenericPK without persisting it */
    public GenericPK makePK(String entityName, Map fields) {
        ModelEntity entity = this.getModelEntity(entityName);

        if (entity == null) {
            throw new IllegalArgumentException("[GenericDelegator.makePK] could not find entity for entityName: " + entityName);
        }
        GenericPK pk = new GenericPK(entity, fields);

        pk.setDelegator(this);
        return pk;
    }

    /** Creates a Entity in the form of a GenericValue and write it to the database
     *@return GenericValue instance containing the new instance
     */
    public GenericValue create(String entityName, Map fields) throws GenericEntityException {
        if (entityName == null || fields == null) {
            return null;
        }
        ModelEntity entity = this.getModelReader().getModelEntity(entityName);
        GenericValue genericValue = new GenericValue(entity, fields);

        return this.create(genericValue, true);
    }

    /** Creates a Entity in the form of a GenericValue and write it to the datasource
     *@param value The GenericValue to create a value in the datasource from
     *@return GenericValue instance containing the new instance
     */
    public GenericValue create(GenericValue value) throws GenericEntityException {
        return this.create(value, true);
    }

    /** Creates a Entity in the form of a GenericValue and write it to the datasource
     *@param value The GenericValue to create a value in the datasource from
     *@param doCacheClear boolean that specifies whether or not to automatically clear cache entries related to this operation
     *@return GenericValue instance containing the new instance
     */
    public GenericValue create(GenericValue value, boolean doCacheClear) throws GenericEntityException {
        Map ecaEventMap = this.getEcaEntityEventMap(value.getEntityName());
        this.evalEcaRules(EntityEcaHandler.EV_VALIDATE, EntityEcaHandler.OP_CREATE, value, ecaEventMap, (ecaEventMap == null), false);

        if (value == null) {
            throw new IllegalArgumentException("Cannot create a null value");
        }
        GenericHelper helper = getEntityHelper(value.getEntityName());

        this.evalEcaRules(EntityEcaHandler.EV_RUN, EntityEcaHandler.OP_CREATE, value, ecaEventMap, (ecaEventMap == null), false);

        value.setDelegator(this);
        value = helper.create(value);

        if (value != null) {
            value.setDelegator(this);
            if (value.lockEnabled()) {
                refresh(value, doCacheClear);
            } else {
                if (doCacheClear) {
                    this.evalEcaRules(EntityEcaHandler.EV_CACHE_CLEAR, EntityEcaHandler.OP_CREATE, value, ecaEventMap, (ecaEventMap == null), false);
                    this.clearCacheLine(value);
                }
            }
        }
        this.evalEcaRules(EntityEcaHandler.EV_RETURN, EntityEcaHandler.OP_CREATE, value, ecaEventMap, (ecaEventMap == null), false);
        return value;
    }

    /** Creates a Entity in the form of a GenericValue and write it to the datasource
     *@param primaryKey The GenericPK to create a value in the datasource from
     *@return GenericValue instance containing the new instance
     */
    public GenericValue create(GenericPK primaryKey) throws GenericEntityException {
        return this.create(primaryKey, true);
    }

    /** Creates a Entity in the form of a GenericValue and write it to the datasource
     *@param primaryKey The GenericPK to create a value in the datasource from
     *@param doCacheClear boolean that specifies whether to clear related cache entries for this primaryKey to be created
     *@return GenericValue instance containing the new instance
     */
    public GenericValue create(GenericPK primaryKey, boolean doCacheClear) throws GenericEntityException {
        if (primaryKey == null) {
            throw new IllegalArgumentException("Cannot create from a null primaryKey");
        }

        return this.create(new GenericValue(primaryKey), doCacheClear);
    }

    /** Find a Generic Entity by its Primary Key
     *@param primaryKey The primary key to find by.
     *@return The GenericValue corresponding to the primaryKey
     */
    public GenericValue findByPrimaryKey(GenericPK primaryKey) throws GenericEntityException {
        Map ecaEventMap = this.getEcaEntityEventMap(primaryKey.getEntityName());
        this.evalEcaRules(EntityEcaHandler.EV_VALIDATE, EntityEcaHandler.OP_FIND, primaryKey, ecaEventMap, (ecaEventMap == null), false);

        GenericHelper helper = getEntityHelper(primaryKey.getEntityName());
        GenericValue value = null;

        if (!primaryKey.isPrimaryKey()) {
            throw new IllegalArgumentException("[GenericDelegator.findByPrimaryKey] Passed primary key is not a valid primary key: " + primaryKey);
        }
        this.evalEcaRules(EntityEcaHandler.EV_RUN, EntityEcaHandler.OP_FIND, primaryKey, ecaEventMap, (ecaEventMap == null), false);
        try {
            value = helper.findByPrimaryKey(primaryKey);
        } catch (GenericEntityNotFoundException e) {
            value = null;
        }
        if (value != null) value.setDelegator(this);

        this.evalEcaRules(EntityEcaHandler.EV_RETURN, EntityEcaHandler.OP_FIND, primaryKey, ecaEventMap, (ecaEventMap == null), false);
        return value;
    }

    /** Find a CACHED Generic Entity by its Primary Key
     *@param primaryKey The primary key to find by.
     *@return The GenericValue corresponding to the primaryKey
     */
    public GenericValue findByPrimaryKeyCache(GenericPK primaryKey) throws GenericEntityException {
        Map ecaEventMap = this.getEcaEntityEventMap(primaryKey.getEntityName());
        this.evalEcaRules(EntityEcaHandler.EV_CACHE_CHECK, EntityEcaHandler.OP_FIND, primaryKey, ecaEventMap, (ecaEventMap == null), false);

        GenericValue value = this.getFromPrimaryKeyCache(primaryKey);
        if (value == null) {
            value = findByPrimaryKey(primaryKey);
            if (value != null) {
                this.evalEcaRules(EntityEcaHandler.EV_CACHE_PUT, EntityEcaHandler.OP_FIND, primaryKey, ecaEventMap, (ecaEventMap == null), false);
                this.putInPrimaryKeyCache(primaryKey, value);
            }
        }
        return value;
    }

    /** Find a Generic Entity by its Primary Key
     *@param entityName The Name of the Entity as defined in the entity XML file
     *@param fields The fields of the named entity to query by with their corresponging values
     *@return The GenericValue corresponding to the primaryKey
     */
    public GenericValue findByPrimaryKey(String entityName, Map fields) throws GenericEntityException {
        return findByPrimaryKey(makePK(entityName, fields));
    }

    /** Find a CACHED Generic Entity by its Primary Key
     *@param entityName The Name of the Entity as defined in the entity XML file
     *@param fields The fields of the named entity to query by with their corresponging values
     *@return The GenericValue corresponding to the primaryKey
     */
    public GenericValue findByPrimaryKeyCache(String entityName, Map fields) throws GenericEntityException {
        return findByPrimaryKeyCache(makePK(entityName, fields));
    }

    /** Find a Generic Entity by its Primary Key and only returns the values requested by the passed keys (names)
     *@param primaryKey The primary key to find by.
     *@param keys The keys, or names, of the values to retrieve; only these values will be retrieved
     *@return The GenericValue corresponding to the primaryKey
     */
    public GenericValue findByPrimaryKeyPartial(GenericPK primaryKey, Set keys) throws GenericEntityException {
        Map ecaEventMap = this.getEcaEntityEventMap(primaryKey.getEntityName());
        this.evalEcaRules(EntityEcaHandler.EV_VALIDATE, EntityEcaHandler.OP_FIND, primaryKey, ecaEventMap, (ecaEventMap == null), false);

        GenericHelper helper = getEntityHelper(primaryKey.getEntityName());
        GenericValue value = null;

        if (!primaryKey.isPrimaryKey()) {
            throw new IllegalArgumentException("[GenericDelegator.findByPrimaryKey] Passed primary key is not a valid primary key: " + primaryKey);
        }

        this.evalEcaRules(EntityEcaHandler.EV_RUN, EntityEcaHandler.OP_FIND, primaryKey, ecaEventMap, (ecaEventMap == null), false);
        try {
            value = helper.findByPrimaryKeyPartial(primaryKey, keys);
        } catch (GenericEntityNotFoundException e) {
            value = null;
        }
        if (value != null) value.setDelegator(this);

        this.evalEcaRules(EntityEcaHandler.EV_RETURN, EntityEcaHandler.OP_FIND, primaryKey, ecaEventMap, (ecaEventMap == null), false);
        return value;
    }

    /** Find a number of Generic Value objects by their Primary Keys, all at once
     *@param primaryKeys A Collection of primary keys to find by.
     *@return List of GenericValue objects corresponding to the passed primaryKey objects
     */
    public List findAllByPrimaryKeys(Collection primaryKeys) throws GenericEntityException {
        //TODO: add eca eval calls
        if (primaryKeys == null) return null;
        List results = new LinkedList();

        // from the delegator level this is complicated because different GenericPK
        // objects in the list may correspond to different helpers
        HashMap pksPerHelper = new HashMap();
        Iterator pkiter = primaryKeys.iterator();

        while (pkiter.hasNext()) {
            GenericPK curPK = (GenericPK) pkiter.next();
            String helperName = this.getEntityHelperName(curPK.getEntityName());
            List pks = (List) pksPerHelper.get(helperName);

            if (pks == null) {
                pks = new LinkedList();
                pksPerHelper.put(helperName, pks);
            }
            pks.add(curPK);
        }

        Iterator helperIter = pksPerHelper.entrySet().iterator();

        while (helperIter.hasNext()) {
            Map.Entry curEntry = (Map.Entry) helperIter.next();
            String helperName = (String) curEntry.getKey();
            GenericHelper helper = GenericHelperFactory.getHelper(helperName);
            List values = helper.findAllByPrimaryKeys((List) curEntry.getValue());

            results.addAll(values);
        }
        return results;
    }

    /** Find a number of Generic Value objects by their Primary Keys, all at once;
     *  this first looks in the local cache for each PK and if there then it puts it
     *  in the return list rather than putting it in the batch to send to
     *  a given helper.
     *@param primaryKeys A Collection of primary keys to find by.
     *@return List of GenericValue objects corresponding to the passed primaryKey objects
     */
    public List findAllByPrimaryKeysCache(Collection primaryKeys) throws GenericEntityException {
        //TODO: add eca eval calls
        if (primaryKeys == null)
            return null;
        List results = new LinkedList();

        // from the delegator level this is complicated because different GenericPK
        // objects in the list may correspond to different helpers
        HashMap pksPerHelper = new HashMap();
        Iterator pkiter = primaryKeys.iterator();

        while (pkiter.hasNext()) {
            GenericPK curPK = (GenericPK) pkiter.next();

            GenericValue value = this.getFromPrimaryKeyCache(curPK);

            if (value != null) {
                // it is in the cache, so just put the cached value in the results
                results.add(value);
            } else {
                // is not in the cache, so put in a list for a call to the helper
                String helperName = this.getEntityHelperName(curPK.getEntityName());
                List pks = (List) pksPerHelper.get(helperName);

                if (pks == null) {
                    pks = new LinkedList();
                    pksPerHelper.put(helperName, pks);
                }
                pks.add(curPK);
            }
        }

        Iterator helperIter = pksPerHelper.entrySet().iterator();

        while (helperIter.hasNext()) {
            Map.Entry curEntry = (Map.Entry) helperIter.next();
            String helperName = (String) curEntry.getKey();
            GenericHelper helper = GenericHelperFactory.getHelper(helperName);
            List values = helper.findAllByPrimaryKeys((List) curEntry.getValue());

            this.putAllInPrimaryKeyCache(values);
            results.addAll(values);
        }
        return results;
    }

    /** Finds all Generic entities
     *@param entityName The Name of the Entity as defined in the entity XML file
     *@return    List containing all Generic entities
     */
    public List findAll(String entityName) throws GenericEntityException {
        return this.findByAnd(entityName, new HashMap(), null);
    }

    /** Finds all Generic entities
     *@param entityName The Name of the Entity as defined in the entity XML file
     *@param orderBy The fields of the named entity to order the query by; optionally add a " ASC" for ascending or " DESC" for descending
     *@return    List containing all Generic entities
     */
    public List findAll(String entityName, List orderBy) throws GenericEntityException {
        return this.findByAnd(entityName, new HashMap(), orderBy);
    }

    /** Finds all Generic entities, looking first in the cache
     *@param entityName The Name of the Entity as defined in the entity XML file
     *@return    List containing all Generic entities
     */
    public List findAllCache(String entityName) throws GenericEntityException {
        return this.findAllCache(entityName, null);
    }

    /** Finds all Generic entities, looking first in the cache; uses orderBy for lookup, but only keys results on the entityName and fields
     *@param entityName The Name of the Entity as defined in the entity XML file
     *@param orderBy The fields of the named entity to order the query by; optionally add a " ASC" for ascending or " DESC" for descending
     *@return    List containing all Generic entities
     */
    public List findAllCache(String entityName, List orderBy) throws GenericEntityException {
        GenericValue dummyValue = makeValue(entityName, null);
        Map ecaEventMap = this.getEcaEntityEventMap(entityName);
        this.evalEcaRules(EntityEcaHandler.EV_CACHE_CHECK, EntityEcaHandler.OP_FIND, dummyValue, ecaEventMap, (ecaEventMap == null), false);

        List lst = this.getFromAllCache(entityName);

        if (lst == null) {
            lst = findAll(entityName, orderBy);
            if (lst != null) {
                this.evalEcaRules(EntityEcaHandler.EV_CACHE_PUT, EntityEcaHandler.OP_FIND, dummyValue, ecaEventMap, (ecaEventMap == null), false);
                this.putInAllCache(entityName, lst);
            }
        }
        return lst;
    }

    /** Finds Generic Entity records by all of the specified fields (ie: combined using AND)
     * @param entityName The Name of the Entity as defined in the entity XML file
     * @param fields The fields of the named entity to query by with their corresponging values
     * @return List of GenericValue instances that match the query
     */
    public List findByAnd(String entityName, Map fields) throws GenericEntityException {
        return this.findByAnd(entityName, fields, null);
    }

    /** Finds Generic Entity records by all of the specified fields (ie: combined using OR)
     * @param entityName The Name of the Entity as defined in the entity XML file
     * @param fields The fields of the named entity to query by with their corresponging values
     * @return List of GenericValue instances that match the query
     */
    public List findByOr(String entityName, Map fields) throws GenericEntityException {
        return this.findByOr(entityName, fields, null);
    }

    /** Finds Generic Entity records by all of the specified fields (ie: combined using AND)
     * @param entityName The Name of the Entity as defined in the entity XML file
     * @param fields The fields of the named entity to query by with their corresponging values
     * @param orderBy The fields of the named entity to order the query by;
     *      optionally add a " ASC" for ascending or " DESC" for descending
     * @return List of GenericValue instances that match the query
     */
    public List findByAnd(String entityName, Map fields, List orderBy) throws GenericEntityException {
        ModelEntity modelEntity = getModelReader().getModelEntity(entityName);
        GenericValue dummyValue = new GenericValue(modelEntity, fields);
        this.evalEcaRules(EntityEcaHandler.EV_VALIDATE, EntityEcaHandler.OP_FIND, dummyValue, null, false, false);
        return findByAnd(modelEntity, fields, orderBy);
    }

    public List findByAnd(ModelEntity modelEntity, Map fields, List orderBy) throws GenericEntityException {
        GenericValue dummyValue = new GenericValue(modelEntity);
        Map ecaEventMap = this.getEcaEntityEventMap(modelEntity.getEntityName());

        GenericHelper helper = getEntityHelper(modelEntity);

        if (fields != null && !modelEntity.areFields(fields.keySet())) {
            throw new GenericModelException("At least one of the passed fields is not valid: " + fields.keySet().toString());
        }

        this.evalEcaRules(EntityEcaHandler.EV_RUN, EntityEcaHandler.OP_FIND, dummyValue, ecaEventMap, (ecaEventMap == null), false);
        List list = null;
        list = helper.findByAnd(modelEntity, fields, orderBy);
        absorbList(list);

        this.evalEcaRules(EntityEcaHandler.EV_RETURN, EntityEcaHandler.OP_FIND, dummyValue, ecaEventMap, (ecaEventMap == null), false);
        return list;
    }

    /** Finds Generic Entity records by all of the specified fields (ie: combined using OR)
     * @param entityName The Name of the Entity as defined in the entity XML file
     * @param fields The fields of the named entity to query by with their corresponging values
     * @param orderBy The fields of the named entity to order the query by;
     *      optionally add a " ASC" for ascending or " DESC" for descending
     * @return List of GenericValue instances that match the query
     */
    public List findByOr(String entityName, Map fields, List orderBy) throws GenericEntityException {
        ModelEntity modelEntity = getModelReader().getModelEntity(entityName);
        GenericValue dummyValue = new GenericValue(modelEntity);
        Map ecaEventMap = this.getEcaEntityEventMap(modelEntity.getEntityName());
        this.evalEcaRules(EntityEcaHandler.EV_VALIDATE, EntityEcaHandler.OP_FIND, dummyValue, null, false, false);

        GenericHelper helper = getEntityHelper(entityName);

        if (fields != null && !modelEntity.areFields(fields.keySet())) {
            throw new IllegalArgumentException("[GenericDelegator.findByOr] At least of the passed fields is not valid: " + fields.keySet().toString());
        }

        this.evalEcaRules(EntityEcaHandler.EV_RUN, EntityEcaHandler.OP_FIND, dummyValue, ecaEventMap, (ecaEventMap == null), false);
        List list = null;
        list = helper.findByOr(modelEntity, fields, orderBy);
        absorbList(list);

        this.evalEcaRules(EntityEcaHandler.EV_RETURN, EntityEcaHandler.OP_FIND, dummyValue, ecaEventMap, (ecaEventMap == null), false);
        return list;
    }

    /** Finds Generic Entity records by all of the specified fields (ie: combined using AND), looking first in the cache; uses orderBy for lookup, but only keys results on the entityName and fields
     *@param entityName The Name of the Entity as defined in the entity XML file
     *@param fields The fields of the named entity to query by with their corresponging values
     *@return List of GenericValue instances that match the query
     */
    public List findByAndCache(String entityName, Map fields) throws GenericEntityException {
        return this.findByAndCache(entityName, fields, null);
    }

    /** Finds Generic Entity records by all of the specified fields (ie: combined using AND), looking first in the cache; uses orderBy for lookup, but only keys results on the entityName and fields
     *@param entityName The Name of the Entity as defined in the entity XML file
     *@param fields The fields of the named entity to query by with their corresponging values
     *@param orderBy The fields of the named entity to order the query by; optionally add a " ASC" for ascending or " DESC" for descending
     *@return List of GenericValue instances that match the query
     */
    public List findByAndCache(String entityName, Map fields, List orderBy) throws GenericEntityException {
        ModelEntity modelEntity = getModelReader().getModelEntity(entityName);
        GenericValue dummyValue = new GenericValue(modelEntity);
        Map ecaEventMap = this.getEcaEntityEventMap(modelEntity.getEntityName());

        this.evalEcaRules(EntityEcaHandler.EV_CACHE_CHECK, EntityEcaHandler.OP_FIND, dummyValue, null, false, false);
        List lst = this.getFromAndCache(modelEntity, fields);

        if (lst == null) {
            lst = findByAnd(modelEntity, fields, orderBy);
            if (lst != null) {
                this.evalEcaRules(EntityEcaHandler.EV_CACHE_PUT, EntityEcaHandler.OP_FIND, dummyValue, null, false, false);
                this.putInAndCache(modelEntity, fields, lst);
            }
        }
        return lst;
    }

    /** Finds Generic Entity records by all of the specified expressions (ie: combined using AND)
     *@param entityName The Name of the Entity as defined in the entity XML file
     *@param expressions The expressions to use for the lookup, each consisting of at least a field name, an EntityOperator, and a value to compare to
     *@return List of GenericValue instances that match the query
     */
    public List findByAnd(String entityName, List expressions) throws GenericEntityException {
        EntityConditionList ecl = new EntityConditionList(expressions, EntityOperator.AND);
        return findByCondition(entityName, ecl, null, null);
    }

    /** Finds Generic Entity records by all of the specified expressions (ie: combined using AND)
     *@param entityName The Name of the Entity as defined in the entity XML file
     *@param expressions The expressions to use for the lookup, each consisting of at least a field name, an EntityOperator, and a value to compare to
     *@param orderBy The fields of the named entity to order the query by; optionally add a " ASC" for ascending or " DESC" for descending
     *@return List of GenericValue instances that match the query
     */
    public List findByAnd(String entityName, List expressions, List orderBy) throws GenericEntityException {
        EntityConditionList ecl = new EntityConditionList(expressions, EntityOperator.AND);
        return findByCondition(entityName, ecl, null, orderBy);
    }

    /** Finds Generic Entity records by all of the specified expressions (ie: combined using OR)
     *@param entityName The Name of the Entity as defined in the entity XML file
     *@param expressions The expressions to use for the lookup, each consisting of at least a field name, an EntityOperator, and a value to compare to
     *@return List of GenericValue instances that match the query
     */
    public List findByOr(String entityName, List expressions) throws GenericEntityException {
        EntityConditionList ecl = new EntityConditionList(expressions, EntityOperator.OR);
        return findByCondition(entityName, ecl, null, null);
    }

    /** Finds Generic Entity records by all of the specified expressions (ie: combined using OR)
     *@param entityName The Name of the Entity as defined in the entity XML file
     *@param expressions The expressions to use for the lookup, each consisting of at least a field name, an EntityOperator, and a value to compare to
     *@param orderBy The fields of the named entity to order the query by; optionally add a " ASC" for ascending or " DESC" for descending
     *@return List of GenericValue instances that match the query
     */
    public List findByOr(String entityName, List expressions, List orderBy) throws GenericEntityException {
        EntityConditionList ecl = new EntityConditionList(expressions, EntityOperator.OR);
        return findByCondition(entityName, ecl, null, orderBy);
    }

    public List findByLike(String entityName, Map fields) throws GenericEntityException {
        return findByLike(entityName, fields, null);
    }

    public List findByLike(String entityName, Map fields, List orderBy) throws GenericEntityException {
        List likeExpressions = new LinkedList();
        if (fields != null) {
            Iterator fieldEntries = fields.entrySet().iterator();
            while (fieldEntries.hasNext()) {
                Map.Entry fieldEntry = (Map.Entry) fieldEntries.next();
                likeExpressions.add(new EntityExpr((String) fieldEntry.getKey(), EntityOperator.LIKE, fieldEntry.getValue()));
            }
        }
        EntityConditionList ecl = new EntityConditionList(likeExpressions, EntityOperator.AND);
        return findByCondition(entityName, ecl, null, orderBy);
    }

/* tentatively removing by clause methods, unless there are really big complaints... because it is a kludge
    public List findByClause(String entityName, List entityClauses, Map fields) throws GenericEntityException {
        return findByClause(entityName, entityClauses, fields, null);
    }

    public List findByClause(String entityName, List entityClauses, Map fields, List orderBy) throws GenericEntityException {
        //TODO: add eca eval calls
        if (entityClauses == null) return null;
        ModelEntity modelEntity = getModelReader().getModelEntity(entityName);
        GenericHelper helper = getEntityHelper(entityName);

        for (int i = 0; i < entityClauses.size(); i++) {
            EntityClause genEntityClause = (EntityClause) entityClauses.get(i);
            genEntityClause.setModelEntities(getModelReader());
        }

        List list = null;
        list = helper.findByClause(modelEntity, entityClauses, fields, orderBy);
        absorbList(list);
        return list;
    }
*/

    /** Finds GenericValues by the conditions specified in the EntityCondition object, the the EntityCondition javadoc for more details.
     *@param entityName The Name of the Entity as defined in the entity model XML file
     *@param entityCondition The EntityCondition object that specifies how to constrain this query
     *@param fieldsToSelect The fields of the named entity to get from the database; if empty or null all fields will be retreived
     *@param orderBy The fields of the named entity to order the query by; optionally add a " ASC" for ascending or " DESC" for descending
     *@return List of GenericValue objects representing the result
     */
    public List findByCondition(String entityName, EntityCondition entityCondition, Collection fieldsToSelect, List orderBy) throws GenericEntityException {
        ModelEntity modelEntity = getModelReader().getModelEntity(entityName);
        GenericValue dummyValue = new GenericValue(modelEntity);
        Map ecaEventMap = this.getEcaEntityEventMap(entityName);

        this.evalEcaRules(EntityEcaHandler.EV_VALIDATE, EntityEcaHandler.OP_FIND, dummyValue, ecaEventMap, (ecaEventMap == null), false);
        if (entityCondition != null) entityCondition.checkCondition(modelEntity);

        this.evalEcaRules(EntityEcaHandler.EV_RUN, EntityEcaHandler.OP_FIND, dummyValue, ecaEventMap, (ecaEventMap == null), false);
        GenericHelper helper = getEntityHelper(entityName);
        List list = null;
        list = helper.findByCondition(modelEntity, entityCondition, fieldsToSelect, orderBy);

        this.evalEcaRules(EntityEcaHandler.EV_RETURN, EntityEcaHandler.OP_FIND, dummyValue, ecaEventMap, (ecaEventMap == null), false);
        absorbList(list);

        return list;
    }

    /** Finds GenericValues by the conditions specified in the EntityCondition object, the the EntityCondition javadoc for more details.
     *@param entityName The Name of the Entity as defined in the entity model XML file
     *@param entityCondition The EntityCondition object that specifies how to constrain this query before any groupings are done (if this is a view entity with group-by aliases)
     *@param fieldsToSelect The fields of the named entity to get from the database; if empty or null all fields will be retreived
     *@param orderBy The fields of the named entity to order the query by; optionally add a " ASC" for ascending or " DESC" for descending
     *@return EntityListIterator representing the result of the query: NOTE THAT THIS MUST BE CLOSED WHEN YOU ARE
     *      DONE WITH IT, AND DON'T LEAVE IT OPEN TOO LONG BEACUSE IT WILL MAINTAIN A DATABASE CONNECTION.
     */
    public EntityListIterator findListIteratorByCondition(String entityName, EntityCondition entityCondition,
        Collection fieldsToSelect, List orderBy) throws GenericEntityException {
        return this.findListIteratorByCondition(entityName, entityCondition, null, fieldsToSelect, orderBy, null);
    }

    /** Finds GenericValues by the conditions specified in the EntityCondition object, the the EntityCondition javadoc for more details.
     *@param entityName The ModelEntity of the Entity as defined in the entity XML file
     *@param whereEntityCondition The EntityCondition object that specifies how to constrain this query before any groupings are done (if this is a view entity with group-by aliases)
     *@param havingEntityCondition The EntityCondition object that specifies how to constrain this query after any groupings are done (if this is a view entity with group-by aliases)
     *@param fieldsToSelect The fields of the named entity to get from the database; if empty or null all fields will be retreived
     *@param orderBy The fields of the named entity to order the query by; optionally add a " ASC" for ascending or " DESC" for descending
     *@param findOptions An instance of EntityFindOptions that specifies advanced query options. See the EntityFindOptions JavaDoc for more details.
     *@return EntityListIterator representing the result of the query: NOTE THAT THIS MUST BE CLOSED WHEN YOU ARE
     *      DONE WITH IT, AND DON'T LEAVE IT OPEN TOO LONG BEACUSE IT WILL MAINTAIN A DATABASE CONNECTION.
     */
    public EntityListIterator findListIteratorByCondition(String entityName, EntityCondition whereEntityCondition,
            EntityCondition havingEntityCondition, Collection fieldsToSelect, List orderBy, EntityFindOptions findOptions)
            throws GenericEntityException {

        ModelEntity modelEntity = getModelReader().getModelEntity(entityName);
        GenericValue dummyValue = new GenericValue(modelEntity);
        Map ecaEventMap = this.getEcaEntityEventMap(entityName);
        this.evalEcaRules(EntityEcaHandler.EV_VALIDATE, EntityEcaHandler.OP_FIND, dummyValue, ecaEventMap, (ecaEventMap == null), false);

        if (whereEntityCondition != null) whereEntityCondition.checkCondition(modelEntity);
        if (havingEntityCondition != null) havingEntityCondition.checkCondition(modelEntity);

        this.evalEcaRules(EntityEcaHandler.EV_RUN, EntityEcaHandler.OP_FIND, dummyValue, ecaEventMap, (ecaEventMap == null), false);
        GenericHelper helper = getEntityHelper(entityName);
        EntityListIterator eli = helper.findListIteratorByCondition(modelEntity, whereEntityCondition,
                havingEntityCondition, fieldsToSelect, orderBy, findOptions);
        eli.setDelegator(this);

        this.evalEcaRules(EntityEcaHandler.EV_RETURN, EntityEcaHandler.OP_FIND, dummyValue, ecaEventMap, (ecaEventMap == null), false);
        return eli;
    }

    /** Remove a Generic Entity corresponding to the primaryKey
     *@param primaryKey  The primary key of the entity to remove.
     *@return int representing number of rows effected by this operation
     */
    public int removeByPrimaryKey(GenericPK primaryKey) throws GenericEntityException {
        return this.removeByPrimaryKey(primaryKey, true);
    }

    /** Remove a Generic Entity corresponding to the primaryKey
     *@param primaryKey  The primary key of the entity to remove.
     *@param doCacheClear boolean that specifies whether to clear cache entries for this primaryKey to be removed
     *@return int representing number of rows effected by this operation
     */
    public int removeByPrimaryKey(GenericPK primaryKey, boolean doCacheClear) throws GenericEntityException {
        Map ecaEventMap = this.getEcaEntityEventMap(primaryKey.getEntityName());
        this.evalEcaRules(EntityEcaHandler.EV_VALIDATE, EntityEcaHandler.OP_REMOVE, primaryKey, ecaEventMap, (ecaEventMap == null), false);

        GenericHelper helper = getEntityHelper(primaryKey.getEntityName());

        if (doCacheClear) {
            // always clear cache before the operation
            this.evalEcaRules(EntityEcaHandler.EV_CACHE_CLEAR, EntityEcaHandler.OP_REMOVE, primaryKey, ecaEventMap, (ecaEventMap == null), false);
            this.clearCacheLine(primaryKey);
        }

        this.evalEcaRules(EntityEcaHandler.EV_RUN, EntityEcaHandler.OP_REMOVE, primaryKey, ecaEventMap, (ecaEventMap == null), false);
        int num = helper.removeByPrimaryKey(primaryKey);

        this.evalEcaRules(EntityEcaHandler.EV_RETURN, EntityEcaHandler.OP_REMOVE, primaryKey, ecaEventMap, (ecaEventMap == null), false);
        return num;
    }

    /** Remove a Generic Value from the database
     *@param value The GenericValue object of the entity to remove.
     *@return int representing number of rows effected by this operation
     */
    public int removeValue(GenericValue value) throws GenericEntityException {
        return this.removeValue(value, true);
    }

    /** Remove a Generic Value from the database
     *@param value The GenericValue object of the entity to remove.
     *@param doCacheClear boolean that specifies whether to clear cache entries for this value to be removed
     *@return int representing number of rows effected by this operation
     */
    public int removeValue(GenericValue value, boolean doCacheClear) throws GenericEntityException {
        Map ecaEventMap = this.getEcaEntityEventMap(value.getEntityName());
        this.evalEcaRules(EntityEcaHandler.EV_VALIDATE, EntityEcaHandler.OP_REMOVE, value, ecaEventMap, (ecaEventMap == null), false);

        GenericHelper helper = getEntityHelper(value.getEntityName());

        if (doCacheClear) {
            this.evalEcaRules(EntityEcaHandler.EV_CACHE_CLEAR, EntityEcaHandler.OP_REMOVE, value, ecaEventMap, (ecaEventMap == null), false);
            this.clearCacheLine(value);
        }

        this.evalEcaRules(EntityEcaHandler.EV_RUN, EntityEcaHandler.OP_REMOVE, value, ecaEventMap, (ecaEventMap == null), false);
        int num = helper.removeByPrimaryKey(value.getPrimaryKey());

        this.evalEcaRules(EntityEcaHandler.EV_RETURN, EntityEcaHandler.OP_REMOVE, value, ecaEventMap, (ecaEventMap == null), false);
        return num;
    }

    /** Removes/deletes Generic Entity records found by all of the specified fields (ie: combined using AND)
     *@param entityName The Name of the Entity as defined in the entity XML file
     *@param fields The fields of the named entity to query by with their corresponging values
     *@return int representing number of rows effected by this operation
     */
    public int removeByAnd(String entityName, Map fields) throws GenericEntityException {
        return this.removeByAnd(entityName, fields, true);
    }

    /** Removes/deletes Generic Entity records found by all of the specified fields (ie: combined using AND)
     *@param entityName The Name of the Entity as defined in the entity XML file
     *@param fields The fields of the named entity to query by with their corresponging values
     *@param doCacheClear boolean that specifies whether to clear cache entries for this value to be removed
     *@return int representing number of rows effected by this operation
     */
    public int removeByAnd(String entityName, Map fields, boolean doCacheClear) throws GenericEntityException {
        GenericValue dummyValue = makeValue(entityName, fields);

        Map ecaEventMap = this.getEcaEntityEventMap(entityName);
        this.evalEcaRules(EntityEcaHandler.EV_VALIDATE, EntityEcaHandler.OP_REMOVE, dummyValue, ecaEventMap, (ecaEventMap == null), false);

        ModelEntity modelEntity = getModelReader().getModelEntity(entityName);
        GenericHelper helper = getEntityHelper(entityName);

        if (doCacheClear) {
            // always clear cache before the operation
            this.evalEcaRules(EntityEcaHandler.EV_CACHE_CLEAR, EntityEcaHandler.OP_REMOVE, dummyValue, ecaEventMap, (ecaEventMap == null), false);
            this.clearCacheLine(entityName, fields);
        }

        this.evalEcaRules(EntityEcaHandler.EV_RUN, EntityEcaHandler.OP_REMOVE, dummyValue, ecaEventMap, (ecaEventMap == null), false);
        int num = helper.removeByAnd(modelEntity, dummyValue.getAllFields());

        this.evalEcaRules(EntityEcaHandler.EV_RETURN, EntityEcaHandler.OP_REMOVE, dummyValue, ecaEventMap, (ecaEventMap == null), false);
        return num;
    }

    /**
     * Get the named Related Entity for the GenericValue from the persistent store across another Relation.
     * Helps to get related Values in a multi-to-multi relationship.
     * @param relationNameOne String containing the relation name which is the
     *      combination of relation.title and relation.rel-entity-name as
     *      specified in the entity XML definition file, for first relation
     * @param relationNameTwo String containing the relation name for second relation
     * @param value GenericValue instance containing the entity
     * @param orderBy The fields of the named entity to order the query by; may be null;
     *      optionally add a " ASC" for ascending or " DESC" for descending
     * @return List of GenericValue instances as specified in the relation definition
     */
    public List getMultiRelation(GenericValue value, String relationNameOne, String relationNameTwo, List orderBy) throws GenericEntityException {
        //TODO: add eca eval calls
        // traverse the relationships
        ModelEntity modelEntity = value.getModelEntity();
        ModelRelation modelRelationOne = modelEntity.getRelation(relationNameOne);
        ModelEntity modelEntityOne = getModelEntity(modelRelationOne.getRelEntityName());
        ModelRelation modelRelationTwo = modelEntityOne.getRelation(relationNameTwo);
        ModelEntity modelEntityTwo = getModelEntity(modelRelationTwo.getRelEntityName());

        GenericHelper helper = getEntityHelper(modelEntity);

        return helper.findByMultiRelation(value, modelRelationOne, modelEntityOne, modelRelationTwo, modelEntityTwo, orderBy);
    }

    /**
     * Get the named Related Entity for the GenericValue from the persistent store across another Relation.
     * Helps to get related Values in a multi-to-multi relationship.
     * @param relationNameOne String containing the relation name which is the
     *      combination of relation.title and relation.rel-entity-name as
     *      specified in the entity XML definition file, for first relation
     * @param relationNameTwo String containing the relation name for second relation
     * @param value GenericValue instance containing the entity
     * @return List of GenericValue instances as specified in the relation definition
     */
    public List getMultiRelation(GenericValue value, String relationNameOne, String relationNameTwo) throws GenericEntityException {
        return getMultiRelation(value, relationNameOne, relationNameTwo, null);
    }

    /** Get the named Related Entity for the GenericValue from the persistent store
     * @param relationName String containing the relation name which is the
     *      combination of relation.title and relation.rel-entity-name as
     *      specified in the entity XML definition file
     * @param value GenericValue instance containing the entity
     * @return List of GenericValue instances as specified in the relation definition
     */
    public List getRelated(String relationName, GenericValue value) throws GenericEntityException {
        return getRelated(relationName, null, null, value);
    }

    /** Get the named Related Entity for the GenericValue from the persistent store
     * @param relationName String containing the relation name which is the
     *      combination of relation.title and relation.rel-entity-name as
     *      specified in the entity XML definition file
     * @param byAndFields the fields that must equal in order to keep; may be null
     * @param value GenericValue instance containing the entity
     * @return List of GenericValue instances as specified in the relation definition
     */
    public List getRelatedByAnd(String relationName, Map byAndFields, GenericValue value) throws GenericEntityException {
        return this.getRelated(relationName, byAndFields, null, value);
    }

    /** Get the named Related Entity for the GenericValue from the persistent store
     * @param relationName String containing the relation name which is the
     *      combination of relation.title and relation.rel-entity-name as
     *      specified in the entity XML definition file
     * @param orderBy The fields of the named entity to order the query by; may be null;
     *      optionally add a " ASC" for ascending or " DESC" for descending
     * @param value GenericValue instance containing the entity
     * @return List of GenericValue instances as specified in the relation definition
     */
    public List getRelatedOrderBy(String relationName, List orderBy, GenericValue value) throws GenericEntityException {
        return this.getRelated(relationName, null, orderBy, value);
    }

    /** Get the named Related Entity for the GenericValue from the persistent store
     * @param relationName String containing the relation name which is the
     *      combination of relation.title and relation.rel-entity-name as
     *      specified in the entity XML definition file
     * @param byAndFields the fields that must equal in order to keep; may be null
     * @param orderBy The fields of the named entity to order the query by; may be null;
     *      optionally add a " ASC" for ascending or " DESC" for descending
     * @param value GenericValue instance containing the entity
     * @return List of GenericValue instances as specified in the relation definition
     */
    public List getRelated(String relationName, Map byAndFields, List orderBy, GenericValue value) throws GenericEntityException {
        ModelEntity modelEntity = value.getModelEntity();
        ModelRelation relation = modelEntity.getRelation(relationName);

        if (relation == null) {
            throw new GenericModelException("Could not find relation for relationName: " + relationName + " for value " + value);
        }

        // put the byAndFields (if not null) into the hash map first,
        // they will be overridden by value's fields if over-specified this is important for security and cleanliness
        Map fields = byAndFields == null ? new HashMap() : new HashMap(byAndFields);
        for (int i = 0; i < relation.getKeyMapsSize(); i++) {
            ModelKeyMap keyMap = relation.getKeyMap(i);
            fields.put(keyMap.getRelFieldName(), value.get(keyMap.getFieldName()));
        }

        return this.findByAnd(relation.getRelEntityName(), fields, orderBy);
    }

    /** Get a dummy primary key for the named Related Entity for the GenericValue
     * @param relationName String containing the relation name which is the
     *      combination of relation.title and relation.rel-entity-name as
     *      specified in the entity XML definition file
     * @param byAndFields the fields that must equal in order to keep; may be null
     * @param value GenericValue instance containing the entity
     * @return GenericPK containing a possibly incomplete PrimaryKey object representing the related entity or entities
     */
    public GenericPK getRelatedDummyPK(String relationName, Map byAndFields, GenericValue value) throws GenericEntityException {
        ModelEntity modelEntity = value.getModelEntity();
        ModelRelation relation = modelEntity.getRelation(relationName);

        if (relation == null) {
            throw new GenericModelException("Could not find relation for relationName: " + relationName + " for value " + value);
        }
        ModelEntity relatedEntity = getModelReader().getModelEntity(relation.getRelEntityName());

        // put the byAndFields (if not null) into the hash map first,
        // they will be overridden by value's fields if over-specified this is important for security and cleanliness
        Map fields = byAndFields == null ? new HashMap() : new HashMap(byAndFields);
        for (int i = 0; i < relation.getKeyMapsSize(); i++) {
            ModelKeyMap keyMap = relation.getKeyMap(i);
            fields.put(keyMap.getRelFieldName(), value.get(keyMap.getFieldName()));
        }

        GenericPK dummyPK = new GenericPK(relatedEntity, fields);
        dummyPK.setDelegator(this);
        return dummyPK;
    }

    /** Get the named Related Entity for the GenericValue from the persistent store, checking first in the cache to see if the desired value is there
     * @param relationName String containing the relation name which is the
     *      combination of relation.title and relation.rel-entity-name as
     *      specified in the entity XML definition file
     * @param value GenericValue instance containing the entity
     * @return List of GenericValue instances as specified in the relation definition
     */
    public List getRelatedCache(String relationName, GenericValue value) throws GenericEntityException {
        ModelEntity modelEntity = value.getModelEntity();
        ModelRelation relation = modelEntity.getRelation(relationName);

        if (relation == null) {
            throw new GenericModelException("Could not find relation for relationName: " + relationName + " for value " + value);
        }

        Map fields = new HashMap();
        for (int i = 0; i < relation.getKeyMapsSize(); i++) {
            ModelKeyMap keyMap = relation.getKeyMap(i);
            fields.put(keyMap.getRelFieldName(), value.get(keyMap.getFieldName()));
        }

        return this.findByAndCache(relation.getRelEntityName(), fields, null);
    }

    /** Get related entity where relation is of type one, uses findByPrimaryKey
     * @throws IllegalArgumentException if the list found has more than one item
     */
    public GenericValue getRelatedOne(String relationName, GenericValue value) throws GenericEntityException {
        ModelEntity modelEntity = value.getModelEntity();
        ModelRelation relation = value.getModelEntity().getRelation(relationName);

        if (relation == null) {
            throw new GenericModelException("Could not find relation for relationName: " + relationName + " for value " + value);
        }
        if (!"one".equals(relation.getType()) && !"one-nofk".equals(relation.getType())) {
            throw new IllegalArgumentException("Relation is not a 'one' or a 'one-nofk' relation: " + relationName + " of entity " + value.getEntityName());
        }

        Map fields = new HashMap();
        for (int i = 0; i < relation.getKeyMapsSize(); i++) {
            ModelKeyMap keyMap = relation.getKeyMap(i);
            fields.put(keyMap.getRelFieldName(), value.get(keyMap.getFieldName()));
        }

        return this.findByPrimaryKey(relation.getRelEntityName(), fields);
    }

    /** Get related entity where relation is of type one, uses findByPrimaryKey, checking first in the cache to see if the desired value is there
     * @throws IllegalArgumentException if the list found has more than one item
     */
    public GenericValue getRelatedOneCache(String relationName, GenericValue value) throws GenericEntityException {
        ModelEntity modelEntity = value.getModelEntity();
        ModelRelation relation = modelEntity.getRelation(relationName);

        if (relation == null) {
            throw new GenericModelException("Could not find relation for relationName: " + relationName + " for value " + value);
        }
        if (!"one".equals(relation.getType()) && !"one-nofk".equals(relation.getType())) {
            throw new IllegalArgumentException("Relation is not a 'one' or a 'one-nofk' relation: " + relationName + " of entity " + value.getEntityName());
        }

        Map fields = new HashMap();
        for (int i = 0; i < relation.getKeyMapsSize(); i++) {
            ModelKeyMap keyMap = relation.getKeyMap(i);
            fields.put(keyMap.getRelFieldName(), value.get(keyMap.getFieldName()));
        }

        return this.findByPrimaryKeyCache(relation.getRelEntityName(), fields);
    }

    /** Remove the named Related Entity for the GenericValue from the persistent store
     *@param relationName String containing the relation name which is the
     *      combination of relation.title and relation.rel-entity-name as
     *      specified in the entity XML definition file
     *@param value GenericValue instance containing the entity
     *@return int representing number of rows effected by this operation
     */
    public int removeRelated(String relationName, GenericValue value) throws GenericEntityException {
        return this.removeRelated(relationName, value, true);
    }

    /** Remove the named Related Entity for the GenericValue from the persistent store
     *@param relationName String containing the relation name which is the
     *      combination of relation.title and relation.rel-entity-name as
     *      specified in the entity XML definition file
     *@param value GenericValue instance containing the entity
     *@param doCacheClear boolean that specifies whether to clear cache entries for this value to be removed
     *@return int representing number of rows effected by this operation
     */
    public int removeRelated(String relationName, GenericValue value, boolean doCacheClear) throws GenericEntityException {
        ModelEntity modelEntity = value.getModelEntity();
        ModelRelation relation = modelEntity.getRelation(relationName);

        if (relation == null) {
            throw new GenericModelException("Could not find relation for relationName: " + relationName + " for value " + value);
        }

        Map fields = new HashMap();
        for (int i = 0; i < relation.getKeyMapsSize(); i++) {
            ModelKeyMap keyMap = relation.getKeyMap(i);
            fields.put(keyMap.getRelFieldName(), value.get(keyMap.getFieldName()));
        }

        return this.removeByAnd(relation.getRelEntityName(), fields, doCacheClear);
    }

    /** Refresh the Entity for the GenericValue from the persistent store
     *@param value GenericValue instance containing the entity to refresh
     */
    public void refresh(GenericValue value) throws GenericEntityException {
        this.refresh(value, true);
    }

    /** Refresh the Entity for the GenericValue from the persistent store
     *@param value GenericValue instance containing the entity to refresh
     *@param doCacheClear boolean that specifies whether or not to automatically clear cache entries related to this operation
     */
    public void refresh(GenericValue value, boolean doCacheClear) throws GenericEntityException {
        if (doCacheClear) {
            // always clear cache before the operation
            clearCacheLine(value);
        }
        GenericPK pk = value.getPrimaryKey();
        GenericValue newValue = findByPrimaryKey(pk);

        if (newValue == null) {
            throw new IllegalArgumentException("[GenericDelegator.refresh] could not refresh value: " + value);
        }
        value.fields = newValue.fields;
        value.setDelegator(this);
        value.modified = false;
    }

    /** Store the Entity from the GenericValue to the persistent store
     *@param value GenericValue instance containing the entity
     *@return int representing number of rows effected by this operation
     */
    public int store(GenericValue value) throws GenericEntityException {
        return this.store(value, true);
    }

    /** Store the Entity from the GenericValue to the persistent store
     *@param value GenericValue instance containing the entity
     *@param doCacheClear boolean that specifies whether or not to automatically clear cache entries related to this operation
     *@return int representing number of rows effected by this operation
     */
    public int store(GenericValue value, boolean doCacheClear) throws GenericEntityException {
        Map ecaEventMap = this.getEcaEntityEventMap(value.getEntityName());
        this.evalEcaRules(EntityEcaHandler.EV_VALIDATE, EntityEcaHandler.OP_STORE, value, ecaEventMap, (ecaEventMap == null), false);
        GenericHelper helper = getEntityHelper(value.getEntityName());

        if (doCacheClear) {
            // always clear cache before the operation
            this.evalEcaRules(EntityEcaHandler.EV_CACHE_CLEAR, EntityEcaHandler.OP_STORE, value, ecaEventMap, (ecaEventMap == null), false);
            this.clearCacheLine(value);
        }

        this.evalEcaRules(EntityEcaHandler.EV_RUN, EntityEcaHandler.OP_STORE, value, ecaEventMap, (ecaEventMap == null), false);
        int retVal = helper.store(value);

        // refresh the valueObject to get the new version
        if (value.lockEnabled()) {
            refresh(value, doCacheClear);
        }

        this.evalEcaRules(EntityEcaHandler.EV_RETURN, EntityEcaHandler.OP_STORE, value, ecaEventMap, (ecaEventMap == null), false);
        return retVal;
    }

    /** Store the Entities from the List GenericValue instances to the persistent store.
     *  <br>This is different than the normal store method in that the store method only does
     *  an update, while the storeAll method checks to see if each entity exists, then
     *  either does an insert or an update as appropriate.
     *  <br>These updates all happen in one transaction, so they will either all succeed or all fail,
     *  if the data source supports transactions. This is just like to othersToStore feature
     *  of the GenericEntity on a create or store.
     *@param values List of GenericValue instances containing the entities to store
     *@return int representing number of rows effected by this operation
     */
    public int storeAll(List values) throws GenericEntityException {
        return this.storeAll(values, true);
    }

    /** Store the Entities from the List GenericValue instances to the persistent store.
     *  <br>This is different than the normal store method in that the store method only does
     *  an update, while the storeAll method checks to see if each entity exists, then
     *  either does an insert or an update as appropriate.
     *  <br>These updates all happen in one transaction, so they will either all succeed or all fail,
     *  if the data source supports transactions. This is just like to othersToStore feature
     *  of the GenericEntity on a create or store.
     *@param values List of GenericValue instances containing the entities to store
     *@param doCacheClear boolean that specifies whether or not to automatically clear cache entries related to this operation
     *@return int representing number of rows effected by this operation
     */
    public int storeAll(List values, boolean doCacheClear) throws GenericEntityException {
        //TODO: add eca eval calls
        if (values == null) {
            return 0;
        }

        // from the delegator level this is complicated because different GenericValue
        // objects in the list may correspond to different helpers
        HashMap valuesPerHelper = new HashMap();
        Iterator viter = values.iterator();

        while (viter.hasNext()) {
            GenericValue value = (GenericValue) viter.next();
            String helperName = this.getEntityHelperName(value.getEntityName());
            List helperValues = (List) valuesPerHelper.get(helperName);
            if (helperValues == null) {
                helperValues = new LinkedList();
                valuesPerHelper.put(helperName, helperValues);
            }
            helperValues.add(value);
        }

        boolean beganTransaction = false;
        int numberChanged = 0;

        try {
            // if there are multiple helpers and no transaction is active, begin one
            if (valuesPerHelper.size() > 1) {
                beganTransaction = TransactionUtil.begin();
            }

            Iterator helperIter = valuesPerHelper.entrySet().iterator();
            while (helperIter.hasNext()) {
                Map.Entry curEntry = (Map.Entry) helperIter.next();
                String helperName = (String) curEntry.getKey();
                GenericHelper helper = GenericHelperFactory.getHelper(helperName);

                if (doCacheClear) {
                    this.clearAllCacheLinesByValue((List) curEntry.getValue());
                }
                numberChanged += helper.storeAll((List) curEntry.getValue());
            }

            // only commit the transaction if we started one...
            TransactionUtil.commit(beganTransaction);
        } catch (GenericEntityException e) {
            try {
                // only rollback the transaction if we started one...
                TransactionUtil.rollback(beganTransaction);
            } catch (GenericEntityException e2) {
                Debug.logError("[GenericDelegator.storeAll] Could not rollback transaction: ", module);
                Debug.logError(e2, module);
            }
            // after rolling back, rethrow the exception
            throw e;
        }

        // Refresh the valueObjects to get the new version
        viter = values.iterator();
        while (viter.hasNext()) {
            GenericValue value = (GenericValue) viter.next();
            if (value.lockEnabled()) {
                refresh(value);
            }
        }

        return numberChanged;
    }

    /** Remove the Entities from the List from the persistent store.
     *  <br>The List contains GenericEntity objects, can be either GenericPK or GenericValue.
     *  <br>If a certain entity contains a complete primary key, the entity in the datasource corresponding
     *  to that primary key will be removed, this is like a removeByPrimary Key.
     *  <br>On the other hand, if a certain entity is an incomplete or non primary key,
     *  if will behave like the removeByAnd method.
     *  <br>These updates all happen in one transaction, so they will either all succeed or all fail,
     *  if the data source supports transactions.
     *@param dummyPKs Collection of GenericEntity instances containing the entities or by and fields to remove
     *@return int representing number of rows effected by this operation
     */
    public int removeAll(List dummyPKs) throws GenericEntityException {
        return this.removeAll(dummyPKs, true);
    }

    /** Remove the Entities from the List from the persistent store.
     *  <br>The List contains GenericEntity objects, can be either GenericPK or GenericValue.
     *  <br>If a certain entity contains a complete primary key, the entity in the datasource corresponding
     *  to that primary key will be removed, this is like a removeByPrimary Key.
     *  <br>On the other hand, if a certain entity is an incomplete or non primary key,
     *  if will behave like the removeByAnd method.
     *  <br>These updates all happen in one transaction, so they will either all succeed or all fail,
     *  if the data source supports transactions.
     *@param dummyPKs Collection of GenericEntity instances containing the entities or by and fields to remove
     *@param doCacheClear boolean that specifies whether or not to automatically clear cache entries related to this operation
     *@return int representing number of rows effected by this operation
     */
    public int removeAll(List dummyPKs, boolean doCacheClear) throws GenericEntityException {
        //TODO: add eca eval calls
        if (dummyPKs == null) {
            return 0;
        }

        // from the delegator level this is complicated because different GenericValue
        // objects in the list may correspond to different helpers
        HashMap valuesPerHelper = new HashMap();
        Iterator viter = dummyPKs.iterator();

        while (viter.hasNext()) {
            GenericEntity entity = (GenericEntity) viter.next();
            String helperName = this.getEntityHelperName(entity.getEntityName());
            Collection helperValues = (Collection) valuesPerHelper.get(helperName);

            if (helperValues == null) {
                helperValues = new LinkedList();
                valuesPerHelper.put(helperName, helperValues);
            }
            helperValues.add(entity);
        }

        boolean beganTransaction = false;
        int numRemoved = 0;

        try {
            // if there are multiple helpers and no transaction is active, begin one
            if (valuesPerHelper.size() > 1) {
                beganTransaction = TransactionUtil.begin();
            }

            Iterator helperIter = valuesPerHelper.entrySet().iterator();

            while (helperIter.hasNext()) {
                Map.Entry curEntry = (Map.Entry) helperIter.next();
                String helperName = (String) curEntry.getKey();
                GenericHelper helper = GenericHelperFactory.getHelper(helperName);

                if (doCacheClear) {
                    this.clearAllCacheLinesByDummyPK((List) curEntry.getValue());
                }
                numRemoved += helper.removeAll((List) curEntry.getValue());
            }

            // only commit the transaction if we started one...
            TransactionUtil.commit(beganTransaction);
        } catch (GenericEntityException e) {
            try {
                // only rollback the transaction if we started one...
                TransactionUtil.rollback(beganTransaction);
            } catch (GenericEntityException e2) {
                Debug.logError("[GenericDelegator.removeAll] Could not rollback transaction: ", module);
                Debug.logError(e2, module);
            }
            // after rolling back, rethrow the exception
            throw e;
        }

        return numRemoved;
    }

    // ======================================
    // ======= Cache Related Methods ========

    /** This method is a shortcut to completely clear all entity engine caches.
     * For performance reasons this should not be called very often.
     */
    public void clearAllCaches() {
        this.clearAllCaches(true);
    }

    public void clearAllCaches(boolean distribute) {
        if (this.allCache != null) this.allCache.clear();
        if (this.andCache != null) this.andCache.clear();
        if (this.andCacheFieldSets != null) this.andCacheFieldSets.clear();
        if (this.primaryKeyCache != null) this.primaryKeyCache.clear();

        if (distribute && this.distributedCacheClear != null) {
            this.distributedCacheClear.clearAllCaches();
        }
    }

    /** Remove a CACHED Generic Entity (List) from the cache, either a PK, ByAnd, or All
     *@param entityName The Name of the Entity as defined in the entity XML file
     *@param fields The fields of the named entity to query by with their corresponging values
     */
    public void clearCacheLine(String entityName, Map fields) {
        // if no fields passed, do the all cache quickly and return
        if (fields == null && allCache != null) {
            allCache.remove(entityName);
            return;
        }

        ModelEntity entity = this.getModelEntity(entityName);
        if (entity == null) {
            throw new IllegalArgumentException("[GenericDelegator.clearCacheLine] could not find entity for entityName: " + entityName);
        }
        //if never cached, then don't bother clearing
        if (entity.getNeverCache()) return;

        GenericPK dummyPK = new GenericPK(entity, fields);
        this.clearCacheLineFlexible(dummyPK);
    }

    /** Remove a CACHED Generic Entity from the cache by its primary key.
     * Checks to see if the passed GenericPK is a complete primary key, if
     * it is then the cache line will be removed from the primaryKeyCache; if it
     * is NOT a complete primary key it will remove the cache line from the andCache.
     * If the fields map is empty, then the allCache for the entity will be cleared.
     *@param dummyPK The dummy primary key to clear by.
     */
    public void clearCacheLineFlexible(GenericEntity dummyPK) {
        this.clearCacheLineFlexible(dummyPK, true);
    }

    public void clearCacheLineFlexible(GenericEntity dummyPK, boolean distribute) {
        if (dummyPK != null) {
            //if never cached, then don't bother clearing
            if (dummyPK.getModelEntity().getNeverCache()) return;

            // always auto clear the all cache too, since we know it's messed up in any case
            if (allCache != null) {
                allCache.remove(dummyPK.getEntityName());
            }

            // check to see if passed fields names exactly make the primary key...
            if (dummyPK.isPrimaryKey()) {
                // findByPrimaryKey
                if (primaryKeyCache != null) {
                    primaryKeyCache.remove(dummyPK);
                }
            } else {
                if (dummyPK.size() > 0) {
                    // findByAnd
                    if (andCache != null) {
                        andCache.remove(dummyPK);
                    }
                }
            }

            if (distribute && this.distributedCacheClear != null) {
                this.distributedCacheClear.distributedClearCacheLineFlexible(dummyPK);
            }
        }
    }

    /** Remove a CACHED Generic Entity from the cache by its primary key, does NOT
     * check to see if the passed GenericPK is a complete primary key.
     * Also tries to clear the corresponding all cache entry.
     *@param primaryKey The primary key to clear by.
     */
    public void clearCacheLine(GenericPK primaryKey) {
        this.clearCacheLine(primaryKey, true);
    }

    public void clearCacheLine(GenericPK primaryKey, boolean distribute) {
        if (primaryKey == null) return;

        //if never cached, then don't bother clearing
        if (primaryKey.getModelEntity().getNeverCache()) return;

        // always auto clear the all cache too, since we know it's messed up in any case
        if (allCache != null) {
            allCache.remove(primaryKey.getEntityName());
        }

        if (primaryKeyCache != null) {
            primaryKeyCache.remove(primaryKey);
        }

        if (distribute && this.distributedCacheClear != null) {
            this.distributedCacheClear.distributedClearCacheLine(primaryKey);
        }
    }

    /** Remove a CACHED GenericValue from as many caches as it can. Automatically
     * tries to remove entries from the all cache, the by primary key cache, and
     * the by and cache. This is the ONLY method that tries to clear automatically
     * from the by and cache.
     *@param value The primary key to clear by.
     */
    public void clearCacheLine(GenericValue value) {
        this.clearCacheLine(value, true);
    }

    public void clearCacheLine(GenericValue value, boolean distribute) {
        // TODO: make this a bit more intelligent by passing in the operation being done (create, update, remove) so we can not do unnecessary cache clears...
        // for instance:
        // on create don't clear by primary cache (and won't clear original values because there won't be any)
        // on remove don't clear by and for new values, but do for original values

        // Debug.logInfo("running clearCacheLine for value: " + value + ", distribute: " + distribute);
        if (value == null) return;

        //if never cached, then don't bother clearing
        if (value.getModelEntity().getNeverCache()) return;

        // always auto clear the all cache too, since we know it's messed up in any case
        if (allCache != null) {
            allCache.remove(value.getEntityName());
        }

        if (primaryKeyCache != null) {
            primaryKeyCache.remove(value.getPrimaryKey());
        }

        // now for the tricky part, automatically clearing from the by and cache

        // get a set of all field combination sets used in the by and cache for this entity
        Set fieldNameSets = (Set) andCacheFieldSets.get(value.getEntityName());

        if (fieldNameSets != null) {
            // note that if fieldNameSets is null then no by and caches have been
            // stored for this entity, so do nothing; ie only run this if not null

            // iterate through the list of field combination sets and do a cache clear
            // for each one using field values from this entity value object
            Iterator fieldNameSetIter = fieldNameSets.iterator();

            while (fieldNameSetIter.hasNext()) {
                Set fieldNameSet = (Set) fieldNameSetIter.next();

                // In this loop get the original values in addition to the
                // current values and clear the cache line with those values
                // too... This is necessary so that by and lists that currently
                // have the entity will be cleared in addition to the by and
                // lists that will have the entity
                // For this we will need to have the GenericValue object keep a
                // map of original values in addition to the "current" values.
                // That may have to be done when an entity is read from the
                // database and not when a put/set is done because a null value
                // is a perfectly valid original value. NOTE: the original value
                // map should be clear by default to denote that there was no
                // original value. When a GenericValue is created from a read
                // from the database only THEN should the original value map
                // be created and set to the same values that are put in the
                // normal field value map.


                Map originalFieldValues = null;

                if (value.isModified() && value.originalDbValuesAvailable()) {
                    originalFieldValues = new HashMap();
                }
                Map fieldValues = new HashMap();
                Iterator fieldNameIter = fieldNameSet.iterator();

                while (fieldNameIter.hasNext()) {
                    String fieldName = (String) fieldNameIter.next();

                    fieldValues.put(fieldName, value.get(fieldName));
                    if (originalFieldValues != null) {
                        originalFieldValues.put(fieldName, value.getOriginalDbValue(fieldName));
                    }
                }

                // now we have a map of values for this field set for this entity, so clear the by and line...
                GenericPK dummyPK = new GenericPK(value.getModelEntity(), fieldValues);

                andCache.remove(dummyPK);

                if (originalFieldValues != null && !originalFieldValues.equals(fieldValues)) {
                    GenericPK dummyPKOriginal = new GenericPK(value.getModelEntity(), originalFieldValues);

                    andCache.remove(dummyPKOriginal);
                }
            }
        }

        if (distribute && this.distributedCacheClear != null) {
            this.distributedCacheClear.distributedClearCacheLine(value);
        }
    }

    /** Gets a Set of Sets of fieldNames used in the by and cache for the given entityName */
    public Set getFieldNameSetsCopy(String entityName) {
        Set fieldNameSets = (Set) andCacheFieldSets.get(entityName);

        if (fieldNameSets == null) return null;

        // create a new container set and a copy of each entry set
        Set setsCopy = new TreeSet();
        Iterator fieldNameSetIter = fieldNameSets.iterator();

        while (fieldNameSetIter.hasNext()) {
            Set fieldNameSet = (Set) fieldNameSetIter.next();

            setsCopy.add(new TreeSet(fieldNameSet));
        }
        return setsCopy;
    }

    public void clearAllCacheLinesByDummyPK(Collection dummyPKs) {
        if (dummyPKs == null) return;
        Iterator iter = dummyPKs.iterator();

        while (iter.hasNext()) {
            GenericEntity entity = (GenericEntity) iter.next();

            this.clearCacheLineFlexible(entity);
        }
    }

    public void clearAllCacheLinesByValue(Collection values) {
        if (values == null) return;
        Iterator iter = values.iterator();

        while (iter.hasNext()) {
            GenericValue value = (GenericValue) iter.next();

            this.clearCacheLine(value);
        }
    }

    public GenericValue getFromPrimaryKeyCache(GenericPK primaryKey) {
        if (primaryKey == null) return null;
        return (GenericValue) primaryKeyCache.get(primaryKey);
    }

    public List getFromAllCache(String entityName) {
        if (entityName == null) return null;
        return (List) allCache.get(entityName);
    }

    public List getFromAndCache(String entityName, Map fields) {
        if (entityName == null || fields == null) return null;
        ModelEntity entity = this.getModelEntity(entityName);

        return getFromAndCache(entity, fields);
    }

    public List getFromAndCache(ModelEntity entity, Map fields) {
        if (entity == null || fields == null) return null;
        GenericPK tempPK = new GenericPK(entity, fields);

        if (tempPK == null) return null;
        return (List) andCache.get(tempPK);
    }

    public void putInPrimaryKeyCache(GenericPK primaryKey, GenericValue value) {
        if (primaryKey == null || value == null) return;

        if (value.getModelEntity().getNeverCache()) {
            Debug.logWarning("Tried to put a value of the " + value.getEntityName() + " entity in the BY PRIMARY KEY cache but this entity has never-cache set to true, not caching.");
            return;
        }

        primaryKeyCache.put(primaryKey, value);
    }

    public void putAllInPrimaryKeyCache(List values) {
        if (values == null) return;
        Iterator iter = values.iterator();

        while (iter.hasNext()) {
            GenericValue value = (GenericValue) iter.next();

            this.putInPrimaryKeyCache(value.getPrimaryKey(), value);
        }
    }

    public void putInAllCache(String entityName, List values) {
        if (entityName == null || values == null) return;
        ModelEntity entity = this.getModelEntity(entityName);
        this.putInAllCache(entity, values);
    }

    public void putInAllCache(ModelEntity entity, List values) {
        if (entity == null || values == null) return;

        if (entity.getNeverCache()) {
            Debug.logWarning("Tried to put values of the " + entity.getEntityName() + " entity in the ALL cache but this entity has never-cache set to true, not caching.");
            return;
        }

        // make the values immutable so that the list can be returned directly from the cache without copying and still be safe
        // NOTE that this makes the list immutable, but not the elements in it, those will still be changeable GenericValue objects...
        allCache.put(entity.getEntityName(), Collections.unmodifiableList(values));
    }

    public void putInAndCache(String entityName, Map fields, List values) {
        if (entityName == null || fields == null || values == null) return;
        ModelEntity entity = this.getModelEntity(entityName);
        putInAndCache(entity, fields, values);
    }

    public void putInAndCache(ModelEntity entity, Map fields, List values) {
        if (entity == null || fields == null || values == null) return;

        if (entity.getNeverCache()) {
            Debug.logWarning("Tried to put values of the " + entity.getEntityName() + " entity in the BY AND cache but this entity has never-cache set to true, not caching.");
            return;
        }

        GenericPK tempPK = new GenericPK(entity, fields);

        if (tempPK == null) return;
        // make the values immutable so that the list can be returned directly from the cache without copying and still be safe
        // NOTE that this makes the list immutable, but not the elements in it, those will still be changeable GenericValue objects...
        andCache.put(tempPK, Collections.unmodifiableList(values));

        // now make sure the fieldName set used for this entry is in the
        // andCacheFieldSets Map which contains a Set of Sets of fieldNames for each entityName
        Set fieldNameSets = (Set) andCacheFieldSets.get(entity.getEntityName());

        if (fieldNameSets == null) {
            synchronized (this) {
                fieldNameSets = (Set) andCacheFieldSets.get(entity.getEntityName());
                if (fieldNameSets == null) {
                    // using a HashSet for both the individual fieldNameSets and
                    // the set of fieldNameSets; this appears to be necessary
                    // because TreeSet has bugs, or does not support, the compare
                    // operation which is necessary when inserted a TreeSet
                    // into a TreeSet.
                    fieldNameSets = new HashSet();
                    andCacheFieldSets.put(entity.getEntityName(), fieldNameSets);
                }
            }
        }
        fieldNameSets.add(new HashSet(fields.keySet()));
    }

    // ======= XML Related Methods ========
    public List readXmlDocument(URL url) throws SAXException, ParserConfigurationException, java.io.IOException {
        if (url == null) return null;
        return this.makeValues(UtilXml.readXmlDocument(url, false));
    }

    public List makeValues(Document document) {
        if (document == null) return null;
        List values = new LinkedList();

        Element docElement = document.getDocumentElement();

        if (docElement == null)
            return null;
        if (!"entity-engine-xml".equals(docElement.getTagName())) {
            Debug.logError("[GenericDelegator.makeValues] Root node was not <entity-engine-xml>", module);
            throw new java.lang.IllegalArgumentException("Root node was not <entity-engine-xml>");
        }
        docElement.normalize();
        Node curChild = docElement.getFirstChild();

        if (curChild != null) {
            do {
                if (curChild.getNodeType() == Node.ELEMENT_NODE) {
                    Element element = (Element) curChild;
                    GenericValue value = this.makeValue(element);

                    if (value != null)
                        values.add(value);
                }
            } while ((curChild = curChild.getNextSibling()) != null);
        } else {
            Debug.logWarning("[GenericDelegator.makeValues] No child nodes found in document.", module);
        }

        return values;
    }

    public GenericPK makePK(Element element) {
        GenericValue value = makeValue(element);

        return value.getPrimaryKey();
    }

    public GenericValue makeValue(Element element) {
        if (element == null) return null;
        String entityName = element.getTagName();

        // if a dash or colon is in the tag name, grab what is after it
        if (entityName.indexOf('-') > 0)
            entityName = entityName.substring(entityName.indexOf('-') + 1);
        if (entityName.indexOf(':') > 0)
            entityName = entityName.substring(entityName.indexOf(':') + 1);
        GenericValue value = this.makeValue(entityName, null);

        ModelEntity modelEntity = value.getModelEntity();

        Iterator modelFields = modelEntity.getFieldsIterator();

        while (modelFields.hasNext()) {
            ModelField modelField = (ModelField) modelFields.next();
            String name = modelField.getName();
            String attr = element.getAttribute(name);

            if (attr != null && attr.length() > 0) {
                value.setString(name, attr);
            } else {
                // if no attribute try a subelement
                Element subElement = UtilXml.firstChildElement(element, name);

                if (subElement != null) {
                    value.setString(name, UtilXml.elementValue(subElement));
                }
            }
        }

        return value;
    }

    // ======= Misc Methods ========

    protected Map getEcaEntityEventMap(String entityName) {
        if (this.entityEcaHandler == null) return null;
        Map ecaEventMap = this.entityEcaHandler.getEntityEventMap(entityName);
        //Debug.logWarning("for entityName " + entityName + " got ecaEventMap: " + ecaEventMap);
        return ecaEventMap;
    }

    protected void evalEcaRules(String event, String currentOperation, GenericEntity value, Map eventMap, boolean noEventMapFound, boolean isError) throws GenericEntityException {
        // if this is true then it means that the caller had looked for an event map but found none for this entity
        if (noEventMapFound) return;
        if (this.entityEcaHandler == null) return;
        //if (!"find".equals(currentOperation)) Debug.logWarning("evalRules for entity " + value.getEntityName() + ", currentOperation " + currentOperation + ", event " + event);
        this.entityEcaHandler.evalRules(currentOperation, eventMap, event, value, isError);
    }


    /** Get the next guaranteed unique seq id from the sequence with the given sequence name;
     * if the named sequence doesn't exist, it will be created
     *@param seqName The name of the sequence to get the next seq id from
     *@return Long with the next seq id for the given sequence name
     */
    public Long getNextSeqId(String seqName) {
        if (sequencer == null) {
            synchronized (this) {
                if (sequencer == null) {
                    String helperName = this.getEntityHelperName("SequenceValueItem");
                    ModelEntity seqEntity = this.getModelEntity("SequenceValueItem");

                    sequencer = new SequenceUtil(helperName, seqEntity, "seqName", "seqId");
                }
            }
        }
        if (sequencer != null) {
            return sequencer.getNextSeqId(seqName);
        } else {
            return null;
        }
    }

    /** Allows you to pass a SequenceUtil class (possibly one that overrides the getNextSeqId method);
     * if null is passed will effectively refresh the sequencer. */
    public void setSequencer(SequenceUtil sequencer) {
        this.sequencer = sequencer;
    }

    /** Refreshes the ID sequencer clearing all cached bank values. */
    public void refreshSequencer() {
        this.sequencer = null;
    }

    protected void absorbList(List lst) {
        if (lst == null) return;
        Iterator iter = lst.iterator();

        while (iter.hasNext()) {
            GenericValue value = (GenericValue) iter.next();

            value.setDelegator(this);
        }
    }

    public UtilCache getPrimaryKeyCache() {
        return primaryKeyCache;
    }

    public UtilCache getAndCache() {
        return andCache;
    }

    public UtilCache getAllCache() {
        return allCache;
    }
}
