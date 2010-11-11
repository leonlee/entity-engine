/*
 * $Id: MemoryHelper.java,v 1.7 2006/05/17 06:39:23 cmountford Exp $
 *
 *  Copyright (c) 2001 The Open For Business Project - www.ofbiz.org
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

package org.ofbiz.core.entity;

import org.apache.commons.collections.comparators.ComparatorChain;
import org.apache.commons.collections.comparators.ReverseComparator;
import org.ofbiz.core.entity.comparator.OFBizFieldComparator;
import org.ofbiz.core.entity.jdbc.ReadOnlySQLProcessor;
import org.ofbiz.core.entity.jdbc.SqlJdbcUtil;
import org.ofbiz.core.entity.model.ModelEntity;
import org.ofbiz.core.entity.model.ModelField;
import org.ofbiz.core.entity.model.ModelFieldTypeReader;
import org.ofbiz.core.entity.model.ModelRelation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

/**
 * Partial GenericHelper implementation that is entirely memory-based,
 * to be used for simple unit testing (can't do anything beyond searches
 * for primary keys, findByOr and findByAnd).
 *
 * @author <a href="mailto:plightbo@.com">Pat Lightbody</a>
 */
public class MemoryHelper implements GenericHelper {
    private static Map cache = getNewCache();

    public static void clearCache() {
        cache = getNewCache();
    }

    private static final Map getNewCache() {
        return Collections.synchronizedMap(new HashMap());
    }

    private String helperName;

    private boolean addToCache(GenericValue value) {
        if (value == null) {
            return false;
        }

        if (!veryifyValue(value)) {
            return false;
        }

        value = (GenericValue) value.clone();

        // we need to be sure that no-one accesses the underlying cache
        // between calling 'get' and 'put'
        synchronized (cache)
        {
            Map entityCache = (Map) cache.get(value.getEntityName());
            if (entityCache == null)
            {
                entityCache = getNewCache();
                cache.put(value.getEntityName(), entityCache);
            }
            entityCache.put(value.getPrimaryKey(), value);
        }

        return true;
    }

    private GenericValue findFromCache(GenericPK pk) {
        if (pk == null) {
            return null;
        }

        Map entityCache = (Map) cache.get(pk.getEntityName());
        if (entityCache == null) {
            return null;
        }

        GenericValue value = (GenericValue) entityCache.get(pk);
        if (value == null) {
            return null;
        } else {
            return (GenericValue) value.clone();
        }
    }

    private int removeFromCache(GenericPK pk) {
        if (pk == null) {
            return 0;
        }

        Map entityCache = (Map) cache.get(pk.getEntityName());
        if (entityCache == null) {
            return 0;
        }

        Object o = entityCache.remove(pk);
        if (o == null) {
            return 0;
        } else {
            return 1;
        }
    }

    private boolean isAndMatch(Map values, Map fields) {
        for (Iterator iterator = fields.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry mapEntry = (Map.Entry) iterator.next();
            if (mapEntry.getValue() == null) {
                if (values.get(mapEntry.getKey()) != null) {
                    return false;
                }
            } else {
                try {
                    if (!mapEntry.getValue().equals(values.get(mapEntry.getKey()))) {
                        return false;
                    }
                } catch (Exception e) {
                    return false;
                }
            }
        }

        return true;
    }

    private boolean isOrMatch(Map values, Map fields) {
        for (Iterator iterator = fields.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry mapEntry = (Map.Entry) iterator.next();
            if (mapEntry.getValue() == null) {
                if (values.get(mapEntry.getKey()) == null) {
                    return true;
                }
            } else {
                try {
                    if (mapEntry.getValue().equals(values.get(mapEntry.getKey()))) {
                        return true;
                    }
                } catch (Exception e) {
                }
            }
        }

        return false;
    }

