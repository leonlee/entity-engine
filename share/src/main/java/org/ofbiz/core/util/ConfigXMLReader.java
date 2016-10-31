/*
 * $Id: ConfigXMLReader.java,v 1.1 2005/04/01 05:58:06 sfarquhar Exp $
 *
 *  Copyright (c) 2001, 2002 The Open For Business Project - www.ofbiz.org
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
package org.ofbiz.core.util;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * ConfigXMLReader.java - Reads and parses the XML site config files.
 *
 * @author <a href="mailto:jaz@ofbiz.org">Andy Zeneski</a>
 * @version $Revision: 1.1 $
 * @since 2.0
 */
public class ConfigXMLReader {

    public static final String module = ConfigXMLReader.class.getName();

    public static UtilCache<URL, HashMap<String, HashMap<String, String>>> requestCache = new UtilCache<URL, HashMap<String, HashMap<String, String>>>("webapp.ConfigXMLReader.Request");
    public static UtilCache<URL, Map<String, Map<String, String>>> viewCache = new UtilCache<URL, Map<String, Map<String, String>>>("webapp.ConfigXMLReader.View");
    public static UtilCache<URL, Map<String, Object>> headCache = new UtilCache<URL, Map<String, Object>>("webapp.ConfigXMLReader.Config");
    public static UtilCache<URL, Map<String, Map<String, String>>> handlerCache = new UtilCache<URL, Map<String, Map<String, String>>>("webapp.ConfigXMLReader.Handler");

    /**
     * Site Config Variables
     */
    public static final String DEFAULT_ERROR_PAGE = "errorpage";
    public static final String SITE_OWNER = "owner";
    public static final String SECURITY_CLASS = "security-class";
    public static final String FIRSTVISIT = "firstvisit";
    public static final String PREPROCESSOR = "preprocessor";
    public static final String POSTPROCESSOR = "postprocessor";

    /**
     * URI Config Variables
     */
    public static final String INCLUDE = "include";
    public static final String INCLUDE_FILE = "file";
    public static final String INCLUDE_URL = "url";

    public static final String REQUEST_MAPPING = "request-map";
    public static final String REQUEST_URI = "uri";
    public static final String REQUEST_EDIT = "edit";

    public static final String REQUEST_DESCRIPTION = "description";
    public static final String ERROR_PAGE = "error";
    public static final String NEXT_PAGE = "success";

    public static final String SECURITY = "security";
    public static final String SECURITY_HTTPS = "https";
    public static final String SECURITY_AUTH = "auth";
    public static final String SECURITY_EXTVIEW = "external-view";
    public static final String SECURITY_DIRECT = "direct-request";

    public static final String EVENT = "event";
    public static final String EVENT_PATH = "path";
    public static final String EVENT_TYPE = "type";
    public static final String EVENT_METHOD = "invoke";

    public static final String RESPONSE = "response";
    public static final String RESPONSE_NAME = "name";
    public static final String RESPONSE_TYPE = "type";
    public static final String RESPONSE_VALUE = "value";

    /**
     * View Config Variables
     */
    public static final String VIEW_MAPPING = "view-map";
    public static final String VIEW_NAME = "name";
    public static final String VIEW_PAGE = "page";
    public static final String VIEW_TYPE = "type";
    public static final String VIEW_INFO = "info";
    public static final String VIEW_CONTENT_TYPE = "content-type";
    public static final String VIEW_ENCODING = "encoding";
    public static final String VIEW_DESCRIPTION = "description";

    /**
     * Handler Config Variables
     */
    public static final String HANDLER = "handler";
    public static final String HANDLER_NAME = "name";
    public static final String HANDLER_TYPE = "type";
    public static final String HANDLER_CLASS = "class";

    /**
     * Loads the XML file and returns the root element
     */
    public static Element loadDocument(URL location) {
        Document document = null;

        try {
            document = UtilXml.readXmlDocument(location, true);

            Element rootElement = document.getDocumentElement();

            // rootElement.normalize();
            if (Debug.verboseOn()) Debug.logVerbose("Loaded XML Config - " + location, module);
            return rootElement;
        } catch (Exception e) {
            Debug.logError(e, module);
        }

        return null;
    }

