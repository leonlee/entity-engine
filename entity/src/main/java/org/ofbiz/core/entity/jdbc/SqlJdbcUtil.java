/*
 * $Id: SqlJdbcUtil.java,v 1.2 2006/03/20 05:10:13 detkin Exp $
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

import org.ofbiz.core.entity.EntityConditionParam;
import org.ofbiz.core.entity.GenericDAO;
import org.ofbiz.core.entity.GenericDataSourceException;
import org.ofbiz.core.entity.GenericEntity;
import org.ofbiz.core.entity.GenericEntityException;
import org.ofbiz.core.entity.GenericModelException;
import org.ofbiz.core.entity.GenericNotImplementedException;
import org.ofbiz.core.entity.GenericValue;
import org.ofbiz.core.entity.config.DatasourceInfo;
import org.ofbiz.core.entity.model.ModelEntity;
import org.ofbiz.core.entity.model.ModelField;
import org.ofbiz.core.entity.model.ModelFieldType;
import org.ofbiz.core.entity.model.ModelFieldTypeReader;
import org.ofbiz.core.entity.model.ModelKeyMap;
import org.ofbiz.core.entity.model.ModelViewEntity;
import org.ofbiz.core.util.Debug;
import org.ofbiz.core.util.UtilValidate;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.sql.Blob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * GenericDAO Utility methods for general tasks
 *
 * @author     <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 * @author     <a href="mailto:chris_maurer@altavista.com">Chris Maurer</a>
 * @author     <a href="mailto:jaz@ofbiz.org">Andy Zeneski</a>
 * @author     <a href="mailto:jdonnerstag@eds.de">Juergen Donnerstag</a>
 * @author     <a href="mailto:peterm@miraculum.com">Peter Moon</a>
 * @version    $Revision: 1.2 $
 * @since      2.0
 */
public class SqlJdbcUtil {

    public static final String module = GenericDAO.class.getName();

    private static final int BOOLEAN = 9;

    /**
     * Indicates whether the given field type represents a Boolean field. Equivalent to calling {@link #getType(String)}
     * and comparing the result to the int value for a Boolean.
     *
     * @param fieldType the field type to check
     * @return see above
     */
    public static boolean isBoolean(final String fieldType) {
        try {
            return getType(fieldType) == BOOLEAN;
        }
        catch (GenericNotImplementedException e) {
            throw new UnsupportedOperationException(e);
        }
    }

