package org.ofbiz.core.entity;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.ofbiz.core.entity.model.ModelField;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests the new Count functionality
 *
 * @since v1.0.35
 */
public class TestCount {
    @Rule
    public ExpectedException exception = ExpectedException.none();

    private final String countDistinctField = "SELECT COUNT(DISTINCT active) FROM cwd_user";
    private final String countDistinctFieldWithCondition = "SELECT COUNT(DISTINCT active) FROM cwd_user WHERE user_name = 'fred'";
    private final String countDistinctFieldIgnoredWithNoField = "SELECT COUNT(*) FROM cwd_user";
    private final String countDistinctFieldIgnoredWithConditionAndNoField = "SELECT COUNT(*) FROM cwd_user WHERE user_name = 'fred'";

    private final String countField = "SELECT COUNT(active) FROM cwd_user";
    private final String countFieldWithCondition = "SELECT COUNT(active) FROM cwd_user WHERE user_name = 'fred'";

    private final String countAll = "SELECT COUNT(*) FROM cwd_user";
    private final String countAllWithCondition = "SELECT COUNT(*) FROM cwd_user WHERE user_name = 'fred'";

    
    private final String tableName = "cwd_user";
    private final String column = "active";
    private final String whereClause = "user_name = 'fred'";
    private final CountHelper helper = new CountHelper();


    @Test
    public void testDistinct() {
        Assert.assertEquals("Distinct with field produces correct result", countDistinctField, helper.buildCountSelectStatement(tableName, column, null, true));
        Assert.assertEquals("Distinct with no field produces correct result", countDistinctFieldIgnoredWithNoField, helper.buildCountSelectStatement(tableName, null, null, true));
        Assert.assertEquals("Distinct with where clause produces correct result", countDistinctFieldWithCondition, helper.buildCountSelectStatement(tableName, column, whereClause, true));
        Assert.assertEquals("Distinct with no field and a where clause produces correct result", countDistinctFieldIgnoredWithConditionAndNoField, helper.buildCountSelectStatement(tableName, null, whereClause, true));
    }

    @Test
    public void testCount() {
        Assert.assertEquals("Count with field produces correct result", countField, helper.buildCountSelectStatement(tableName, column, null, false));
        Assert.assertEquals("Count with no field produces correct result", countAll, helper.buildCountSelectStatement(tableName, null, null, false));
        Assert.assertEquals("Count with where clause produces correct result", countFieldWithCondition, helper.buildCountSelectStatement(tableName, column, whereClause, false));
        Assert.assertEquals("Count with no field and a where clause produces correct result", countAllWithCondition, helper.buildCountSelectStatement(tableName, null, whereClause, false));
    }
}
