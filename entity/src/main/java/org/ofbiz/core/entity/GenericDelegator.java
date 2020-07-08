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
 */
package org.ofbiz.core.entity;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.ofbiz.core.entity.config.DatasourceInfo;
import org.ofbiz.core.entity.config.EntityConfigUtil;
import org.ofbiz.core.entity.model.ModelEntity;
import org.ofbiz.core.entity.model.ModelField;
import org.ofbiz.core.entity.model.ModelFieldType;
import org.ofbiz.core.entity.model.ModelFieldTypeReader;
import org.ofbiz.core.entity.model.ModelGroupReader;
import org.ofbiz.core.entity.model.ModelKeyMap;
import org.ofbiz.core.entity.model.ModelReader;
import org.ofbiz.core.entity.model.ModelRelation;
import org.ofbiz.core.util.Debug;
import org.ofbiz.core.util.UtilCache;
import org.ofbiz.core.util.UtilMisc;
import org.ofbiz.core.util.UtilXml;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.Optional.ofNullable;
import static org.ofbiz.core.entity.EntityOperator.AND;
import static org.ofbiz.core.entity.EntityOperator.LIKE;
import static org.ofbiz.core.entity.EntityOperator.OR;
import static org.ofbiz.core.entity.config.EntityConfigUtil.DelegatorInfo;

/**
 * Generic Data Source Delegator.
 * <p/>
 * TODO The thread safety in here (and everywhere in ofbiz) is crap, improper double checked locking,
 * modification of maps while other threads may be reading them, this class is not thread safe at all.
 *
 * @author <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 * @author <a href="mailto:chris_maurer@altavista.com">Chris Maurer</a>
 * @author <a href="mailto:jaz@ofbiz.org">Andy Zeneski</a
 * @since 1.0
 */
@SuppressWarnings("deprecation")
public class GenericDelegator implements DelegatorInterface {

    // ------------------------------- Statics --------------------------------

    public static final String module = GenericDelegator.class.getName();

    private static final String MESSAGE = "Database is locked";

    // A cache of delegator names to instances
    private static final LoadingCache<String, GenericDelegator> delegatorCache =
            CacheBuilder.newBuilder().build(new CacheLoader<String, GenericDelegator>() {
                @Override
                public GenericDelegator load(final String delegatorName) throws GenericEntityException {
                    return new GenericDelegator(delegatorName);
                }
            });

    private static final AtomicBoolean isLocked = new AtomicBoolean(false);

    /**
     * Factory method for a GenericDelegator with the given name.
     *
     * @param delegatorName the name of the server configuration that corresponds to this delegator
     * @return a non-null instance
     * @throws RuntimeException if the delegator could not be instantiated
     */
    public static GenericDelegator getGenericDelegator(final String delegatorName) {
        return delegatorCache.getUnchecked(delegatorName);
    }

    /**
     * Removes any references to the delegator with the given name.
     *
     * @param delegatorName the name of the server configuration that corresponds to this delegator
     */
    public static synchronized void removeGenericDelegator(final String delegatorName) {
        delegatorCache.invalidate(delegatorName);
    }

    public static void lock() {
        isLocked.set(true);
    }

    public static void unlock() {
        isLocked.set(false);
    }

    public static boolean isLocked() {
        return isLocked.get();
    }

    // ----------------------------- Non-statics ------------------------------

    protected final ModelGroupReader modelGroupReader;
    protected final ModelReader modelReader;
    protected final String delegatorName;
    protected final UtilCache<GenericEntity, GenericValue> primaryKeyCache;
    protected final UtilCache<GenericPK, List<GenericValue>> andCache;
    protected final UtilCache<String, List<GenericValue>> allCache;

    // keeps a list of field key sets used in the by and cache, a Set (of Sets of fieldNames) for each entityName
    protected final Map<String, Set<Set<String>>> andCacheFieldSets = new HashMap<>();

    protected volatile DelegatorInfo delegatorInfo;
    protected volatile DistributedCacheClear distributedCacheClear;
    protected volatile SequenceUtil sequencer;

    // this is really only for testing and the LockedDatabaseGenericDelegator ..... don't use unless know why!
    protected GenericDelegator() {
        modelGroupReader = null;
        modelReader = null;
        delegatorName = "";
        primaryKeyCache = null;
        andCache = null;
        allCache = null;
    }

    /**
     * Contructor is protected to enforce creation through the factory method.
     *
     * @param delegatorName the name of the server configuration that corresponds to this delegator
     */
    protected GenericDelegator(final String delegatorName) throws GenericEntityException {
        if (Debug.infoOn()) {
            Debug.logInfo("Creating new Delegator with name \"" + delegatorName + "\".", module);
        }
        this.delegatorName = delegatorName;
        this.modelReader = ModelReader.getModelReader(delegatorName);
        this.modelGroupReader = ModelGroupReader.getModelGroupReader(delegatorName);
        this.primaryKeyCache = new UtilCache<GenericEntity, GenericValue>("entity.xFindByPrimaryKey." + delegatorName, 0, 0, true);
        this.allCache = new UtilCache<String, List<GenericValue>>("entity.FindAll." + delegatorName, 0, 0, true);
        this.andCache = new UtilCache<GenericPK, List<GenericValue>>("entity.FindByAnd." + delegatorName, 0, 0, true);

        if (!isLocked()) {
            initialiseAndCheckDatabase();
        }

        // Do some tricks with manual class loading that resolves circular dependencies, like calling services
        final ClassLoader loader = Thread.currentThread().getContextClassLoader();

        if (getDelegatorInfo().useDistributedCacheClear) {
            // initialize the distributedCacheClear mechanism
            String distributedCacheClearClassName = getDelegatorInfo().distributedCacheClearClassName;

            try {
                Class<?> dccClass = loader.loadClass(distributedCacheClearClassName);
                this.distributedCacheClear = (DistributedCacheClear) dccClass.newInstance();
                this.distributedCacheClear.setDelegator(this, getDelegatorInfo().distributedCacheClearUserLoginId);
            } catch (ClassNotFoundException e) {
                Debug.logWarning(e, "DistributedCacheClear class with name " + distributedCacheClearClassName +
                        " was not found, distributed cache clearing will be disabled");
            } catch (InstantiationException e) {
                Debug.logWarning(e, "DistributedCacheClear class with name " + distributedCacheClearClassName +
                        " could not be instantiated, distributed cache clearing will be disabled");
            } catch (IllegalAccessException e) {
                Debug.logWarning(e, "DistributedCacheClear class with name " + distributedCacheClearClassName +
                        " could not be accessed (illegal), distributed cache clearing will be disabled");
            } catch (ClassCastException e) {
                Debug.logWarning(e, "DistributedCacheClear class with name " + distributedCacheClearClassName +
                        " does not implement the DistributedCacheClear interface, distributed cache clearing will be disabled");
            }
        }
    }

    /**
     * If you got an instance of GenericDelegator while the whole thing was locked, you need to make sure you call this
     * once it becomes unlocked before you do anything.
     */
    public void initialiseAndCheckDatabase() {
        checkIfLocked();
        // initialize helpers by group
        final Iterator<String> groups = UtilMisc.toIterator(getModelGroupReader().getGroupNames());

        while (groups != null && groups.hasNext()) {
            String groupName = groups.next();
            String helperName = getGroupHelperName(groupName);

            if (Debug.infoOn()) Debug.logInfo("Delegator \"" + delegatorName + "\" initializing helper \"" +
                    helperName + "\" for entity group \"" + groupName + "\".", module);
            TreeSet<String> helpersDone = new TreeSet<String>();

            if (helperName != null && helperName.length() > 0) {
                // make sure each helper is only loaded once
                if (helpersDone.contains(helperName)) {
                    if (Debug.infoOn()) {
                        Debug.logInfo("Helper \"" + helperName + "\" already initialized, not re-initializing.", module);
                    }
                    continue;
                }
                helpersDone.add(helperName);
                // pre-load field type defs, the return value is ignored
                ModelFieldTypeReader.getModelFieldTypeReader(helperName);
                // get the helper and if configured, do the datasource check
                GenericHelper helper = GenericHelperFactory.getHelper(helperName);

                DatasourceInfo datasourceInfo = EntityConfigUtil.getInstance().getDatasourceInfo(helperName);

                if (datasourceInfo.isCheckOnStart()) {
                    if (Debug.infoOn()) {
                        Debug.logInfo("Doing database check as requested in entityengine.xml with addMissing=" +
                                datasourceInfo.isAddMissingOnStart(), module);
                    }
                    try {
                        helper.checkDataSource(getModelEntityMapByGroup(groupName), null,
                                datasourceInfo.isAddMissingOnStart());
                    } catch (GenericEntityException e) {
                        Debug.logWarning(e.getMessage(), module);
                    }
                }
            }
        }
    }

    /**
     * Gets the name of the server configuration that corresponds to this delegator.
     *
     * @return server configuration name
     */
    public String getDelegatorName() {
        checkIfLocked();
        return this.delegatorName;
    }

    protected DelegatorInfo getDelegatorInfo() {
        if (delegatorInfo == null) {
            delegatorInfo = EntityConfigUtil.getInstance().getDelegatorInfo(this.delegatorName);
        }
        return delegatorInfo;
    }

    /**
     * Gets the instance of ModelReader that corresponds to this delegator.
     *
     * @return ModelReader that corresponds to this delegator
     */
    public ModelReader getModelReader() {
        checkIfLocked();
        return this.modelReader;
    }

    /**
     * Gets the instance of ModelGroupReader that corresponds to this delegator.
     *
     * @return ModelGroupReader that corresponds to this delegator
     */
    public ModelGroupReader getModelGroupReader() {
        checkIfLocked();
        return this.modelGroupReader;
    }

    /**
     * Gets the instance of ModelEntity that corresponds to this delegator and the specified entityName.
     *
     * @param entityName The name of the entity to get
     * @return ModelEntity that corresponds to this delegator and the specified entityName
     */
    public ModelEntity getModelEntity(final String entityName) {
        checkIfLocked();
        try {
            return getModelReader().getModelEntity(entityName);
        } catch (GenericEntityException e) {
            Debug.logError(e, "Error getting entity definition from model", module);
            return null;
        }
    }

    /**
     * Gets the helper name that corresponds to this delegator and the specified entityName.
     *
     * @param entityName The name of the entity to get the helper for
     * @return String with the helper name that corresponds to this delegator and the specified entityName
     */
    public String getEntityGroupName(final String entityName) {
        checkIfLocked();
        return getModelGroupReader().getEntityGroupName(entityName);
    }

