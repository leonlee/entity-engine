package org.ofbiz.core.entity.external;

import org.ofbiz.core.util.Debug;
import org.w3c.dom.Element;

import java.util.Optional;

public class ExternalDataHandler {

    private static final String KEY_PREFIX = "external-";

    public enum ExternalData {
        PASSWORD(KEY_PREFIX + "password"), USERNAME(KEY_PREFIX + "username");
        final String key;

        public String getKey() {
            return key;
        }

        ExternalData(String key) {
            this.key = key;
        }
    }

    public Optional<String> getExternalData(Element jdbcDatasourceElement, ExternalData externalData) {
        if (jdbcDatasourceElement == null || externalData == null) {
            return Optional.empty();
        }
        final String className = jdbcDatasourceElement.getAttribute(externalData.key);
        return getExternalData(className, externalData.key);
    }

    public Optional<String> getExternalData(String className, String key) {
        if (className == null || key == null) {
            return Optional.empty();
        }
        Debug.logInfo("Loading data: " + key + " from class: " + className);
        try {
            Class<?> externalDataSupplierClass = Class.forName(className);
            ExternalDataProvider externalDataProviderInstance = (ExternalDataProvider) externalDataSupplierClass.newInstance();
            return Optional.of(externalDataProviderInstance.getData());

        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            Debug.logWarning(e, "Couldn't load data: " + key + " from class: " + className);
        }
        return Optional.empty();
    }
}
