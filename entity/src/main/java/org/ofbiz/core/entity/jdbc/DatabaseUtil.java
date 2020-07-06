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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import org.ofbiz.core.entity.ConnectionFactory;
import org.ofbiz.core.entity.ConnectionProvider;
import org.ofbiz.core.entity.GenericEntityException;
import org.ofbiz.core.entity.config.DatasourceInfo;
import org.ofbiz.core.entity.config.EntityConfigUtil;
import org.ofbiz.core.entity.jdbc.alternative.IndexAlternativeAction;
import org.ofbiz.core.entity.jdbc.dbtype.DatabaseType;
import org.ofbiz.core.entity.jdbc.dbtype.DatabaseTypeFactory;
import org.ofbiz.core.entity.jdbc.dbtype.Oracle10GDatabaseType;
import org.ofbiz.core.entity.jdbc.sql.escape.SqlEscapeHelper;
import org.ofbiz.core.entity.model.ModelEntity;
import org.ofbiz.core.entity.model.ModelField;
import org.ofbiz.core.entity.model.ModelFieldType;
import org.ofbiz.core.entity.model.ModelFieldTypeReader;
import org.ofbiz.core.entity.model.ModelFunctionBasedIndex;
import org.ofbiz.core.entity.model.ModelIndex;
import org.ofbiz.core.entity.model.ModelKeyMap;
import org.ofbiz.core.entity.model.ModelRelation;
import org.ofbiz.core.entity.model.ModelViewEntity;
import org.ofbiz.core.util.Debug;
import org.ofbiz.core.util.UtilTimer;
import org.ofbiz.core.util.UtilValidate;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

/**
 * Utilities for Entity Database Maintenance
 *
 * @author <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 * @version $Revision: 1.3 $
 * @since 2.0
 */
public class DatabaseUtil {

    public static final String module = DatabaseUtil.class.getName();

    /**
     * A map of legal and expected field type promotions.
     */
    private static final Multimap<String, String> allowedFieldTypePromotions = ImmutableMultimap.<String, String>builder()
            .put("VARCHAR", "NVARCHAR")
            .put("VARCHAR", "TEXT")
            .put("VARCHAR", "LONGTEXT")
            .put("VARCHAR2", "NVARCHAR2")
            .put("NVARCHAR", "NTEXT")
            .build();

    protected final String helperName;
    protected final ModelFieldTypeReader modelFieldTypeReader;
    protected final DatasourceInfo datasourceInfo;
    protected final SqlEscapeHelper sqlEscapeHelper;

    private final ConnectionProvider connectionProvider;

    /**
     * Constructs with the name of a helper that is used to load {@link org.ofbiz.core.entity.config.DatasourceInfo} from
     * {@link org.ofbiz.core.entity.config.EntityConfigUtil} and uses the static {@link
     * org.ofbiz.core.entity.ConnectionFactory} for connections.
     *
     * @param helperName
     */
    public DatabaseUtil(String helperName) {
        this.helperName = helperName;
        this.modelFieldTypeReader = ModelFieldTypeReader.getModelFieldTypeReader(helperName);
        this.datasourceInfo = EntityConfigUtil.getInstance().getDatasourceInfo(helperName);
        this.connectionProvider = ConnectionFactory.provider;
        this.sqlEscapeHelper = new SqlEscapeHelper(datasourceInfo);
    }

    /**
     * Full monty constructor.
     *
     * @param helperName           the helperName
     * @param modelFieldTypeReader the ModelFieldTypeReader
     * @param datasourceInfo       the DatasourceInfo
     * @param connectionProvider   used to create {@link java.sql.Connection Connections}.
     */
    DatabaseUtil(final String helperName, final ModelFieldTypeReader modelFieldTypeReader, final DatasourceInfo datasourceInfo, final ConnectionProvider connectionProvider, SqlEscapeHelper sqlEscapeHelper) {
        this.helperName = helperName;
        this.modelFieldTypeReader = modelFieldTypeReader;
        this.datasourceInfo = datasourceInfo;
        this.connectionProvider = connectionProvider;
        this.sqlEscapeHelper = sqlEscapeHelper;

    }

    /**
     * Uses the configured {@link org.ofbiz.core.entity.ConnectionProvider} to get a {@link java.sql.Connection} based on
     * the configured helper name.
     *
     * @return the {@link java.sql.Connection}
     */
    public Connection getConnection() throws SQLException, GenericEntityException {
        return connectionProvider.getConnection(helperName);
    }

    /**
     * Does a gzillion things to upgrade the database to the entitymodel by adding tables etc.
     *
     * @param modelEntities Model entity names to ModelEntity objects.
     * @param messages      a thing to collect errors.
     * @param addMissing    if true, will attempt to add tables and columns, fks, indices etc are added always.
     */
    public void checkDb(Map<String, ? extends ModelEntity> modelEntities, Collection<String> messages, boolean addMissing) {
        checkDb(modelEntities, messages, addMissing, addMissing, addMissing);
    }

    /**
     * Does a gzillion things to upgrade the database to the entitymodel by adding tables etc.
     *
     * @param modelEntities Model entity names to ModelEntity objects.
     * @param messages      a thing to collect errors.
     * @param addMissing    if true, will attempt to add tables and columns, fks, indices etc are added always.
     * @param promote       if true, will attempt to promote types to wider types, as defined in {@code
     *                      allowedFieldTypePromotions}.
     * @param widen         if true, will attempt to widen types with size (only widen, never shorten)
     */

