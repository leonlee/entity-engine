/*
 * $Id: GenericEntity.java,v 1.2 2005/04/02 09:30:55 sfarquhar Exp $
 *
 *  Copyright (c) 2002 The Open For Business Project - www.ofbiz.org
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


import com.google.common.collect.Maps;
import org.ofbiz.core.entity.jdbc.SqlJdbcUtil;
import org.ofbiz.core.entity.jdbc.SqlJdbcUtil.FieldType;
import org.ofbiz.core.entity.model.ModelEntity;
import org.ofbiz.core.entity.model.ModelField;
import org.ofbiz.core.entity.model.ModelFieldType;
import org.ofbiz.core.entity.model.ModelFieldTypeReader;
import org.ofbiz.core.util.Debug;
import org.ofbiz.core.util.UtilFormatOut;
import org.ofbiz.core.util.UtilValidate;
import org.ofbiz.core.util.UtilXml;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.PrintWriter;
import java.io.Serializable;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Observable;
import java.util.Set;
import java.util.TreeSet;

import static org.apache.commons.codec.binary.Base64.decodeBase64;
import static org.ofbiz.core.entity.jdbc.SerializationUtil.deserialize;
import static org.ofbiz.core.entity.jdbc.SerializationUtil.encodeBase64;
import static org.ofbiz.core.entity.jdbc.SerializationUtil.serialize;
import static org.ofbiz.core.entity.jdbc.SqlJdbcUtil.getFieldType;


/**
 * Generic Entity Value Object - Handles persistence for any defined entity.
 * <p>Note that this class extends <code>Observable</code> to achieve change notification for
 * <code>Observer</code>s. Whenever a field changes the name of the field will be passed to
 * the <code>notifyObservers()</code> method, and through that to the <code>update()</code> method of each
 * <code>Observer</code>.
 *
 *@author     <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 *@author     <a href="mailto:jaz@ofbiz.org">Andy Zeneski</a>
 *@created    Wed Aug 08 2001
 *@version    1.0
 */
public class GenericEntity extends Observable implements Map<String, Object>, Serializable, Comparable<GenericEntity>, Cloneable {

    /** Name of the GenericDelegator, used to re-get the GenericDelegator when deserialized */
    public String delegatorName = null;

    /** Reference to an instance of GenericDelegator used to do some basic operations on this entity value. If null various methods in this class will fail. This is automatically set by the GenericDelegator for all GenericValue objects instantiated through it. You may set this manually for objects you instantiate manually, but it is optional. */
    public transient GenericDelegator internalDelegator = null;

    /** Contains the fields for this entity.
     */
    protected Map<String, Object> fields;

    /** Contains the entityName of this entity, necessary for efficiency when creating EJBs */
    public String entityName = null;

    /** Contains the ModelEntity instance that represents the definition of this entity, not to be serialized */
    public transient ModelEntity modelEntity = null;

    /** Denotes whether or not this entity has been modified, or is known to be out of sync with the persistent record */
    public boolean modified = false;

    /** Creates new GenericEntity
     * @since 1.0.13
     */
    public GenericEntity(GenericDelegator delegator) {
        setDelegator(delegator);
        this.entityName = null;
        this.modelEntity = null;
        this.fields = new HashMap<String, Object>();
    }

    /** Creates new GenericEntity.
     * @since 1.0.13
     */
    public GenericEntity(GenericDelegator delegator, ModelEntity modelEntity) {
        setDelegator(delegator);
        if (modelEntity == null) throw new IllegalArgumentException("Cannont create a GenericEntity with a null modelEntity parameter");
        this.modelEntity = modelEntity;
        this.entityName = modelEntity.getEntityName();
        this.fields = new HashMap<String, Object>();
    }

