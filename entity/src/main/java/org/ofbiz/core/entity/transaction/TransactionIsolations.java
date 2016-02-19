package org.ofbiz.core.entity.transaction;

import org.apache.commons.collections.BidiMap;
import org.apache.commons.collections.bidimap.DualHashBidiMap;

import java.sql.Connection;

/**
 * Converts from a text
 */
public class TransactionIsolations {
    /**
     * Supported JDBC isolation levels.
     */
    private static final BidiMap ISOLATION_LEVELS = new DualHashBidiMap() {
        {
            put("None", Connection.TRANSACTION_NONE);
            put("ReadUncommitted", Connection.TRANSACTION_READ_UNCOMMITTED);
            put("ReadCommitted", Connection.TRANSACTION_READ_COMMITTED);
            put("RepeatableRead", Connection.TRANSACTION_REPEATABLE_READ);
            put("Serializable", Connection.TRANSACTION_SERIALIZABLE);
        }
    };

    /**
     * Returns an int that corresponds to the JDBC transaction isolation level for the given string.
     *
     * @param isolationLevel a String describing a transaction isolation level
     * @return an int describing a transaction isolation level
     * @throws IllegalArgumentException if the given string is not a known isolation level
     */
    public static int fromString(String isolationLevel) throws IllegalArgumentException {
        if (!ISOLATION_LEVELS.containsKey(isolationLevel)) {
            throw new IllegalArgumentException("Invalid transaction isolation: " + isolationLevel);
        }

        return (Integer) ISOLATION_LEVELS.get(isolationLevel);
    }

    /**
     * @param isolationLevel an int describing a transaction isolation level
     * @return a String representation of the given isolation level
     * @throws IllegalArgumentException if the given int does not correspond to a known isolation level
     */
    public static String asString(int isolationLevel) {
        if (!ISOLATION_LEVELS.containsValue(isolationLevel)) {
            throw new IllegalArgumentException("Invalid transaction isolation: " + isolationLevel);
        }

        return (String) ISOLATION_LEVELS.getKey(isolationLevel);
    }

    private TransactionIsolations() {
        // prevent instantiation
    }
}
