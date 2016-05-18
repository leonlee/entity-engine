package org.ofbiz.core.entity.model;

import org.ofbiz.core.util.UtilXml;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Generic Entity - Relation model function-based-index class
 */

public class ModelFunctionBasedIndex {

    /**
     * reference to the entity this index refers to
     */
    protected ModelEntity mainEntity;

    /**
     * the index name, used for the database index name
     */
    protected String name;

    /**
     * the function to base the index on
     */
    protected String function;

    /**
     * specifies whether or not this index should include the unique constraint
     */
    protected boolean unique;

    /**
     * the virtual column name, used for databases that do not support function based indexes
     */
    protected String virtualColumn;


    /**
     * the virtual column type, used for databases that do not support function based indexes
     */
    protected String type;


    /**
     * Default Constructor
     */
    public ModelFunctionBasedIndex() {
        name = "";
        unique = false;
        function = "";
    }

    public ModelFunctionBasedIndex(ModelEntity mainEntity, String name, String function, boolean unique, String virtualColumn, String type) {
        this.mainEntity = mainEntity;
        this.name = name;
        this.function = function;
        this.unique = unique;
        this.virtualColumn = virtualColumn;
        this.type = type;
    }

    /**
     * XML Constructor
     */
    public ModelFunctionBasedIndex(ModelEntity mainEntity, Element indexElement) {
        this.mainEntity = mainEntity;

        this.name = UtilXml.checkEmpty(indexElement.getAttribute("name"));
        this.unique = "true".equals(UtilXml.checkEmpty(indexElement.getAttribute("unique")));
        this.function = UtilXml.checkEmpty(indexElement.getAttribute("function"));

        NodeList virtualColumnsList = indexElement.getElementsByTagName("virtual-column");
        for (int i = 0; i < virtualColumnsList.getLength(); i++) {
            Element indexFieldElement = (Element) virtualColumnsList.item(i);

            if (indexFieldElement.getParentNode() == indexElement) {
                this.virtualColumn = indexFieldElement.getAttribute("name");
                this.type = indexFieldElement.getAttribute("type");
            }
        }
    }

    /**
     * the index name, used for the database index name
     */
    public String getName() {
        return this.name;
    }

    public String getFunction() {
        return function;
    }

    /**
     * specifies whether or not this index should include the unique constraint
     */
    public boolean getUnique() {
        return this.unique;
    }

    public String getVirtualColumn() {
        return virtualColumn;
    }

    public String getType() {
        return type;
    }

    /**
     * the main entity of this relation
     */
    public ModelEntity getMainEntity() {
        return this.mainEntity;
    }

}
