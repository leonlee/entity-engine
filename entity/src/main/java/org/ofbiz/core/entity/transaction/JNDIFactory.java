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
import java.util.concurrent.Callable;
import javax.sql.*;

import com.atlassian.util.concurrent.CopyOnWriteMap;

import org.ofbiz.core.entity.*;
import org.ofbiz.core.entity.config.*;
import org.ofbiz.core.config.*;
import org.ofbiz.core.entity.jdbc.interceptors.connection.ConnectionPoolInfoSynthesizer;
import org.ofbiz.core.entity.jdbc.interceptors.connection.ConnectionTracker;
import org.ofbiz.core.entity.util.TenantInfo;
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
    protected static Map<String, ConnectionTracker> trackerCache = CopyOnWriteMap.newHashMap();

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

        // Check if there is a tenant datasource and configure it for use.
        DatasourceInfo datasourceInfo = TenantInfo.getTenantDatasource();
        if (datasourceInfo != null) {
            System.out.println(">>> tenant datasource info:" + datasourceInfo);
            System.out.println(">>> setting helper name to:" + datasourceInfo.getName());
            helperName = datasourceInfo.getName();

        // Otherwise, fall back to the basic helper.
        } else {
            System.out.println(">>> falling back to helper datasource:" + helperName);
            datasourceInfo = EntityConfigUtil.getInstance().getDatasourceInfo(helperName);
        }

        if (datasourceInfo.getJndiDatasource() != null) {
            JndiDatasourceInfo jndiDatasource = datasourceInfo.getJndiDatasource();
            Connection con = getJndiConnection(helperName, jndiDatasource.getJndiName(), jndiDatasource.getJndiServerName());
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
    
    private static Connection getJndiConnection(String helperName, String jndiName, String jndiServerName) throws SQLException, GenericEntityException {
        Object ds;

        ds = dsCache.get(jndiName);
        if (ds != null) {
            if (ds instanceof XADataSource) {
                XADataSource xads = (XADataSource) ds;
                return trackConnection(helperName, xads);
            } else {
                DataSource nds = (DataSource) ds;
                return trackConnection(helperName, nds);
            }
        }

        synchronized (ConnectionFactory.class) {
            // try again inside the synch just in case someone when through while we were waiting
            ds = dsCache.get(jndiName);
            if (ds != null) {
                if (ds instanceof XADataSource) {
                    XADataSource xads = (XADataSource) ds;
                    return trackConnection(helperName, xads);
                } else {
                    DataSource nds = (DataSource) ds;
                    return trackConnection(helperName, nds);
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

                    if (ds instanceof XADataSource) {
                        if (Debug.infoOn()) Debug.logInfo("Got XADataSource for name " + jndiName, module);
                        XADataSource xads = (XADataSource) ds;

                        trackerCache.put(helperName, new ConnectionTracker());
                        return trackConnection(helperName,xads);
                    } else {
                        if (Debug.infoOn()) Debug.logInfo("Got DataSource for name " + jndiName, module);
                        DataSource nds = (DataSource) ds;

                        trackerCache.put(helperName, new ConnectionTracker(ConnectionPoolInfoSynthesizer.synthesizeConnectionPoolInfo(nds)));
                        return trackConnection(helperName,nds);
                    }
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

    private static Connection trackConnection(final String helperName, final XADataSource xads)
    {
        ConnectionTracker connectionTracker = trackerCache.get(helperName);
        return connectionTracker.trackConnection(helperName, new Callable<Connection>()
        {
            public Connection call() throws Exception
            {
                return TransactionUtil.enlistConnection(xads.getXAConnection());
            }
        });
    }

    private static Connection trackConnection(final String helperName, final DataSource nds)
    {
        ConnectionTracker connectionTracker = trackerCache.get(helperName);
        return connectionTracker.trackConnection(helperName, new Callable<Connection>()
        {
            public Connection call() throws Exception
            {
                return nds.getConnection();
            }
        });
    }



    public void removeDatasource(final String helperName)
    {
        DatasourceInfo datasourceInfo = EntityConfigUtil.getInstance().getDatasourceInfo(helperName);
        if (datasourceInfo.getJndiDatasource() == null && datasourceInfo.getJdbcDatasource() != null)
        {
            // If a JDBC connection was configured, then there may be one here
            ConnectionFactory.removeDatasource(helperName);
        }
        trackerCache.remove(helperName);
    }
}
