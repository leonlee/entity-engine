/*
 * $Id: SequenceUtil.java,v 1.4 2005/11/14 00:54:37 amazkovoi Exp $
 *
 *  Copyright (c) 2001, 2002 The Open For Business Project - www.ofbiz.org
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a
 *  copy of this software and associated documentation files (the "Software"),
 *  to deal in the Software without restriction, including without limitation
 *  the rights to use, copy, modify, merge, publish, distribute, sublicense,
 *  and/or sell copies of the Software, and to permit persons to whom the
 *  Software is furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included
 *  in all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 *  OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 *  IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 *  CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT
 *  OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 *  THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.ofbiz.core.entity;

import org.ofbiz.core.entity.model.ModelEntity;
import org.ofbiz.core.entity.model.ModelField;
import org.ofbiz.core.util.Debug;

import javax.transaction.InvalidTransactionException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Hashtable;
import java.util.Map;

/**
 * Sequence Utility to get unique sequences from named sequence banks
 * Uses a collision detection approach to safely get unique sequenced ids in banks from the database
 *
 * @author     <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 * @version    $Revision: 1.4 $
 * @since      2.0
 */
public class SequenceUtil {

    public static final String module = SequenceUtil.class.getName();

    Map<String, SequenceBank> sequences = new Hashtable<String, SequenceBank>();
    String helperName;
    ModelEntity seqEntity;
    String tableName;
    String nameColName;
    String idColName;

    private SequenceUtil() {}

    public SequenceUtil(String helperName, ModelEntity seqEntity, String nameFieldName, String idFieldName) {
        this.helperName = helperName;
        this.seqEntity = seqEntity;
        if (seqEntity == null) {
            throw new IllegalArgumentException("The sequence model entity was null but is required.");
        }
        this.tableName = seqEntity.getTableName(helperName);

        ModelField nameField = seqEntity.getField(nameFieldName);

        if (nameField == null) {
            throw new IllegalArgumentException("Could not find the field definition for the sequence name field " + nameFieldName);
        }
        this.nameColName = nameField.getColName();

        ModelField idField = seqEntity.getField(idFieldName);

        if (idField == null) {
            throw new IllegalArgumentException("Could not find the field definition for the sequence id field " + idFieldName);
        }
        this.idColName = idField.getColName();
    }

    public Long getNextSeqId(String seqName) {
        SequenceBank bank = sequences.get(seqName);

        if (bank == null) {
            bank = constructSequenceBank(seqName);
        }
        return bank.getNextSeqId();
    }
    
    /**
     * this is hit if we can't get one from the cache, must be synchronized
     */
    private synchronized SequenceBank constructSequenceBank(String seqName)
    {
        // check the cache first in-case someone has already populated 
        SequenceBank bank = sequences.get(seqName);
        if (bank == null) {
            bank = new SequenceBank(seqName, this);
            sequences.put(seqName, bank);
        }
        return bank;
    }

    class SequenceBank {

        public static final long bankSize = 100;
        public static final long startSeqId = 10000;
        public static final int minWaitNanos = 500000;   // 1/2 ms
        public static final int maxWaitNanos = 1000000;  // 1 ms
        public static final int maxTries = 5;

        long curSeqId;
        long maxSeqId;
        String seqName;
        SequenceUtil parentUtil;

        public SequenceBank(String seqName, SequenceUtil parentUtil) {
            this.seqName = seqName;
            this.parentUtil = parentUtil;
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
                    Debug.logError("[SequenceUtil.SequenceBank.getNextSeqId] Fill bank failed, returning null", module);
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
                Debug.logWarning("[SequenceUtil.SequenceBank.fillBank] Exception was thrown trying to check " +
                    "transaction status: " + e.toString(), module);
            }

            Connection connection = null;
            ResultSet rs = null;

