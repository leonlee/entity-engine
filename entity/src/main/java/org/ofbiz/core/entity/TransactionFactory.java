/*
 * $Id: TransactionFactory.java,v 1.1 2005/04/01 05:58:02 sfarquhar Exp $
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

import org.ofbiz.core.entity.config.EntityConfigUtil;
import org.ofbiz.core.entity.transaction.TransactionFactoryInterface;
import org.ofbiz.core.util.Debug;

import javax.annotation.concurrent.GuardedBy;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * TransactionFactory - central source for JTA objects
 *
 * @author <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 * @version $Revision: 1.1 $
 * @since 2.0
 */
public class TransactionFactory {
    @GuardedBy("TransactionFactory.class")
    private static volatile TransactionFactoryInterface transactionFactory = null;

    public static TransactionFactoryInterface getTransactionFactory() {
        final TransactionFactoryInterface existing = transactionFactory;
        return (existing != null) ? existing : getTransactionFactoryUnderLock();
    }

    synchronized private static TransactionFactoryInterface getTransactionFactoryUnderLock() {
        final TransactionFactoryInterface existing = transactionFactory;
        if (existing != null) {
            return existing;
        }

        try {
            final TransactionFactoryInterface created = createTransactionFactory();
            transactionFactory = created;
            return created;
        } catch (SecurityException se) {
            Debug.logError(se);
            throw new IllegalStateException("Error loading TransactionFactory class: " + se.getMessage());
        }
    }

    @GuardedBy("TransactionFactory.class")
    private static TransactionFactoryInterface createTransactionFactory() {
        final String className = EntityConfigUtil.getInstance().getTxFactoryClass();
        if (className == null || className.isEmpty()) {
            throw new IllegalStateException("Could not find transaction factory class name definition");
        }

        final Class<?> tfClass = loadClass(className);
        try {
            return (TransactionFactoryInterface) tfClass.newInstance();
        } catch (IllegalAccessException | InstantiationException e) {
            Debug.logWarning(e);
            throw new IllegalStateException("Error loading TransactionFactory class \"" + className + "\": "
                    + e.getMessage());
        }
    }

    private static Class<?> loadClass(String className) {
        final Class<?> tfClass;
        try {
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            if (loader == null) {
                loader = TransactionFactory.class.getClassLoader();
            }
            tfClass = loader.loadClass(className);
        } catch (ClassNotFoundException e) {
            Debug.logWarning(e);
            throw new IllegalStateException(
                    "Error loading TransactionFactory class \"" + className + "\": " + e.getMessage());
        }
        return tfClass;
    }

    public static TransactionManager getTransactionManager() {
        return getTransactionFactory().getTransactionManager();
    }

    public static UserTransaction getUserTransaction() {
        return getTransactionFactory().getUserTransaction();
    }

    public static String getTxMgrName() {
        return getTransactionFactory().getTxMgrName();
    }

    public static Connection getConnection(String helperName) throws SQLException, GenericEntityException {
        return getTransactionFactory().getConnection(helperName);
    }
}