    /** Makes the FROM clause and when necessary the JOIN clause(s) as well */
    public static String makeFromClause(ModelEntity modelEntity, DatasourceInfo datasourceInfo) throws GenericEntityException {
        StringBuilder sql = new StringBuilder(" FROM ");

        if (modelEntity instanceof ModelViewEntity) {
            ModelViewEntity modelViewEntity = (ModelViewEntity) modelEntity;

            if ("ansi".equals(datasourceInfo.getJoinStyle())) {

                // FROM clause: in this case will be a bunch of joins that correspond with the view-links

                // BIG NOTE on the JOIN clauses: the order of joins is determined by the order of the
                // view-links; for more flexible order we'll have to figure something else out and
                // extend the DTD for the nested view-link elements or something

                // At this point it is assumed that in each view-link the left hand alias will
                // either be the first alias in the series or will already be in a previous
                // view-link and already be in the big join; SO keep a set of all aliases
                // in the join so far and if the left entity alias isn't there yet, and this
                // isn't the first one, throw an exception
                Set<String> joinedAliasSet = new TreeSet<String>();

                // TODO: at view-link read time make sure they are ordered properly so that each
                // left hand alias after the first view-link has already been linked before

                StringBuilder openParens = new StringBuilder();
                StringBuilder restOfStatement = new StringBuilder();

                for (int i = 0; i < modelViewEntity.getViewLinksSize(); i++) {
                    // don't put starting parenthesis
                    if (i > 0) openParens.append('(');

                    ModelViewEntity.ModelViewLink viewLink = modelViewEntity.getViewLink(i);

                    ModelEntity linkEntity = modelViewEntity.getMemberModelEntity(viewLink.getEntityAlias());
                    ModelEntity relLinkEntity = modelViewEntity.getMemberModelEntity(viewLink.getRelEntityAlias());

                    if (i == 0) {
                        // this is the first referenced member alias, so keep track of it for future use...
                        restOfStatement.append(makeViewTable(linkEntity, datasourceInfo));
                        restOfStatement.append(" ");
                        restOfStatement.append(viewLink.getEntityAlias());

                        joinedAliasSet.add(viewLink.getEntityAlias());
                    } else {
                        // make sure the left entity alias is already in the join...
                        if (!joinedAliasSet.contains(viewLink.getEntityAlias())) {
                            throw new GenericModelException("Tried to link the " + viewLink.getEntityAlias() + " alias to the " + viewLink.getRelEntityAlias() + " alias of the " + modelViewEntity.getEntityName() + " view-entity, but it is not the first view-link and has not been included in a previous view-link. In other words, the left/main alias isn't connected to the rest of the member-entities yet.");
                        }
                    }
                    // now put the rel (right) entity alias into the set that is in the join
                    joinedAliasSet.add(viewLink.getRelEntityAlias());

                    if (viewLink.isRelOptional()) {
                        restOfStatement.append(" LEFT JOIN ");
                    } else {
                        restOfStatement.append(" INNER JOIN ");
                    }

                    restOfStatement.append(makeViewTable(relLinkEntity, datasourceInfo));
                    restOfStatement.append(" ");
                    restOfStatement.append(viewLink.getRelEntityAlias());
                    restOfStatement.append(" ON ");

                    StringBuilder condBuffer = new StringBuilder();

                    for (int j = 0; j < viewLink.getKeyMapsSize(); j++) {
                        ModelKeyMap keyMap = viewLink.getKeyMap(j);
                        String constValue = keyMap.getConstValue();
                        ModelField linkField = linkEntity.getField(keyMap.getFieldName());
                        String aliasToUse = viewLink.getEntityAlias();
                        //
                        // When constant values are in play, we have to have a way of indicating that we are
                        // using a field from the RHS table.  So if the LHS field is null
                        // then we use the related RHS field instead.
                        //
                        if (constValue.length() > 0 && linkField == null) {
                            linkField = relLinkEntity.getField(keyMap.getRelFieldName());
                            aliasToUse = viewLink.getRelEntityAlias();
                        }

                        if (condBuffer.length() > 0) {
                            condBuffer.append(" AND ");
                        }
                        condBuffer.append(aliasToUse);
                        condBuffer.append(".");
                        condBuffer.append(filterColName(linkField.getColName()));

                        condBuffer.append(" = ");

                        if (constValue.length() > 0) {
                            //
                            // Our limitation is that we don't escape quotes in the constant value.
                            // This is OK because its us declaring the constant value in the model xml
                            // and we will make sure there is NO quotes.
                            //
                            // but just to be sure we go bang if a quote is there
                            if (constValue.indexOf('\'') >= 0) {
                                throw new GenericModelException("The declared const-value in the key-map contains a single quote");
                            }
                            condBuffer.append("'");
                            condBuffer.append(constValue);
                            condBuffer.append("'");
                        } else {
                            ModelField relLinkField = relLinkEntity.getField(keyMap.getRelFieldName());
                            condBuffer.append(viewLink.getRelEntityAlias());
                            condBuffer.append(".");
                            condBuffer.append(filterColName(relLinkField.getColName()));
                        }
                    }
                    if (condBuffer.length() == 0) {
                        throw new GenericModelException("No view-link/join key-maps found for the " + viewLink.getEntityAlias() + " and the " + viewLink.getRelEntityAlias() + " member-entities of the " + modelViewEntity.getEntityName() + " view-entity.");
                    }
                    restOfStatement.append(condBuffer.toString());

                    // don't put ending parenthesis
                    if (i < (modelViewEntity.getViewLinksSize() - 1)) restOfStatement.append(')');
                }

                sql.append(openParens.toString());
                sql.append(restOfStatement.toString());
                
                // handle tables not included in view-link
                Iterator<? extends Map.Entry<String, ?>> meIter = modelViewEntity.getMemberModelMemberEntities().entrySet().iterator();
                boolean fromEmpty = restOfStatement.length() == 0;
                
                while (meIter.hasNext()) {
                    Map.Entry<String, ?> entry = meIter.next();
                    ModelEntity fromEntity = modelViewEntity.getMemberModelEntity(entry.getKey());
                    
                    if (!joinedAliasSet.contains(entry.getKey())) {
                        if (!fromEmpty) sql.append(", ");
                        fromEmpty = false;

                        sql.append(makeViewTable(fromEntity, datasourceInfo));
                        sql.append(" ");
                        sql.append(entry.getKey());
                    }
                }


            } else if ("theta-oracle".equals(datasourceInfo.getJoinStyle()) || "theta-mssql".equals(datasourceInfo.getJoinStyle())) {
                // FROM clause
                Iterator<? extends Map.Entry<String, ?>> meIter = modelViewEntity.getMemberModelMemberEntities().entrySet().iterator();

                while (meIter.hasNext()) {
                    Map.Entry<String, ?> entry = meIter.next();
                    ModelEntity fromEntity = modelViewEntity.getMemberModelEntity(entry.getKey());

                    sql.append(makeViewTable(fromEntity, datasourceInfo));
                    sql.append(" ");
                    sql.append(entry.getKey());
                    if (meIter.hasNext()) sql.append(", ");
                }

                // JOIN clause(s): none needed, all the work done in the where clause for theta-oracle
            } else {
                throw new GenericModelException("The join-style " + datasourceInfo.getJoinStyle() + " is not yet supported");
            }
        } else {
            sql.append(modelEntity.getTableName(datasourceInfo));
        }
        return sql.toString();
    }

