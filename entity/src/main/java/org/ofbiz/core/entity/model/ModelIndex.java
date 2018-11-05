/*
 * $Id: ModelIndex.java,v 1.1 2005/04/01 05:58:03 sfarquhar Exp $
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

import org.ofbiz.core.entity.GenericEntityException;
import org.ofbiz.core.entity.jdbc.DatabaseUtil;
import org.ofbiz.core.entity.jdbc.alternative.IndexAlternativeAction;
import org.ofbiz.core.entity.util.ClassLoaderUtils;
import org.ofbiz.core.util.UtilXml;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

/**
 * Generic Entity - Relation model class
 *
 * @author <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 * @version $Revision: 1.1 $
 * @since 2.0
 */
public class ModelIndex {

    /**
     * reference to the entity this index refers to
     */
    protected ModelEntity mainEntity;

    /**
     * the index name, used for the database index name
     */
    protected String name;

    /**
     * specifies whether or not this index should include the unique constraint
     */
    protected boolean unique;

    /**
     * list of the field names included in this index
     */
    protected List<String> fieldNames = new ArrayList<>();

    protected List<IndexAlternativeAction> alternativeActions = new ArrayList<>();

    /**
     * Default Constructor
     */
    public ModelIndex() {
        name = "";
        unique = false;
    }

    /**
     * XML Constructor
     */
    public ModelIndex(ModelEntity mainEntity, Element indexElement) {
        this.mainEntity = mainEntity;
        populateFieldsFromIndexElement(indexElement);
        instantiateAlternativeActionsFromIndexElement(indexElement);
    }

    private void populateFieldsFromIndexElement(Element indexElement) {
        this.name = UtilXml.checkEmpty(indexElement.getAttribute("name"));
        this.unique = "true".equals(UtilXml.checkEmpty(indexElement.getAttribute("unique")));

        NodeList indexFieldList = indexElement.getElementsByTagName("index-field");
        for (int i = 0; i < indexFieldList.getLength(); i++) {
            Element indexFieldElement = (Element) indexFieldList.item(i);

            if (indexFieldElement.getParentNode() == indexElement) {
                String fieldName = indexFieldElement.getAttribute("name");
                this.fieldNames.add(fieldName);
            }
        }
    }

    private void instantiateAlternativeActionsFromIndexElement(Element indexElement) {
        NodeList alternativeActionList = indexElement.getElementsByTagName("alternative");

        for (int i = 0; i < alternativeActionList.getLength(); i++) {
            try {
                Element alternativeAction = (Element) alternativeActionList.item(i);
                String actionClass = alternativeAction.getAttribute("action");

                IndexAlternativeAction indexAlternativeAction =
                        (IndexAlternativeAction) ClassLoaderUtils
                                .loadClass(actionClass, ModelIndex.class)
                                .newInstance();

                alternativeActions.add(indexAlternativeAction);
            } catch (ReflectiveOperationException re) {
                throw new RuntimeException(re);
            }
        }
    }

    /**
     * the index name, used for the database index name
     */
    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * specifies whether or not this index should include the unique constraint
     */
    public boolean getUnique() {
        return this.unique;
    }

    public void setUnique(boolean unique) {
        this.unique = unique;
    }

    /**
     * the main entity of this relation
     */
    public ModelEntity getMainEntity() {
        return this.mainEntity;
    }

    public void setMainEntity(ModelEntity mainEntity) {
        this.mainEntity = mainEntity;
    }

    public Iterator<String> getIndexFieldsIterator() {
        return this.fieldNames.iterator();
    }

    public int getIndexFieldsSize() {
        return this.fieldNames.size();
    }

    public String getIndexField(int index) {
        return this.fieldNames.get(index);
    }

    public void addIndexField(String fieldName) {
        this.fieldNames.add(fieldName);
    }

    public String removeIndexField(int index) {
        return this.fieldNames.remove(index);
    }


    public Optional<IndexAlternativeAction> getAlternativeIndexAction(DatabaseUtil dbUtil) throws SQLException, GenericEntityException {
        List<IndexAlternativeAction> runnableAlternativeActions = new LinkedList<>();

        for (IndexAlternativeAction alternativeAction : alternativeActions) {
            if (alternativeAction.shouldRun(mainEntity, this, dbUtil)) {
                runnableAlternativeActions.add(alternativeAction);
            }
        }

        switch (runnableAlternativeActions.size()) {
            case 0:
                return Optional.empty();
            case 1:
                return Optional.of(runnableAlternativeActions.get(0));
            default:
                throw new IllegalStateException("There can't be two runnable alternative actions for one index");
        }
    }

    public void addAlternativeAction(IndexAlternativeAction indexAlternativeAction) {
        alternativeActions.add(indexAlternativeAction);
    }
}
