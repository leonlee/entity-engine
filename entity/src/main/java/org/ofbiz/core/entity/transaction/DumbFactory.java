/*
 * $Id: DumbFactory.java,v 1.1 2005/04/01 05:58:03 sfarquhar Exp $
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

import javax.transaction.*;
import java.sql.*;

import org.ofbiz.core.entity.*;
import org.ofbiz.core.entity.config.*;
import org.ofbiz.core.util.*;

/**
 * A dumb, non-working transaction manager.
 * 
 * @author     <a href="mailto:plightbo@hotmail.com">Pat Lightbody</a>
 * @author     <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 * @version    $Revision: 1.1 $
 * @since      2.0
 */
public class DumbFactory implements TransactionFactoryInterface {
    public TransactionManager getTransactionManager() {
        return new TransactionManager() {
            public void begin() throws NotSupportedException, SystemException {
            }

            public void commit() throws RollbackException, HeuristicMixedException, HeuristicRollbackException, SecurityException, IllegalStateException, SystemException {
            }

            public int getStatus() throws SystemException {
                return TransactionUtil.STATUS_NO_TRANSACTION;
            }

            public Transaction getTransaction() throws SystemException {
                return null;
            }

            public void resume(Transaction transaction) throws InvalidTransactionException, IllegalStateException, SystemException {
            }

            public void rollback() throws IllegalStateException, SecurityException, SystemException {
            }

            public void setRollbackOnly() throws IllegalStateException, SystemException {
            }

            public void setTransactionTimeout(int i) throws SystemException {
            }

            public Transaction suspend() throws SystemException {
                return null;
            }
        };
    }

    public UserTransaction getUserTransaction() {
        return new UserTransaction() {
            public void begin() throws NotSupportedException, SystemException {
            }

            public void commit() throws RollbackException, HeuristicMixedException, HeuristicRollbackException, SecurityException, IllegalStateException, SystemException {
            }

            public int getStatus() throws SystemException {
                return TransactionUtil.STATUS_NO_TRANSACTION;
            }

            public void rollback() throws IllegalStateException, SecurityException, SystemException {
            }

            public void setRollbackOnly() throws IllegalStateException, SystemException {
            }

            public void setTransactionTimeout(int i) throws SystemException {
            }
        };
    }
    
    public String getTxMgrName() {
        return "dumb";
    }
    
    public Connection getConnection(String helperName) throws SQLException, GenericEntityException {
        DatasourceInfo datasourceInfo = EntityConfigUtil.getInstance().getDatasourceInfo(helperName);

        if (datasourceInfo.getJdbcDatasource() != null) {
            Connection otherCon = ConnectionFactory.tryGenericConnectionSources(helperName, datasourceInfo.getJdbcDatasource());
            return otherCon;
        } else {
            Debug.logError("Dumb/Empty is the configured transaction manager but no inline-jdbc element was specified in the " + helperName + " datasource. Please check your configuration");
            return null;
        }
    }
}
