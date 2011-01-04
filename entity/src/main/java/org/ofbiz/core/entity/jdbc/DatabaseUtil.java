/*
 * $Id: DatabaseUtil.java,v 1.3 2006/03/07 01:08:05 hbarney Exp $
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
package org.ofbiz.core.entity.jdbc;

import org.ofbiz.core.entity.ConnectionFactory;
import org.ofbiz.core.entity.ConnectionProvider;
import org.ofbiz.core.entity.GenericEntityException;
import org.ofbiz.core.entity.config.DatasourceInfo;
import org.ofbiz.core.entity.config.EntityConfigUtil;
import org.ofbiz.core.entity.model.*;
import org.ofbiz.core.util.Debug;
import org.ofbiz.core.util.UtilTimer;
import org.ofbiz.core.util.UtilValidate;

import java.sql.*;
import java.util.*;

/**
 * Utilities for Entity Database Maintenance
 *
 * @author     <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 * @version    $Revision: 1.3 $
 * @since      2.0
 */
public class DatabaseUtil {

    public static final String module = DatabaseUtil.class.getName();

    protected String helperName;
    protected ModelFieldTypeReader modelFieldTypeReader;
    protected DatasourceInfo datasourceInfo;

    private final ConnectionProvider connectionProvider;

    /**
     * Constructs with the name of a helper that is used to load {@link org.ofbiz.core.entity.config.DatasourceInfo}
     * from {@link org.ofbiz.core.entity.config.EntityConfigUtil} and uses the static
     * {@link org.ofbiz.core.entity.ConnectionFactory} for connections.
     *
     * @param helperName
     */
    public DatabaseUtil(String helperName) {
        this.helperName = helperName;
        this.modelFieldTypeReader = ModelFieldTypeReader.getModelFieldTypeReader(helperName);
        this.datasourceInfo = EntityConfigUtil.getInstance().getDatasourceInfo(helperName);
        this.connectionProvider = ConnectionFactory.provider;
    }

    /**
     * Full monty constructor.
     *
     * @param helperName the helperName
     * @param modelFieldTypeReader the ModelFieldTypeReader
     * @param datasourceInfo the DatasourceInfo
     * @param connectionProvider used to create {@link java.sql.Connection Connections}.
     */
    DatabaseUtil(final String helperName, final ModelFieldTypeReader modelFieldTypeReader, final DatasourceInfo datasourceInfo, final ConnectionProvider connectionProvider) {
        this.helperName = helperName;
        this.modelFieldTypeReader = modelFieldTypeReader;
        this.datasourceInfo = datasourceInfo;
        this.connectionProvider = connectionProvider;
    }

    /**
     * Uses the configured {@link org.ofbiz.core.entity.ConnectionProvider} to get a {@link java.sql.Connection} based
     * on the configured helper name.
     *
     * @return the {@link java.sql.Connection}
     * @throws SQLException
     * @throws GenericEntityException
     */
    public Connection getConnection() throws SQLException, GenericEntityException {
        return connectionProvider.getConnection(helperName);
    }

