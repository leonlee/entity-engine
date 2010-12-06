/*
 * $Id: JotmConnectionFactory.java,v 1.1 2005/04/01 05:58:03 sfarquhar Exp $
 *
 * Copyright (c) 2001, 2002 The Open For Business Project - www.ofbiz.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 * CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT
 * OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 * THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */
package org.ofbiz.core.entity.transaction;

import com.atlassian.util.concurrent.CopyOnWriteMap;
import org.enhydra.jdbc.pool.StandardXAPoolDataSource;
import org.enhydra.jdbc.standard.StandardXADataSource;
import org.ofbiz.core.entity.GenericEntityException;
import org.ofbiz.core.entity.TransactionFactory;
import org.ofbiz.core.entity.config.ConnectionPoolInfo;
import org.ofbiz.core.entity.config.JdbcDatasourceInfo;
import org.ofbiz.core.util.Debug;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

/**
 * JotmFactory - Central source for JOTM JDBC Objects
 *
 * @author     <a href="mailto:jaz@ofbiz.org">Andy Zeneski</a>
 * @version    $Revision: 1.1 $
 * @since      2.1
 */
public class JotmConnectionFactory {
        
    public static final String module = JotmConnectionFactory.class.getName();                
        
    protected static Map<String, StandardXAPoolDataSource> dsCache = CopyOnWriteMap.newHashMap();

    public static synchronized void removeDatasource(String helperName)
    {
        StandardXAPoolDataSource pds = dsCache.get(helperName);
        if (pds != null)
        {
            pds.shutdown(true);
            dsCache.remove(helperName);
        }
    }

    public static Connection getConnection(String helperName, JdbcDatasourceInfo jdbcDatasource) throws SQLException, GenericEntityException {
        StandardXAPoolDataSource pds = dsCache.get(helperName);
        if (pds != null) {                      
            if (Debug.verboseOn()) Debug.logInfo(helperName + " pool size: " + pds.pool.getCount(), module);
            //return TransactionUtil.enlistConnection(ds.getXAConnection());
            //return ds.getXAConnection().getConnection();
            return pds.getConnection();
        }
        
        synchronized (JotmConnectionFactory.class) {            
            pds = dsCache.get(helperName);
            if (pds != null) {              
                //return TransactionUtil.enlistConnection(ds.getXAConnection());
                //return ds.getXAConnection().getConnection();
                return pds.getConnection();
            }
              
            StandardXADataSource ds;          
            try {            
                ds =  new StandardXADataSource();
                pds = new StandardXAPoolDataSource();
            } catch (NoClassDefFoundError e) {                
                throw new GenericEntityException("Cannot find enhydra-jdbc.jar");                       
            }
            ds.setDriverName(jdbcDatasource.getDriverClassName());
            ds.setUrl(jdbcDatasource.getUri());
            ds.setUser(jdbcDatasource.getUsername());
            ds.setPassword(jdbcDatasource.getPassword());
            ds.setDescription(helperName);  
            ds.setTransactionManager(TransactionFactory.getTransactionManager()); 
            String transIso = jdbcDatasource.getIsolationLevel();
            if (transIso != null && transIso.length() > 0) {
                if ("Serializable".equals(transIso)) {
                    ds.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
                } else if ("RepeatableRead".equals(transIso)) {
                    ds.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
                } else if ("ReadUncommitted".equals(transIso)) {
                    ds.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
                } else if ("ReadCommitted".equals(transIso)) {
                    ds.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
                } else if ("None".equals(transIso)) {
                    ds.setTransactionIsolation(Connection.TRANSACTION_NONE);
                }                                            
            }
            // set the datasource in the pool
            pds.setDataSource(ds);
            pds.setDescription(ds.getDescription());
            pds.setUser(ds.getUser());
            pds.setPassword(ds.getPassword());
            // set the transaction manager in the pool
            pds.setTransactionManager(TransactionFactory.getTransactionManager());
            // configure the pool settings
            ConnectionPoolInfo poolInfo = jdbcDatasource.getConnectionPoolInfo();
            try
            {
                pds.setMaxSize(poolInfo.getMaxSize());
                pds.setMinSize(poolInfo.getMinSize());
                pds.setSleepTime(poolInfo.getSleepTime());
                pds.setLifeTime(poolInfo.getLifeTime());
                pds.setDeadLockMaxWait(poolInfo.getDeadLockMaxWait());
                pds.setDeadLockRetryWait(poolInfo.getDeadLockRetryWait());
            }
            catch (Exception e)
            {
                Debug.logError(e, "Problems with pool settings", module);
            }
            // TODO: set the test statement to test connections
            //pds.setJdbcTestStmt("select sysdate from dual");
            // cache the pool
            dsCache.put(helperName, pds);        
                                            
            //return TransactionUtil.enlistConnection(ds.getXAConnection());
            //return ds.getXAConnection().getConnection();
            return pds.getConnection();
        }                
    }                                                                     
}
