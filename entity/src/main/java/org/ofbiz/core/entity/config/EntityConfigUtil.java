/*
 * $Id: EntityConfigUtil.java,v 1.7 2006/03/13 01:39:01 hbarney Exp $
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
package org.ofbiz.core.entity.config;

import com.atlassian.util.concurrent.CopyOnWriteMap;
import org.ofbiz.core.config.GenericConfigException;
import org.ofbiz.core.config.ResourceLoader;
import org.ofbiz.core.entity.ConnectionFactory;
import org.ofbiz.core.entity.GenericEntityConfException;
import org.ofbiz.core.entity.GenericEntityException;
import org.ofbiz.core.entity.jdbc.dbtype.DatabaseType;
import org.ofbiz.core.entity.jdbc.dbtype.DatabaseTypeFactory;
import org.ofbiz.core.entity.util.ClassLoaderUtils;
import org.ofbiz.core.util.Debug;
import org.ofbiz.core.util.GeneralRuntimeException;
import org.ofbiz.core.util.UtilValidate;
import org.ofbiz.core.util.UtilXml;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.sql.Connection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Misc. utility method for dealing with the entityengine.xml file
 *
 * @author <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 * @author <a href="mailto:jaz@ofbiz.org">Andy Zeneski</a>
 * @version $Revision: 1.7 $
 * @since 2.0
 */
