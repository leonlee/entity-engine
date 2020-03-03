package org.ofbiz.core.entity.transaction;

import org.apache.commons.collections.BidiMap;
import org.apache.commons.collections.bidimap.DualHashBidiMap;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.util.Locale;

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
     * Supported JDBC isolation levels as full text.
     */
    private static final BidiMap ISOLATION_LEVELS_FULL_TEXT = new DualHashBidiMap() {
        {
            put("TRANSACTION_NONE", Connection.TRANSACTION_NONE);
            put("TRANSACTION_READ_UNCOMMITTED", Connection.TRANSACTION_READ_UNCOMMITTED);
            put("TRANSACTION_READ_COMMITTED", Connection.TRANSACTION_READ_COMMITTED);
            put("TRANSACTION_REPEATABLE_READ", Connection.TRANSACTION_REPEATABLE_READ);
            put("TRANSACTION_SERIALIZABLE", Connection.TRANSACTION_SERIALIZABLE);
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
        if (ISOLATION_LEVELS.containsKey(isolationLevel)) {
            return (Integer) ISOLATION_LEVELS.get(isolationLevel);
        } else if (ISOLATION_LEVELS_FULL_TEXT.containsKey(isolationLevel)) {
            return (Integer) ISOLATION_LEVELS_FULL_TEXT.get(isolationLevel);
        }

        throw new IllegalArgumentException("Invalid transaction isolation: " + isolationLevel);
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

    /**
     * Map a entity engine transaction isolation level to a SQL connection version.
     *
     * @param transactionIsolationName the name of the transaction isolation level
     * @return the int value of the isolation level or -1
     */
    public static String mapTransactionIsolation(final String transactionIsolationName) {
        String isolation = null;
        if (transactionIsolationName != null) {
            isolation = (String) ISOLATION_LEVELS_FULL_TEXT.getKey(fromString(transactionIsolationName));
        }
        return isolation;
    }

    private TransactionIsolations() {
        // prevent instantiation
    }
}