    /**
     * Does a gzillion things to upgrade the database to the entitymodel by adding tables etc.
     *
     * @param modelEntities Model entity names to ModelEntity objects.
     * @param messages a thing to collect errors.
     * @param addMissing if true, will attempt to add tables and columns, fks, indices etc are added always.
     */
    public void checkDb(Map modelEntities, Collection messages, boolean addMissing) {

        UtilTimer timer = new UtilTimer();

        timer.timerString("Start - Before Get Database Meta Data");

        // get ALL tables from this database
        TreeSet tableNames = this.getTableNames(messages);
        TreeSet fkTableNames = tableNames == null ? null : new TreeSet(tableNames);
        TreeSet indexTableNames = tableNames == null ? null : new TreeSet(tableNames);
        // keep track of entities whose tables already existed
        Map existingTableEntities = new HashMap();

        if (tableNames == null) {

            error("Could not get table name information from the database, aborting.", messages);
            return;
        }
        timer.timerString("After Get All Table Names");

        // get ALL column info, put into hashmap by table name
        Map colInfo = this.getColumnInfo(tableNames, messages);
        if (colInfo == null) {

            error("Could not get column information from the database, aborting.", messages);
            return;
        }
        timer.timerString("After Get All Column Info");

        // -make sure all entities have a corresponding table
        // -list all tables that do not have a corresponding entity
        // -display message if number of table columns does not match number of entity fields
        // -list all columns that do not have a corresponding field
        // -make sure each corresponding column is of the correct type
        // -list all fields that do not have a corresponding column

        timer.timerString("Before Individual Table/Column Check");

        ArrayList modelEntityList = new ArrayList(modelEntities.values());

        // sort using compareTo method on ModelEntity
        Collections.sort(modelEntityList);

        Iterator modelEntityIter = modelEntityList.iterator();
        int curEnt = 0;
        int totalEnt = modelEntityList.size();
        List entitiesAdded = new LinkedList();

        while (modelEntityIter.hasNext()) {
            curEnt++;
            ModelEntity entity = (ModelEntity) modelEntityIter.next();
            String entityName = entity.getEntityName();

            // if this is a view entity, do not check it...
            if (entity instanceof ModelViewEntity) {
                verbose("(" + timer.timeSinceLast() + "ms) NOT Checking #" + curEnt + "/" + totalEnt + " View Entity " + entityName, messages);
                continue;
            }

            String tableName = entity.getTableName(datasourceInfo);
            String entMessage = "(" + timer.timeSinceLast() + "ms) Checking #" + curEnt + "/" + totalEnt +
                " Entity " + entityName + " with table " + tableName;

            verbose(entMessage, messages);

            final String upperTableName = tableName.toUpperCase();
            // -make sure all entities have a corresponding table
            if (tableNames.contains(upperTableName)) {
                tableNames.remove(upperTableName);
                existingTableEntities.put(upperTableName, entity);

                if (colInfo != null) {
                    Map fieldColNames = new HashMap();
                    for (int fnum = 0; fnum < entity.getFieldsSize(); fnum++) {
                        ModelField field = entity.getField(fnum);
                        fieldColNames.put(field.getColName().toUpperCase(), field);
                    }

                    List colList = (List) colInfo.get(upperTableName);
                    int numCols = 0;

                    if (colList != null) {
                        for (; numCols < colList.size(); numCols++) {
                            ColumnCheckInfo ccInfo = (ColumnCheckInfo) colList.get(numCols);

                            // -list all columns that do not have a corresponding field
                            if (fieldColNames.containsKey(ccInfo.columnName)) {
                                ModelField field = null;

                                field = (ModelField) fieldColNames.remove(ccInfo.columnName);
                                checkFieldType(entity, field, ccInfo, messages);
                            } else {

                                warn("Column \"" + ccInfo.columnName + "\" of table \"" + tableName + "\" of entity \"" + entityName + "\" exists in the database but has no corresponding field", messages);
                            }
                        }
                    }

                    // -display message if number of table columns does not match number of entity fields
                    if (numCols != entity.getFieldsSize()) {
                        String message = "Entity \"" + entityName + "\" has " + entity.getFieldsSize() + " fields but table \"" + tableName + "\" has " +
                            numCols + " columns.";

                        warn(message, messages);
                    }

                    // -list all fields that do not have a corresponding column
                    Iterator fcnIter = fieldColNames.keySet().iterator();

                    while (fcnIter.hasNext()) {
                        String colName = (String) fcnIter.next();
                        ModelField field = (ModelField) fieldColNames.get(colName);

                        warn("Field \"" + field.getName() + "\" of entity \"" + entityName + "\" is missing its corresponding column \"" + field.getColName() + "\"", messages);

                        if (addMissing) {
                            // add the column
                            String errMsg = addColumn(entity, field);

                            if (errMsg != null && errMsg.length() > 0) {
                                error("Could not add column \"" + field.getColName() + "\" to table \"" + tableName + "\"", messages);
                                error(errMsg, messages);
                            } else {
                                important("Added column \"" + field.getColName() + "\" to table \"" + tableName + "\"", messages);
                            }
                        }
                    }
                }
            } else {

                warn("Entity \"" + entityName + "\" has no table in the database", messages);

                if (addMissing) {
                    // create the table
                    String errMsg = createTable(entity, modelEntities, false, datasourceInfo.isUsePkConstraintNames(), datasourceInfo.getConstraintNameClipLength(), datasourceInfo.getFkStyle(), datasourceInfo.isUseFkInitiallyDeferred());

                    if (errMsg != null && errMsg.length() > 0) {
                        error("Could not create table \"" + tableName + "\"", messages);
                        error(errMsg, messages);
                    } else {
                        entitiesAdded.add(entity);
                        important("Created table \"" + tableName + "\"", messages);
                    }
                }
            }
        }

        timer.timerString("After Individual Table/Column Check");

        // -list all tables that do not have a corresponding entity
        Iterator tableNamesIter = tableNames.iterator();

        while (tableNamesIter != null && tableNamesIter.hasNext()) {
            String tableName = (String) tableNamesIter.next();

            warn("Table named \"" + tableName + "\" exists in the database but has no corresponding entity", messages);
        }

        // for each newly added table, add fks
        if (datasourceInfo.isUseFks()) {
            Iterator eaIter = entitiesAdded.iterator();

            while (eaIter.hasNext()) {
                ModelEntity curEntity = (ModelEntity) eaIter.next();
                String errMsg = this.createForeignKeys(curEntity, modelEntities, datasourceInfo.getConstraintNameClipLength(), datasourceInfo.getFkStyle(), datasourceInfo.isUseFkInitiallyDeferred());

                if (errMsg != null && errMsg.length() > 0) {

                    error("Could not create foreign keys for entity \"" + curEntity.getEntityName() + "\"", messages);
                    error(errMsg, messages);
                } else {

                    important("Created foreign keys for entity \"" + curEntity.getEntityName() + "\"", messages);
                }
            }
        }
        // for each newly added table, add fk indices
        if (datasourceInfo.isUseFkIndices()) {
            Iterator eaIter = entitiesAdded.iterator();

            while (eaIter.hasNext()) {
                ModelEntity curEntity = (ModelEntity) eaIter.next();
                String indErrMsg = this.createForeignKeyIndices(curEntity, datasourceInfo.getConstraintNameClipLength());

                if (indErrMsg != null && indErrMsg.length() > 0) {

                    error("Could not create foreign key indices for entity \"" + curEntity.getEntityName() + "\"", messages);
                    error(indErrMsg, messages);
                } else {

                    important("Created foreign key indices for entity \"" + curEntity.getEntityName() + "\"", messages);
                }
            }
        }

        if (datasourceInfo.isUseIndices()) {
            // for each newly added table, add declared indexes
            Iterator eaIter = entitiesAdded.iterator();

            while (eaIter.hasNext()) {
                ModelEntity curEntity = (ModelEntity) eaIter.next();
                String indErrMsg = this.createDeclaredIndices(curEntity);

                if (indErrMsg != null && indErrMsg.length() > 0) {
                    error("Could not create declared indices for entity \"" + curEntity.getEntityName() + "\"", messages);
                    error(indErrMsg, messages);
                } else {
                    important("Created declared indices for entity \"" + curEntity.getEntityName() + "\"", messages);
                }
            }

            createMissingIndices(existingTableEntities, messages);
        }

        // make sure each one-relation has an FK
        if (datasourceInfo.isUseFks() && datasourceInfo.isCheckForeignKeysOnStart()) {
            // NOTE: This ISN'T working for Postgres or MySQL, who knows about others, may be from JDBC driver bugs...
            int numFksCreated = 0;
            // TODO: check each key-map to make sure it exists in the FK, if any differences warn and then remove FK and recreate it

            // get ALL column info, put into hashmap by table name
            Map refTableInfoMap = this.getReferenceInfo(fkTableNames, messages);

            // Debug.logVerbose("Ref Info Map: " + refTableInfoMap);

            if (refTableInfoMap == null) {// uh oh, something happened while getting info...
            } else {
                Iterator refModelEntityIter = modelEntityList.iterator();

                while (refModelEntityIter.hasNext()) {
                    ModelEntity entity = (ModelEntity) refModelEntityIter.next();
                    String entityName = entity.getEntityName();

                    // if this is a view entity, do not check it...
                    if (entity instanceof ModelViewEntity) {

                        verbose("NOT Checking View Entity " + entity.getEntityName(), messages);
                        continue;
                    }

                    // get existing FK map for this table
                    Map rcInfoMap = (Map) refTableInfoMap.get(entity.getTableName(datasourceInfo));
                    // Debug.logVerbose("Got ref info for table " + entity.getTableName(datasourceInfo) + ": " + rcInfoMap);

                    // go through each relation to see if an FK already exists
                    Iterator relations = entity.getRelationsIterator();
                    boolean createdConstraints = false;

                    while (relations.hasNext()) {
                        ModelRelation modelRelation = (ModelRelation) relations.next();

                        if (!"one".equals(modelRelation.getType())) {
                            continue;
                        }

                        ModelEntity relModelEntity = (ModelEntity) modelEntities.get(modelRelation.getRelEntityName());

                        String relConstraintName = makeFkConstraintName(modelRelation, datasourceInfo.getConstraintNameClipLength());
                        ReferenceCheckInfo rcInfo = null;

                        if (rcInfoMap != null) {
                            rcInfo = (ReferenceCheckInfo) rcInfoMap.get(relConstraintName);
                        }

                        if (rcInfo != null) {
                            rcInfoMap.remove(relConstraintName);
                        } else {
                            // if not, create one
                            if (Debug.verboseOn()) Debug.logVerbose("No Foreign Key Constraint " + relConstraintName + " found in entity " + entityName);
                            String errMsg = createForeignKey(entity, modelRelation, relModelEntity, datasourceInfo.getConstraintNameClipLength(), datasourceInfo.getFkStyle(), datasourceInfo.isUseFkInitiallyDeferred());

                            if (errMsg != null && errMsg.length() > 0) {

                                error("Could not create foreign key " + relConstraintName + " for entity \"" + entity.getEntityName() + "\"", messages);
                                error(errMsg, messages);
                            } else {

                                verbose("Created foreign key " + relConstraintName + " for entity \"" + entity.getEntityName() + "\"", messages);

                                createdConstraints = true;
                                numFksCreated++;
                            }
                        }
                    }
                    if (createdConstraints) {

                        important("Created foreign key(s) for entity \"" + entity.getEntityName() + "\"", messages);
                    }

                    // show foreign key references that exist but are unknown
                    if (rcInfoMap != null) {
                        Iterator rcInfoKeysLeft = rcInfoMap.keySet().iterator();

                        while (rcInfoKeysLeft.hasNext()) {
                            String rcKeyLeft = (String) rcInfoKeysLeft.next();

                            Debug.logImportant("Unknown Foreign Key Constraint " + rcKeyLeft + " found in table " + entity.getTableName(datasourceInfo));
                        }
                    }
                }
            }
            if (Debug.infoOn()) Debug.logInfo("Created " + numFksCreated + " fk refs");
        }

        // make sure each one-relation has an index
        if (datasourceInfo.isUseFkIndices() && datasourceInfo.isCheckFkIndicesOnStart()) {
            int numIndicesCreated = 0;
            // TODO: check each key-map to make sure it exists in the index, if any differences warn and then remove the index and recreate it

            // TODO: also check the declared indices on start, if the datasourceInfo.checkIndicesOnStart flag is set

            // get ALL column info, put into hashmap by table name
            Map tableIndexListMap = this.getIndexInfo(indexTableNames, messages);

            // Debug.logVerbose("Ref Info Map: " + refTableInfoMap);

            if (tableIndexListMap == null) {// uh oh, something happened while getting info...
            } else {
                Iterator refModelEntityIter = modelEntityList.iterator();

                while (refModelEntityIter.hasNext()) {
                    ModelEntity entity = (ModelEntity) refModelEntityIter.next();
                    String entityName = entity.getEntityName();

                    // if this is a view entity, do not check it...
                    if (entity instanceof ModelViewEntity) {

                        verbose("NOT Checking View Entity " + entity.getEntityName(), messages);
                        continue;
                    }

                    // get existing index list for this table
                    TreeSet tableIndexList = (TreeSet) tableIndexListMap.get(entity.getTableName(datasourceInfo));

                    // Debug.logVerbose("Got ind info for table " + entity.getTableName(datasourceInfo) + ": " + tableIndexList);

                    if (tableIndexList == null) {
                        // evidently no indexes in the database for this table, do the create all
                        String indErrMsg = this.createForeignKeyIndices(entity, datasourceInfo.getConstraintNameClipLength());

                        if (indErrMsg != null && indErrMsg.length() > 0) {
                            String message = "Could not create foreign key indices for entity \"" + entity.getEntityName() + "\"";

                            error(message, messages);
                            error(indErrMsg, messages);
                        } else {
                            String message = "Created foreign key indices for entity \"" + entity.getEntityName() + "\"";

                            important(message, messages);
                        }
                    } else {
                        // go through each relation to see if an FK already exists
                        boolean createdConstraints = false;
                        Iterator relations = entity.getRelationsIterator();

                        while (relations.hasNext()) {
                            ModelRelation modelRelation = (ModelRelation) relations.next();

                            if (!"one".equals(modelRelation.getType())) {
                                continue;
                            }

                            String relConstraintName = makeFkConstraintName(modelRelation, datasourceInfo.getConstraintNameClipLength());

                            if (tableIndexList.contains(relConstraintName)) {
                                tableIndexList.remove(relConstraintName);
                            } else {
                                // if not, create one
                                if (Debug.verboseOn()) Debug.logVerbose("No Index " + relConstraintName + " found for entity " + entityName);
                                String errMsg = createForeignKeyIndex(entity, modelRelation, datasourceInfo.getConstraintNameClipLength());

                                if (errMsg != null && errMsg.length() > 0) {
                                    String message = "Could not create foreign key index " + relConstraintName + " for entity \"" + entity.getEntityName() + "\"";

                                    error(message, messages);
                                    error(errMsg, messages);
                                } else {
                                    String message = "Created foreign key index " + relConstraintName + " for entity \"" + entity.getEntityName() + "\"";

                                    verbose(message, messages);

                                    createdConstraints = true;
                                    numIndicesCreated++;
                                }
                            }
                        }
                        if (createdConstraints) {

                            important("Created foreign key index/indices for entity \"" + entity.getEntityName() + "\"", messages);
                        }
                    }

                    // show foreign key references that exist but are unknown
                    if (tableIndexList != null) {
                        Iterator tableIndexListIter = tableIndexList.iterator();

                        while (tableIndexListIter.hasNext()) {
                            String indexLeft = (String) tableIndexListIter.next();

                            Debug.logImportant("Unknown Index " + indexLeft + " found in table " + entity.getTableName(datasourceInfo));
                        }
                    }
                }
            }
            if (Debug.infoOn()) Debug.logInfo("Created " + numIndicesCreated + " indices");
        }

        timer.timerString("Finished Checking Entity Database");
    }