    /**
     * Makes a WHERE clause String with "<col name>=?" if not null or "<col name> IS NULL" if null, all separated by the
     * given operator.
     *
     * @param modelFields the fields to include in the WHERE string (can be null)
     * @param fieldValues any field values to be checked against non-null values; keys are field (not column) names
     * @param operator the operator to insert between each column condition in the returned WHERE string (typically
     * "AND" or "OR")
     * @return an empty string if the given list of fields is null or empty, otherwise a string like
     * "first_name IS NULL OR last_name=?"
     */
    public static String makeWhereStringFromFields(
            final List<ModelField> modelFields, final Map<String, ?> fieldValues, final String operator)
    {
        return makeWhereStringFromFields(modelFields, fieldValues, operator, null);
    }

    /**
     * Makes a WHERE clause String with "<col name>=?" if not null or "<col name> IS NULL" if null, all separated by the
     * given operator.
     *
     * @param modelFields the fields to include in the WHERE string (can be null)
     * @param fieldValues any field values to be checked against non-null values; keys are field (not column) names
     * @param operator the operator to insert between each column
     * condition in the returned WHERE string (typically "AND" or "OR")
     * @param entityConditionParams if not null, an element will be added to this list
     * @return an empty string if the given list of fieldValues is null or empty, otherwise a string like
     * "first_name IS NULL OR last_name=?"
     */
    public static String makeWhereStringFromFields(final List<ModelField> modelFields, final Map<String, ?> fieldValues,
            final String operator, final List<? super EntityConditionParam> entityConditionParams)
    {
        if (modelFields == null || modelFields.isEmpty()) {
            return "";
        }

        final StringBuilder returnString = new StringBuilder();
        final Iterator<ModelField> iter = modelFields.iterator();

        while (iter.hasNext()) {
            final ModelField modelField = iter.next();

            returnString.append(modelField.getColName());
            final Object fieldValue = fieldValues.get(modelField.getName());

            if (fieldValue == null) {
                returnString.append(" IS NULL");
            } else {
                returnString.append("=?");
                if (entityConditionParams != null) {
                    entityConditionParams.add(new EntityConditionParam(modelField, fieldValue));
                }
            }

            if (iter.hasNext()) {
                returnString.append(' ');
                returnString.append(operator);
                returnString.append(' ');
            }
        }

        return returnString.toString();
    }