    /**
     * Gets a list of entity models that are in a group corresponding to the specified group name.
     *
     * @param groupName The name of the group
     * @return List of ModelEntity instances
     */
    public List<ModelEntity> getModelEntitiesByGroup(final String groupName) {
        checkIfLocked();
        final Iterator<String> enames = UtilMisc.toIterator(getModelGroupReader().getEntityNamesByGroup(groupName));
        final List<ModelEntity> entities = new LinkedList<ModelEntity>();

        if (enames == null || !enames.hasNext())
            return entities;
        while (enames.hasNext()) {
            String ename = enames.next();
            ModelEntity entity = getModelEntity(ename);

            if (entity != null)
                entities.add(entity);
        }
        return entities;
    }

    /**
     * Gets a Map of entity name & entity model pairs that are in the named group.
     *
     * @param groupName The name of the group
     * @return Map of entityName String keys and ModelEntity instance values
     */
    public Map<String, ModelEntity> getModelEntityMapByGroup(final String groupName) {
        checkIfLocked();
        Iterator<String> enames = UtilMisc.toIterator(getModelGroupReader().getEntityNamesByGroup(groupName));
        Map<String, ModelEntity> entities = new HashMap<String, ModelEntity>();

        if (enames == null || !enames.hasNext()) {
            return entities;
        }

        int errorCount = 0;
        while (enames.hasNext()) {
            String ename = enames.next();
            try {
                ModelEntity entity = getModelReader().getModelEntity(ename);
                if (entity != null) {
                    entities.put(entity.getEntityName(), entity);
                } else {
                    throw new IllegalStateException("Program Error: entity was null with name " + ename);
                }
            } catch (GenericEntityException ex) {
                errorCount++;
                Debug.logError("Entity " + ename + " named in Entity Group with name " + groupName +
                        " are not defined in any Entity Definition file");
            }
        }

        if (errorCount > 0) {
            Debug.logError(errorCount + " entities were named in ModelGroup but not defined in any EntityModel");
        }

        return entities;
    }

    /**
     * Gets the helper name that corresponds to this delegator and the specified entityName.
     *
     * @param groupName The name of the group to get the helper name for
     * @return String with the helper name that corresponds to this delegator and the specified entityName
     */
    public String getGroupHelperName(final String groupName) {
        checkIfLocked();
        return getDelegatorInfo().groupMap.get(groupName);
    }

    /**
     * Gets the helper name that corresponds to this delegator and the specified entityName.
     *
     * @param entityName The name of the entity to get the helper name for
     * @return String with the helper name that corresponds to this delegator and the specified entityName
     */
    public String getEntityHelperName(final String entityName) {
        checkIfLocked();
        final String groupName = getModelGroupReader().getEntityGroupName(entityName);
        return getGroupHelperName(groupName);
    }

    /**
     * Gets the helper name that corresponds to this delegator and the specified entity.
     *
     * @param entity the entity for which to get the helper (can be null)
     * @return String with the helper name that corresponds to this delegator and the specified entity
     */
    public String getEntityHelperName(final ModelEntity entity) {
        checkIfLocked();
        if (entity == null) {
            return null;
        }
        return getEntityHelperName(entity.getEntityName());
    }

    /**
     * Gets the helper that corresponds to this delegator and the specified entityName.
     *
     * @param entityName The name of the entity to get the helper for
     * @return GenericHelper that corresponds to this delegator and the specified entityName
     */
    public GenericHelper getEntityHelper(final String entityName) throws GenericEntityException {
        checkIfLocked();
        final String helperName = getEntityHelperName(entityName);
        if (helperName != null && helperName.length() > 0) {
            return GenericHelperFactory.getHelper(helperName);
        }
        throw new GenericEntityException("Helper name not found for entity " + entityName);
    }

    /**
     * Gets the helper that corresponds to this delegator and the specified entity.
     *
     * @param entity The entity for which to get the helper (required)
     * @return GenericHelper that corresponds to this delegator and the specified entity
     */
    public GenericHelper getEntityHelper(final ModelEntity entity) throws GenericEntityException {
        checkIfLocked();
        return getEntityHelper(entity.getEntityName());
    }

    /**
     * Gets a field type instance by name from the helper that corresponds to the specified entity.
     *
     * @param entity The entity
     * @param type   The name of the type
     * @return ModelFieldType instance for the named type from the helper that corresponds to the specified entity
     */
    public ModelFieldType getEntityFieldType(final ModelEntity entity, final String type) throws GenericEntityException {
        checkIfLocked();
        final String helperName = getEntityHelperName(entity);
        if (helperName == null || helperName.length() == 0) {
            return null;
        }
        ModelFieldTypeReader modelFieldTypeReader = ModelFieldTypeReader.getModelFieldTypeReader(helperName);

        if (modelFieldTypeReader == null) {
            throw new GenericEntityException("ModelFieldTypeReader not found for entity " + entity.getEntityName() +
                    " with helper name " + helperName);
        }
        return modelFieldTypeReader.getModelFieldType(type);
    }

    /**
     * Gets field type names from the helper that corresponds to the specified entity.
     *
     * @param entity The entity
     * @return Collection of field type names from the helper that corresponds to the specified entity
     */
    public Collection<String> getEntityFieldTypeNames(final ModelEntity entity) throws GenericEntityException {
        checkIfLocked();
        final String helperName = getEntityHelperName(entity);
        if (helperName == null || helperName.length() == 0) {
            return null;
        }
        ModelFieldTypeReader modelFieldTypeReader = ModelFieldTypeReader.getModelFieldTypeReader(helperName);

        if (modelFieldTypeReader == null) {
            throw new GenericEntityException("ModelFieldTypeReader not found for entity " + entity.getEntityName() +
                    " with helper name " + helperName);
        }
        return modelFieldTypeReader.getFieldTypeNames();
    }

    /**
     * Creates a Entity in the form of a GenericValue without persisting it.
     *
     * @param entityName the type of entity to create (must exist in the model)
     * @param fields     the entity fields and their values (can be null)
     * @return the created value
     */
    public GenericValue makeValue(final String entityName, final Map<String, ?> fields) {
        checkIfLocked();
        final ModelEntity entity = getModelEntity(entityName);
        if (entity == null) {
            throw new IllegalArgumentException(
                    "[GenericDelegator.makeValue] could not find entity for entityName: " + entityName);
        }
        return new GenericValue(this, entity, fields);
    }

    /**
     * Creates a Primary Key in the form of a GenericPK without persisting it.
     *
     * @param entityName the type of entity for which to create a PK (must exist in the model)
     * @param fields     the primary key fields and their values (can be null)
     * @return the created PK
     */
    public GenericPK makePK(final String entityName, final Map<String, ?> fields) {
        checkIfLocked();
        ModelEntity entity = getModelEntity(entityName);

        if (entity == null) {
            throw new IllegalArgumentException(
                    "[GenericDelegator.makePK] could not find entity for entityName: " + entityName);
        }
        final GenericPK pk = new GenericPK(entity, fields);
        pk.setDelegator(this);
        return pk;
    }

    /**
     * Creates a Entity in the form of a GenericValue and write it to the database.
     *
     * @param entityName the type of entity to create (if null, this method does nothing)
     * @param fields     the field values to use (if null, this method does nothing)
     * @return the created instance
     */
    public GenericValue create(final String entityName, final Map<String, ?> fields) throws GenericEntityException {
        checkIfLocked();
        if (entityName == null || fields == null) {
            return null;
        }
        final ModelEntity entity = getModelReader().getModelEntity(entityName);
        final GenericValue genericValue = new GenericValue(this, entity, fields);
        return create(genericValue, true);
    }

    /**
     * Creates a Entity in the form of a GenericValue and write it to the datasource.
     *
     * @param value The GenericValue to create a value in the datasource from
     * @return GenericValue instance containing the new instance
     */
    public GenericValue create(final GenericValue value) throws GenericEntityException {
        checkIfLocked();
        return create(value, true);
    }

    /**
     * Creates a Entity in the form of a GenericValue and write it to the datasource.
     *
     * @param value        The GenericValue from which to create a value in the datasource (required)
     * @param doCacheClear whether to automatically clear cache entries related to this operation
     * @return GenericValue instance containing the new instance
     */
    public GenericValue create(GenericValue value, final boolean doCacheClear) throws GenericEntityException {
        checkIfLocked();
        final GenericHelper helper = getEntityHelper(value.getEntityName());
        value.setDelegator(this);
        value = helper.create(value);

        if (value != null) {
            value.setDelegator(this);
            if (value.lockEnabled()) {
                refresh(value, doCacheClear);
            } else if (doCacheClear) {
                clearCacheLine(value);
            }
        }
        return value;
    }

    /**
     * Creates a Entity in the form of a GenericValue and write it to the datasource.
     *
     * @param primaryKey The GenericPK to create a value in the datasource from
     * @return GenericValue instance containing the new instance
     */
    public GenericValue create(final GenericPK primaryKey) throws GenericEntityException {
        checkIfLocked();
        return create(primaryKey, true);
    }

    /**
     * Creates a Entity in the form of a GenericValue and write it to the datasource.
     *
     * @param primaryKey   the PK from which to create a value in the datasource (required)
     * @param doCacheClear whether to clear related cache entries for this primaryKey to be created
     * @return GenericValue instance containing the new instance
     */
    public GenericValue create(final GenericPK primaryKey, final boolean doCacheClear) throws GenericEntityException {
        checkIfLocked();
        if (primaryKey == null) {
            throw new IllegalArgumentException("Cannot create from a null primaryKey");
        }
        return create(new GenericValue(primaryKey), doCacheClear);
    }

    /**
     * Find a Generic Entity by its Primary Key.
     *
     * @param primaryKey The primary key to find by.
     * @return The GenericValue corresponding to the primaryKey
     */
    public GenericValue findByPrimaryKey(final GenericPK primaryKey) throws GenericEntityException {
        checkIfLocked();
        final GenericHelper helper = getEntityHelper(primaryKey.getEntityName());

        if (!primaryKey.isPrimaryKey()) {
            throw new IllegalArgumentException(
                    "[GenericDelegator.findByPrimaryKey] Passed primary key is not a valid primary key: " + primaryKey);
        }
        GenericValue value;
        try {
            value = helper.findByPrimaryKey(primaryKey);
        } catch (GenericEntityNotFoundException e) {
            value = null;
        }
        if (value != null) {
            value.setDelegator(this);
        }
        return value;
    }

    /**
     * Find a cached Generic Entity by its Primary Key.
     *
     * @param primaryKey The primary key to find by.
     * @return The GenericValue corresponding to the primaryKey
     */
    public GenericValue findByPrimaryKeyCache(final GenericPK primaryKey) throws GenericEntityException {
        checkIfLocked();
        GenericValue value = getFromPrimaryKeyCache(primaryKey);
        if (value == null) {
            value = findByPrimaryKey(primaryKey);
            if (value != null) {
                putInPrimaryKeyCache(primaryKey, value);
            }
        }
        return value;
    }

    /**
     * Find a Generic Entity by its Primary Key.
     *
     * @param entityName The Name of the Entity as defined in the entity XML file
     * @param fields     The fields of the named entity to query by with their corresponging values
     * @return The GenericValue corresponding to the primaryKey
     */
    public GenericValue findByPrimaryKey(final String entityName, final Map<String, ?> fields)
            throws GenericEntityException {
        checkIfLocked();
        return findByPrimaryKey(makePK(entityName, fields));
    }