    private boolean veryifyValue(GenericValue value) {
        ModelEntity me = value.getModelEntity();

        // make sure the PKs exist
        for (Iterator iterator = me.getPksIterator(); iterator.hasNext();) {
            ModelField field = (ModelField) iterator.next();
            if (!value.fields.containsKey(field.getName())) {
                return false;
            }
        }

        // make sure the value doesn't have any extra (unknown) fields
        for (Iterator iterator = value.fields.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry entry = (Map.Entry) iterator.next();
            if (me.getField((String) entry.getKey()) == null) {
                return false;
            }
        }

        // make sure all fields that are in the value are of the right type
        for (Iterator iterator = me.getFieldsIterator(); iterator.hasNext();) {
            ModelField field = (ModelField) iterator.next();
            Object o = value.get(field.getName());
            int typeValue = 0;
            try {
                typeValue = SqlJdbcUtil.getType(modelFieldTypeReader.getModelFieldType(field.getType()).getJavaType());
            } catch (GenericNotImplementedException e) {
                return false;
            }

            if (o != null) {
                switch (typeValue) {
                    case 1:
                        if (!(o instanceof String)) {
                            return false;
                        }
                        break;
                    case 2:
                        if (!(o instanceof java.sql.Timestamp)) {
                            return false;
                        }
                        break;

                    case 3:
                        if (!(o instanceof java.sql.Time)) {
                            return false;
                        }
                        break;

                    case 4:
                        if (!(o instanceof java.sql.Date)) {
                            return false;
                        }
                        break;

                    case 5:
                        if (!(o instanceof Integer)) {
                            return false;
                        }
                        break;

                    case 6:
                        if (!(o instanceof Long)) {
                            return false;
                        }
                        break;

                    case 7:
                        if (!(o instanceof Float)) {
                            return false;
                        }
                        break;

                    case 8:
                        if (!(o instanceof Double)) {
                            return false;
                        }
                        break;

                    case 9:
                        if (!(o instanceof Boolean)) {
                            return false;
                        }
                        break;
                }
            }
        }

        return true;
    }

    private ModelFieldTypeReader modelFieldTypeReader;

    public MemoryHelper(String helperName) {
        this.helperName = helperName;
        modelFieldTypeReader = ModelFieldTypeReader.getModelFieldTypeReader(helperName);
    }

    public String getHelperName() {
        return helperName;
    }

    public GenericValue create(GenericValue value) throws GenericEntityException {
        if (addToCache(value)) {
            return value;
        } else {
            return null;
        }
    }

    public GenericValue create(GenericPK primaryKey) throws GenericEntityException {
        return create(new GenericValue(primaryKey));
    }

    public GenericValue findByPrimaryKey(GenericPK primaryKey) throws GenericEntityException {
        return findFromCache(primaryKey);
    }

    public GenericValue findByPrimaryKeyPartial(GenericPK primaryKey, Set keys) throws GenericEntityException {
        GenericValue value = findFromCache(primaryKey);
        value.setFields(value.getFields(keys));
        return value;
    }

    public List findAllByPrimaryKeys(List primaryKeys) throws GenericEntityException {
        ArrayList result = new ArrayList(primaryKeys.size());
        for (Iterator iterator = primaryKeys.iterator(); iterator.hasNext();) {
            GenericPK pk = (GenericPK) iterator.next();
            result.add(this.findByPrimaryKey(pk));
        }

        return result;
    }

    public int removeByPrimaryKey(GenericPK primaryKey) throws GenericEntityException {
        return removeFromCache(primaryKey);
    }

    public List findByAnd(ModelEntity modelEntity, Map fields, List orderBy) throws GenericEntityException {
        Map entityCache = (Map) cache.get(modelEntity.getEntityName());
        if (entityCache == null) {
            return Collections.EMPTY_LIST;
        }

        ArrayList result = new ArrayList();
        // according to the javadocs for Collections.synchronizedMap() we need to
        // synchronize when iterating over the elements of the collection
        synchronized(entityCache) {
            for (Iterator iterator = entityCache.entrySet().iterator(); iterator.hasNext();) {
                Map.Entry mapEntry = (Map.Entry) iterator.next();
                GenericValue value = (GenericValue) mapEntry.getValue();

                if (isAndMatch(value.fields, fields)) {
                    result.add(value);
                }
            }
        }
        ComparatorChain comp = new ComparatorChain();
        if (orderBy != null) {
            for (int i = 0; i < orderBy.size(); i++) {
                String fieldAndOrder = (String) orderBy.get(i);
                StringTokenizer stringTokenizer = new StringTokenizer(fieldAndOrder);
                String field = null;
                String order = null;
                if (stringTokenizer.hasMoreElements())
                    field = stringTokenizer.nextToken();
                if (stringTokenizer.hasMoreElements())
                    order = stringTokenizer.nextToken();
                if (field != null) {
                    if (order == null || "ASC".equalsIgnoreCase(order))
                        comp.addComparator(new OFBizFieldComparator(field));
                    else
                        comp.addComparator(new ReverseComparator(new OFBizFieldComparator(field)));
                }
            }
            Collections.sort(result, comp);
        }
        return result;
    }

