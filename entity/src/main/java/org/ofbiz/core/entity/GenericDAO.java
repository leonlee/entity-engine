/*
 * $Id: GenericDAO.java,v 1.1 2005/04/01 05:58:02 sfarquhar Exp $
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

import com.atlassian.util.concurrent.CopyOnWriteMap;
import org.ofbiz.core.entity.config.EntityConfigUtil;
import org.ofbiz.core.entity.jdbc.AutoCommitSQLProcessor;
import org.ofbiz.core.entity.jdbc.DatabaseUtil;
import org.ofbiz.core.entity.jdbc.ExplcitCommitSQLProcessor;
import org.ofbiz.core.entity.jdbc.PassThruSQLProcessor;
import org.ofbiz.core.entity.jdbc.ReadOnlySQLProcessor;
import org.ofbiz.core.entity.jdbc.SQLProcessor;
import org.ofbiz.core.entity.jdbc.SqlJdbcUtil;
import org.ofbiz.core.entity.model.ModelEntity;
import org.ofbiz.core.entity.model.ModelField;
import org.ofbiz.core.entity.model.ModelFieldTypeReader;
import org.ofbiz.core.entity.model.ModelKeyMap;
import org.ofbiz.core.entity.model.ModelRelation;
import org.ofbiz.core.entity.model.ModelViewEntity;
import org.ofbiz.core.util.Debug;
import org.ofbiz.core.util.UtilDateTime;
import org.ofbiz.core.util.UtilValidate;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;

/**
 * Generic Entity Data Access Object - Handles persisntence for any defined entity.
 *
 * @author     <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 * @author     <a href="mailto:jaz@ofbiz.org">Andy Zeneski</a>
 * @author     <a href="mailto:chris_maurer@altavista.com">Chris Maurer</a>
 * @author     <a href="mailto:jdonnerstag@eds.de">Juergen Donnerstag</a>
 * @author     <a href="mailto:gielen@aixcept.de">Rene Gielen</a>
 * @author     <a href="mailto:john_nutting@telluridetechnologies.com">John Nutting</a>
 * @version    $Revision: 1.1 $
 * @since      1.0
 */
public class GenericDAO {

    public static final String module = GenericDAO.class.getName();

    protected static Map<String, GenericDAO> genericDAOs = CopyOnWriteMap.newHashMap();
    protected String helperName;
    protected ModelFieldTypeReader modelFieldTypeReader = null;
    protected EntityConfigUtil.DatasourceInfo datasourceInfo;

    public static synchronized void removeGenericDAO(String helperName)
    {
        genericDAOs.remove(helperName);
    }

    public static GenericDAO getGenericDAO(String helperName) {
        GenericDAO newGenericDAO = genericDAOs.get(helperName);

        if (newGenericDAO == null)// don't want to block here
        {
            synchronized (GenericDAO.class) {
                newGenericDAO = genericDAOs.get(helperName);
                if (newGenericDAO == null) {
                    newGenericDAO = new GenericDAO(helperName);
                    genericDAOs.put(helperName, newGenericDAO);
                }
            }
        }
        return newGenericDAO;
    }

    public GenericDAO(String helperName) {
        this.helperName = helperName;
        this.modelFieldTypeReader = ModelFieldTypeReader.getModelFieldTypeReader(helperName);
        this.datasourceInfo = EntityConfigUtil.getInstance().getDatasourceInfo(helperName);
    }

    public int insert(GenericEntity entity) throws GenericEntityException {
        ModelEntity modelEntity = entity.getModelEntity();

        if (modelEntity == null) {
            throw new GenericModelException("Could not find ModelEntity record for entityName: " + entity.getEntityName());
        }

        SQLProcessor sql = new AutoCommitSQLProcessor(helperName);

        try {
            return singleInsert(entity, modelEntity, modelEntity.getFieldsCopy(), sql.getConnection());
        } catch (GenericDataSourceException e) {
            sql.rollback();
            throw new GenericDataSourceException("Exception while inserting the following entity: " + entity.toString(), e);
        } finally {
            sql.close();
        }
    }

    private int singleInsert(GenericEntity entity, ModelEntity modelEntity, List fieldsToSave, Connection connection) throws GenericEntityException {
        if (modelEntity instanceof ModelViewEntity) {
            return singleUpdateView(entity, (ModelViewEntity) modelEntity, fieldsToSave, connection);
        }

        // if we have a STAMP_FIELD then set it with NOW.
        if (modelEntity.isField(ModelEntity.STAMP_FIELD)) {
            entity.set(ModelEntity.STAMP_FIELD, UtilDateTime.nowTimestamp());
        }

        String sql = "INSERT INTO " + modelEntity.getTableName(datasourceInfo) + " (" + modelEntity.colNameString(fieldsToSave) + ") VALUES (" +
            modelEntity.fieldsStringList(fieldsToSave, "?", ", ") + ")";

        SQLProcessor sqlP = new PassThruSQLProcessor(helperName, connection);

        try {
            sqlP.prepareStatement(sql);
            SqlJdbcUtil.setValues(sqlP, fieldsToSave, entity, modelFieldTypeReader);
            int retVal = sqlP.executeUpdate();

            entity.modified = false;
            if (entity instanceof GenericValue) {
                ((GenericValue) entity).copyOriginalDbValues();
            }
            return retVal;
        } catch (GenericEntityException e) {
            throw new GenericEntityException("while inserting: " + entity.toString(), e);
        } finally {
            sqlP.close();
        }
    }

    public int updateAll(GenericEntity entity) throws GenericEntityException {
        ModelEntity modelEntity = entity.getModelEntity();

        if (modelEntity == null) {
            throw new GenericModelException("Could not find ModelEntity record for entityName: " + entity.getEntityName());
        }

        return customUpdate(entity, modelEntity, modelEntity.getNopksCopy());
    }

    public int update(GenericEntity entity) throws GenericEntityException {
        ModelEntity modelEntity = entity.getModelEntity();

        if (modelEntity == null) {
            throw new GenericModelException("Could not find ModelEntity record for entityName: " + entity.getEntityName());
        }

        // we don't want to update ALL fields, just the nonpk fields that are in the passed GenericEntity
        List partialFields = new ArrayList();
        Collection keys = entity.getAllKeys();

        for (int fi = 0; fi < modelEntity.getNopksSize(); fi++) {
            ModelField curField = modelEntity.getNopk(fi);

            if (keys.contains(curField.getName()))
                partialFields.add(curField);
        }

        return customUpdate(entity, modelEntity, partialFields);
    }

