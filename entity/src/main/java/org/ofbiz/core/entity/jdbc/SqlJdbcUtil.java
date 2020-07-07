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

import com.google.common.collect.ImmutableMap;
import org.ofbiz.core.entity.EntityConditionParam;
import org.ofbiz.core.entity.GenericDAO;
import org.ofbiz.core.entity.GenericDataSourceException;
import org.ofbiz.core.entity.GenericEntity;
import org.ofbiz.core.entity.GenericEntityException;
import org.ofbiz.core.entity.GenericModelException;
import org.ofbiz.core.entity.GenericNotImplementedException;
import org.ofbiz.core.entity.GenericValue;
import org.ofbiz.core.entity.config.DatasourceInfo;
import org.ofbiz.core.entity.jdbc.sql.escape.SqlEscapeHelper;
import org.ofbiz.core.entity.model.ModelEntity;
import org.ofbiz.core.entity.model.ModelField;
import org.ofbiz.core.entity.model.ModelFieldType;
import org.ofbiz.core.entity.model.ModelFieldTypeReader;
import org.ofbiz.core.entity.model.ModelKeyMap;
import org.ofbiz.core.entity.model.ModelViewEntity;
import org.ofbiz.core.util.Debug;
import org.ofbiz.core.util.UtilValidate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.ofbiz.core.entity.jdbc.SerializationUtil.deserialize;


/**
 * GenericDAO Utility methods for general tasks
 *
 * @author <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 * @author <a href="mailto:chris_maurer@altavista.com">Chris Maurer</a>
 * @author <a href="mailto:jaz@ofbiz.org">Andy Zeneski</a>
 * @author <a href="mailto:jdonnerstag@eds.de">Juergen Donnerstag</a>
 * @author <a href="mailto:peterm@miraculum.com">Peter Moon</a>
 * @version $Revision: 1.2 $
 * @since 2.0
 */
public class SqlJdbcUtil {

    public static final String module = GenericDAO.class.getName();

    /**
     * Indicates whether the given field type represents a Boolean field.
     * This convenience method is exactly equivalent to
     * {@link FieldType#matches(String) FieldType.BOOLEAN.matches(fieldType)}
     *
     * @param fieldType the field type to check
     * @return see above
     */
    public static boolean isBoolean(final String fieldType) {
        return FieldType.BOOLEAN.matches(fieldType);
    }