    public List findByAnd(ModelEntity modelEntity, List expressions, List orderBy) throws GenericEntityException {
        return null;
    }

    public List findByLike(ModelEntity modelEntity, Map fields, List orderBy) throws GenericEntityException {
        return null;
    }

    /* tentatively removing by clause methods, unless there are really big complaints... because it is a kludge
    public List findByClause(ModelEntity modelEntity, List entityClauses, Map fields, List orderBy) throws GenericEntityException {
        return null;
    }
    */

    public List findByOr(ModelEntity modelEntity, Map fields, List orderBy) throws GenericEntityException {
        Map entityCache = (Map) cache.get(modelEntity.getEntityName());
        if (entityCache == null) {
            return Collections.EMPTY_LIST;
        }

        ArrayList result = new ArrayList();
        // according to the javadocs for Collections.synchronizedMap() we need to
        // synchronize when iterating over the elements of the collection
        synchronized(entityCache) {
            for (Iterator iterator = entityCache.entrySet().iterator(); iterator.hasNext();) {
                Map.Entry mapEntry = (Map.Entry) iterator.next();
                GenericValue value = (GenericValue) mapEntry.getValue();

                if (isOrMatch(value.fields, fields)) {
                    result.add(value);
                }
            }
        }

        return result;

    }

    public List findByOr(ModelEntity modelEntity, List expressions, List orderBy) throws GenericEntityException {
        return null;
    }

    public List findByCondition(ModelEntity modelEntity, EntityCondition entityCondition, Collection fieldsToSelect, List orderBy) throws GenericEntityException {
        Map entityCache = (Map) cache.get(modelEntity.getEntityName());
        if (entityCache == null) {
            return Collections.EMPTY_LIST;
        }

        List result = findByConditionWorker(entityCache, modelEntity, entityCondition, fieldsToSelect, orderBy);

        return result;

    }

    private List findByConditionWorker(Map entityCache, ModelEntity modelEntity, EntityCondition entityCondition, Collection fieldsToSelect, List orderBy) throws GenericEntityException {
        if (entityCondition instanceof EntityExpr) {
            List result = new ArrayList();
            EntityExpr entityExpr = (EntityExpr) entityCondition;

            // according to the javadocs for Collections.synchronizedMap() we need to
            // synchronize when iterating over the elements of the collection
            synchronized(entityCache) {
                for (Iterator iterator = entityCache.entrySet().iterator(); iterator.hasNext();) {
                    Map.Entry mapEntry = (Map.Entry) iterator.next();
                    GenericValue value = (GenericValue) mapEntry.getValue();
                    if (checkEntityExpr(value, entityExpr))
                        result.add(value);
                }
            }

            return result;
        } else if (entityCondition instanceof EntityConditionList) {
            EntityConditionList entityConditionList = (EntityConditionList) entityCondition;
            Map tempEntityCache = entityCache;
            List tempResult = new ArrayList();

            for (int i = 0; i < entityConditionList.getConditionListSize(); i++) {
                EntityCondition subEntityCondition = entityConditionList.getCondition(i);
                if (EntityOperator.AND.equals(entityConditionList.getOperator())) {
                    tempResult = findByConditionWorker(tempEntityCache, modelEntity, subEntityCondition, fieldsToSelect, orderBy);

                    //result are now a subset and only these should have the next condition run on them so put them in the
                    //tempEntityCache
                    tempEntityCache = new HashMap();
                    for (int j = 0; j < tempResult.size(); j++) {
                        GenericValue genericValue = (GenericValue) tempResult.get(j);
                        tempEntityCache.put(genericValue, genericValue);
                    }
                } else if (EntityOperator.OR.equals(entityConditionList.getOperator())) {
                    // Ensure we do not add the same GenericEntity to the list more than once
                    List list = findByConditionWorker(tempEntityCache, modelEntity, subEntityCondition, fieldsToSelect, orderBy);
                    for (Iterator iterator = list.iterator(); iterator.hasNext();)
                    {
                        Object o =  iterator.next();
                        if (!tempResult.contains(o))
                            tempResult.add(o);
                    }
                }
            }
            return tempResult;
        } else if (entityCondition instanceof EntityFieldMap) {
            EntityFieldMap entityCond = (EntityFieldMap) entityCondition;

            if (entityCond.getOperator().equals(EntityOperator.AND))
            {
                return findByAnd(modelEntity, entityCond.fieldMap, orderBy);
            } else
            {
                return findByOr(modelEntity, entityCond.fieldMap, orderBy);
            }

        } else if (entityCondition == null)
        {
            return new ArrayList(entityCache.values());
        } else {
            throw new UnsupportedOperationException("findByCondidition not implemented for expression:" + entityCondition.getClass().getName());
        }
    }

