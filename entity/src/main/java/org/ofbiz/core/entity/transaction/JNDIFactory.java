/*
 * $Id: JNDIFactory.java,v 1.1 2005/04/01 05:58:03 sfarquhar Exp $
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

package org.ofbiz.core.entity.transaction;

import java.util.*;
import javax.naming.*;
import javax.transaction.*;
import java.sql.*;
import javax.sql.*;

import com.atlassian.util.concurrent.CopyOnWriteMap;

import org.ofbiz.core.entity.*;
import org.ofbiz.core.entity.config.*;
import org.ofbiz.core.config.*;
import org.ofbiz.core.util.*;

/**
 * Central source for Tyrex JTA objects from JNDI
 *
 * @author     <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 * @version    $Revision: 1.1 $
 * @since      2.0
 */
public class JNDIFactory implements TransactionFactoryInterface {
    
    // Debug module name
    public static final String module = JNDIFactory.class.getName();

    static TransactionManager transactionManager = null;
    static UserTransaction userTransaction = null;

    protected static Map<String, Object> dsCache = CopyOnWriteMap.newHashMap();

    public TransactionManager getTransactionManager() {
        if (transactionManager == null) {
            synchronized (JNDIFactory.class) {
                // try again inside the synch just in case someone when through while we were waiting
                if (transactionManager == null) {
                    try {
                        String jndiName = EntityConfigUtil.getInstance().getTxFactoryTxMgrJndiName();
                        String jndiServerName = EntityConfigUtil.getInstance().getTxFactoryTxMgrJndiServerName();

                        if (jndiName != null && jndiName.length() > 0) {
                            // if (Debug.verboseOn()) Debug.logVerbose("[JNDIFactory.getTransactionManager] Trying JNDI name " + jndiName, module);

                            try {
                                InitialContext ic = JNDIContextFactory.getInitialContext(jndiServerName);

                                if (ic != null) {
                                    transactionManager = (TransactionManager) ic.lookup(jndiName);
                                }
                            } catch (NamingException ne) {
                                Debug.logWarning(ne, "NamingException while finding TransactionManager named " + jndiName + " in JNDI.", module);
                                transactionManager = null;
                            }
                            if (transactionManager == null) {
                                Debug.logWarning("[JNDIFactory.getTransactionManager] Failed to find TransactionManager named " + jndiName + " in JNDI.", module);
                            }
                        }
                    } catch (GeneralException e) {
                        Debug.logError(e);
                        transactionManager = null;
                    }
                }
            }
        }
        return transactionManager;
    }

    public UserTransaction getUserTransaction() {
        if (userTransaction == null) {
            synchronized (JNDIFactory.class) {
                // try again inside the synch just in case someone when through while we were waiting
                if (userTransaction == null) {
                    try {
                        String jndiName = EntityConfigUtil.getInstance().getTxFactoryUserTxJndiName();
                        String jndiServerName = EntityConfigUtil.getInstance().getTxFactoryUserTxJndiServerName();

                        if (jndiName != null && jndiName.length() > 0) {
                            // if (Debug.verboseOn()) Debug.logVerbose("[JNDIFactory.getTransactionManager] Trying JNDI name " + jndiName, module);

                            try {
                                InitialContext ic = JNDIContextFactory.getInitialContext(jndiServerName);

                                if (ic != null) {
                                    userTransaction = (UserTransaction) ic.lookup(jndiName);
                                }
                            } catch (NamingException ne) {
                                Debug.logWarning(ne, "NamingException while finding UserTransaction named " + jndiName + " in JNDI.", module);
                                userTransaction = null;
                            }
                            if (userTransaction == null) {
                                Debug.logWarning("[JNDIFactory.getUserTransaction] Failed to find UserTransaction named " + jndiName + " in JNDI.", module);
                            }
                        }
                    } catch (GeneralException e) {
                        Debug.logError(e);
                        transactionManager = null;
                    }
                }
            }
        }
        return userTransaction;
    }
    
    public String getTxMgrName() {
        return "jndi";
    }
    
