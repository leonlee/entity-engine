package org.ofbiz.core.entity;

import org.ofbiz.core.entity.jdbc.dbtype.DatabaseType;
import org.ofbiz.core.entity.jdbc.dbtype.DatabaseTypeFactory;
import org.ofbiz.core.util.Debug;

import javax.transaction.InvalidTransactionException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.Optional;

/**
 * Sequence Ids Bank implementation
 *
 * Uses a collision detection approach to safely get unique sequenced ids in banks from the database
 *
 * @since 1.5.0
 */
class SequenceBank {

    public static final long DEFAULT_BANK_SIZE = 100;
    public static final long startSeqId = 10000;
    public static final int minWaitNanos = 500000;   // 1/2 ms
    public static final int maxWaitNanos = 1000000;  // 1 ms
    public static final int maxTries = 5;

    private static final String module = SequenceBank.class.getName();

    private volatile long curSeqId;
    private volatile long maxSeqId;
    private volatile DatabaseType databaseType;

    private final String seqName;
    private final boolean clusterMode;
    private final SequenceTableParams dbArguments;

    private final long bankSize;

    public SequenceBank(String seqName, boolean clusterMode, SequenceTableParams dbArguments) {
        this(seqName, clusterMode, dbArguments, DEFAULT_BANK_SIZE);
    }

    public SequenceBank(String seqName, boolean clusterMode, SequenceTableParams dbArguments, long bankSize) {
        this.seqName = seqName;
        this.dbArguments = dbArguments;
        this.clusterMode = clusterMode;
        this.bankSize = bankSize;
        curSeqId = 0;
        maxSeqId = 0;
        fillBank();
    }

    public synchronized Long getNextSeqId() {
        if (curSeqId < maxSeqId) {
            Long retSeqId = curSeqId;

            curSeqId++;
            return retSeqId;
        } else {
            fillBank();
            if (curSeqId < maxSeqId) {
                Long retSeqId = curSeqId;

                curSeqId++;
                return retSeqId;
            } else {
                Debug.logError("[SequenceBank.getNextSeqId] Fill bank failed, returning null", module);
                return null;
            }
        }
    }