    public static String makeWhereClause(ModelEntity modelEntity, List<ModelField> modelFields, Map<String, ?> fields, String operator, String joinStyle) throws GenericEntityException {
        StringBuilder whereString = new StringBuilder("");

        if (modelFields != null && modelFields.size() > 0) {
            whereString.append(makeWhereStringFromFields(modelFields, fields, "AND"));
        }

        String viewClause = makeViewWhereClause(modelEntity, joinStyle);

        if (viewClause.length() > 0) {
            if (whereString.length() > 0) {
                whereString.append(' ');
                whereString.append(operator);
                whereString.append(' ');
            }

            whereString.append(viewClause);
        }

        if (whereString.length() > 0) {
            return " WHERE " + whereString.toString();
        }

        return "";
    }

    public static String makeViewWhereClause(ModelEntity modelEntity, String joinStyle) throws GenericEntityException {
        if (modelEntity instanceof ModelViewEntity) {
            StringBuilder whereString = new StringBuilder("");
            ModelViewEntity modelViewEntity = (ModelViewEntity) modelEntity;

            if ("ansi".equals(joinStyle)) {
                // nothing to do here, all done in the JOIN clauses
            } else if ("theta-oracle".equals(joinStyle) || "theta-mssql".equals(joinStyle)) {
                boolean isOracleStyle = "theta-oracle".equals(joinStyle);
                boolean isMssqlStyle = "theta-mssql".equals(joinStyle);

                for (int i = 0; i < modelViewEntity.getViewLinksSize(); i++) {
                    ModelViewEntity.ModelViewLink viewLink = modelViewEntity.getViewLink(i);

                    ModelEntity linkEntity = modelViewEntity.getMemberModelEntity(viewLink.getEntityAlias());
                    ModelEntity relLinkEntity = modelViewEntity.getMemberModelEntity(viewLink.getRelEntityAlias());
                    
                    if (linkEntity == null) {
                        throw new GenericEntityException("Link entity not found with alias: " + viewLink.getEntityAlias() + " for entity: " + modelViewEntity.getEntityName());
                    }

                    if (relLinkEntity == null) {
                        throw new GenericEntityException("Rel-Link entity not found with alias: " + viewLink.getRelEntityAlias() + " for entity: " + modelViewEntity.getEntityName());
                    }

                    // ModelViewEntity.ModelMemberEntity linkMemberEntity = modelViewEntity.getMemberModelMemberEntity(viewLink.getEntityAlias());
                    // ModelViewEntity.ModelMemberEntity relLinkMemberEntity = modelViewEntity.getMemberModelMemberEntity(viewLink.getRelEntityAlias());

                    for (int j = 0; j < viewLink.getKeyMapsSize(); j++) {
                        ModelKeyMap keyMap = viewLink.getKeyMap(j);
                        ModelField linkField = linkEntity.getField(keyMap.getFieldName());
                        ModelField relLinkField = relLinkEntity.getField(keyMap.getRelFieldName());

                        if (whereString.length() > 0) {
                            whereString.append(" AND ");
                        }
                        whereString.append(viewLink.getEntityAlias());
                        whereString.append(".");
                        whereString.append(filterColName(linkField.getColName()));

                        //We throw an exception because we didn't implement this for theta joins. This should not be
                        //an issue because we do not support Oracle 8i or MSSQL pre 2000.
                        if (viewLink.isRelOptional() && keyMap.getConstValue().length() > 0) {
                            throw new GenericEntityException("Constant join arguments not supported for '" + joinStyle + "'.");
                        }

                        // check to see whether the left or right members are optional, if so:
                        // oracle: use the (+) on the optional side
                        // mssql: use the * on the required side

                        // NOTE: not testing if original table is optional, ONLY if related table is optional; otherwise things get really ugly...
                        // if (isOracleStyle && linkMemberEntity.getOptional()) whereString.append(" (+) ");
                        if (isMssqlStyle && viewLink.isRelOptional()) whereString.append("*");
                        whereString.append("=");
                        // if (isMssqlStyle && linkMemberEntity.getOptional()) whereString.append("*");
                        if (isOracleStyle && viewLink.isRelOptional()) whereString.append(" (+) ");

                        whereString.append(viewLink.getRelEntityAlias());
                        whereString.append(".");
                        whereString.append(filterColName(relLinkField.getColName()));
                   }
                }
            } else {
                throw new GenericModelException("The join-style " + joinStyle + " is not yet supported");
            }

            if (whereString.length() > 0) {
                return "(" + whereString.toString() + ")";
            }
        }
        return "";
    }