    /** Creates new GenericEntity from existing Map.
     * @since 1.0.13
     */
    public GenericEntity(GenericDelegator delegator, ModelEntity modelEntity, Map<String, ?> fields) {
        setDelegator(delegator);
        if (modelEntity == null) throw new IllegalArgumentException("Cannont create a GenericEntity with a null modelEntity parameter");
        this.modelEntity = modelEntity;
        this.entityName = modelEntity.getEntityName();
        this.fields = new HashMap<String, Object>();
        setFields(fields);
    }

    /** Creates new GenericEntity
     * @deprecated since 1.0.13 Use {@link #GenericEntity(GenericDelegator internalDelegator)}
     */
    public GenericEntity() {
        this.entityName = null;
        this.modelEntity = null;
        this.fields = new HashMap<String, Object>();
    }

    /** Creates new GenericEntity.
     * @deprecated since 1.0.13 Use {@link #GenericEntity(GenericDelegator internalDelegator, ModelEntity modelEntity)}
     */
    public GenericEntity(ModelEntity modelEntity) {
        if (modelEntity == null) throw new IllegalArgumentException("Cannont create a GenericEntity with a null modelEntity parameter");
        this.modelEntity = modelEntity;
        this.entityName = modelEntity.getEntityName();
        this.fields = new HashMap<String, Object>();
    }

    /** Creates new GenericEntity from existing Map.
     * @deprecated since 1.0.13 Use {@link #GenericEntity(GenericDelegator internalDelegator, ModelEntity modelEntity, Map fields)}
     */
    public GenericEntity(ModelEntity modelEntity, Map<String, ?> fields) {
        if (modelEntity == null) throw new IllegalArgumentException("Cannont create a GenericEntity with a null modelEntity parameter");
        this.modelEntity = modelEntity;
        this.entityName = modelEntity.getEntityName();
        this.fields = new HashMap<String, Object>();
        setFields(fields);
    }

    /** Copy Constructor: Creates new GenericEntity from existing GenericEntity */
    public GenericEntity(GenericEntity value) {
        this.entityName = value.modelEntity.getEntityName();
        this.modelEntity = value.modelEntity;
        this.fields = (value.fields == null ? new HashMap<String, Object>() : new HashMap<String, Object>(value.fields));
        this.delegatorName = value.delegatorName;
        this.internalDelegator = value.internalDelegator;
    }

    public boolean isModified() {
        return modified;
    }

    public String getEntityName() {
        return entityName;
    }

    public ModelEntity getModelEntity() {
        if (modelEntity == null) {
            if (entityName != null) modelEntity = this.getDelegator().getModelEntity(entityName);
            if (modelEntity == null) {
                throw new IllegalStateException("[GenericEntity.getModelEntity] could not find modelEntity for entityName " + entityName);
            }
        }
        return modelEntity;
    }

    /** Get the GenericDelegator instance that created this value object and that is responsible for it.
     *@return GenericDelegator object
     */
    public GenericDelegator getDelegator() {
        if (internalDelegator == null) {
            if (delegatorName != null) internalDelegator = GenericDelegator.getGenericDelegator(delegatorName);
            if (internalDelegator == null) {
                throw new IllegalStateException("[GenericEntity.getDelegator] could not find delegator with name " + delegatorName);
            }
        }
        return internalDelegator;
    }

    /** Set the GenericDelegator instance that created this value object and that is responsible for it. */
    public void setDelegator(final GenericDelegator internalDelegator) {
        if (internalDelegator == null) {
            return;
        }
        this.delegatorName = internalDelegator.getDelegatorName();
        this.internalDelegator = internalDelegator;
    }

    public Object get(final String name) {
        final Object value = fields.get(name);
        // We test for a valid field name after trying to retrieve the value, because this method is called a trillion times
        // and even a small increase in performance for this method is really worthwhile.
        if (value == null && getModelEntity().getField(name) == null) {
            throw new IllegalArgumentException("[GenericEntity.get] \"" + name + "\" is not a field of " + entityName);
        }
        return value;
    }