public class
        EntityConfigUtil
{

    public static final String ENTITY_ENGINE_XML_FILENAME = "entityengine.xml";

    // ========== engine info fields ==========
    protected String txFactoryClass;
    protected String txFactoryUserTxJndiName;
    protected String txFactoryUserTxJndiServerName;
    protected String txFactoryTxMgrJndiName;
    protected String txFactoryTxMgrJndiServerName;

    protected Map<String, ResourceLoaderInfo> resourceLoaderInfos = CopyOnWriteMap.newHashMap();
    protected Map<String, DelegatorInfo> delegatorInfos = CopyOnWriteMap.newHashMap();
    protected Map<String, EntityModelReaderInfo> entityModelReaderInfos = CopyOnWriteMap.newHashMap();
    protected Map<String, EntityGroupReaderInfo> entityGroupReaderInfos = CopyOnWriteMap.newHashMap();
    protected Map<String, EntityEcaReaderInfo> entityEcaReaderInfos = CopyOnWriteMap.newHashMap();
    protected Map<String, FieldTypeInfo> fieldTypeInfos = CopyOnWriteMap.newHashMap();
    protected Map<String, DatasourceInfo> datasourceInfos = CopyOnWriteMap.newHashMap();

    private static volatile EntityConfigUtil singletonInstance;

    public static EntityConfigUtil getInstance()
    {
        if (singletonInstance == null)
        {
            singletonInstance = new EntityConfigUtil();
        }
        return singletonInstance;
    }

    protected Element getXmlRootElement() throws GenericEntityConfException
    {
        try
        {
            return ResourceLoader.getXmlRootElement(EntityConfigUtil.ENTITY_ENGINE_XML_FILENAME);
        }
        catch (GenericConfigException e)
        {
            throw new GenericEntityConfException("Could not get entity engine XML root element", e);
        }
    }

    protected Document getXmlDocument() throws GenericEntityConfException
    {
        try
        {
            return ResourceLoader.getXmlDocument(EntityConfigUtil.ENTITY_ENGINE_XML_FILENAME);
        }
        catch (GenericConfigException e)
        {
            throw new GenericEntityConfException("Could not get entity engine XML document", e);
        }
    }

    public EntityConfigUtil()
    {
        try
        {
            initialize(getXmlRootElement());
        }
        catch (Exception e)
        {
            Debug.logError(e, "Error loading entity config XML file " + ENTITY_ENGINE_XML_FILENAME);
        }
    }

    public synchronized void reinitialize() throws GenericEntityException
    {
        try
        {
            ResourceLoader.invalidateDocument(ENTITY_ENGINE_XML_FILENAME);
            initialize(getXmlRootElement());
        }
        catch (Exception e)
        {
            throw new GenericEntityException("Error reloading entity config XML file " + ENTITY_ENGINE_XML_FILENAME, e);
        }
    }

    public synchronized void removeDatasourceInfo(String datasourceInfoName)
    {
        this.datasourceInfos.remove(datasourceInfoName);
    }

    public synchronized void addDatasourceInfo(Element element)
    {
        DatasourceInfo datasourceInfo = new DatasourceInfo(element);
        datasourceInfos.put(datasourceInfo.name, datasourceInfo);
    }

    public synchronized void removeDelegatorInfo(String delegatorInfoName)
    {
        this.delegatorInfos.remove(delegatorInfoName);
    }

    public synchronized void addDelegatorInfo(DelegatorInfo delegatorInfo)
    {
        delegatorInfos.put(delegatorInfo.name, delegatorInfo);
    }

    public void initialize(Element rootElement) throws GenericEntityException
    {
        Element transactionFactoryElement = UtilXml.firstChildElement(rootElement, "transaction-factory");
        if (transactionFactoryElement == null)
        {
            throw new GenericEntityConfException("ERROR: no transaction-factory definition was found in " + ENTITY_ENGINE_XML_FILENAME);
        }

        this.txFactoryClass = transactionFactoryElement.getAttribute("class");

        Element userTxJndiElement = UtilXml.firstChildElement(transactionFactoryElement, "user-transaction-jndi");
        if (userTxJndiElement != null)
        {
            this.txFactoryUserTxJndiName = userTxJndiElement.getAttribute("jndi-name");
            this.txFactoryUserTxJndiServerName = userTxJndiElement.getAttribute("jndi-server-name");
        }
        else
        {
            this.txFactoryUserTxJndiName = null;
            this.txFactoryUserTxJndiServerName = null;
        }

        Element txMgrJndiElement = UtilXml.firstChildElement(transactionFactoryElement, "transaction-manager-jndi");
        if (txMgrJndiElement != null)
        {
            this.txFactoryTxMgrJndiName = txMgrJndiElement.getAttribute("jndi-name");
            this.txFactoryTxMgrJndiServerName = txMgrJndiElement.getAttribute("jndi-server-name");
        }
        else
        {
            this.txFactoryTxMgrJndiName = null;
            this.txFactoryTxMgrJndiServerName = null;
        }

        // not load all of the maps...
        List childElements = null;
        Iterator elementIter = null;

        // resource-loader - resourceLoaderInfos
        childElements = UtilXml.childElementList(rootElement, "resource-loader");
        elementIter = childElements.iterator();
        while (elementIter.hasNext())
        {
            Element curElement = (Element) elementIter.next();
            ResourceLoaderInfo resourceLoaderInfo = new EntityConfigUtil.ResourceLoaderInfo(curElement);
            this.resourceLoaderInfos.put(resourceLoaderInfo.name, resourceLoaderInfo);
        }

        // delegator - delegatorInfos
        childElements = UtilXml.childElementList(rootElement, "delegator");
        elementIter = childElements.iterator();
        while (elementIter.hasNext())
        {
            Element curElement = (Element) elementIter.next();
            DelegatorInfo delegatorInfo = new EntityConfigUtil.DelegatorInfo(curElement);
            this.delegatorInfos.put(delegatorInfo.name, delegatorInfo);
        }

        // entity-model-reader - entityModelReaderInfos
        childElements = UtilXml.childElementList(rootElement, "entity-model-reader");
        elementIter = childElements.iterator();
        while (elementIter.hasNext())
        {
            Element curElement = (Element) elementIter.next();
            EntityModelReaderInfo entityModelReaderInfo = new EntityModelReaderInfo(curElement);
            entityModelReaderInfos.put(entityModelReaderInfo.name, entityModelReaderInfo);
        }

        // entity-group-reader - entityGroupReaderInfos
        childElements = UtilXml.childElementList(rootElement, "entity-group-reader");
        elementIter = childElements.iterator();
        while (elementIter.hasNext())
        {
            Element curElement = (Element) elementIter.next();
            EntityGroupReaderInfo entityGroupReaderInfo = new EntityGroupReaderInfo(curElement);
            entityGroupReaderInfos.put(entityGroupReaderInfo.name, entityGroupReaderInfo);
        }

        // entity-eca-reader - entityEcaReaderInfos
        childElements = UtilXml.childElementList(rootElement, "entity-eca-reader");
        elementIter = childElements.iterator();
        while (elementIter.hasNext())
        {
            Element curElement = (Element) elementIter.next();
            EntityEcaReaderInfo entityEcaReaderInfo = new EntityEcaReaderInfo(curElement);
            entityEcaReaderInfos.put(entityEcaReaderInfo.name, entityEcaReaderInfo);
        }

        // field-type - fieldTypeInfos
        childElements = UtilXml.childElementList(rootElement, "field-type");
        elementIter = childElements.iterator();
        while (elementIter.hasNext())
        {
            Element curElement = (Element) elementIter.next();
            FieldTypeInfo fieldTypeInfo = new FieldTypeInfo(curElement);
            fieldTypeInfos.put(fieldTypeInfo.name, fieldTypeInfo);
        }

        // datasource - datasourceInfos
        childElements = UtilXml.childElementList(rootElement, "datasource");
        // Allow there to be no datasource as yet... as in the case of a fresh multi tenant app with
        // no tenants.
        if (childElements != null)
        {
            elementIter = childElements.iterator();
            while (elementIter.hasNext())
            {
                Element curElement = (Element) elementIter.next();
                DatasourceInfo datasourceInfo = new DatasourceInfo(curElement);
                datasourceInfos.put(datasourceInfo.name, datasourceInfo);
            }
        }
    }

    public String getTxFactoryClass()
    {
        return txFactoryClass;
    }

    public String getTxFactoryUserTxJndiName()
    {
        return txFactoryUserTxJndiName;
    }

    public String getTxFactoryUserTxJndiServerName()
    {
        return txFactoryUserTxJndiServerName;
    }

    public String getTxFactoryTxMgrJndiName()
    {
        return txFactoryTxMgrJndiName;
    }

    public String getTxFactoryTxMgrJndiServerName()
    {
        return txFactoryTxMgrJndiServerName;
    }

    public ResourceLoaderInfo getResourceLoaderInfo(String name)
    {
        return resourceLoaderInfos.get(name);
    }

    public DelegatorInfo getDelegatorInfo(String name)
    {
        return delegatorInfos.get(name);
    }

    public EntityModelReaderInfo getEntityModelReaderInfo(String name)
    {
        return entityModelReaderInfos.get(name);
    }

    public EntityGroupReaderInfo getEntityGroupReaderInfo(String name)
    {
        return entityGroupReaderInfos.get(name);
    }

    public EntityEcaReaderInfo getEntityEcaReaderInfo(String name)
    {
        return entityEcaReaderInfos.get(name);
    }

    public FieldTypeInfo getFieldTypeInfo(String name)
    {
        return fieldTypeInfos.get(name);
    }

    public DatasourceInfo getDatasourceInfo(String name)
    {
        return datasourceInfos.get(name);
    }

    public static class ResourceLoaderInfo
    {
        public String name;
        public String className;
        public String prependEnv;
        public String prefix;

        public ResourceLoaderInfo(Element element)
        {
            this.name = element.getAttribute("name");
            this.className = element.getAttribute("class");
            this.prependEnv = element.getAttribute("prepend-env");
            this.prefix = element.getAttribute("prefix");
        }
    }


    public static class DelegatorInfo
    {
        public String name;
        public String entityModelReader;
        public String entityGroupReader;
        public String entityEcaReader;
        public boolean useDistributedCacheClear;
        public String distributedCacheClearClassName;
        public String distributedCacheClearUserLoginId;
        public Map<String, String> groupMap = new HashMap<String, String>();

        public DelegatorInfo(String name, String entityModelReader, String entityGroupReader, Map groupMap)
        {
            this.name = name;
            this.entityModelReader = entityModelReader;
            this.entityGroupReader = entityGroupReader;
            this.groupMap = groupMap;
        }

        public DelegatorInfo(Element element)
        {
            this.name = element.getAttribute("name");
            entityModelReader = element.getAttribute("entity-model-reader");
            entityGroupReader = element.getAttribute("entity-group-reader");
            entityEcaReader = element.getAttribute("entity-eca-reader");
            // this defaults to false, ie anything but true is false
            this.useDistributedCacheClear = "true".equals(element.getAttribute("distributed-cache-clear-enabled"));
            this.distributedCacheClearClassName = element.getAttribute("distributed-cache-clear-class-name");
            if (UtilValidate.isEmpty(this.distributedCacheClearClassName))
            {
                this.distributedCacheClearClassName = "org.ofbiz.core.extentity.EntityCacheServices";
            }

            this.distributedCacheClearUserLoginId = element.getAttribute("distributed-cache-clear-user-login-id");
            if (UtilValidate.isEmpty(this.distributedCacheClearUserLoginId))
            {
                this.distributedCacheClearUserLoginId = "admin";
            }

            List groupMapList = UtilXml.childElementList(element, "group-map");
            Iterator groupMapIter = groupMapList.iterator();

            while (groupMapIter.hasNext())
            {
                Element groupMapElement = (Element) groupMapIter.next();

                groupMap.put(groupMapElement.getAttribute("group-name"), groupMapElement.getAttribute("datasource-name"));
            }
        }
    }


    public static class EntityModelReaderInfo
    {
        public String name;
        public List resourceElements;

        public EntityModelReaderInfo(Element element)
        {
            this.name = element.getAttribute("name");
            resourceElements = UtilXml.childElementList(element, "resource");
        }
    }


    public static class EntityGroupReaderInfo
    {
        public String name;
        public Element resourceElement;

        public EntityGroupReaderInfo(Element element)
        {
            this.name = element.getAttribute("name");
            resourceElement = element;
        }
    }


    public static class EntityEcaReaderInfo
    {
        public String name;
        public List resourceElements;

        public EntityEcaReaderInfo(Element element)
        {
            this.name = element.getAttribute("name");
            resourceElements = UtilXml.childElementList(element, "resource");
        }
    }


    public static class FieldTypeInfo
    {
        public String name;
        public Element resourceElement;

        public FieldTypeInfo(Element element)
        {
            this.name = element.getAttribute("name");
            resourceElement = element;
        }
    }


    public static class DatasourceInfo
    {
        public String name;
        public String helperClass;
        private String fieldTypeName;
        public List sqlLoadPaths = new LinkedList();
        public Element datasourceElement;

        public static final int TYPE_JNDI_JDBC = 1;
        public static final int TYPE_INLINE_JDBC = 2;
        public static final int TYPE_TYREX_DATA_SOURCE = 3;
        public static final int TYPE_OTHER = 4;

        public Element jndiJdbcElement;
        public Element tyrexDataSourceElement;
        public Element inlineJdbcElement;

        private String schemaName = null;
        public boolean checkOnStart = true;
        public boolean addMissingOnStart = false;
        public boolean useFks = true;
        public boolean useFkIndices = true;
        public boolean checkForeignKeysOnStart = false;
        public boolean checkFkIndicesOnStart = false;
        public boolean usePkConstraintNames = true;
        private Integer constraintNameClipLength = null;
        public String fkStyle = null;
        public boolean useFkInitiallyDeferred = true;
        public boolean useIndices = true;
        public boolean checkIndicesOnStart = false;
        public String joinStyle = null;

        protected static final Properties CONFIGURATION;


        static
        {
            CONFIGURATION = new Properties();
            try
            {
                CONFIGURATION.load(ClassLoaderUtils.getResourceAsStream("ofbiz-database.properties", EntityConfigUtil.class));
            }
            catch (Exception e)
            {
                Debug.logError("Unable to find ofbiz-database.properties file. Using default values for ofbiz configuration.");
            }
        }

        /**
         * If the field-type-name property matches this string we will try and guess the field-type-name by using the
         * metadata returned by the database connection.
         */
        public static final String AUTO_FIELD_TYPE;

        public static final String AUTO_SCHEMA_NAME;

        public static final String AUTO_CONSTRAINT_NAME_CLIP_LENGTH;

        static
        {
            AUTO_FIELD_TYPE = getNonNullProperty("fieldType.autoConfigue", "${auto-field-type-name}");

            AUTO_SCHEMA_NAME = getNonNullProperty("schemaName.autoConfigure", "${auto-schema-name}");

            AUTO_CONSTRAINT_NAME_CLIP_LENGTH = getNonNullProperty("constraintNameClipLength.autoConfigure", "${auto-constraint-name-clip-length}");
        }

        public static final int DEFAULT_CONSTRAINT_NAME_CLIP_LENGTH = 20;

        /**
         * A method for getting properties from the configuration file. Uses the default value passed in if the key as
         * read from the property file was null.
         */
        private static String getNonNullProperty(String propertyKey, String defaultValue)
        {
            String fieldValue = CONFIGURATION.getProperty(propertyKey);
            if (fieldValue != null)
            {
                return fieldValue;
            }
            else
            {
                Debug.logError(propertyKey + " not set in the ofbiz-database.properties file. Using default value: " + defaultValue);
                return defaultValue;
            }
        }

        public DatasourceInfo(Element element)
        {
            this.name = element.getAttribute("name");
            this.helperClass = element.getAttribute("helper-class");
            this.fieldTypeName = element.getAttribute("field-type-name");

            sqlLoadPaths = UtilXml.childElementList(element, "sql-load-path");
            datasourceElement = element;

            if (datasourceElement == null)
            {
                Debug.logWarning("datasource def not found with name " + this.name + ", using default for schema-name (none)");
                Debug.logWarning("datasource def not found with name " + this.name + ", using default for check-on-start (true)");
                Debug.logWarning("datasource def not found with name " + this.name + ", using default for add-missing-on-start (false)");
                Debug.logWarning("datasource def not found with name " + this.name + ", using default for use-foreign-keys (true)");
                Debug.logWarning("datasource def not found with name " + this.name + ", using default use-foreign-key-indices (true)");
                Debug.logWarning("datasource def not found with name " + this.name + ", using default for check-fks-on-start (false)");
                Debug.logWarning("datasource def not found with name " + this.name + ", using default for check-fk-indices-on-start (false)");
                Debug.logWarning("datasource def not found with name " + this.name + ", using default for use-pk-constraint-names (true)");
                Debug.logWarning("datasource def not found with name " + this.name + ", using default for constraint-name-clip-length (30)");
                Debug.logWarning("datasource def not found with name " + this.name + ", using default for fk-style (name_constraint)");
                Debug.logWarning("datasource def not found with name " + this.name + ", using default for use-fk-initially-deferred (true)");
                Debug.logWarning("datasource def not found with name " + this.name + ", using default for use-indices (true)");
                Debug.logWarning("datasource def not found with name " + this.name + ", using default for check-indices-on-start (false)");
                Debug.logWarning("datasource def not found with name " + this.name + ", using default for join-style (ansi)");
            }
            else
            {
                schemaName = datasourceElement.getAttribute("schema-name");
                // anything but false is true
                checkOnStart = !"false".equals(datasourceElement.getAttribute("check-on-start"));
                // anything but true is false
                addMissingOnStart = "true".equals(datasourceElement.getAttribute("add-missing-on-start"));
                // anything but false is true
                useFks = !"false".equals(datasourceElement.getAttribute("use-foreign-keys"));
                // anything but false is true
                useFkIndices = !"false".equals(datasourceElement.getAttribute("use-foreign-key-indices"));
                // anything but true is false
                checkForeignKeysOnStart = "true".equals(datasourceElement.getAttribute("check-fks-on-start"));
                // anything but true is false
                checkFkIndicesOnStart = "true".equals(datasourceElement.getAttribute("check-fk-indices-on-start"));
                // anything but false is true
                usePkConstraintNames = !"false".equals(datasourceElement.getAttribute("use-pk-constraint-names"));
//                try {
//                    constraintNameClipLength = Integer.parseInt(datasourceElement.getAttribute("constraint-name-clip-length"));
//                } catch (Exception e) {
//                    Debug.logError("Could not parse constraint-name-clip-length value for datasource with name " + this.name + ", using default value of 30");
//                }
                fkStyle = datasourceElement.getAttribute("fk-style");
                // anything but true is false
                useFkInitiallyDeferred = "true".equals(datasourceElement.getAttribute("use-fk-initially-deferred"));
                // anything but false is true
                useIndices = !"false".equals(datasourceElement.getAttribute("use-indices"));
                // anything but true is false
                checkIndicesOnStart = "true".equals(datasourceElement.getAttribute("check-indices-on-start"));
                joinStyle = datasourceElement.getAttribute("join-style");
            }
            if (fkStyle == null || fkStyle.length() == 0)
            {
                fkStyle = "name_constraint";
            }
            if (joinStyle == null || joinStyle.length() == 0)
            {
                joinStyle = "ansi";
            }

            jndiJdbcElement = UtilXml.firstChildElement(datasourceElement, "jndi-jdbc");
            tyrexDataSourceElement = UtilXml.firstChildElement(datasourceElement, "tyrex-dataSource");
            inlineJdbcElement = UtilXml.firstChildElement(datasourceElement, "inline-jdbc");
        }

        public String getFieldTypeName()
        {
            // Check whether the field has already been initialized
            if (AUTO_FIELD_TYPE.equals(fieldTypeName))
            {
                fieldTypeName = findFieldTypeFromJDBCConnection();
            }
            return fieldTypeName;
        }

        public String getSchemaName()
        {
            if (AUTO_SCHEMA_NAME.equals(schemaName))
            {
                schemaName = findSchemaNameFromJDBCConnection();
            }
            return schemaName;
        }

        public int getConstraintNameClipLength()
        {
            if(constraintNameClipLength == null) {
                final String clipLength = datasourceElement.getAttribute("constraint-name-clip-length");
                if(AUTO_CONSTRAINT_NAME_CLIP_LENGTH.equals(clipLength)) {
                   constraintNameClipLength = new Integer(findConstraintNameClipLengthFromJDBCConnection());
               } else {
                   try {
                       constraintNameClipLength = new Integer(30);
                       if ((clipLength != null) && (!clipLength.equals(""))) {
                           constraintNameClipLength = new Integer(clipLength);
                       }
                   } catch (Exception e) {
                       Debug.logError("Could not parse constraint-name-clip-length value for datasource with name " + this.name + ", using default value of 30");
                   }
               }
            }

            return constraintNameClipLength.intValue();
        }

        private String findFieldTypeFromJDBCConnection()
        {
            final Connection connection;
            try
            {
                connection = ConnectionFactory.getConnection(name);
                final DatabaseType typeForConnection = DatabaseTypeFactory.getTypeForConnection(connection);
                if (typeForConnection == null)
                {
                    Debug.logError("Could not determine database type from ");
                }
                return typeForConnection.getFieldTypeName();
            }
            catch (Exception e)
            {
                String error = "Could not get connection to database to determine database type for " + AUTO_FIELD_TYPE;
                Debug.logError(e, error);
                throw new GeneralRuntimeException(error, e);
            }
        }

        private String findSchemaNameFromJDBCConnection()
        {
            final Connection connection;
            try
            {
                connection = ConnectionFactory.getConnection(name);
                final DatabaseType typeForConnection = DatabaseTypeFactory.getTypeForConnection(connection);
                if (typeForConnection == null)
                {
                    Debug.logError("Could not determine database type from ");
                }
                return typeForConnection.getSchemaName(connection);
            }
            catch (Exception e)
            {
                String error = "Could not get connection to database to determine database schema-name for " + AUTO_SCHEMA_NAME;
                Debug.logError(e, error);
                throw new GeneralRuntimeException(error, e);
            }
        }

        private int findConstraintNameClipLengthFromJDBCConnection()
        {
            final Connection connection;
            try
            {
                connection = ConnectionFactory.getConnection(name);
                final DatabaseType typeForConnection = DatabaseTypeFactory.getTypeForConnection(connection);
                if (typeForConnection == null)
                {
                    Debug.logError("Could not determine database type from ");
                }
                return typeForConnection.getConstraintNameClipLength();
            }
            catch (Exception e)
            {
                String error = "Could not get connection to database to determine database clip length";
                Debug.logError(e, error);
                return DEFAULT_CONSTRAINT_NAME_CLIP_LENGTH;
            }
        }
            }
        }
