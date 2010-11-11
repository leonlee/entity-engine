/*
 * $Id: CachedClassLoader.java,v 1.1 2005/04/01 05:58:06 sfarquhar Exp $
 *
 * Copyright (c) 2003 The Open For Business Project - www.ofbiz.org
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

package org.ofbiz.core.util;

import java.util.*;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * Caching Class Loader
 *
 * @author     <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 * @version    $Revision: 1.1 $
 * @since      2.1
 */
public class CachedClassLoader extends URLClassLoader {
    private String contextName;

    public static Map globalClassNameClassMap = new HashMap();
    public static HashSet globalBadClassNameSet = new HashSet();

    public Map localClassNameClassMap = new HashMap();
    public HashSet localBadClassNameSet = new HashSet();

    public static Map globalResourceMap = new HashMap();
    public static HashSet globalBadResourceNameSet = new HashSet();

    public Map localResourceMap = new HashMap();
    public HashSet localBadResourceNameSet = new HashSet();

    static {
        // setup some commonly used classes...
        globalClassNameClassMap.put("Object", java.lang.Object.class);
        globalClassNameClassMap.put("java.lang.Object", java.lang.Object.class);

        globalClassNameClassMap.put("String", java.lang.String.class);
        globalClassNameClassMap.put("java.lang.String", java.lang.String.class);
        
        globalClassNameClassMap.put("Boolean", java.lang.Boolean.class);
        globalClassNameClassMap.put("java.lang.Boolean", java.lang.Boolean.class);

        globalClassNameClassMap.put("Double", java.lang.Double.class);
        globalClassNameClassMap.put("java.lang.Double", java.lang.Double.class);
        globalClassNameClassMap.put("Float", java.lang.Float.class);
        globalClassNameClassMap.put("java.lang.Float", java.lang.Float.class);
        globalClassNameClassMap.put("Long", java.lang.Long.class);
        globalClassNameClassMap.put("java.lang.Long", java.lang.Long.class);
        globalClassNameClassMap.put("Integer", java.lang.Integer.class);
        globalClassNameClassMap.put("java.lang.Integer", java.lang.Integer.class);

        globalClassNameClassMap.put("Timestamp", java.sql.Timestamp.class);
        globalClassNameClassMap.put("java.sql.Timestamp", java.sql.Timestamp.class);
        globalClassNameClassMap.put("Time", java.sql.Time.class);
        globalClassNameClassMap.put("java.sql.Time", java.sql.Time.class);
        globalClassNameClassMap.put("Date", java.sql.Date.class);
        globalClassNameClassMap.put("java.sql.Date", java.sql.Date.class);

        globalClassNameClassMap.put("Locale", java.util.Locale.class);
        globalClassNameClassMap.put("java.util.Locale", java.util.Locale.class);
        
        globalClassNameClassMap.put("java.util.Date", java.util.Date.class);
        globalClassNameClassMap.put("Collection", java.util.Collection.class);
        globalClassNameClassMap.put("java.util.Collection", java.util.Collection.class);
        globalClassNameClassMap.put("List", java.util.List.class);
        globalClassNameClassMap.put("java.util.List", java.util.List.class);
        globalClassNameClassMap.put("Set", java.util.Set.class);
        globalClassNameClassMap.put("java.util.Set", java.util.Set.class);
        globalClassNameClassMap.put("Map", java.util.Map.class);
        globalClassNameClassMap.put("java.util.Map", java.util.Map.class);
        globalClassNameClassMap.put("HashMap", java.util.HashMap.class);
        globalClassNameClassMap.put("java.util.HashMap", java.util.HashMap.class);

        try {
            ClassLoader loader = Thread.currentThread().getContextClassLoader();

            // note: loadClass is necessary for these since this class doesn't know anything about the Entity Engine at compile time
            globalClassNameClassMap.put("GenericValue", loader.loadClass("org.ofbiz.core.entity.GenericValue"));
            globalClassNameClassMap.put("org.ofbiz.core.entity.GenericValue", loader.loadClass("org.ofbiz.core.entity.GenericValue"));
            globalClassNameClassMap.put("GenericPK", loader.loadClass("org.ofbiz.core.entity.GenericPK"));
            globalClassNameClassMap.put("org.ofbiz.core.entity.GenericPK", loader.loadClass("org.ofbiz.core.entity.GenericPK"));
            globalClassNameClassMap.put("GenericEntity", loader.loadClass("org.ofbiz.core.entity.GenericEntity"));
            globalClassNameClassMap.put("org.ofbiz.core.entity.GenericEntity", loader.loadClass("org.ofbiz.core.entity.GenericEntity"));
        } catch (ClassNotFoundException e) {
            Debug.logError(e, "Could not pre-initialize dynamically loaded class: ");
        }
    }
    