    public static String makeOrderByClause(ModelEntity modelEntity, List<String> orderBy, DatasourceInfo datasourceInfo) {
        return makeOrderByClause(modelEntity, orderBy, false, datasourceInfo);
    }

    public static String makeOrderByClause(ModelEntity modelEntity, List<String> orderBy, boolean includeTablenamePrefix, DatasourceInfo datasourceInfo) {
        StringBuilder sql = new StringBuilder("");
        String fieldPrefix = includeTablenamePrefix ? (modelEntity.getTableName(datasourceInfo) + ".") : "";

        if (orderBy != null && orderBy.size() > 0) {
            if (Debug.verboseOn()) Debug.logVerbose("Order by list contains: " + orderBy.size() + " entries.", module);
            List<String> orderByStrings = new LinkedList<String>();

            for (String keyName : orderBy) {
                String ext = null;

                // check for ASC/DESC
                int spaceIdx = keyName.indexOf(" ");

                if (spaceIdx > 0) {
                    ext = keyName.substring(spaceIdx);
                    keyName = keyName.substring(0, spaceIdx);
                }
                // optional way -/+
                if (keyName.startsWith("-") || keyName.startsWith("+")) {
                    ext = keyName.startsWith("-") ? " DESC" : " ASC";
                    keyName = keyName.substring(1);
                }

                for (int fi = 0; fi < modelEntity.getFieldsSize(); fi++) {
                    ModelField curField = modelEntity.getField(fi);

                    if (curField.getName().equals(keyName)) {
                        if (ext != null)
                            orderByStrings.add(fieldPrefix + curField.getColName() + ext);
                        else
                            orderByStrings.add(fieldPrefix + curField.getColName());
                    }
                }
            }

            if (orderByStrings.size() > 0) {
                sql.append(" ORDER BY ");

                Iterator<String> iter = orderByStrings.iterator();

                while (iter.hasNext()) {
                    String curString = iter.next();

                    sql.append(curString);
                    if (iter.hasNext())
                        sql.append(", ");
                }
            }
        }
        if (Debug.verboseOn()) Debug.logVerbose("makeOrderByClause: " + sql.toString(), module);
        return sql.toString();
    }