    /**
     * Checks the given {@link org.ofbiz.core.entity.model.ModelEntity entity's} @{link ModelField field} to see that
     * its type matches the given ColumnCheckInfo. Error messages are added to messages.
     */
    void checkFieldType(final ModelEntity entity, final ModelField field, final ColumnCheckInfo ccInfo, final Collection messages) {
        ModelFieldType modelFieldType = modelFieldTypeReader.getModelFieldType(field.getType());

        if (modelFieldType != null) {
            // make sure each corresponding column is of the correct type
            String fullTypeStr = modelFieldType.getSqlType();
            String typeName;
            int columnSize = -1;
            int decimalDigits = -1;

            int openParen = fullTypeStr.indexOf('(');
            int closeParen = fullTypeStr.indexOf(')');
            int comma = fullTypeStr.indexOf(',');

            if (openParen > 0 && closeParen > 0 && closeParen > openParen) {
                typeName = fullTypeStr.substring(0, openParen);
                if (comma > 0 && comma > openParen && comma < closeParen) {
                    String csStr = fullTypeStr.substring(openParen + 1, comma);

                    try {
                        columnSize = Integer.parseInt(csStr);
                    } catch (NumberFormatException e) {
                        Debug.logError(e, module);
                    }

                    String ddStr = fullTypeStr.substring(comma + 1, closeParen);

                    try {
                        decimalDigits = Integer.parseInt(ddStr);
                    } catch (NumberFormatException e) {
                        Debug.logError(e, module);
                    }
                } else {
                    String csStr = fullTypeStr.substring(openParen + 1, closeParen);

                    try {
                        columnSize = Integer.parseInt(csStr);
                    } catch (NumberFormatException e) {
                        Debug.logError(e, module);
                    }
                }
            } else {
                typeName = fullTypeStr;
            }

            // override the default typeName with the sqlTypeAlias if it is specified
            if (UtilValidate.isNotEmpty(modelFieldType.getSqlTypeAlias())) {
                typeName = modelFieldType.getSqlTypeAlias();
            }

            if (!ccInfo.typeName.equals(typeName.toUpperCase())) {
                String message = "WARNING: Column \"" + ccInfo.columnName + "\" of table \"" + entity.getTableName(datasourceInfo) + "\" of entity \"" +
                    entity.getEntityName() + "\" is of type \"" + ccInfo.typeName + "\" in the database, but is defined as type \"" +
                    typeName + "\" in the entity definition.";

                error(message, messages);
            }
            if (columnSize != -1 && ccInfo.columnSize != -1 && columnSize != ccInfo.columnSize) {
                String message = "WARNING: Column \"" + ccInfo.columnName + "\" of table \"" + entity.getTableName(datasourceInfo) + "\" of entity \"" +
                    entity.getEntityName() + "\" has a column size of \"" + ccInfo.columnSize +
                    "\" in the database, but is defined to have a column size of \"" + columnSize + "\" in the entity definition.";

                warn(message, messages);
            }
            if (decimalDigits != -1 && decimalDigits != ccInfo.decimalDigits) {
                String message = "WARNING: Column \"" + ccInfo.columnName + "\" of table \"" + entity.getTableName(datasourceInfo) + "\" of entity \"" +
                    entity.getEntityName() + "\" has a decimalDigits of \"" + ccInfo.decimalDigits +
                    "\" in the database, but is defined to have a decimalDigits of \"" + decimalDigits + "\" in the entity definition.";

                warn(message, messages);
            }
        } else {
            String message = "Column \"" + ccInfo.columnName + "\" of table \"" + entity.getTableName(datasourceInfo) + "\" of entity \"" + entity.getEntityName() +
                "\" has a field type name of \"" + field.getType() + "\" which is not found in the field type definitions";

            error(message, messages);
        }
    }

    /**
     * Add only the missing indexes for the given modelEntities, keyed by table name. The existence of an index is
     * determined soley by its name. If the {@link org.ofbiz.core.entity.model.ModelIndex} defines an index that has
     * the same name but different fields or unique flag as the one in the database, no action is taken to rectify the
     * difference.
     *
     * @param tableToModelEntities a map of table name to corresponding model entity.
     * @param messages error messages go here
     */
    void createMissingIndices(Map<String, ModelEntity> tableToModelEntities, Collection messages) {
        // get the actual db index names per table
        final Map<String, Set<String>> indexInfo = getIndexInfo(tableToModelEntities.keySet(), messages, true);

        for (Map.Entry<String, Set<String>> indexInfoEntry : indexInfo.entrySet()) {
            final String tableName = indexInfoEntry.getKey();
            final Set<String> actualIndexes = indexInfoEntry.getValue();

            final ModelEntity modelEntity = tableToModelEntities.get(tableName);
            final Iterator indexesIterator = modelEntity.getIndexesIterator();

            final StringBuffer retMsgsBuffer = new StringBuffer();

            while (indexesIterator.hasNext()) {
                ModelIndex modelIndex = (ModelIndex) indexesIterator.next();
                if (!actualIndexes.contains(modelIndex.getName())) {
                    if (Debug.infoOn()) {
                        Debug.logInfo("Missing index '" + modelIndex.getName() + "' on existing table '" + tableName + "' ...creating");
                    }
                    String retMsg = createDeclaredIndex(modelEntity, modelIndex);

                    if (retMsg != null && retMsg.length() > 0) {
                        if (retMsgsBuffer.length() > 0) {
                            retMsgsBuffer.append("\n");
                        }
                        retMsgsBuffer.append(retMsg);
                    }
                }
            }
            if (retMsgsBuffer.length() > 0) {
                error("Could not create missing indices for entity \"" + modelEntity.getEntityName() + "\"", messages);
                error(retMsgsBuffer.toString(), messages);
            }
        }
    }


    /** Creates a list of ModelEntity objects based on meta data from the database */
    public List induceModelFromDb(Collection messages) {
        // get ALL tables from this database
        TreeSet tableNames = this.getTableNames(messages);

        // get ALL column info, put into hashmap by table name
        Map colInfo = this.getColumnInfo(tableNames, messages);

        // go through each table and make a ModelEntity object, add to list
        // for each entity make corresponding ModelField objects
        // then print out XML for the entities/fields
        List newEntList = new LinkedList();

        // iterate over the table names is alphabetical order
        Iterator tableNamesIter = new TreeSet(colInfo.keySet()).iterator();

        while (tableNamesIter.hasNext()) {
            String tableName = (String) tableNamesIter.next();
            List colList = (ArrayList) colInfo.get(tableName);

            ModelEntity newEntity = new ModelEntity(tableName, colList, modelFieldTypeReader);

            newEntList.add(newEntity);
        }

        return newEntList;
    }

