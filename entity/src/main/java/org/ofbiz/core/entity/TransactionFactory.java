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

import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * TransactionFactory - central source for JTA objects
 *
 * @author     <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 * @version    $Revision: 1.1 $
 * @since      2.0
 */
public class TransactionFactory {

    public static TransactionFactoryInterface transactionFactory = null;

    public static TransactionFactoryInterface getTransactionFactory() {
        if (transactionFactory == null) { // don't want to block here
            synchronized (TransactionFactory.class) {
                // must check if null again as one of the blocked threads can still enter
                if (transactionFactory == null) {
                    try {
                        String className = EntityConfigUtil.getInstance().getTxFactoryClass();

                        if (className == null) {
                            throw new IllegalStateException("Could not find transaction factory class name definition");
                        }
                        Class<?> tfClass = null;

                        if (className != null && className.length() > 0) {
                            try {
                                ClassLoader loader = Thread.currentThread().getContextClassLoader();
                                tfClass = loader.loadClass(className);
                            } catch (ClassNotFoundException e) {
                                Debug.logWarning(e);
                                throw new IllegalStateException("Error loading TransactionFactory class \"" + className + "\": " + e.getMessage());
                            }
                        }

                        try {
                            transactionFactory = (TransactionFactoryInterface) tfClass.newInstance();
                        } catch (IllegalAccessException e) {
                            Debug.logWarning(e);
                            throw new IllegalStateException("Error loading TransactionFactory class \"" + className + "\": " + e.getMessage());
                        } catch (InstantiationException e) {
                            Debug.logWarning(e);
                            throw new IllegalStateException("Error loading TransactionFactory class \"" + className + "\": " + e.getMessage());
                        }
                    } catch (SecurityException e) {
                        Debug.logError(e);
                        throw new IllegalStateException("Error loading TransactionFactory class: " + e.getMessage());
                    }
                }
            }
        }
        return transactionFactory;
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