    /**
     * Find a CACHED Generic Entity by its Primary Key.
     *
     * @param entityName The Name of the Entity as defined in the entity XML file
     * @param fields     The fields of the named entity to query by with their corresponging values
     * @return The GenericValue corresponding to the primaryKey
     */
    public GenericValue findByPrimaryKeyCache(final String entityName, final Map<String, ?> fields)
            throws GenericEntityException {
        checkIfLocked();
        return findByPrimaryKeyCache(makePK(entityName, fields));
    }

    /**
     * Find a Generic Entity by its Primary Key and only returns the values
     * requested by the passed keys (names).
     *
     * @param primaryKey The primary key to find by.
     * @param keys       The keys, or names, of the values to retrieve; only these values will be retrieved
     * @return The GenericValue corresponding to the primaryKey
     */
    public GenericValue findByPrimaryKeyPartial(final GenericPK primaryKey, final Set<String> keys)
            throws GenericEntityException {
        checkIfLocked();
        final GenericHelper helper = getEntityHelper(primaryKey.getEntityName());

        if (!primaryKey.isPrimaryKey()) {
            throw new IllegalArgumentException(
                    "[GenericDelegator.findByPrimaryKey] Passed primary key is not a valid primary key: " + primaryKey);
        }

        GenericValue value;
        try {
            value = helper.findByPrimaryKeyPartial(primaryKey, keys);
        } catch (GenericEntityNotFoundException e) {
            value = null;
        }
        if (value != null) {
            value.setDelegator(this);
        }
        return value;
    }

    /**
     * Find a number of Generic Value objects by their Primary Keys, all at once.
     *
     * @param primaryKeys A Collection of primary keys to find by.
     * @return List of GenericValue objects corresponding to the passed primaryKey objects
     */
    public List<GenericValue> findAllByPrimaryKeys(final Collection<? extends GenericPK> primaryKeys)
            throws GenericEntityException {
        checkIfLocked();
        if (primaryKeys == null) {
            return null;
        }
        List<GenericValue> results = new LinkedList<GenericValue>();

        // from the delegator level this is complicated because different GenericPK
        // objects in the list may correspond to different helpers
        final Map<String, List<GenericPK>> pksPerHelper = new HashMap<String, List<GenericPK>>();

        for (GenericPK primaryKey : primaryKeys) {
            String helperName = getEntityHelperName(primaryKey.getEntityName());
            List<GenericPK> pks = pksPerHelper.get(helperName);

            if (pks == null) {
                pks = new LinkedList<GenericPK>();
                pksPerHelper.put(helperName, pks);
            }
            pks.add(primaryKey);
        }

        for (Map.Entry<String, List<GenericPK>> entry : pksPerHelper.entrySet()) {
            String helperName = entry.getKey();
            GenericHelper helper = GenericHelperFactory.getHelper(helperName);
            List<GenericValue> values = helper.findAllByPrimaryKeys(entry.getValue());
            results.addAll(values);
        }
        return results;
    }

    /**
     * Find a number of Generic Value objects by their Primary Keys, all at
     * once; this first looks in the local cache for each PK and if there then
     * it puts it in the return list rather than putting it in the batch to
     * send to a given helper.
     *
     * @param primaryKeys A Collection of primary keys to find by.
     * @return List of GenericValue objects corresponding to the passed primaryKey objects
     */
    public List<GenericValue> findAllByPrimaryKeysCache(final Collection<? extends GenericPK> primaryKeys)
            throws GenericEntityException {
        checkIfLocked();
        if (primaryKeys == null) {
            return null;
        }
        final List<GenericValue> results = new LinkedList<GenericValue>();

        // from the delegator level this is complicated because different GenericPK
        // objects in the list may correspond to different helpers
        final Map<String, List<GenericPK>> pksPerHelper = new HashMap<String, List<GenericPK>>();

        for (GenericPK primaryKey : primaryKeys) {

            GenericValue value = getFromPrimaryKeyCache(primaryKey);

            if (value != null) {
                // it is in the cache, so just put the cached value in the results
                results.add(value);
            } else {
                // is not in the cache, so put in a list for a call to the helper
                final String helperName = getEntityHelperName(primaryKey.getEntityName());
                List<GenericPK> pks = pksPerHelper.get(helperName);
                if (pks == null) {
                    pks = new LinkedList<GenericPK>();
                    pksPerHelper.put(helperName, pks);
                }
                pks.add(primaryKey);
            }
        }

        for (final Map.Entry<String, List<GenericPK>> stringListEntry : pksPerHelper.entrySet()) {
            final String helperName = stringListEntry.getKey();
            final GenericHelper helper = GenericHelperFactory.getHelper(helperName);
            final List<GenericValue> values = helper.findAllByPrimaryKeys(stringListEntry.getValue());
            putAllInPrimaryKeyCache(values);
            results.addAll(values);
        }
        return results;
    }

    /**
     * Finds all Generic entities of the given type.
     *
     * @param entityName The Name of the Entity as defined in the entity XML file
     * @return all entities of the given type
     */
    public List<GenericValue> findAll(final String entityName) throws GenericEntityException {
        checkIfLocked();
        return findByAnd(entityName, new HashMap<String, Object>(), null);
    }

    /**
     * Finds all Generic entities of the given type, optionally sorted.
     *
     * @param entityName The Name of the Entity as defined in the entity XML file
     * @param orderBy    the entity fields by which to order the query; optionally
     *                   add " ASC" for ascending or " DESC" for descending to each field name
     * @return List containing all Generic entities
     */
    public List<GenericValue> findAll(final String entityName, final List<String> orderBy) throws GenericEntityException {
        checkIfLocked();
        return findByAnd(entityName, new HashMap<String, Object>(), orderBy);
    }

    /**
     * Finds all Generic entities of the given type, looking first in the cache.
     *
     * @param entityName The Name of the Entity as defined in the entity XML file
     * @return List containing all Generic entities
     */
    public List<GenericValue> findAllCache(final String entityName) throws GenericEntityException {
        checkIfLocked();
        return findAllCache(entityName, null);
    }

    /**
     * Finds all Generic entities, looking first in the cache; uses orderBy for
     * lookup, but only keys results on the entityName and fields.
     *
     * @param entityName The Name of the Entity as defined in the entity XML file
     * @param orderBy    The fields of the named entity by which to order the
     *                   query; optionally add " ASC" for ascending or " DESC" for descending
     * @return all Generic entities
     */
    public List<GenericValue> findAllCache(final String entityName, final List<String> orderBy)
            throws GenericEntityException {
        checkIfLocked();
        List<GenericValue> lst = getFromAllCache(entityName);
        if (lst == null) {
            lst = findAll(entityName, orderBy);
            if (lst != null) {
                putInAllCache(entityName, lst);
            }
        }
        return lst;
    }

    /**
     * Finds Generic Entity records by all of the specified fields (ie: combined using AND).
     *
     * @param entityName The Name of the Entity as defined in the entity XML file
     * @param fields     The fields of the named entity to query by with their corresponging values
     * @return List of GenericValue instances that match the query
     */
    public List<GenericValue> findByAnd(final String entityName, final Map<String, ?> fields)
            throws GenericEntityException {
        checkIfLocked();
        return findByAnd(entityName, fields, null);
    }

    /**
     * Finds Generic Entity records by any of the specified fields (i.e. combined using OR).
     *
     * @param entityName The Name of the Entity as defined in the entity XML file
     * @param fields     The fields of the named entity to query by with their corresponging values
     * @return List of GenericValue instances that match the query
     */
    public List<GenericValue> findByOr(final String entityName, final Map<String, ?> fields)
            throws GenericEntityException {
        checkIfLocked();
        return findByOr(entityName, fields, null);
    }

    /**
     * Finds Generic Entity records by all of the specified fields (i.e.
     * combined using AND).
     *
     * @param entityName The Name of the Entity as defined in the entity XML file
     * @param fields     the names and values of the fields by which to query (can be null)
     * @param orderBy    The fields of the named entity to order the query by;
     *                   optionally add a " ASC" for ascending or " DESC" for descending
     * @return List of GenericValue instances that match the query
     */
    public List<GenericValue> findByAnd(
            final String entityName, final Map<String, ?> fields, final List<String> orderBy)
            throws GenericEntityException {
        checkIfLocked();
        final ModelEntity modelEntity = getModelReader().getModelEntity(entityName);
        return findByAnd(modelEntity, fields, orderBy);
    }

    /**
     * Finds any entities matching the given criteria.
     *
     * @param modelEntity the type of entity to find (required)
     * @param fields      the names and values of the fields by which to query (can be null)
     * @param orderBy     the names of fields by which to sort the results (can be null)
     * @return any matching entities
     * @throws GenericEntityException
     */
    public List<GenericValue> findByAnd(
            final ModelEntity modelEntity, final Map<String, ?> fields, final List<String> orderBy)
            throws GenericEntityException {
        checkIfLocked();
        final GenericHelper helper = getEntityHelper(modelEntity);

        if (fields != null && !modelEntity.areFields(fields.keySet())) {
            throw new GenericModelException("At least one of the passed fields is not valid: " + fields.keySet());
        }

        final List<GenericValue> list = helper.findByAnd(modelEntity, fields, orderBy);
        absorbList(list);
        return list;
    }

    /**
     * Finds Generic Entity records by all of the specified fields (i.e.
     * combined using OR).
     *
     * @param entityName The Name of the Entity as defined in the entity XML file
     * @param fields     The fields of the named entity to query by with their corresponging values
     * @param orderBy    The fields of the named entity to order the query by;
     *                   optionally add a " ASC" for ascending or " DESC" for descending
     * @return List of GenericValue instances that match the query
     */
    public List<GenericValue> findByOr(final String entityName, final Map<String, ?> fields, final List<String> orderBy)
            throws GenericEntityException {
        checkIfLocked();
        final ModelEntity modelEntity = getModelReader().getModelEntity(entityName);
        final GenericHelper helper = getEntityHelper(entityName);

        if (fields != null && !modelEntity.areFields(fields.keySet())) {
            throw new IllegalArgumentException(
                    "[GenericDelegator.findByOr] At least of the passed fields is not valid: " + fields.keySet());
        }

        final List<GenericValue> list = helper.findByOr(modelEntity, fields, orderBy);
        absorbList(list);
        return list;
    }

    /**
     * Finds Generic Entity records by all of the specified fields (i.e.
     * combined using AND), looking first in the cache; uses orderBy for
     * lookup, but only keys results on the entityName and fields.
     *
     * @param entityName The Name of the Entity as defined in the entity XML file
     * @param fields     The fields of the named entity to query by with their corresponging values
     * @return List of GenericValue instances that match the query
     */
    public List<GenericValue> findByAndCache(final String entityName, final Map<String, ?> fields)
            throws GenericEntityException {
        checkIfLocked();
        return findByAndCache(entityName, fields, null);
    }

