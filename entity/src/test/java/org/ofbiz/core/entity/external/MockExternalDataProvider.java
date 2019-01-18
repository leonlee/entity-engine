package org.ofbiz.core.entity.external;

public class MockExternalDataProvider implements ExternalDataProvider {

    @Override
    public String getData() {
        return "mock";
    }
}
