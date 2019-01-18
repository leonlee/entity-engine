package org.ofbiz.core.entity.external;

import org.ofbiz.core.util.Debug;
import org.w3c.dom.Element;

import java.util.Optional;

public class ExternalDataHandler {

    private static final String KEY_PREFIX = "external-";

    enum ExternalData {
        PASSWORD(KEY_PREFIX + "jdbc-password"), USERNAME(KEY_PREFIX + "jdbc-username");
        final String key;

        ExternalData(String key) {
            this.key = key;
        }
    }

    Optional<String> getExternalData(Element jdbcDatasourceElement, ExternalData externalData) {
        if (jdbcDatasourceElement == null || externalData == null) {
            return Optional.empty();
        }
        final String className = jdbcDatasourceElement.getAttribute(externalData.key);
        Debug.logInfo("Loading data: " + externalData.key + " from class: " + className);
        try {
            Class<?> externalDataSupplierClass = Class.forName(className);
            ExternalDataProvider externalDataProviderInstance = (ExternalDataProvider) externalDataSupplierClass.newInstance();
            return Optional.of(externalDataProviderInstance.getData());

        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            Debug.logWarning(e, "Couldn't load data: " + externalData.key + " from class: " + className);
        }
        return Optional.empty();
    }
}