    private int customUpdate(GenericEntity entity, ModelEntity modelEntity, List fieldsToSave) throws GenericEntityException {
        SQLProcessor sqlP = new AutoCommitSQLProcessor(helperName);

        try {
            return singleUpdate(entity, modelEntity, fieldsToSave, sqlP.getConnection());
        } catch (GenericDataSourceException e) {
            sqlP.rollback();
            throw new GenericDataSourceException("Exception while updating the following entity: " + entity.toString(), e);
        } finally {
            sqlP.close();
        }
    }

    private int singleUpdate(GenericEntity entity, ModelEntity modelEntity, List fieldsToSave, Connection connection) throws GenericEntityException {
        if (modelEntity instanceof ModelViewEntity) {
            return singleUpdateView(entity, (ModelViewEntity) modelEntity, fieldsToSave, connection);
        }

        // no non-primaryKey fields, update doesn't make sense, so don't do it
        if (fieldsToSave.size() <= 0) {
            if (Debug.verboseOn()) Debug.logVerbose("Trying to do an update on an entity with no non-PK fields, returning having done nothing; entity=" + entity);
            // returning one because it was effectively updated, ie the same thing, so don't trigger any errors elsewhere
            return 1;
        }

        if (modelEntity.lock()) {
            GenericEntity entityCopy = new GenericEntity(entity);

            select(entityCopy, connection);
            Object stampField = entity.get(ModelEntity.STAMP_FIELD);

            if ((stampField != null) && (!stampField.equals(entityCopy.get(ModelEntity.STAMP_FIELD)))) {
                String lockedTime = entityCopy.getTimestamp(ModelEntity.STAMP_FIELD).toString();

                throw new EntityLockedException("You tried to update an old version of this data. Version locked: (" + lockedTime + ")");
            }
        }

        // if we have a STAMP_FIELD then update it with NOW.
        if (modelEntity.isField(ModelEntity.STAMP_FIELD)) {
            entity.set(ModelEntity.STAMP_FIELD, UtilDateTime.nowTimestamp());
        }

        String sql = "UPDATE " + modelEntity.getTableName(datasourceInfo) + " SET " + modelEntity.colNameString(fieldsToSave, "=?, ", "=?") + " WHERE " +
            SqlJdbcUtil.makeWhereStringFromFields(modelEntity.getPksCopy(), entity, "AND");

        SQLProcessor sqlP = new PassThruSQLProcessor(helperName, connection);

        int retVal = 0;

        try {
            sqlP.prepareStatement(sql);
            SqlJdbcUtil.setValues(sqlP, fieldsToSave, entity, modelFieldTypeReader);
            SqlJdbcUtil.setPkValues(sqlP, modelEntity, entity, modelFieldTypeReader);
            retVal = sqlP.executeUpdate();
            entity.modified = false;
            if (entity instanceof GenericValue) {
                ((GenericValue) entity).copyOriginalDbValues();
            }
        } catch (GenericEntityException e) {
            throw new GenericEntityException("while updating: " + entity.toString(), e);
        } finally {
            sqlP.close();
        }

        if (retVal == 0) {
            throw new GenericEntityNotFoundException("Tried to update an entity that does not exist.");
        }
        return retVal;
    }

    /** Store the passed entity - insert if does not exist, otherwise update */
    private int singleStore(GenericEntity entity, Connection connection) throws GenericEntityException {
        GenericPK tempPK = entity.getPrimaryKey();
        ModelEntity modelEntity = entity.getModelEntity();

        try {
            // must use same connection for select or it won't be in the same transaction...
            select(tempPK, connection);
        } catch (GenericEntityNotFoundException e) {
            // Debug.logInfo(e);
            // select failed, does not exist, insert
            return singleInsert(entity, modelEntity, modelEntity.getFieldsCopy(), connection);
        }
        // select did not fail, so exists, update

        List partialFields = new ArrayList();
        Collection keys = entity.getAllKeys();

        for (int fi = 0; fi < modelEntity.getNopksSize(); fi++) {
            ModelField curField = modelEntity.getNopk(fi);

            // we don't want to update ALL fields, just the nonpk fields that are in the passed GenericEntity
            if (keys.contains(curField.getName())) {
                //also, only update the fields that have changed, since we have the selected values in tempPK we can compare
                if (entity.get(curField.getName()) == null) {
                    if (tempPK.get(curField.getName()) != null) {
                        //entity field is null, tempPK is not so are different
                        partialFields.add(curField);
                    }
                } else if (!entity.get(curField.getName()).equals(tempPK.get(curField.getName()))) {
                    //entity field is not null, and compared to tempPK field is different
                    partialFields.add(curField);
                }
            }
        }

        return singleUpdate(entity, modelEntity, partialFields, connection);
    }

    public int storeAll(List entities) throws GenericEntityException {
        if (entities == null || entities.size() <= 0) {
            return 0;
        }

        SQLProcessor sqlP = new ExplcitCommitSQLProcessor(helperName);

        int totalStored = 0;

        try {
            Iterator entityIter = entities.iterator();

            while (entityIter != null && entityIter.hasNext()) {
                GenericEntity curEntity = (GenericEntity) entityIter.next();

                totalStored += singleStore(curEntity, sqlP.getConnection());
            }
        } catch (GenericDataSourceException e) {
            sqlP.rollback();
            throw new GenericDataSourceException("Exception occurred in storeAll", e);
        } finally {
            sqlP.close();
        }
        return totalStored;
    }

    /* ====================================================================== */

    /* ====================================================================== */