    /**
     * Gets a HashMap of request mappings.
     */
    public static HashMap<String, HashMap<String, String>> getRequestMap(URL xml) {
        HashMap<String, HashMap<String, String>> requestMap = requestCache.get(xml);

        if (requestMap == null) // don't want to block here
        {
            synchronized (ConfigXMLReader.class) {
                // must check if null again as one of the blocked threads can still enter
                requestMap = requestCache.get(xml);
                if (requestMap == null) {
                    requestMap = loadRequestMap(xml);
                    requestCache.put(xml, requestMap);
                }
            }
        }
        // never return null, just an empty map...
        if (requestMap == null) requestMap = new HashMap<String, HashMap<String, String>>();
        return requestMap;
    }

    /**
     * Gets a HashMap of request mappings.
     */
    public static HashMap<String, HashMap<String, String>> loadRequestMap(URL xml) {
        HashMap<String, HashMap<String, String>> map = new HashMap<String, HashMap<String, String>>();
        Element root = loadDocument(xml);

        if (root == null) return map;

        NodeList list = root.getElementsByTagName(INCLUDE);

        for (int rootCount = 0; rootCount < list.getLength(); rootCount++) {
            Node node = list.item(rootCount);

            // Make sure we are an element.
            if (node instanceof Element) {
                // Get the file to include
                Element mapping = (Element) node;
                String includeFile = mapping.getAttribute(INCLUDE_FILE);

                if ((includeFile != null) && (includeFile.length() > 0)) {
                    File oldFile = new File(xml.getFile());
                    File newFile = new java.io.File("" + oldFile.getParent() + java.io.File.separator + includeFile);

                    try {
                        HashMap<String, HashMap<String, String>> subMap = loadRequestMap(newFile.toURL());

                        map.putAll(subMap);
                    } catch (MalformedURLException mue) {
                        mue.printStackTrace();
                    }
                }

                String includeURL = mapping.getAttribute(INCLUDE_URL);

                if ((includeURL != null) && (includeURL.length() > 0)) {
                    try {
                        HashMap<String, HashMap<String, String>> subMap = loadRequestMap(new URL(includeURL));

                        map.putAll(subMap);
                    } catch (MalformedURLException mue) {
                        mue.printStackTrace();
                    }
                }

            }
        }

        list = root.getElementsByTagName(REQUEST_MAPPING);
        for (int rootCount = 0; rootCount < list.getLength(); rootCount++) {
            // Create a URI-MAP for each element found.
            HashMap<String, String> uriMap = new HashMap<String, String>();
            // Get the node.
            Node node = list.item(rootCount);

            // Make sure we are an element.
            if (node instanceof Element) {
                // Get the URI info.
                Element mapping = (Element) node;
                String uri = mapping.getAttribute(REQUEST_URI);
                String edit = mapping.getAttribute(REQUEST_EDIT);

                if (edit == null || edit.equals(""))
                    edit = "true";
                if (uri != null) {
                    uriMap.put(REQUEST_URI, uri);
                    uriMap.put(REQUEST_EDIT, edit);
                }

                // Check for security.
                NodeList securityList = mapping.getElementsByTagName(SECURITY);

                if (securityList.getLength() > 0) {
                    Node securityNode = securityList.item(0);  // There should be only one.

                    if (securityNode instanceof Element) {       // We must be an element.
                        Element security = (Element) securityNode;
                        String securityHttps = security.getAttribute(SECURITY_HTTPS);
                        String securityAuth = security.getAttribute(SECURITY_AUTH);
                        String securityExtView = security.getAttribute(SECURITY_EXTVIEW);
                        String securityDirectRequest = security.getAttribute(SECURITY_DIRECT);

                        uriMap.put(SECURITY_HTTPS, securityHttps);
                        uriMap.put(SECURITY_AUTH, securityAuth);
                        uriMap.put(SECURITY_EXTVIEW, securityExtView);
                        uriMap.put(SECURITY_DIRECT, securityDirectRequest);
                    }
                }

                // Check for an event.
                NodeList eventList = mapping.getElementsByTagName(EVENT);

                if (eventList.getLength() > 0) {
                    Node eventNode = eventList.item(0);  // There should be only one.

                    if (eventNode instanceof Element) {   // We must be an element.
                        Element event = (Element) eventNode;
                        String type = event.getAttribute(EVENT_TYPE);
                        String path = event.getAttribute(EVENT_PATH);
                        String invoke = event.getAttribute(EVENT_METHOD);

                        uriMap.put(EVENT_TYPE, type);
                        uriMap.put(EVENT_PATH, path);
                        uriMap.put(EVENT_METHOD, invoke);
                    }
                }

                // Check for a description.
                NodeList descList = mapping.getElementsByTagName(REQUEST_DESCRIPTION);

                if (descList.getLength() > 0) {
                    Node descNode = descList.item(0);   // There should be only one.

                    if (descNode instanceof Element) {   // We must be an element.
                        NodeList children = descNode.getChildNodes();

                        if (children.getLength() > 0) {
                            Node cdata = children.item(0);  // Just get the first one.
                            String description = cdata.getNodeValue();

                            if (description != null)
                                description = description.trim();
                            else
                                description = "";
                            uriMap.put(REQUEST_DESCRIPTION, description);
                        }
                    }
                } else {
                    uriMap.put(REQUEST_DESCRIPTION, "");
                }

                // Get the response(s).
                NodeList respList = mapping.getElementsByTagName(RESPONSE);

                for (int respCount = 0; respCount < respList.getLength(); respCount++) {
                    Node responseNode = respList.item(respCount);

                    if (responseNode instanceof Element) {
                        Element response = (Element) responseNode;
                        String name = response.getAttribute(RESPONSE_NAME);
                        String type = response.getAttribute(RESPONSE_TYPE);
                        String value = response.getAttribute(RESPONSE_VALUE);

                        uriMap.put(name, type + ":" + value);
                    }
                }

                if (uri != null)
                    map.put(uri, uriMap);
            }

        }

        /* Debugging */
        if (Debug.verboseOn()) Debug.logVerbose("-------- Request Mappings --------", module);
        Set<String> debugSet = map.keySet();

        for (String request : debugSet) {
            HashMap<String, String> thisURI = map.get(request);

            if (Debug.verboseOn()) Debug.logVerbose(request, module);

            for (String name : thisURI.keySet()) {
                String value = thisURI.get(name);

                if (Debug.verboseOn()) Debug.logVerbose("\t" + name + " -> " + value, module);
            }
        }
        if (Debug.verboseOn()) Debug.logVerbose("------ End Request Mappings ------", module);

        /* End Debugging */

        if (Debug.infoOn()) Debug.logInfo("RequestMap Created: (" + map.size() + ") records.", module);
        return map;
    }

