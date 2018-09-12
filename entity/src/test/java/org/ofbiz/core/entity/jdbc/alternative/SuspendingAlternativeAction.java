package org.ofbiz.core.entity.jdbc.alternative;

import org.ofbiz.core.entity.jdbc.DatabaseUtil;
import org.ofbiz.core.entity.model.ModelEntity;
import org.ofbiz.core.entity.model.ModelIndex;


public class SuspendingAlternativeAction implements IndexAlternativeAction {
    static public String ACTION_EXECUTED_DUMMY_ERROR = "Suspending alternative action executed";

    @Override
    public String run(ModelEntity modelEntity, ModelIndex modelIndex, DatabaseUtil dbUtil) {
        return ACTION_EXECUTED_DUMMY_ERROR;
    }

    @Override
    public boolean shouldRun(ModelEntity modelEntity, ModelIndex modelIndex, DatabaseUtil dbUtil) {
        return true;
    }
}
