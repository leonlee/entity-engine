/*
 * $Id: TransactionUtil.java,v 1.5 2006/08/04 05:23:17 amazkovoi Exp $
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

import java.sql.*;
import javax.sql.*;
import javax.transaction.*;
import javax.transaction.xa.*;

import org.ofbiz.core.util.*;

/**
 * <p>Transaction Utility to help with some common transaction tasks
 * <p>Provides a wrapper around the transaction objects to allow for changes in underlying implementations in the future.
 *
 * @author     <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 * @version    $Revision: 1.5 $
 * @since      2.0
 */
public class TransactionUtil implements javax.transaction.Status {
    // Debug module name
    public static final String module = TransactionUtil.class.getName();

    public static final ThreadLocal localTransaction = new ThreadLocal();

    /** Begins a transaction in the current thread IF transactions are available; only
     * tries if the current transaction status is ACTIVE, if not active it returns false.
     * If and on only if it begins a transaction it will return true. In other words, if
     * a transaction is already in place it will return false and do nothing.
     */
    public static boolean begin() throws GenericTransactionException {
        UserTransaction ut = TransactionFactory.getUserTransaction();

        if (ut != null) {
            try {
                if (ut.getStatus() == TransactionUtil.STATUS_ACTIVE) {
                    Debug.logVerbose("[TransactionUtil.begin] active transaction in place, so no transaction begun", module);
                    return false;
                }
                ut.begin();
                Debug.logVerbose("[TransactionUtil.begin] transaction begun", module);
                return true;
            } catch (NotSupportedException e) {
                //This is Java 1.4 only, but useful for certain debuggins: Throwable t = e.getCause() == null ? e : e.getCause();
                throw new GenericTransactionException("Not Supported error, could not begin transaction (probably a nesting problem)", e);
            } catch (SystemException e) {
                //This is Java 1.4 only, but useful for certain debuggins: Throwable t = e.getCause() == null ? e : e.getCause();
                throw new GenericTransactionException("System error, could not begin transaction", e);
            }
        } else {
            Debug.logInfo("[TransactionUtil.begin] no user transaction, so no transaction begun", module);
            return false;
        }
    }

    /** Gets the status of the transaction in the current thread IF
     * transactions are available, otherwise returns STATUS_NO_TRANSACTION */
    public static int getStatus() throws GenericTransactionException {
        UserTransaction ut = TransactionFactory.getUserTransaction();

        if (ut != null) {
            try {
                return ut.getStatus();
            } catch (SystemException e) {
                throw new GenericTransactionException("System error, could not get status", e);
            }
        } else {
            return STATUS_NO_TRANSACTION;
        }
    }

    /** Commits the transaction in the current thread IF transactions are available
     *  AND if beganTransaction is true
     */
    public static void commit(boolean beganTransaction) throws GenericTransactionException {
        if (beganTransaction)
            TransactionUtil.commit();
    }

    /** Commits the transaction in the current thread IF transactions are available */
    public static void commit() throws GenericTransactionException {
        UserTransaction ut = TransactionFactory.getUserTransaction();

        if (ut != null) {
            try {
                int status = ut.getStatus();

                if (status != STATUS_NO_TRANSACTION) {
                    ut.commit();
                    Debug.logVerbose("[TransactionUtil.commit] transaction committed", module);
                } else {
                    Debug.logInfo("[TransactionUtil.commit] Not committing transaction, status is STATUS_NO_TRANSACTION", module);
                }
            } catch (RollbackException e) {
                //This is Java 1.4 only, but useful for certain debuggins: Throwable t = e.getCause() == null ? e : e.getCause();
                throw new GenericTransactionException("Roll back error, could not commit transaction, was rolled back instead", e);
            } catch (HeuristicMixedException e) {
                //This is Java 1.4 only, but useful for certain debuggins: Throwable t = e.getCause() == null ? e : e.getCause();
                throw new GenericTransactionException("Could not commit transaction, HeuristicMixed exception", e);
            } catch (HeuristicRollbackException e) {
                //This is Java 1.4 only, but useful for certain debuggins: Throwable t = e.getCause() == null ? e : e.getCause();
                throw new GenericTransactionException("Could not commit transaction, HeuristicRollback exception", e);
            } catch (SystemException e) {
                //This is Java 1.4 only, but useful for certain debuggins: Throwable t = e.getCause() == null ? e : e.getCause();
                throw new GenericTransactionException("System error, could not commit transaction", e);
            }
        } else {
            Debug.logInfo("[TransactionUtil.commit] UserTransaction is null, not commiting", module);
        }
    }

    /** Rolls back transaction in the current thread IF transactions are available
     *  AND if beganTransaction is true; if beganTransaction is not true,
     *  setRollbackOnly is called to insure that the transaction will be rolled back
     */
    public static void rollback(boolean beganTransaction) throws GenericTransactionException {
        if (beganTransaction) {
            TransactionUtil.rollback();
        } else {
            TransactionUtil.setRollbackOnly();
        }
    }

