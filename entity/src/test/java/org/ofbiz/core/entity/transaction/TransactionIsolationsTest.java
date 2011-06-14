package org.ofbiz.core.entity.transaction;

import junit.framework.TestCase;

import java.sql.Connection;

public class TransactionIsolationsTest extends TestCase
{
    public void testMappingFromStringToInt() throws Exception
    {
        assertEquals(Connection.TRANSACTION_NONE, TransactionIsolations.fromString("None"));
        assertEquals(Connection.TRANSACTION_READ_UNCOMMITTED, TransactionIsolations.fromString("ReadUncommitted"));
        assertEquals(Connection.TRANSACTION_READ_COMMITTED, TransactionIsolations.fromString("ReadCommitted"));
        assertEquals(Connection.TRANSACTION_REPEATABLE_READ, TransactionIsolations.fromString("RepeatableRead"));
        assertEquals(Connection.TRANSACTION_SERIALIZABLE, TransactionIsolations.fromString("Serializable"));
    }

    public void testMappingFromIntToString() throws Exception
    {
        assertEquals("None", TransactionIsolations.asString(Connection.TRANSACTION_NONE));
        assertEquals("ReadUncommitted", TransactionIsolations.asString(Connection.TRANSACTION_READ_UNCOMMITTED));
        assertEquals("ReadCommitted", TransactionIsolations.asString(Connection.TRANSACTION_READ_COMMITTED));
        assertEquals("RepeatableRead", TransactionIsolations.asString(Connection.TRANSACTION_REPEATABLE_READ));
        assertEquals("Serializable", TransactionIsolations.asString(Connection.TRANSACTION_SERIALIZABLE));
    }
}