    public CachedClassLoader(ClassLoader parent, String contextName) {
        super (new URL[0], parent);
        this.contextName = contextName;
    }
    
    public String toString() {
        return "org.ofbiz.core.util.CachedClassLoader(" + contextName + ") / " + getParent().toString();
    }
    
    public Class loadClass(String name) throws ClassNotFoundException {
        return loadClass(name, false);
    }
    
    protected Class loadClass(String name, boolean resolve) throws ClassNotFoundException {
        //check glocal common classes, ie for all instances
        Class theClass = (Class) globalClassNameClassMap.get(name);
        
        //check local classes, ie for this instance
        if (theClass == null) theClass = (Class) localClassNameClassMap.get(name);

        //make sure it is not a known bad class name
        if (theClass == null) {
            if (localBadClassNameSet.contains(name) || globalBadClassNameSet.contains(name)) {
                if (Debug.verboseOn()) Debug.logVerbose("Cached loader got a known bad class name: [" + name + "]");
                throw new ClassNotFoundException("Cached loader got a known bad class name: " + name);
            }
        }
        
        if (theClass == null) {
            if (Debug.verboseOn()) Debug.logVerbose("Cached loader cache miss for class name: [" + name + "]");
            
            synchronized (this) {
                theClass = (Class) localClassNameClassMap.get(name);
                if (theClass == null) {
                    try {
                        theClass = super.loadClass(name, resolve);
                        if (isGlobalPath(name)) {
                            globalClassNameClassMap.put(name, theClass);
                        } else {
                            localClassNameClassMap.put(name, theClass);
                        }
                    } catch (ClassNotFoundException e) {
                        //Debug.logInfo(e);
                        if (Debug.verboseOn()) Debug.logVerbose("Remembering invalid class name: [" + name + "]");
                        if (isGlobalPath(name)) {
                            globalBadClassNameSet.add(name);
                        } else {
                            localBadClassNameSet.add(name);
                        }
                        throw e;
                    }
                }
            }
        }
        return theClass;
    }
    
    public URL getResource(String name) {
        //check glocal common resources, ie for all instances
        URL theResource = (URL) globalResourceMap.get(name);
        
        //check local resources, ie for this instance
        if (theResource == null) theResource = (URL) localResourceMap.get(name);

        //make sure it is not a known bad resource name
        if (theResource == null) {
            if (localBadResourceNameSet.contains(name) || globalBadResourceNameSet.contains(name)) {
                if (Debug.verboseOn()) Debug.logVerbose("Cached loader got a known bad resource name: [" + name + "]");
                return null;
            }
        }
        
        if (theResource == null) {
            //if (Debug.verboseOn()) Debug.logVerbose("Cached loader cache miss for resource name: [" + name + "]");
            Debug.logInfo("Cached loader cache miss for resource name: [" + name + "]");
            
            synchronized (this) {
                theResource = (URL) localResourceMap.get(name);
                if (theResource == null) {
                    theResource = super.getResource(name);
                    if (theResource == null) {
                        //if (Debug.verboseOn()) Debug.logVerbose("Remembering invalid resource name: [" + name + "]");
                        Debug.logInfo("Remembering invalid resource name: [" + name + "]");
                        if (isGlobalPath(name)) {
                            globalBadResourceNameSet.add(name);
                        } else {
                            localBadResourceNameSet.add(name);
                        }
                    } else {
                        if (isGlobalPath(name)) {
                            globalResourceMap.put(name, theResource);
                        } else {
                            localResourceMap.put(name, theResource);
                        }
                    }
                }
            }
        }
        return theResource;
    }
    
    protected boolean isGlobalPath(String name) {
        if (name.startsWith("java.") || name.startsWith("java/") || name.startsWith("/java/")) return true;
        if (name.startsWith("javax.") || name.startsWith("javax/") || name.startsWith("/javax/")) return true;
        if (name.startsWith("sun.") || name.startsWith("sun/") || name.startsWith("/sun/")) return true;
        if (name.startsWith("org.ofbiz.core.")) return true;
        return false;
    }
}