    private boolean checkEntityExpr(GenericValue value, EntityExpr entityExpr) {
        EntityOperator operator = entityExpr.getOperator();
        return operator.compare(value.get(entityExpr.getLhs()), entityExpr.getRhs());
    }

    public List findByMultiRelation(GenericValue value, ModelRelation modelRelationOne, ModelEntity modelEntityOne,
                                    ModelRelation modelRelationTwo, ModelEntity modelEntityTwo, List orderBy) throws GenericEntityException {
        return null;
    }

    /**
     * The memory implementation does the *minimum* that it can to allow tests to work.  In particular it will
     * return *all* values for a particular entity from this method.
     */
    public EntityListIterator findListIteratorByCondition(ModelEntity modelEntity, EntityCondition whereEntityCondition,
                                                          EntityCondition havingEntityCondition, Collection fieldsToSelect, List orderBy, EntityFindOptions findOptions)
            throws GenericEntityException {
        final Iterator entities = (new ArrayList(findByCondition(modelEntity, whereEntityCondition, fieldsToSelect, orderBy))).iterator();

        // hack in the minimum that we can for this.
        EntityListIterator entityListIterator = new EntityListIterator(new ReadOnlySQLProcessor(null), modelEntity, null, modelFieldTypeReader)
        {
            public Object next()
            {
                if (entities.hasNext())

                    return entities.next();
                else
                    return null;
            }

            public void close()
            {
                //do nothing
            }
        };
        return entityListIterator;
    }

    public int removeByAnd(ModelEntity modelEntity, Map fields) throws GenericEntityException {
        Map entityCache = (Map) cache.get(modelEntity.getEntityName());
        if (entityCache == null) {
            return 0;
        }

        ArrayList removeList = new ArrayList();
        for (Iterator iterator = entityCache.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry mapEntry = (Map.Entry) iterator.next();
            GenericValue value = (GenericValue) mapEntry.getValue();
            if (isAndMatch(value.fields, fields)) {
                removeList.add(mapEntry.getKey());
            }
        }

        return removeAll(removeList);
    }

    public int store(GenericValue value) throws GenericEntityException {
        if (addToCache(value)) {
            return 1;
        } else {
            return 0;
        }
    }

    public int storeAll(List values) throws GenericEntityException {
        int count = 0;
        for (Iterator iterator = values.iterator(); iterator.hasNext();) {
            GenericValue gv = (GenericValue) iterator.next();
            if (addToCache(gv)) {
                count++;
            }
        }

        return count;
    }

    public int removeAll(List dummyPKs) throws GenericEntityException {
        int count = 0;
        for (Iterator iterator = dummyPKs.iterator(); iterator.hasNext();) {
            GenericEntity ge = (GenericEntity) iterator.next();
            if (ge instanceof GenericPK) {
                count = count + removeFromCache((GenericPK) ge);
            } else {
                Map pkFields = new HashMap();
                List pkFieldNames = ge.getModelEntity().getPkFieldNames();
                for (int i = 0; i < pkFieldNames.size(); i++) {
                    String fieldName = (String) pkFieldNames.get(i);
                    pkFields.put(fieldName, ge.get(fieldName));
                }
                GenericPK pk = new GenericPK(ge.getModelEntity(), pkFields);
                count = count + removeFromCache(pk);
            }
        }
        return count;
    }

    public void checkDataSource(Map modelEntities, Collection messages, boolean addMissing) throws GenericEntityException {
    }
}