    /** Returns true if the entity contains all of the primary key fields, but NO others. */
    public boolean isPrimaryKey() {
        Set<String> fieldKeys = new TreeSet<String>(fields.keySet());

        for (int i = 0; i < getModelEntity().getPksSize(); i++) {
            if (!fieldKeys.contains(getModelEntity().getPk(i).getName())) return false;
            fieldKeys.remove(getModelEntity().getPk(i).getName());
        }
        return fieldKeys.isEmpty();
    }

    /** Returns true if the entity contains all of the primary key fields. */
    public boolean containsPrimaryKey() {
        Set<String> fieldKeys = new TreeSet<String>(fields.keySet());

        for (int i = 0; i < getModelEntity().getPksSize(); i++) {
            if (!fieldKeys.contains(getModelEntity().getPk(i).getName())) return false;
        }
        return true;
    }

    /** Sets the named field to the passed value, even if the value is null
     * @param name The field name to set
     * @param value The value to set
     */
    public void set(String name, Object value) {
        set(name, value, true);
    }

    /**
     * Sets the named field to the passed value. If value is null, it is only
     * set if the setIfNull parameter is true. This is useful because an update
     * will only set values that are included in the HashMap and will store null
     * values in the HashMap to the datastore. If a value is not in the HashMap,
     * it will be left unmodified in the datastore.
     *
     * @param field the name of the field to set; must be a valid field of this entity
     * @param value the value to set; if null, will only be stored if <code>setIfNull</code> is true
     * @param setIfNull specifies whether or not to set the value if it is null
     * @return the previous value of the given field (whether updated or not)
     */
    public Object set(final String field, final Object value, final boolean setIfNull) {
        final ModelField modelField = getModelEntity().getField(field);
        if (modelField == null) {
            throw new IllegalArgumentException("[GenericEntity.set] \"" + field + "\" is not a field of " + entityName);
        }
        if (value == null && !setIfNull) {
            // Don't modify the field, just return its existing value
            return fields.get(field);
        }
        final Object valueToPut = getValueToPut(value, modelField.getType());
        final Object previousValue = fields.put(field, valueToPut);
        modified = true;
        setChanged();
        notifyObservers(field);
        return previousValue;
    }

    private Object getValueToPut(final Object value, final String fieldType) {
        if (value instanceof Boolean) {
            final String javaType = getModelFieldType(fieldType).getJavaType();
            if (!SqlJdbcUtil.isBoolean(javaType)) {
                return (Boolean) value ? "Y" : "N";
            }
        }
        return value;
    }

    private ModelFieldType getModelFieldType(final String fieldType) {
        ModelFieldType type = null;
        try {
            type = getDelegator().getEntityFieldType(getModelEntity(), fieldType);
        } catch (GenericEntityException e) {
            Debug.logWarning(e);
        }
        if (type == null) {
            throw new IllegalArgumentException("Type " + fieldType + " not found");
        }
        return type;
    }

    public void dangerousSetNoCheckButFast(ModelField modelField, Object value) {
        this.fields.put(modelField.getName(), value);
    }

    public Object dangerousGetNoCheckButFast(ModelField modelField) {
        return this.fields.get(modelField.getName());
    }