            try {
                connection = ConnectionFactory.getConnection(parentUtil.helperName);
            } catch (SQLException sqle) {
                Debug.logWarning("[SequenceUtil.SequenceBank.fillBank]: Unable to esablish a connection with the database... Error was:", module);
                Debug.logWarning(sqle.getMessage(), module);
            } catch (GenericEntityException e) {
                Debug.logWarning("[SequenceUtil.SequenceBank.fillBank]: Unable to esablish a connection with the database... Error was:", module);
                Debug.logWarning(e.getMessage(), module);
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
                    if (Debug.verboseOn()) Debug.logVerbose("[SequenceUtil.SequenceBank.fillBank] Trying to get a bank of sequenced ids for " +
                            this.seqName + "; start of loop val1=" + val1 + ", val2=" + val2 + ", bankSize=" + bankSize, module);

                    // try to SELECT the next id
                    if (selectPstmt == null) {
                        selectPstmt = connection.prepareStatement("SELECT " + parentUtil.idColName + " FROM " + parentUtil.tableName + " WHERE " + parentUtil.nameColName + "=?");
                    }
                    selectPstmt.setString(1, this.seqName);
                    selectPstmt.execute();

                    rs = selectPstmt.getResultSet();
                    if (rs.next()) {
                        val1 = rs.getInt(parentUtil.idColName);
                    } else {
                        Debug.logVerbose("[SequenceUtil.SequenceBank.fillBank] first select failed: trying to add " +
                                "row, result set was empty for sequence: " + seqName, module);
                        closeQuietly(rs);

                        // INSERT the row if it doesn't exist
                        if (insertPstmt == null) {
                            insertPstmt = connection.prepareStatement("INSERT INTO " + parentUtil.tableName + " (" + parentUtil.nameColName + ", " + parentUtil.idColName + ") VALUES (?,?)");
                        }
                        insertPstmt.setString(1, this.seqName);
                        insertPstmt.setLong(2, startSeqId);
                        insertPstmt.execute();

                        if (insertPstmt.getUpdateCount() <= 0) return;
                        continue;
                    }
                    closeQuietly(rs);

                    // UPDATE the next id by adding bankSize
                    if (updatePstmt == null) {
                            updatePstmt = connection.prepareStatement("UPDATE " + parentUtil.tableName + " SET " + parentUtil.idColName + "=" + parentUtil.idColName + "+" + SequenceBank.bankSize + " WHERE " + parentUtil.nameColName + "=?");
                    }
                    updatePstmt.setString(1, this.seqName);
                    updatePstmt.execute();

                    if (updatePstmt.getUpdateCount() <= 0) {
                        Debug.logWarning("[SequenceUtil.SequenceBank.fillBank] update failed, no rows changes for seqName: " + seqName, module);
                        return;
                    }

                    if (manualTX) {
                        connection.commit();
                    }

                    // try to SELECT the next id
                    selectPstmt.setString(1, this.seqName);
                    selectPstmt.execute();
                    rs = selectPstmt.getResultSet();

                    if (rs.next()) {
                        val2 = rs.getInt(parentUtil.idColName);
                    } else {
                        Debug.logWarning("[SequenceUtil.SequenceBank.fillBank] second select failed: aborting, result " +
                            "set was empty for sequence: " + seqName, module);
                        closeQuietly(rs);
                        return;
                    }
                    closeQuietly(rs);

                    // Commit the connection to keep WebSphere happy. See the above comment when transaction was started.
                    if (manualTX) {
                        connection.commit();
                    }

                    if (val1 + bankSize != val2) {
                        if (numTries >= maxTries) {
                            Debug.logError("[SequenceUtil.SequenceBank.fillBank] maxTries (" + maxTries + ") reached, giving up.", module);
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
                }

                curSeqId = val1;
                maxSeqId = val2;
                if (Debug.verboseOn()) Debug.logVerbose("[SequenceUtil.SequenceBank.fillBank] Successfully got a bank of sequenced ids for " +
                        this.seqName + "; curSeqId=" + curSeqId + ", maxSeqId=" + maxSeqId + ", bankSize=" + bankSize, module);
            } catch (SQLException sqle) {
                Debug.logWarning(sqle, "[SequenceUtil.SequenceBank.fillBank] SQL Exception", module);
                return;
            } finally {
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
    }

    private void closeQuietly(Connection connection)
    {
        try
        {
            if (connection != null)
            {
                connection.close();
            }
        }
        catch (Exception sqle)
        {
            Debug.logWarning(sqle, "Error closing connection in sequence util");
        }
    }

    private void closeQuietly(PreparedStatement stmt)
    {
        try
        {
            if (stmt != null)
            {
                stmt.close();
            }
        }
        catch (Exception sqle)
        {
            Debug.logWarning(sqle, "Error closing statement in sequence util");
        }
    }

    private void closeQuietly(ResultSet rs)
    {
        try
        {
            if (rs != null)
            {
                rs.close();
            }
        }
        catch (Exception sqle)
        {
            Debug.logWarning(sqle, "Error closing result set in sequence util");
        }
    }
}