    /**
     * Finds Generic Entity records by all of the specified fields (i.e.
     * combined using AND), looking first in the cache; uses orderBy for
     * lookup, but only keys results on the entityName and fields.
     *
     * @param entityName The Name of the Entity as defined in the entity XML file
     * @param fields     The fields of the named entity to query by with their corresponging values
     * @param orderBy    The fields of the named entity to order the query by;
     *                   optionally add " ASC" for ascending or " DESC" for descending
     * @return List of GenericValue instances that match the query
     */
    public List<GenericValue> findByAndCache(
            final String entityName, final Map<String, ?> fields, final List<String> orderBy)
            throws GenericEntityException {
        checkIfLocked();
        final ModelEntity modelEntity = getModelReader().getModelEntity(entityName);
        List<GenericValue> lst = getFromAndCache(modelEntity, fields);
        if (lst == null) {
            lst = findByAnd(modelEntity, fields, orderBy);
            if (lst != null) {
                putInAndCache(modelEntity, fields, lst);
            }
        }
        return lst;
    }

    /**
     * Finds Generic Entity records by all of the specified expressions (ie: combined using AND).
     *
     * @param entityName  The Name of the Entity as defined in the entity XML file
     * @param expressions The expressions to use for the lookup, each
     *                    consisting of at least a field name, an EntityOperator, and a value to
     *                    compare to
     * @return List of GenericValue instances that match the query
     */
    public List<GenericValue> findByAnd(final String entityName, final List<? extends EntityCondition> expressions)
            throws GenericEntityException {
        checkIfLocked();
        final EntityConditionList ecl = new EntityConditionList(expressions, AND);
        return findByCondition(entityName, ecl, null, null);
    }

    /**
     * Finds Generic Entity records by all of the specified expressions (i.e.
     * combined using AND).
     *
     * @param entityName  The Name of the Entity as defined in the entity XML file
     * @param expressions The expressions to use for the lookup, each
     *                    consisting of at least a field name, an EntityOperator, and a value to
     *                    compare to
     * @param orderBy     The fields of the named entity to order the query by;
     *                    optionally add " ASC" for ascending or " DESC" for descending
     * @return List of GenericValue instances that match the query
     */
    public List<GenericValue> findByAnd(
            final String entityName, final List<? extends EntityCondition> expressions, final List<String> orderBy)
            throws GenericEntityException {
        checkIfLocked();
        final EntityConditionList ecl = new EntityConditionList(expressions, AND);
        return findByCondition(entityName, ecl, null, orderBy);
    }

    /**
     * Finds Generic Entity records by all of the specified expressions (i.e.
     * combined using OR).
     *
     * @param entityName  The Name of the Entity as defined in the entity XML file
     * @param expressions The expressions to use for the lookup, each
     *                    consisting of at least a field name, an EntityOperator, and a value to
     *                    compare to
     * @return List of GenericValue instances that match the query
     */
    public List<GenericValue> findByOr(final String entityName, final List<? extends EntityCondition> expressions)
            throws GenericEntityException {
        checkIfLocked();
        final EntityConditionList ecl = new EntityConditionList(expressions, OR);
        return findByCondition(entityName, ecl, null, null);
    }

    /**
     * Finds Generic Entity records by all of the specified expressions (i.e.
     * combined using OR).
     *
     * @param entityName  The Name of the Entity as defined in the entity XML file
     * @param expressions The expressions to use for the lookup, each
     *                    consisting of at least a field name, an EntityOperator, and a value to compare to
     * @param orderBy     The fields of the named entity to order the query by;
     *                    optionally add " ASC" for ascending or " DESC" for descending
     * @return List of GenericValue instances that match the query
     */
    public List<GenericValue> findByOr(
            final String entityName, final List<? extends EntityCondition> expressions, final List<String> orderBy)
            throws GenericEntityException {
        final EntityConditionList ecl = new EntityConditionList(expressions, OR);
        return findByCondition(entityName, ecl, null, orderBy);
    }

    public List<GenericValue> findByLike(String entityName, Map<String, ?> fields) throws GenericEntityException {
        checkIfLocked();
        return findByLike(entityName, fields, null);
    }

    public List<GenericValue> findByLike(
            final String entityName, final Map<String, ?> fields, final List<String> orderBy)
            throws GenericEntityException {
        checkIfLocked();
        final List<EntityExpr> likeExpressions = new LinkedList<EntityExpr>();
        if (fields != null) {
            for (Map.Entry<String, ?> entry : fields.entrySet()) {
                likeExpressions.add(new EntityExpr(entry.getKey(), LIKE, entry.getValue()));
            }
        }
        final EntityConditionList ecl = new EntityConditionList(likeExpressions, AND);
        return findByCondition(entityName, ecl, null, orderBy);
    }

    /**
     * Finds any GenericValues matching the given conditions.
     *
     * @param entityName      The Name of the Entity as defined in the entity model XML file
     * @param entityCondition The EntityCondition object that specifies how to constrain this query
     * @param fieldsToSelect  The fields of the named entity to get from the
     *                        database; if empty or null all fields will be retreived
     * @param orderBy         The fields of the named entity by which to order the
     *                        query; optionally add " ASC" for ascending or " DESC" for descending
     * @return any matching values
     */
    public List<GenericValue> findByCondition(final String entityName, final EntityCondition entityCondition,
                                              final Collection<String> fieldsToSelect, final List<String> orderBy)
            throws GenericEntityException {
        checkIfLocked();
        final ModelEntity modelEntity = getModelReader().getModelEntity(entityName);
        if (entityCondition != null) {
            entityCondition.checkCondition(modelEntity);
        }
        final GenericHelper helper = getEntityHelper(entityName);
        final List<GenericValue> list = helper.findByCondition(modelEntity, entityCondition, fieldsToSelect, orderBy);
        absorbList(list);
        return list;
    }

    /**
     * Returns the count of the results that match all of the specified expressions (i.e. combined using AND).
     *
     * @param entityName  The Name of the Entity as defined in the entity model XML file
     * @param fieldName   The field of the named entity to count, if null this is equivalent to count(*)
     * @param expressions The expressions to use for the lookup, each
     *                    consisting of at least a field name, an EntityOperator, and a value to
     *                    compare to
     * @param findOptions An instance of EntityFindOptions that specifies
     *                    advanced query options.  The only option that is used is distinct, in
     *                    which case a select (distinct fieldname) is issued.<p>If you issue a
     *                    distinct without a fieldName, it will be ignored because <code>select
     *                    count (distinct *)</code> makes no sense
     * @return the number of rows that match the query
     */
    public int countByAnd(final String entityName, final String fieldName,
                          final List<? extends EntityCondition> expressions, final EntityFindOptions findOptions)
            throws GenericEntityException {
        checkIfLocked();
        final EntityConditionList ecl = (expressions == null) ? null : new EntityConditionList(expressions, AND);
        return countByCondition(entityName, fieldName, ecl, findOptions);
    }

    /**
     * Returns the count of the results that match any of the specified
     * expressions (i.e. combined using OR).
     *
     * @param entityName  The Name of the Entity as defined in the entity model XML file
     * @param fieldName   The field of the named entity to count, if null this is equivalent to count(*)
     * @param expressions The expressions to use for the lookup, each
     *                    consisting of at least a field name, an EntityOperator, and a value to compare to
     * @param findOptions An instance of EntityFindOptions that specifies
     *                    advanced query options. The only option that is used is distinct, in
     *                    which case a <code>select (distinct fieldname)</code> is issued.<p>If
     *                    you issue a distinct without a fieldName, it will be ignored because
     *                    <code>select count (distinct *)</code> makes no sense
     * @return the number of rows that match the query
     */
    public int countByOr(final String entityName, final String fieldName,
                         final List<? extends EntityCondition> expressions, final EntityFindOptions findOptions)
            throws GenericEntityException {
        checkIfLocked();
        final EntityConditionList ecl = (expressions == null) ? null : new EntityConditionList(expressions, OR);
        return countByCondition(entityName, fieldName, ecl, findOptions);
    }

    /**
     * Returns the count of the results that match any of the specified expressions (ie: combined using OR).
     *
     * @param entityName      The Name of the Entity as defined in the entity model XML file
     * @param fieldName       The field of the named entity to count, if null this is equivalent to count(*)
     * @param entityCondition The EntityCondition object that specifies how to
     *                        constrain this query The expressions to use for the lookup, each
     *                        consisting of at least a field name, an EntityOperator, and a value to
     *                        compare to
     * @param findOptions     An instance of EntityFindOptions that specifies
     *                        advanced query options.  The only option that is used is distinct, in
     *                        which case a select (distinct fieldname) is issued.<p>If you issue a
     *                        distinct without a fieldName, it will be ignored as <code>select count
     *                        (distinct *)</code> makes no sense
     * @return the number of rows that match the query
     */
    public int countByCondition(final String entityName, final String fieldName, final EntityCondition entityCondition,
                                final EntityFindOptions findOptions)
            throws GenericEntityException {
        checkIfLocked();
        ModelEntity modelEntity = getModelReader().getModelEntity(entityName);
        if (entityCondition != null) {
            entityCondition.checkCondition(modelEntity);
        }
        GenericHelper helper = getEntityHelper(entityName);
        return helper.count(modelEntity, fieldName, entityCondition, findOptions);
    }

    /**
     * Returns the row count of the specified entity.
     *
     * @param entityName The Name of the Entity as defined in the entity model XML file
     * @return the number of rows in the table
     */
    public int countAll(final String entityName) throws GenericEntityException {
        checkIfLocked();
        return countByCondition(entityName, null, null, null);
    }

    /**
     * Finds GenericValues by the given conditions.
     *
     * @param entityName      The Name of the Entity as defined in the entity model
     *                        XML file
     * @param entityCondition The EntityCondition object that specifies how to
     *                        constrain this query before any groupings are done (if this is a view
     *                        entity with group-by aliases)
     * @param fieldsToSelect  The fields of the named entity to get from the
     *                        database; if empty or null all fields will be retreived
     * @param orderBy         The fields of the named entity to order the query by;
     *                        optionally add " ASC" for ascending or " DESC" for descending
     * @return EntityListIterator representing the result of the query: NOTE
     * THAT THIS MUST BE CLOSED WHEN YOU ARE DONE WITH IT, AND DON'T LEAVE IT
     * OPEN TOO LONG BECAUSE IT WILL MAINTAIN A DATABASE CONNECTION.
     */
    public EntityListIterator findListIteratorByCondition(
            final String entityName, final EntityCondition entityCondition, final Collection<String> fieldsToSelect,
            final List<String> orderBy)
            throws GenericEntityException {
        checkIfLocked();
        return findListIteratorByCondition(
                entityName, entityCondition, null, fieldsToSelect, orderBy, null);
    }