    /** Sets the named field to the passed value, converting the value from a String to the correct type using
     * {@code Type.valueOf()} or similar.
     * <p>
     * <strong>WARNING</strong>: calling this for an {@link FieldType#OBJECT OBJECT} field is ambiguous, because
     * you could mean either that the {@code String} is the Base64-encoded representation of the object and
     * should be be deserialized or that the {@code String} is the actual object to be stored.  Since this
     * method is intended for use in restoring the entity from an XML export, it assumes that the value is
     * the Base64-encoding of an arbitrary object and attempts to deserialize it.  If this is not what is
     * intended, then it is up to the caller to use {@link #set(String, Object)} for those fields, instead.
     * </p>
     *
     * @param name The field name to set
     * @param value The String value to convert and set
     */
    public void setString(String name, String value)
    {
        ModelField field = getModelEntity().getField(name);
        if (field == null)
        {
            throw new IllegalArgumentException("[GenericEntity.setString] \"" + name + "\" is not a field of " + entityName);
        }

        final ModelFieldType type = getModelFieldType(field.getType());
        final FieldType fieldType = SqlJdbcUtil.getFieldType(type.getJavaType());
        switch (fieldType)
        {
            case STRING:
                set(name, value);
                break;

            case TIMESTAMP:
                set(name, Timestamp.valueOf(value));
                break;

            case TIME:
                set(name, Time.valueOf(value));
                break;

            case DATE:
                set(name, Date.valueOf(value));
                break;

            case INTEGER:
                set(name, Integer.valueOf(value));
                break;

            case LONG:
                set(name, Long.valueOf(value));
                break;

            case FLOAT:
                set(name, Float.valueOf(value));
                break;

            case DOUBLE:
                set(name, Double.valueOf(value));
                break;

            case BOOLEAN:
                set(name, Boolean.valueOf(value));
                break;

            case OBJECT:
                set(name, deserialize(decodeBase64(value)));
                break;

            case CLOB:
                set(name, value);
                break;

            case BLOB:
                set(name, decodeBase64(value));
                break;

            case BYTE_ARRAY:
                set(name, decodeBase64(value));
                break;

            default:
                throw new UnsupportedOperationException("Unsupported type: " + fieldType);
        }
    }

    /** Sets a field with an array of bytes, wrapping them automatically for easy use.
     * @param name The field name to set
     * @param bytes The byte array to be wrapped and set
     */
    public void setBytes(String name, byte[] bytes) {
        this.set(name, new ByteWrapper(bytes));
    }

    public Boolean getBoolean(String name) {
        Object obj = get(name);

        if (obj == null) {
            return null;
        }
        if (obj instanceof Boolean) {
            return (Boolean) obj;
        } else if (obj instanceof String) {
            String value = (String) obj;

            if ("Y".equals(value)) {
                return Boolean.TRUE;
            } else if ("N".equals(value)) {
                return Boolean.FALSE;
            } else {
                throw new IllegalArgumentException("getBoolean could not map the String '" + value + "' to Boolean type");
            }
        } else {
            throw new IllegalArgumentException("getBoolean could not map the object '" + obj.toString() + "' to Boolean type, unknown object type: " + obj.getClass().getName());
        }
    }

    // might be nice to add some ClassCastException handling... and auto conversion? hmmm...
    public String getString(String name) {
        Object object = get(name);

        if (object == null) return null;
        if (object instanceof java.lang.String)
            return (String) object;
        else
            return object.toString();
    }

    public Timestamp getTimestamp(String name) {
        return (Timestamp) get(name);
    }

    public Time getTime(String name) {
        return (Time) get(name);
    }

    public Date getDate(String name) {
        return (Date) get(name);
    }

    public Integer getInteger(String name) {
        return (Integer) get(name);
    }

    public Long getLong(String name) {
        return (Long) get(name);
    }

    public Float getFloat(String name) {
        return (Float) get(name);
    }

    public Double getDouble(String name) {
        return (Double) get(name);
    }

    public byte[] getBytes(String name) {
        ByteWrapper wrapper = (ByteWrapper) get(name);
        if (wrapper == null) return null;
        return wrapper.getBytes();
    }

    public GenericPK getPrimaryKey() {
        Collection<String> pkNames = new LinkedList<String>();
        Iterator<ModelField> iter = this.getModelEntity().getPksIterator();

        while (iter != null && iter.hasNext()) {
            ModelField curField = iter.next();

            pkNames.add(curField.getName());
        }
        return new GenericPK(getModelEntity(), this.getFields(pkNames));
    }

    /** go through the pks and for each one see if there is an entry in fields to set */
    public void setPKFields(Map<String, Object> fields) {
        this.setPKFields(fields, true);
    }

