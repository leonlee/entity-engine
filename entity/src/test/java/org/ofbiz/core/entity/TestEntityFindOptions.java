package org.ofbiz.core.entity;

import org.junit.Test;

import static java.sql.ResultSet.CONCUR_READ_ONLY;
import static java.sql.ResultSet.CONCUR_UPDATABLE;
import static java.sql.ResultSet.TYPE_FORWARD_ONLY;
import static java.sql.ResultSet.TYPE_SCROLL_SENSITIVE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Unit test of EntityFindOptions.
 */
public class TestEntityFindOptions {

    @Test
    public void noArgConstructorShouldSetTheDocumentedDefaults() {
        // Invoke and check
        assertDefaultOptions(new EntityFindOptions());
    }

    @Test
    public void factoryMethodShouldSetTheDocumentedDefaults() {
        // Invoke and check
        assertDefaultOptions(EntityFindOptions.findOptions());
    }

    @Test
    public void buildingWithNonDefaultOptionsShouldApplyThoseOptions() {
        // Set up
        final EntityFindOptions options = EntityFindOptions.findOptions();
        final int fetchSize = 20;
        final int maxResults = 40;
        final int offset = 100;

        // Invoke
        options.distinct().fetchSize(fetchSize).maxResults(maxResults).scrollSensitive().updatable().setOffset(offset);
        options.setSpecifyTypeAndConcur(false);

        // Check
        assertTrue(options.getDistinct());
        assertEquals(fetchSize, options.getFetchSize());
        assertEquals(maxResults, options.getMaxResults());
        assertEquals(offset, options.getOffset());
        assertEquals(CONCUR_UPDATABLE, options.getResultSetConcurrency());
        assertEquals(TYPE_SCROLL_SENSITIVE, options.getResultSetType());
        assertFalse(options.getSpecifyTypeAndConcur());
    }

    private void assertDefaultOptions(final EntityFindOptions options) {
        assertFalse(options.getDistinct());
        assertEquals(-1, options.getFetchSize());
        assertEquals(-1, options.getMaxResults());
        assertEquals(0, options.getOffset());
        assertEquals(CONCUR_READ_ONLY, options.getResultSetConcurrency());
        assertEquals(TYPE_FORWARD_ONLY, options.getResultSetType());
        assertTrue(options.getSpecifyTypeAndConcur());
    }

    @Test
    public void shouldRespectWishToUseDriverDefaultsForConcurrencyAndResultSetType() {
        // Set up
        final EntityFindOptions options = EntityFindOptions.findOptions();
        assertTrue(options.getSpecifyTypeAndConcur());
        assertTrue(options.isCustomResultSetTypeAndConcurrency());

        // Invoke
        options.useDriverDefaultsForTypeAndConcurrency();

        // Check
        assertFalse(options.getSpecifyTypeAndConcur());
        assertFalse(options.isCustomResultSetTypeAndConcurrency());
    }
}