    /**
     * Finds GenericValues by the given conditions.
     *
     * @param entityName            The ModelEntity of the Entity as defined in the entity
     *                              XML file
     * @param whereEntityCondition  The EntityCondition object that specifies
     *                              how to constrain this query before any groupings are done (if this is a
     *                              view entity with group-by aliases)
     * @param havingEntityCondition The EntityCondition object that specifies
     *                              how to constrain this query after any groupings are done (if this is a
     *                              view entity with group-by aliases)
     * @param fieldsToSelect        The fields of the named entity to get from the
     *                              database; if empty or null all fields will be retreived
     * @param orderBy               The fields of the named entity to order the query by;
     *                              optionally add " ASC" for ascending or " DESC" for descending
     * @param findOptions           An instance of EntityFindOptions that specifies
     *                              advanced query options. See the EntityFindOptions JavaDoc for more
     *                              details.
     * @return EntityListIterator representing the result of the query: NOTE
     * THAT THIS MUST BE CLOSED WHEN YOU ARE DONE WITH IT, AND DON'T LEAVE IT
     * OPEN TOO LONG BECAUSE IT WILL MAINTAIN A DATABASE CONNECTION.
     * @see EntityCondition
     */
    public EntityListIterator findListIteratorByCondition(
            final String entityName, final EntityCondition whereEntityCondition,
            final EntityCondition havingEntityCondition, final Collection<String> fieldsToSelect,
            final List<String> orderBy, final EntityFindOptions findOptions)
            throws GenericEntityException {
        checkIfLocked();
        final ModelEntity modelEntity = getModelReader().getModelEntity(entityName);
        if (whereEntityCondition != null) {
            whereEntityCondition.checkCondition(modelEntity);
        }
        if (havingEntityCondition != null) {
            havingEntityCondition.checkCondition(modelEntity);
        }
        final GenericHelper helper = getEntityHelper(entityName);
        final EntityListIterator eli = helper.findListIteratorByCondition(modelEntity, whereEntityCondition,
                havingEntityCondition, fieldsToSelect, orderBy, findOptions);
        eli.setDelegator(this);
        return eli;
    }

    /**
     * Remove a Generic Entity corresponding to the primaryKey.
     *
     * @param primaryKey The primary key of the entity to remove.
     * @return int representing number of rows affected by this operation
     */
    public int removeByPrimaryKey(final GenericPK primaryKey) throws GenericEntityException {
        checkIfLocked();
        return removeByPrimaryKey(primaryKey, true);
    }

    /**
     * Remove a Generic Entity corresponding to the primaryKey.
     *
     * @param primaryKey   The primary key of the entity to remove.
     * @param doCacheClear boolean that specifies whether to clear cache entries for this primaryKey to be removed
     * @return int representing number of rows affected by this operation
     */
    public int removeByPrimaryKey(final GenericPK primaryKey, final boolean doCacheClear) throws GenericEntityException {
        checkIfLocked();
        if (doCacheClear) {
            // always clear cache before the operation
            clearCacheLine(primaryKey);
        }
        final GenericHelper helper = getEntityHelper(primaryKey.getEntityName());
        return helper.removeByPrimaryKey(primaryKey);
    }

    /**
     * Remove a Generic Value from the database.
     *
     * @param value The GenericValue object of the entity to remove.
     * @return int representing number of rows affected by this operation
     */
    public int removeValue(final GenericValue value) throws GenericEntityException {
        checkIfLocked();
        return removeValue(value, true);
    }

    /**
     * Remove a Generic Value from the database.
     *
     * @param value        The GenericValue object of the entity to remove.
     * @param doCacheClear boolean that specifies whether to clear cache entries for this value to be removed
     * @return int representing number of rows affected by this operation
     */
    public int removeValue(final GenericValue value, final boolean doCacheClear) throws GenericEntityException {
        checkIfLocked();
        final GenericHelper helper = getEntityHelper(value.getEntityName());
        if (doCacheClear) {
            clearCacheLine(value);
        }
        return helper.removeByPrimaryKey(value.getPrimaryKey());
    }

    /**
     * Removes/deletes Generic Entity records found by all of the specified fields (ie: combined using AND).
     *
     * @param entityName The Name of the Entity as defined in the entity XML file
     * @param fields     The fields of the named entity to query by with their corresponging values
     * @return int representing number of rows affected by this operation
     */
    public int removeByAnd(final String entityName, final Map<String, ?> fields) throws GenericEntityException {
        checkIfLocked();
        return removeByAnd(entityName, fields, true);
    }

    /**
     * Removes/deletes Generic Entity records found by all of the specified fields (ie: combined using AND).
     *
     * @param entityName   The Name of the Entity as defined in the entity XML file
     * @param fields       The fields of the named entity to query by with their corresponging values
     * @param doCacheClear boolean that specifies whether to clear cache entries for this value to be removed
     * @return int representing number of rows affected by this operation
     */
    public int removeByAnd(final String entityName, final Map<String, ?> fields, final boolean doCacheClear)
            throws GenericEntityException {
        checkIfLocked();
        final GenericValue dummyValue = makeValue(entityName, fields);
        final ModelEntity modelEntity = getModelReader().getModelEntity(entityName);
        final GenericHelper helper = getEntityHelper(entityName);
        if (doCacheClear) {
            // always clear cache before the operation
            clearCacheLine(entityName, fields);
        }
        return helper.removeByAnd(modelEntity, dummyValue.getAllFields());
    }

    /**
     * Removes/deletes Generic Entity records found by matching the EntityCondition.
     *
     * @param entityName     The Name of the Entity as defined in the entity XML file
     * @param whereCondition The EntityCondition object that specifies how to constrain this query
     * @return int representing number of rows affected by this operation
     */
    public int removeByCondition(final String entityName, final EntityCondition whereCondition)
            throws GenericEntityException {
        checkIfLocked();
        return removeByCondition(entityName, whereCondition, true);
    }

    /**
     * Removes/deletes Generic Entity records found by matching the EntityCondition.
     *
     * @param entityName     The Name of the Entity as defined in the entity XML file
     * @param whereCondition The EntityCondition object that specifies how to constrain this query
     * @param doCacheClear   boolean that specifies whether to clear cache entries for this value to be removed
     * @return int representing number of rows affected by this operation
     */
    public int removeByCondition(
            final String entityName, final EntityCondition whereCondition, final boolean doCacheClear)
            throws GenericEntityException {
        checkIfLocked();
        ModelEntity modelEntity = getModelReader().getModelEntity(entityName);
        GenericHelper helper = getEntityHelper(entityName);

        if (doCacheClear) {
            // always clear cache before the operation
            Collection<GenericValue> toBeDeleted = helper.findByCondition(modelEntity, whereCondition, null, null);
            clearAllCacheLinesByValue(toBeDeleted);
        }
        return helper.removeByCondition(modelEntity, whereCondition);
    }

    /**
     * Get the named Related Entity for the GenericValue from the persistent
     * store across another Relation. Helps to get related Values in a
     * multi-to-multi relationship.
     *
     * @param relationNameOne String containing the relation name which is the
     *                        combination of relation.title and relation.rel-entity-name as
     *                        specified in the entity XML definition file, for first relation
     * @param relationNameTwo String containing the relation name for second relation
     * @param value           GenericValue instance containing the entity
     * @param orderBy         The fields of the named entity to order the query by; may be null;
     *                        optionally add a " ASC" for ascending or " DESC" for descending
     * @return List of GenericValue instances as specified in the relation definition
     */
    public List<GenericValue> getMultiRelation(final GenericValue value, final String relationNameOne,
                                               final String relationNameTwo, final List<String> orderBy)
            throws GenericEntityException {
        checkIfLocked();
        // traverse the relationships
        ModelEntity modelEntity = value.getModelEntity();
        ModelRelation modelRelationOne = modelEntity.getRelation(relationNameOne);
        ModelEntity modelEntityOne = getModelEntity(modelRelationOne.getRelEntityName());
        ModelRelation modelRelationTwo = modelEntityOne.getRelation(relationNameTwo);
        ModelEntity modelEntityTwo = getModelEntity(modelRelationTwo.getRelEntityName());

        GenericHelper helper = getEntityHelper(modelEntity);

        return helper.findByMultiRelation(
                value, modelRelationOne, modelEntityOne, modelRelationTwo, modelEntityTwo, orderBy);
    }

    /**
     * Get the named Related Entity for the GenericValue from the persistent
     * store across another Relation. Helps to get related Values in a
     * multi-to-multi relationship.
     *
     * @param relationNameOne String containing the relation name which is the
     *                        combination of relation.title and relation.rel-entity-name as
     *                        specified in the entity XML definition file, for first relation
     * @param relationNameTwo String containing the relation name for second relation
     * @param value           GenericValue instance containing the entity
     * @return List of GenericValue instances as specified in the relation definition
     */
    public List<GenericValue> getMultiRelation(
            final GenericValue value, final String relationNameOne, final String relationNameTwo)
            throws GenericEntityException {
        checkIfLocked();
        return getMultiRelation(value, relationNameOne, relationNameTwo, null);
    }

    /**
     * Get the named Related Entity for the GenericValue from the persistent store
     *
     * @param relationName String containing the relation name which is the
     *                     combination of relation.title and relation.rel-entity-name as
     *                     specified in the entity XML definition file
     * @param value        GenericValue instance containing the entity
     * @return List of GenericValue instances as specified in the relation definition
     */
    public List<GenericValue> getRelated(final String relationName, final GenericValue value)
            throws GenericEntityException {
        checkIfLocked();
        return getRelated(relationName, null, null, value);
    }

    /**
     * Get the named Related Entity for the GenericValue from the persistent store.
     *
     * @param relationName String containing the relation name which is the
     *                     combination of relation.title and relation.rel-entity-name as
     *                     specified in the entity XML definition file
     * @param byAndFields  the fields that must equal in order to keep; may be null
     * @param value        GenericValue instance containing the entity
     * @return List of GenericValue instances as specified in the relation definition
     */
    public List<GenericValue> getRelatedByAnd(
            final String relationName, final Map<String, ?> byAndFields, final GenericValue value)
            throws GenericEntityException {
        checkIfLocked();
        return getRelated(relationName, byAndFields, null, value);
    }

    /**
     * Get the named Related Entity for the GenericValue from the persistent store.
     *
     * @param relationName String containing the relation name which is the
     *                     combination of relation.title and relation.rel-entity-name as
     *                     specified in the entity XML definition file
     * @param orderBy      The fields of the named entity to order the query by; may be null;
     *                     optionally add a " ASC" for ascending or " DESC" for descending
     * @param value        GenericValue instance containing the entity
     * @return List of GenericValue instances as specified in the relation definition
     */
    public List<GenericValue> getRelatedOrderBy(
            final String relationName, final List<String> orderBy, final GenericValue value)
            throws GenericEntityException {
        return getRelated(relationName, null, orderBy, value);
    }