    public Connection getConnection(String helperName) throws SQLException, GenericEntityException {
        DatasourceInfo datasourceInfo = EntityConfigUtil.getInstance().getDatasourceInfo(helperName);

        if (datasourceInfo.getJndiDatasource() != null) {
            JndiDatasourceInfo jndiDatasource = datasourceInfo.getJndiDatasource();
            Connection con = getJndiConnection(jndiDatasource.getJndiName(), jndiDatasource.getJndiServerName());
            if (con != null) return con;
        }
        if (datasourceInfo.getJdbcDatasource() != null) {
            Connection otherCon = ConnectionFactory.tryGenericConnectionSources(helperName, datasourceInfo.getJdbcDatasource());
            return otherCon;
        } else {
            //no real need to print an error here
            return null;
        }
    }
    
    public static Connection getJndiConnection(String jndiName, String jndiServerName) throws SQLException, GenericEntityException {
        // if (Debug.verboseOn()) Debug.logVerbose("Trying JNDI name " + jndiName, module);
        Object ds;

        ds = dsCache.get(jndiName);
        if (ds != null) {
            if (ds instanceof XADataSource) {
                XADataSource xads = (XADataSource) ds;

                return TransactionUtil.enlistConnection(xads.getXAConnection());
            } else {
                DataSource nds = (DataSource) ds;

                return nds.getConnection();
            }
        }

        synchronized (ConnectionFactory.class) {
            // try again inside the synch just in case someone when through while we were waiting
            ds = dsCache.get(jndiName);
            if (ds != null) {
                if (ds instanceof XADataSource) {
                    XADataSource xads = (XADataSource) ds;

                    return TransactionUtil.enlistConnection(xads.getXAConnection());
                } else {
                    DataSource nds = (DataSource) ds;

                    return nds.getConnection();
                }
            }

            try {
                if (Debug.infoOn()) Debug.logInfo("Doing JNDI lookup for name " + jndiName, module);
                InitialContext ic = JNDIContextFactory.getInitialContext(jndiServerName);

                if (ic != null) {
                    ds = ic.lookup(jndiName);
                } else {
                    Debug.logWarning("Initial Context returned was NULL for server name " + jndiServerName, module);
                }

                if (ds != null) {
                    if (Debug.verboseOn()) Debug.logVerbose("Got a Datasource object.", module);
                    dsCache.put(jndiName, ds);
                    Connection con = null;

                    if (ds instanceof XADataSource) {
                        if (Debug.infoOn()) Debug.logInfo("Got XADataSource for name " + jndiName, module);
                        XADataSource xads = (XADataSource) ds;
                        XAConnection xac = xads.getXAConnection();

                        con = TransactionUtil.enlistConnection(xac);
                    } else {
                        if (Debug.infoOn()) Debug.logInfo("Got DataSource for name " + jndiName, module);
                        DataSource nds = (DataSource) ds;

                        con = nds.getConnection();
                    }

                    /* NOTE: This code causes problems because settting the transaction isolation level after a transaction has started is a no-no
                     * The question is: how should we do this?
                     String isolationLevel = jndiJdbcElement.getAttribute("isolation-level");
                     if (con != null && isolationLevel != null && isolationLevel.length() > 0) {
                     if ("Serializable".equals(isolationLevel)) {
                     con.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
                     } else if ("RepeatableRead".equals(isolationLevel)) {
                     con.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
                     } else if ("ReadUncommitted".equals(isolationLevel)) {
                     con.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
                     } else if ("ReadCommitted".equals(isolationLevel)) {
                     con.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
                     } else if ("None".equals(isolationLevel)) {
                     con.setTransactionIsolation(Connection.TRANSACTION_NONE);
                     }
                     }
                     */

                    // if (con != null) if (Debug.infoOn()) Debug.logInfo("[ConnectionFactory.getConnection] Got JNDI connection with catalog: " + con.getCatalog());
                    return con;
                } else {
                    Debug.logError("Datasource returned was NULL.", module);
                }
            } catch (NamingException ne) {
                Debug.logWarning(ne, "[ConnectionFactory.getConnection] Failed to find DataSource named " + jndiName + " in JNDI server with name " + jndiServerName + ". Trying normal database.", module);
            } catch (GenericConfigException gce) {
                throw new GenericEntityException("Problems with the JNDI configuration.", gce.getNested());
            }
        }
        return null;
    }
}
