/*
 * $Id: JNDIConfigUtil.java,v 1.1 2005/04/01 05:58:05 sfarquhar Exp $
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
package org.ofbiz.core.config;

import org.ofbiz.core.util.Debug;
import org.ofbiz.core.util.UtilXml;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * JNDIConfigUtil
 *
 * @author <a href="mailto:jaz@ofbiz.org">Andy Zeneski</a>
 * @version $Revision: 1.1 $
 * @since 2.0
 */
public class JNDIConfigUtil {

    public static final String JNDI_CONFIG_XML_FILENAME = "jndiservers.xml";
    protected static Map<String, JndiServerInfo> jndiServerInfos = new HashMap<String, JndiServerInfo>();

    protected static Element getXmlRootElement() throws GenericConfigException {
        try {
            return ResourceLoader.getXmlRootElement(JNDIConfigUtil.JNDI_CONFIG_XML_FILENAME);
        } catch (GenericConfigException e) {
            throw new GenericConfigException("Could not get JNDI XML root element", e);
        }
    }

    protected static Document getXmlDocument() throws GenericConfigException {
        try {
            return ResourceLoader.getXmlDocument(JNDIConfigUtil.JNDI_CONFIG_XML_FILENAME);
        } catch (GenericConfigException e) {
            throw new GenericConfigException("Could not get JNDI XML document", e);
        }
    }

    static {
        try {
            initialize(getXmlRootElement());
        } catch (Exception e) {
            Debug.logError(e, "Error loading JNDI config XML file " + JNDI_CONFIG_XML_FILENAME);
        }
    }

    public static void initialize(Element rootElement) throws GenericConfigException {
        List<Element> childElements = null;
        Iterator<Element> elementIter = null;

        // jndi-server - jndiServerInfos
        childElements = UtilXml.childElementList(rootElement, "jndi-server");
        elementIter = childElements.iterator();
        while (elementIter.hasNext()) {
            Element curElement = elementIter.next();
            JNDIConfigUtil.JndiServerInfo jndiServerInfo = new JNDIConfigUtil.JndiServerInfo(curElement);

            JNDIConfigUtil.jndiServerInfos.put(jndiServerInfo.name, jndiServerInfo);
        }
    }

    public static JNDIConfigUtil.JndiServerInfo getJndiServerInfo(String name) {
        return jndiServerInfos.get(name);
    }

    public static class JndiServerInfo {
        public String name;
        public String contextProviderUrl;
        public String initialContextFactory;
        public String urlPkgPrefixes;
        public String securityPrincipal;
        public String securityCredentials;

        public JndiServerInfo(Element element) {
            this.name = element.getAttribute("name");
            this.contextProviderUrl = element.getAttribute("context-provider-url");
            this.initialContextFactory = element.getAttribute("initial-context-factory");
            this.urlPkgPrefixes = element.getAttribute("url-pkg-prefixes");
            this.securityPrincipal = element.getAttribute("security-principal");
            this.securityCredentials = element.getAttribute("security-credentials");
        }
    }
}