    /**
     * Try to update the given ModelViewEntity by trying to insert/update on the entities of which the view is composed.
     *
     * Works fine with standard O/R mapped models, but has some restrictions meeting more complicated view entities.
     * <li>A direct link is required, which means that one of the ModelViewLink field entries must have a value found
     * in the given view entity, for each ModelViewLink</li>
     * <li>For now, each member entity is updated iteratively, so if eg. the second member entity fails to update,
     * the first is written although. See code for details. Try to use "clean" views, until code is more robust ...</li>
     * <li>For now, aliased field names in views are not processed correctly, I guess. To be honest, I did not
     * find out how to construct such a view - so view fieldnames must have same named fields in member entities.</li>
     * <li>A new exception, e.g. GenericViewNotUpdatable, should be defined and thrown if the update fails</li>
     *
     */
    private int singleUpdateView(GenericEntity entity, ModelViewEntity modelViewEntity, List fieldsToSave, Connection connection) throws GenericEntityException {
        GenericDelegator delegator = entity.getDelegator();

        int retVal = 0;
        ModelEntity memberModelEntity = null;

        // Construct insert/update for each model entity
        Iterator meIter = modelViewEntity.getMemberModelMemberEntities().entrySet().iterator();

        while (meIter != null && meIter.hasNext()) {
            Map.Entry meMapEntry = (Map.Entry) meIter.next();
            ModelViewEntity.ModelMemberEntity modelMemberEntity = (ModelViewEntity.ModelMemberEntity) meMapEntry.getValue();
            String meName = modelMemberEntity.getEntityName();
            String meAlias = modelMemberEntity.getEntityAlias();

	        if (Debug.verboseOn()) Debug.logVerbose("[singleUpdateView]: Processing MemberEntity " + meName + " with Alias " + meAlias);
            try {
                memberModelEntity = delegator.getModelReader().getModelEntity(meName);
            } catch (GenericEntityException e) {
                throw new GenericEntityException("Failed to get model entity for " + meName, e);
            }

            Map findByMap = new Hashtable();

            // Now iterate the ModelViewLinks to construct the "WHERE" part for update/insert
            Iterator linkIter = modelViewEntity.getViewLinksIterator();

            while (linkIter != null && linkIter.hasNext()) {
                ModelViewEntity.ModelViewLink modelViewLink = (ModelViewEntity.ModelViewLink) linkIter.next();

                if (modelViewLink.getEntityAlias().equals(meAlias) || modelViewLink.getRelEntityAlias().equals(meAlias)) {

                    Iterator kmIter = modelViewLink.getKeyMapsIterator();

                    while (kmIter != null && kmIter.hasNext()) {
                        ModelKeyMap keyMap = (ModelKeyMap) kmIter.next();

                        String fieldName = "";

                        if (modelViewLink.getEntityAlias().equals(meAlias)) {
                            fieldName = keyMap.getFieldName();
                        } else {
                            fieldName = keyMap.getRelFieldName();
                        }

                        if (Debug.verboseOn()) Debug.logVerbose("[singleUpdateView]: --- Found field to set: " + meAlias + "." + fieldName);
                        Object value = null;

                        if (modelViewEntity.isField(keyMap.getFieldName())) {
                            value = entity.get(keyMap.getFieldName());
                            if (Debug.verboseOn()) Debug.logVerbose("[singleUpdateView]: --- Found map value: " + value.toString());
                        } else if (modelViewEntity.isField(keyMap.getRelFieldName())) {
                            value = entity.get(keyMap.getRelFieldName());
                            if (Debug.verboseOn()) Debug.logVerbose("[singleUpdateView]: --- Found map value: " + value.toString());
                        } else {
                            throw new GenericNotImplementedException("Update on view entities: no direct link found, unable to update");
                        }

                        findByMap.put(fieldName, value);
                    }
                }
            }

            // Look what there already is in the database
            List meResult = null;

            try {
                meResult = delegator.findByAnd(meName, findByMap);
            } catch (GenericEntityException e) {
                throw new GenericEntityException("Error while retrieving partial results for entity member: " + meName, e);
            }
            if (Debug.verboseOn()) Debug.logVerbose("[singleUpdateView]: --- Found " + meResult.size() + " results for entity member " + meName);

            // Got results 0 -> INSERT, 1 -> UPDATE, >1 -> View is nor updatable
            GenericValue meGenericValue = null;

            if (meResult.size() == 0) {
                // Create new value to insert
                try {
                    // Create new value to store
                    meGenericValue = delegator.makeValue(meName, findByMap);
                } catch (Exception e) {
                    throw new GenericEntityException("Could not create new value for member entity" + meName + " of view " + modelViewEntity.getEntityName(), e);
                }
            } else if (meResult.size() == 1) {
                // Update existing value
                meGenericValue = (GenericValue) meResult.iterator().next();
            } else {
                throw new GenericEntityException("Found more than one result for member entity " + meName + " in view " + modelViewEntity.getEntityName() + " - this is no updatable view");
            }

            // Construct fieldsToSave list for this member entity
            List meFieldsToSave = new Vector();
            Iterator fieldIter = fieldsToSave.iterator();

            while (fieldIter != null && fieldIter.hasNext()) {
                ModelField modelField = (ModelField) fieldIter.next();

                if (memberModelEntity.isField(modelField.getName())) {
                    ModelField meModelField = memberModelEntity.getField(modelField.getName());

                    if (meModelField != null) {
                        meGenericValue.set(meModelField.getName(), entity.get(modelField.getName()));
                        meFieldsToSave.add(meModelField);
                        if (Debug.verboseOn()) Debug.logVerbose("[singleUpdateView]: --- Added field to save: " + meModelField.getName() + " with value " + meGenericValue.get(meModelField.getName()));
                    } else {
                        throw new GenericEntityException("Could not get field " + modelField.getName() + " from model entity " + memberModelEntity.getEntityName());
                    }
                }
            }

            /*
             * Finally, do the insert/update
             * TODO:
             * Do the real inserts/updates outside the memberEntity-loop,
             * only if all of the found member entities are updatable.
             * This avoids partial creation of member entities, which would mean data inconsistency:
             * If not all member entities can be updated, then none should be updated
             */
            if (meResult.size() == 0) {
                retVal += singleInsert(meGenericValue, memberModelEntity, memberModelEntity.getFieldsCopy(), connection);
            } else {
                if (meFieldsToSave.size() > 0) {
                    retVal += singleUpdate(meGenericValue, memberModelEntity, meFieldsToSave, connection);
                } else {
                    if (Debug.verboseOn()) Debug.logVerbose("[singleUpdateView]: No update on member entity " + memberModelEntity.getEntityName() + " needed");
                }
            }
        }

        return retVal;
    }

    /* ====================================================================== */

    /* ====================================================================== */

    public void select(GenericEntity entity) throws GenericEntityException {
        SQLProcessor sqlP = new ReadOnlySQLProcessor(helperName);

        try {
            select(entity, sqlP.getConnection());
        } finally {
            sqlP.close();
        }
    }