    /** Rolls back transaction in the current thread IF transactions are available */
    public static void rollback() throws GenericTransactionException {
        UserTransaction ut = TransactionFactory.getUserTransaction();

        if (ut != null) {
            try {
                int status = ut.getStatus();

                if (status != STATUS_NO_TRANSACTION) {
                    ut.rollback();
                    Debug.logInfo("[TransactionUtil.rollback] transaction rolled back", module);
                } else {
                    Debug.logInfo("[TransactionUtil.rollback] transaction not rolled back, status is STATUS_NO_TRANSACTION", module);
                }
            } catch (SystemException e) {
                //This is Java 1.4 only, but useful for certain debuggins: Throwable t = e.getCause() == null ? e : e.getCause();
                throw new GenericTransactionException("System error, could not roll back transaction", e);
            }
        } else {
            Debug.logInfo("[TransactionUtil.rollback] No UserTransaction, transaction not rolled back", module);
        }
    }

    /** Makes a roll back the only possible outcome of the transaction in the current thread IF transactions are available */
    public static void setRollbackOnly() throws GenericTransactionException {
        UserTransaction ut = TransactionFactory.getUserTransaction();

        if (ut != null) {
            try {
                int status = ut.getStatus();

                if (status != STATUS_NO_TRANSACTION) {
                    ut.setRollbackOnly();
                    Debug.logInfo("[TransactionUtil.setRollbackOnly] transaction roll back only set", module);
                } else {
                    Debug.logInfo("[TransactionUtil.setRollbackOnly] transaction roll back only set, status is STATUS_NO_TRANSACTION", module);
                }
            } catch (SystemException e) {
                //This is Java 1.4 only, but useful for certain debuggins: Throwable t = e.getCause() == null ? e : e.getCause();
                throw new GenericTransactionException("System error, could not set roll back only on transaction", e);
            }
        } else {
            Debug.logInfo("[TransactionUtil.setRollbackOnly] No UserTransaction, transaction roll back only not set", module);
        }
    }

    /** Sets the timeout of the transaction in the current thread IF transactions are available */
    public static void setTransactionTimeout(int seconds) throws GenericTransactionException {
        UserTransaction ut = TransactionFactory.getUserTransaction();

        if (ut != null) {
            try {
                ut.setTransactionTimeout(seconds);
            } catch (SystemException e) {
                throw new GenericTransactionException("System error, could not set transaction timeout", e);
            }
        }
    }

    /** Enlists the given XAConnection and if a transaction is active in the current thread, returns a plain JDBC Connection */
    public static Connection enlistConnection(XAConnection xacon) throws GenericTransactionException {
        if (xacon == null)
            return null;
        try {
            XAResource resource = xacon.getXAResource();

            TransactionUtil.enlistResource(resource);
            return xacon.getConnection();
        } catch (SQLException e) {
            throw new GenericTransactionException("SQL error, could not enlist connection in transaction even though transactions are available", e);
        }
    }

    public static void enlistResource(XAResource resource) throws GenericTransactionException {
        if (resource == null)
            return;

        try {
            TransactionManager tm = TransactionFactory.getTransactionManager();

            if (tm != null && tm.getStatus() == STATUS_ACTIVE) {
                Transaction tx = tm.getTransaction();

                if (tx != null)
                    tx.enlistResource(resource);
            }
        } catch (RollbackException e) {
            //This is Java 1.4 only, but useful for certain debuggins: Throwable t = e.getCause() == null ? e : e.getCause();
            throw new GenericTransactionException("Roll Back error, could not enlist connection in transaction even though transactions are available, current transaction rolled back", e);
        } catch (SystemException e) {
            //This is Java 1.4 only, but useful for certain debuggins: Throwable t = e.getCause() == null ? e : e.getCause();
            throw new GenericTransactionException("System error, could not enlist connection in transaction even though transactions are available", e);
        }
    }


    // -------------- Atlassian Added methods for Local Transactions -----------------------------------------
    /**
     * Starts a transaction if one does not exist already.
     * @param helperName the OfBiz helperName that is regeistered in entityengine.xml. The helperName is used
     * to retrieve the connection from the {@link com.atlassian.core.ofbiz.CoreFactory} object.
     * @param transactionIsolationLevel the transaction isolation level to set on teh connection if the transaction is started
     * see {@link Connection.TRANSACTION_NONE}, {@link Connection.TRANSACTION_READ_COMMITTED}, etc. Negative means do not set anything
     * (the current default on the connection will be left)
     * @return true if the transaction was started, false if one was active already
     * @throws GenericTransactionException if something goes wrong. See the getNested() method of the exception
     * for the underlying exception.
     */
    public static boolean beginLocalTransaction(String helperName, int transactionIsolationLevel) throws GenericTransactionException
    {
        try
        {
            if (!isTransactionActive())
            {
                Debug.logInfo("[TransactionUtil.beginLocalTransaction] Transaction not started so starting transaction.", module);
                Connection connection = ConnectionFactory.getConnection(helperName);
                if (transactionIsolationLevel >= 0)
                    connection.setTransactionIsolation(transactionIsolationLevel);
                connection.setAutoCommit(false);
                localTransaction.set(connection);
                Debug.logInfo("[TransactionUtil.beginLocalTransaction] Transaction started.", module);
                return true;
            }
            else
            {
                Debug.logInfo("[TransactionUtil.beginLocalTransaction] Transaction already started so not starting transaction.", module);
                return false;
            }
        }
        catch (SQLException e)
        {
            throw new GenericTransactionException("Error occurred while starting transaction.", e);
        }
        catch(GenericEntityException e)
        {
            throw new GenericTransactionException("Error occurred while starting transaction.", e);
        }
    }