    /**
     * Gets a HashMap of view mappings.
     */
    public static Map<String, Map<String, String>> getViewMap(URL xml) {
        Map<String, Map<String, String>> viewMap = viewCache.get(xml);

        if (viewMap == null) // don't want to block here
        {
            synchronized (ConfigXMLReader.class) {
                // must check if null again as one of the blocked threads can still enter
                viewMap = viewCache.get(xml);
                if (viewMap == null) {
                    viewMap = loadViewMap(xml);
                    viewCache.put(xml, viewMap);
                }
            }
        }
        // never return null, just an empty map...
        if (viewMap == null) viewMap = new HashMap<String, Map<String, String>>();
        return viewMap;
    }

    /**
     * Gets a HashMap of view mappings.
     */
    public static Map<String, Map<String, String>> loadViewMap(URL xml) {
        HashMap<String, Map<String, String>> map = new HashMap<String, Map<String, String>>();
        Element root = loadDocument(xml);

        if (root == null)
            return map;

        NodeList list = root.getElementsByTagName(INCLUDE);

        for (int rootCount = 0; rootCount < list.getLength(); rootCount++) {
            Node node = list.item(rootCount);

            // Make sure we are an element.
            if (node instanceof Element) {
                // Get the file to include
                Element mapping = (Element) node;
                String includeFile = mapping.getAttribute(INCLUDE_FILE);

                if ((includeFile != null) && (includeFile.length() > 0)) {
                    File oldFile = new File(xml.getFile());
                    File newFile = new java.io.File("" + oldFile.getParent() + java.io.File.separator + includeFile);

                    try {
                        Map<String, Map<String, String>> subMap = loadViewMap(newFile.toURL());

                        map.putAll(subMap);
                    } catch (MalformedURLException mue) {
                        mue.printStackTrace();
                    }
                }

                String includeURL = mapping.getAttribute(INCLUDE_URL);

                if ((includeURL != null) && (includeURL.length() > 0)) {
                    try {
                        Map<String, Map<String, String>> subMap = loadViewMap(new URL(includeURL));

                        map.putAll(subMap);
                    } catch (MalformedURLException mue) {
                        mue.printStackTrace();
                    }
                }

            }
        }

        list = root.getElementsByTagName(VIEW_MAPPING);
        for (int rootCount = 0; rootCount < list.getLength(); rootCount++) {
            // Create a URI-MAP for each element found.
            HashMap<String, String> uriMap = new HashMap<String, String>();
            // Get the node.
            Node node = list.item(rootCount);

            // Make sure we are an element.
            if (node instanceof Element) {
                // Get the view info.
                Element mapping = (Element) node;
                String name = mapping.getAttribute(VIEW_NAME);
                String page = mapping.getAttribute(VIEW_PAGE);
                String type = mapping.getAttribute(VIEW_TYPE);

                if (page == null || page.length() == 0) {
                    page = name;
                }

                uriMap.put(VIEW_NAME, name);
                uriMap.put(VIEW_PAGE, page);
                uriMap.put(VIEW_TYPE, type);
                uriMap.put(VIEW_INFO, mapping.getAttribute(VIEW_INFO));
                uriMap.put(VIEW_CONTENT_TYPE, mapping.getAttribute(VIEW_CONTENT_TYPE));
                uriMap.put(VIEW_ENCODING, mapping.getAttribute(VIEW_ENCODING));

                // Check for a description.
                NodeList descList = mapping.getElementsByTagName(VIEW_DESCRIPTION);

                if (descList.getLength() > 0) {
                    Node descNode = descList.item(0);   // There should be only one.

                    if (descNode instanceof Element) {   // We must be an element.
                        NodeList children = descNode.getChildNodes();

                        if (children.getLength() > 0) {
                            Node cdata = children.item(0);  // Just get the first one.
                            String description = cdata.getNodeValue();

                            if (description != null)
                                description = description.trim();
                            else
                                description = "";
                            uriMap.put(VIEW_DESCRIPTION, description);
                        }
                    }
                } else {
                    uriMap.put(VIEW_DESCRIPTION, "");
                }

                if (name != null)
                    map.put(name, uriMap);
            }
        }

        /* Debugging */
        Debug.logVerbose("-------- View Mappings --------", module);
        Set<String> debugSet = map.keySet();

        for (String request : debugSet) {
            Map<String, String> thisURI = map.get(request);

            Debug.logVerbose(request, module);

            for (String name : (thisURI.keySet())) {
                String value = thisURI.get(name);

                if (Debug.verboseOn()) Debug.logVerbose("\t" + name + " -> " + value, module);
            }
        }
        Debug.logVerbose("------ End View Mappings ------", module);

        /* End Debugging */

        if (Debug.infoOn()) Debug.logInfo("ViewMap Created: (" + map.size() + ") records.", module);
        return map;
    }

