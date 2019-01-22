package org.ofbiz.core.entity.external;

public class MockExternalPasswordProviderWithAnException implements ExternalDataProvider {
    @Override
    public String getData() {
        throw new RuntimeException("This is exception from external password provider");
    }
}
