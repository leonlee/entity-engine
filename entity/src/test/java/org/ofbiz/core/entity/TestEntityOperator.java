package org.ofbiz.core.entity;

import org.junit.Test;

import static org.junit.Assert.assertFalse;

public class TestEntityOperator
{
    @Test
    public void instanceShouldNotEqualObjectOfDifferentType() {
        assertFalse(new EntityOperator(1, "a").equals("a string"));
    }
}