    /**
     * Makes the FROM clause and when necessary the JOIN clause(s) as well
     */
    public static String makeFromClause(ModelEntity modelEntity, DatasourceInfo datasourceInfo,
                                        SqlEscapeHelper sqlEscapeHelper) throws GenericEntityException {
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
                        restOfStatement.append(makeViewTable(linkEntity, datasourceInfo, sqlEscapeHelper));
                        restOfStatement.append(' ');
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

                    restOfStatement.append(makeViewTable(relLinkEntity, datasourceInfo, sqlEscapeHelper));
                    restOfStatement.append(' ');
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
                        condBuffer.append('.');
                        condBuffer.append(filterColName(sqlEscapeHelper.escapeColumn(linkField.getColName())));

                        condBuffer.append(" = ");

                        if (constValue.length() > 0) {
                            // Quoting is not handled here because ModelKeyMap's constructor guards us against
                            // single-quotes in the value.
                            condBuffer.append('\'');
                            condBuffer.append(constValue);
                            condBuffer.append('\'');
                        } else {
                            ModelField relLinkField = relLinkEntity.getField(keyMap.getRelFieldName());
                            condBuffer.append(viewLink.getRelEntityAlias());
                            condBuffer.append('.');
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

                        sql.append(makeViewTable(fromEntity, datasourceInfo, sqlEscapeHelper));
                        sql.append(' ');
                        sql.append(entry.getKey());
                    }
                }


            } else if ("theta-oracle".equals(datasourceInfo.getJoinStyle()) || "theta-mssql".equals(datasourceInfo.getJoinStyle())) {
                // FROM clause
                Iterator<? extends Map.Entry<String, ?>> meIter = modelViewEntity.getMemberModelMemberEntities().entrySet().iterator();

                while (meIter.hasNext()) {
                    Map.Entry<String, ?> entry = meIter.next();
                    ModelEntity fromEntity = modelViewEntity.getMemberModelEntity(entry.getKey());

                    sql.append(makeViewTable(fromEntity, datasourceInfo, sqlEscapeHelper));
                    sql.append(' ');
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
     * @param operator    the operator to insert between each column condition in the returned WHERE string (typically
     *                    "AND" or "OR")
     * @return an empty string if the given list of fields is null or empty, otherwise a string like
     * "first_name IS NULL OR last_name=?"
     */
    public static String makeWhereStringFromFields(
            final List<ModelField> modelFields, final Map<String, ?> fieldValues, final String operator, final SqlEscapeHelper sqlEscapeHelper) {
        return makeWhereStringFromFields(modelFields, fieldValues, operator, null, sqlEscapeHelper);
    }

    public static int countWhereStringParametersFromFields(final List<ModelField> modelFields, final Map<String, ?> fieldValues) {
        if (modelFields == null || modelFields.isEmpty()) {
            return 0;
        }

        int parameterCount = 0;
        for (ModelField modelField : modelFields) {
            final Object fieldValue = fieldValues.get(modelField.getName());
            if (fieldValue != null) //null fieldValue -> ... IS NULL
            {
                parameterCount++;
            }
        }

        return parameterCount;
    }

    /**
     * Makes a WHERE clause String with "<col name>=?" if not null or "<col name> IS NULL" if null, all separated by the
     * given operator.
     *
     * @param modelFields           the fields to include in the WHERE string (can be null)
     * @param fieldValues           any field values to be checked against non-null values; keys are field (not column) names
     * @param operator              the operator to insert between each column
     *                              condition in the returned WHERE string (typically "AND" or "OR")
     * @param entityConditionParams if not null, an element will be added to this list
     * @return an empty string if the given list of fieldValues is null or empty, otherwise a string like
     * "first_name IS NULL OR last_name=?"
     */
    public static String makeWhereStringFromFields(final List<ModelField> modelFields, final Map<String, ?> fieldValues,
                                                   final String operator, final List<? super EntityConditionParam> entityConditionParams,
                                                   SqlEscapeHelper sqlEscapeHelper) {
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

    public static String makeWhereClause(ModelEntity modelEntity, List<ModelField> modelFields, Map<String, ?> fields, String operator, String joinStyle,
                                         SqlEscapeHelper sqlEscapeHelper) throws GenericEntityException {
        StringBuilder whereString = new StringBuilder("");

        if (modelFields != null && modelFields.size() > 0) {
            whereString.append(makeWhereStringFromFields(modelFields, fields, "AND", sqlEscapeHelper));
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
                        whereString.append('.');
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
                        if (isMssqlStyle && viewLink.isRelOptional()) whereString.append('*');
                        whereString.append('=');
                        // if (isMssqlStyle && linkMemberEntity.getOptional()) whereString.append("*");
                        if (isOracleStyle && viewLink.isRelOptional()) whereString.append(" (+) ");

                        whereString.append(viewLink.getRelEntityAlias());
                        whereString.append('.');
                        whereString.append(filterColName(relLinkField.getColName()));
                    }
                }
            } else {
                throw new GenericModelException("The join-style " + joinStyle + " is not yet supported");
            }

            if (whereString.length() > 0) {
                return '(' + whereString.toString() + ')';
            }
        }
        return "";
    }

    public static String makeOrderByClause(ModelEntity modelEntity, List<String> orderBy, DatasourceInfo datasourceInfo) {
        return makeOrderByClause(modelEntity, orderBy, false, datasourceInfo);
    }

    public static String makeOrderByClause(ModelEntity modelEntity, List<String> orderBy, boolean includeTablenamePrefix, DatasourceInfo datasourceInfo) {
        StringBuilder sql = new StringBuilder("");
        String fieldPrefix = includeTablenamePrefix ? (modelEntity.getTableName(datasourceInfo) + '.') : "";

        if (orderBy != null && orderBy.size() > 0) {
            if (Debug.verboseOn()) Debug.logVerbose("Order by list contains: " + orderBy.size() + " entries.", module);
            List<String> orderByStrings = new LinkedList<String>();

            for (String keyName : orderBy) {
                String ext = null;

                // check for ASC/DESC
                int spaceIdx = keyName.indexOf(' ');

                if (spaceIdx > 0) {
                    ext = keyName.substring(spaceIdx);
                    keyName = keyName.substring(0, spaceIdx);
                }
                // optional way -/+
                if (keyName.startsWith("-") || keyName.startsWith("+")) {
                    ext = (keyName.charAt(0) == '-') ? " DESC" : " ASC";
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
        if (Debug.verboseOn()) Debug.logVerbose("makeOrderByClause: " + sql, module);
        return sql.toString();
    }

    public static String makeViewTable(ModelEntity modelEntity, DatasourceInfo datasourceInfo,
                                       SqlEscapeHelper sqlEscapeHelper) throws GenericEntityException {
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
            sql.append(makeFromClause(modelEntity, datasourceInfo, sqlEscapeHelper));
            sql.append(makeViewWhereClause(modelEntity, datasourceInfo.getJoinStyle()));
            ModelViewEntity modelViewEntity = (ModelViewEntity) modelEntity;
            String groupByString = modelViewEntity.colNameString(modelViewEntity.getGroupBysCopy(), ", ", "", sqlEscapeHelper);
            if (UtilValidate.isNotEmpty(groupByString)) {
                sql.append(" GROUP BY ");
                sql.append(groupByString);
            }

            sql.append(')');
            return sql.toString();
        } else {
            return modelEntity.getTableName(datasourceInfo);
        }
    }

    public static String filterColName(String colName) {
        return colName.replace('.', '_').replace('(', '_').replace(')', '_');
    }

    /* ====================================================================== */

    /* ====================================================================== */

    /**
     * The elements (ModelFields) of the list are bound to an SQL statement
     * (SQL-Processor)
     *
     * @param sqlP
     * @param list
     * @param entity
     * @param modelFieldTypeReader
     * @throws GenericEntityException
     */
    public static void setValues(final SQLProcessor sqlP, final List<ModelField> list, final GenericEntity entity,
                                 final ModelFieldTypeReader modelFieldTypeReader)
            throws GenericEntityException {
        for (final ModelField curField : list) {
            setValue(sqlP, curField, entity, modelFieldTypeReader);
        }
    }

    /**
     * The elements (ModelFields) of the list are bound to an SQL statement
     * (SQL-Processor), but values must not be null.
     *
     * @param sqlP
     * @param list
     * @param dummyValue
     * @param modelFieldTypeReader
     * @throws GenericEntityException
     */
    public static void setValuesWhereClause(final SQLProcessor sqlP, final List<ModelField> list,
                                            final GenericValue dummyValue, final ModelFieldTypeReader modelFieldTypeReader)
            throws GenericEntityException {
        for (final ModelField curField : list) {
            // for where clause variables only setValue if not null...
            if (dummyValue.get(curField.getName()) != null) {
                setValue(sqlP, curField, dummyValue, modelFieldTypeReader);
            }
        }
    }

    /**
     * Get all primary keys from the model entity and bind their values
     * to the an SQL statement (SQL-Processor)
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
                    entity.getEntityName() + '.' + curField.getName() + '.');
        }
        String fieldType = mft.getJavaType();

        try {
            // checking to see if the object is null is really only necessary for the numbers
            FieldType type = getFieldType(fieldType);

            switch (type) {
                case STRING:
                    entity.dangerousSetNoCheckButFast(curField, rs.getString(ind));
                    break;

                case TIMESTAMP:
                    entity.dangerousSetNoCheckButFast(curField, rs.getTimestamp(ind));
                    break;

                case TIME:
                    entity.dangerousSetNoCheckButFast(curField, rs.getTime(ind));
                    break;

                case DATE:
                    entity.dangerousSetNoCheckButFast(curField, rs.getDate(ind));
                    break;

                case INTEGER:
                    int intValue = rs.getInt(ind);

                    if (rs.wasNull()) {
                        entity.dangerousSetNoCheckButFast(curField, null);
                    } else {
                        entity.dangerousSetNoCheckButFast(curField, intValue);
                    }
                    break;

                case LONG:
                    long longValue = rs.getLong(ind);

                    if (rs.wasNull()) {
                        entity.dangerousSetNoCheckButFast(curField, null);
                    } else {
                        entity.dangerousSetNoCheckButFast(curField, longValue);
                    }
                    break;

                case FLOAT:
                    float floatValue = rs.getFloat(ind);

                    if (rs.wasNull()) {
                        entity.dangerousSetNoCheckButFast(curField, null);
                    } else {
                        entity.dangerousSetNoCheckButFast(curField, floatValue);
                    }
                    break;

                case DOUBLE:
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

                case OBJECT:
                    if (isByteArrayType(mft)) {
                        entity.dangerousSetNoCheckButFast(curField, getByteArrayAsObject(rs, ind));
                    } else {
                        entity.dangerousSetNoCheckButFast(curField, getBlobAsObject(rs, ind));
                    }
                    break;

                case BLOB:
                    entity.dangerousSetNoCheckButFast(curField, rs.getBlob(ind));
                    break;

                case CLOB:
                    entity.dangerousSetNoCheckButFast(curField, rs.getClob(ind));
                    break;

                case BYTE_ARRAY:
                    if (isByteArrayType(mft)) {
                        entity.dangerousSetNoCheckButFast(curField, rs.getBytes(ind));
                    } else {
                        entity.dangerousSetNoCheckButFast(curField, getBlobAsByteArray(rs, ind));
                    }
                    break;
            }
        } catch (SQLException sqle) {
            throw new GenericDataSourceException("SQL Exception while getting value: ", sqle);
        }
    }

    @Nullable
    private static Object getByteArrayAsObject(final ResultSet rs, final int ind)
            throws SQLException, GenericDataSourceException {
        final byte[] bytes = rs.getBytes(ind);
        return (bytes != null && bytes.length > 0) ? deserialize(new ByteArrayInputStream(bytes)) : null;
    }

    @Nullable
    private static Object getBlobAsObject(final ResultSet rs, final int ind)
            throws SQLException, GenericDataSourceException {
        final Blob blob = rs.getBlob(ind);
        if (blob == null || blob.length() <= 0L) {
            return null;
        }
        final InputStream is = blob.getBinaryStream();
        try {
            return deserialize(is);
        } finally {
            try {
                is.close();
            } catch (IOException ioe) {
                Debug.logWarning(ioe);
            }
        }
    }

    @Nullable
    private static byte[] getBlobAsByteArray(final ResultSet rs, final int ind) throws SQLException {
        final Blob blob = rs.getBlob(ind);
        if (blob == null) {
            return null;
        }

        final long len = blob.length();
        if (len <= 0L) {
            return null;
        }

        if (blob.length() > Integer.MAX_VALUE) {
            throw new SQLException("BLOB exceeds Integer.MAX_VALUE in length; cannot be retrieved as byte array");
        }

        // Yes, the starting position really is 1L.  Because JDBC.  *sigh*
        return blob.getBytes(1L, (int) len);
    }

    public static void setValue(SQLProcessor sqlP, ModelField modelField, GenericEntity entity, ModelFieldTypeReader modelFieldTypeReader) throws GenericEntityException {
        Object fieldValue = entity.dangerousGetNoCheckButFast(modelField);

        setValue(sqlP, modelField, entity.getEntityName(), fieldValue, modelFieldTypeReader);
    }

    public static void setValue(SQLProcessor sqlP, ModelField modelField, String entityName, Object fieldValue, ModelFieldTypeReader modelFieldTypeReader) throws GenericEntityException {
        ModelFieldType mft = modelFieldTypeReader.getModelFieldType(modelField.getType());

        if (mft == null) {
            throw new GenericModelException("GenericDAO.getValue: definition fieldType " + modelField.getType() + " not found, cannot setValue for field " +
                    entityName + '.' + modelField.getName() + '.');
        }

        String fieldType = mft.getJavaType();

        if (fieldValue != null) {
            Class<?> fieldClass = fieldValue.getClass();
            String fieldClassName = fieldClass.getName();

            if (!fieldClassName.equals(mft.getJavaType()) && !fieldClassName.contains(mft.getJavaType()) && !"java.lang.Object".equals(mft.getJavaType())) {
                // this is only an info level message because under normal operation for most JDBC
                // drivers this will be okay, but if not then the JDBC driver will throw an exception
                // and when lower debug levels are on this should help give more info on what happened
                if (Debug.verboseOn()) Debug.logVerbose("type of field " + entityName + '.' + modelField.getName() +
                        " is " + fieldClassName + ", was expecting " + mft.getJavaType() + "; this may " +
                        "indicate an error in the configuration or in the class, and may result " +
                        "in an SQL-Java data conversion error. Will use the real field type: " +
                        fieldClassName + ", not the definition.", module);
                fieldType = fieldClassName;
            }
        }

        try {
            FieldType type = getFieldType(fieldType);

            switch (type) {
                case STRING:
                    sqlP.setValue((String) fieldValue);
                    break;

                case TIMESTAMP:
                    sqlP.setValue((Timestamp) fieldValue);
                    break;

                case TIME:
                    sqlP.setValue((Time) fieldValue);
                    break;

                case DATE:
                    sqlP.setValue((Date) fieldValue);
                    break;

                case INTEGER:
                    sqlP.setValue((Integer) fieldValue);
                    break;

                case LONG:
                    sqlP.setValue((Long) fieldValue);
                    break;

                case FLOAT:
                    sqlP.setValue((Float) fieldValue);
                    break;

                case DOUBLE:
                    sqlP.setValue((Double) fieldValue);
                    break;

                case BOOLEAN:
                    sqlP.setValue((Boolean) fieldValue);
                    break;

                case OBJECT:
                    if (isByteArrayType(mft)) {
                        sqlP.setByteArrayData(fieldValue);
                    } else {
                        sqlP.setBinaryStream(fieldValue);
                    }
                    break;

                case BLOB:
                    if (fieldValue == null && isByteArrayType(mft)) {
                        sqlP.setByteArrayData(null);
                    } else if (fieldValue instanceof Blob) {
                        sqlP.setValue((Blob) fieldValue);
                    } else {
                        sqlP.setBlob((byte[]) fieldValue);
                    }
                    break;

                case CLOB:
                    if (fieldValue instanceof Clob) {
                        sqlP.setValue((Clob) fieldValue);
                    } else {
                        sqlP.setValue((String) fieldValue);
                    }
                    break;

                case BYTE_ARRAY:
                    if (isByteArrayType(mft)) {
                        sqlP.setByteArray((byte[]) fieldValue);
                    } else {
                        sqlP.setBlob((byte[]) fieldValue);
                    }
                    break;

            }
        } catch (SQLException sqle) {
            throw new GenericDataSourceException("SQL Exception while setting value: ", sqle);
        }
    }

    private static boolean isByteArrayType(ModelFieldType mft) {
        final String type = mft.getSqlType();
        return "BYTEA".equals(type)
                || "IMAGE".equals(type)
                || "OTHER".equals(type);
    }

    private static final Map<String, FieldType> JAVA_TYPE_MAP = FieldType.buildJavaTypeMap();

    public static int getType(String javaType) throws GenericNotImplementedException {
        final FieldType type = JAVA_TYPE_MAP.get(javaType);
        if (type == null) {
            throw new IllegalArgumentException("Java type " + javaType + " not currently supported. Sorry.");
        }
        return type.getOldTypeNumber();
    }

    /**
     * Does the same thing as {@link #getType(String)}, except that it returns an {@code enum} type instead
     * of a magic number and doesn't throw a checked exception.
     *
     * @param javaType the java type to resolve to a field type
     * @return the matching field type
     * @throws IllegalArgumentException if the java class type is unsupported
     * @since 1.1.0
     */
    @Nonnull
    public static FieldType getFieldType(String javaType) {
        final FieldType type = JAVA_TYPE_MAP.get(javaType);
        if (type == null) {
            throw new IllegalArgumentException("Java type " + javaType + " not currently supported. Sorry.");
        }
        return type;
    }

    /**
     * An enumeration of the various data types supported by the entity engine.
     * <p>
     * Each field type expresses the type of data that the {@link GenericEntity#get(String) get} and
     * {@link GenericEntity#set(String, Object)} methods will expect to work with.  For example, if
     * the field type is {@link #TIMESTAMP}, then it expects {@code java.sql.Timestamp} values to be
     * provided to {@code set} and will use {@link ResultSet#getTimestamp(int)} to obtain the values
     * when reading them from the database.
     * </p><p>
     * Exactly how these map to actual database types is defined by the field types XML for that
     * particular database.  In short, the mapping process is that {@code entitymodel.xml} defines
     * the field and specifies its model type, which is something like {@code "long-varchar"}.
     * The {@code fieldtype-mydbtype.xml} file then maps this to a Java type and database type.
     * For example:
     * </p>
     * <code><pre>
     *     &lt;!-- From entitymodel.xml --&gt;
     *             &lt;field name="lowerUserName" col-name="lower_user_name" type="long-varchar" /&gt;
     *     &lt;!-- From fieldtype-mysql.xml --&gt;
     *             &lt;field-type-def type="long-varchar" sql-type="VARCHAR(255)" java-type="String" /&gt;
     * </pre></code>
     * <p>
     * So the field {@code "lowerUserName"} on this entity maps to the {@code "long-varchar"} {@link ModelFieldType}.
     * That returns {@code "VARCHAR(255)"} for {@link ModelFieldType#getSqlType()} and this is what gets used in
     * the database schema.  It returns {@code "String"} for {@link ModelFieldType#getJavaType()}, and
     * {@link #getFieldType(String)} maps that to {@link #STRING}.  This is what tells {@link GenericEntity}
     * and {@link SQLProcessor} to use {@link ResultSet#getString(int)} and
     * {@link PreparedStatement#setString(int, String)} to interact with the database storage and to use
     * {@code String} objects internally.
     * </p>
     * <p>
     * It's convoluted, especially when you realize that there are four different things you can mean when
     * talking about a field's "type", but it mostly works. :P
     * </p>
     *
     * @since 1.1.0
     */
    public enum FieldType {
        /**
         * The underlying field type is something like VARCHAR or TEXT that can hold variable-length
         * character data.
         */
        STRING(1, "String", "java.lang.String"),

        /**
         * The database field type maps to {@link Timestamp}.
         */
        TIMESTAMP(2, "Timestamp", "java.sql.Timestamp"),

        /**
         * The database field type maps to {@link Time}.
         */
        TIME(3, "Time", "java.sql.Time"),

        /**
         * The database field type maps to {@link Date}.
         */
        DATE(4, "Date", "java.sql.Date"),

        /**
         * The database field has an integer type with sufficient precision to hold {@code int} values.
         */
        INTEGER(5, "Integer", "java.lang.Integer"),

        /**
         * The database field has an integer type with sufficient precision to hold {@code long} values.
         */
        LONG(6, "Long", "java.lang.Long"),

        /**
         * The database field has a floating-point decimal type with sufficient precision to hold {@code float} values.
         */
        FLOAT(7, "Float", "java.lang.Float"),

        /**
         * The database field has a floating-point decimal type with sufficient precision to hold {@code double} values.
         */
        DOUBLE(8, "Double", "java.lang.Double"),

        /**
         * The database field can hold a boolean value.  On most databases that just means it is a {@code BOOLEAN}
         * column, but some databases do not have this type and emulate it with something else, such as a
         * {@code TINYINT} on MySQL.
         */
        BOOLEAN(9, "Boolean", "java.lang.Boolean"),

        /**
         * The database field holds serialized Object data.  The underlying database type is a BLOB on most
         * databases; however, it is {@code BYTEA} for Postgres and {@code IMAGE} for SqlServer.  The bytes
         * stored are the serialized form of the object assigned to the field, and the value is implicitly
         * deserialized when the value is read back in.  Classes are loaded using the default behaviour of
         * {@link ObjectInputStream#resolveClass(ObjectStreamClass)}, and no mechanism is exposed for changing
         * this behaviour.  If your field requires any kind of customized serialization, then {@link #BYTE_ARRAY}
         * should be preferred.
         */
        OBJECT(10, "Object", "java.lang.Object"),

        /**
         * The database field is a {@code CLOB} or whatever the nearest equivalent is.
         */
        CLOB(11, "Clob", "java.sql.Clob"),

        /**
         * The database field is a {@code BLOB} or whatever the nearest equivalent is.  BLOB field support
         * is untested and probably incomplete.
         */
        BLOB(12, "Blob", "java.sql.Blob"),

        /**
         * The database field holds arbitrary binary data.  The underlying database type is a BLOB on most
         * databases; however, it is {@code BYTEA} for Postgres and {@code IMAGE} for SqlServer.  Unlike
         * {@link #OBJECT}, this type does not perform any implicit serialization or deserialization of
         * its data.
         */
        BYTE_ARRAY(13, "byte[]", "[B");

        private final int oldTypeNumber;
        private final String[] javaTypes;

        // The array never escapes this class, so the warning is meaningless
        @SuppressWarnings("AssignmentToCollectionOrArrayFieldFromParameter")
        FieldType(final int oldTypeNumber, String... javaTypes) {
            this.oldTypeNumber = oldTypeNumber;
            this.javaTypes = javaTypes;
        }

        /**
         * Returns the old hardcoded type number that is associated with this field type, such as {@code 5} for
         * {@code INTEGER}.
         * <p>
         * The use of magic numbers to communicate this information is error-prone and heavily discouraged.  Stick
         * with the {@code FieldType} enum itself wherever possible.
         * </p>
         *
         * @return the old hardcoded type number that is associated with this field type.
         */
        public int getOldTypeNumber() {
            return oldTypeNumber;
        }

        /**
         * Returns {@code true} if the specified java type corresponds to this field type.
         * Calling {@code fieldType.matches(javaType)} is equivalent to {@code fieldType == getFieldType(javaType)},
         * except that it does not throw an exception when {@code javaType} is {@code null} or unrecognized.
         *
         * @param javaType the proposed java type
         * @return {@code true} if the specified java type corresponds to this field type; {@code false} otherwise.
         */
        public boolean matches(final String javaType) {
            for (String myJavaType : javaTypes) {
                if (myJavaType.equals(javaType)) {
                    return true;
                }
            }
            return false;
        }

        static Map<String, FieldType> buildJavaTypeMap() {
            final Map<Integer, FieldType> typeNumberMapping = new HashMap<Integer, FieldType>(64);
            final ImmutableMap.Builder<String, FieldType> map = ImmutableMap.builder();
            for (FieldType fieldType : FieldType.values()) {
                final FieldType collision = typeNumberMapping.put(fieldType.oldTypeNumber, fieldType);
                if (collision != null) {
                    throw new IllegalStateException("FieldType '" + fieldType + "' uses the same value that '" +
                            collision + "' uses for its old type number: " + fieldType.oldTypeNumber);
                }

                for (String javaType : fieldType.javaTypes) {
                    map.put(javaType, fieldType);
                }
            }
            return map.build();
        }
    }
}