    /** go through the pks and for each one see if there is an entry in fields to set */
    public void setPKFields(Map<String, Object> fields, boolean setIfEmpty) {
        Iterator<ModelField> iter = this.getModelEntity().getPksIterator();

        while (iter != null && iter.hasNext()) {
            ModelField curField = iter.next();

            if (fields.containsKey(curField.getName())) {
                Object field = fields.get(curField.getName());

                if (setIfEmpty) {
                    // if empty string, set to null
                    if (field != null && field instanceof String && ((String) field).length() == 0) {
                        this.set(curField.getName(), null);
                    } else {
                        this.set(curField.getName(), field);
                    }
                } else {
                    // okay, only set if not empty...
                    if (field != null) {
                        // if it's a String then we need to check length, otherwise set it because it's not null
                        if (field instanceof String) {
                            String fieldStr = (String) field;

                            if (fieldStr.length() > 0) {
                                this.set(curField.getName(), field);
                            }
                        } else {
                            this.set(curField.getName(), field);
                        }
                    }
                }
                // this.set(curField.getName(), fields.get(curField.getName()));
            }
        }
    }

    /** go through the non-pks and for each one see if there is an entry in fields to set */
    public void setNonPKFields(Map<String, Object> fields) {
        this.setNonPKFields(fields, true);
    }

    /** go through the non-pks and for each one see if there is an entry in fields to set */
    public void setNonPKFields(Map<String, Object> fields, boolean setIfEmpty) {
        Iterator<ModelField> iter = this.getModelEntity().getNopksIterator();

        while (iter != null && iter.hasNext()) {
            ModelField curField = iter.next();

            if (fields.containsKey(curField.getName())) {
                Object field = fields.get(curField.getName());

                // if (Debug.verboseOn()) Debug.logVerbose("Setting field " + curField.getName() + ": " + field + ", setIfEmpty = " + setIfEmpty);
                if (setIfEmpty) {
                    // if empty string, set to null
                    if (field != null && field instanceof String && ((String) field).length() == 0) {
                        this.set(curField.getName(), null);
                    } else {
                        this.set(curField.getName(), field);
                    }
                } else {
                    // okay, only set if not empty...
                    if (field != null) {
                        // if it's a String then we need to check length, otherwise set it because it's not null
                        if (field instanceof String) {
                            String fieldStr = (String) field;

                            if (fieldStr.length() > 0) {
                                this.set(curField.getName(), field);
                            }
                        } else {
                            this.set(curField.getName(), field);
                        }
                    }
                }
            }
        }
    }

    /** Returns keys of entity fields
     * @return java.util.Collection
     */
    public Collection<String> getAllKeys() {
        return fields.keySet();
    }

    /** Returns key/value pairs of entity fields
     * @return java.util.Map
     */
    public Map<String, Object> getAllFields() {
        return new HashMap<String, Object>(fields);
    }

    /** Used by clients to specify exactly the fields they are interested in
     * @param keysofFields the name of the fields the client is interested in
     * @return java.util.Map
     */
    public Map<String, Object> getFields(Collection<String> keysofFields) {
        if (keysofFields == null) return null;
        Map<String, Object> map = Maps.newHashMapWithExpectedSize(keysofFields.size());
        for (String key : keysofFields)
        {
            map.put(key, fields.get(key));
        }
        return map;
    }

    /**
     * Updates the given fields of this entity with the values in the given map, including nulls.
     *
     * @param newFieldValues map of valid field names to new values; if null, this method does nothing
     */
    public void setFields(final Map<? extends String, ?> newFieldValues) {
        if (newFieldValues == null) {
            return;
        }
        // We could implement this with Map.putAll, but this way validates the given field names
        for (final Entry<? extends String, ?> update : newFieldValues.entrySet()) {
            set(update.getKey(), update.getValue(), true);
        }
    }