    /**
     * @return the {@link Connection} that has an active connection for the current thread.
     */
    public static Connection getLocalTransactionConnection()
    {
        return (Connection) localTransaction.get();
    }

    /**
     * Checks if there is a {@link Connection} with a transaction for the current thread.
     * @return true if there is an active transaction for the current thread
     */
    public static boolean isTransactionActive()
    {
        return (getLocalTransactionConnection() != null);
    }

    /**
     * Commits a transaction if beganTransaction is true and there is an active transaction. See {@link #isTransactionActive()}.
     * If beganTransaction is false or if there is no active transaction this method does nothing.
     * <p>
     * Common usage is:
     * <pre>
     * boolean started = TransactionUtil.beginLocalTransaction("default", Connection.TRANSACTION_READ_COMMITTED);
     * ...
     * TransactionUtil.commitLocalTransaction(started);
     * </pre>
     * @param beganTransaction whether the transaction was started
     * @throws GenericTransactionException  if something goes wrong. See the getNested() method of the exception
     * for the underlying exception.
     */
    public static void commitLocalTransaction(boolean beganTransaction) throws GenericTransactionException
    {
        if (!beganTransaction)
        {
            Debug.logInfo("[TransactionUtil.commitLocalTransaction] Transaction not started so not committing transaction.", module);
            return;
        }

        if (isTransactionActive())
        {
            try
            {
                Debug.logInfo("[TransactionUtil.commitLocalTransaction] Transaction started and active so committing transaction.", module);
                getLocalTransactionConnection().commit();
                Debug.logInfo("[TransactionUtil.commitLocalTransaction] Transaction committed.", module);

            }
            catch (SQLException e)
            {
                throw new GenericTransactionException("Error occurred while starting transaction.", e);
            }
            finally
            {
                closeAndClearThreadLocalConnection();
            }
        }
        else
        {
            Debug.logInfo("[TransactionUtil.commitLocalTransaction] Transaction not active so not committing transaction.", module);
        }
    }

    /**
     * Rolls back a transaction if beganTransaction is true and there is an active transaction. See {@link #isTransactionActive()}.
     * If beganTransaction is false or if there is no active transaction this method does nothing.
     * <p>
     * Common usage is:
     * <pre>
     * boolean started = TransactionUtil.beginLocalTransaction("default", Connection.TRANSACTION_READ_COMMITTED);
     * ...
     * TransactionUtil.rollbackLocalTransaction(started);
     * </pre>
     * @param beganTransaction whether the transaction was started
     * @throws GenericTransactionException  if something goes wrong. See the getNested() method of the exception
     * for the underlying exception.
     */
    public static void rollbackLocalTransaction(boolean beganTransaction) throws GenericTransactionException
    {
        if (!beganTransaction)
        {
            Debug.logInfo("[TransactionUtil.rollbackLocalTransaction] Transaction not started so not rolling back.", module);
            return;
        }

        if (isTransactionActive())
        {
            try
            {
                Debug.logInfo("[TransactionUtil.rollbackLocalTransaction] Transaction started and active so rolling back.", module);
                getLocalTransactionConnection().rollback();
                Debug.logInfo("[TransactionUtil.rollbackLocalTransaction] Transaction rolled back.", module);
            }
            catch (SQLException e)
            {
                throw new GenericTransactionException("Error occurred while rolling back transaction.", e);
            }
            finally
            {
                closeAndClearThreadLocalConnection();
            }
        }
        else
        {
            Debug.logInfo("[TransactionUtil.rollbackLocalTransaction] Transaction not active so not rolling back.", module);
        }
    }

    /**
     * If a connection exists in the thread local close it. Clear the thread local no matter what.
     */
    public static void closeAndClearThreadLocalConnection()
    {
        Connection connection = getLocalTransactionConnection();

        try
        {
            if (connection != null)
            {
                connection.close();
                Debug.logInfo("Connection closed.", module);
            }
        }
        catch(SQLException se)
        {
            Debug.logInfo(se, "Exception occurred while closing connection after transaction commit. Ignoring the exception.", module);
        }
        finally
        {
            // No matter what happens reset the thread local.
            clearTransactionThreadLocal();
        }
    }

    /**
     * A method that ensures the connection is cleared. This is useful to call from within a servlet filter to ensure that
     * connections are not leaked anywhere.
     */
    public static void clearTransactionThreadLocal()
    {
        localTransaction.set(null);
        Debug.logInfo("Thread local cleared.", module);
    }
}
