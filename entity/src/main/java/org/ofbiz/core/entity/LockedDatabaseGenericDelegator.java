package org.ofbiz.core.entity;

import org.apache.log4j.Logger;
import org.ofbiz.core.entity.config.EntityConfigUtil;
import org.ofbiz.core.entity.model.ModelEntity;
import org.ofbiz.core.entity.model.ModelFieldType;
import org.ofbiz.core.entity.model.ModelGroupReader;
import org.ofbiz.core.entity.model.ModelReader;
import org.ofbiz.core.util.UtilCache;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Copyright All Rights Reserved.
 * Created: christo 15/09/2006 12:16:27
 */
public class LockedDatabaseGenericDelegator extends GenericDelegator {
    private static final Logger log = Logger.getLogger(LockedDatabaseGenericDelegator.class);
    private static final String MESSAGE = "Database is locked";

    public LockedDatabaseGenericDelegator() {
        log.info("Constructor: must be trouble in the database...");
    }

    protected void absorbList(List<GenericValue> lst) {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public void clearAllCacheLinesByDummyPK(Collection<? extends GenericEntity> dummyPKs) {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public void clearAllCacheLinesByValue(Collection<? extends GenericValue> values) {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public void clearAllCaches() {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public void clearAllCaches(boolean distribute) {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public void clearCacheLine(String entityName, Map<String, ?> fields) {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public void clearCacheLine(GenericPK primaryKey) {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public void clearCacheLine(GenericPK primaryKey, boolean distribute) {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public void clearCacheLine(GenericValue value) {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public void clearCacheLine(GenericValue value, boolean distribute) {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public void clearCacheLineFlexible(GenericEntity dummyPK) {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public void clearCacheLineFlexible(GenericEntity dummyPK, boolean distribute) {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public GenericValue create(String entityName, Map<String, ?> fields) throws GenericEntityException {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public GenericValue create(GenericPK primaryKey) throws GenericEntityException {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public GenericValue create(GenericPK primaryKey, boolean doCacheClear) throws GenericEntityException {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public GenericValue create(GenericValue value) throws GenericEntityException {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public GenericValue create(GenericValue value, boolean doCacheClear) throws GenericEntityException {
        throw new UnsupportedOperationException(MESSAGE);
    }

    protected void evalEcaRules(String event, String currentOperation, GenericEntity value, Map<?, ?> eventMap, boolean noEventMapFound, boolean isError) throws GenericEntityException {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public List<GenericValue> findAll(String entityName) throws GenericEntityException {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public List<GenericValue> findAll(String entityName, List<String> orderBy) throws GenericEntityException {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public List<GenericValue> findAllByPrimaryKeys(Collection<? extends GenericPK> primaryKeys) throws GenericEntityException {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public List<GenericValue> findAllByPrimaryKeysCache(Collection<? extends GenericPK> primaryKeys) throws GenericEntityException {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public List<GenericValue> findAllCache(String entityName) throws GenericEntityException {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public List<GenericValue> findAllCache(String entityName, List<String> orderBy) throws GenericEntityException {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public List<GenericValue> findByAnd(String entityName, List<? extends EntityCondition> expressions) throws GenericEntityException {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public List<GenericValue> findByAnd(String entityName, List<? extends EntityCondition> expressions, List<String> orderBy) throws GenericEntityException {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public List<GenericValue> findByAnd(String entityName, Map<String, ?> fields) throws GenericEntityException {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public List<GenericValue> findByAnd(String entityName, Map<String, ?> fields, List<String> orderBy) throws GenericEntityException {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public List<GenericValue> findByAnd(ModelEntity modelEntity, Map<String, ?> fields, List<String> orderBy) throws GenericEntityException {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public List<GenericValue> findByAndCache(String entityName, Map<String, ?> fields) throws GenericEntityException {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public List<GenericValue> findByAndCache(String entityName, Map<String, ?> fields, List<String> orderBy) throws GenericEntityException {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public List<GenericValue> findByCondition(String entityName, EntityCondition entityCondition, Collection<String> fieldsToSelect, List<String> orderBy) throws GenericEntityException {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public List<GenericValue> findByLike(String entityName, Map<String, ?> fields) throws GenericEntityException {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public List<GenericValue> findByLike(String entityName, Map<String, ?> fields, List<String> orderBy) throws GenericEntityException {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public List<GenericValue> findByOr(String entityName, List<? extends EntityCondition> expressions) throws GenericEntityException {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public List<GenericValue> findByOr(String entityName, List<? extends EntityCondition> expressions, List<String> orderBy) throws GenericEntityException {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public List<GenericValue> findByOr(String entityName, Map<String, ?> fields) throws GenericEntityException {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public List<GenericValue> findByOr(String entityName, Map<String, ?> fields, List<String> orderBy) throws GenericEntityException {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public GenericValue findByPrimaryKey(String entityName, Map<String, ?> fields) throws GenericEntityException {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public GenericValue findByPrimaryKey(GenericPK primaryKey) throws GenericEntityException {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public GenericValue findByPrimaryKeyCache(String entityName, Map<String, ?> fields) throws GenericEntityException {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public GenericValue findByPrimaryKeyCache(GenericPK primaryKey) throws GenericEntityException {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public GenericValue findByPrimaryKeyPartial(GenericPK primaryKey, Set<String> keys) throws GenericEntityException {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public EntityListIterator findListIteratorByCondition(String entityName, EntityCondition entityCondition, Collection<String> fieldsToSelect, List<String> orderBy) throws GenericEntityException {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public EntityListIterator findListIteratorByCondition(String entityName, EntityCondition whereEntityCondition, EntityCondition havingEntityCondition, Collection<String> fieldsToSelect, List<String> orderBy, EntityFindOptions findOptions) throws GenericEntityException {
        throw new UnsupportedOperationException(MESSAGE);
    }

    protected LockedDatabaseGenericDelegator(String delegatorName) throws GenericEntityException {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public UtilCache<String, List<GenericValue>> getAllCache() {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public UtilCache<GenericPK, List<GenericValue>> getAndCache() {
        throw new UnsupportedOperationException(MESSAGE);
    }

    protected EntityConfigUtil.DelegatorInfo getDelegatorInfo() {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public String getDelegatorName() {
        throw new UnsupportedOperationException(MESSAGE);
    }

    protected Map<?, ?> getEcaEntityEventMap(String entityName) {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public ModelFieldType getEntityFieldType(ModelEntity entity, String type) throws GenericEntityException {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public Collection<String> getEntityFieldTypeNames(ModelEntity entity) throws GenericEntityException {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public String getEntityGroupName(String entityName) {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public GenericHelper getEntityHelper(ModelEntity entity) throws GenericEntityException {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public GenericHelper getEntityHelper(String entityName) throws GenericEntityException {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public String getEntityHelperName(ModelEntity entity) {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public String getEntityHelperName(String entityName) {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public Set<Set<String>> getFieldNameSetsCopy(String entityName) {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public List<GenericValue> getFromAllCache(String entityName) {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public List<GenericValue> getFromAndCache(ModelEntity entity, Map<String, ?> fields) {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public List<GenericValue> getFromAndCache(String entityName, Map<String, ?> fields) {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public GenericValue getFromPrimaryKeyCache(GenericPK primaryKey) {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public String getGroupHelperName(String groupName) {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public List<ModelEntity> getModelEntitiesByGroup(String groupName) {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public ModelEntity getModelEntity(String entityName) {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public Map<String, ModelEntity> getModelEntityMapByGroup(String groupName) {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public ModelGroupReader getModelGroupReader() {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public ModelReader getModelReader() {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public List<GenericValue> getMultiRelation(GenericValue value, String relationNameOne, String relationNameTwo) throws GenericEntityException {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public List<GenericValue> getMultiRelation(GenericValue value, String relationNameOne, String relationNameTwo, List<String> orderBy) throws GenericEntityException {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public Long getNextSeqId(String seqName) {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public UtilCache<GenericEntity, GenericValue> getPrimaryKeyCache() {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public List<GenericValue> getRelated(String relationName, Map<String, ?> byAndFields, List<String> orderBy, GenericValue value) throws GenericEntityException {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public List<GenericValue> getRelated(String relationName, GenericValue value) throws GenericEntityException {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public List<GenericValue> getRelatedByAnd(String relationName, Map<String, ?> byAndFields, GenericValue value) throws GenericEntityException {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public List<GenericValue> getRelatedCache(String relationName, GenericValue value) throws GenericEntityException {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public GenericPK getRelatedDummyPK(String relationName, Map<String, ?> byAndFields, GenericValue value) throws GenericEntityException {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public GenericValue getRelatedOne(String relationName, GenericValue value) throws GenericEntityException {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public GenericValue getRelatedOneCache(String relationName, GenericValue value) throws GenericEntityException {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public List<GenericValue> getRelatedOrderBy(String relationName, List<String> orderBy, GenericValue value) throws GenericEntityException {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public GenericPK makePK(Element element) {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public GenericPK makePK(String entityName, Map<String, ?> fields) {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public GenericValue makeValue(Element element) {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public GenericValue makeValue(String entityName, Map<String, ?> fields) {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public List<GenericValue> makeValues(Document document) {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public void putAllInPrimaryKeyCache(List<? extends GenericValue> values) {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public void putInAllCache(ModelEntity entity, List<? extends GenericValue> values) {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public void putInAllCache(String entityName, List<? extends GenericValue> values) {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public void putInAndCache(ModelEntity entity, Map<String, ?> fields, List<? extends GenericValue> values) {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public void putInAndCache(String entityName, Map<String, ?> fields, List<? extends GenericValue> values) {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public void putInPrimaryKeyCache(GenericPK primaryKey, GenericValue value) {
        throw new UnsupportedOperationException(MESSAGE);
    }

    // ======= XML Related Methods ========
    public List<GenericValue> readXmlDocument(URL url) throws SAXException, ParserConfigurationException, IOException {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public void refresh(GenericValue value) throws GenericEntityException {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public void refresh(GenericValue value, boolean doCacheClear) throws GenericEntityException {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public void refreshSequencer() {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public int removeAll(List<? extends GenericEntity> dummyPKs) throws GenericEntityException {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public int removeAll(List<? extends GenericEntity> dummyPKs, boolean doCacheClear) throws GenericEntityException {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public int removeByAnd(String entityName, Map<String, ?> fields) throws GenericEntityException {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public int removeByAnd(String entityName, Map<String, ?> fields, boolean doCacheClear) throws GenericEntityException {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public int removeByPrimaryKey(GenericPK primaryKey) throws GenericEntityException {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public int removeByPrimaryKey(GenericPK primaryKey, boolean doCacheClear) throws GenericEntityException {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public int removeRelated(String relationName, GenericValue value) throws GenericEntityException {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public int removeRelated(String relationName, GenericValue value, boolean doCacheClear) throws GenericEntityException {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public int removeValue(GenericValue value) throws GenericEntityException {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public int removeValue(GenericValue value, boolean doCacheClear) throws GenericEntityException {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public void setSequencer(SequenceUtil sequencer) {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public int store(GenericValue value) throws GenericEntityException {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public int store(GenericValue value, boolean doCacheClear) throws GenericEntityException {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public int storeAll(List<? extends GenericValue> values) throws GenericEntityException {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public int storeAll(List<? extends GenericValue> values, boolean doCacheClear) throws GenericEntityException {
        throw new UnsupportedOperationException(MESSAGE);
    }

    @Override
    public List<GenericValue> transform(final String entityName, final EntityCondition entityCondition,
                                        final List<String> orderBy, final String lockField, final Transformation transformation) {
        throw new UnsupportedOperationException(MESSAGE);
    }
}