    protected synchronized void fillBank() {
        // no need to get a new bank, SeqIds available
        if (curSeqId < maxSeqId) return;

        long val1 = 0;
        long val2 = 0;

        // NOTE: the fancy ethernet type stuff is for the case where transactions not available
        boolean manualTX = true;
        Transaction suspendedTransaction = null;
        TransactionManager transactionManager = null;

        try {
            if (TransactionUtil.getStatus() == TransactionUtil.STATUS_ACTIVE) {
                manualTX = false;
                try {
                    //if we can suspend the transaction, we'll try to do this in a local manual transaction
                    transactionManager = TransactionFactory.getTransactionManager();
                    if (transactionManager != null) {
                        suspendedTransaction = transactionManager.suspend();
                        manualTX = true;
                    }
                } catch (SystemException e) {
                    Debug.logError(e, "System Error suspending transaction in sequence util");
                }
            }
        } catch (GenericTransactionException e) {
            // nevermind, don't worry about it, but print the exc anyway
            Debug.logWarning("[SequenceBank.fillBank] Exception was thrown trying to check " +
                    "transaction status: " + e.toString(), module);
        }

        Connection connection = null;

        try {
            connection = ConnectionFactory.getConnection(dbArguments.getHelperName());
        } catch (SQLException | GenericEntityException sqle) {
            Debug.logWarning("[SequenceBank.fillBank]: Unable to establish a connection with the database... Error was:", module);
            Debug.logWarning(sqle.getMessage(), module);
        }

        PreparedStatement selectPstmt = null;
        PreparedStatement insertPstmt = null;
        PreparedStatement updatePstmt = null;
        try {
            try {
                connection.setAutoCommit(false);
            } catch (SQLException sqle) {
                manualTX = false;
            }

            int numTries = 0;

            while (val1 + bankSize != val2) {
                ResultSet rs1 = null;
                ResultSet rs2 = null;
                try {
                    if (Debug.verboseOn())
                        Debug.logVerbose("[SequenceBank.fillBank] Trying to get a bank of sequenced ids for " +
                                this.seqName + "; start of loop val1=" + val1 + ", val2=" + val2 + ", bankSize=" + bankSize, module);

                    // try 1: SELECT the next id
                    if (selectPstmt == null) {
                        final String selectSyntax =
                                getDatabaseTypeSingleton(connection)
                                        .map(databaseType -> databaseType.getSimpleSelectSqlSyntax(clusterMode))
                                        .orElseGet(() -> DatabaseType.STANDARD_SELECT_SYNTAX);

                        selectPstmt = connection.prepareStatement(MessageFormat.format(selectSyntax, dbArguments.getIdColName(), dbArguments.getTableName(), dbArguments.getNameColName() + "=?"));
                    }

                    selectPstmt.setString(1, this.seqName);
                    selectPstmt.execute();

                    rs1 = selectPstmt.getResultSet();
                    if (rs1.next()) {
                        val1 = rs1.getLong(dbArguments.getIdColName());
                    } else {
                        Debug.logVerbose("[SequenceBank.fillBank] first select failed: trying to add " +
                                "row, result set was empty for sequence: " + seqName, module);

                        // INSERT the row if it doesn't exist
                        if (insertPstmt == null) {
                            insertPstmt = connection.prepareStatement("INSERT INTO " + dbArguments.getTableName() + " (" + dbArguments.getNameColName() + ", " + dbArguments.getIdColName() + ") VALUES (?,?)");
                        }
                        insertPstmt.setString(1, this.seqName);
                        insertPstmt.setLong(2, startSeqId);
                        insertPstmt.execute();

                        if (insertPstmt.getUpdateCount() <= 0) return;
                        continue;
                    }

                    // UPDATE the next id by adding bankSize
                    if (updatePstmt == null) {
                        updatePstmt = connection.prepareStatement("UPDATE " + dbArguments.getTableName() + " SET " + dbArguments.getIdColName() + "=" + dbArguments.getIdColName() + "+" + bankSize + " WHERE " + dbArguments.getNameColName() + "=?");
                    }
                    updatePstmt.setString(1, this.seqName);
                    updatePstmt.execute();

                    if (updatePstmt.getUpdateCount() <= 0) {
                        Debug.logWarning("[SequenceBank.fillBank] update failed, no rows changes for seqName: " + seqName, module);
                        return;
                    }

                    if (manualTX) {
                        connection.commit();
                    }

                    // try 2: SELECT the next id
                    selectPstmt.setString(1, this.seqName);
                    selectPstmt.execute();
                    rs2 = selectPstmt.getResultSet();

                    if (rs2.next()) {
                        val2 = rs2.getLong(dbArguments.getIdColName());
                    } else {
                        Debug.logWarning("[SequenceBank.fillBank] second select failed: aborting, result " +
                                "set was empty for sequence: " + seqName, module);
                        return;
                    }

                    // Commit the connection to keep WebSphere happy. See the above comment when transaction was started.
                    if (manualTX) {
                        connection.commit();
                    }

                    if (val1 + bankSize != val2) {
                        if (numTries >= maxTries) {
                            Debug.logError("[SequenceBank.fillBank] maxTries (" + maxTries + ") reached, giving up.", module);
                            return;
                        }
                        // collision happened, wait a bounded random amount of time then continue
                        int waitTime = (new Double(Math.random() * (maxWaitNanos - minWaitNanos))).intValue() + minWaitNanos;

                        try {
                            this.wait(0, waitTime);
                        } catch (Exception e) {
                            Debug.logWarning(e, "Error waiting in sequence util");
                        }
                    }

                    numTries++;
                } finally {
                    closeQuietly(rs2);
                    closeQuietly(rs1);
                }
            }

            curSeqId = val1;
            maxSeqId = val2;
            if (Debug.verboseOn())
                Debug.logVerbose("[SequenceBank.fillBank] Successfully got a bank of sequenced ids for " +
                        this.seqName + "; curSeqId=" + curSeqId + ", maxSeqId=" + maxSeqId + ", bankSize=" + bankSize, module);
        } catch (SQLException sqle) {
            Debug.logWarning(sqle, "[SequenceBank.fillBank] SQL Exception", module);
            return;
        } finally {
            // close all prepared statements and the connection
            closeQuietly(updatePstmt);
            closeQuietly(insertPstmt);
            closeQuietly(selectPstmt);
            closeQuietly(connection);
        }

        if (suspendedTransaction != null) {
            try {
                if (transactionManager == null) {
                    transactionManager = TransactionFactory.getTransactionManager();
                }
                if (transactionManager != null) {
                    transactionManager.resume(suspendedTransaction);
                }
            } catch (InvalidTransactionException e) {
                Debug.logError(e, "InvalidTransaction Error resuming suspended transaction in sequence util");
            } catch (IllegalStateException e) {
                Debug.logError(e, "IllegalState Error resuming suspended transaction in sequence util");
            } catch (SystemException e) {
                Debug.logError(e, "System Error resuming suspended transaction in sequence util");
            }
        }
    }

    private void closeQuietly(Connection connection) {
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (Exception sqle) {
            Debug.logWarning(sqle, "Error closing connection in sequence util");
        }
    }

    private void closeQuietly(PreparedStatement stmt) {
        try {
            if (stmt != null) {
                stmt.close();
            }
        } catch (Exception sqle) {
            Debug.logWarning(sqle, "Error closing statement in sequence util");
        }
    }

    private void closeQuietly(ResultSet rs) {
        try {
            if (rs != null) {
                rs.close();
            }
        } catch (Exception sqle) {
            Debug.logWarning(sqle, "Error closing result set in sequence util");
        }
    }

    private Optional<DatabaseType> getDatabaseTypeSingleton(final Connection connection) {
        // just load the database type once, we are already executed only within a synced block
        if (databaseType == null) {
            databaseType = DatabaseTypeFactory.getTypeForConnection(connection);
        }
        return Optional.ofNullable(databaseType);
    }

    /**
     * Holder for sequence table parameters
     */
    public static class SequenceTableParams {

        private String helperName;
        private String tableName;
        private String nameColName;
        private String idColName;

        public SequenceTableParams withHelperName(String helperName) {
            this.helperName = helperName;
            return this;
        }

        public SequenceTableParams withTableName(String tableName) {
            this.tableName = tableName;
            return this;
        }

        public SequenceTableParams withNameColName(String nameColName) {
            this.nameColName = nameColName;
            return this;
        }

        public SequenceTableParams withIdColName(String idColName) {
            this.idColName = idColName;
            return this;
        }

        public String getHelperName() {
            return helperName;
        }

        public String getTableName() {
            return tableName;
        }

        public String getNameColName() {
            return nameColName;
        }

        public String getIdColName() {
            return idColName;
        }
    }
}