    /**
     * Get the named Related Entity for the GenericValue from the persistent store.
     *
     * @param relationName String containing the relation name which is the
     *                     combination of relation.title and relation.rel-entity-name as
     *                     specified in the entity XML definition file
     * @param byAndFields  the fields that must equal in order to keep; may be null
     * @param orderBy      The fields of the named entity to order the query by; may be null;
     *                     optionally add a " ASC" for ascending or " DESC" for descending
     * @param value        GenericValue instance containing the entity
     * @return List of GenericValue instances as specified in the relation definition
     */
    public List<GenericValue> getRelated(final String relationName, final Map<String, ?> byAndFields,
                                         final List<String> orderBy, final GenericValue value)
            throws GenericEntityException {
        checkIfLocked();
        ModelEntity modelEntity = value.getModelEntity();
        ModelRelation relation = modelEntity.getRelation(relationName);

        if (relation == null) {
            throw new GenericModelException(
                    "Could not find relation for relationName: " + relationName + " for value " + value);
        }

        // put the byAndFields (if not null) into the hash map first,
        // they will be overridden by value's fields if over-specified this is important for security and cleanliness
        final Map<String, Object> fields = byAndFields == null ?
                new HashMap<String, Object>() : new HashMap<String, Object>(byAndFields);
        for (int i = 0; i < relation.getKeyMapsSize(); i++) {
            ModelKeyMap keyMap = relation.getKeyMap(i);
            fields.put(keyMap.getRelFieldName(), value.get(keyMap.getFieldName()));
        }

        return findByAnd(relation.getRelEntityName(), fields, orderBy);
    }

    /**
     * Get a dummy primary key for the named Related Entity for the GenericValue.
     *
     * @param relationName String containing the relation name which is the
     *                     combination of relation.title and relation.rel-entity-name as
     *                     specified in the entity XML definition file
     * @param byAndFields  the fields that must equal in order to keep; may be null
     * @param value        GenericValue instance containing the entity
     * @return GenericPK containing a possibly incomplete PrimaryKey object representing the related entity or entities
     */
    public GenericPK getRelatedDummyPK(
            final String relationName, final Map<String, ?> byAndFields, final GenericValue value)
            throws GenericEntityException {
        checkIfLocked();
        ModelEntity modelEntity = value.getModelEntity();
        ModelRelation relation = modelEntity.getRelation(relationName);

        if (relation == null) {
            throw new GenericModelException(
                    "Could not find relation for relationName: " + relationName + " for value " + value);
        }
        ModelEntity relatedEntity = getModelReader().getModelEntity(relation.getRelEntityName());

        // put the byAndFields (if not null) into the hash map first,
        // they will be overridden by value's fields if over-specified this is important for security and cleanliness
        final Map<String, Object> fields = byAndFields == null ?
                new HashMap<String, Object>() : new HashMap<String, Object>(byAndFields);
        for (int i = 0; i < relation.getKeyMapsSize(); i++) {
            ModelKeyMap keyMap = relation.getKeyMap(i);
            fields.put(keyMap.getRelFieldName(), value.get(keyMap.getFieldName()));
        }

        GenericPK dummyPK = new GenericPK(relatedEntity, fields);
        dummyPK.setDelegator(this);
        return dummyPK;
    }

    /**
     * Get the named Related Entity for the GenericValue from the persistent
     * store, checking first in the cache to see if the desired value is there.
     *
     * @param relationName String containing the relation name which is the
     *                     combination of relation.title and relation.rel-entity-name as
     *                     specified in the entity XML definition file
     * @param value        GenericValue instance containing the entity
     * @return List of GenericValue instances as specified in the relation definition
     */
    public List<GenericValue> getRelatedCache(final String relationName, final GenericValue value)
            throws GenericEntityException {
        checkIfLocked();
        ModelEntity modelEntity = value.getModelEntity();
        ModelRelation relation = modelEntity.getRelation(relationName);

        if (relation == null) {
            throw new GenericModelException(
                    "Could not find relation for relationName: " + relationName + " for value " + value);
        }

        Map<String, Object> fields = new HashMap<String, Object>();
        for (int i = 0; i < relation.getKeyMapsSize(); i++) {
            ModelKeyMap keyMap = relation.getKeyMap(i);
            fields.put(keyMap.getRelFieldName(), value.get(keyMap.getFieldName()));
        }

        return findByAndCache(relation.getRelEntityName(), fields, null);
    }

    /**
     * Get related entity where relation is of type one, uses findByPrimaryKey.
     *
     * @param relationName the name of the relation to get (required)
     * @param value        the value whose relation to get (required)
     * @throws IllegalArgumentException if the list found has more than one item
     */
    public GenericValue getRelatedOne(final String relationName, final GenericValue value) throws GenericEntityException {
        checkIfLocked();
        ModelRelation relation = value.getModelEntity().getRelation(relationName);

        if (relation == null) {
            throw new GenericModelException(
                    "Could not find relation for relationName: " + relationName + " for value " + value);
        }
        if (!"one".equals(relation.getType()) && !"one-nofk".equals(relation.getType())) {
            throw new IllegalArgumentException("Relation is not a 'one' or a 'one-nofk' relation: " + relationName +
                    " of entity " + value.getEntityName());
        }

        Map<String, Object> fields = new HashMap<String, Object>();
        for (int i = 0; i < relation.getKeyMapsSize(); i++) {
            ModelKeyMap keyMap = relation.getKeyMap(i);
            fields.put(keyMap.getRelFieldName(), value.get(keyMap.getFieldName()));
        }

        return findByPrimaryKey(relation.getRelEntityName(), fields);
    }

    /**
     * Get related entity where relation is of type one, uses findByPrimaryKey,
     * checking first in the cache to see if the desired value is there.
     *
     * @param relationName the name of the relation to get (required)
     * @param value        the value whose relation to get (required)
     * @throws IllegalArgumentException if the list found has more than one item
     */
    public GenericValue getRelatedOneCache(final String relationName, final GenericValue value)
            throws GenericEntityException {
        checkIfLocked();
        ModelEntity modelEntity = value.getModelEntity();
        ModelRelation relation = modelEntity.getRelation(relationName);

        if (relation == null) {
            throw new GenericModelException(
                    "Could not find relation for relationName: " + relationName + " for value " + value);
        }
        if (!"one".equals(relation.getType()) && !"one-nofk".equals(relation.getType())) {
            throw new IllegalArgumentException("Relation is not a 'one' or a 'one-nofk' relation: " + relationName +
                    " of entity " + value.getEntityName());
        }

        Map<String, Object> fields = new HashMap<String, Object>();
        for (int i = 0; i < relation.getKeyMapsSize(); i++) {
            ModelKeyMap keyMap = relation.getKeyMap(i);
            fields.put(keyMap.getRelFieldName(), value.get(keyMap.getFieldName()));
        }

        return findByPrimaryKeyCache(relation.getRelEntityName(), fields);
    }

    /**
     * Remove the named Related Entity for the GenericValue from the persistent store.
     *
     * @param relationName String containing the relation name which is the
     *                     combination of relation.title and relation.rel-entity-name as
     *                     specified in the entity XML definition file
     * @param value        GenericValue instance containing the entity
     * @return int representing number of rows affected by this operation
     */
    public int removeRelated(final String relationName, final GenericValue value) throws GenericEntityException {
        checkIfLocked();
        return removeRelated(relationName, value, true);
    }

    /**
     * Remove the named Related Entity for the GenericValue from the persistent store.
     *
     * @param relationName String containing the relation name which is the
     *                     combination of relation.title and relation.rel-entity-name as specified
     *                     in the entity XML definition file
     * @param value        GenericValue instance containing the entity
     * @param doCacheClear boolean that specifies whether to clear cache
     *                     entries for this value to be removed
     * @return int representing number of rows affected by this operation
     */
    public int removeRelated(final String relationName, final GenericValue value, final boolean doCacheClear)
            throws GenericEntityException {
        ModelEntity modelEntity = value.getModelEntity();
        ModelRelation relation = modelEntity.getRelation(relationName);

        if (relation == null) {
            throw new GenericModelException(
                    "Could not find relation for relationName: " + relationName + " for value " + value);
        }

        Map<String, Object> fields = new HashMap<String, Object>();
        for (int i = 0; i < relation.getKeyMapsSize(); i++) {
            ModelKeyMap keyMap = relation.getKeyMap(i);
            fields.put(keyMap.getRelFieldName(), value.get(keyMap.getFieldName()));
        }

        return removeByAnd(relation.getRelEntityName(), fields, doCacheClear);
    }

    /**
     * Refresh the Entity for the GenericValue from the persistent store.
     *
     * @param value GenericValue instance containing the entity to refresh
     */
    public void refresh(final GenericValue value) throws GenericEntityException {
        checkIfLocked();
        refresh(value, true);
    }