    public static String makeViewTable(ModelEntity modelEntity, DatasourceInfo datasourceInfo) throws GenericEntityException {
        if (modelEntity instanceof ModelViewEntity) {
            StringBuilder sql = new StringBuilder("(SELECT ");
            List<ModelField> fields = modelEntity.getFieldsCopy();
            if (fields.size() > 0) {
                String colname = fields.get(0).getColName();
                sql.append(colname);
                sql.append(" AS ");
                sql.append(filterColName(colname));
                for (int i = 1; i < fields.size(); i++) {
                    colname = fields.get(i).getColName();
                    sql.append(", ");
                    sql.append(colname);
                    sql.append(" AS ");
                    sql.append(filterColName(colname));
                }
            }
            sql.append(makeFromClause(modelEntity, datasourceInfo));
            sql.append(makeViewWhereClause(modelEntity, datasourceInfo.getJoinStyle()));
            ModelViewEntity modelViewEntity = (ModelViewEntity)modelEntity;
            String groupByString = modelViewEntity.colNameString(modelViewEntity.getGroupBysCopy(), ", ", "");
            if (UtilValidate.isNotEmpty(groupByString)) {
                sql.append(" GROUP BY ");
                sql.append(groupByString);
            }
            
            sql.append(")");
            return sql.toString();
        } else {
            return modelEntity.getTableName(datasourceInfo);
        }
    }

    public static String filterColName(String colName) {
        return colName.replace('.', '_').replace('(','_').replace(')','_');
    }

    /* ====================================================================== */

    /* ====================================================================== */

    /**
     *  The elements (ModelFields) of the list are bound to an SQL statement
     *  (SQL-Processor)
     *
     * @param sqlP
     * @param list
     * @param entity
     * @param modelFieldTypeReader
     * @throws GenericEntityException
     */
    public static void setValues(final SQLProcessor sqlP, final List<ModelField> list, final GenericEntity entity,
            final ModelFieldTypeReader modelFieldTypeReader)
        throws GenericEntityException
    {
        for (final ModelField curField : list) {
            setValue(sqlP, curField, entity, modelFieldTypeReader);
        }
    }

    /**
     *  The elements (ModelFields) of the list are bound to an SQL statement
     *  (SQL-Processor), but values must not be null.
     *
     * @param sqlP
     * @param list
     * @param dummyValue
     * @param modelFieldTypeReader
     * @throws GenericEntityException
     */
    public static void setValuesWhereClause(final SQLProcessor sqlP, final List<ModelField> list,
            final GenericValue dummyValue, final ModelFieldTypeReader modelFieldTypeReader)
        throws GenericEntityException
    {
        for (final ModelField curField : list) {
            // for where clause variables only setValue if not null...
            if (dummyValue.get(curField.getName()) != null) {
                setValue(sqlP, curField, dummyValue, modelFieldTypeReader);
            }
        }
    }

    /**
     *  Get all primary keys from the model entity and bind their values
     *  to the an SQL statement (SQL-Processor)
     *
     * @param sqlP
     * @param modelEntity
     * @param entity
     * @param modelFieldTypeReader
     * @throws GenericEntityException
     */
    public static void setPkValues(SQLProcessor sqlP, ModelEntity modelEntity, GenericEntity entity, ModelFieldTypeReader modelFieldTypeReader) throws GenericEntityException {
        for (int j = 0; j < modelEntity.getPksSize(); j++) {
            ModelField curField = modelEntity.getPk(j);

            // for where clause variables only setValue if not null...
            if (entity.dangerousGetNoCheckButFast(curField) != null) {
                setValue(sqlP, curField, entity, modelFieldTypeReader);
            }
        }
    }