    public boolean matchesFields(Map<String, ?> keyValuePairs) {
        if (fields == null) return true;
        if (keyValuePairs == null || keyValuePairs.size() == 0) return true;

        for (Entry<String, ?> entry : keyValuePairs.entrySet()) {

            if (!UtilValidate.areEqual(entry.getValue(), this.fields.get(entry.getKey()))) {
                return false;
            }
        }
        return true;
    }

    /** Used to indicate if locking is enabled for this entity
     * @return True if locking is enabled
     */
    public boolean lockEnabled() {
        return modelEntity.lock();
    }

    // ======= XML Related Methods ========
    public static Document makeXmlDocument(Collection<GenericValue> values) {
        Document document = UtilXml.makeEmptyXmlDocument("entity-engine-xml");

        if (document == null) return null;

        addToXmlDocument(values, document);
        return document;
    }

    public static int addToXmlDocument(Collection<GenericValue> values, Document document) {
        if (values == null || document == null) return 0;

        Element rootElement = document.getDocumentElement();
        int numberAdded = 0;
        for (GenericEntity value : values) {
            Element valueElement = value.makeXmlElement(document);
            rootElement.appendChild(valueElement);
            numberAdded++;
        }
        return numberAdded;
    }

    /** Makes an XML Element object with an attribute for each field of the entity
     *@param document The XML Document that the new Element will be part of
     *@return org.w3c.dom.Element object representing this generic entity
     */
    public Element makeXmlElement(Document document) {
        return makeXmlElement(document, null);
    }

    /** Makes an XML Element object with an attribute for each field of the entity
     *@param document The XML Document that the new Element will be part of
     *@param prefix A prefix to put in front of the entity name in the tag name
     *@return org.w3c.dom.Element object representing this generic entity
     */
    public Element makeXmlElement(Document document, String prefix) {
        Element element = null;

        if (prefix == null) prefix = "";
        if (document != null) element = document.createElement(prefix + entityName);
        // else element = new ElementImpl(null, this.getEntityName());
        if (element == null) return null;

        ModelEntity modelEntity = this.getModelEntity();

        Iterator<ModelField> modelFields = modelEntity.getFieldsIterator();
        while (modelFields.hasNext()) {
            ModelField modelField = modelFields.next();
            String name = modelField.getName();
            String value = this.getString(name);

            if (value != null) {
                if (value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0) {
                    // Atlassian FIX: Escape the CDATA sections (if any) in the value of the string
                    UtilXml.addChildElementCDATAValue(element, name, escapeCData(value), document);
                } else {
                    element.setAttribute(name, value);
                }
            }
        }

        return element;
    }

    /** Writes XML text with an attribute or CDATA element for each field of the entity
     *@param writer A PrintWriter to write to
     *@param prefix A prefix to put in front of the entity name in the tag name
     */
    public void writeXmlText(PrintWriter writer, String prefix) {
        if (prefix == null) prefix = "";
        ModelEntity modelEntity = this.getModelEntity();
        ModelFieldTypeReader mtr = ModelFieldTypeReader.getModelFieldTypeReader(getDelegator().getEntityHelperName(modelEntity));

        writer.print("    <");
        writer.print(prefix);
        writer.print(entityName);

        // write attributes immediately and if a CDATA element is needed, put those in a Map for now
        Map<String, String> cdataMap = new HashMap<String, String>();

        Iterator<ModelField> modelFields = modelEntity.getFieldsIterator();

        while (modelFields.hasNext()) {
            ModelField modelField = modelFields.next();
            String name = modelField.getName();
            String value;

            ModelFieldType mft = mtr.getModelFieldType(modelField.getType());
            FieldType fieldType = getFieldType(mft.getJavaType());

            switch (fieldType)
            {
                case OBJECT:
                    value = encodeBase64(serialize(get(name)));
                    if (value != null)
                    {
                        cdataMap.put(name, value);
                    }
                    continue;

                case BLOB:
                    throw new UnsupportedOperationException("These can't be exported, yet");

                case BYTE_ARRAY:
                    value = encodeBase64((byte[])get(name));
                    if (value != null)
                    {
                        cdataMap.put(name, value);
                    }
                    continue;
            }

            value = getString(name);
            if (value != null) {
                if (value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0) {
                    cdataMap.put(name, value);
                } else {
                    writer.print(' ');
                    writer.print(name);
                    writer.print("=\"");
                    // encode the value...
                    writer.print(UtilFormatOut.encodeXmlValue(value));
                    writer.print('"');
                }
            }
        }

        if (cdataMap.isEmpty()) {
            writer.println("/>");
        } else {
            writer.println('>');

            for (Entry<String, String> entry : cdataMap.entrySet()) {
                writer.print("        <");
                writer.print(entry.getKey());
                writer.print("><![CDATA[");
                // Atlassian FIX: Escape the CDATA sections (if any) in the value of the string
                writer.print(escapeCData(entry.getValue()));
                // Do not just print the value as CDATA section might have to be escaped.
                // writer.print((String) entry.getValue());
                writer.print("]]></");
                writer.print(entry.getKey());
                writer.println('>');
            }

            // don't forget to close the entity.
            writer.print("    </");
            writer.print(entityName);
            writer.println(">");
        }
    }