    /**
     * Gets a HashMap of site configuration variables.
     */
    public static Map<String, Object> getConfigMap(URL xml) {
        Map<String, Object> configMap = headCache.get(xml);

        if (configMap == null) // don't want to block here
        {
            synchronized (ConfigXMLReader.class) {
                // must check if null again as one of the blocked threads can still enter
                configMap = headCache.get(xml);
                if (configMap == null) {
                    configMap = loadConfigMap(xml);
                    headCache.put(xml, configMap);
                }
            }
        }
        // never return null, just an empty map...
        if (configMap == null) configMap = new HashMap<String, Object>();
        return configMap;
    }

    /**
     * Gets a HashMap of site configuration variables.
     */
    public static Map<String, Object> loadConfigMap(URL xml) {
        HashMap<String, Object> map = new HashMap<String, Object>();
        Element root = loadDocument(xml);
        NodeList list = null;

        if (root != null) {
            // default error page
            list = root.getElementsByTagName(DEFAULT_ERROR_PAGE);
            if (list.getLength() > 0) {
                Node node = list.item(0);
                NodeList children = node.getChildNodes();
                Node child = children.item(0);

                if (child.getNodeName() != null)
                    map.put(DEFAULT_ERROR_PAGE, child.getNodeValue());
            }
            list = null;
            // site owner
            list = root.getElementsByTagName(SITE_OWNER);
            if (list.getLength() > 0) {
                Node node = list.item(0);
                NodeList children = node.getChildNodes();
                Node child = children.item(0);

                if (child.getNodeName() != null)
                    map.put(SITE_OWNER, child.getNodeValue());
            }
            list = null;
            // security class
            list = root.getElementsByTagName(SECURITY_CLASS);
            if (list.getLength() > 0) {
                Node node = list.item(0);
                NodeList children = node.getChildNodes();
                Node child = children.item(0);

                if (child.getNodeName() != null)
                    map.put(SECURITY_CLASS, child.getNodeValue());
            }
            list = null;
            // first visit events
            list = root.getElementsByTagName(FIRSTVISIT);
            if (list.getLength() > 0) {
                ArrayList<Map<String, String>> eventList = new ArrayList<Map<String, String>>();
                Node node = list.item(0);

                if (node instanceof Element) {
                    Element nodeElement = (Element) node;
                    NodeList procEvents = nodeElement.getElementsByTagName(EVENT);

                    for (int procCount = 0; procCount < procEvents.getLength(); procCount++) {
                        Node eventNode = procEvents.item(procCount);

                        if (eventNode instanceof Element) {
                            Element event = (Element) eventNode;
                            String type = event.getAttribute(EVENT_TYPE);
                            String path = event.getAttribute(EVENT_PATH);
                            String invoke = event.getAttribute(EVENT_METHOD);

                            HashMap<String, String> eventMap = new HashMap<String, String>();

                            eventMap.put(EVENT_TYPE, type);
                            eventMap.put(EVENT_PATH, path);
                            eventMap.put(EVENT_METHOD, invoke);
                            eventList.add(eventMap);
                        }
                    }
                }
                map.put(FIRSTVISIT, eventList);
            }
            list = null;
            // preprocessor events
            list = root.getElementsByTagName(PREPROCESSOR);
            if (list.getLength() > 0) {
                ArrayList<Map<String, String>> eventList = new ArrayList<Map<String, String>>();
                Node node = list.item(0);

                if (node instanceof Element) {
                    Element nodeElement = (Element) node;
                    NodeList procEvents = nodeElement.getElementsByTagName(EVENT);

                    for (int procCount = 0; procCount < procEvents.getLength(); procCount++) {
                        Node eventNode = procEvents.item(procCount);

                        if (eventNode instanceof Element) {
                            Element event = (Element) eventNode;
                            String type = event.getAttribute(EVENT_TYPE);
                            String path = event.getAttribute(EVENT_PATH);
                            String invoke = event.getAttribute(EVENT_METHOD);

                            HashMap<String, String> eventMap = new HashMap<String, String>();

                            eventMap.put(EVENT_TYPE, type);
                            eventMap.put(EVENT_PATH, path);
                            eventMap.put(EVENT_METHOD, invoke);
                            eventList.add(eventMap);
                        }
                    }
                }
                map.put(PREPROCESSOR, eventList);
            }
            list = null;
            // postprocessor events
            list = root.getElementsByTagName(POSTPROCESSOR);
            if (list.getLength() > 0) {
                ArrayList<Map<String, String>> eventList = new ArrayList<Map<String, String>>();
                Node node = list.item(0);

                if (node instanceof Element) {
                    Element nodeElement = (Element) node;
                    NodeList procEvents = nodeElement.getElementsByTagName(EVENT);

                    for (int procCount = 0; procCount < procEvents.getLength(); procCount++) {
                        Node eventNode = procEvents.item(procCount);

                        if (eventNode instanceof Element) {
                            Element event = (Element) eventNode;
                            String type = event.getAttribute(EVENT_TYPE);
                            String path = event.getAttribute(EVENT_PATH);
                            String invoke = event.getAttribute(EVENT_METHOD);

                            HashMap<String, String> eventMap = new HashMap<String, String>();

                            eventMap.put(EVENT_TYPE, type);
                            eventMap.put(EVENT_PATH, path);
                            eventMap.put(EVENT_METHOD, invoke);
                            eventList.add(eventMap);
                        }
                    }
                }
                map.put(POSTPROCESSOR, eventList);
            }
            list = null;
        }

        /* Debugging */

        /*
         Debug.logVerbose("-------- Config Mappings --------", module);
         HashMap debugMap = map;
         Set debugSet = debugMap.keySet();
         Iterator i = debugSet.iterator();
         while (i.hasNext()) {
         Object o = i.next();
         String request = (String) o;
         HashMap thisURI = (HashMap) debugMap.get(o);
         Debug.logVerbose(request, module);
         Iterator debugIter = ((Set) thisURI.keySet()).iterator();
         while (debugIter.hasNext()) {
         Object lo = debugIter.next();
         String name = (String) lo;
         String value = (String) thisURI.get(lo);
         if (Debug.verboseOn()) Debug.logVerbose("\t" + name + " -> " + value, module);
         }
         }
         Debug.logVerbose("------ End Config Mappings ------", module);
         */

        /* End Debugging */

        if (Debug.infoOn()) Debug.logInfo("ConfigMap Created: (" + map.size() + ") records.", module);
        return map;
    }