    public static void getValue(ResultSet rs, int ind, ModelField curField, GenericEntity entity, ModelFieldTypeReader modelFieldTypeReader) throws GenericEntityException {
        ModelFieldType mft = modelFieldTypeReader.getModelFieldType(curField.getType());

        if (mft == null) {
            throw new GenericModelException("definition fieldType " + curField.getType() + " not found, cannot getValue for field " +
                    entity.getEntityName() + "." + curField.getName() + ".");
        }
        String fieldType = mft.getJavaType();

        try {
            // checking to see if the object is null is really only necessary for the numbers
            int typeValue = getType(fieldType);

            if (typeValue <= 4 || typeValue == 10 || typeValue == 11) {
                switch (typeValue) {
                case 1:
                    entity.dangerousSetNoCheckButFast(curField, rs.getString(ind));
                    break;

                case 2:
                    entity.dangerousSetNoCheckButFast(curField, rs.getTimestamp(ind));
                    break;

                case 3:
                    entity.dangerousSetNoCheckButFast(curField, rs.getTime(ind));
                    break;

                case 4:
                    entity.dangerousSetNoCheckButFast(curField, rs.getDate(ind));
                    break;

                case 10:
                    Object obj = null;
                    Blob blobLocator = rs.getBlob(ind);
                    if (blobLocator != null && blobLocator.length() > 0)
                    {
                        InputStream binaryInput = blobLocator.getBinaryStream();
                        if (null != binaryInput) {
                            try {
                                ObjectInputStream in = new ObjectInputStream(binaryInput);
                                obj = in.readObject();
                                in.close();
                            } catch (IOException ex) {
                                throw new GenericDataSourceException("Unable to read BLOB data from input stream: ", ex);
                            } catch (ClassNotFoundException ex) {
                                throw new GenericDataSourceException("Class not found: Unable to cast BLOB data to a Java object: ", ex);
                            }
                        }
                        entity.dangerousSetNoCheckButFast(curField, obj);
                    }
                    break;
                case 11:
                    entity.dangerousSetNoCheckButFast(curField, rs.getBlob(ind));
                    break;
                case 12:
                    entity.dangerousSetNoCheckButFast(curField, rs.getClob(ind));
                    break;
                }
            } else {
                switch (typeValue) {
                case 5:
                    int intValue = rs.getInt(ind);

                    if (rs.wasNull()) {
                        entity.dangerousSetNoCheckButFast(curField, null);
                    } else {
                        entity.dangerousSetNoCheckButFast(curField, intValue);
                    }
                    break;

                case 6:
                    long longValue = rs.getLong(ind);

                    if (rs.wasNull()) {
                        entity.dangerousSetNoCheckButFast(curField, null);
                    } else {
                        entity.dangerousSetNoCheckButFast(curField, longValue);
                    }
                    break;

                case 7:
                    float floatValue = rs.getFloat(ind);

                    if (rs.wasNull()) {
                        entity.dangerousSetNoCheckButFast(curField, null);
                    } else {
                        entity.dangerousSetNoCheckButFast(curField, floatValue);
                    }
                    break;

                case 8:
                    double doubleValue = rs.getDouble(ind);

                    if (rs.wasNull()) {
                        entity.dangerousSetNoCheckButFast(curField, null);
                    } else {
                        entity.dangerousSetNoCheckButFast(curField, doubleValue);
                    }
                    break;

                case BOOLEAN:
                    boolean booleanValue = rs.getBoolean(ind);

                    if (rs.wasNull()) {
                        entity.dangerousSetNoCheckButFast(curField, null);
                    } else {
                        entity.dangerousSetNoCheckButFast(curField, booleanValue);
                    }
                    break;
                }
            }
        } catch (SQLException sqle) {
            throw new GenericDataSourceException("SQL Exception while getting value: ", sqle);
        }
    }

    public static void setValue(SQLProcessor sqlP, ModelField modelField, GenericEntity entity, ModelFieldTypeReader modelFieldTypeReader) throws GenericEntityException {
        Object fieldValue = entity.dangerousGetNoCheckButFast(modelField);

        setValue(sqlP, modelField, entity.getEntityName(), fieldValue, modelFieldTypeReader);
    }

