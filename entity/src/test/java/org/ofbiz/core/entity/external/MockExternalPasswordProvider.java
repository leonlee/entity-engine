package org.ofbiz.core.entity.external;

public class MockExternalPasswordProvider implements ExternalDataProvider {
    @Override
    public String getData() {
        return "PasswordFromExternalSource";
    }
}
