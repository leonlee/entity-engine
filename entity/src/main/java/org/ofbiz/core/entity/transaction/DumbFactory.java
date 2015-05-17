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

import java.sql.Connection;
import java.sql.SQLException;

import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;

import org.ofbiz.core.entity.ConnectionFactory;
import org.ofbiz.core.entity.GenericEntityException;
import org.ofbiz.core.entity.config.DatasourceInfo;
import org.ofbiz.core.entity.config.EntityConfigUtil;
import org.ofbiz.core.util.Debug;

import static javax.transaction.Status.STATUS_NO_TRANSACTION;

/**
 * A dumb, non-working transaction manager.
 * 
 * @author     <a href="mailto:plightbo@hotmail.com">Pat Lightbody</a>
 * @author     <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 * @version    $Revision: 1.1 $
 * @since      2.0
 */
public class DumbFactory implements TransactionFactoryInterface
{
    public TransactionManager getTransactionManager()
    {
        return DumbTransactionManager.INSTANCE;
    }

    public UserTransaction getUserTransaction()
    {
        return DumbUserTransaction.INSTANCE;
    }
    
    public String getTxMgrName()
    {
        return "dumb";
    }
    
    public Connection getConnection(String helperName) throws SQLException, GenericEntityException
    {
        final DatasourceInfo datasourceInfo = EntityConfigUtil.getInstance().getDatasourceInfo(helperName);
        if (datasourceInfo.getJdbcDatasource() == null) {
            Debug.logError("Dumb/Empty is the configured transaction manager but no inline-jdbc element was specified in the "
                    + helperName + " datasource. Please check your configuration");
            return null;
        }

        return ConnectionFactory.tryGenericConnectionSources(helperName, datasourceInfo.getJdbcDatasource());
    }

    public void removeDatasource(final String helperName)
    {
        ConnectionFactory.removeDatasource(helperName);
    }

    static class DumbTransactionManager implements TransactionManager
    {
        static final DumbTransactionManager INSTANCE = new DumbTransactionManager();

        public void begin() {}
        public void commit() {}

        public int getStatus()
        {
            return STATUS_NO_TRANSACTION;
        }

        public Transaction getTransaction()
        {
            return null;
        }

        public void resume(Transaction transaction) {}

        public void rollback() {}

        public void setRollbackOnly() {}

        public void setTransactionTimeout(int i) {}

        public Transaction suspend()
        {
            return null;
        }
    }

    static class DumbUserTransaction implements UserTransaction
    {
        static final DumbUserTransaction INSTANCE = new DumbUserTransaction();

        public void begin() {}

        public void commit() {}

        public int getStatus()
        {
            return STATUS_NO_TRANSACTION;
        }

        public void rollback() {}

        public void setRollbackOnly() {}

        public void setTransactionTimeout(int i) {}
    }
}