    public TreeSet getTableNames(Collection messages) {
        Connection connection = null;

        try {
            connection = getConnection();
        } catch (SQLException sqle) {

            error("Unable to esablish a connection with the database... Error was:" + sqle.toString(), messages);
            return null;
        } catch (GenericEntityException e) {

            error("Unable to esablish a connection with the database... Error was:" + e.toString(), messages);
            return null;
        }

        if (connection == null) {
            error("Unable to esablish a connection with the database, no additional information available.", messages);
            return null;
        }

        DatabaseMetaData dbData = null;

        try {
            dbData = connection.getMetaData();
        } catch (SQLException sqle) {

            error("Unable to get database meta data... Error was:" + sqle.toString(), messages);
            return null;
        }

        if (dbData == null) {
            Debug.logWarning("Unable to get database meta data; method returned null", module);
        }

        logDbInfo(dbData);

        if (Debug.infoOn()) Debug.logInfo("Getting Table Info From Database");

        // get ALL tables from this database
        TreeSet tableNames = new TreeSet();
        ResultSet tableSet = null;

        try {
            String[] types = {"TABLE", "VIEW", "ALIAS", "SYNONYM"};
            String lookupSchemaName = lookupSchemaName(dbData);
            tableSet = dbData.getTables(null, lookupSchemaName, null, types);
            if (tableSet == null) {
                Debug.logWarning("getTables returned null set", module);
            }
        } catch (SQLException sqle) {

            error("Unable to get list of table information, let's try the create anyway... Error was:" + sqle.toString(), messages);

            cleanup(connection, messages);
            // we are returning an empty set here because databases like SapDB throw an exception when there are no tables in the database
            return tableNames;
        }

        try {
            while (tableSet.next()) {
                try {
                    String tableName = tableSet.getString("TABLE_NAME");

                    tableName = (tableName == null) ? null : tableName.toUpperCase();

					// Atlassian Modification - Ensure that The code works with Postgress 7.3 and up which have schema support
					// but do not return the table name as schema_name.table_name but simply as table_name at all times
                    tableName = convertToSchemaTableName(tableName, dbData);

                    String tableType = tableSet.getString("TABLE_TYPE");

                    tableType = (tableType == null) ? null : tableType.toUpperCase();
                    // only allow certain table types
                    if (tableType != null && !"TABLE".equals(tableType) && !"VIEW".equals(tableType) && !"ALIAS".equals(tableType) && !"SYNONYM".equals(tableType))
                        continue;

                    // String remarks = tableSet.getString("REMARKS");
                    tableNames.add(tableName);
                    // if (Debug.infoOn()) Debug.logInfo("Found table named \"" + tableName + "\" of type \"" + tableType + "\" with remarks: " + remarks);
                } catch (SQLException sqle) {

                    error("Error getting table information... Error was:" + sqle.toString(), messages);
                    continue;
                }
            }
        } catch (SQLException sqle) {
            error("Error getting next table information... Error was:" + sqle.toString(), messages);
        } finally {
            try {
                tableSet.close();
            } catch (SQLException sqle) {
                error("Unable to close ResultSet for table list, continuing anyway... Error was:" + sqle.toString(), messages);
            }

            cleanup(connection, messages);
        }
        return tableNames;
    }

    private String lookupSchemaName(final DatabaseMetaData dbData) throws SQLException {
        String lookupSchemaName = null;
        if (dbData.supportsSchemasInTableDefinitions()) {
            if (this.datasourceInfo.getSchemaName() != null && this.datasourceInfo.getSchemaName().length() > 0) {
                lookupSchemaName = this.datasourceInfo.getSchemaName();
            } else {
                lookupSchemaName = dbData.getUserName();
            }
        }
        return lookupSchemaName;
    }

    public Map getColumnInfo(Set tableNames, Collection messages) {
        // if there are no tableNames, don't even try to get the columns
        if (tableNames.size() == 0) {
            return new HashMap();
        }

        Connection connection = null;

        try {
            connection = getConnection();
        } catch (SQLException sqle) {

            error("Unable to esablish a connection with the database... Error was:" + sqle.toString(), messages);
            return null;
        } catch (GenericEntityException e) {

            error("Unable to esablish a connection with the database... Error was:" + e.toString(), messages);
            return null;
        }

        DatabaseMetaData dbData = null;

        try {
            dbData = connection.getMetaData();
        } catch (SQLException sqle) {

            error("Unable to get database meta data... Error was:" + sqle.toString(), messages);

            cleanup(connection, messages);
            return null;
        }

        /*
         try {
         if (Debug.infoOn()) Debug.logInfo("Database Product Name is " + dbData.getDatabaseProductName(), module);
         if (Debug.infoOn()) Debug.logInfo("Database Product Version is " + dbData.getDatabaseProductVersion(), module);
         } catch (SQLException sqle) {
         Debug.logWarning("Unable to get Database name & version information", module);
         }
         try {
         if (Debug.infoOn()) Debug.logInfo("Database Driver Name is " + dbData.getDriverName(), module);
         if (Debug.infoOn()) Debug.logInfo("Database Driver Version is " + dbData.getDriverVersion(), module);
         } catch (SQLException sqle) {
         Debug.logWarning("Unable to get Driver name & version information", module);
         }
         */

        if (Debug.infoOn()) Debug.logInfo("Getting Column Info From Database");

        Map colInfo = new HashMap();

        try {
            String lookupSchemaName = lookupSchemaName(dbData);

            ResultSet rsCols = dbData.getColumns(null, lookupSchemaName, null, null);
            while (rsCols.next()) {
                try {
                    ColumnCheckInfo ccInfo = new ColumnCheckInfo();

                    ccInfo.tableName = rsCols.getString("TABLE_NAME");
                    ccInfo.tableName = (ccInfo.tableName == null) ? null : ccInfo.tableName.toUpperCase();

                    // Atlassian Modification - Ensure that the code works with PostgreSQL 7.3 and up which has schema support
                    // but does not return the table name as schema_name.table_name but simply as table_name at all times
                    ccInfo.tableName = convertToSchemaTableName(ccInfo.tableName, dbData);

                    // ignore the column info if the table name is not in the list we are concerned with
                    if (!tableNames.contains(ccInfo.tableName))
                        continue;

                    ccInfo.columnName = rsCols.getString("COLUMN_NAME");
                    ccInfo.columnName = (ccInfo.columnName == null) ? null : ccInfo.columnName.toUpperCase();

                    ccInfo.typeName = rsCols.getString("TYPE_NAME");
                    ccInfo.typeName = (ccInfo.typeName == null) ? null : ccInfo.typeName.toUpperCase();
                    ccInfo.columnSize = rsCols.getInt("COLUMN_SIZE");
                    ccInfo.decimalDigits = rsCols.getInt("DECIMAL_DIGITS");

                    ccInfo.isNullable = rsCols.getString("IS_NULLABLE");
                    ccInfo.isNullable = (ccInfo.isNullable == null) ? null : ccInfo.isNullable.toUpperCase();

                    List tableColInfo = (List) colInfo.get(ccInfo.tableName);

                    if (tableColInfo == null) {
                        tableColInfo = new ArrayList();
                        colInfo.put(ccInfo.tableName, tableColInfo);
                    }
                    tableColInfo.add(ccInfo);
                } catch (SQLException sqle) {

                    error("Error getting column info for column. Error was:" + sqle.toString(), messages);
                    continue;
                }
            }

            try {
                rsCols.close();
            } catch (SQLException sqle) {

                error("Unable to close ResultSet for column list, continuing anyway... Error was:" + sqle.toString(), messages);
            }
        } catch (SQLException sqle) {

            error("Error getting column meta data for Error was:" + sqle.toString() + ". Not checking columns.", messages);
            // we are returning an empty set in this case because databases like SapDB throw an exception when there are no tables in the database
            // colInfo = null;
        } finally {
            cleanup(connection, messages);
        }
        return colInfo;
    }

