/*
 * $Id: TyrexConnectionFactory.java,v 1.1 2005/04/01 05:58:03 sfarquhar Exp $
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

import com.atlassian.util.concurrent.CopyOnWriteMap;
import org.ofbiz.core.entity.GenericEntityException;
import org.ofbiz.core.entity.TransactionUtil;
import org.ofbiz.core.entity.config.JdbcDatasourceInfo;
import org.ofbiz.core.entity.jdbc.interceptors.connection.ConnectionTracker;
import org.ofbiz.core.util.Debug;
import tyrex.resource.jdbc.xa.EnabledDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.Callable;

// For Tyrex 0.9.8.5

// For Tyrex 0.9.7.0
// import tyrex.jdbc.xa.*;

/**
 * Tyrex ConnectionFactory - central source for JDBC connections from Tyrex
 *
 * @author <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 * @version $Revision: 1.1 $
 * @since 2.0
 */
public class TyrexConnectionFactory {

    protected static Map<String, EnabledDataSource> dsCache = CopyOnWriteMap.newHashMap();
    protected static Map<String, ConnectionTracker> trackerCache = CopyOnWriteMap.newHashMap();

    public static Connection getConnection(String helperName, JdbcDatasourceInfo jdbcDatasource)
            throws SQLException, GenericEntityException {
        EnabledDataSource ds;

        // try once
        ds = dsCache.get(helperName);
        if (ds != null) {
            return trackConnection(helperName, ds);
        }

        synchronized (TyrexConnectionFactory.class) {
            // try again inside the synch just in case someone when through while we were waiting
            ds = dsCache.get(helperName);
            if (ds != null) {
                return trackConnection(helperName, ds);
            }

            ds = new EnabledDataSource();
            ds.setDriverClassName(jdbcDatasource.getDriverClassName());
            ds.setDriverName(jdbcDatasource.getUri());
            ds.setUser(jdbcDatasource.getUsername());
            ds.setPassword(jdbcDatasource.getPassword());
            ds.setDescription(helperName);

            String transIso = jdbcDatasource.getIsolationLevel();

            if (transIso != null && transIso.length() > 0) {
                ds.setIsolationLevel(transIso);
            }

            ds.setLogWriter(Debug.getPrintWriter());

            dsCache.put(helperName, ds);
            trackerCache.put(helperName, new ConnectionTracker());

            return trackConnection(helperName, ds);
        }

    }

    private static Connection trackConnection(final String helperName, final EnabledDataSource ds) {
        ConnectionTracker connectionTracker = trackerCache.get(helperName);
        return connectionTracker.trackConnection(helperName, new Callable<Connection>() {
            public Connection call() throws Exception {
                return TransactionUtil.enlistConnection(ds.getXAConnection());
            }
        });
    }

}

