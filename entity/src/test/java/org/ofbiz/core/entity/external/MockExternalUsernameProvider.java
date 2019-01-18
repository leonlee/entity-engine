package org.ofbiz.core.entity.external;

public class MockExternalUsernameProvider implements ExternalDataProvider{
    @Override
    public String getData() {
        return "UsernameFromExternalSource";
    }
}
