package org.ofbiz.core.entity;

import org.ofbiz.core.entity.config.EntityConfigUtil;
import org.ofbiz.core.entity.model.ModelEntity;
import org.ofbiz.core.entity.model.ModelFieldType;
import org.ofbiz.core.entity.model.ModelGroupReader;
import org.ofbiz.core.entity.model.ModelReader;
import org.ofbiz.core.util.UtilCache;
import org.w3c.dom.Element;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import org.apache.log4j.Logger;

import javax.xml.parsers.ParserConfigurationException;
import java.util.List;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.net.URL;
import java.io.IOException;

/**
 * Copyright All Rights Reserved.
 * Created: christo 15/09/2006 12:16:27
 */
public class LockedDatabaseGenericDelegator extends GenericDelegator
{
    private static final Logger log = Logger.getLogger(LockedDatabaseGenericDelegator.class);
    private static final String MESSAGE = "Database is locked";

    public LockedDatabaseGenericDelegator()
    {
       log.info("Constructor: must be trouble in the database...");
    }

    protected void absorbList(List lst)
    {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public void clearAllCacheLinesByDummyPK(Collection dummyPKs)
    {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public void clearAllCacheLinesByValue(Collection values)
    {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public void clearAllCaches()
    {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public void clearAllCaches(boolean distribute)
    {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public void clearCacheLine(String entityName, Map fields)
    {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public void clearCacheLine(GenericPK primaryKey)
    {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public void clearCacheLine(GenericPK primaryKey, boolean distribute)
    {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public void clearCacheLine(GenericValue value)
    {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public void clearCacheLine(GenericValue value, boolean distribute)
    {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public void clearCacheLineFlexible(GenericEntity dummyPK)
    {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public void clearCacheLineFlexible(GenericEntity dummyPK, boolean distribute)
    {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public GenericValue create(String entityName, Map fields) throws GenericEntityException
    {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public GenericValue create(GenericPK primaryKey) throws GenericEntityException
    {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public GenericValue create(GenericPK primaryKey, boolean doCacheClear) throws GenericEntityException
    {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public GenericValue create(GenericValue value) throws GenericEntityException
    {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public GenericValue create(GenericValue value, boolean doCacheClear) throws GenericEntityException
    {
        throw new UnsupportedOperationException(MESSAGE);
    }

    protected void evalEcaRules(String event, String currentOperation, GenericEntity value, Map eventMap, boolean noEventMapFound, boolean isError) throws GenericEntityException
    {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public List findAll(String entityName) throws GenericEntityException
    {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public List findAll(String entityName, List orderBy) throws GenericEntityException
    {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public List findAllByPrimaryKeys(Collection primaryKeys) throws GenericEntityException
    {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public List findAllByPrimaryKeysCache(Collection primaryKeys) throws GenericEntityException
    {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public List findAllCache(String entityName) throws GenericEntityException
    {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public List findAllCache(String entityName, List orderBy) throws GenericEntityException
    {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public List findByAnd(String entityName, List expressions) throws GenericEntityException
    {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public List findByAnd(String entityName, List expressions, List orderBy) throws GenericEntityException
    {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public List findByAnd(String entityName, Map fields) throws GenericEntityException
    {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public List findByAnd(String entityName, Map fields, List orderBy) throws GenericEntityException
    {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public List findByAnd(ModelEntity modelEntity, Map fields, List orderBy) throws GenericEntityException
    {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public List findByAndCache(String entityName, Map fields) throws GenericEntityException
    {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public List findByAndCache(String entityName, Map fields, List orderBy) throws GenericEntityException
    {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public List findByCondition(String entityName, EntityCondition entityCondition, Collection fieldsToSelect, List orderBy) throws GenericEntityException
    {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public List findByLike(String entityName, Map fields) throws GenericEntityException
    {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public List findByLike(String entityName, Map fields, List orderBy) throws GenericEntityException
    {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public List findByOr(String entityName, List expressions) throws GenericEntityException
    {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public List findByOr(String entityName, List expressions, List orderBy) throws GenericEntityException
    {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public List findByOr(String entityName, Map fields) throws GenericEntityException
    {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public List findByOr(String entityName, Map fields, List orderBy) throws GenericEntityException
    {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public GenericValue findByPrimaryKey(String entityName, Map fields) throws GenericEntityException
    {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public GenericValue findByPrimaryKey(GenericPK primaryKey) throws GenericEntityException
    {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public GenericValue findByPrimaryKeyCache(String entityName, Map fields) throws GenericEntityException
    {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public GenericValue findByPrimaryKeyCache(GenericPK primaryKey) throws GenericEntityException
    {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public GenericValue findByPrimaryKeyPartial(GenericPK primaryKey, Set keys) throws GenericEntityException
    {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public EntityListIterator findListIteratorByCondition(String entityName, EntityCondition entityCondition, Collection fieldsToSelect, List orderBy) throws GenericEntityException
    {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public EntityListIterator findListIteratorByCondition(String entityName, EntityCondition whereEntityCondition, EntityCondition havingEntityCondition, Collection fieldsToSelect, List orderBy, EntityFindOptions findOptions) throws GenericEntityException
    {
        throw new UnsupportedOperationException(MESSAGE);
    }

    protected LockedDatabaseGenericDelegator(String delegatorName) throws GenericEntityException
    {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public UtilCache getAllCache()
    {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public UtilCache getAndCache()
    {
        throw new UnsupportedOperationException(MESSAGE);
    }

    protected EntityConfigUtil.DelegatorInfo getDelegatorInfo()
    {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public String getDelegatorName()
    {
        throw new UnsupportedOperationException(MESSAGE);
    }

    protected Map getEcaEntityEventMap(String entityName)
    {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public ModelFieldType getEntityFieldType(ModelEntity entity, String type) throws GenericEntityException
    {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public Collection getEntityFieldTypeNames(ModelEntity entity) throws GenericEntityException
    {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public String getEntityGroupName(String entityName)
    {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public GenericHelper getEntityHelper(ModelEntity entity) throws GenericEntityException
    {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public GenericHelper getEntityHelper(String entityName) throws GenericEntityException
    {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public String getEntityHelperName(ModelEntity entity)
    {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public String getEntityHelperName(String entityName)
    {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public Set getFieldNameSetsCopy(String entityName)
    {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public List getFromAllCache(String entityName)
    {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public List getFromAndCache(ModelEntity entity, Map fields)
    {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public List getFromAndCache(String entityName, Map fields)
    {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public GenericValue getFromPrimaryKeyCache(GenericPK primaryKey)
    {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public String getGroupHelperName(String groupName)
    {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public List getModelEntitiesByGroup(String groupName)
    {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public ModelEntity getModelEntity(String entityName)
    {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public Map getModelEntityMapByGroup(String groupName)
    {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public ModelGroupReader getModelGroupReader()
    {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public ModelReader getModelReader()
    {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public List getMultiRelation(GenericValue value, String relationNameOne, String relationNameTwo) throws GenericEntityException
    {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public List getMultiRelation(GenericValue value, String relationNameOne, String relationNameTwo, List orderBy) throws GenericEntityException
    {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public Long getNextSeqId(String seqName)
    {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public UtilCache getPrimaryKeyCache()
    {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public List getRelated(String relationName, Map byAndFields, List orderBy, GenericValue value) throws GenericEntityException
    {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public List getRelated(String relationName, GenericValue value) throws GenericEntityException
    {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public List getRelatedByAnd(String relationName, Map byAndFields, GenericValue value) throws GenericEntityException
    {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public List getRelatedCache(String relationName, GenericValue value) throws GenericEntityException
    {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public GenericPK getRelatedDummyPK(String relationName, Map byAndFields, GenericValue value) throws GenericEntityException
    {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public GenericValue getRelatedOne(String relationName, GenericValue value) throws GenericEntityException
    {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public GenericValue getRelatedOneCache(String relationName, GenericValue value) throws GenericEntityException
    {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public List getRelatedOrderBy(String relationName, List orderBy, GenericValue value) throws GenericEntityException
    {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public GenericPK makePK(Element element)
    {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public GenericPK makePK(String entityName, Map fields)
    {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public GenericValue makeValue(Element element)
    {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public GenericValue makeValue(String entityName, Map fields)
    {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public List makeValues(Document document)
    {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public void putAllInPrimaryKeyCache(List values)
    {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public void putInAllCache(ModelEntity entity, List values)
    {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public void putInAllCache(String entityName, List values)
    {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public void putInAndCache(ModelEntity entity, Map fields, List values)
    {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public void putInAndCache(String entityName, Map fields, List values)
    {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public void putInPrimaryKeyCache(GenericPK primaryKey, GenericValue value)
    {
        throw new UnsupportedOperationException(MESSAGE);
    }

    // ======= XML Related Methods ========
    public List readXmlDocument(URL url) throws SAXException, ParserConfigurationException, IOException
    {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public void refresh(GenericValue value) throws GenericEntityException
    {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public void refresh(GenericValue value, boolean doCacheClear) throws GenericEntityException
    {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public void refreshSequencer()
    {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public int removeAll(List dummyPKs) throws GenericEntityException
    {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public int removeAll(List dummyPKs, boolean doCacheClear) throws GenericEntityException
    {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public int removeByAnd(String entityName, Map fields) throws GenericEntityException
    {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public int removeByAnd(String entityName, Map fields, boolean doCacheClear) throws GenericEntityException
    {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public int removeByPrimaryKey(GenericPK primaryKey) throws GenericEntityException
    {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public int removeByPrimaryKey(GenericPK primaryKey, boolean doCacheClear) throws GenericEntityException
    {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public int removeRelated(String relationName, GenericValue value) throws GenericEntityException
    {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public int removeRelated(String relationName, GenericValue value, boolean doCacheClear) throws GenericEntityException
    {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public int removeValue(GenericValue value) throws GenericEntityException
    {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public int removeValue(GenericValue value, boolean doCacheClear) throws GenericEntityException
    {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public void setSequencer(SequenceUtil sequencer)
    {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public int store(GenericValue value) throws GenericEntityException
    {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public int store(GenericValue value, boolean doCacheClear) throws GenericEntityException
    {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public int storeAll(List values) throws GenericEntityException
    {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public int storeAll(List values, boolean doCacheClear) throws GenericEntityException
    {
        throw new UnsupportedOperationException(MESSAGE);
    }
}

