/*
 * $Id: ModelGroupReader.java,v 1.1 2005/04/01 05:58:03 sfarquhar Exp $
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
package org.ofbiz.core.entity.model;

import org.ofbiz.core.config.GenericConfigException;
import org.ofbiz.core.config.ResourceHandler;
import org.ofbiz.core.entity.GenericEntityConfException;
import org.ofbiz.core.entity.config.EntityConfigUtil;
import org.ofbiz.core.util.Debug;
import org.ofbiz.core.util.UtilCache;
import org.ofbiz.core.util.UtilTimer;
import org.ofbiz.core.util.UtilXml;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.*;

/**
 * Generic Entity - Entity Group Definition Reader
 *
 * @author     <a href="mailto:jonesde@ofbiz.org">David E. Jones</a> 
 * @version    $Revision: 1.1 $
 * @since      2.0
 */
public class ModelGroupReader {

    public static UtilCache<String, ModelGroupReader> readers = new UtilCache<String, ModelGroupReader>("entity.ModelGroupReader", 0, 0);

    private Map<String, String> groupCache = null;
    private Set<String> groupNames = null;

    public String modelName;
    public ResourceHandler entityGroupResourceHandler;

    public static ModelGroupReader getModelGroupReader(String delegatorName) throws GenericEntityConfException {
        EntityConfigUtil.DelegatorInfo delegatorInfo = EntityConfigUtil.getInstance().getDelegatorInfo(delegatorName);

        if (delegatorInfo == null) {
            throw new GenericEntityConfException("Could not find a delegator with the name " + delegatorName);
        }

        String tempModelName = delegatorInfo.entityGroupReader;
        ModelGroupReader reader = readers.get(tempModelName);

        if (reader == null) { // don't want to block here
            synchronized (ModelGroupReader.class) {
                // must check if null again as one of the blocked threads can still enter
                reader = readers.get(tempModelName);
                if (reader == null) {
                    reader = new ModelGroupReader(tempModelName);
                    readers.put(tempModelName, reader);
                }
            }
        }
        return reader;
    }

    public ModelGroupReader(String modelName) throws GenericEntityConfException {
        this.modelName = modelName;
        EntityConfigUtil.EntityGroupReaderInfo entityGroupReaderInfo = EntityConfigUtil.getInstance().getEntityGroupReaderInfo(modelName);

        if (entityGroupReaderInfo == null) {
            throw new GenericEntityConfException("Cound not find an entity-group-reader with the name " + modelName);
        }
        entityGroupResourceHandler = new ResourceHandler(EntityConfigUtil.ENTITY_ENGINE_XML_FILENAME, entityGroupReaderInfo.resourceElement);

        // preload caches...
        getGroupCache();
    }

    public Map<String, String> getGroupCache() {
        if (groupCache == null) // don't want to block here
        {
            synchronized (ModelGroupReader.class) {
                // must check if null again as one of the blocked threads can still enter
                if (groupCache == null) // now it's safe
                {
                    groupCache = new HashMap<String, String>();
                    groupNames = new TreeSet<String>();

                    UtilTimer utilTimer = new UtilTimer();
                    // utilTimer.timerString("[ModelGroupReader.getGroupCache] Before getDocument");

                    Document document = null;

                    try {
                        document = entityGroupResourceHandler.getDocument();
                    } catch (GenericConfigException e) {
                        Debug.logError(e, "Error loading entity group model");
                    }
                    if (document == null) {
                        groupCache = null;
                        return null;
                    }

                    // utilTimer.timerString("[ModelGroupReader.getGroupCache] Before getDocumentElement");
                    Element docElement = document.getDocumentElement();

                    if (docElement == null) {
                        groupCache = null;
                        return null;
                    }
                    docElement.normalize();
                    Node curChild = docElement.getFirstChild();

                    int i = 0;

                    if (curChild != null) {
                        utilTimer.timerString("[ModelGroupReader.getGroupCache] Before start of entity loop");
                        do {
                            if (curChild.getNodeType() == Node.ELEMENT_NODE && "entity-group".equals(curChild.getNodeName())) {
                                Element curEntity = (Element) curChild;
                                String entityName = UtilXml.checkEmpty(curEntity.getAttribute("entity"));
                                String groupName = UtilXml.checkEmpty(curEntity.getAttribute("group"));

                                if (groupName == null || entityName == null) continue;
                                groupNames.add(groupName);
                                groupCache.put(entityName, groupName);
                                // utilTimer.timerString("  After entityEntityName -- " + i + " --");
                                i++;
                            }
                        } while ((curChild = curChild.getNextSibling()) != null);
                    } else
                        Debug.logWarning("[ModelGroupReader.getGroupCache] No child nodes found.");
                    utilTimer.timerString("[ModelGroupReader.getGroupCache] FINISHED - Total Entity-Groups: " + i + " FINISHED");
                }
            }
        }
        return groupCache;
    }

    /** Gets a group name based on a definition from the specified XML Entity Group descriptor file.
     * @param entityName The entityName of the Entity Group definition to use.
     * @return A group name
     */
    public String getEntityGroupName(String entityName) {
        Map<String, String> gc = getGroupCache();

        if (gc != null)
            return gc.get(entityName);
        else
            return null;
    }

    /** Creates a Collection with all of the groupNames defined in the specified XML Entity Group Descriptor file.
     * @return A Collection of groupNames Strings
     */
    public Collection<String> getGroupNames() {
        getGroupCache();
        if (groupNames == null) return null;
        return new ArrayList<String>(groupNames);
    }

    /** Creates a Collection with names of all of the entities for a given group
     * @return A Collection of entityName Strings
     */
    public Collection<String> getEntityNamesByGroup(String groupName) {
        Map<String, String> gc = getGroupCache();
        Collection<String> enames = new LinkedList<String>();

        if (groupName == null || groupName.length() <= 0) return enames;
        if (gc == null || gc.size() < 0) return enames;

        for (Map.Entry<String, String> gcEntry : gc.entrySet()) {
            if (groupName.equals(gcEntry.getValue())) enames.add(gcEntry.getKey());
        }
        return enames;
    }
}
