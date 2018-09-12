package org.ofbiz.core.entity.jdbc.alternative;


import org.ofbiz.core.entity.GenericEntityException;
import org.ofbiz.core.entity.jdbc.DatabaseUtil;
import org.ofbiz.core.entity.model.ModelEntity;
import org.ofbiz.core.entity.model.ModelIndex;

import java.sql.SQLException;

/*
 * Implementing and using implementations of this interface may be dangerous.
 * Please test your code carefully, check if you thought about all cases and all databases.
 *
 * REMEMBER: some alternative actions can shadow other actions in some cases.
 * Two same conditional alternative actions cannot be used together.
 */
public interface IndexAlternativeAction {
    /**
     * This method is used to perform alternative actions on index before it is created.
     * Mostly to create the index yourself depending on the database type.
     * You have to notice that alternative action can only happen if index is not created.
     * <p>
     * <p>
     * CONTRACT: Run MUST handle index creation AND persist its original name.
     *
     * @param modelEntity model of entity containing index
     * @param modelIndex  model of index we want to handle alternatively
     * @param dbUtil      database helper class to handle many scenarios
     * @return null or string if error occured - it is ofBiz way
     */
    String run(ModelEntity modelEntity, ModelIndex modelIndex, DatabaseUtil dbUtil) throws SQLException, GenericEntityException;


    /**
     * This method flags if alernative action should be invoked.
     * This method is only run if index was not created before.
     * <p>
     * <p>
     * CONTRACT: This function shouldn't have any side effects
     *
     * @param modelEntity model of entity containing index
     * @param modelIndex  model of index we want to handle alternatively
     * @param dbUtil      database helper class to handle many scenarios
     * @return true if index creation will be handled and ofbiz should not attempt
     * to create index by itself and stop ConditionalAction chain.
     */
    boolean shouldRun(ModelEntity modelEntity, ModelIndex modelIndex, DatabaseUtil dbUtil) throws SQLException, GenericEntityException;
}