    public static void setValue(SQLProcessor sqlP, ModelField modelField, String entityName, Object fieldValue, ModelFieldTypeReader modelFieldTypeReader) throws GenericEntityException {
        ModelFieldType mft = modelFieldTypeReader.getModelFieldType(modelField.getType());

        if (mft == null) {
            throw new GenericModelException("GenericDAO.getValue: definition fieldType " + modelField.getType() + " not found, cannot setValue for field " +
                    entityName + "." + modelField.getName() + ".");
        }

        String fieldType = mft.getJavaType();

        if (fieldValue != null) {
            Class<?> fieldClass = fieldValue.getClass();
            String fieldClassName = fieldClass.getName();

            if (!fieldClassName.equals(mft.getJavaType()) && !fieldClassName.contains(mft.getJavaType()) && !"java.lang.Object".equals(mft.getJavaType())) {
                // this is only an info level message because under normal operation for most JDBC 
                // drivers this will be okay, but if not then the JDBC driver will throw an exception
                // and when lower debug levels are on this should help give more info on what happened
                if (Debug.verboseOn()) Debug.logVerbose("type of field " + entityName + "." + modelField.getName() +
                        " is " + fieldClassName + ", was expecting " + mft.getJavaType() + "; this may " +
                        "indicate an error in the configuration or in the class, and may result " +
                        "in an SQL-Java data conversion error. Will use the real field type: " +
                        fieldClassName + ", not the definition.", module);
                fieldType = fieldClassName;
            }
        }

        try {
            int typeValue = getType(fieldType);

            switch (typeValue) {
            case 1:
                sqlP.setValue((String) fieldValue);
                break;

            case 2:
                sqlP.setValue((java.sql.Timestamp) fieldValue);
                break;

            case 3:
                sqlP.setValue((java.sql.Time) fieldValue);
                break;

            case 4:
                sqlP.setValue((java.sql.Date) fieldValue);
                break;

            case 5:
                sqlP.setValue((java.lang.Integer) fieldValue);
                break;

            case 6:
                sqlP.setValue((java.lang.Long) fieldValue);
                break;

            case 7:
                sqlP.setValue((java.lang.Float) fieldValue);
                break;

            case 8:
                sqlP.setValue((java.lang.Double) fieldValue);
                break;

            case BOOLEAN:
                sqlP.setValue((java.lang.Boolean) fieldValue);
                break;

            case 10:
                sqlP.setBinaryStream(fieldValue);
                break;
                
            case 11:
                sqlP.setValue((java.sql.Blob) fieldValue);
                break;
            
            case 12:
                sqlP.setValue((java.sql.Clob) fieldValue);
                break;
            }
        } catch (SQLException sqle) {
            throw new GenericDataSourceException("SQL Exception while setting value: ", sqle);
        }
    }

    protected static HashMap<String, Integer> fieldTypeMap = new HashMap<String, Integer>();
    static {
        fieldTypeMap.put("java.lang.String", 1);
        fieldTypeMap.put("String", 1);
        fieldTypeMap.put("java.sql.Timestamp", 2);
        fieldTypeMap.put("Timestamp", 2);
        fieldTypeMap.put("java.sql.Time", 3);
        fieldTypeMap.put("Time", 3);
        fieldTypeMap.put("java.sql.Date", 4);
        fieldTypeMap.put("Date", 4);
        fieldTypeMap.put("java.lang.Integer", 5);
        fieldTypeMap.put("Integer", 5);
        fieldTypeMap.put("java.lang.Long", 6);
        fieldTypeMap.put("Long", 6);
        fieldTypeMap.put("java.lang.Float", 7);
        fieldTypeMap.put("Float", 7);
        fieldTypeMap.put("java.lang.Double", 8);
        fieldTypeMap.put("Double", 8);
        fieldTypeMap.put("java.lang.Boolean", BOOLEAN);
        fieldTypeMap.put("Boolean", BOOLEAN);
        fieldTypeMap.put("java.lang.Object", 10);
        fieldTypeMap.put("Object", 10);
        fieldTypeMap.put("java.sql.Blob", 11);
        fieldTypeMap.put("Blob", 11);
        fieldTypeMap.put("java.sql.Clob", 12);
        fieldTypeMap.put("Clob", 12);
    }

    public static int getType(String fieldType) throws GenericNotImplementedException {
        Integer val = fieldTypeMap.get(fieldType);

        if (val == null) {
            throw new GenericNotImplementedException("Java type " + fieldType + " not currently supported. Sorry.");
        }
        return val;
    }
}
