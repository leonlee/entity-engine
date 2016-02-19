/*
 * $Id: JotmFactory.java,v 1.1 2005/04/01 05:58:03 sfarquhar Exp $
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

import org.objectweb.jotm.Jotm;
import org.objectweb.transaction.jta.TMService;
import org.ofbiz.core.entity.ConnectionFactory;
import org.ofbiz.core.entity.GenericEntityException;
import org.ofbiz.core.entity.config.DatasourceInfo;
import org.ofbiz.core.entity.config.EntityConfigUtil;
import org.ofbiz.core.util.Debug;

import javax.naming.NamingException;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * JotmFactory - Central source for JOTM JTA objects
 *
 * @author <a href="mailto:jaz@ofbiz.org">Andy Zeneski</a>
 * @version $Revision: 1.1 $
 * @since 2.1
 */
public class JotmFactory implements TransactionFactoryInterface {

    public static final String module = JotmFactory.class.getName();

    private static TMService jotm;

    static {
        try {
            // creates an instance of JOTM with a local transaction factory which is not bound to a registry            
            jotm = new Jotm(true, false);
        } catch (NamingException ne) {
            Debug.logError(ne, "Problems creating JOTM instance", module);
        }
    }

    /*
     * @see org.ofbiz.core.entity.transaction.TransactionFactoryInterface#getTransactionManager()
     */
    public TransactionManager getTransactionManager() {
        if (jotm != null) {
            return jotm.getTransactionManager();
        } else {
            Debug.logError("Cannot get TransactionManager, JOTM object is null", module);
            return null;
        }
    }

    /*
     * @see org.ofbiz.core.entity.transaction.TransactionFactoryInterface#getUserTransaction()
     */
    public UserTransaction getUserTransaction() {
        if (jotm != null) {
            return jotm.getUserTransaction();
        } else {
            Debug.logError("Cannot get UserTransaction, JOTM object is null", module);
            return null;
        }
    }

    public String getTxMgrName() {
        return "jotm";
    }

    public Connection getConnection(String helperName) throws SQLException, GenericEntityException {
        DatasourceInfo datasourceInfo = EntityConfigUtil.getInstance().getDatasourceInfo(helperName);

        if (datasourceInfo.getJdbcDatasource() != null) {
            // Use JOTM (enhydra-jdbc.jar) connection pooling
            try {
                Connection con = JotmConnectionFactory.getConnection(helperName, datasourceInfo.getJdbcDatasource());
                if (con != null) return con;
            } catch (Exception ex) {
                Debug.logError(ex, "JOTM is the configured transaction manager but there was an error getting a database Connection through JOTM for the " + helperName + " datasource. Please check your configuration, class path, etc.");
            }

            Connection otherCon = ConnectionFactory.tryGenericConnectionSources(helperName, datasourceInfo.getJdbcDatasource());
            return otherCon;
        } else {
            Debug.logError("JOTM is the configured transaction manager but no inline-jdbc element was specified in the " + helperName + " datasource. Please check your configuration");
            return null;
        }
    }

    public void removeDatasource(final String helperName) {
        try {
            JotmConnectionFactory.removeDatasource(helperName);
        } catch (Exception ex) {
            Debug.logError(ex, "Error shutting down the JOTM " + helperName + " datasource. Please check your configuration, class path, etc.");
            // If no JOTM datasource was found, then the generic connection factory would have been used
            ConnectionFactory.removeDatasource(helperName);
        }
    }
}
