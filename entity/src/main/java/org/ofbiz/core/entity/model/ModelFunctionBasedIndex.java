package org.ofbiz.core.entity.model;

import org.ofbiz.core.entity.jdbc.dbtype.DatabaseType;
import org.ofbiz.core.util.UtilXml;
import org.w3c.dom.Element;

import javax.annotation.Nullable;
import java.lang.reflect.Constructor;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.ofbiz.core.util.UtilXml.childElementList;
import static org.ofbiz.core.util.UtilXml.firstChildElement;

/**
 * Generic Entity - Relation model function-based-index class
 */

public class ModelFunctionBasedIndex {

    /**
     * reference to the entity this index refers to
     */
    private final ModelEntity mainEntity;

    /**
     * the index name, used for the database index name
     */
    private final String name;

    /**
     * the function to base the index on
     */
    private String function;

    /**
     * specifies whether or not this index should include the unique constraint
     */
    private final boolean unique;

    private final FunctionDefinitionBuilder builder;
    
    /**
     * the function return type, used for databases that do not support function based indexes
     */
    private String type;


    public ModelFunctionBasedIndex(ModelEntity mainEntity, String name, boolean unique, FunctionDefinitionBuilder builder) {
        this.mainEntity = mainEntity;
        this.name = name;
        this.unique = unique;
        this.builder = builder;
        this.type = builder.getType();
    }

    /**
     * XML Constructor
     */
    public ModelFunctionBasedIndex(ModelEntity mainEntity, Element indexElement) {
        this.mainEntity = mainEntity;

        this.name = UtilXml.checkEmpty(indexElement.getAttribute("name"));
        this.unique = "true".equals(UtilXml.checkEmpty(indexElement.getAttribute("unique")));
        this.function = UtilXml.checkEmpty(indexElement.getAttribute("function"));
        Element builderElement = firstChildElement(indexElement, "builder");
        String builderClass = builderElement.getAttribute("class");
        Element functionDefinitionElement = firstChildElement(builderElement, "function-definition");
        this.type = functionDefinitionElement.getAttribute("type");
        String virtualColumn = functionDefinitionElement.getAttribute("virtual-column");
        String argList = functionDefinitionElement.getAttribute("arg-list");
        List<String> columns = childElementList(functionDefinitionElement, "column")
                .stream()
                .map(new Function<Element, String>() {
                    @Override
                    public String apply(Element element) {
                        return element.getAttribute("name");
                    }
                })
                .collect(Collectors.toList());
        this.builder = getFunctionDefinitionBuilder(builderClass, virtualColumn, type, columns, argList);
    }

    private FunctionDefinitionBuilder getFunctionDefinitionBuilder(String clazz, String virtualColumn, String type, List<String> columns, @Nullable String argsList) {
        FunctionDefinitionBuilder builder;
        try {
            Constructor cons = Class.forName(clazz).getConstructor(String.class, String.class, List.class, String.class);
            builder = (FunctionDefinitionBuilder)cons.newInstance(virtualColumn, type, columns, argsList);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
        return builder;
    }

    /**
     * the index name, used for the database index name
     */
    public String getName() {
        return this.name;
    }

    public String getFunction(DatabaseType dbType) {
        return builder.getFunctionDefinition(dbType);
    }

    /**
     * specifies whether or not this index should include the unique constraint
     */
    public boolean getUnique() {
        return this.unique;
    }

    public String getVirtualColumn(DatabaseType dbType) {
        return builder.getVirtualColumn(dbType);
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

    public ModelField getVirtualColumnModelField(DatabaseType dbType) {
        ModelField modelField = null;
        String virtualColumn = builder.getVirtualColumn(dbType);
        if (virtualColumn != null) {
            modelField = new ModelField();
            modelField.setName(virtualColumn);
            modelField.setType(type);
            modelField.setColName(ModelUtil.javaNameToDbName(virtualColumn));
        }
        return modelField;
    }

    public boolean supportsFunctionBasedIndices(DatabaseType databaseType) {
        return builder.supportsFunctionBasedIndices(databaseType);
    }
}