    public Map getReferenceInfo(Set tableNames, Collection messages) {
        Connection connection = null;
        try {
            connection = getConnection();
        } catch (SQLException sqle) {

            error("Unable to esablish a connection with the database... Error was:" + sqle.toString(), messages);
            return null;
        } catch (GenericEntityException e) {

            error("Unable to esablish a connection with the database... Error was:" + e.toString(), messages);
            return null;
        }

        DatabaseMetaData dbData = null;
        try {
            dbData = connection.getMetaData();
        } catch (SQLException sqle) {

            error("Unable to get database meta data... Error was:" + sqle.toString(), messages);

            cleanup(connection, messages);
            return null;
        }

        /*
         try {
         if (Debug.infoOn()) Debug.logInfo("Database Product Name is " + dbData.getDatabaseProductName(), module);
         if (Debug.infoOn()) Debug.logInfo("Database Product Version is " + dbData.getDatabaseProductVersion(), module);
         } catch (SQLException sqle) {
         Debug.logWarning("Unable to get Database name & version information", module);
         }
         try {
         if (Debug.infoOn()) Debug.logInfo("Database Driver Name is " + dbData.getDriverName(), module);
         if (Debug.infoOn()) Debug.logInfo("Database Driver Version is " + dbData.getDriverVersion(), module);
         } catch (SQLException sqle) {
         Debug.logWarning("Unable to get Driver name & version information", module);
         }
         */

        if (Debug.infoOn()) Debug.logInfo("Getting Foreign Key (Reference) Info From Database");

        Map refInfo = new HashMap();

        try {
            // ResultSet rsCols = dbData.getCrossReference(null, null, null, null, null, null);
            String lookupSchemaName = lookupSchemaName(dbData);

            ResultSet rsCols = dbData.getImportedKeys(null, lookupSchemaName, null);
            int totalFkRefs = 0;

            // Iterator tableNamesIter = tableNames.iterator();
            // while (tableNamesIter.hasNext()) {
            // String tableName = (String) tableNamesIter.next();
            // ResultSet rsCols = dbData.getImportedKeys(null, null, tableName);
            // Debug.logVerbose("Getting imported keys for table " + tableName);

            while (rsCols.next()) {
                try {
                    ReferenceCheckInfo rcInfo = new ReferenceCheckInfo();

                    rcInfo.pkTableName = rsCols.getString("PKTABLE_NAME");
                    rcInfo.pkTableName = (rcInfo.pkTableName == null) ? null : rcInfo.pkTableName.toUpperCase();
                    rcInfo.pkColumnName = rsCols.getString("PKCOLUMN_NAME");
                    rcInfo.pkColumnName = (rcInfo.pkColumnName == null) ? null : rcInfo.pkColumnName.toUpperCase();

                    rcInfo.fkTableName = rsCols.getString("FKTABLE_NAME");
                    rcInfo.fkTableName = (rcInfo.fkTableName == null) ? null : rcInfo.fkTableName.toUpperCase();
                    // ignore the column info if the FK table name is not in the list we are concerned with
                    if (!tableNames.contains(rcInfo.fkTableName))
                        continue;
                    rcInfo.fkColumnName = rsCols.getString("FKCOLUMN_NAME");
                    rcInfo.fkColumnName = (rcInfo.fkColumnName == null) ? null : rcInfo.fkColumnName.toUpperCase();

                    rcInfo.fkName = rsCols.getString("FK_NAME");
                    rcInfo.fkName = (rcInfo.fkName == null) ? null : rcInfo.fkName.toUpperCase();

                    if (Debug.verboseOn()) Debug.logVerbose("Got: " + rcInfo.toString());

                    Map tableRefInfo = (Map) refInfo.get(rcInfo.fkTableName);

                    if (tableRefInfo == null) {
                        tableRefInfo = new HashMap();
                        refInfo.put(rcInfo.fkTableName, tableRefInfo);
                        if (Debug.verboseOn()) Debug.logVerbose("Adding new Map for table: " + rcInfo.fkTableName);
                    }
                    if (!tableRefInfo.containsKey(rcInfo.fkName)) totalFkRefs++;
                    tableRefInfo.put(rcInfo.fkName, rcInfo);
                } catch (SQLException sqle) {

                    error("Error getting fk reference info for table. Error was:" + sqle.toString(), messages);
                    continue;
                }
            }

            // if (Debug.infoOn()) Debug.logInfo("There are " + totalFkRefs + " in the database");
            try {
                rsCols.close();
            } catch (SQLException sqle) {

                error("Unable to close ResultSet for fk reference list, continuing anyway... Error was:" + sqle.toString(), messages);
            }
            // }
            if (Debug.infoOn()) Debug.logInfo("There are " + totalFkRefs + " foreign key refs in the database");

        } catch (SQLException sqle) {

            error("Error getting fk reference meta data Error was:" + sqle.toString() + ". Not checking fk refs.", messages);
            refInfo = null;
        } finally {
            cleanup(connection, messages);
        }
        return refInfo;
    }

    public Map getIndexInfo(Set tableNames, Collection messages) {

        // preserving backwards compatibility of this method while providing a new version that can
        // return unique indexes.

        // HACK: for now skip all "unique" indexes since our foreign key indices are not unique, but the primary key ones are
        return getIndexInfo(tableNames, messages, false);
    }

    /**
     * Gets index information from the database for the given table names only, optionally including unique indexes.
     *
     * @param tableNames the names of tables to get indexes for.
     * @param messages a collector of errors.
     * @param includeUnique if true, the index info will include unique indexes which could include pk indexes.
     * @return a map of table names to sets of index names or null on failure.
     */
    Map getIndexInfo(Set tableNames, Collection messages, boolean includeUnique) {

        Connection connection = null;

        try {
            connection = getConnection();
        } catch (SQLException sqle) {
            error("Unable to esablish a connection with the database... Error was:" + sqle.toString(), messages);
            return null;
        } catch (GenericEntityException e) {
            error("Unable to esablish a connection with the database... Error was:" + e.toString(), messages);
            return null;
        }

        DatabaseMetaData dbData = null;

        try {
            dbData = connection.getMetaData();
        } catch (SQLException sqle) {
            error("Unable to get database meta data... Error was:" + sqle.toString(), messages);
            cleanup(connection, messages);
            return null;
        }

        if (Debug.infoOn()) Debug.logInfo("Getting Index Info From Database");

        Map<String, TreeSet<String>> indexInfo = new HashMap<String, TreeSet<String>>();

        try {
            int totalIndices = 0;
            Iterator tableNamesIter = tableNames.iterator();

            String lookupSchemaName = lookupSchemaName(dbData);

            while (tableNamesIter.hasNext()) {
                String curTableName = (String) tableNamesIter.next();

                ResultSet rsCols = null;
                try {
                    // true for approximate, don't really care if stats are up-to-date
                    rsCols = dbData.getIndexInfo(null, lookupSchemaName, curTableName, false, true);
                } catch (Exception e) {
                    Debug.logWarning(e, "Error getting index info for table: " + curTableName + " using lookupSchemaName " + lookupSchemaName);
                }

                while (rsCols != null && rsCols.next()) {
                    // NOTE: The code in this block may look funny, but it is designed so that the wrapping loop can be removed
                    try {
                        // skip all index info for statistics
                        if (rsCols.getShort("TYPE") == DatabaseMetaData.tableIndexStatistic) continue;

                        if (!includeUnique && !rsCols.getBoolean("NON_UNIQUE")) continue;

                        String tableName = rsCols.getString("TABLE_NAME");

                        tableName = (tableName == null) ? null : tableName.toUpperCase();
                        if (!tableNames.contains(tableName)) continue;

                        String indexName = rsCols.getString("INDEX_NAME");

                        indexName = (indexName == null) ? null : indexName.toUpperCase();

                        TreeSet<String> tableIndexList = indexInfo.get(tableName);

                        if (tableIndexList == null) {
                            tableIndexList = new TreeSet<String>();
                            indexInfo.put(tableName, tableIndexList);
                            if (Debug.verboseOn()) Debug.logVerbose("Adding new Map for table: " + tableName);
                        }
                        if (!tableIndexList.contains(indexName)) totalIndices++;
                        tableIndexList.add(indexName);
                    } catch (SQLException sqle) {

                        error("Error getting fk reference info for table. Error was:" + sqle.toString(), messages);
                        continue;
                    }
                }

                // if (Debug.infoOn()) Debug.logInfo("There are " + totalIndices + " indices in the database");
                if (rsCols != null) {
                    try {
                        rsCols.close();
                    } catch (SQLException sqle) {

                        error("Unable to close ResultSet for fk reference list, continuing anyway... Error was:" + sqle.toString(), messages);
                    }
                }
            }
            if (Debug.infoOn()) Debug.logInfo("There are " + totalIndices + " indices in the database");

        } catch (SQLException sqle) {

            error("Error getting fk reference meta data Error was:" + sqle.toString() + ". Not checking fk refs.", messages);
            indexInfo = null;
        } finally {
            cleanup(connection, messages);
        }
        return indexInfo;
    }

    /* ====================================================================== */

    /* ====================================================================== */
    public String createTable(ModelEntity entity, Map modelEntities, boolean addFks, boolean usePkConstraintNames, int constraintNameClipLength, String fkStyle, boolean useFkInitiallyDeferred) {
        if (entity == null) {
            return "ModelEntity was null and is required to create a table";
        }
        if (entity instanceof ModelViewEntity) {
            return "ERROR: Cannot create table for a view entity";
        }

        Connection connection = null;
        Statement stmt = null;

        try {
            connection = getConnection();
        } catch (SQLException sqle) {
            return "Unable to esablish a connection with the database... Error was: " + sqle.toString();
        } catch (GenericEntityException e) {
            return "Unable to esablish a connection with the database... Error was: " + e.toString();
        }

        StringBuffer sqlBuf = new StringBuffer("CREATE TABLE ");
        sqlBuf.append(entity.getTableName(datasourceInfo));
        sqlBuf.append(" (");
        for (int i = 0; i < entity.getFieldsSize(); i++) {
            ModelField field = entity.getField(i);
            ModelFieldType type = modelFieldTypeReader.getModelFieldType(field.getType());

            if (type == null) {
                return "Field type [" + type + "] not found for field [" + field.getName() + "] of entity [" + entity.getEntityName() + "], not creating table.";
            }

            sqlBuf.append(field.getColName());
            sqlBuf.append(" ");
            sqlBuf.append(type.getSqlType());
            if (field.getIsPk()) {
                sqlBuf.append(" NOT NULL, ");
            } else {
                sqlBuf.append(", ");
            }
        }
        String pkName = "PK_" + entity.getPlainTableName();

        if (pkName.length() > constraintNameClipLength) {
            pkName = pkName.substring(0, constraintNameClipLength);
        }

        if (usePkConstraintNames) {
            sqlBuf.append("CONSTRAINT ");
            sqlBuf.append(pkName);
        }
        sqlBuf.append(" PRIMARY KEY (");
        sqlBuf.append(entity.colNameString(entity.getPksCopy()));
        sqlBuf.append(")");

        if (addFks) {
            // NOTE: This is kind of a bad idea anyway since ordering table creations is crazy, if not impossible

            // go through the relationships to see if any foreign keys need to be added
            Iterator relationsIter = entity.getRelationsIterator();

            while (relationsIter.hasNext()) {
                ModelRelation modelRelation = (ModelRelation) relationsIter.next();

                if ("one".equals(modelRelation.getType())) {
                    ModelEntity relModelEntity = (ModelEntity) modelEntities.get(modelRelation.getRelEntityName());

                    if (relModelEntity == null) {
                        Debug.logError("Error adding foreign key: ModelEntity was null for related entity name " + modelRelation.getRelEntityName());
                        continue;
                    }
                    if (relModelEntity instanceof ModelViewEntity) {
                        Debug.logError("Error adding foreign key: related entity is a view entity for related entity name " + modelRelation.getRelEntityName());
                        continue;
                    }

                    sqlBuf.append(", ");
                    sqlBuf.append(makeFkConstraintClause(entity, modelRelation, relModelEntity, constraintNameClipLength, fkStyle, useFkInitiallyDeferred));
                }
            }
        }

        sqlBuf.append(")");
        if (Debug.verboseOn()) Debug.logVerbose("[createTable] sql=" + sqlBuf.toString());
        try {
            stmt = connection.createStatement();
            stmt.executeUpdate(sqlBuf.toString());
        } catch (SQLException sqle) {
            return "SQL Exception while executing the following:\n" + sqlBuf.toString() + "\nError was: " + sqle.toString();
        } finally {
            cleanup(connection, stmt);
        }
        return null;
    }