    public void select(GenericEntity entity, Connection connection) throws GenericEntityException {
        ModelEntity modelEntity = entity.getModelEntity();

        if (modelEntity == null) {
            throw new GenericModelException("Could not find ModelEntity record for entityName: " + entity.getEntityName());
        }

        if (modelEntity.getPksSize() <= 0) {
            throw new GenericEntityException("Entity has no primary keys, cannot select by primary key");
        }

        StringBuffer sqlBuffer = new StringBuffer("SELECT ");

        if (modelEntity.getNopksSize() > 0) {
            sqlBuffer.append(modelEntity.colNameString(modelEntity.getNopksCopy(), ", ", ""));
        } else {
            sqlBuffer.append("*");
        }

        sqlBuffer.append(SqlJdbcUtil.makeFromClause(modelEntity, datasourceInfo));
        sqlBuffer.append(SqlJdbcUtil.makeWhereClause(modelEntity, modelEntity.getPksCopy(), entity, "AND", datasourceInfo.joinStyle));

        SQLProcessor sqlP = new PassThruSQLProcessor(helperName, connection);

        try {
            sqlP.prepareStatement(sqlBuffer.toString(), true, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            SqlJdbcUtil.setPkValues(sqlP, modelEntity, entity, modelFieldTypeReader);
            sqlP.executeQuery();

            if (sqlP.next()) {
                for (int j = 0; j < modelEntity.getNopksSize(); j++) {
                    ModelField curField = modelEntity.getNopk(j);

                    SqlJdbcUtil.getValue(sqlP.getResultSet(), j + 1, curField, entity, modelFieldTypeReader);
                }

                entity.modified = false;
                if (entity instanceof GenericValue) {
                    ((GenericValue) entity).copyOriginalDbValues();
                }
            } else {
                // Debug.logWarning("[GenericDAO.select]: select failed, result set was empty for entity: " + entity.toString());
                throw new GenericEntityNotFoundException("Result set was empty for entity: " + entity.toString());
            }
        } finally {
            sqlP.close();
        }
    }

    public void partialSelect(GenericEntity entity, Set keys) throws GenericEntityException {
        ModelEntity modelEntity = entity.getModelEntity();

        if (modelEntity == null) {
            throw new GenericModelException("Could not find ModelEntity record for entityName: " + entity.getEntityName());
        }

        if (modelEntity instanceof ModelViewEntity) {
            throw new org.ofbiz.core.entity.GenericNotImplementedException("Operation partialSelect not supported yet for view entities");
        }

        /*
         if(entity == null || entity.<%=modelEntity.pkNameString(" == null || entity."," == null")%>) {
         Debug.logWarning("[GenericDAO.select]: Cannot select GenericEntity: required primary key field(s) missing.");
         return false;
         }
         */
        // we don't want to select ALL fields, just the nonpk fields that are in the passed GenericEntity
        List partialFields = new ArrayList();

        Set tempKeys = new TreeSet(keys);

        for (int fi = 0; fi < modelEntity.getNopksSize(); fi++) {
            ModelField curField = modelEntity.getNopk(fi);

            if (tempKeys.contains(curField.getName())) {
                partialFields.add(curField);
                tempKeys.remove(curField.getName());
            }
        }

        if (tempKeys.size() > 0) {
            throw new GenericModelException("In partialSelect invalid field names specified: " + tempKeys.toString());
        }

        StringBuffer sqlBuffer = new StringBuffer("SELECT ");

        if (partialFields.size() > 0) {
            sqlBuffer.append(modelEntity.colNameString(partialFields, ", ", ""));
        } else {
            sqlBuffer.append("*");
        }
        sqlBuffer.append(SqlJdbcUtil.makeFromClause(modelEntity, datasourceInfo));
        sqlBuffer.append(SqlJdbcUtil.makeWhereClause(modelEntity, modelEntity.getPksCopy(), entity, "AND", datasourceInfo.joinStyle));

        SQLProcessor sqlP = new ReadOnlySQLProcessor(helperName);

        try {
            sqlP.prepareStatement(sqlBuffer.toString(), true, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            SqlJdbcUtil.setPkValues(sqlP, modelEntity, entity, modelFieldTypeReader);
            sqlP.executeQuery();

            if (sqlP.next()) {
                for (int j = 0; j < partialFields.size(); j++) {
                    ModelField curField = (ModelField) partialFields.get(j);

                    SqlJdbcUtil.getValue(sqlP.getResultSet(), j + 1, curField, entity, modelFieldTypeReader);
                }

                entity.modified = false;
                if (entity instanceof GenericValue) {
                    ((GenericValue) entity).copyOriginalDbValues();
                }
            } else {
                // Debug.logWarning("[GenericDAO.select]: select failed, result set was empty.");
                throw new GenericEntityNotFoundException("Result set was empty for entity: " + entity.toString());
            }
        } finally {
            sqlP.close();
        }
    }

    public List selectByAnd(ModelEntity modelEntity, Map fields, List orderBy) throws GenericEntityException {
        if (modelEntity == null) {
            return null;
        }

        EntityCondition entityCondition = null;

        if (fields != null) {
            entityCondition = new EntityFieldMap(fields, EntityOperator.AND);
        }

        EntityListIterator entityListIterator = null;

        try {
            entityListIterator = selectListIteratorByCondition(modelEntity, entityCondition, null, null, orderBy, null);
            return entityListIterator.getCompleteList();
        } finally {
            if (entityListIterator != null) {
                entityListIterator.close();
            }
        }
    }

    public List selectByOr(ModelEntity modelEntity, Map fields, List orderBy) throws GenericEntityException {
        if (modelEntity == null) {
            return null;
        }

        EntityCondition entityCondition = null;

        if (fields != null) {
            entityCondition = new EntityFieldMap(fields, EntityOperator.OR);
        }

        EntityListIterator entityListIterator = null;

        try {
            entityListIterator = selectListIteratorByCondition(modelEntity, entityCondition, null, null, orderBy, null);
            return entityListIterator.getCompleteList();
        } finally {
            if (entityListIterator != null) {
                entityListIterator.close();
            }
        }
    }

    /* tentatively removing by clause methods, unless there are really big complaints... because it is a kludge
    public List selectByClause(ModelEntity modelEntity, List entityClauses, Map fields, List orderBy) throws GenericEntityException {
        if (modelEntity == null)
            return null;
        if (modelEntity instanceof ModelViewEntity) {
            throw new org.ofbiz.core.entity.GenericNotImplementedException("Operation insert not supported yet for view entities");
        }

        boolean verboseOn = Debug.verboseOn();

        if (verboseOn) Debug.logVerbose("[selectByClause] Start");
        if (entityClauses == null)
            return null;

        List list = new LinkedList();
        ModelEntity firstModelEntity = null;
        ModelEntity secondModelEntity = null;

        ModelField firstModelField = null;
        ModelField secondModelField = null;

        if (verboseOn) Debug.logVerbose("[selectByClause] Starting to build select statement.");
        StringBuffer select = new StringBuffer(" SELECT DISTINCT ");
        StringBuffer from = new StringBuffer(" FROM ");
        StringBuffer where = new StringBuffer("");

        if (entityClauses.size() > 0) where.append(" WHERE ");
        StringBuffer order = new StringBuffer();

        String test = "";

        List whereTables = new ArrayList();

        // Add the main table to the FROM clause in case there are no WHERE clause entries.
        whereTables.add(modelEntity.getTableName(datasourceInfo));

        if (verboseOn) Debug.logVerbose("[selectByClause] Starting to iterate through entity clauses.");
        ModelReader entityModelReader = modelEntity.getModelReader();
        boolean paren = false;

        // Each iteration defines one relationship for the query.
        for (int i = 0; i < entityClauses.size(); i++) {
            if (verboseOn) Debug.logVerbose("[selectByClause] Processing entity clause " + String.valueOf(i));
            EntityClause entityClause = (EntityClause) entityClauses.get(i);

            if (verboseOn) Debug.logVerbose("[selectByClause] Entity clause: " + entityClause.toString());
            EntityClause nextEntityClause = null;
            // get the next interFieldOperation.  This is used to determine if
            // we need to insert a parenthesis.
            String nextInterFieldOperation = null;

            if ((i + 1) < entityClauses.size()) {
                nextEntityClause = (EntityClause) entityClauses.get(i + 1);
            } else {
                nextEntityClause = (EntityClause) entityClauses.get(i);
            }

            if (nextEntityClause != null) {
                if (verboseOn) Debug.logVerbose("[selectByClause] Next entity clause: " + nextEntityClause.toString());
                nextInterFieldOperation = nextEntityClause.getInterFieldOperation().getCode();
            }
            String interFieldOperation = entityClause.getInterFieldOperation().toString();
            String intraFieldOperation = entityClause.getIntraFieldOperation().toString();

            if (verboseOn) Debug.logVerbose("[selectByClause] Got operations");
            firstModelEntity = entityClause.getFirstModelEntity();
            if (!whereTables.contains(firstModelEntity.getTableName(datasourceInfo))) {
                whereTables.add(firstModelEntity.getTableName(datasourceInfo));
            }
            if (verboseOn) Debug.logVerbose("[selectByClause] Got first model entity.");

            if (entityClause.getSecondEntity().trim().length() > 0) {
                secondModelEntity = entityClause.getSecondModelEntity();
                if (verboseOn) Debug.logVerbose("[selectByClause] Got second model entity.");
                if (!whereTables.contains(secondModelEntity.getTableName(datasourceInfo))) {
                    whereTables.add(secondModelEntity.getTableName(datasourceInfo));
                }
                secondModelField = secondModelEntity.getField(entityClause.getSecondField());
            }

            firstModelField = firstModelEntity.getField(entityClause.getFirstField());

            test = where.toString();
            if (i > 0) {
                where.append(' ');
                where.append(interFieldOperation);
                where.append(' ');
            }
            // if the next interFieldOperation is an OR, add a parenthesis.
            if (nextInterFieldOperation != null && nextInterFieldOperation.trim().equals("OR") && !paren) {
                where.append(" ( ");
                paren = true;
            }

            if (verboseOn) Debug.logVerbose("[selectByClause] About to append entity clause info onto select statement.");
            if (entityClause.getSecondEntity().trim().length() > 0) {
                // Entity clause has a second entity and field instead of a constant value.
                where.append(firstModelEntity.getTableName(datasourceInfo));
                if (verboseOn) Debug.logVerbose("[selectByClause] Method 2 - Appended first table name: " +
                        firstModelEntity.getTableName(datasourceInfo));
                where.append(".");
                if (verboseOn) Debug.logVerbose("[selectByClause] Method 2 - Appended \".\"");
                if (firstModelField == null || firstModelField.getColName() == null) {
                    if (verboseOn) Debug.logVerbose("[selectByClause] Method 2 - error 1");
                    throw new GenericEntityException(entityClause.getFirstField() +
                            " is not a field of " + entityClause.getFirstEntity());
                }
                where.append(firstModelField.getColName());
                if (verboseOn) Debug.logVerbose("[selectByClause] Method 2 - Appended first column name: " +
                        firstModelField.getColName());
                where.append(" ");
                where.append(intraFieldOperation);
                if (verboseOn) Debug.logVerbose("[selectByClause] Method 2 - Appended intra field operation: " +
                        intraFieldOperation);
                where.append(" ");
                where.append(secondModelEntity.getTableName(datasourceInfo));
                if (verboseOn) Debug.logVerbose("[selectByClause] Method 2 - Appended second table name: " +
                        secondModelEntity.getTableName(datasourceInfo));
                where.append(".");
                if (secondModelField == null || secondModelField.getColName() == null) {
                    if (verboseOn) Debug.logVerbose("[selectByClause] Method 2 - error 2");
                    throw new GenericEntityException(entityClause.getSecondField() +
                            " is not a field of " + entityClause.getSecondEntity());
                }
                where.append(secondModelField.getColName());
                if (verboseOn) Debug.logVerbose("[selectByClause] Method 1 - Appended second column name: " +
                        secondModelField.getColName());
            } else {
                // Entity clause has a constant value instead of a second entity and field.
                where.append(firstModelEntity.getTableName(datasourceInfo));
                if (verboseOn) Debug.logVerbose("[selectByClause] Method 1 - Appended first table name: " +
                        firstModelEntity.getTableName(datasourceInfo));
                where.append(".");
                if (firstModelField == null || firstModelField.getColName() == null) {
                    if (verboseOn) Debug.logVerbose("[selectByClause] Method 1 - error 1");
                    throw new GenericEntityException(entityClause.getFirstField() +
                            " is not a field of " + entityClause.getFirstEntity());
                }
                where.append(firstModelField.getColName());
                if (verboseOn) Debug.logVerbose("[selectByClause] Method 1 - Appended first column name: " +
                        firstModelField.getColName());
                where.append(" ");
                where.append(intraFieldOperation);
                if (verboseOn) Debug.logVerbose("[selectByClause] Method 2 - Appended intra field operation: " +
                        intraFieldOperation);
                if (intraFieldOperation.equals("IN")) {
                    if (verboseOn) Debug.logVerbose("[selectByClause] Intrafield operation is IN");
                    where.append(" (");
                } else {
                    if (verboseOn) Debug.logVerbose("[selectByClause] Intrafield operation is not IN");
                    where.append(" '");
                }
                where.append(entityClause.getValue());
                if (verboseOn) Debug.logVerbose("[selectByClause] Method 2 - Appended value: " + entityClause.getValue());
                if (intraFieldOperation.equals("IN")) {
                    where.append(")");
                } else {
                    where.append("' ");
                }
            }

            if ((nextInterFieldOperation != null && !nextInterFieldOperation.trim().equals("OR") && paren) ||
                (i == (entityClauses.size() - 1) && nextInterFieldOperation.trim().equals("OR") && paren)) {
                where.append(" ) ");
                paren = false;
            }
        }

        List whereFields = new ArrayList();
        List selectFields = new ArrayList();

        // Add all fields from the main model entity to the selected fields list.
        Set keys = null;

        if (fields != null && fields.size() > 0) {
            keys = fields.keySet();
        }
        for (int fi = 0; fi < modelEntity.getFieldsSize(); fi++) {
            ModelField curField = modelEntity.getField(fi);

            if (verboseOn) Debug.logVerbose("[selectByClause] Adding field " + curField.getName() + " to selectFields");
            selectFields.add(curField);

            // Add all filter fields to the where field list.
            if (keys != null && keys.contains(curField.getName()))
                whereFields.add(curField);
        }

        String tableNamePrefix = modelEntity.getTableName(datasourceInfo) + ".";

        // Construct the SELECT clause.
        select.append(tableNamePrefix);
        select.append(modelEntity.colNameString(selectFields, ", " + tableNamePrefix, ""));

        // Construct the FROM clause.
        int ix = 0;

        for (; ix < whereTables.size() - 1; ix++) {
            from.append(whereTables.get(ix) + ", ");
        }
        from.append(whereTables.get(ix));

        // Construct the WHERE clause.
        if (fields != null && fields.size() > 0) {
            // Add filter fields.
            test = where.toString();
            if (test.trim().length() > 0)
                where.append(" AND ");
            where.append(tableNamePrefix);
            where.append(modelEntity.colNameString(whereFields, "=? AND " + tableNamePrefix, "=?"));
        }

        // Construct the ORDER BY clause.
        order.append(SqlJdbcUtil.makeOrderByClause(modelEntity, orderBy));

        String sql = "";

        SQLProcessor sqlP = new SQLProcessor(helperName);

        try {
            sql = select.toString() + " " + from.toString() + " " + where.toString() + (order.toString().trim().length() > 0 ? order.toString() : "");
            sqlP.prepareStatement(sql, true, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);

            GenericValue dummyValue = new GenericValue(modelEntity, fields);

            if (fields != null && fields.size() > 0) {
                SqlJdbcUtil.setValuesWhereClause(sqlP, whereFields, dummyValue, modelFieldTypeReader);
            }
            sqlP.executeQuery();

            while (sqlP.next()) {
                GenericValue value = new GenericValue(dummyValue);

                for (int j = 0; j < selectFields.size(); j++) {
                    ModelField curField = (ModelField) selectFields.get(j);

                    SqlJdbcUtil.getValue(sqlP.getResultSet(), j + 1, curField, value, modelFieldTypeReader);
                }

                value.modified = false;
                value.copyOriginalDbValues();
                list.add(value);
            }
        } finally {
            sqlP.close();
        }

        return list;
    }
     */

    /* ====================================================================== */

    /* ====================================================================== */

    /** Finds GenericValues by the conditions specified in the EntityCondition object, the the EntityCondition javadoc for more details.
     *@param modelEntity The ModelEntity of the Entity as defined in the entity XML file
     *@param entityCondition The EntityCondition object that specifies how to constrain this query
     *@param fieldsToSelect The fields of the named entity to get from the database; if empty or null all fields will be retreived
     *@param orderBy The fields of the named entity to order the query by; optionally add a " ASC" for ascending or " DESC" for descending
     *@return List of GenericValue objects representing the result
     */
    public List selectByCondition(ModelEntity modelEntity, EntityCondition entityCondition, Collection fieldsToSelect, List orderBy) throws GenericEntityException {
        EntityListIterator entityListIterator = null;

        try {
            entityListIterator = selectListIteratorByCondition(modelEntity, entityCondition, null, fieldsToSelect, orderBy, null);
            return entityListIterator.getCompleteList();
        } finally {
            if (entityListIterator != null) {
                entityListIterator.close();
            }
        }
    }

    /** Finds GenericValues by the conditions specified in the EntityCondition object, the the EntityCondition javadoc for more details.
     *@param modelEntity The ModelEntity of the Entity as defined in the entity XML file
     *@param whereEntityCondition The EntityCondition object that specifies how to constrain this query before any groupings are done (if this is a view entity with group-by aliases)
     *@param havingEntityCondition The EntityCondition object that specifies how to constrain this query after any groupings are done (if this is a view entity with group-by aliases)
     *@param fieldsToSelect The fields of the named entity to get from the database; if empty or null all fields will be retreived
     *@param orderBy The fields of the named entity to order the query by; optionally add a " ASC" for ascending or " DESC" for descending
     *@param findOptions An instance of EntityFindOptions that specifies advanced query options. See the EntityFindOptions JavaDoc for more details.
     *@return EntityListIterator representing the result of the query: NOTE THAT THIS MUST BE CLOSED WHEN YOU ARE 
     *      DONE WITH IT, AND DON'T LEAVE IT OPEN TOO LONG BEACUSE IT WILL MAINTAIN A DATABASE CONNECTION.
     */
    public EntityListIterator selectListIteratorByCondition(ModelEntity modelEntity, EntityCondition whereEntityCondition,
        EntityCondition havingEntityCondition, Collection fieldsToSelect, List orderBy, EntityFindOptions findOptions)
        throws GenericEntityException {
        if (modelEntity == null) {
            return null;
        }

        // if no find options passed, use default
        if (findOptions == null) findOptions = new EntityFindOptions();

        boolean verboseOn = Debug.verboseOn();

        if (verboseOn) {
            // put this inside an if statement so that we don't have to generate the string when not used...
            Debug.logVerbose("Doing selectListIteratorByCondition with whereEntityCondition: " + whereEntityCondition);
        }

        // make two ArrayLists of fields, one for fields to select and the other for where clause fields (to find by)
        List selectFields = new ArrayList();

        if (fieldsToSelect != null && fieldsToSelect.size() > 0) {
            Set tempKeys = new HashSet(fieldsToSelect);

            for (int fi = 0; fi < modelEntity.getFieldsSize(); fi++) {
                ModelField curField = modelEntity.getField(fi);

                if (tempKeys.contains(curField.getName())) {
                    selectFields.add(curField);
                    tempKeys.remove(curField.getName());
                }
            }

            if (tempKeys.size() > 0) {
                throw new GenericModelException("In selectListIteratorByCondition invalid field names specified: " + tempKeys.toString());
            }
        } else {
            selectFields = modelEntity.getFieldsCopy();
        }

        GenericValue dummyValue = new GenericValue(modelEntity);
        StringBuffer sqlBuffer = new StringBuffer("SELECT ");

        if (findOptions.getDistinct()) {
            sqlBuffer.append("DISTINCT ");
        }

        if (selectFields.size() > 0) {
            sqlBuffer.append(modelEntity.colNameString(selectFields, ", ", ""));
        } else {
            sqlBuffer.append("*");
        }

        // FROM clause and when necessary the JOIN or LEFT JOIN clause(s) as well
        sqlBuffer.append(SqlJdbcUtil.makeFromClause(modelEntity, datasourceInfo));

        // WHERE clause
        StringBuffer whereString = new StringBuffer();
        String entityCondWhereString = "";
        List whereEntityConditionParams = new LinkedList();

        if (whereEntityCondition != null) {
            entityCondWhereString = whereEntityCondition.makeWhereString(modelEntity, whereEntityConditionParams);
        }

        String viewClause = SqlJdbcUtil.makeViewWhereClause(modelEntity, datasourceInfo.joinStyle);

        if (viewClause.length() > 0) {
            if (entityCondWhereString.length() > 0) {
                whereString.append("(");
                whereString.append(entityCondWhereString);
                whereString.append(") AND ");
            }

            whereString.append(viewClause);
        } else {
            whereString.append(entityCondWhereString);
        }

        if (whereString.length() > 0) {
            sqlBuffer.append(" WHERE ");
            sqlBuffer.append(whereString.toString());
        }

        // GROUP BY clause for view-entity
        if (modelEntity instanceof ModelViewEntity) {
            ModelViewEntity modelViewEntity = (ModelViewEntity) modelEntity;
            String groupByString = modelViewEntity.colNameString(modelViewEntity.getGroupBysCopy(), ", ", "");

            if (UtilValidate.isNotEmpty(groupByString)) {
                sqlBuffer.append(" GROUP BY ");
                sqlBuffer.append(groupByString);
            }
        }

        // HAVING clause
        String entityCondHavingString = "";
        List havingEntityConditionParams = new LinkedList();

        if (havingEntityCondition != null) {
            entityCondHavingString = havingEntityCondition.makeWhereString(modelEntity, havingEntityConditionParams);
        }
        if (entityCondHavingString.length() > 0) {
            sqlBuffer.append(" HAVING ");
            sqlBuffer.append(entityCondHavingString);
        }

        // ORDER BY clause
        sqlBuffer.append(SqlJdbcUtil.makeOrderByClause(modelEntity, orderBy, datasourceInfo));
        String sql = sqlBuffer.toString();

        SQLProcessor sqlP = new ReadOnlySQLProcessor(helperName);

        sqlP.prepareStatement(sql, findOptions.getSpecifyTypeAndConcur(), findOptions.getResultSetType(), findOptions.getResultSetConcurrency());
        if (verboseOn) {
            // put this inside an if statement so that we don't have to generate the string when not used...
            Debug.logVerbose("Setting the whereEntityConditionParams: " + whereEntityConditionParams);
        }
        // set all of the values from the Where EntityCondition
        Iterator whereEntityConditionParamsIter = whereEntityConditionParams.iterator();

        while (whereEntityConditionParamsIter.hasNext()) {
            EntityConditionParam whereEntityConditionParam = (EntityConditionParam) whereEntityConditionParamsIter.next();

            SqlJdbcUtil.setValue(sqlP, whereEntityConditionParam.getModelField(), modelEntity.getEntityName(), whereEntityConditionParam.getFieldValue(), modelFieldTypeReader);
        }
        if (verboseOn) {
            // put this inside an if statement so that we don't have to generate the string when not used...
            Debug.logVerbose("Setting the havingEntityConditionParams: " + havingEntityConditionParams);
        }
        // set all of the values from the Having EntityCondition
        Iterator havingEntityConditionParamsIter = havingEntityConditionParams.iterator();

        while (havingEntityConditionParamsIter.hasNext()) {
            EntityConditionParam havingEntityConditionParam = (EntityConditionParam) havingEntityConditionParamsIter.next();

            SqlJdbcUtil.setValue(sqlP, havingEntityConditionParam.getModelField(), modelEntity.getEntityName(), havingEntityConditionParam.getFieldValue(), modelFieldTypeReader);
        }

        sqlP.executeQuery();

        return new EntityListIterator(sqlP, modelEntity, selectFields, modelFieldTypeReader);
    }

    public List selectByMultiRelation(GenericValue value, ModelRelation modelRelationOne, ModelEntity modelEntityOne,
        ModelRelation modelRelationTwo, ModelEntity modelEntityTwo, List orderBy) throws GenericEntityException {
        SQLProcessor sqlP = new ReadOnlySQLProcessor(helperName);

        // get the tables names
        String atable = modelEntityOne.getTableName(datasourceInfo);
        String ttable = modelEntityTwo.getTableName(datasourceInfo);

        // get the column name string to select
        StringBuffer selsb = new StringBuffer();
        ArrayList collist = new ArrayList();
        ArrayList fldlist = new ArrayList();

        for (Iterator iterator = modelEntityTwo.getFieldsIterator(); iterator.hasNext();) {
            ModelField mf = (ModelField) iterator.next();

            collist.add(mf.getColName());
            fldlist.add(mf.getName());
            selsb.append(ttable + "." + mf.getColName());
            if (iterator.hasNext()) {
                selsb.append(", ");
            } else {
                selsb.append(" ");
            }
        }

        // construct assoc->target relation string
        int kmsize = modelRelationTwo.getKeyMapsSize();
        StringBuffer wheresb = new StringBuffer();

        for (int i = 0; i < kmsize; i++) {
            ModelKeyMap mkm = modelRelationTwo.getKeyMap(i);
            String lfname = mkm.getFieldName();
            String rfname = mkm.getRelFieldName();

            if (wheresb.length() > 0) {
                wheresb.append(" AND ");
            }
            wheresb.append(atable + "." + modelEntityOne.getField(lfname).getColName() + " = " + ttable + "." + modelEntityTwo.getField(rfname).getColName());
        }

        // construct the source entity qualifier
        // get the fields from relation description
        kmsize = modelRelationOne.getKeyMapsSize();
        HashMap bindMap = new HashMap();

        for (int i = 0; i < kmsize; i++) {
            // get the equivalent column names in the relation
            ModelKeyMap mkm = modelRelationOne.getKeyMap(i);
            String sfldname = mkm.getFieldName();
            String lfldname = mkm.getRelFieldName();
            ModelField amf = modelEntityOne.getField(lfldname);
            String lcolname = amf.getColName();
            Object rvalue = value.get(sfldname);

            bindMap.put(amf, rvalue);
            // construct one condition
            if (wheresb.length() > 0) {
                wheresb.append(" AND ");
            }
            wheresb.append(atable + "." + lcolname + " = ? ");
        }

        // construct a join sql query
        StringBuffer sqlsb = new StringBuffer();

        sqlsb.append("SELECT ");
        sqlsb.append(selsb.toString());
        sqlsb.append(" FROM ");
        sqlsb.append(atable + ", " + ttable);
        sqlsb.append(" WHERE ");
        sqlsb.append(wheresb.toString());
        sqlsb.append(SqlJdbcUtil.makeOrderByClause(modelEntityTwo, orderBy, true, datasourceInfo));

        // now execute the query
        ArrayList retlist = new ArrayList();
        GenericDelegator gd = value.getDelegator();

        try {
            sqlP.prepareStatement(sqlsb.toString());
            Set entrySet = bindMap.entrySet();

            for (Iterator iterator = entrySet.iterator(); iterator.hasNext();) {
                Map.Entry entry = (Map.Entry) iterator.next();
                ModelField mf = (ModelField) entry.getKey();
                Object curvalue = entry.getValue();

                SqlJdbcUtil.setValue(sqlP, mf, modelEntityOne.getEntityName(), curvalue, modelFieldTypeReader);
            }
            sqlP.executeQuery();
            int collsize = collist.size();

            while (sqlP.next()) {
                GenericValue gv = gd.makeValue(modelEntityTwo.getEntityName(), Collections.EMPTY_MAP);

                // loop thru all columns for in one row
                for (int j = 0; j < collsize; j++) {
                    String fldname = (String) fldlist.get(j);
                    ModelField mf = modelEntityTwo.getField(fldname);

                    SqlJdbcUtil.getValue(sqlP.getResultSet(), j + 1, mf, gv, modelFieldTypeReader);
                }
                retlist.add(gv);
            }
        } finally {
            sqlP.close();
        }

        return retlist;
    }

    /* ====================================================================== */

    /* ====================================================================== */

    public int delete(GenericEntity entity) throws GenericEntityException {
        SQLProcessor sqlP = new AutoCommitSQLProcessor(helperName);

        try {
            return delete(entity, sqlP.getConnection());
        } catch (GenericDataSourceException e) {
            sqlP.rollback();
            throw new GenericDataSourceException("Exception while deleting the following entity: " + entity.toString(), e);
        } finally {
            sqlP.close();
        }
    }

    public int delete(GenericEntity entity, Connection connection) throws GenericEntityException {
        ModelEntity modelEntity = entity.getModelEntity();
        if (modelEntity == null) {
            throw new GenericModelException("Could not find ModelEntity record for entityName: " + entity.getEntityName());
        }
        if (modelEntity instanceof ModelViewEntity) {
            throw new org.ofbiz.core.entity.GenericNotImplementedException("Operation delete not supported yet for view entities");
        }

        String sql = "DELETE FROM " + modelEntity.getTableName(datasourceInfo) + " WHERE " + SqlJdbcUtil.makeWhereStringFromFields(modelEntity.getPksCopy(), entity, "AND");

        SQLProcessor sqlP = new PassThruSQLProcessor(helperName, connection);

        int retVal;

        try {
            sqlP.prepareStatement(sql);
            SqlJdbcUtil.setPkValues(sqlP, modelEntity, entity, modelFieldTypeReader);
            retVal = sqlP.executeUpdate();
            entity.modified = true;
        } finally {
            sqlP.close();
        }
        return retVal;
    }

    public int deleteByAnd(ModelEntity modelEntity, Map fields) throws GenericEntityException {
        SQLProcessor sqlP = new AutoCommitSQLProcessor(helperName);

        try {
            return deleteByAnd(modelEntity, fields, sqlP.getConnection());
        } catch (GenericDataSourceException e) {
            sqlP.rollback();
            throw new GenericDataSourceException("Generic Entity Exception occurred in deleteByAnd", e);
        } finally {
            sqlP.close();
        }
    }

    public int deleteByAnd(ModelEntity modelEntity, Map fields, Connection connection) throws GenericEntityException {
        if (modelEntity == null || fields == null) return 0;
        if (modelEntity instanceof ModelViewEntity) {
            throw new org.ofbiz.core.entity.GenericNotImplementedException("Operation deleteByAnd not supported yet for view entities");
        }

        List whereFields = new ArrayList();
        if (fields != null && fields.size() > 0) {
            for (int fi = 0; fi < modelEntity.getFieldsSize(); fi++) {
                ModelField curField = modelEntity.getField(fi);

                if (fields.containsKey(curField.getName())) {
                    whereFields.add(curField);
                }
            }
        }

        GenericValue dummyValue = new GenericValue(modelEntity, fields);
        String sql = "DELETE FROM " + modelEntity.getTableName(datasourceInfo);
        if (fields != null && fields.size() > 0) {
            sql += " WHERE " + SqlJdbcUtil.makeWhereStringFromFields(whereFields, dummyValue, "AND");
        }

        SQLProcessor sqlP = new AutoCommitSQLProcessor(helperName);
        try {
            sqlP.prepareStatement(sql);

            if (fields != null && fields.size() > 0) {
                SqlJdbcUtil.setValuesWhereClause(sqlP, whereFields, dummyValue, modelFieldTypeReader);
            }

            return sqlP.executeUpdate();
        } finally {
            sqlP.close();
        }
    }

    /** Called dummyPKs because they can be invalid PKs, doing a deleteByAnd instead of a normal delete */
    public int deleteAll(List dummyPKs) throws GenericEntityException {
        if (dummyPKs == null || dummyPKs.size() == 0) {
            return 0;
        }

        SQLProcessor sqlP = new ExplcitCommitSQLProcessor(helperName);
        try {
            Iterator iter = dummyPKs.iterator();

            int numDeleted = 0;

            while (iter.hasNext()) {
                GenericEntity entity = (GenericEntity) iter.next();

                // if it contains a complete primary key, delete the one, otherwise deleteByAnd
                if (entity.containsPrimaryKey()) {
                    numDeleted += delete(entity, sqlP.getConnection());
                } else {
                    numDeleted += deleteByAnd(entity.getModelEntity(), entity.getAllFields(), sqlP.getConnection());
                }
            }
            return numDeleted;
        } catch (GenericDataSourceException e) {
            sqlP.rollback();
            throw new GenericDataSourceException("Generic Entity Exception occurred in deleteAll", e);
        } finally {
            sqlP.close();
        }
    }

    /* ====================================================================== */

    public void checkDb(Map modelEntities, Collection messages, boolean addMissing) {
        DatabaseUtil dbUtil = new DatabaseUtil(this.helperName);

        dbUtil.checkDb(modelEntities, messages, addMissing);
    }

    /** Creates a list of ModelEntity objects based on meta data from the database */
    public List induceModelFromDb(Collection messages) {
        DatabaseUtil dbUtil = new DatabaseUtil(this.helperName);

        return dbUtil.induceModelFromDb(messages);
    }
}
