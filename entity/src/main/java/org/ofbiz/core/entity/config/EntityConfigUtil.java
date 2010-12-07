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
import org.ofbiz.core.entity.GenericDAO;
import org.ofbiz.core.entity.GenericDelegator;
import org.ofbiz.core.entity.GenericEntityConfException;
import org.ofbiz.core.entity.GenericEntityException;
import org.ofbiz.core.entity.GenericHelperFactory;
import org.ofbiz.core.entity.TransactionFactory;
import org.ofbiz.core.util.Debug;
import org.ofbiz.core.util.UtilValidate;
import org.ofbiz.core.util.UtilXml;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Misc. utility method for dealing with the entityengine.xml file
 *
 * @author <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 * @author <a href="mailto:jaz@ofbiz.org">Andy Zeneski</a>
 * @version $Revision: 1.7 $
 * @since 2.0
 */
public class EntityConfigUtil
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

    public EntityConfigUtil(String filename)
    {
        try
        {
            initialize(ResourceLoader.getXmlRootElement(filename));
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

    public synchronized void removeDatasource(String helperName)
    {
        // Remove the helper
        GenericHelperFactory.removeHelper(helperName);
        // Remove the DAO
        GenericDAO.removeGenericDAO(helperName);
        // Shut down the connection pool if there is one
        TransactionFactory.getTransactionFactory().removeDatasource(helperName);
        // Remove the datasource info
        this.datasourceInfos.remove(helperName);
    }

    public synchronized void addDatasourceInfo(DatasourceInfo datasourceInfo)
    {
        datasourceInfos.put(datasourceInfo.getName(), datasourceInfo);
    }

    public synchronized void removeDelegator(String delegatorName)
    {
        this.delegatorInfos.remove(delegatorName);
        GenericDelegator.removeGenericDelegator(delegatorName);
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
                datasourceInfos.put(datasourceInfo.getName(), datasourceInfo);
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

}
