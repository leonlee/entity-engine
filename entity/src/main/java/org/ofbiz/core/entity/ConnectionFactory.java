/*
 * $Id: ConnectionFactory.java,v 1.1 2005/04/01 05:58:02 sfarquhar Exp $
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
package org.ofbiz.core.entity;

import java.sql.*;

import org.ofbiz.core.entity.config.*;
import org.ofbiz.core.entity.transaction.*;
import org.ofbiz.core.util.*;

/**
 * ConnectionFactory - central source for JDBC connections
 *
 * @author     <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 * @author     <a href="mailto:jaz@ofbiz.org">Andy Zeneski</a>
 * @version    $Revision: 1.1 $
 * @since      2.0
 */
public class ConnectionFactory {
    // Debug module name
    public static final String module = ConnectionFactory.class.getName();

    public static Connection getConnection(String helperName) throws SQLException, GenericEntityException {
        // Debug.logVerbose("Getting a connection", module);

        Connection con = TransactionFactory.getConnection(helperName);
        if (con == null) {
            Debug.logError("******* ERROR: No database connection found for helperName \"" + helperName + "\"", module);
        }
        return con;
    }
    
    public static Connection tryGenericConnectionSources(String helperName, JdbcDatasourceInfo jdbcDatasource) throws SQLException, GenericEntityException {
        // first try DBCP
        try {
            Connection con = DBCPConnectionFactory.getConnection(helperName, jdbcDatasource);
            if (con != null) return con;
        } catch (Exception ex) {
            Debug.logError(ex, "There was an error getting a DBCP datasource.");
        }
        
        // Default to plain JDBC.
        String driverClassName = jdbcDatasource.getDriverClassName();

        if (driverClassName != null && driverClassName.length() > 0) {
            try {
                ClassLoader loader = Thread.currentThread().getContextClassLoader();
                loader.loadClass(driverClassName);
            } catch (ClassNotFoundException cnfe) {
                Debug.logWarning("Could not find JDBC driver class named " + driverClassName + ".\n");
                Debug.logWarning(cnfe);
                return null;
            }
            return DriverManager.getConnection(jdbcDatasource.getUri(),
                    jdbcDatasource.getUsername(), jdbcDatasource.getPassword());
        }

        return null;
    }
}