    /**
     * Gets a HashMap of handler mappings.
     */
    public static Map<String, Map<String, String>> getHandlerMap(URL xml) {
        Map<String, Map<String, String>> handlerMap = handlerCache.get(xml);

        if (handlerMap == null) // don't want to block here
        {
            synchronized (ConfigXMLReader.class) {
                // must check if null again as one of the blocked threads can still enter
                handlerMap = handlerCache.get(xml);
                if (handlerMap == null) {
                    handlerMap = loadHandlerMap(xml);
                    handlerCache.put(xml, handlerMap);
                }
            }
        }
        // never return null, just an empty map...
        if (handlerMap == null) handlerMap = new HashMap<String, Map<String, String>>();
        return handlerMap;
    }

    public static Map<String, Map<String, String>> loadHandlerMap(URL xml) {
        HashMap<String, Map<String, String>> map = new HashMap<String, Map<String, String>>();
        Element root = loadDocument(xml);
        NodeList list = null;

        if (root != null) {
            Map<String, String> rMap = new HashMap<String, String>();
            Map<String, String> vMap = new HashMap<String, String>();

            list = root.getElementsByTagName(HANDLER);
            for (int i = 0; i < list.getLength(); i++) {
                Element handler = (Element) list.item(i);
                String hName = checkEmpty(handler.getAttribute(HANDLER_NAME));
                String hClass = checkEmpty(handler.getAttribute(HANDLER_CLASS));
                String hType = checkEmpty(handler.getAttribute(HANDLER_TYPE));

                if (hType.equals("view"))
                    vMap.put(hName, hClass);
                else
                    rMap.put(hName, hClass);
            }
            map.put("view", vMap);
            map.put("event", rMap);
        }

        /* Debugging */
        Debug.logVerbose("-------- Handler Mappings --------", module);
        Map<String, String> debugMap = map.get("event");

        if (debugMap != null && debugMap.size() > 0) {
            Debug.logVerbose("-------------- EVENT -------------", module);
            Set<String> debugSet = debugMap.keySet();

            for (String handlerName : debugSet) {
                String className = debugMap.get(handlerName);

                if (Debug.verboseOn()) Debug.logVerbose("[EH] : " + handlerName + " => " + className, module);
            }
        }
        debugMap = map.get("view");
        if (debugMap != null && debugMap.size() > 0) {
            Debug.logVerbose("-------------- VIEW --------------", module);
            Set<String> debugSet = debugMap.keySet();

            for (String handlerName : debugSet) {
                String className = debugMap.get(handlerName);

                if (Debug.verboseOn()) Debug.logVerbose("[VH] : " + handlerName + " => " + className, module);
            }
        }
        Debug.logVerbose("------ End Handler Mappings ------", module);

        /* End Debugging */

        if (Debug.infoOn()) Debug.logInfo("HandlerMap Created: (" + map.size() + ") records.", module);
        return map;
    }