    private static String escapeCData(String s)
    {
        if (s == null)
        {
            return null;
        }

        // If there are no occurrences of "]]>", then we can use the string as-is.
        int index = s.indexOf("]]>");
        if (index == -1)
        {
            return s;
        }

        // Otherwise, we must split it into multiple CDATA sections to protect that sequence.
        final StringBuilder sb = new StringBuilder(s.length() + 64);
        int mark = 0;
        do
        {
            sb.append(s, mark, index).append("]]]]><![CDATA[>");
            mark = index + 3;
            index = s.indexOf("]]>", mark);
        }
        while (index != -1);

        return sb.append(s, mark, s.length()).toString();
    }



    /** Determines the equality of two GenericEntity objects, overrides the default equals
     *@param  obj  The object (GenericEntity) to compare this two
     *@return      boolean stating if the two objects are equal
     */
    public boolean equals(Object obj) {
        if (obj == null) return false;

        // from here, use the compareTo method since it is more efficient:
        if (this.compareTo((GenericEntity)obj) == 0) {
            return true;
        } else {
            return false;
        }

        /*
         if (this.getClass().equals(obj.getClass())) {
         GenericEntity that = (GenericEntity) obj;
         if (this.getEntityName() != null && !this.getEntityName().equals(that.getEntityName())) {
         //if (Debug.infoOn()) Debug.logInfo("[GenericEntity.equals] Not equal: This entityName \"" + this.getEntityName() + "\" is not equal to that entityName \"" + that.getEntityName() + "\"");
         return false;
         }
         if (this.fields.equals(that.fields)) {
         return true;
         } else {
         //if (Debug.infoOn()) Debug.logInfo("[GenericEntity.equals] Not equal: Fields of this entity: \n" + this.toString() + "\n are not equal to fields of that entity:\n" + that.toString());
         }
         }
         return false;
         */
    }

    /** Creates a hashCode for the entity, using the default String hashCode and Map hashCode, overrides the default hashCode
     *@return    Hashcode corresponding to this entity
     */
    public int hashCode() {
        // divide both by two (shift to right one bit) to maintain scale and add together
        return getEntityName().hashCode() >> 1 + fields.hashCode() >> 1;
    }

    /** Creates a String for the entity, overrides the default toString
     *@return    String corresponding to this entity
     */
    public String toString() {
        StringBuilder theString = new StringBuilder();

        theString.append("[GenericEntity:");
        theString.append(getEntityName());
        theString.append(']');

        Iterator<Entry<String, Object>> entries = fields.entrySet().iterator();
        Map.Entry<String, Object> anEntry = null;

        while (entries.hasNext()) {
            anEntry = entries.next();
            theString.append('[');
            theString.append(anEntry.getKey());
            theString.append(',');
            theString.append(anEntry.getValue());
            theString.append(']');
        }
        return theString.toString();
    }

