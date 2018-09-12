package org.ofbiz.core.entity.jdbc.alternative;

import org.ofbiz.core.entity.jdbc.DatabaseUtil;
import org.ofbiz.core.entity.model.ModelEntity;
import org.ofbiz.core.entity.model.ModelIndex;

public class NoOpIndexAlternativeAction implements IndexAlternativeAction {
    @Override
    public String run(ModelEntity modelEntity, ModelIndex modelIndex, DatabaseUtil dbUtil) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public boolean shouldRun(ModelEntity modelEntity, ModelIndex modelIndex, DatabaseUtil dbUtil) {
        return false;
    }
}
