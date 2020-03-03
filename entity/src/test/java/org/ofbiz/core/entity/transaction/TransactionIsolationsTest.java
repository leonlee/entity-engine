package org.ofbiz.core.entity.transaction;

import org.junit.Test;

import static java.sql.Connection.TRANSACTION_NONE;
import static java.sql.Connection.TRANSACTION_READ_COMMITTED;
import static java.sql.Connection.TRANSACTION_READ_UNCOMMITTED;
import static java.sql.Connection.TRANSACTION_REPEATABLE_READ;
import static java.sql.Connection.TRANSACTION_SERIALIZABLE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.ofbiz.core.entity.transaction.TransactionIsolations.*;

public class TransactionIsolationsTest {
    @Test
    public void testMappingFromStringToInt() {
        assertEquals(TRANSACTION_NONE, fromString("None"));
        assertEquals(TRANSACTION_READ_UNCOMMITTED, fromString("ReadUncommitted"));
        assertEquals(TRANSACTION_READ_COMMITTED, fromString("ReadCommitted"));
        assertEquals(TRANSACTION_REPEATABLE_READ, fromString("RepeatableRead"));
        assertEquals(TRANSACTION_SERIALIZABLE, fromString("Serializable"));
    }

    @Test
    public void testMappingFromIntToString() {
        assertEquals("None", asString(TRANSACTION_NONE));
        assertEquals("ReadUncommitted", asString(TRANSACTION_READ_UNCOMMITTED));
        assertEquals("ReadCommitted", asString(TRANSACTION_READ_COMMITTED));
        assertEquals("RepeatableRead", asString(TRANSACTION_REPEATABLE_READ));
        assertEquals("Serializable", asString(TRANSACTION_SERIALIZABLE));
    }

    @Test
    public void willMapEntityEngineTransactionIsolationLevel() {
        assertEquals("TRANSACTION_NONE", mapTransactionIsolation("None"));
        assertEquals("TRANSACTION_READ_UNCOMMITTED", mapTransactionIsolation("ReadUncommitted"));
        assertEquals("TRANSACTION_READ_COMMITTED", mapTransactionIsolation("ReadCommitted"));
        assertEquals("TRANSACTION_REPEATABLE_READ", mapTransactionIsolation("RepeatableRead"));
        assertEquals("TRANSACTION_SERIALIZABLE", mapTransactionIsolation("Serializable"));
        try {
            assertEquals(null, mapTransactionIsolation("Banana"));
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Invalid transaction isolation"));
        }

    }
}