    /** Compares this GenericEntity to the passed object
     *@param obj Object to compare this to
     *@return int representing the result of the comparison (-1,0, or 1)
     */
    public int compareTo(GenericEntity obj) {
        // if null, it will push to the beginning
        if (obj == null) return -1;

        // rather than doing an if instanceof, just cast it and let it throw an exception if
        // it fails, this will be faster for the expected case (that it IS a GenericEntity)
        // if not a GenericEntity throw ClassCastException, as the spec says

        int tempResult = this.entityName.compareTo(obj.entityName);

        // if they did not match, we know the order, otherwise compare the primary keys
        if (tempResult != 0) return tempResult;

        // both have same entityName, should be the same so let's compare PKs
        int pksSize = modelEntity.getPksSize();

        for (int i = 0; i < pksSize; i++) {
            ModelField curField = modelEntity.getPk(i);
            Comparable thisVal = (Comparable<?>) this.fields.get(curField.getName());
            Comparable thatVal = (Comparable<?>) obj.fields.get(curField.getName());

            if (thisVal == null) {
                if (thatVal == null)
                    tempResult = 0;
                // if thisVal is null, but thatVal is not, return 1 to put this earlier in the list
                else
                    tempResult = 1;
            } else {
                // if thatVal is null, put the other earlier in the list
                if (thatVal == null)
                    tempResult = -1;
                else
                    tempResult = thisVal.compareTo(thatVal);
            }
            if (tempResult != 0) return tempResult;
        }

        // okay, if we got here it means the primaryKeys are exactly the SAME, so compare the rest of the fields
        int nopksSize = modelEntity.getNopksSize();

        for (int i = 0; i < nopksSize; i++) {
            ModelField curField = modelEntity.getNopk(i);
            Comparable thisVal = (Comparable<?>) this.fields.get(curField.getName());
            Comparable thatVal = (Comparable<?>) obj.fields.get(curField.getName());

            if (thisVal == null) {
                if (thatVal == null)
                    tempResult = 0;
                // if thisVal is null, but thatVal is not, return 1 to put this earlier in the list
                else
                    tempResult = 1;
            } else {
                // if thatVal is null, put the other earlier in the list
                if (thatVal == null)
                    tempResult = -1;
                else
                    tempResult = thisVal.compareTo(thatVal);
            }
            if (tempResult != 0) return tempResult;
        }

        // if we got here it means the two are exactly the same, so return tempResult, which should be 0
        return tempResult;
    }

    /** Clones this GenericEntity, this is a shallow clone & uses the default shallow HashMap clone
     *@return Object that is a clone of this GenericEntity
     */
    public Object clone() {
        GenericEntity newEntity = new GenericEntity(this);

        newEntity.setDelegator(internalDelegator);
        return newEntity;
    }

    // ---- Methods added to implement the Map interface: ----

    public Object remove(Object key) {
        return fields.remove(key);
    }

    public boolean containsKey(Object key) {
        return fields.containsKey(key);
    }

    public java.util.Set<Map.Entry<String, Object>> entrySet() {
        return fields.entrySet();
    }

    public Object put(String key, Object value) {
        return this.set(key, value, true);
    }

    public void putAll(Map<? extends String, ?> map) {
        this.setFields(map);
    }

    public void clear() {
        this.fields.clear();
    }

    public Object get(Object key) {
        return this.get((String) key);
    }

    public Set<String> keySet() {
        return this.fields.keySet();
    }

    public boolean isEmpty() {
        return this.fields.isEmpty();
    }

    public Collection<Object> values() {
        return this.fields.values();
    }

    public boolean containsValue(Object value) {
        return this.fields.containsValue(value);
    }

    public int size() {
        return this.fields.size();
    }
}