    /**
     * Refresh the Entity for the GenericValue from the persistent store.
     *
     * @param value        GenericValue instance containing the entity to refresh
     * @param doCacheClear whether to automatically clear cache entries related to this operation
     */
    public void refresh(final GenericValue value, final boolean doCacheClear) throws GenericEntityException {
        checkIfLocked();
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

    /**
     * Store the Entity from the GenericValue to the persistent store.
     *
     * @param value GenericValue instance containing the entity
     * @return int representing number of rows affected by this operation
     */
    public int store(final GenericValue value) throws GenericEntityException {
        checkIfLocked();
        return store(value, true);
    }

    /**
     * Store the Entity from the GenericValue to the persistent store.
     *
     * @param value        GenericValue instance containing the entity
     * @param doCacheClear whether to automatically clear cache entries related to this operation
     * @return int representing number of rows affected by this operation
     */
    public int store(final GenericValue value, final boolean doCacheClear) throws GenericEntityException {
        checkIfLocked();
        final GenericHelper helper = getEntityHelper(value.getEntityName());

        if (doCacheClear) {
            // always clear cache before the operation
            clearCacheLine(value);
        }

        final int retVal = helper.store(value);
        // refresh the valueObject to get the new version
        if (value.lockEnabled()) {
            refresh(value, doCacheClear);
        }
        return retVal;
    }

    /**
     * Store the Entities from the List GenericValue instances to the persistent store.
     * <br>This is different than the normal store method in that the store method only does
     * an update, while the storeAll method checks to see if each entity exists, then
     * either does an insert or an update as appropriate.
     * <br>These updates all happen in one transaction, so they will either all succeed or all fail,
     * if the data source supports transactions. This is just like to othersToStore feature
     * of the GenericEntity on a create or store.
     *
     * @param values List of GenericValue instances containing the entities to store
     * @return int representing number of rows affected by this operation
     */
    public int storeAll(final List<? extends GenericValue> values) throws GenericEntityException {
        checkIfLocked();
        return storeAll(values, true);
    }

    /**
     * Store the Entities from the List GenericValue instances to the persistent store.
     * <br>This is different than the normal store method in that the store method only does
     * an update, while the storeAll method checks to see if each entity exists, then
     * either does an insert or an update as appropriate.
     * <br>These updates all happen in one transaction, so they will either all succeed or all fail,
     * if the data source supports transactions. This is just like to othersToStore feature
     * of the GenericEntity on a create or store.
     *
     * @param values       List of GenericValue instances containing the entities to store
     * @param doCacheClear whether to automatically clear cache entries related to this operation
     * @return int representing number of rows affected by this operation
     */
    public int storeAll(final List<? extends GenericValue> values, final boolean doCacheClear)
            throws GenericEntityException {
        checkIfLocked();
        if (values == null) {
            return 0;
        }

        // from the delegator level this is complicated because different GenericValue
        // objects in the list may correspond to different helpers
        Map<String, List<GenericValue>> valuesPerHelper = new HashMap<String, List<GenericValue>>();
        Iterator<? extends GenericValue> viter = values.iterator();

        while (viter.hasNext()) {
            GenericValue value = viter.next();
            String helperName = getEntityHelperName(value.getEntityName());
            List<GenericValue> helperValues = valuesPerHelper.get(helperName);
            if (helperValues == null) {
                helperValues = new LinkedList<GenericValue>();
                valuesPerHelper.put(helperName, helperValues);
            }
            helperValues.add(value);
        }

        boolean beganTransaction = false;
        int numberChanged = 0;

        try {
            // This code caught me out, valuesPerHelper is a confusing name for this list
            // effectively in JIRA we have 1 helper defaultDS - so we will only have one element in this list
            // We connect via JDBC so are not enlisted in XA transactions, so for the JIrA case we don't
            // want to start the XA transaction manager.  It is up to the SQLProcessors to do the committing
            if (valuesPerHelper.size() > 1) {
                beganTransaction = TransactionUtil.begin();
            }

            for (Map.Entry<String, List<GenericValue>> stringListEntry : valuesPerHelper.entrySet()) {
                String helperName = stringListEntry.getKey();
                GenericHelper helper = GenericHelperFactory.getHelper(helperName);

                if (doCacheClear) {
                    clearAllCacheLinesByValue(stringListEntry.getValue());
                }
                numberChanged += helper.storeAll(stringListEntry.getValue());
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
            GenericValue value = viter.next();
            if (value.lockEnabled()) {
                refresh(value);
            }
        }

        return numberChanged;
    }

    /**
     * Remove the Entities from the List from the persistent store.
     * <br>The List contains GenericEntity objects, can be either GenericPK or GenericValue.
     * <br>If a certain entity contains a complete primary key, the entity in the datasource corresponding
     * to that primary key will be removed, this is like a removeByPrimary Key.
     * <br>On the other hand, if a certain entity is an incomplete or non primary key,
     * if will behave like the removeByAnd method.
     * <br>These updates all happen in one transaction, so they will either all succeed or all fail,
     * if the data source supports transactions.
     *
     * @param dummyPKs Collection of GenericEntity instances containing the entities or by and fields to remove
     * @return int representing number of rows affected by this operation
     */
    public int removeAll(List<? extends GenericEntity> dummyPKs) throws GenericEntityException {
        checkIfLocked();
        return removeAll(dummyPKs, true);
    }

    /**
     * Remove the Entities from the List from the persistent store.
     * <br>The List contains GenericEntity objects, can be either GenericPK or GenericValue.
     * <br>If a certain entity contains a complete primary key, the entity in the datasource corresponding
     * to that primary key will be removed, this is like a removeByPrimary Key.
     * <br>On the other hand, if a certain entity is an incomplete or non primary key,
     * if will behave like the removeByAnd method.
     * <br>These updates all happen in one transaction, so they will either all succeed or all fail,
     * if the data source supports transactions.
     *
     * @param dummyPKs     Collection of GenericEntity instances containing the entities or by and fields to remove
     * @param doCacheClear whether to automatically clear cache entries related to this operation
     * @return int representing number of rows affected by this operation
     */
    public int removeAll(List<? extends GenericEntity> dummyPKs, boolean doCacheClear) throws GenericEntityException {
        checkIfLocked();
        if (dummyPKs == null) {
            return 0;
        }

        // from the delegator level this is complicated because different GenericValue
        // objects in the list may correspond to different helpers
        HashMap<String, List<GenericEntity>> valuesPerHelper = new HashMap<String, List<GenericEntity>>();

        for (GenericEntity dummyPK : dummyPKs) {
            String helperName = getEntityHelperName(dummyPK.getEntityName());
            List<GenericEntity> helperValues = valuesPerHelper.get(helperName);

            if (helperValues == null) {
                helperValues = new LinkedList<GenericEntity>();
                valuesPerHelper.put(helperName, helperValues);
            }
            helperValues.add(dummyPK);
        }

        boolean beganTransaction = false;
        int numRemoved = 0;

        try {
            // if there are multiple helpers and no transaction is active, begin one
            if (valuesPerHelper.size() > 1) {
                beganTransaction = TransactionUtil.begin();
            }

            for (Map.Entry<String, List<GenericEntity>> entry : valuesPerHelper.entrySet()) {
                String helperName = entry.getKey();
                GenericHelper helper = GenericHelperFactory.getHelper(helperName);

                if (doCacheClear) {
                    clearAllCacheLinesByDummyPK(entry.getValue());
                }
                numRemoved += helper.removeAll(entry.getValue());
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

    /**
     * This method is a shortcut to completely clear all entity engine caches.
     * For performance reasons this should not be called very often.
     */
    public void clearAllCaches() {
        checkIfLocked();
        clearAllCaches(true);
    }

    public void clearAllCaches(boolean distribute) {
        checkIfLocked();
        if (allCache != null) allCache.clear();
        if (andCache != null) andCache.clear();
        if (andCacheFieldSets != null) andCacheFieldSets.clear();
        if (primaryKeyCache != null) primaryKeyCache.clear();

        if (distribute && distributedCacheClear != null) {
            distributedCacheClear.clearAllCaches();
        }
    }

    /**
     * Remove a CACHED Generic Entity (List) from the cache, either a PK, ByAnd, or All
     *
     * @param entityName The Name of the Entity as defined in the entity XML file
     * @param fields     The fields of the named entity to query by with their corresponging values
     */
    public void clearCacheLine(String entityName, Map<String, ?> fields) {
        checkIfLocked();
        // if no fields passed, do the all cache quickly and return
        if (fields == null && allCache != null) {
            allCache.remove(entityName);
            return;
        }

        ModelEntity entity = getModelEntity(entityName);
        if (entity == null) {
            throw new IllegalArgumentException(
                    "[GenericDelegator.clearCacheLine] could not find entity for entityName: " + entityName);
        }
        //if never cached, then don't bother clearing
        if (entity.getNeverCache()) return;

        GenericPK dummyPK = new GenericPK(entity, fields);
        clearCacheLineFlexible(dummyPK);
    }

    /**
     * Remove a CACHED Generic Entity from the cache by its primary key. Checks
     * whether the passed GenericPK is a complete primary key; if it is, then
     * the cache line will be removed from the primaryKeyCache; if it
     * is NOT a complete primary key it will remove the cache line from the andCache.
     * If the fields map is empty, then the allCache for the entity will be cleared.
     *
     * @param dummyPK The dummy primary key to clear by.
     */
    public void clearCacheLineFlexible(final GenericEntity dummyPK) {
        checkIfLocked();
        clearCacheLineFlexible(dummyPK, true);
    }

    public void clearCacheLineFlexible(final GenericEntity dummyPK, final boolean distribute) {
        checkIfLocked();
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

            if (distribute && distributedCacheClear != null) {
                distributedCacheClear.distributedClearCacheLineFlexible(dummyPK);
            }
        }
    }

    /**
     * Remove a CACHED Generic Entity from the cache by its primary key, does NOT
     * check to see if the passed GenericPK is a complete primary key.
     * Also tries to clear the corresponding all cache entry.
     *
     * @param primaryKey The primary key to clear by.
     */
    public void clearCacheLine(final GenericPK primaryKey) {
        checkIfLocked();
        clearCacheLine(primaryKey, true);
    }

    public void clearCacheLine(final GenericPK primaryKey, final boolean distribute) {
        checkIfLocked();
        if (primaryKey == null) {
            return;
        }

        // if never cached, then don't bother clearing
        if (primaryKey.getModelEntity().getNeverCache()) {
            return;
        }

        // always auto clear the all cache too, since we know it's messed up in any case
        if (allCache != null) {
            allCache.remove(primaryKey.getEntityName());
        }

        if (primaryKeyCache != null) {
            primaryKeyCache.remove(primaryKey);
        }

        if (distribute && distributedCacheClear != null) {
            distributedCacheClear.distributedClearCacheLine(primaryKey);
        }
    }

    /**
     * Remove a CACHED GenericValue from as many caches as it can.
     * Automatically tries to remove entries from the all cache, the by primary
     * key cache, and the "by and" cache. This is the ONLY method that tries to
     * clear automatically from the by and cache.
     *
     * @param value The primary key to clear by.
     */
    public void clearCacheLine(final GenericValue value) {
        checkIfLocked();
        clearCacheLine(value, true);
    }

    public void clearCacheLine(final GenericValue value, final boolean distribute) {
        checkIfLocked();
        /*
            TODO: make this a bit more intelligent by passing in the operation
            being done (create, update, remove) so we don't clear the cache
            unnecessarily. For instance:
            * on create, don't clear by primary cache (and don't clear original
              values because there won't be any)
            * on remove, don't clear "by and" for new values, but do for original values
         */

        if (value == null) return;

        // If never cached, then don't bother clearing
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
        Set<Set<String>> fieldNameSets = andCacheFieldSets.get(value.getEntityName());

        if (fieldNameSets != null) {
            // note that if fieldNameSets is null then no by and caches have been
            // stored for this entity, so do nothing; ie only run this if not null

            // iterate through the list of field combination sets and do a cache clear
            // for each one using field values from this entity value object

            for (Set<String> fieldNameSet : fieldNameSets) {

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


                Map<String, Object> originalFieldValues = null;

                if (value.isModified() && value.originalDbValuesAvailable()) {
                    originalFieldValues = new HashMap<String, Object>();
                }
                Map<String, Object> fieldValues = new HashMap<String, Object>();

                for (String fieldName : fieldNameSet) {

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

        if (distribute && distributedCacheClear != null) {
            distributedCacheClear.distributedClearCacheLine(value);
        }
    }

    /**
     * Gets a Set of Sets of fieldNames used in the by and cache for the given entityName.
     *
     * @param entityName the entity for which to get the field names (can be null)
     * @return null if the field name is null or simply unknown
     */
    public Set<Set<String>> getFieldNameSetsCopy(final String entityName) {
        checkIfLocked();
        final Set<Set<String>> fieldNameSets = andCacheFieldSets.get(entityName);

        if (fieldNameSets == null) {
            return null;
        }

        // create a new container set and a copy of each entry set
        final Set<Set<String>> setsCopy = new TreeSet<Set<String>>();
        for (final Set<String> fieldNameSet : fieldNameSets) {
            setsCopy.add(new TreeSet<String>(fieldNameSet));
        }
        return setsCopy;
    }

    public void clearAllCacheLinesByDummyPK(final Collection<? extends GenericEntity> dummyPKs) {
        checkIfLocked();
        if (dummyPKs == null) {
            return;
        }
        for (final GenericEntity dummyPK : dummyPKs) {
            clearCacheLineFlexible(dummyPK);
        }
    }

    public void clearAllCacheLinesByValue(final Collection<? extends GenericValue> values) {
        checkIfLocked();
        if (values == null) return;

        for (final GenericValue value : values) {
            clearCacheLine(value);
        }
    }

    public GenericValue getFromPrimaryKeyCache(final GenericPK primaryKey) {
        checkIfLocked();
        if (primaryKey == null) {
            return null;
        }
        return primaryKeyCache.get(primaryKey);
    }

    public List<GenericValue> getFromAllCache(final String entityName) {
        checkIfLocked();
        if (entityName == null) {
            return null;
        }
        return allCache.get(entityName);
    }

    public List<GenericValue> getFromAndCache(final String entityName, final Map<String, ?> fields) {
        checkIfLocked();
        if (entityName == null || fields == null) {
            return null;
        }
        ModelEntity entity = getModelEntity(entityName);

        return getFromAndCache(entity, fields);
    }

    public List<GenericValue> getFromAndCache(final ModelEntity entity, final Map<String, ?> fields) {
        checkIfLocked();
        if (entity == null || fields == null) {
            return null;
        }
        final GenericPK tempPK = new GenericPK(entity, fields);
        return andCache.get(tempPK);
    }

    public void putInPrimaryKeyCache(final GenericPK primaryKey, final GenericValue value) {
        checkIfLocked();
        if (primaryKey == null || value == null) {
            return;
        }

        if (value.getModelEntity().getNeverCache()) {
            Debug.logWarning("Tried to put a value of the " + value.getEntityName() +
                    " entity in the BY PRIMARY KEY cache but this entity has never-cache set to true, not caching.");
            return;
        }

        primaryKeyCache.put(primaryKey, value);
    }

    public void putAllInPrimaryKeyCache(final List<? extends GenericValue> values) {
        checkIfLocked();
        if (values == null) {
            return;
        }
        for (final GenericValue value : values) {
            putInPrimaryKeyCache(value.getPrimaryKey(), value);
        }
    }

    public void putInAllCache(final String entityName, final List<? extends GenericValue> values) {
        checkIfLocked();
        if (entityName == null || values == null) {
            return;
        }
        final ModelEntity entity = getModelEntity(entityName);
        putInAllCache(entity, values);
    }

    public void putInAllCache(final ModelEntity entity, final List<? extends GenericValue> values) {
        checkIfLocked();
        if (entity == null || values == null) {
            return;
        }

        if (entity.getNeverCache()) {
            Debug.logWarning("Tried to put values of the " + entity.getEntityName() +
                    " entity in the ALL cache but this entity has never-cache set to true, not caching.");
            return;
        }

        /*
            Make the values immutable so that the list can be returned directly
            from the cache without copying and still be safe. NOTE that this
            makes the list immutable, but not the elements in it, which will
            still be mutable GenericValue objects.
         */
        allCache.put(entity.getEntityName(), Collections.unmodifiableList(values));
    }

    public void putInAndCache(
            final String entityName, final Map<String, ?> fields, final List<? extends GenericValue> values) {
        checkIfLocked();
        if (entityName == null || fields == null || values == null) {
            return;
        }
        final ModelEntity entity = getModelEntity(entityName);
        putInAndCache(entity, fields, values);
    }

    public void putInAndCache(
            final ModelEntity entity, final Map<String, ?> fields, final List<? extends GenericValue> values) {
        checkIfLocked();
        if (entity == null || fields == null || values == null) {
            return;
        }

        if (entity.getNeverCache()) {
            Debug.logWarning("Tried to put values of the " + entity.getEntityName() +
                    " entity in the BY AND cache but this entity has never-cache set to true, not caching.");
            return;
        }

        final GenericPK tempPK = new GenericPK(entity, fields);

        /*
            Make the values immutable so that the list can be returned directly
            from the cache without copying and still be safe. NOTE that this
            makes the list immutable, but not the elements in it, which will
            still be mutable GenericValue objects.
         */
        andCache.put(tempPK, Collections.unmodifiableList(values));

        // now make sure the fieldName set used for this entry is in the
        // andCacheFieldSets Map which contains a Set of Sets of fieldNames for each entityName
        Set<Set<String>> fieldNameSets = andCacheFieldSets.get(entity.getEntityName());

        if (fieldNameSets == null) {
            synchronized (this) {
                // using a HashSet for both the individual fieldNameSets and
                // the set of fieldNameSets; this appears to be necessary
                // because TreeSet has bugs, or does not support, the compare
                // operation which is necessary when inserted a TreeSet
                // into a TreeSet.
                fieldNameSets = andCacheFieldSets.computeIfAbsent(entity.getEntityName(), k -> new HashSet<>());
            }
        }
        fieldNameSets.add(new HashSet<>(fields.keySet()));
    }

    /**
     * Parses the given XML file for entities. Does not insert them into the database.
     *
     * @param url the URL of the XML file (can be null)
     * @return null if a null URL was given, otherwise the parsed entities
     */
    @SuppressWarnings("unused")
    public List<GenericValue> readXmlDocument(final URL url)
            throws SAXException, ParserConfigurationException, IOException {
        if (url == null) {
            return null;
        }
        return makeValues(UtilXml.readXmlDocument(url, false));
    }

    /**
     * Parses the given XML document for entities. Does not insert them into the database.
     *
     * @param document the document to parse (can be null)
     * @return null if a null document was given, otherwise the parsed entities
     */
    public List<GenericValue> makeValues(final Document document) {
        checkIfLocked();
        if (document == null) {
            return null;
        }
        final List<GenericValue> values = new LinkedList<GenericValue>();
        final Element docElement = document.getDocumentElement();
        if (docElement == null) {
            return null;
        }
        if (!"entity-engine-xml".equals(docElement.getTagName())) {
            Debug.logError("[GenericDelegator.makeValues] Root node was not <entity-engine-xml>", module);
            throw new IllegalArgumentException("Root node was not <entity-engine-xml>");
        }
        docElement.normalize();
        Node curChild = docElement.getFirstChild();

        if (curChild != null) {
            do {
                if (curChild.getNodeType() == Node.ELEMENT_NODE) {
                    final Element element = (Element) curChild;
                    final GenericValue value = makeValue(element);
                    if (value != null) {
                        values.add(value);
                    }
                }
            } while ((curChild = curChild.getNextSibling()) != null);
        } else {
            Debug.logWarning("[GenericDelegator.makeValues] No child nodes found in document.", module);
        }

        return values;
    }

    @SuppressWarnings("unused")
    public GenericPK makePK(final Element element) {
        checkIfLocked();
        GenericValue value = makeValue(element);
        return value.getPrimaryKey();
    }

    public GenericValue makeValue(final Element element) {
        checkIfLocked();
        if (element == null) {
            return null;
        }
        final String entityName = getEntityName(element);
        final GenericValue value = makeValue(entityName, null);
        final ModelEntity modelEntity = value.getModelEntity();
        final Iterator<ModelField> modelFields = modelEntity.getFieldsIterator();

        while (modelFields.hasNext()) {
            final ModelField modelField = modelFields.next();
            final String name = modelField.getName();
            final String attr = element.getAttribute(name);

            if (attr != null && attr.length() > 0) {
                value.setString(name, attr);
            } else {
                // if no attribute try a subelement
                final Element subElement = UtilXml.firstChildElement(element, name);

                if (subElement != null) {
                    value.setString(name, UtilXml.elementValue(subElement));
                }
            }
        }

        return value;
    }

    private String getEntityName(final Element element) {
        final String tagName = element.getTagName();
        // if a dash or colon is in the tag name, grab what is after it
        if (tagName.indexOf('-') > 0) {
            return tagName.substring(tagName.indexOf('-') + 1);
        }
        if (tagName.indexOf(':') > 0) {
            return tagName.substring(tagName.indexOf(':') + 1);
        }
        return tagName;
    }

    @Override
    public Long getNextSeqId(String seqName, boolean clusterMode) {
        checkIfLocked();
        if (sequencer == null) {
            synchronized (this) {
                if (sequencer == null) {
                    String helperName = getEntityHelperName("SequenceValueItem");
                    ModelEntity seqEntity = getModelEntity("SequenceValueItem");

                    sequencer = new SequenceUtil(
                            helperName,
                            seqEntity,
                            "seqName",
                            "seqId",
                            clusterMode || ofNullable(getDelegatorInfo()).map(info -> info.useDistributedCacheClear).orElse(false)
                    );
                }
            }
        }
        return sequencer.getNextSeqId(seqName);
    }

    /**
     * Allows you to pass a SequenceUtil class (possibly one that overrides the getNextSeqId method);
     * if null is passed will effectively refresh the sequencer.
     *
     * @param sequencer the sequencer to set
     */
    public void setSequencer(final SequenceUtil sequencer) {
        checkIfLocked();
        this.sequencer = sequencer;
    }

    /**
     * Refreshes the ID sequencer clearing all cached bank values.
     */
    public void refreshSequencer() {
        checkIfLocked();
        this.sequencer = null;
    }

    private void absorbList(final List<GenericValue> lst) {
        if (lst == null) {
            return;
        }
        for (GenericValue aLst : lst) {
            aLst.setDelegator(this);
        }
    }

    public UtilCache<GenericEntity, GenericValue> getPrimaryKeyCache() {
        checkIfLocked();
        return primaryKeyCache;
    }

    public UtilCache<GenericPK, List<GenericValue>> getAndCache() {
        checkIfLocked();
        return andCache;
    }

    public UtilCache<String, List<GenericValue>> getAllCache() {
        checkIfLocked();
        return allCache;
    }

    @Override
    public List<GenericValue> transform(final String entityName, final EntityCondition entityCondition,
                                        final List<String> orderBy, final String lockField, final Transformation transformation)
            throws GenericEntityException {
        checkIfLocked();
        final ModelEntity modelEntity = getModelReader().getModelEntity(entityName);
        final GenericHelper entityHelper = getEntityHelper(entityName);
        final List<GenericValue> transformedEntities =
                entityHelper.transform(modelEntity, entityCondition, orderBy, lockField, transformation);
        for (final GenericValue genericValue : transformedEntities) {
            genericValue.setDelegator(this);
        }
        return transformedEntities;
    }

    private static void checkIfLocked() {
        if (isLocked()) {
            throw new UnsupportedOperationException(MESSAGE);
        }
    }
}