    public String addColumn(ModelEntity entity, ModelField field) {
        if (entity == null || field == null)
            return "ModelEntity or ModelField where null, cannot add column";
        if (entity instanceof ModelViewEntity) {
            return "ERROR: Cannot add column for a view entity";
        }

        Connection connection = null;
        Statement stmt = null;

        try {
            connection = getConnection();
        } catch (SQLException sqle) {
            return "Unable to esablish a connection with the database... Error was: " + sqle.toString();
        } catch (GenericEntityException e) {
            return "Unable to esablish a connection with the database... Error was: " + e.toString();
        }

        ModelFieldType type = modelFieldTypeReader.getModelFieldType(field.getType());

        if (type == null) {
            return "Field type [" + type + "] not found for field [" + field.getName() + "] of entity [" + entity.getEntityName() + "], not adding column.";
        }

        StringBuffer sqlBuf = new StringBuffer("ALTER TABLE ");
        sqlBuf.append(entity.getTableName(datasourceInfo));
        sqlBuf.append(" ADD ");
        sqlBuf.append(field.getColName());
        sqlBuf.append(" ");
        sqlBuf.append(type.getSqlType());

        String sql = sqlBuf.toString();
        if (Debug.infoOn()) Debug.logInfo("[addColumn] sql=" + sql);
        try {
            stmt = connection.createStatement();
            stmt.executeUpdate(sql);
        } catch (SQLException sqle) {
            // if that failed try the alternate syntax real quick
            String sql2 = "ALTER TABLE " + entity.getTableName(datasourceInfo) + " ADD COLUMN " + field.getColName() + " " + type.getSqlType();
            if (Debug.infoOn()) Debug.logInfo("[addColumn] sql failed, trying sql2=" + sql2);
            try {
                stmt = connection.createStatement();
                stmt.executeUpdate(sql2);
            } catch (SQLException sqle2) {
                // if this also fails report original error, not this error...
                return "SQL Exception while executing the following:\n" + sql + "\nError was: " + sqle.toString();
            }
        } finally {
            cleanup(connection, stmt);
        }
        return null;
    }

    /* ====================================================================== */

    /* ====================================================================== */
    public String makeFkConstraintName(ModelRelation modelRelation, int constraintNameClipLength) {
        String relConstraintName = modelRelation.getFkName();

        if (relConstraintName == null || relConstraintName.length() == 0) {
            relConstraintName = modelRelation.getTitle() + modelRelation.getRelEntityName();
            relConstraintName = relConstraintName.toUpperCase();
        }

        if (relConstraintName.length() > constraintNameClipLength) {
            relConstraintName = relConstraintName.substring(0, constraintNameClipLength);
        }

        return relConstraintName;
    }

    /* ====================================================================== */

    /* ====================================================================== */
    public String createForeignKeys(ModelEntity entity, Map modelEntities, int constraintNameClipLength, String fkStyle, boolean useFkInitiallyDeferred) {
        if (entity == null) {
            return "ModelEntity was null and is required to create foreign keys for a table";
        }
        if (entity instanceof ModelViewEntity) {
            return "ERROR: Cannot create foreign keys for a view entity";
        }

        StringBuffer retMsgsBuffer = new StringBuffer();

        // go through the relationships to see if any foreign keys need to be added
        Iterator relationsIter = entity.getRelationsIterator();

        while (relationsIter.hasNext()) {
            ModelRelation modelRelation = (ModelRelation) relationsIter.next();

            if ("one".equals(modelRelation.getType())) {
                ModelEntity relModelEntity = (ModelEntity) modelEntities.get(modelRelation.getRelEntityName());

                if (relModelEntity == null) {
                    Debug.logError("Error adding foreign key: ModelEntity was null for related entity name " + modelRelation.getRelEntityName());
                    continue;
                }
                if (relModelEntity instanceof ModelViewEntity) {
                    Debug.logError("Error adding foreign key: related entity is a view entity for related entity name " + modelRelation.getRelEntityName());
                    continue;
                }

                String retMsg = createForeignKey(entity, modelRelation, relModelEntity, constraintNameClipLength, fkStyle, useFkInitiallyDeferred);

                if (retMsg != null && retMsg.length() > 0) {
                    if (retMsgsBuffer.length() > 0) {
                        retMsgsBuffer.append("\n");
                    }
                    retMsgsBuffer.append(retMsg);
                }
            }
        }
        if (retMsgsBuffer.length() > 0) {
            return retMsgsBuffer.toString();
        } else {
            return null;
        }
    }

    public String createForeignKey(ModelEntity entity, ModelRelation modelRelation, ModelEntity relModelEntity, int constraintNameClipLength, String fkStyle, boolean useFkInitiallyDeferred) {
        Connection connection = null;
        Statement stmt = null;

        try {
            connection = getConnection();
        } catch (SQLException sqle) {
            return "Unable to esablish a connection with the database... Error was: " + sqle.toString();
        } catch (GenericEntityException e) {
            return "Unable to esablish a connection with the database... Error was: " + e.toString();
        }

        // now add constraint clause
        StringBuffer sqlBuf = new StringBuffer("ALTER TABLE ");
        sqlBuf.append(entity.getTableName(datasourceInfo));
        sqlBuf.append(" ADD ");
        sqlBuf.append(makeFkConstraintClause(entity, modelRelation, relModelEntity, constraintNameClipLength, fkStyle, useFkInitiallyDeferred));

        if (Debug.verboseOn()) Debug.logVerbose("[createForeignKey] sql=" + sqlBuf.toString());
        try {
            stmt = connection.createStatement();
            stmt.executeUpdate(sqlBuf.toString());
        } catch (SQLException sqle) {
            return "SQL Exception while executing the following:\n" + sqlBuf.toString() + "\nError was: " + sqle.toString();
        } finally {
            cleanup(connection, stmt);
        }
        return null;
    }

    public String makeFkConstraintClause(ModelEntity entity, ModelRelation modelRelation, ModelEntity relModelEntity, int constraintNameClipLength, String fkStyle, boolean useFkInitiallyDeferred) {
        // make the two column lists
        Iterator keyMapsIter = modelRelation.getKeyMapsIterator();
        StringBuffer mainCols = new StringBuffer();
        StringBuffer relCols = new StringBuffer();

        while (keyMapsIter.hasNext()) {
            ModelKeyMap keyMap = (ModelKeyMap) keyMapsIter.next();

            ModelField mainField = entity.getField(keyMap.getFieldName());

            if (mainCols.length() > 0) {
                mainCols.append(", ");
            }
            mainCols.append(mainField.getColName());

            ModelField relField = relModelEntity.getField(keyMap.getRelFieldName());

            if (relCols.length() > 0) {
                relCols.append(", ");
            }
            relCols.append(relField.getColName());
        }

        StringBuffer sqlBuf = new StringBuffer("");

        if ("name_constraint".equals(fkStyle)) {
            sqlBuf.append("CONSTRAINT ");
            String relConstraintName = makeFkConstraintName(modelRelation, constraintNameClipLength);

            sqlBuf.append(relConstraintName);

            sqlBuf.append(" FOREIGN KEY (");
            sqlBuf.append(mainCols.toString());
            sqlBuf.append(") REFERENCES ");
            sqlBuf.append(relModelEntity.getTableName(datasourceInfo));
            sqlBuf.append(" (");
            sqlBuf.append(relCols.toString());
            sqlBuf.append(")");
            if (useFkInitiallyDeferred) {
                sqlBuf.append(" INITIALLY DEFERRED");
            }
        } else if ("name_fk".equals(fkStyle)) {
            sqlBuf.append(" FOREIGN KEY ");
            String relConstraintName = makeFkConstraintName(modelRelation, constraintNameClipLength);

            sqlBuf.append(relConstraintName);
            sqlBuf.append(" (");
            sqlBuf.append(mainCols.toString());
            sqlBuf.append(") REFERENCES ");
            sqlBuf.append(relModelEntity.getTableName(datasourceInfo));
            sqlBuf.append(" (");
            sqlBuf.append(relCols.toString());
            sqlBuf.append(")");
            if (useFkInitiallyDeferred) {
                sqlBuf.append(" INITIALLY DEFERRED");
            }
        } else {
            String emsg = "ERROR: fk-style specified for this data-source is not valid: " + fkStyle;

            Debug.logError(emsg);
            throw new IllegalArgumentException(emsg);
        }

        return sqlBuf.toString();
    }