    public void checkDb(Map<String, ? extends ModelEntity> modelEntities, Collection<String> messages,
                        boolean addMissing, boolean promote, boolean widen) {
        UtilTimer timer = new UtilTimer();

        timer.timerString("Start - Before Get Database metadata");

        // get ALL tables from this database
        TreeSet<String> tableNames = this.getTableNames(messages);
        TreeSet<String> fkTableNames = tableNames == null ? null : new TreeSet<String>(tableNames);
        TreeSet<String> indexTableNames = tableNames == null ? null : new TreeSet<String>(tableNames);
        // keep track of entities whose tables already existed
        Map<String, ModelEntity> existingTableEntities = new HashMap<String, ModelEntity>();

        if (tableNames == null) {
            error("Could not get table name information from the database, aborting.", messages);
            return;
        }
        timer.timerString("After Get All Table Names");

        // get ALL column info, put into hashmap by table name
        Map<String, List<ColumnCheckInfo>> colInfo = this.getColumnInfo(tableNames, messages);
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

        ArrayList<ModelEntity> modelEntityList = new ArrayList<ModelEntity>(modelEntities.values());

        // sort using compareTo method on ModelEntity
        Collections.sort(modelEntityList);

        Iterator<ModelEntity> modelEntityIter = modelEntityList.iterator();
        int curEnt = 0;
        int totalEnt = modelEntityList.size();
        List<ModelEntity> entitiesAdded = new LinkedList<ModelEntity>();

        while (modelEntityIter.hasNext()) {
            curEnt++;
            ModelEntity entity = modelEntityIter.next();
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
                existingTableEntities.put(entity.getPlainTableName(), entity);

                if (colInfo != null) {
                    Map<String, ModelField> fieldColNames = new HashMap<String, ModelField>();
                    Map<String, ModelField> virtualColumnNames = new HashMap<String, ModelField>();
                    for (int fnum = 0; fnum < entity.getFieldsSize(); fnum++) {
                        ModelField field = entity.getField(fnum);
                        fieldColNames.put(field.getColName().toUpperCase(), field);
                    }
                    for (Iterator<ModelFunctionBasedIndex> iter = entity.getFunctionBasedIndexesIterator(); iter.hasNext(); ) {
                        ModelFunctionBasedIndex fbIndex = iter.next();
                        ModelField mf = fbIndex.getVirtualColumnModelField(datasourceInfo.getDatabaseTypeFromJDBCConnection());
                        if (mf != null) {
                            fieldColNames.put(mf.getColName().toUpperCase(), mf);
                            virtualColumnNames.put(mf.getColName().toUpperCase(), mf);
                        }
                    }

                    List<ColumnCheckInfo> colList = colInfo.get(upperTableName);
                    int numCols = 0;
                    int numFields = fieldColNames.size();
                    if (colList != null) {
                        for (; numCols < colList.size(); numCols++) {
                            ColumnCheckInfo ccInfo = colList.get(numCols);

                            // -list all columns that do not have a corresponding field
                            if (fieldColNames.containsKey(ccInfo.columnName)) {
                                ModelField field = fieldColNames.remove(ccInfo.columnName);
                                checkFieldType(entity, field, ccInfo, messages, promote, widen);
                            } else {
                                warn("Column \"" + ccInfo.columnName + "\" of table \"" + tableName + "\" of entity \"" + entityName + "\" exists in the database but has no corresponding field", messages);
                            }
                        }
                    }

                    // -display message if number of table columns does not match number of entity fields
                    if (numCols != numFields) {
                        String message = "Entity \"" + entityName + "\" has " + numFields + " fields but table \"" + tableName + "\" has " +
                                numCols + " columns.";

                        warn(message, messages);
                    }

                    // remove all calculated columns from list of columns, if the virtual column is missing it will
                    // be added via the createMissingFunctionBasedIndices method call later in this method
                    for (String s : virtualColumnNames.keySet()) {
                        fieldColNames.remove(s);
                    }

                    // -list all fields that do not have a corresponding column

                    for (String s : fieldColNames.keySet()) {
                        ModelField field = fieldColNames.get(s);

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
                    String errMsg = createTable(entity, modelEntities, false, datasourceInfo.isUsePkConstraintNames(), datasourceInfo.getConstraintNameClipLength(), datasourceInfo.getFkStyle(), datasourceInfo.isUseFkInitiallyDeferred(), sqlEscapeHelper);

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
        Iterator<String> tableNamesIter = tableNames.iterator();

        while (tableNamesIter != null && tableNamesIter.hasNext()) {
            String tableName = tableNamesIter.next();
            verbose("Table named \"" + tableName + "\" exists in the database but has no corresponding entity", messages);
        }

        // for each newly added table, add fks
        if (datasourceInfo.isUseFks()) {
            for (ModelEntity curEntity : entitiesAdded) {
                String errMsg = createForeignKeys(curEntity, modelEntities, datasourceInfo.getConstraintNameClipLength(), datasourceInfo.getFkStyle(), datasourceInfo.isUseFkInitiallyDeferred());

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
            for (ModelEntity curEntity : entitiesAdded) {
                String indErrMsg = createForeignKeyIndices(curEntity, datasourceInfo.getConstraintNameClipLength());

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
            for (ModelEntity curEntity : entitiesAdded) {
                String indErrMsg = createDeclaredIndices(curEntity);

                if (indErrMsg != null && indErrMsg.length() > 0) {
                    error("Could not create declared indices for entity \"" + curEntity.getEntityName() + "\"", messages);
                    error(indErrMsg, messages);
                } else {
                    important("Created declared indices for entity \"" + curEntity.getEntityName() + "\"", messages);
                }
            }

            createMissingIndices(existingTableEntities, messages);
        }

        if (datasourceInfo.isUseFunctionBasedIndices()) {
            // for each newly added table, add function based indexes
            for (ModelEntity curEntity : entitiesAdded) {
                String indErrMsg = createFunctionBasedIndices(curEntity);

                if (indErrMsg != null && indErrMsg.length() > 0) {
                    error("Could not create function based indices for entity \"" + curEntity.getEntityName() + "\"", messages);
                    error(indErrMsg, messages);
                }
            }
            createMissingFunctionBasedIndices(existingTableEntities, messages);
        }

        // make sure each one-relation has an FK
        if (datasourceInfo.isUseFks() && datasourceInfo.isCheckForeignKeysOnStart()) {
            // NOTE: This ISN'T working for Postgres or MySQL, who knows about others, may be from JDBC driver bugs...
            int numFksCreated = 0;
            // TODO: check each key-map to make sure it exists in the FK, if any differences warn and then remove FK and recreate it

            // get ALL column info, put into hashmap by table name
            Map<String, Map<String, ReferenceCheckInfo>> refTableInfoMap = this.getReferenceInfo(fkTableNames, messages);

            // Debug.logVerbose("Ref Info Map: " + refTableInfoMap);

            if (refTableInfoMap == null) {// uh oh, something happened while getting info...
            } else {
                for (ModelEntity entity : modelEntityList) {
                    String entityName = entity.getEntityName();

                    // if this is a view entity, do not check it...
                    if (entity instanceof ModelViewEntity) {
                        verbose("NOT Checking View Entity " + entity.getEntityName(), messages);
                        continue;
                    }

                    // get existing FK map for this table
                    Map<String, ReferenceCheckInfo> rcInfoMap = refTableInfoMap.get(entity.getTableName(datasourceInfo));
                    // Debug.logVerbose("Got ref info for table " + entity.getTableName(datasourceInfo) + ": " + rcInfoMap);

                    // go through each relation to see if an FK already exists
                    Iterator<ModelRelation> relations = entity.getRelationsIterator();
                    boolean createdConstraints = false;

                    while (relations.hasNext()) {
                        ModelRelation modelRelation = relations.next();

                        if (!"one".equals(modelRelation.getType())) {
                            continue;
                        }

                        ModelEntity relModelEntity = modelEntities.get(modelRelation.getRelEntityName());

                        String relConstraintName = makeFkConstraintName(modelRelation, datasourceInfo.getConstraintNameClipLength());
                        ReferenceCheckInfo rcInfo = null;

                        if (rcInfoMap != null) {
                            rcInfo = rcInfoMap.get(relConstraintName);
                        }

                        if (rcInfo != null) {
                            rcInfoMap.remove(relConstraintName);
                        } else {
                            // if not, create one
                            if (Debug.verboseOn()) {
                                Debug.logVerbose("No Foreign Key Constraint " + relConstraintName + " found in entity " + entityName);
                            }
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
                        for (String rcKeyLeft : rcInfoMap.keySet()) {
                            Debug.logImportant("Unknown Foreign Key Constraint " + rcKeyLeft + " found in table " + entity.getTableName(datasourceInfo));
                        }
                    }
                }
            }
            if (Debug.infoOn()) {
                Debug.logInfo("Created " + numFksCreated + " fk refs");
            }
        }

        // make sure each one-relation has an index
        if (datasourceInfo.isUseFkIndices() && datasourceInfo.isCheckFkIndicesOnStart()) {
            int numIndicesCreated = 0;
            // TODO: check each key-map to make sure it exists in the index, if any differences warn and then remove the index and recreate it

            // TODO: also check the declared indices on start, if the datasourceInfo.checkIndicesOnStart flag is set

            // get ALL column info, put into hashmap by table name
            Map<String, Set<String>> tableIndexListMap = this.getIndexInfo(indexTableNames, messages);

            // Debug.logVerbose("Ref Info Map: " + refTableInfoMap);

            if (tableIndexListMap == null) {// uh oh, something happened while getting info...
            } else {
                for (ModelEntity entity : modelEntityList) {
                    String entityName = entity.getEntityName();

                    // if this is a view entity, do not check it...
                    if (entity instanceof ModelViewEntity) {
                        verbose("NOT Checking View Entity " + entity.getEntityName(), messages);
                        continue;
                    }

                    // get existing index list for this table
                    Set<String> tableIndexList = tableIndexListMap.get(entity.getTableName(datasourceInfo));

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
                        Iterator<ModelRelation> relations = entity.getRelationsIterator();

                        while (relations.hasNext()) {
                            ModelRelation modelRelation = relations.next();

                            if (!"one".equals(modelRelation.getType())) {
                                continue;
                            }

                            String relConstraintName = makeFkConstraintName(modelRelation, datasourceInfo.getConstraintNameClipLength());

                            if (tableIndexList.contains(relConstraintName)) {
                                tableIndexList.remove(relConstraintName);
                            } else {
                                // if not, create one
                                if (Debug.verboseOn()) {
                                    Debug.logVerbose("No Index " + relConstraintName + " found for entity " + entityName);
                                }
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
                        for (String indexLeft : tableIndexList) {
                            Debug.logImportant("Unknown Index " + indexLeft + " found in table " + entity.getTableName(datasourceInfo));
                        }
                    }
                }
            }
            if (Debug.infoOn()) {
                Debug.logInfo("Created " + numIndicesCreated + " indices");
            }
        }

        timer.timerString("Finished Checking Entity Database");
    }

    /**
     * Checks the given {@link org.ofbiz.core.entity.model.ModelEntity entity's}  fieldType to see that it matches the
     * given ColumnCheckInfo. Error messages are added to messages.
     */
    void checkFieldType(final ModelEntity entity, final ModelField field, final ColumnCheckInfo ccInfo, final Collection<String> messages,
                        final boolean promote, final boolean widen) {
        final String fieldType = field.getType();
        final ModelFieldType modelFieldType = modelFieldTypeReader.getModelFieldType(fieldType);

        if (modelFieldType != null) {
            final ColumnTypeParser parsedColumnType = new ColumnTypeParser(entity, ccInfo, messages, modelFieldType).invoke();
            final String typeName = parsedColumnType.getTypeName();
            final int decimalDigits = parsedColumnType.getDecimalDigits();
            final String fullTypeStr = parsedColumnType.getFullTypeStr();

            if (!ccInfo.typeName.equals(typeName.toUpperCase())) {
                final Collection<String> allowedPromotions = DatabaseUtil.allowedFieldTypePromotions.get(ccInfo.typeName);
                // decimal digits not supported yet for promoting!
                if (promote && allowedPromotions != null && allowedPromotions.contains(typeName.toUpperCase()) && decimalDigits == -1) {
                    // promote the field:
                    final String errorMessage = modifyColumnType(entity, field);
                    if (errorMessage == null) {
                        final String message = "Column \"" + ccInfo.columnName + "\" of table \"" + entity.getTableName(datasourceInfo) + "\" of entity \"" +
                                entity.getEntityName() + "\" is of wrong type and has been promoted from \"" + ccInfo.typeAsString() +
                                "\" to \"" + fullTypeStr + "\".";
                        important(message, messages);
                    } else {
                        error("Could not promote column \"" + ccInfo.columnName + "\" in table \"" + entity.getTableName(datasourceInfo) + "\" from type: \"" +
                                ccInfo.typeAsString() + "\" to type: \"" + fullTypeStr + "\".", messages);
                        error(errorMessage, messages);
                    }
                } else {
                    final String message = "WARNING: Column \"" + ccInfo.columnName + "\" of table \"" + entity.getTableName(datasourceInfo) + "\" of entity \"" +
                            entity.getEntityName() + "\" is of type \"" + ccInfo.typeAsString() + "\" in the database, but is defined as type \"" +
                            fullTypeStr + "\" in the entity definition.";

                    error(message, messages);
                }
            } else {
                if (isTypeChangeNeeded(parsedColumnType)) {
                    if (isTypeChangeAllowed(parsedColumnType, widen)) {
                        // widen the field:
                        final String errorMessage = modifyColumnType(entity, field);
                        if (errorMessage == null) {
                            final String message = "Column \"" + ccInfo.columnName + "\" of type \"" + typeName + "\" of table \"" +
                                    entity.getTableName(datasourceInfo) + "\" of entity \"" + entity.getEntityName() + "\" has different type definition and has been changed from " +
                                    ccInfo.typeAsString() + " to " + fullTypeStr + ".";
                            important(message, messages);
                        } else {
                            error("Could not widen column \"" + ccInfo.columnName + "\" in table \"" + entity.getTableName(datasourceInfo) + "\" to size: " +
                                    fullTypeStr + ".", messages);
                            error(errorMessage, messages);
                        }
                    } else {
                        final String message = "WARNING: Column \"" + ccInfo.columnName + "\" of table \"" + entity.getTableName(datasourceInfo) + "\" of entity \"" +
                                entity.getEntityName() + "\" has a column size of \"" + ccInfo.typeAsString() +
                                "\" in the database, but is defined to have a column size of \"" + fullTypeStr + "\" in the entity definition.";

                        warn(message, messages);
                    }
                }
                if (decimalDigits != -1 && decimalDigits != ccInfo.decimalDigits) {
                    final String message = "WARNING: Column \"" + ccInfo.columnName + "\" of table \"" + entity.getTableName(datasourceInfo) + "\" of entity \"" +
                            entity.getEntityName() + "\" has a decimalDigits of \"" + ccInfo.decimalDigits +
                            "\" in the database, but is defined to have a decimalDigits of \"" + decimalDigits + "\" in the entity definition.";

                    warn(message, messages);
                }
            }
        } else {
            final String message = "Column \"" + ccInfo.columnName + "\" of table \"" + entity.getTableName(datasourceInfo) + "\" of entity \"" + entity.getEntityName() +
                    "\" has a field type name of \"" + fieldType + "\" which is not found in the field type definitions";

            error(message, messages);
        }
    }

    private boolean isTypeChangeAllowed(ColumnTypeParser type, boolean widen) {
        final boolean oracleUnicodeWidening = type.isOracle && Oracle10GDatabaseType.detectUnicodeWidening(type.typeName, type.ccInfo, type.typeNameExtension);
        return widen && type.decimalDigits == -1 && (type.columnSize > type.ccInfo.columnSize || oracleUnicodeWidening);
    }

    private boolean isTypeChangeNeeded(ColumnTypeParser type) {
        boolean ret = type.columnSize != -1 && type.ccInfo.columnSize != -1 && (type.columnSize != type.ccInfo.columnSize);
        if (ret || !type.isOracle) {
            return ret;
        }
        final boolean oracleUnicodeWidening = Oracle10GDatabaseType.detectUnicodeWidening(type.typeName, type.ccInfo, type.typeNameExtension);
        final boolean columnWithOracleUnicode = Oracle10GDatabaseType.detectUnicodeExtension(type.ccInfo);
        return ret | oracleUnicodeWidening || (columnWithOracleUnicode && !"CHAR".equals(type.typeNameExtension));

    }

    /**
     * Change the type of the field in database.
     *
     * @param entity the entity (db table)
     * @param field  the existing column type
     */
    private String modifyColumnType(ModelEntity entity, ModelField field) {
        if (entity == null || field == null) {
            return "ModelEntity or ModelField null, cannot alter table";
        }
        if (entity instanceof ModelViewEntity) {
            return "ERROR: Cannot change column for a view entity";
        }

        Connection connection = null;
        Statement stmt = null;

        try {
            try {
                connection = getConnection();
            } catch (SQLException sqle) {
                return "Unable to establish a connection with the database... Error was: " + sqle.toString();
            } catch (GenericEntityException e) {
                return "Unable to establish a connection with the database... Error was: " + e.toString();
            }

            ModelFieldType type = modelFieldTypeReader.getModelFieldType(field.getType());

            if (type == null) {

                return "Field type [" + type + "] not found for field [" + field.getName() + "] of entity [" + entity.getEntityName() + "], not changing column type.";
            }

            DatabaseType dbType = datasourceInfo.getDatabaseTypeFromJDBCConnection(connection);
            if (dbType == null) {
                return "Failed to detect DB type.";
            }

            String changeColumnTypeClause = dbType.getChangeColumnTypeSQL(entity.getTableName(datasourceInfo), field.getColName(), type.getSqlType());
            if (changeColumnTypeClause == null) {
                return "Changing of column type is not supported in " + dbType.getName() + ".";
            }

            if (Debug.infoOn()) {
                Debug.logInfo("[modifyColumnType] sql=" + changeColumnTypeClause);
            }
            try {
                stmt = connection.createStatement();
                stmt.executeUpdate(changeColumnTypeClause);
            } catch (SQLException sqle) {
                return "SQL Exception while executing the following:\n" + changeColumnTypeClause + "\nError was: " + sqle.toString();
            }
        } finally {
            cleanup(connection, stmt);
        }
        return null;
    }

    /**
     * Add only the missing indexes for the given modelEntities, keyed by table name. The existence of an index is
     * determined soley by its name. If the {@link org.ofbiz.core.entity.model.ModelIndex} defines an index that has the
     * same name but different fields or unique flag as the one in the database, no action is taken to rectify the
     * difference.
     *
     * @param tableToModelEntities a map of table name to corresponding model entity.
     * @param messages             error messages go here
     */
    void createMissingIndices(Map<String, ModelEntity> tableToModelEntities, Collection<String> messages) {
        // get the actual db index names per table
        final Map<String, Set<String>> indexInfo = getIndexInfo(tableToModelEntities.keySet(), messages, true);

        for (Map.Entry<String, Set<String>> indexInfoEntry : indexInfo.entrySet()) {
            final String tableName = indexInfoEntry.getKey();
            final Set<String> actualIndexes = indexInfoEntry.getValue();

            final ModelEntity modelEntity = tableToModelEntities.get(tableName);
            final Iterator<ModelIndex> indexesIterator = modelEntity.getIndexesIterator();

            final StringBuilder retMsgsBuffer = new StringBuilder();

            while (indexesIterator.hasNext()) {
                ModelIndex modelIndex = indexesIterator.next();
                if (!actualIndexes.contains(modelIndex.getName().toUpperCase())) {
                    if (Debug.infoOn()) {
                        Debug.logInfo("Missing index '" + modelIndex.getName() + "' on existing table '" + tableName + "' ...creating");
                    }
                    String retMsg = createDeclaredIndex(modelEntity, modelIndex);

                    if (retMsg != null && retMsg.length() > 0) {
                        if (retMsgsBuffer.length() > 0) {
                            retMsgsBuffer.append('\n');
                        }
                        retMsgsBuffer.append(retMsg);
                    }
                }
            }
            if (retMsgsBuffer.length() > 0) {
                error("Could not create missing indices for entity \"" + modelEntity.getEntityName() + '"', messages);
                error(retMsgsBuffer.toString(), messages);
            }
        }
    }


    /**
     * Creates a list of ModelEntity objects based on metadata from the database
     */
    public List<ModelEntity> induceModelFromDb(Collection<String> messages) {
        // get ALL tables from this database
        TreeSet<String> tableNames = this.getTableNames(messages);

        // get ALL column info, put into hashmap by table name
        Map<String, List<ColumnCheckInfo>> colInfo = this.getColumnInfo(tableNames, messages);

        // go through each table and make a ModelEntity object, add to list
        // for each entity make corresponding ModelField objects
        // then print out XML for the entities/fields
        List<ModelEntity> newEntList = new LinkedList<ModelEntity>();

        // iterate over the table names is alphabetical order

        for (String tableName : new TreeSet<String>(colInfo.keySet())) {
            List<ColumnCheckInfo> colList = colInfo.get(tableName);

            ModelEntity newEntity = new ModelEntity(tableName, colList, modelFieldTypeReader);

            newEntList.add(newEntity);
        }

        return newEntList;
    }

    public TreeSet<String> getTableNames(Collection<String> messages) {
        Connection connection;

        try {
            connection = getConnection();
        } catch (SQLException sqle) {
            error("Unable to establish a connection with the database... Error was:" + sqle.toString(), messages);
            return null;
        } catch (GenericEntityException e) {
            error("Unable to establish a connection with the database... Error was:" + e.toString(), messages);
            return null;
        }

        if (connection == null) {
            error("Unable to establish a connection with the database, no additional information available.", messages);
            return null;
        }

        DatabaseMetaData dbData = null;

        try {
            dbData = connection.getMetaData();
        } catch (SQLException sqle) {
            error("Unable to get database metadata... Error was:" + sqle.toString(), messages);
            return null;
        }

        if (dbData == null) {
            Debug.logWarning("Unable to get database metadata; method returned null", module);
        }

        logDbInfo(dbData);

        if (Debug.infoOn()) {
            Debug.logInfo("Getting Table Info From Database");
        }

        // get ALL tables from this database
        TreeSet<String> tableNames = new TreeSet<String>();
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
                    if (tableType != null && !"TABLE".equals(tableType) && !"VIEW".equals(tableType) && !"ALIAS".equals(tableType) && !"SYNONYM".equals(tableType)) {
                        continue;
                    }

                    // String remarks = tableSet.getString("REMARKS");
                    tableNames.add(tableName);
                    // if (Debug.infoOn()) Debug.logInfo("Found table named \"" + tableName + "\" of type \"" + tableType + "\" with remarks: " + remarks);
                } catch (SQLException sqle) {
                    error("Error getting table information... Error was:" + sqle.toString(), messages);
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

    private String lookupSchemaName(DatabaseMetaData dbData) throws SQLException {
        return datasourceInfo == null ? null : getSchemaPattern(dbData, datasourceInfo.getSchemaName());

    }

    /**
     * Lookup schema name according do database metadata
     * see JIRA-28526 this method needs to be coherent with {@link #convertToSchemaTableName(String, java.sql.DatabaseMetaData)} and
     * {@link ModelEntity#getTableName(org.ofbiz.core.entity.config.DatasourceInfo)}
     */
    public static String getSchemaPattern(final DatabaseMetaData dbData, String schemaName) throws SQLException {

        if (dbData.supportsSchemasInTableDefinitions()) {
            if (schemaName != null && schemaName.length() > 0) {
                return schemaName;
            } else if ("Oracle".equalsIgnoreCase(dbData.getDatabaseProductName())) {
                // For Oracle, the username is the schema name
                return dbData.getUserName();
            }
        }
        return null;
    }

    public Map<String, List<ColumnCheckInfo>> getColumnInfo(Set<String> tableNames, Collection<String> messages) {
        // if there are no tableNames, don't even try to get the columns
        if (tableNames.size() == 0) {
            return new HashMap<String, List<ColumnCheckInfo>>();
        }

        Connection connection;

        try {
            connection = getConnection();
        } catch (SQLException sqle) {
            error("Unable to establish a connection with the database... Error was:" + sqle.toString(), messages);
            return null;
        } catch (GenericEntityException e) {
            error("Unable to establish a connection with the database... Error was:" + e.toString(), messages);
            return null;
        }

        DatabaseMetaData dbData = null;

        try {
            dbData = connection.getMetaData();
        } catch (SQLException sqle) {
            error("Unable to get database metadata... Error was:" + sqle.toString(), messages);
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

        if (Debug.infoOn()) {
            Debug.logInfo("Getting Column Info From Database");
        }

        Map<String, List<ColumnCheckInfo>> colInfo = new HashMap<String, List<ColumnCheckInfo>>();

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
                    if (!tableNames.contains(ccInfo.tableName)) {
                        continue;
                    }

                    ccInfo.columnName = rsCols.getString("COLUMN_NAME");
                    ccInfo.columnName = (ccInfo.columnName == null) ? null : ccInfo.columnName.toUpperCase();

                    ccInfo.typeName = rsCols.getString("TYPE_NAME");
                    ccInfo.typeName = (ccInfo.typeName == null) ? null : ccInfo.typeName.toUpperCase();
                    ccInfo.columnSize = rsCols.getInt("COLUMN_SIZE");
                    ccInfo.maxSizeInBytes = rsCols.getInt("CHAR_OCTET_LENGTH");
                    ccInfo.decimalDigits = rsCols.getInt("DECIMAL_DIGITS");

                    final String isNullableSqlResponse = rsCols.getString("IS_NULLABLE");
                    if (isNullableSqlResponse != null && !isNullableSqlResponse.isEmpty()) {
                        ccInfo.isNullable = "YES".equals(isNullableSqlResponse.toUpperCase()) ? Boolean.TRUE : Boolean.FALSE;
                    }

                    List<ColumnCheckInfo> tableColInfo = colInfo.get(ccInfo.tableName);

                    if (tableColInfo == null) {
                        tableColInfo = new ArrayList<ColumnCheckInfo>();
                        colInfo.put(ccInfo.tableName, tableColInfo);
                    }
                    tableColInfo.add(ccInfo);
                } catch (SQLException sqle) {

                    error("Error getting column info for column. Error was:" + sqle.toString(), messages);
                }
            }

            try {
                rsCols.close();
            } catch (SQLException sqle) {
                error("Unable to close ResultSet for column list, continuing anyway... Error was:" + sqle.toString(), messages);
            }
        } catch (SQLException sqle) {
            error("Error getting column metadata for Error was:" + sqle.toString() + ". Not checking columns.", messages);
            // we are returning an empty set in this case because databases like SapDB throw an exception when there are no tables in the database
            // colInfo = null;
        } finally {
            cleanup(connection, messages);
        }
        return colInfo;
    }

    public Map<String, Map<String, ReferenceCheckInfo>> getReferenceInfo(Set<String> tableNames, Collection<String> messages) {
        Connection connection;
        try {
            connection = getConnection();
        } catch (SQLException sqle) {
            error("Unable to establish a connection with the database... Error was:" + sqle.toString(), messages);
            return null;
        } catch (GenericEntityException e) {
            error("Unable to establish a connection with the database... Error was:" + e.toString(), messages);
            return null;
        }

        DatabaseMetaData dbData = null;
        try {
            dbData = connection.getMetaData();
        } catch (SQLException sqle) {
            error("Unable to get database metadata... Error was:" + sqle.toString(), messages);

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

        if (Debug.infoOn()) {
            Debug.logInfo("Getting Foreign Key (Reference) Info From Database");
        }

        Map<String, Map<String, ReferenceCheckInfo>> refInfo = new HashMap<String, Map<String, ReferenceCheckInfo>>();

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
                    if (!tableNames.contains(rcInfo.fkTableName)) {
                        continue;
                    }
                    rcInfo.fkColumnName = rsCols.getString("FKCOLUMN_NAME");
                    rcInfo.fkColumnName = (rcInfo.fkColumnName == null) ? null : rcInfo.fkColumnName.toUpperCase();

                    rcInfo.fkName = rsCols.getString("FK_NAME");
                    rcInfo.fkName = (rcInfo.fkName == null) ? null : rcInfo.fkName.toUpperCase();

                    if (Debug.verboseOn()) {
                        Debug.logVerbose("Got: " + rcInfo.toString());
                    }

                    Map<String, ReferenceCheckInfo> tableRefInfo = refInfo.get(rcInfo.fkTableName);

                    if (tableRefInfo == null) {
                        tableRefInfo = new HashMap<String, ReferenceCheckInfo>();
                        refInfo.put(rcInfo.fkTableName, tableRefInfo);
                        if (Debug.verboseOn()) {
                            Debug.logVerbose("Adding new Map for table: " + rcInfo.fkTableName);
                        }
                    }
                    if (!tableRefInfo.containsKey(rcInfo.fkName)) {
                        totalFkRefs++;
                    }
                    tableRefInfo.put(rcInfo.fkName, rcInfo);
                } catch (SQLException sqle) {

                    error("Error getting fk reference info for table. Error was:" + sqle.toString(), messages);
                }
            }

            try {
                rsCols.close();
            } catch (SQLException sqle) {

                error("Unable to close ResultSet for fk reference list, continuing anyway... Error was:" + sqle.toString(), messages);
            }
            if (Debug.infoOn()) {
                Debug.logInfo("There are " + totalFkRefs + " foreign key refs in the database");
            }

        } catch (SQLException sqle) {
            error("Error getting fk reference metadata Error was:" + sqle.toString() + ". Not checking fk refs.", messages);
            refInfo = null;
        } finally {
            cleanup(connection, messages);
        }
        return refInfo;
    }

    public Map<String, Set<String>> getIndexInfo(Set<String> tableNames, Collection<String> messages) {

        // preserving backwards compatibility of this method while providing a new version that can
        // return unique indexes.

        // HACK: for now skip all "unique" indexes since our foreign key indices are not unique, but the primary key ones are
        return getIndexInfo(tableNames, messages, false);
    }

    /**
     * Gets index information from the database for the given table names only, optionally including unique indexes.
     *
     * @param tableNames    the names of tables to get indexes for.
     * @param messages      a collector of errors.
     * @param includeUnique if true, the index info will include unique indexes which could include pk indexes.
     * @return a map of table names to sets of index names or null on failure.
     */
    public Map<String, Set<String>> getIndexInfo(Set<String> tableNames, Collection<String> messages, boolean includeUnique) {
        Connection connection;
        try {
            connection = getConnection();
        } catch (SQLException sqle) {
            error("Unable to establish a connection with the database... Error was:" + sqle.toString(), messages);
            return null;
        } catch (GenericEntityException e) {
            error("Unable to establish a connection with the database... Error was:" + e.toString(), messages);
            return null;
        }

        DatabaseMetaData dbData = null;

        try {
            dbData = connection.getMetaData();
        } catch (SQLException sqle) {
            error("Unable to get database metadata... Error was:" + sqle.toString(), messages);
            cleanup(connection, messages);
            return null;
        }

        if (Debug.infoOn()) {
            Debug.logInfo("Getting Index Info From Database");
        }

        Map<String, Set<String>> indexInfo = new HashMap<String, Set<String>>();

        try {
            int totalIndices = 0;
            Iterator<String> tableNamesIter = tableNames.iterator();

            String lookupSchemaName = lookupSchemaName(dbData);
            DatabaseType databaseType = DatabaseTypeFactory.getTypeForConnection(connection);

            while (tableNamesIter.hasNext()) {
                String curTableName = tableNamesIter.next();
                Set<String> tableIndexList = new TreeSet<String>();
                indexInfo.put(curTableName, tableIndexList);

                ResultSet rsCols = null;
                try {
                    rsCols = getIndexInfo(dbData, databaseType, lookupSchemaName, curTableName);
                } catch (Exception e) {
                    Debug.logWarning(e, "Error getting index info for table: " + curTableName + " using lookupSchemaName " + lookupSchemaName);
                }

                while (rsCols != null && rsCols.next()) {
                    // NOTE: The code in this block may look funny, but it is designed so that the wrapping loop can be removed
                    // REPLY: Well you fucked that up then
                    try {
                        // skip all index info for statistics
                        if (rsCols.getShort("TYPE") == DatabaseMetaData.tableIndexStatistic) {
                            continue;
                        }

                        if (!includeUnique && !rsCols.getBoolean("NON_UNIQUE")) {
                            continue;
                        }

                        String indexName = rsCols.getString("INDEX_NAME");

                        indexName = (indexName == null) ? null : indexName.toUpperCase();

                        if (!tableIndexList.contains(indexName)) {
                            totalIndices++;
                        }
                        tableIndexList.add(indexName);
                    } catch (SQLException sqle) {
                        error("Error getting fk reference info for table. Error was:" + sqle.toString(), messages);
                    }
                }

                if (rsCols != null) {
                    try {
                        rsCols.close();
                    } catch (SQLException sqle) {

                        error("Unable to close ResultSet for fk reference list, continuing anyway... Error was:" + sqle.toString(), messages);
                    }
                }
            }
            if (Debug.infoOn()) {
                Debug.logInfo("There are " + totalIndices + " indices in the database");
            }

        } catch (SQLException sqle) {

            error("Error getting fk reference metadata Error was:" + sqle.toString() + ". Not checking fk refs.", messages);
            indexInfo = null;
        } finally {
            cleanup(connection, messages);
        }
        return indexInfo;
    }

    /**
     * Gets the index info for the given schema and table from the given dbData, taking into account the wacky
     * upper/lowercase rules for table names for the database type, e.g. Oracle and Postgres.
     *
     * @param dbData     the DatabaseMetaData to get the index info through.
     * @param dbType     the type of the databaes, e.g. {@link DatabaseTypeFactory#ORACLE_10G}
     * @param schemaName the name of the schema.
     * @param tableName  the name of the table whose indexes are being queried.
     * @return the {@link ResultSet} for the IndexInfo
     * @throws SQLException direct from the jdbc call.
     */
    @VisibleForTesting
    ResultSet getIndexInfo(DatabaseMetaData dbData, DatabaseType dbType, String schemaName, String tableName)
            throws SQLException {
        ResultSet rsCols;
        // ORACLE's table names are case sensitive when used as a parameter to getIndexInfo call
        // however when a table is created and the table name is not quoted then oracle automatically uppercase it.
        // i.e
        // 'create table issues' will create table with name ISSUES and inside getIndexInfo 'ISSUES' must be used.
        // OFBiz does not put table names in quotes when it creates them thus for oracle they are always uppercased.
        //
        // The same rule can be used for HSQLDB and H2.
        if (DatabaseTypeFactory.ORACLE_10G == dbType
                || DatabaseTypeFactory.ORACLE_8I == dbType
                || DatabaseTypeFactory.HSQL == dbType
                || DatabaseTypeFactory.HSQL_2_3_3 == dbType
                || DatabaseTypeFactory.H2 == dbType) {
            rsCols = dbData.getIndexInfo(null, schemaName, tableName.toUpperCase(), false, true);
        } else {
            rsCols = dbData.getIndexInfo(null, schemaName, tableName, false, true);
            boolean isPostgres = DatabaseTypeFactory.POSTGRES == dbType
                    || DatabaseTypeFactory.POSTGRES_7_2 == dbType
                    || DatabaseTypeFactory.POSTGRES_7_3 == dbType;
            if (isPostgres) {
                // Postgres can make tables in the lowercase version of the declared table name, though not universally.
                // if there are no index details for the given table and we're on postgres,
                // we fall back to the index info of the lower case version of the table
                if (rsCols == null || !rsCols.next()) {
                    close("empty result set from reading index info", rsCols);
                    rsCols = dbData.getIndexInfo(null, schemaName, tableName.toLowerCase(), false, true);
                } else {
                    rsCols.beforeFirst();
                }
            }
        }
        return rsCols;
    }

    public String createTable(ModelEntity entity, Map<String, ? extends ModelEntity> modelEntities,
                              boolean addFks,
                              boolean usePkConstraintNames,
                              int constraintNameClipLength,
                              String fkStyle,
                              boolean useFkInitiallyDeferred,
                              SqlEscapeHelper sqlEscapeHelper) {
        if (entity == null) {
            return "ModelEntity was null and is required to create a table";
        }
        if (entity instanceof ModelViewEntity) {
            return "ERROR: Cannot create table for a view entity";
        }

        Connection connection;

        try {
            connection = getConnection();
        } catch (SQLException sqle) {
            return "Unable to establish a connection with the database... Error was: " + sqle.toString();
        } catch (GenericEntityException e) {
            return "Unable to establish a connection with the database... Error was: " + e.toString();
        }

        StringBuilder sqlBuf = new StringBuilder("CREATE TABLE ");
        sqlBuf.append(entity.getTableName(datasourceInfo));
        sqlBuf.append(" (");
        for (int i = 0; i < entity.getFieldsSize(); i++) {
            ModelField field = entity.getField(i);
            ModelFieldType type = modelFieldTypeReader.getModelFieldType(field.getType());

            if (type == null) {
                return "Field type [" + type + "] not found for field [" + field.getName() + "] of entity [" + entity.getEntityName() + "], not creating table.";
            }

            sqlBuf.append(sqlEscapeHelper.escapeColumn(field.getColName()));
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
        sqlBuf.append(entity.colNameString(entity.getPksCopy(), sqlEscapeHelper));
        sqlBuf.append(")");

        if (addFks) {
            // NOTE: This is kind of a bad idea anyway since ordering table creations is crazy, if not impossible

            // go through the relationships to see if any foreign keys need to be added
            Iterator<ModelRelation> relationsIter = entity.getRelationsIterator();

            while (relationsIter.hasNext()) {
                ModelRelation modelRelation = relationsIter.next();

                if ("one".equals(modelRelation.getType())) {
                    ModelEntity relModelEntity = modelEntities.get(modelRelation.getRelEntityName());

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
        if (Debug.verboseOn()) {
            Debug.logVerbose("[createTable] sql=" + sqlBuf.toString());
        }
        return executeStatement(connection, sqlBuf.toString());
    }

    public String addColumn(ModelEntity entity, ModelField field) {
        if (entity == null || field == null) {
            return "ModelEntity or ModelField where null, cannot add column";
        }
        if (entity instanceof ModelViewEntity) {
            return "ERROR: Cannot add column for a view entity";
        }

        Connection connection = null;
        Statement stmt = null;

        try {
            connection = getConnection();
        } catch (SQLException sqle) {
            return "Unable to establish a connection with the database... Error was: " + sqle.toString();
        } catch (GenericEntityException e) {
            return "Unable to establish a connection with the database... Error was: " + e.toString();
        }

        ModelFieldType type = modelFieldTypeReader.getModelFieldType(field.getType());

        if (type == null) {
            return "Field type [" + type + "] not found for field [" + field.getName() + "] of entity [" + entity.getEntityName() + "], not adding column.";
        }

        StringBuilder sqlBuf = new StringBuilder("ALTER TABLE ");
        sqlBuf.append(entity.getTableName(datasourceInfo));
        sqlBuf.append(" ADD ");
        sqlBuf.append(sqlEscapeHelper.escapeColumn(field.getColName()));
        sqlBuf.append(" ");
        sqlBuf.append(type.getSqlType());

        String sql = sqlBuf.toString();
        if (Debug.infoOn()) {
            Debug.logInfo("[addColumn] sql=" + sql);
        }
        try {
            stmt = connection.createStatement();
            stmt.executeUpdate(sql);
        } catch (SQLException sqle) {
            // if that failed try the alternate syntax real quick
            String sql2 = "ALTER TABLE " + entity.getTableName(datasourceInfo) + " ADD COLUMN " + sqlEscapeHelper.escapeColumn(field.getColName()) + " " + type.getSqlType();
            if (Debug.infoOn()) {
                Debug.logInfo("[addColumn] sql failed, trying sql2=" + sql2);
            }
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


    public String addVirtualColumn(ModelEntity entity, ModelFunctionBasedIndex index) {
        if (entity == null || index == null) {
            return "Either ModelEntity or ModelFunctionBasedIndex was null; cannot add column";
        }
        if (entity instanceof ModelViewEntity) {
            return "ERROR: Cannot add column for a view entity";
        }
        Connection connection;
        try {
            connection = getConnection();
        } catch (SQLException sqle) {
            return "Unable to establish a connection with the database... Error was: " + sqle.toString();
        } catch (GenericEntityException e) {
            return "Unable to establish a connection with the database... Error was: " + e.toString();
        }
        ModelFieldType type = modelFieldTypeReader.getModelFieldType(index.getType());
        if (type == null) {
            return "Field type [" + type + "] not found for field [" + index.getName() + "] of entity [" + entity.getEntityName() + "], not adding column.";
        }
        DatabaseType dbType = datasourceInfo.getDatabaseTypeFromJDBCConnection(connection);
        StringBuilder sqlBuf = new StringBuilder("ALTER TABLE ");
        sqlBuf.append(entity.getTableName(datasourceInfo));
        sqlBuf.append(" ADD ");
        sqlBuf.append(sqlEscapeHelper.escapeColumn(index.getVirtualColumn(dbType)));
        sqlBuf.append(" ");
        sqlBuf.append(type.getSqlType());
        sqlBuf.append(" AS (");
        sqlBuf.append(index.getFunction(dbType));
        sqlBuf.append(")");
        String sql = sqlBuf.toString();
        if (Debug.infoOn()) {
            Debug.logInfo("[addColumn] sql=" + sql);
        }
        String sqle = executeStatement(connection, sql);
        if (sqle != null) return sqle;
        return null;
    }

    private String executeStatement(Connection connection, String sql) {
        Statement stmt = null;
        try {
            stmt = connection.createStatement();
            stmt.executeUpdate(sql);
        } catch (SQLException sqle) {
            return "SQL Exception while executing the following:\n" + sql + "\nError was: " + sqle.toString();
        } finally {
            cleanup(connection, stmt);
        }
        return null;
    }

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

    public String createForeignKeys(ModelEntity entity, Map<String, ? extends ModelEntity> modelEntities, int constraintNameClipLength, String fkStyle, boolean useFkInitiallyDeferred) {
        if (entity == null) {
            return "ModelEntity was null and is required to create foreign keys for a table";
        }
        if (entity instanceof ModelViewEntity) {
            return "ERROR: Cannot create foreign keys for a view entity";
        }

        StringBuilder retMsgsBuffer = new StringBuilder();

        // go through the relationships to see if any foreign keys need to be added
        Iterator<ModelRelation> relationsIter = entity.getRelationsIterator();

        while (relationsIter.hasNext()) {
            ModelRelation modelRelation = relationsIter.next();

            if ("one".equals(modelRelation.getType())) {
                ModelEntity relModelEntity = modelEntities.get(modelRelation.getRelEntityName());

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
        Connection connection;

        try {
            connection = getConnection();
        } catch (SQLException sqle) {
            return "Unable to establish a connection with the database... Error was: " + sqle.toString();
        } catch (GenericEntityException e) {
            return "Unable to establish a connection with the database... Error was: " + e.toString();
        }

        // now add constraint clause
        StringBuilder sqlBuf = new StringBuilder("ALTER TABLE ");
        sqlBuf.append(entity.getTableName(datasourceInfo));
        sqlBuf.append(" ADD ");
        sqlBuf.append(makeFkConstraintClause(entity, modelRelation, relModelEntity, constraintNameClipLength, fkStyle, useFkInitiallyDeferred));

        if (Debug.verboseOn()) {
            Debug.logVerbose("[createForeignKey] sql=" + sqlBuf.toString());
        }
        return executeStatement(connection, sqlBuf.toString());
    }

    public String makeFkConstraintClause(ModelEntity entity, ModelRelation modelRelation, ModelEntity relModelEntity, int constraintNameClipLength, String fkStyle, boolean useFkInitiallyDeferred) {
        // make the two column lists
        Iterator<ModelKeyMap> keyMapsIter = modelRelation.getKeyMapsIterator();
        StringBuilder mainCols = new StringBuilder();
        StringBuilder relCols = new StringBuilder();

        while (keyMapsIter.hasNext()) {
            ModelKeyMap keyMap = keyMapsIter.next();

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

        StringBuilder sqlBuf = new StringBuilder("");

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

    public String deleteForeignKeys(ModelEntity entity, Map<String, ? extends ModelEntity> modelEntities, int constraintNameClipLength) {
        if (entity == null) {
            return "ModelEntity was null and is required to delete foreign keys for a table";
        }
        if (entity instanceof ModelViewEntity) {
            return "ERROR: Cannot delete foreign keys for a view entity";
        }

        // go through the relationships to see if any foreign keys need to be added
        Iterator<ModelRelation> relationsIter = entity.getRelationsIterator();
        StringBuilder retMsgsBuffer = new StringBuilder();

        while (relationsIter.hasNext()) {
            ModelRelation modelRelation = relationsIter.next();

            if ("one".equals(modelRelation.getType())) {
                ModelEntity relModelEntity = modelEntities.get(modelRelation.getRelEntityName());

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
        Connection connection;

        try {
            connection = getConnection();
        } catch (SQLException sqle) {
            return "Unable to establish a connection with the database... Error was: " + sqle.toString();
        } catch (GenericEntityException e) {
            return "Unable to establish a connection with the database... Error was: " + e.toString();
        }

        String relConstraintName = makeFkConstraintName(modelRelation, constraintNameClipLength);

        // now add constraint clause
        StringBuilder sqlBuf = new StringBuilder("ALTER TABLE ");
        sqlBuf.append(entity.getTableName(datasourceInfo));
        sqlBuf.append(" DROP CONSTRAINT ");
        sqlBuf.append(relConstraintName);

        if (Debug.verboseOn()) {
            Debug.logVerbose("[deleteForeignKey] sql=" + sqlBuf.toString());
        }
        return executeStatement(connection, sqlBuf.toString());
    }

    /**
     * Creates a database index for every declared index on the given entity.
     *
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

        StringBuilder retMsgsBuffer = new StringBuilder();

        // go through the indexes to see if any need to be added
        Iterator<ModelIndex> indexesIter = entity.getIndexesIterator();
        while (indexesIter.hasNext()) {
            ModelIndex modelIndex = indexesIter.next();

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
        Connection connection;

        try {
            connection = getConnection();
        } catch (SQLException | GenericEntityException sqle) {
            return "Unable to establish a connection with the database... Error was: " + sqle.toString();
        }

        try {
            Optional<IndexAlternativeAction> indexAlternativeAction = modelIndex.getAlternativeIndexAction(this);
            if (indexAlternativeAction.isPresent()) {
                return indexAlternativeAction.get().run(entity, modelIndex, this);
            }
        } catch (SQLException | GenericEntityException ex) {
            return "Unable to handle alternative action... Error was: " + ex.toString();
        }

        String createIndexSql = makeIndexClause(entity, modelIndex);
        if (Debug.verboseOn()) {
            Debug.logVerbose("[createForeignKeyIndex] index sql=" + createIndexSql);
        }

        return executeStatement(connection, createIndexSql);
    }

    public String makeIndexClause(ModelEntity entity, ModelIndex modelIndex) {
        Iterator<String> fieldNamesIter = modelIndex.getIndexFieldsIterator();
        StringBuilder mainCols = new StringBuilder();

        while (fieldNamesIter.hasNext()) {
            String fieldName = fieldNamesIter.next();
            ModelField mainField = entity.getField(fieldName);
            if (mainCols.length() > 0) {
                mainCols.append(", ");
            }
            mainCols.append(mainField.getColName());
        }
        StringBuilder indexSqlBuf = generateIndexClause(entity, modelIndex.getUnique(), modelIndex.getName(), mainCols.toString());
        return indexSqlBuf.toString();
    }

    private StringBuilder generateIndexClause(ModelEntity entity, boolean unique, String indexName, String mainCols) {
        StringBuilder indexSqlBuf = new StringBuilder("CREATE ");
        if (unique) {
            indexSqlBuf.append("UNIQUE ");
        }
        indexSqlBuf.append("INDEX ");
        indexSqlBuf.append(indexName);
        indexSqlBuf.append(" ON ");
        indexSqlBuf.append(entity.getTableName(datasourceInfo));

        indexSqlBuf.append(" (");
        indexSqlBuf.append(mainCols);
        indexSqlBuf.append(")");
        return indexSqlBuf;
    }

    public String deleteDeclaredIndices(ModelEntity entity) {
        if (entity == null) {
            return "ModelEntity was null and is required to delete foreign keys indices for a table";
        }
        if (entity instanceof ModelViewEntity) {
            return "ERROR: Cannot delete foreign keys indices for a view entity";
        }

        StringBuilder retMsgsBuffer = new StringBuilder();

        // go through the relationships to see if any foreign keys need to be added
        Iterator<ModelIndex> indexesIter = entity.getIndexesIterator();
        while (indexesIter.hasNext()) {
            ModelIndex modelIndex = indexesIter.next();
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
        Connection connection;

        try {
            connection = getConnection();
        } catch (SQLException | GenericEntityException sqle) {
            return "Unable to establish a connection with the database... Error was: " + sqle.toString();
        }

        final DatabaseType dbType = datasourceInfo.getDatabaseTypeFromJDBCConnection(connection);
        final String deleteIndexSql = dbType.getDropIndexSQL(datasourceInfo.getSchemaName(),
                entity.getPlainTableName(), modelIndex.getName());

        if (Debug.verboseOn()) {
            Debug.logVerbose("[deleteForeignKeyIndex] index sql=" + deleteIndexSql);
        }

        return executeStatement(connection, deleteIndexSql);
    }

    public String createForeignKeyIndices(ModelEntity entity, int constraintNameClipLength) {
        if (entity == null) {
            return "ModelEntity was null and is required to create foreign keys indices for a table";
        }
        if (entity instanceof ModelViewEntity) {
            return "ERROR: Cannot create foreign keys indices for a view entity";
        }

        StringBuilder retMsgsBuffer = new StringBuilder();

        // go through the relationships to see if any foreign keys need to be added
        Iterator<ModelRelation> relationsIter = entity.getRelationsIterator();

        while (relationsIter.hasNext()) {
            ModelRelation modelRelation = relationsIter.next();

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
        Connection connection;

        try {
            connection = getConnection();
        } catch (SQLException | GenericEntityException sqle) {
            return "Unable to establish a connection with the database... Error was: " + sqle.toString();
        }

        String createIndexSql = makeFkIndexClause(entity, modelRelation, constraintNameClipLength);

        if (Debug.verboseOn()) {
            Debug.logVerbose("[createForeignKeyIndex] index sql=" + createIndexSql);
        }

        return executeStatement(connection, createIndexSql);
    }

    public String makeFkIndexClause(ModelEntity entity, ModelRelation modelRelation, int constraintNameClipLength) {
        Iterator<ModelKeyMap> keyMapsIter = modelRelation.getKeyMapsIterator();
        StringBuilder mainCols = new StringBuilder();

        while (keyMapsIter.hasNext()) {
            ModelKeyMap keyMap = keyMapsIter.next();

            ModelField mainField = entity.getField(keyMap.getFieldName());

            if (mainCols.length() > 0) {
                mainCols.append(", ");
            }
            mainCols.append(mainField.getColName());
        }

        String relConstraintName = makeFkConstraintName(modelRelation, constraintNameClipLength);
        StringBuilder indexSqlBuf = generateIndexClause(entity, false, relConstraintName, mainCols.toString());
        return indexSqlBuf.toString();
    }

    public String deleteForeignKeyIndices(ModelEntity entity, int constraintNameClipLength) {
        if (entity == null) {
            return "ModelEntity was null and is required to delete foreign keys indices for a table";
        }
        if (entity instanceof ModelViewEntity) {
            return "ERROR: Cannot delete foreign keys indices for a view entity";
        }

        StringBuilder retMsgsBuffer = new StringBuilder();

        // go through the relationships to see if any foreign keys need to be added
        Iterator<ModelRelation> relationsIter = entity.getRelationsIterator();

        while (relationsIter.hasNext()) {
            ModelRelation modelRelation = relationsIter.next();

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
        Connection connection;

        try {
            connection = getConnection();
        } catch (SQLException sqle) {
            return "Unable to establish a connection with the database... Error was: " + sqle.toString();
        } catch (GenericEntityException e) {
            return "Unable to establish a connection with the database... Error was: " + e.toString();
        }

        StringBuilder indexSqlBuf = new StringBuilder("DROP INDEX ");
        String relConstraintName = makeFkConstraintName(modelRelation, constraintNameClipLength);

        indexSqlBuf.append(entity.getTableName(datasourceInfo));
        indexSqlBuf.append(".");
        indexSqlBuf.append(relConstraintName);

        String deleteIndexSql = indexSqlBuf.toString();

        if (Debug.verboseOn()) {
            Debug.logVerbose("[deleteForeignKeyIndex] index sql=" + deleteIndexSql);
        }

        return executeStatement(connection, deleteIndexSql);
    }

    /**
     * Creates a function based index for every declared index on the given entity.
     *
     * @param entity
     * @return an error message if there is an error, or null if it worked.
     */
    public String createFunctionBasedIndices(ModelEntity entity) {
        if (entity == null) {
            return "ModelEntity was null and is required to create functional based indices for a table";
        }
        if (entity instanceof ModelViewEntity) {
            return "ERROR: Cannot create functional based indices for a view entity";
        }
        StringBuilder retMsgsBuffer = new StringBuilder();
        // go through the indexes to see if any need to be added
        Iterator<ModelFunctionBasedIndex> indexesIter = entity.getFunctionBasedIndexesIterator();
        while (indexesIter.hasNext()) {
            ModelFunctionBasedIndex fbIndex = indexesIter.next();

            String retMsg = createFunctionBasedIndex(entity, fbIndex);

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

    /**
     * Add only the missing function based indexes for the given modelEntities, keyed by table name. The existence of an index is
     * determined soley by its name. If the {@link org.ofbiz.core.entity.model.ModelFunctionBasedIndex} defines an index that has the
     * same name but a different function or unique flag as the one in the database, no action is taken to rectify the
     * difference.
     *
     * @param tableToModelEntities a map of table name to corresponding model entity.
     * @param messages             error messages go here
     */
    void createMissingFunctionBasedIndices(Map<String, ModelEntity> tableToModelEntities, Collection<String> messages) {
        // get the actual db index names per table
        final Map<String, Set<String>> indexInfo = getIndexInfo(tableToModelEntities.keySet(), messages, true);

        for (Map.Entry<String, Set<String>> indexInfoEntry : indexInfo.entrySet()) {
            final String tableName = indexInfoEntry.getKey();
            final Set<String> actualIndexes = indexInfoEntry.getValue();

            final ModelEntity modelEntity = tableToModelEntities.get(tableName);
            final Iterator<ModelFunctionBasedIndex> indexesIterator = modelEntity.getFunctionBasedIndexesIterator();

            final StringBuilder retMsgsBuffer = new StringBuilder();

            while (indexesIterator.hasNext()) {
                ModelFunctionBasedIndex modelIndex = indexesIterator.next();
                if (!actualIndexes.contains(modelIndex.getName().toUpperCase())) {
                    if (Debug.infoOn()) {
                        Debug.logInfo("Missing index '" + modelIndex.getName() + "' on existing table '" + tableName + "' ...creating");
                    }
                    String retMsg = createFunctionBasedIndex(modelEntity, modelIndex);

                    if (retMsg != null && retMsg.length() > 0) {
                        if (retMsgsBuffer.length() > 0) {
                            retMsgsBuffer.append('\n');
                        }
                        retMsgsBuffer.append(retMsg);
                    }
                }
            }
            if (retMsgsBuffer.length() > 0) {
                error("Could not create missing function based indices for entity \"" + modelEntity.getEntityName() + '"', messages);
                error(retMsgsBuffer.toString(), messages);
            } else {
                important("Created function based indices for entity \"" + modelEntity.getEntityName() + "\"", messages);
            }
        }
    }

    public String createFunctionBasedIndex(ModelEntity entity, ModelFunctionBasedIndex fbIndex) {
        Connection connection;
        try {
            connection = getConnection();
        } catch (SQLException sqle) {
            return "Unable to establish a connection with the database... Error was: " + sqle.toString();
        } catch (GenericEntityException e) {
            return "Unable to establish a connection with the database... Error was: " + e.toString();
        }
        String createFBIndexSql = makeFunctionBasedIndexClause(entity, fbIndex, datasourceInfo.getDatabaseTypeFromJDBCConnection(connection));
        if (Debug.verboseOn()) {
            Debug.logVerbose("[createFunctionBasedIndex] index sql=" + createFBIndexSql);
        }

        return executeStatement(connection, createFBIndexSql);
    }

    public String makeFunctionBasedIndexClause(ModelEntity entity, ModelFunctionBasedIndex fbIndex, DatabaseType databaseType) {
        String indexOver;
        if (fbIndex.supportsFunctionBasedIndices(databaseType)) {
            indexOver = fbIndex.getFunction(databaseType);
        } else {
            addVirtualColumn(entity, fbIndex);
            indexOver = fbIndex.getVirtualColumn(databaseType);
        }
        StringBuilder indexSqlBuf = generateIndexClause(entity, fbIndex.getUnique(), fbIndex.getName(), indexOver);
        return indexSqlBuf.toString();
    }


    private String convertToSchemaTableName(String tableName, DatabaseMetaData dbData) throws SQLException {
        // Check if the database supports schemas
        if (tableName != null && dbData.supportsSchemasInTableDefinitions()) {
            // Check if the table name does not start with the shema name
            if (this.datasourceInfo.getSchemaName() != null && this.datasourceInfo.getSchemaName().length() > 0 && !tableName.startsWith(this.datasourceInfo.getSchemaName() + ".")) {
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
     * @param stmt       to be closed.
     */
    private void cleanup(final Connection connection, final Statement stmt) {
        try {
            if (stmt != null) {
                stmt.close();
            }
        } catch (SQLException sqle) {
        }
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (SQLException sqle) {
        }
    }

    /**
     * Null OK closer.
     *
     * @param connection to be closed.
     * @param messages   for writing messages.
     */
    private void cleanup(final Connection connection, final Collection<String> messages) {
        try {
            connection.close();
        } catch (SQLException sqle2) {
            error("Unable to close database connection, continuing anyway... Error was:" + sqle2.toString(), messages);
        }
    }

    void error(final String message, final Collection<String> messages) {
        Debug.logError(message, module);
        if (messages != null) {
            messages.add(message);
        }
    }

    void warn(final String message, final Collection<String> messages) {
        Debug.logWarning(message, module);
        if (messages != null) {
            messages.add(message);
        }
    }

    void important(final String message, final Collection<String> messages) {
        Debug.logImportant(message, module);
        if (messages != null) {
            messages.add(message);
        }
    }

    void verbose(final String message, final Collection<String> messages) {
        Debug.logVerbose(message, module);
        if (messages != null) {
            messages.add(message);
        }
    }

    public static class ColumnCheckInfo {
        public String tableName;
        public String columnName;
        public String typeName;
        public int columnSize;
        public int decimalDigits;
        public Boolean isNullable; // null = ie nobody knows
        public int maxSizeInBytes;

        public String typeAsString() {
            if (columnSize > 0) {
                if (decimalDigits > 0) {
                    return String.format("%s(%d,%d)", typeName, columnSize, decimalDigits);
                } else {
                    return String.format("%s(%d%s)", typeName, columnSize, Oracle10GDatabaseType.detectUnicodeExtension(this) ? " CHAR" : "");
                }
            } else {
                return typeName;
            }
        }
    }

    public static class ReferenceCheckInfo {
        public String pkTableName;

        /**
         * Comma separated list of column names in the related tables primary key
         */
        public String pkColumnName;
        public String fkName;
        public String fkTableName;

        /**
         * Comma separated list of column names in the primary tables foreign keys
         */
        public String fkColumnName;

        public String toString() {
            return "FK Reference from table " + fkTableName + " called " + fkName + " to PK in table " + pkTableName;
        }
    }

    private class ColumnTypeParser {
        private final ModelEntity entity;
        private final ColumnCheckInfo ccInfo;
        private final Collection<String> messages;
        private final ModelFieldType modelFieldType;
        private final boolean isOracle;
        private String fullTypeStr;
        private String typeName;
        private String typeNameExtension;
        private int columnSize;
        private int decimalDigits;

        public ColumnTypeParser(ModelEntity entity, ColumnCheckInfo ccInfo, Collection<String> messages, ModelFieldType modelFieldType) {
            this.entity = entity;
            this.ccInfo = ccInfo;
            this.messages = messages;
            this.modelFieldType = modelFieldType;
            Connection connection = null;
            boolean isOracle = false;
            try {
                connection = getConnection();
                final DatabaseType dbType = datasourceInfo.getDatabaseTypeFromJDBCConnection(connection);
                isOracle = DatabaseTypeFactory.ORACLE_10G == dbType || DatabaseTypeFactory.ORACLE_8I == dbType;
            } catch (SQLException sqle) {
                error("Unable to establish a connection with the database... Error was: " + sqle.toString(), messages);
            } catch (GenericEntityException e) {
                error("Unable to establish a connection with the database... Error was: " + e.toString(), messages);
            } finally {
                cleanup(connection, (Statement) null);
            }

            this.isOracle = isOracle;
        }

        public String getFullTypeStr() {
            return fullTypeStr;
        }

        public String getTypeName() {
            return typeName;
        }

        public String getTypeNameExtension() {
            return typeNameExtension;
        }

        public int getColumnSize() {
            return columnSize;
        }

        public int getDecimalDigits() {
            return decimalDigits;
        }

        public ColumnTypeParser invoke() {
            // make sure each corresponding column is of the correct type
            fullTypeStr = modelFieldType.getSqlType();
            final int openParen = fullTypeStr.indexOf('(');
            final int closeParen = fullTypeStr.indexOf(')', openParen);
            final int comma = fullTypeStr.indexOf(',');

            typeNameExtension = "";
            columnSize = -1;
            decimalDigits = -1;

            if (openParen > 0 && closeParen > 0 && closeParen > openParen) {
                typeName = fullTypeStr.substring(0, openParen);
                if (comma > 0 && comma > openParen && comma < closeParen) {
                    final String csStr = fullTypeStr.substring(openParen + 1, comma);

                    try {
                        columnSize = Integer.parseInt(csStr);
                    } catch (NumberFormatException e) {
                        Debug.logError(e, module);
                    }

                    final String ddStr = fullTypeStr.substring(comma + 1, closeParen);

                    try {
                        decimalDigits = Integer.parseInt(ddStr);
                    } catch (NumberFormatException e) {
                        Debug.logError(e, module);
                    }
                } else {
                    final String fullColumnSizeStr = fullTypeStr.substring(openParen + 1, closeParen);

                    try {
                        // In Oracle, the VARCHAR2 can be represented as VARCHAR2(x) or VARCHAR2(x BYTE) for non-unicode,
                        // or VARCHAR2(x CHAR) for unicode.
                        final String[] splitSizeStr = fullColumnSizeStr.trim().split(" +");
                        switch (splitSizeStr.length) {
                            case 2:
                                if (splitSizeStr[1].matches("BYTE|CHAR")) {
                                    typeNameExtension = splitSizeStr[1];
                                } else {
                                    final String message = "Definition for column \"" + ccInfo.columnName + "\" of table \"" + entity.getTableName(datasourceInfo) +
                                            "\" of entity \"" + entity.getEntityName() + "\" has an invalid size extension \"" + splitSizeStr[1] +
                                            "\" which will be ignored.";
                                    warn(message, messages);
                                }
                            case 1:  // note: follow-through
                                columnSize = Integer.parseInt(splitSizeStr[0]);
                                break;
                            default:
                                throw new NumberFormatException("For input string: \"" + fullColumnSizeStr + "\"");
                        }
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
            return this;
        }
    }

    private void close(String context, ResultSet rs) {
        if (rs == null) {
            return;
        }
        try {
            rs.close();
        } catch (SQLException sqle) {
            Debug.logError(sqle, "Unexpected error closing result set for " + context + "; ignoring...", module);
        }
    }
}