    private static String checkEmpty(String string) {
        if (string != null && string.length() > 0)
            return string;
        else
            return "";
    }

    /**
     * Not used right now
     */
    public static String getSubTagValue(Node node, String subTagName) {
        String returnString = "";

        if (node != null) {
            NodeList children = node.getChildNodes();

            for (int innerLoop = 0; innerLoop < children.getLength(); innerLoop++) {
                Node child = children.item(innerLoop);

                if ((child != null) && (child.getNodeName() != null) && child.getNodeName().equals(subTagName)) {
                    Node grandChild = child.getFirstChild();

                    if (grandChild.getNodeValue() != null)
                        return grandChild.getNodeValue();
                }
            }
        }
        return returnString;
    }

    public static void main(String args[]) throws Exception {

        /** Debugging */
        if (args[0] == null) {
            System.out.println("Please give a path to the config file you wish to test.");
            return;
        }
        System.out.println("----------------------------------");
        System.out.println("Request Mappings:");
        System.out.println("----------------------------------");
        HashMap<String, HashMap<String, String>> debugMap = getRequestMap(new URL(args[0]));
        java.util.Set<String> debugSet = debugMap.keySet();

        for (String request : debugSet) {
            HashMap<String, String> thisURI = debugMap.get(request);

            System.out.println(request);

            for (String name : thisURI.keySet()) {
                String value = thisURI.get(name);

                System.out.println("\t" + name + " -> " + value);
            }
        }
        System.out.println("----------------------------------");
        System.out.println("End Request Mappings.");
        System.out.println("----------------------------------");

        /** End Debugging */
    }

}