    public String deleteForeignKeys(ModelEntity entity, Map modelEntities, int constraintNameClipLength) {
        if (entity == null) {
            return "ModelEntity was null and is required to delete foreign keys for a table";
        }
        if (entity instanceof ModelViewEntity) {
            return "ERROR: Cannot delete foreign keys for a view entity";
        }

        // go through the relationships to see if any foreign keys need to be added
        Iterator relationsIter = entity.getRelationsIterator();
        StringBuffer retMsgsBuffer = new StringBuffer();

        while (relationsIter.hasNext()) {
            ModelRelation modelRelation = (ModelRelation) relationsIter.next();

            if ("one".equals(modelRelation.getType())) {
                ModelEntity relModelEntity = (ModelEntity) modelEntities.get(modelRelation.getRelEntityName());

                if (relModelEntity == null) {
                    Debug.logError("Error removing foreign key: ModelEntity was null for related entity name " + modelRelation.getRelEntityName());
                    continue;
                }
                if (relModelEntity instanceof ModelViewEntity) {
                    Debug.logError("Error removing foreign key: related entity is a view entity for related entity name " + modelRelation.getRelEntityName());
                    continue;
                }

                String retMsg = deleteForeignKey(entity, modelRelation, relModelEntity, constraintNameClipLength);

                if (retMsg != null && retMsg.length() > 0) {
                    if (retMsgsBuffer.length() > 0) {
                        retMsgsBuffer.append("\n");
                    }
                    retMsgsBuffer.append(retMsg);
                }
            }
        }
        if (retMsgsBuffer.length() > 0) {
            return retMsgsBuffer.toString();
        } else {
            return null;
        }
    }

    public String deleteForeignKey(ModelEntity entity, ModelRelation modelRelation, ModelEntity relModelEntity, int constraintNameClipLength) {
        Connection connection = null;
        Statement stmt = null;

        try {
            connection = getConnection();
        } catch (SQLException sqle) {
            return "Unable to esablish a connection with the database... Error was: " + sqle.toString();
        } catch (GenericEntityException e) {
            return "Unable to esablish a connection with the database... Error was: " + e.toString();
        }

        String relConstraintName = makeFkConstraintName(modelRelation, constraintNameClipLength);

        // now add constraint clause
        StringBuffer sqlBuf = new StringBuffer("ALTER TABLE ");
        sqlBuf.append(entity.getTableName(datasourceInfo));
        sqlBuf.append(" DROP CONSTRAINT ");
        sqlBuf.append(relConstraintName);

        if (Debug.verboseOn()) Debug.logVerbose("[deleteForeignKey] sql=" + sqlBuf.toString());
        try {
            stmt = connection.createStatement();
            stmt.executeUpdate(sqlBuf.toString());
        } catch (SQLException sqle) {
            return "SQL Exception while executing the following:\n" + sqlBuf.toString() + "\nError was: " + sqle.toString();
        } finally {
            cleanup(connection, stmt);
        }
        return null;
    }

    /**
     * Creates a database index for every declared index on the given entity.
     * @param entity
     * @return an error message if there is an error, or null if it worked.
     */
    public String createDeclaredIndices(ModelEntity entity) {
        if (entity == null) {
            return "ModelEntity was null and is required to create declared indices for a table";
        }
        if (entity instanceof ModelViewEntity) {
            return "ERROR: Cannot create declared indices for a view entity";
        }

        StringBuffer retMsgsBuffer = new StringBuffer();

        // go through the indexes to see if any need to be added
        Iterator indexesIter = entity.getIndexesIterator();
        while (indexesIter.hasNext()) {
            ModelIndex modelIndex = (ModelIndex) indexesIter.next();

            String retMsg = createDeclaredIndex(entity, modelIndex);

            if (retMsg != null && retMsg.length() > 0) {
                if (retMsgsBuffer.length() > 0) {
                    retMsgsBuffer.append("\n");
                }
                retMsgsBuffer.append(retMsg);
            }
        }

        if (retMsgsBuffer.length() > 0) {
            return retMsgsBuffer.toString();
        } else {
            return null;
        }
    }

    public String createDeclaredIndex(ModelEntity entity, ModelIndex modelIndex) {
        Connection connection = null;
        Statement stmt = null;

        try {
            connection = getConnection();
        } catch (SQLException sqle) {
            return "Unable to esablish a connection with the database... Error was: " + sqle.toString();
        } catch (GenericEntityException e) {
            return "Unable to esablish a connection with the database... Error was: " + e.toString();
        }

        String createIndexSql = makeIndexClause(entity, modelIndex);
        if (Debug.verboseOn()) Debug.logVerbose("[createForeignKeyIndex] index sql=" + createIndexSql);

        try {
            stmt = connection.createStatement();
            stmt.executeUpdate(createIndexSql);
        } catch (SQLException sqle) {
            return "SQL Exception while executing the following:\n" + createIndexSql + "\nError was: " + sqle.toString();
        } finally {
            cleanup(connection, stmt);
        }
        return null;
    }

    public String makeIndexClause(ModelEntity entity, ModelIndex modelIndex) {
        Iterator fieldNamesIter = modelIndex.getIndexFieldsIterator();
        StringBuffer mainCols = new StringBuffer();

        while (fieldNamesIter.hasNext()) {
            String fieldName = (String) fieldNamesIter.next();
            ModelField mainField = entity.getField(fieldName);
            if (mainCols.length() > 0) {
                mainCols.append(", ");
            }
            mainCols.append(mainField.getColName());
        }

        StringBuffer indexSqlBuf = new StringBuffer("CREATE ");
        if (modelIndex.getUnique()) {
            indexSqlBuf.append("UNIQUE ");
        }
        indexSqlBuf.append("INDEX ");
        indexSqlBuf.append(modelIndex.getName());
        indexSqlBuf.append(" ON ");
        indexSqlBuf.append(entity.getTableName(datasourceInfo));

        indexSqlBuf.append(" (");
        indexSqlBuf.append(mainCols.toString());
        indexSqlBuf.append(")");

        return indexSqlBuf.toString();
    }

    public String deleteDeclaredIndices(ModelEntity entity) {
        if (entity == null) {
            return "ModelEntity was null and is required to delete foreign keys indices for a table";
        }
        if (entity instanceof ModelViewEntity) {
            return "ERROR: Cannot delete foreign keys indices for a view entity";
        }

        StringBuffer retMsgsBuffer = new StringBuffer();

        // go through the relationships to see if any foreign keys need to be added
        Iterator indexesIter = entity.getIndexesIterator();
        while (indexesIter.hasNext()) {
            ModelIndex modelIndex = (ModelIndex) indexesIter.next();
            String retMsg = deleteDeclaredIndex(entity, modelIndex);
            if (retMsg != null && retMsg.length() > 0) {
                if (retMsgsBuffer.length() > 0) {
                    retMsgsBuffer.append("\n");
                }
                retMsgsBuffer.append(retMsg);
            }
        }

        if (retMsgsBuffer.length() > 0) {
            return retMsgsBuffer.toString();
        } else {
            return null;
        }
    }

    public String deleteDeclaredIndex(ModelEntity entity, ModelIndex modelIndex) {
        Connection connection = null;
        Statement stmt = null;

        try {
            connection = getConnection();
        } catch (SQLException sqle) {
            return "Unable to esablish a connection with the database... Error was: " + sqle.toString();
        } catch (GenericEntityException e) {
            return "Unable to esablish a connection with the database... Error was: " + e.toString();
        }

        // TODO: also remove the constraing if this was a unique index, in most databases dropping the index does not drop the constraint

        StringBuffer indexSqlBuf = new StringBuffer("DROP INDEX ");
        indexSqlBuf.append(entity.getTableName(datasourceInfo));
        indexSqlBuf.append(".");
        indexSqlBuf.append(modelIndex.getName());

        String deleteIndexSql = indexSqlBuf.toString();

        if (Debug.verboseOn()) Debug.logVerbose("[deleteForeignKeyIndex] index sql=" + deleteIndexSql);

        try {
            stmt = connection.createStatement();
            stmt.executeUpdate(deleteIndexSql);
        } catch (SQLException sqle) {
            return "SQL Exception while executing the following:\n" + deleteIndexSql + "\nError was: " + sqle.toString();
        } finally {
            cleanup(connection, stmt);
        }
        return null;
    }

    /* ====================================================================== */

