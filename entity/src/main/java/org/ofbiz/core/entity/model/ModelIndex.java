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

import org.ofbiz.core.util.UtilXml;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

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
    protected List<String> fieldNames = new ArrayList<String>();

    /**
     * Pattern to exclude certain servers from index creation
     */
    protected Optional<Pattern> serverExclude = Optional.empty();

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

        this.serverExclude =
                Optional.of(indexElement.getAttribute("serverExclude"))
                        .filter(unused -> indexElement.hasAttribute("serverExclude"))
                        .map(Pattern::compile);

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

    public void setServerExclude(Pattern serverExclude) {
        this.serverExclude = Optional.ofNullable(serverExclude);
    }

    public Optional<Pattern> getServerExclude() {
        return serverExclude;
    }

    /**
     * checks if generation of this index should be skipped.
     *
     * @param generatedDBVersion {@link org.ofbiz.core.entity.jdbc.DatabaseUtil#generateDBVersion(Connection)}
     */
    public boolean isServerExcluded(String generatedDBVersion) {
        return serverExclude.map(x -> x.matcher(generatedDBVersion).matches()).orElse(false);
    }
}
