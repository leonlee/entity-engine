package org.ofbiz.core.entity;

import org.ofbiz.core.entity.model.ModelEntity;
import org.ofbiz.core.util.Debug;

import javax.transaction.InvalidTransactionException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

/**
 * SequenceUtil that does not cache the sequence ids but gets them as needed from the database.
 */
public class VSequenceUtil extends SequenceUtil {

    public static final String module = VSequenceUtil.class.getName();
    private static final long START_SEQ_ID = 10000L;
    private static final ThreadLocal<Boolean> manualTx = new ThreadLocal<>();

    public VSequenceUtil(String helperName, ModelEntity seqEntity, String nameFieldName, String idFieldName) {
        super(helperName, seqEntity, nameFieldName, idFieldName);
    }

    public synchronized Long getNextSeqId(String seqName) {
        return getNextSeqIds(seqName, 1).get(0);
    }

    public synchronized List<Long> getNextSeqIds(String seqName, int quantity) {
        Connection connection = null;
        manualTx.set(true);
        Transaction suspendedTransaction = suspendActiveTransaction();
        long nextSeq = START_SEQ_ID;

        try {
            connection = ConnectionFactory.getConnection(helperName);
        } catch (SQLException | GenericEntityException sqle) {
            Debug.logWarning("[VSequenceUtil.getNextSeqId]: Unable to establish a connection with the database... Error was:", module);
            Debug.logWarning(sqle.getMessage(), module);
        }

        PreparedStatement selectPstmt = null;
        ResultSet rs = null;

        try {
            try {
                connection.setAutoCommit(false);
            } catch (SQLException sqle) {
                manualTx.set(false);
            }
            selectPstmt = connection.prepareStatement("SELECT * FROM " + tableName + " WHERE " + nameColName + "=? FOR UPDATE",
                    ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
            selectPstmt.setString(1, seqName);
            rs = selectPstmt.executeQuery();
            if (rs.next()) {
                nextSeq = rs.getLong(idColName);
                rs.updateLong(idColName, nextSeq + quantity);
                rs.updateRow();
            } else {
                rs.moveToInsertRow();
                rs.updateString(nameColName, seqName);
                rs.updateLong(idColName, nextSeq + quantity);
                rs.insertRow();
            }
            // Updateable result sets will always result in database locks, so we don't
            // need to check if another thread has created a sequence.... the method is also
            // synchronized to protect against the case where no row exists yet
            if (manualTx.get()) {
                connection.commit();
            }
        } catch (SQLException e) {
            Debug.logWarning(e, "[VSequenceUtil.getNextSeqId] SQL Exception", module);
            return null;
        } finally {
            // close all prepared statements and the connection
            closeQuietly(rs);
            closeQuietly(selectPstmt);
            closeQuietly(connection);
            manualTx.remove();
        }
        resumeTransaction(suspendedTransaction);
        return LongStream.range(nextSeq, quantity).boxed().collect(Collectors.toList());
    }

    private Transaction suspendActiveTransaction() {
        Transaction suspendedTransaction = null;
        try {
            if (TransactionUtil.getStatus() == TransactionUtil.STATUS_ACTIVE) {
                manualTx.set(false);
                try {
                    //if we can suspend the transaction, we'll try to do this in a local manual transaction
                    TransactionManager transactionManager = TransactionFactory.getTransactionManager();
                    if (transactionManager != null) {
                        suspendedTransaction = transactionManager.suspend();
                        manualTx.set(true);
                    }
                } catch (SystemException e) {
                    Debug.logError(e, "System Error suspending transaction in sequence util");
                }
            }
        } catch (GenericTransactionException e) {
            // nevermind, don't worry about it, but print the exc anyway
            Debug.logWarning("[VSequenceUtil.suspendActiveTransaction] Exception was thrown trying to check " +
                    "transaction status: " + e.toString(), module);
        }
        return suspendedTransaction;
    }

    private void resumeTransaction(Transaction suspendedTransaction) {
        if (suspendedTransaction != null) {
            try {
                TransactionManager transactionManager = TransactionFactory.getTransactionManager();
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

}
