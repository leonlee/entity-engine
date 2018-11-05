package org.ofbiz.core.entity.jdbc.alternative;


import org.ofbiz.core.entity.GenericEntityException;
import org.ofbiz.core.entity.jdbc.DatabaseUtil;
import org.ofbiz.core.entity.model.ModelEntity;
import org.ofbiz.core.entity.model.ModelIndex;

import java.sql.SQLException;

/*
 * Implementing and using implementations of this interface may be dangerous.
 * Index may be not created if you misuse alternative index creation in effect severely impacting performance.
 * Please test your code carefully, check if you thought about all cases, that's all supported databases!
 */
public interface IndexAlternativeAction {
    /**
     * This method is used to perform alternative actions on index before it is created.
     * Mostly to create the index yourself depending on the database type.
     * You have to notice that alternative action can only happen if index is not created.
     * <p>
     * <p>
     * CONTRACT: Run MUST handle index creation AND persist its original name
     * OR ofBiz will attain to recreate it every time JIRA starts.
     *
     * HINT: see {@link DatabaseUtil#createDeclaredIndex(ModelEntity, ModelIndex)} to experience ofBiz index creation.
     *
     * @param modelEntity model of entity containing index
     * @param modelIndex  model of index we want to handle alternatively
     * @param dbUtil      database helper class to handle many scenarios
     * @return null on success or error string on failure.
     */
    String run(ModelEntity modelEntity, ModelIndex modelIndex, DatabaseUtil dbUtil) throws SQLException, GenericEntityException;


    /**
     * This method flags if alternative action should be invoked.
     * This method is only run if index was not created before.
     * <p>
     * <p>
     * CONTRACT: This function shouldn't have any side effects as it may get rerun.
     *
     * CONTRACT: Only one alternative action in a entity definition can return true for shouldRun
     * OR IllegalStateException will be thrown.
     *
     * @param modelEntity model of entity containing index
     * @param modelIndex  model of index we want to handle alternatively
     * @param dbUtil      database helper class to handle many scenarios
     * @return true if index creation will be handled and ofBiz should not attempt to create index by itself.
     */
    boolean shouldRun(ModelEntity modelEntity, ModelIndex modelIndex, DatabaseUtil dbUtil) throws SQLException, GenericEntityException;
}