    /* ====================================================================== */
    public String createForeignKeyIndices(ModelEntity entity, int constraintNameClipLength) {
        if (entity == null) {
            return "ModelEntity was null and is required to create foreign keys indices for a table";
        }
        if (entity instanceof ModelViewEntity) {
            return "ERROR: Cannot create foreign keys indices for a view entity";
        }

        StringBuffer retMsgsBuffer = new StringBuffer();

        // go through the relationships to see if any foreign keys need to be added
        Iterator relationsIter = entity.getRelationsIterator();

        while (relationsIter.hasNext()) {
            ModelRelation modelRelation = (ModelRelation) relationsIter.next();

            if ("one".equals(modelRelation.getType())) {
                String retMsg = createForeignKeyIndex(entity, modelRelation, constraintNameClipLength);

                if (retMsg != null && retMsg.length() > 0) {
                    if (retMsgsBuffer.length() > 0) {
                        retMsgsBuffer.append("\n");
                    }
                    retMsgsBuffer.append(retMsg);
                }
            }
        }
        if (retMsgsBuffer.length() > 0) {
            return retMsgsBuffer.toString();
        } else {
            return null;
        }
    }

    public String createForeignKeyIndex(ModelEntity entity, ModelRelation modelRelation, int constraintNameClipLength) {
        Connection connection = null;
        Statement stmt = null;

        try {
            connection = getConnection();
        } catch (SQLException sqle) {
            return "Unable to esablish a connection with the database... Error was: " + sqle.toString();
        } catch (GenericEntityException e) {
            return "Unable to esablish a connection with the database... Error was: " + e.toString();
        }

        String createIndexSql = makeFkIndexClause(entity, modelRelation, constraintNameClipLength);

        if (Debug.verboseOn()) Debug.logVerbose("[createForeignKeyIndex] index sql=" + createIndexSql);

        try {
            stmt = connection.createStatement();
            stmt.executeUpdate(createIndexSql);
        } catch (SQLException sqle) {
            return "SQL Exception while executing the following:\n" + createIndexSql + "\nError was: " + sqle.toString();
        } finally {
            cleanup(connection, stmt);
        }
        return null;
    }

    public String makeFkIndexClause(ModelEntity entity, ModelRelation modelRelation, int constraintNameClipLength) {
        Iterator keyMapsIter = modelRelation.getKeyMapsIterator();
        StringBuffer mainCols = new StringBuffer();

        while (keyMapsIter.hasNext()) {
            ModelKeyMap keyMap = (ModelKeyMap) keyMapsIter.next();

            ModelField mainField = entity.getField(keyMap.getFieldName());

            if (mainCols.length() > 0) {
                mainCols.append(", ");
            }
            mainCols.append(mainField.getColName());
        }

        StringBuffer indexSqlBuf = new StringBuffer("CREATE INDEX ");
        String relConstraintName = makeFkConstraintName(modelRelation, constraintNameClipLength);

        indexSqlBuf.append(relConstraintName);
        indexSqlBuf.append(" ON ");
        indexSqlBuf.append(entity.getTableName(datasourceInfo));

        indexSqlBuf.append(" (");
        indexSqlBuf.append(mainCols.toString());
        indexSqlBuf.append(")");

        return indexSqlBuf.toString();
    }

    public String deleteForeignKeyIndices(ModelEntity entity, int constraintNameClipLength) {
        if (entity == null) {
            return "ModelEntity was null and is required to delete foreign keys indices for a table";
        }
        if (entity instanceof ModelViewEntity) {
            return "ERROR: Cannot delete foreign keys indices for a view entity";
        }

        StringBuffer retMsgsBuffer = new StringBuffer();

        // go through the relationships to see if any foreign keys need to be added
        Iterator relationsIter = entity.getRelationsIterator();

        while (relationsIter.hasNext()) {
            ModelRelation modelRelation = (ModelRelation) relationsIter.next();

            if ("one".equals(modelRelation.getType())) {
                String retMsg = deleteForeignKeyIndex(entity, modelRelation, constraintNameClipLength);

                if (retMsg != null && retMsg.length() > 0) {
                    if (retMsgsBuffer.length() > 0) {
                        retMsgsBuffer.append("\n");
                    }
                    retMsgsBuffer.append(retMsg);
                }
            }
        }
        if (retMsgsBuffer.length() > 0) {
            return retMsgsBuffer.toString();
        } else {
            return null;
        }
    }

    public String deleteForeignKeyIndex(ModelEntity entity, ModelRelation modelRelation, int constraintNameClipLength) {
        Connection connection = null;
        Statement stmt = null;

        try {
            connection = getConnection();
        } catch (SQLException sqle) {
            return "Unable to esablish a connection with the database... Error was: " + sqle.toString();
        } catch (GenericEntityException e) {
            return "Unable to esablish a connection with the database... Error was: " + e.toString();
        }

        StringBuffer indexSqlBuf = new StringBuffer("DROP INDEX ");
        String relConstraintName = makeFkConstraintName(modelRelation, constraintNameClipLength);

        indexSqlBuf.append(entity.getTableName(datasourceInfo));
        indexSqlBuf.append(".");
        indexSqlBuf.append(relConstraintName);

        String deleteIndexSql = indexSqlBuf.toString();

        if (Debug.verboseOn()) Debug.logVerbose("[deleteForeignKeyIndex] index sql=" + deleteIndexSql);

        try {
            stmt = connection.createStatement();
            stmt.executeUpdate(deleteIndexSql);
        } catch (SQLException sqle) {
            return "SQL Exception while executing the following:\n" + deleteIndexSql + "\nError was: " + sqle.toString();
        } finally {
            cleanup(connection, stmt);
        }
        return null;
    }

    private String convertToSchemaTableName(String tableName, DatabaseMetaData dbData) throws SQLException {
        // Check if the database supports schemas
       if (tableName != null && dbData.supportsSchemasInTableDefinitions())
       {
          // Check if the table name does not start with the shema name
          if (this.datasourceInfo.getSchemaName() != null  && this.datasourceInfo.getSchemaName().length() > 0 && !tableName.startsWith(this.datasourceInfo.getSchemaName()))
          {
             // Prepend the schema name
             return this.datasourceInfo.getSchemaName().toUpperCase() + "." + tableName;
          }
       }

       return tableName;
    }


    private void logDbInfo(final DatabaseMetaData dbData) {
        try {
            if (Debug.infoOn()) {
                Debug.logInfo("Database Product Name is " + dbData.getDatabaseProductName(), module);
            }
            if (Debug.infoOn()) {
                Debug.logInfo("Database Product Version is " + dbData.getDatabaseProductVersion(), module);
            }
        } catch (SQLException sqle) {
            Debug.logWarning("Unable to get Database name & version information", module);
        }
        try {
            if (Debug.infoOn()) {
                Debug.logInfo("Database Driver Name is " + dbData.getDriverName(), module);
            }
            if (Debug.infoOn()) {
                Debug.logInfo("Database Driver Version is " + dbData.getDriverVersion(), module);
            }
        } catch (SQLException sqle) {
            Debug.logWarning("Unable to get Driver name & version information", module);
        }
    }

    /**
     * Null OK silent closer.
     *
     * @param connection to be closed.
     * @param stmt to be closed.
     */
    private void cleanup(final Connection connection, final Statement stmt) {
        try {
            if (stmt != null)
                stmt.close();
        } catch (SQLException sqle) {}
        try {
            if (connection != null)
                connection.close();
        } catch (SQLException sqle) {}
    }

    /**
     * Null OK closer.
     *
     * @param connection to be closed.
     * @param messages for writing messages.
     */
    private void cleanup(final Connection connection, final Collection messages) {
        try {
            connection.close();
        } catch (SQLException sqle2) {
            error("Unable to close database connection, continuing anyway... Error was:" + sqle2.toString(), messages);
        }
    }

    void error(final String message, final Collection messages) {
        Debug.logError(message, module);
        if (messages != null)
            messages.add(message);
    }

    void warn(final String message, final Collection messages) {
        Debug.logWarning(message, module);
        if (messages != null) {
            messages.add(message);
        }
    }

    void important(final String message, final Collection messages) {
        Debug.logImportant(message, module);
        if (messages != null) {
            messages.add(message);
        }
    }

    void verbose(final String message, final Collection messages) {
        Debug.logVerbose(message, module);
        if (messages != null) messages.add(message);
    }

    /* ====================================================================== */

    /* ====================================================================== */
    public static class ColumnCheckInfo {
        public String tableName;
        public String columnName;
        public String typeName;
        public int columnSize;
        public int decimalDigits;
        public String isNullable; // YES/NO or "" = ie nobody knows
    }

    public static class ReferenceCheckInfo {
        public String pkTableName;

        /** Comma separated list of column names in the related tables primary key */
        public String pkColumnName;
        public String fkName;
        public String fkTableName;

        /** Comma separated list of column names in the primary tables foreign keys */
        public String fkColumnName;

        public String toString() {
            return "FK Reference from table " + fkTableName + " called " + fkName + " to PK in table " + pkTableName;
        }
    }

}
