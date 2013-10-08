package org.ofbiz.core.entity;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Tests the new Count functionality
 *
 * @since v1.0.35
 */
public class TestCount {

    private static final String COUNT_DISTINCT_FIELD = "SELECT COUNT(DISTINCT active) FROM cwd_user";
    private static final String COUNT_DISTINCT_FIELD_WITH_CONDITION = "SELECT COUNT(DISTINCT active) FROM cwd_user WHERE user_name = 'fred'";
    private static final String COUNT_DISTINCT_FIELD_IGNORED_WITH_NO_FIELD = "SELECT COUNT(*) FROM cwd_user";
    private static final String COUNT_DISTINCT_FIELD_IGNORED_WITH_CONDITION_AND_NO_FIELD = "SELECT COUNT(*) FROM cwd_user WHERE user_name = 'fred'";

    private static final String COUNT_FIELD = "SELECT COUNT(active) FROM cwd_user";
    private static final String COUNT_FIELD_WITH_CONDITION = "SELECT COUNT(active) FROM cwd_user WHERE user_name = 'fred'";

    private static final String COUNT_ALL = "SELECT COUNT(*) FROM cwd_user";
    private static final String COUNT_ALL_WITH_CONDITION = "SELECT COUNT(*) FROM cwd_user WHERE user_name = 'fred'";

    private static final String COLUMN = "active";
    private static final String TABLE_NAME = "cwd_user";
    private static final String WHERE_CLAUSE = "user_name = 'fred'";

    // Fixture
    private CountHelper helper;

    @Before
    public void setUp() {
        this.helper = new CountHelper();
    }

    @Test
    public void testDistinct() {
        assertEquals("Distinct with field produces correct result", COUNT_DISTINCT_FIELD,
                helper.buildCountSelectStatement(TABLE_NAME, COLUMN, null, true));
        assertEquals("Distinct with no field produces correct result", COUNT_DISTINCT_FIELD_IGNORED_WITH_NO_FIELD,
                helper.buildCountSelectStatement(TABLE_NAME, null, null, true));
        assertEquals("Distinct with where clause produces correct result", COUNT_DISTINCT_FIELD_WITH_CONDITION,
                helper.buildCountSelectStatement(TABLE_NAME, COLUMN, WHERE_CLAUSE, true));
        assertEquals("Distinct with no field and a where clause produces correct result", COUNT_DISTINCT_FIELD_IGNORED_WITH_CONDITION_AND_NO_FIELD,
                helper.buildCountSelectStatement(TABLE_NAME, null, WHERE_CLAUSE, true));
    }

    @Test
    public void testCount() {
        assertEquals("Count with field produces correct result", COUNT_FIELD,
                helper.buildCountSelectStatement(TABLE_NAME, COLUMN, null, false));
        assertEquals("Count with no field produces correct result", COUNT_ALL,
                helper.buildCountSelectStatement(TABLE_NAME, null, null, false));
        assertEquals("Count with where clause produces correct result", COUNT_FIELD_WITH_CONDITION,
                helper.buildCountSelectStatement(TABLE_NAME, COLUMN, WHERE_CLAUSE, false));
        assertEquals("Count with no field and a where clause produces correct result", COUNT_ALL_WITH_CONDITION,
                helper.buildCountSelectStatement(TABLE_NAME, null, WHERE_CLAUSE, false));
    }
}
