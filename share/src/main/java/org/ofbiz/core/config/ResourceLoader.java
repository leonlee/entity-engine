/*
 * $Id: ResourceLoader.java,v 1.1 2005/04/01 05:58:05 sfarquhar Exp $
 *
 * Copyright (c) 2002 The Open For Business Project - www.ofbiz.org
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
import org.ofbiz.core.util.UtilCache;
import org.ofbiz.core.util.UtilURL;
import org.ofbiz.core.util.UtilXml;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;


/**
 * Loads resources using dynamically specified resource loader classes
 *
 *@author     <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 *@created    March 1, 2002
 *@version    1.0
 */
public abstract class ResourceLoader {
    protected static UtilCache<String, ResourceLoader> loaderCache = new UtilCache<String, ResourceLoader>("resource.ResourceLoaders", 0, 0);
    protected static Map<String, Document> docSaveMap = new HashMap<String, Document>();

    protected String name;
    protected String prefix;
    protected String envName;

    public static InputStream loadResource(String xmlFilename, String location, String loaderName) throws GenericConfigException {
        ResourceLoader loader = getLoader(xmlFilename, loaderName);

        if (loader == null) throw new IllegalArgumentException("ResourceLoader not found with name [" + loaderName + "] in " + xmlFilename);
        return loader.loadResource(location);
    }

    public static ResourceLoader getLoader(String xmlFilename, String loaderName) throws GenericConfigException {
        ResourceLoader loader = loaderCache.get(xmlFilename + "::" + loaderName);

        if (loader == null) {
            synchronized (ResourceLoader.class) {
                loader = loaderCache.get(xmlFilename + "::" + loaderName);
                if (loader == null) {
                    Element rootElement = getXmlRootElement(xmlFilename);

                    Element loaderElement = UtilXml.firstChildElement(rootElement, "resource-loader", "name", loaderName);

                    loader = makeLoader(loaderElement);

                    if (loader != null) {
                        loaderCache.put(xmlFilename + "::" + loaderName, loader);
                    }
                }
            }
        }

        return loader;
    }

    public static Element getXmlRootElement(String xmlFilename) throws GenericConfigException {
        Document document = ResourceLoader.getXmlDocument(xmlFilename);

        if (document != null) {
            return document.getDocumentElement();
        } else {
            return null;
        }
    }

    public static void invalidateDocument(String xmlFilename) throws GenericConfigException {
        synchronized (ResourceLoader.class) {
            docSaveMap.remove(xmlFilename);
        }
    }

    public static Document getXmlDocument(String xmlFilename) throws GenericConfigException {
        Document document = docSaveMap.get(xmlFilename);

        if (document == null) {
            synchronized (ResourceLoader.class) {
                document = docSaveMap.get(xmlFilename);
                if (document == null) {
                    URL confUrl = UtilURL.fromResource(xmlFilename);

                    if (confUrl == null) {
                        throw new GenericConfigException("ERROR: could not find the [" + xmlFilename + "] XML file on the classpath");
                    }

                    try {
                        document = UtilXml.readXmlDocument(confUrl);
                    } catch (org.xml.sax.SAXException e) {
                        throw new GenericConfigException("Error reading " + xmlFilename + "", e);
                    } catch (javax.xml.parsers.ParserConfigurationException e) {
                        throw new GenericConfigException("Error reading " + xmlFilename + "", e);
                    } catch (java.io.IOException e) {
                        throw new GenericConfigException("Error reading " + xmlFilename + "", e);
                    }

                    if (document != null) {
                        docSaveMap.put(xmlFilename, document);
                    }
                }
            }
        }
        return document;
    }

    public static ResourceLoader makeLoader(Element loaderElement) throws GenericConfigException {
        if (loaderElement == null)
            return null;

        String loaderName = loaderElement.getAttribute("name");
        String className = loaderElement.getAttribute("class");
        ResourceLoader loader = null;

        try {
            Class<?> lClass = null;

            if (className != null && className.length() > 0) {
                try {
                    ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
                    lClass = classLoader.loadClass(className);
                } catch (ClassNotFoundException e) {
                    throw new GenericConfigException("Error loading Resource Loader class \"" + className + "\"", e);
                }
            }

            try {
                loader = (ResourceLoader) lClass.newInstance();
            } catch (IllegalAccessException e) {
                throw new GenericConfigException("Error loading Resource Loader class \"" + className + "\"", e);
            } catch (InstantiationException e) {
                throw new GenericConfigException("Error loading Resource Loader class \"" + className + "\"", e);
            }
        } catch (SecurityException e) {
            throw new GenericConfigException("Error loading Resource Loader class \"" + className + "\"", e);
        }

        if (loader != null) {
            loader.init(loaderName, loaderElement.getAttribute("prefix"), loaderElement.getAttribute("prepend-env"));
        }

        return loader;
    }

    protected ResourceLoader() {}

    public void init(String name, String prefix, String envName) {
        this.name = name;
        this.prefix = prefix;
        this.envName = envName;
    }

    /** Just a utility method to be used in loadResource by the implementing class */
    public String fullLocation(String location) {
        StringBuffer buf = new StringBuffer();

        if (envName != null && envName.length() > 0) {
            String propValue = System.getProperty(envName);
            if (propValue == null) {
                String errMsg = "The Java environment (-Dxxx=yyy) variable with name " + envName + " is not set, cannot load resource.";
                Debug.logError(errMsg);
                throw new IllegalArgumentException(errMsg);
            }
            buf.append(propValue);
        }
        if (prefix != null && prefix.length() > 0) {
            buf.append(prefix);
        }
        buf.append(location);
        return buf.toString();
    }

    public abstract InputStream loadResource(String location) throws GenericConfigException;
}
