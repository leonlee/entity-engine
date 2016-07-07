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

/**
 * SequenceUtil that does not cache the sequence ids but gets them as needed from the database.
 *
 */
public class VSequenceUtil extends SequenceUtil {

    public static final String module = VSequenceUtil.class.getName();
    private static final long START_SEQ_ID = 10000L;

    public VSequenceUtil(String helperName, ModelEntity seqEntity, String nameFieldName, String idFieldName) {
        super(helperName, seqEntity, nameFieldName, idFieldName);
    }

    public synchronized Long getNextSeqId(String seqName) {
        boolean manualTX = true;
        Transaction suspendedTransaction = null;
        TransactionManager transactionManager = null;
        long nextSeq = START_SEQ_ID;
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
            Debug.logWarning("[VSequenceUtil.getNextSeqId] Exception was thrown trying to check " +
                    "transaction status: " + e.toString(), module);
        }
        Connection connection = null;

        try {
            connection = ConnectionFactory.getConnection(helperName);
        } catch (SQLException sqle) {
            Debug.logWarning("[VSequenceUtil.getNextSeqId]: Unable to establish a connection with the database... Error was:", module);
            Debug.logWarning(sqle.getMessage(), module);
        } catch (GenericEntityException e) {
            Debug.logWarning("[VSequenceUtil.getNextSeqId]: Unable to establish a connection with the database... Error was:", module);
            Debug.logWarning(e.getMessage(), module);
        }

        PreparedStatement selectPstmt = null;
        ResultSet rs = null;

        try {
            try {
                connection.setAutoCommit(false);
            } catch (SQLException sqle) {
                manualTX = false;
            }
            selectPstmt = connection.prepareStatement("SELECT * FROM " + tableName + " WHERE " + nameColName + "=? FOR UPDATE",
                    ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
            selectPstmt.setString(1, seqName);
            rs = selectPstmt.executeQuery();
            if (rs.next()) {
                nextSeq = rs.getLong(idColName) + 1;
                rs.updateLong(idColName, nextSeq);
                rs.updateRow();
            } else {
                rs.moveToInsertRow();
                rs.updateString(nameColName, seqName);
                rs.updateLong(idColName, nextSeq);
                rs.insertRow();
            }
            // Updateable result sets will always result in database locks, so we don't
            // need to check if another thread has created a sequence.... the method is also
            // synchronized to protect against the case where no row exists yet
            if (manualTX) {
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
        return nextSeq;
    }
}
