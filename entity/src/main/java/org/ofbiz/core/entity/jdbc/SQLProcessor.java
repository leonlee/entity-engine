/*
 * $Id: SQLProcessor.java,v 1.7 2005/09/28 09:55:24 amazkovoi Exp $
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
 */
package org.ofbiz.core.entity.jdbc;

import com.google.common.annotations.VisibleForTesting;
import org.ofbiz.core.entity.ConnectionFactory;
import org.ofbiz.core.entity.GenericDataSourceException;
import org.ofbiz.core.entity.GenericEntityException;
import org.ofbiz.core.entity.GenericTransactionException;
import org.ofbiz.core.entity.TransactionUtil;
import org.ofbiz.core.entity.jdbc.interceptors.SQLInterceptor;
import org.ofbiz.core.entity.jdbc.interceptors.connection.ConnectionWithSQLInterceptor;
import org.ofbiz.core.util.Debug;

import javax.annotation.concurrent.NotThreadSafe;
import javax.transaction.Status;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * SQLProcessor - provides utility functions to ease database access
 *
 * @author <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 * @version $Revision: 1.7 $
 * @since 2.0
 */
@NotThreadSafe  // You really ought to just assume this for everything in entity engine...
public class SQLProcessor {

    public enum CommitMode {
        /**
         * The SQLProcessor is in read only mode and only SELECT statements may be issued
         */
        READONLY,
        /**
         * The SQLProcessor is in "auto commit" mode, and will implicitly commit any UPDATES on execution
         */
        AUTO_COMMIT,
        /**
         * The SQLProcessor is in "explcit commit" mode, and will explcitly commit UPDATEs on close
         */
        EXPLICIT_COMMIT,
        /**
         * The SQLProcessor is in "no commit" mode, and will NOT get involved in commiting at all
         */
        NOT_INVOLVED,
        /**
         * The SQLProcessor is in "external commit" mode, and {@link org.ofbiz.core.entity.TransactionUtil} code is
         * being used to control the commits
         */
        EXTERNAL_COMMIT,
    }

    /**
     * Module Name Used for debugging
     */
    public static final String module = SQLProcessor.class.getName();

    /**
     * The datasource helper (see entityengine.xml <datasource name="..">)
     */
    private String helperName;

    // Finalization guard.  This is meant to help catch errors where the SQLProcessor is not correctly closed.
    private volatile ConnectionGuard _guard = null;

    // / The database resources to be used
    private Connection _connection = null;

    // / The database resources to be used
    private PreparedStatement _ps = null;

    // / The database resources to be used
    private ResultSet _rs = null;

    private int[] _batchResult;

    // / The SQL String used. Use for debugging only
    private String _sql;

    // / Index to be used with preparedStatement.setValue(_ind, ...)
    private int _ind;

    // / true in case of manual transactions
    private boolean _manualTX;

    private CommitMode _commitMode;

    // The interceptor to use
    private SQLInterceptor _sqlInterceptor;

    private List<String> _parameterValues;
    private List<List<String>> _parameterValuesForBatch;

    /**
     * Construct a SQLProcessor based on the helper/datasource and a specific {@link
     * org.ofbiz.core.entity.jdbc.SQLProcessor.CommitMode}
     *
     * @param helperName The datasource helper (see entityengine.xml &lt;datasource name=".."&gt;)
     * @param commitMode the {@link org.ofbiz.core.entity.jdbc.SQLProcessor.CommitMode} to use
     */
    SQLProcessor(String helperName, CommitMode commitMode) {
        this.helperName = helperName;
        this._manualTX = true;
        this._connection = null;
        this._commitMode = commitMode;
    }

    /**
     * Construct a SQLProcessor based on the helper/datasource.
     * <p>
     * It will be in {@link org.ofbiz.core.entity.jdbc.SQLProcessor.CommitMode#EXPLICIT_COMMIT} by default
     * </p>
     * <p>
     * This is the legacy mode of SQLProcessor as it was before the CommitMode support was added.  This constructor
     * has been left in place because other plugins may be using it and hence to preserve the API.
     * </p>
     *
     * @param helperName The datasource helper (see entityengine.xml &lt;datasource name=".."&gt;)
     */
    public SQLProcessor(String helperName) {
        this(helperName, CommitMode.EXPLICIT_COMMIT);
    }

    /**
     * Construct a SQLProcessor with an connection given. The connection will not be closed by this SQLProcessor, but
     * may be by some other.
     * <p>
     * It does not participate in any commiting not does it set the autoCommit mode of the connection
     * </p>
     *
     * @param helperName The datasource helper (see entityengine.xml &lt;datasource name=".."&gt;)
     * @param connection The connection to be used
     */
    public SQLProcessor(String helperName, Connection connection) {
        this(helperName, CommitMode.NOT_INVOLVED);
        this._connection = connection;

        // If the connection is provided to us, then commits are left up to the caller
        this._manualTX = (connection == null);
    }

    /**
     * @return the {@link CommitMode} the SQLProcessor is in
     */
    public CommitMode getCommitMode() {
        return _commitMode;
    }

    /**
     * Commit all modifications
     *
     * @throws GenericDataSourceException if an SQLException occurs
     */
    public void commit() throws GenericDataSourceException {
        if (_connection == null) {
            return;
        }

        if (_manualTX) {
            try {
                _connection.commit();
            } catch (SQLException sqle) {
                rollback();
                Debug.logWarning("[SQLProcessor.commit]: SQL Exception occurred on commit. Error was:" + sqle);
                throw new GenericDataSourceException("SQL Exception occurred on commit", sqle);
            }
        }
    }

    /**
     * Rollback all modifications
     *
     * @throws GenericDataSourceException if an SQLException occurs
     */
    public void rollback() throws GenericDataSourceException {
        if (_connection == null) {
            return;
        }

        try {
            if (_manualTX) {
                _connection.rollback();
            } else {
                try {
                    TransactionUtil.setRollbackOnly();
                } catch (GenericTransactionException e) {
                    Debug.logError(e, "Error setting rollback only");
                    throw new GenericDataSourceException("Error setting rollback only", e);
                }
            }
        } catch (SQLException sqle2) {
            Debug.logWarning("[SQLProcessor.rollback]: SQL Exception while rolling back insert. Error was:" + sqle2,
                    module);
            Debug.logWarning(sqle2, module);
        }
    }

    /**
     * Commit if required and remove all allocated resources
     *
     * @throws GenericDataSourceException if an SQLException occurs
     */
    public void close() throws GenericDataSourceException {
        try {
            smartCommit(_connection);
        } finally {
            // Hold onto _sql and _parameterValues so we can report if somebody tries to close us again...
            closeResultSet();
            closePreparedStatement();
            closeConnection();
        }
    }

    private void closeResultSet() {
        final ResultSet rs = _rs;
        if (rs == null) {
            return;
        }
        _rs = null;

        try {
            rs.close();
        } catch (SQLException sqle) {
            Debug.logWarning(sqle, "Error closing ResultSet", module);
        }
    }

    private void closePreparedStatement() {
        final PreparedStatement ps = _ps;
        if (ps == null) {
            return;
        }
        _ps = null;

        try {
            ps.close();
        } catch (SQLException sqle) {
            Debug.logWarning(sqle, "Error closing PreparedStatement", module);
        }
    }

    private void closeConnection() {
        // If we don't have a connection guard, then we didn't open the connection ourselves, so we shouldn't close it
        final ConnectionGuard guard = _guard;
        if (guard == null) {
            return;
        }

        // JDEV-35590: Ensure that guard gets cleared before we let go of the reference to the connection.
        guard.clear();

        final Connection connection = _connection;
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (SQLException sqle) {
            Debug.logWarning(sqle, "Error closing Connection", module);
        } finally {
            _guard = null;
            _connection = null;
        }
    }


    /**
     * Once upon a time, SQLProcessor used to commit the connection, regardless of what SQL was executed.  Now we do
     * this in a smarter way.
     *
     * @param connection the connection in play
     * @throws GenericDataSourceException if something goes wrong
     */
    private void smartCommit(final Connection connection) throws GenericDataSourceException {
        if (connection == null) {
            return;
        }
        if (_commitMode != CommitMode.EXPLICIT_COMMIT) {
            return;
        }
        commit();
    }

    /**
     * Get a connection from the ConnectionFactory
     *
     * @return The connection created
     * @throws GenericEntityException if an SQLException occurs
     */
    public Connection getConnection() throws GenericEntityException {
        if (_connection != null) {
            return _connection;
        }

        if (TransactionUtil.isTransactionActive()) {
            // Ensure that we do not do commits as we do not own the connection
            _manualTX = false;
            _commitMode = CommitMode.EXTERNAL_COMMIT;
            _connection = TransactionUtil.getLocalTransactionConnection();
            _guard = null;
            return _connection;
        }

        // Seems like a good time to purge any abandoned processors...
        ConnectionGuard.closeAbandonedProcessors();

        _manualTX = true;
        try {
            _connection = ConnectionFactory.getConnection(helperName);
            _guard = guard(_connection);
        } catch (SQLException sqle) {
            throw new GenericDataSourceException("Unable to establish a connection with the database.", sqle);
        }

        if (Debug.verboseOn()) {
            int isoLevel = -999;
            try {
                isoLevel = _connection.getTransactionIsolation();
            } catch (SQLException e) {
                Debug.logError(e, "Problems getting the connection's isolation level", module);
            }
            if (isoLevel == Connection.TRANSACTION_NONE) {
                Debug.logVerbose("Transaction isolation level set to 'None'.", module);
            } else if (isoLevel == Connection.TRANSACTION_READ_COMMITTED) {
                Debug.logVerbose("Transaction isolation level set to 'ReadCommited'.", module);
            } else if (isoLevel == Connection.TRANSACTION_READ_UNCOMMITTED) {
                Debug.logVerbose("Transaction isolation level set to 'ReadUncommitted'.", module);
            } else if (isoLevel == Connection.TRANSACTION_REPEATABLE_READ) {
                Debug.logVerbose("Transaction isolation level set to 'RepeatableRead'.", module);
            } else if (isoLevel == Connection.TRANSACTION_SERIALIZABLE) {
                Debug.logVerbose("Transaction isolation level set to 'Serializable'.", module);
            }
        }

        // we need to normalise the connection's autoCommit status
        smartSetAutoCommit(_connection);
        try {
            if (TransactionUtil.getStatus() == Status.STATUS_ACTIVE) {
                _manualTX = false;
            }
        } catch (GenericTransactionException e) {
            // nevermind, don't worry about it, but print the exc anyway
            Debug.logWarning("[SQLProcessor.getConnection]: Exception was thrown trying to check " +
                    "transaction status: " + e.toString(), module);
        }

        return _connection;
    }

    @VisibleForTesting
    ConnectionGuard guard(Connection connection) {
        return ConnectionGuard.register(this, connection);
    }

    /**
     * The old versions of OFBIZ used to set the autoCommit to FALSE on every interaction.  This COSTS a lot because
     * other code, such as Tomcats DBCP will try to set autoCommit back to TRUE on passivate.
     * <p>
     * Hence we get a flip-flop effect where OFBIZ sets it to false and then DBCP sets it to true and the DB layer runs
     * hot following contradicting orders.
     * <p>
     * So we need to be a bit smarter in how we do that
     *
     * @param connection the Connection in play
     */
    private void smartSetAutoCommit(final Connection connection) {
        // if we are in read only mode, then there is no need to perform tweak autoCommit at all!
        if (_commitMode == CommitMode.READONLY) {
            return;
        }

        if (_commitMode == CommitMode.AUTO_COMMIT) {
            doSetAutoCommit(connection, true);
        } else {
            doSetAutoCommit(connection, false);
        }
    }

    /**
     * This will set the connections autoCommit to the provided value ONLY if its doesnt already have that value.
     *
     * @param connection the JDBC connection
     * @param autoCommit the desired autoCommit state
     */
    private void doSetAutoCommit(final Connection connection, final boolean autoCommit) {
        // always try to set auto commit to false, but if we can't then later on we won't commit
        try {
            if (connection.getAutoCommit() != autoCommit) {
                connection.setAutoCommit(autoCommit);
            }
        } catch (SQLException sqle) {
            _manualTX = false;
            _commitMode = CommitMode.NOT_INVOLVED;
        }
    }


    /**
     * Prepare a statement. In case no connection has been given, allocate a new one.
     *
     * @param sql The SQL statement to be executed
     * @throws GenericEntityException if an SQLException occurs
     */
    public void prepareStatement(String sql) throws GenericEntityException {
        this.prepareStatement(sql, false, 0, 0);
    }

    /**
     * Prepare a statement. In case no connection has been given, allocate a new one.
     *
     * @param sql                  The SQL statement to be executed
     * @param specifyTypeAndConcur true if the result set Type and Concurrency has been specified
     * @param resultSetType        a result set type; one of <code>ResultSet.TYPE_FORWARD_ONLY</code>,
     *                             <code>ResultSet.TYPE_SCROLL_INSENSITIVE</code>, or <code>ResultSet.TYPE_SCROLL_SENSITIVE</code>
     * @param resultSetConcurrency a concurrency type; one of <code>ResultSet.CONCUR_READ_ONLY</code> or
     *                             <code>ResultSet.CONCUR_UPDATABLE</code>
     * @throws GenericEntityException if an SQLException occurs
     */
    public void prepareStatement(final String sql, final boolean specifyTypeAndConcur, final int resultSetType,
                                 final int resultSetConcurrency) throws GenericEntityException {
        if (Debug.verboseOn()) {
            Debug.logVerbose("[SQLProcessor.prepareStatement] sql=" + sql, module);
        }

        final Connection connection = getConnection();
        final ConnectionGuard guard = _guard;
        try {
            // If the guard is null, then the connection we are using belongs to somebody else, so how it gets
            // cleaned up is not our problem.  However, if we allocated the connection and then this SQLProcessor
            // gets abandoned to GC without closing it, then we want to be able to report the last SQL that was
            // requested on the connection, as knowing the kind of work the connection was used for may help us
            // track down the code that leaked it.
            if (guard != null) {
                guard.setSql(sql);
            }

            _sql = sql;
            _parameterValues = new ArrayList<>();
            _parameterValuesForBatch = new ArrayList<>();
            _ind = 1;
            if (specifyTypeAndConcur) {
                _ps = connection.prepareStatement(sql, resultSetType, resultSetConcurrency);
            } else {
                _ps = connection.prepareStatement(sql);
            }
        } catch (SQLException sqle) {
            throw new GenericDataSourceException("SQL Exception while executing the following:" + sql, sqle);
        }
    }

    /**
     * This will allocate a new SQL Interceptor for each interaction.  While ideally we would do this as a final in the
     * constructor, SQLProcessor is not design as a one time use object and hence we cant really know the calling
     * lifecycle.  So to be safe we do it for every call to beforeExecution() and protect it in each of the following
     * helpers with a null check.
     */
    private void beforeExecution() {
        if (_connection instanceof ConnectionWithSQLInterceptor) {
            _sqlInterceptor = ((ConnectionWithSQLInterceptor) _connection).getNonNullSQLInterceptor();
        } else {
            _sqlInterceptor = SQLInterceptorSupport.getNonNullSQLInterceptor(helperName);
        }
        _sqlInterceptor.beforeExecution(_sql, _parameterValues, _ps);
    }

    private void afterExecution(int rowsUpdated) {
        if (_sqlInterceptor != null) {
            _sqlInterceptor.afterSuccessfulExecution(_sql, _parameterValues, _ps, null, rowsUpdated);
            _sqlInterceptor = null;
        }
    }

    private void afterExecution() {
        if (_sqlInterceptor != null) {
            _sqlInterceptor.afterSuccessfulExecution(_sql, _parameterValues, _ps, _rs, -1);
            _sqlInterceptor = null;
        }
    }

    private void onException(final SQLException sqle) {
        if (_sqlInterceptor != null) {
            _sqlInterceptor.onException(_sql, _parameterValues, _ps, sqle);
            _sqlInterceptor = null;
        }
    }

    /**
     * Execute a query based on the prepared statement
     *
     * @return The result set of the query
     * @throws GenericDataSourceException if an SQLException occurs
     */
    public ResultSet executeQuery() throws GenericDataSourceException {
        try {
            beforeExecution();

            _rs = _ps.executeQuery();

            afterExecution();
        } catch (SQLException sqle) {
            onException(sqle);

            throw new GenericDataSourceException("SQL Exception while executing the following:" + _sql, sqle);
        }
        return _rs;
    }

    /**
     * Execute a query based on the SQL string given
     *
     * @param sql The SQL string to be executed
     * @return The result set of the query
     * @throws GenericDataSourceException if an SQLException occurs
     */
    public ResultSet executeQuery(String sql) throws GenericEntityException {
        prepareStatement(sql);
        return executeQuery();
    }

    /**
     * Execute updates
     *
     * @return The number of rows updated
     * @throws GenericDataSourceException if an SQLException occurs
     */
    public int executeUpdate() throws GenericDataSourceException {
        validateCommitMode();

        try {
            beforeExecution();

            int rc = _ps.executeUpdate();

            afterExecution(rc);

            return rc;
        } catch (SQLException sqle) {
            onException(sqle);

            throw new GenericDataSourceException("SQL Exception while executing the following:" + _sql, sqle);
        }
    }

    /**
     * Execute update based on the SQL statement given
     *
     * @param sql SQL statement to be executed
     * @return either (1) the row count for SQL Data Manipulation Language (DML) statements
     * @throws GenericDataSourceException if an SQLException occurs or (2) 0 for SQL statements that return nothing
     */
    public int executeUpdate(String sql) throws GenericDataSourceException {
        validateCommitMode();

        SQLInterceptor sqlInterceptor = SQLInterceptorSupport.getNonNullSQLInterceptor(helperName);
        List<String> emptyList = Collections.emptyList();

        Statement stmt = null;
        try {
            // Note: NPE if no one has called getConnection() yet!  This is inconsistent with the prepareStatement(),
            // which will go ahead and allocate a new connection for you.
            stmt = _connection.createStatement();

            // If there is a connection guard, record the SQL we are executing for debugging purposes if the
            // connection gets leaked.
            final ConnectionGuard guard = _guard;
            if (guard != null) {
                guard.setSql(sql);
            }

            sqlInterceptor.beforeExecution(sql, emptyList, stmt);

            int rc = stmt.executeUpdate(sql);

            sqlInterceptor.afterSuccessfulExecution(sql, emptyList, stmt, null, rc);
            return rc;
        } catch (SQLException sqle) {
            sqlInterceptor.onException(sql, emptyList, stmt, sqle);
            throw new GenericDataSourceException("SQL Exception while executing the following:" + sql, sqle);
        } finally {
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException sqle) {
                    Debug.logWarning("Unable to close 'statement': " + sqle.getMessage(), module);
                }
            }
        }
    }

    /**
     * Execute a batch query based on the prepared statement
     *
     * @return an array of update counts containing one element for each command in the batch. The elements of the array are ordered according to the order in which commands were added to the batch.
     * @throws GenericDataSourceException if an SQLException occurs
     * @see PreparedStatement#executeBatch()
     * @see PreparedStatement#addBatch()
     */
    public int[] executeBatch() throws GenericDataSourceException {
        try {
            //todo probably I need some batched version
            beforeExecution();

            _batchResult = _ps.executeBatch();

            //todo probably I need some batched version
            afterExecution();
        } catch (SQLException sqle) {
            onException(sqle);

            throw new GenericDataSourceException("SQL Exception while executing the following (batch mode):" + _sql, sqle);
        }
        return _batchResult;
    }

    private void validateCommitMode() {
        if (_commitMode == CommitMode.READONLY) {
            throw new IllegalStateException(
                    "The current CommitMode is READ ONLY and you are trying to perform an UPDATE");
        }
    }

    /**
     * Test if there more records available
     *
     * @return true, if there more records available
     * @throws GenericDataSourceException if an SQLException occurs
     */
    public boolean next() throws GenericDataSourceException {
        try {
            return _rs.next();
        } catch (SQLException sqle) {
            throw new GenericDataSourceException("SQL Exception while executing the following:" + _sql, sqle);
        }
    }

    /**
     * Getter: get the currently activ ResultSet
     *
     * @return ResultSet
     */
    public ResultSet getResultSet() {
        return _rs;
    }

    /**
     * Getter: get the prepared statement
     *
     * @return PreparedStatement
     */
    public PreparedStatement getPreparedStatement() {
        return _ps;
    }

    /**
     * Execute a query based on the SQL string given. For each record of the ResultSet return, execute a callback
     * function
     *
     * @param sql       The SQL string to be executed
     * @param aListener The callback function object
     * @throws GenericEntityException if an SQLException occurs
     */
    public void execQuery(String sql, ExecQueryCallbackFunctionIF aListener) throws GenericEntityException {
        if (_connection == null) {
            getConnection();
        }

        try {
            if (Debug.verboseOn()) {
                Debug.logVerbose("[SQLProcessor.execQuery]: " + sql, module);
            }
            executeQuery(sql);

            // process the results by calling the listener for
            // each row...
            boolean keepGoing = true;

            while (keepGoing && _rs.next()) {
                keepGoing = aListener.processNextRow(_rs);
            }

            if (_manualTX) {
                _connection.commit();
            }

        } catch (SQLException sqle) {
            Debug.logWarning("[SQLProcessor.execQuery]: SQL Exception while executing the following:\n" +
                    sql + "\nError was:", module);
            Debug.logWarning(sqle.getMessage(), module);
            throw new GenericEntityException("SQL Exception while executing the following:" + _sql, sqle);
        } finally {
            close();
        }
    }

    private void recordParameter(final Object field) {
        _parameterValues.add(String.valueOf(field));
    }

    /**
     * Set the next binding variable of the currently active prepared statement.
     *
     * @param field the field value in play
     * @throws SQLException if somethings goes wrong
     */
    public void setValue(String field) throws SQLException {
        if (field != null) {
            _ps.setString(_ind, field);
        } else {
            _ps.setNull(_ind, Types.VARCHAR);
        }
        recordParameter(field);

        _ind++;
    }

    /**
     * Set the next binding variable of the currently active prepared statement.
     *
     * @param field the field value in play
     * @throws SQLException if somethings goes wrong
     */
    public void setValue(java.sql.Timestamp field) throws SQLException {
        if (field != null) {
            _ps.setTimestamp(_ind, field);
        } else {
            _ps.setNull(_ind, Types.TIMESTAMP);
        }
        recordParameter(field);

        _ind++;
    }

    /**
     * Set the next binding variable of the currently active prepared statement.
     *
     * @param field the field value in play
     * @throws SQLException if somethings goes wrong
     */
    public void setValue(java.sql.Time field) throws SQLException {
        if (field != null) {
            _ps.setTime(_ind, field);
        } else {
            _ps.setNull(_ind, Types.TIME);
        }
        recordParameter(field);

        _ind++;
    }

    /**
     * Set the next binding variable of the currently active prepared statement.
     *
     * @param field the field value in play
     * @throws SQLException if somethings goes wrong
     */
    public void setValue(java.sql.Date field) throws SQLException {
        if (field != null) {
            _ps.setDate(_ind, field);
        } else {
            _ps.setNull(_ind, Types.DATE);
        }
        recordParameter(field);

        _ind++;
    }

    /**
     * Set the next binding variable of the currently active prepared statement.
     *
     * @param field the field value in play
     * @throws SQLException if somethings goes wrong
     */
    public void setValue(Integer field) throws SQLException {
        if (field != null) {
            _ps.setInt(_ind, field);
        } else {
            _ps.setNull(_ind, Types.NUMERIC);
        }
        recordParameter(field);

        _ind++;
    }

    /**
     * Set the next binding variable of the currently active prepared statement.
     *
     * @param field the field value in play
     * @throws SQLException if somethings goes wrong
     */
    public void setValue(Long field) throws SQLException {
        if (field != null) {
            _ps.setLong(_ind, field);
        } else {
            _ps.setNull(_ind, Types.NUMERIC);
        }
        recordParameter(field);

        _ind++;
    }

    /**
     * Set the next binding variable of the currently active prepared statement.
     *
     * @param field the field value in play
     * @throws SQLException if somethings goes wrong
     */
    public void setValue(Float field) throws SQLException {
        if (field != null) {
            _ps.setFloat(_ind, field);
        } else {
            _ps.setNull(_ind, Types.NUMERIC);
        }
        recordParameter(field);

        _ind++;
    }

    /**
     * Set the next binding variable of the currently active prepared statement.
     *
     * @param field the field value in play
     * @throws SQLException if somethings goes wrong
     */
    public void setValue(Double field) throws SQLException {
        if (field != null) {
            _ps.setDouble(_ind, field);
        } else {
            _ps.setNull(_ind, Types.NUMERIC);
        }
        recordParameter(field);

        _ind++;
    }

    /**
     * Set the next binding variable of the currently active prepared statement.
     *
     * @param field the field value in play
     * @throws SQLException if somethings goes wrong
     */
    public void setValue(Boolean field) throws SQLException {
        if (field != null) {
            _ps.setBoolean(_ind, field);
        } else {
            _ps.setNull(_ind, Types.NULL);
        }
        recordParameter(field);

        _ind++;
    }

    /**
     * Set the next binding variable of the currently active prepared statement.
     *
     * @param field the field value in play
     * @throws SQLException if somethings goes wrong
     */
    public void setValue(Object field) throws SQLException {
        if (field != null) {
            _ps.setObject(_ind, field, Types.JAVA_OBJECT);
        } else {
            _ps.setNull(_ind, Types.JAVA_OBJECT);
        }
        recordParameter(field);

        _ind++;
    }

    /**
     * Set the next binding variable of the currently active prepared statement
     *
     * @param field the field value in play
     * @throws SQLException if somethings goes wrong
     */
    public void setValue(Blob field) throws SQLException {
        if (field != null) {
            _ps.setBlob(_ind, field);
        } else {
            _ps.setNull(_ind, Types.BLOB);
        }
        recordParameter("BLOB");

        _ind++;
    }

    /**
     * Set the next binding variable of the currently active prepared statement
     *
     * @param field the field value in play
     * @throws SQLException if somethings goes wrong
     */
    public void setValue(Clob field) throws SQLException {
        if (field != null) {
            _ps.setClob(_ind, field);
        } else {
            _ps.setNull(_ind, Types.CLOB);
        }
        recordParameter("CLOB");

        _ind++;
    }

    /**
     * Set the next binding variable of the currently active prepared statement.
     * Note that this *must* actually be a {@code BLOB} field, not a PostgreSQL {@code BYTEA}, a
     * SQLServer {@code IMAGE}, or an HSQL {@code OTHER} field.  Use {@link #setByteArray(byte[])}
     * to set those!
     *
     * @param field the field value
     * @throws SQLException if something goes wrong
     */
    public void setBlob(byte[] field) throws SQLException {
        if (field != null) {
            _ps.setBinaryStream(_ind, new ByteArrayInputStream(field));
        } else {
            _ps.setNull(_ind, Types.BLOB);
        }

        recordParameter("BLOB");
        _ind++;
    }

    /**
     * Set the next binding variable of the currently active prepared statement.
     * Note that this *must* actually be a PostgreSQL {@code BYTEA}, a SQLServer {@code IMAGE},
     * or an HSQL {@code OTHER} field.  Use {@link #setBlob(byte[])} to set {@code BLOB}s!
     *
     * @param field the field value
     * @throws SQLException if something goes wrong
     */
    public void setByteArray(byte[] field) throws SQLException {
        if (field != null) {
            _ps.setBytes(_ind, field);
        } else {
            _ps.setNull(_ind, Types.LONGVARBINARY);
        }

        recordParameter("byte[]");
        _ind++;
    }

    /**
     * Set the next binding variable of the currently active prepared statement to write the serialized data of 'field'
     * to a BLOB that is stored as an OID SQL type.
     *
     * @param field the field value in play
     * @throws SQLException if somethings goes wrong
     */
    public void setBinaryStream(Object field) throws SQLException {
        if (field != null) {
            try {
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(os);
                oos.writeObject(field);
                oos.close();

                byte[] buf = os.toByteArray();
                os.close();
                ByteArrayInputStream is = new ByteArrayInputStream(buf);
                _ps.setBinaryStream(_ind, is, buf.length);
                is.close();
            } catch (IOException ex) {
                throw new SQLException(ex);
            }
        } else {
            _ps.setNull(_ind, Types.BLOB);
        }
        recordParameter("BLOB");

        _ind++;
    }

    public void addBatch() throws SQLException {
        _ps.addBatch();
        _parameterValuesForBatch.add(_parameterValues);
        _parameterValues = new ArrayList<>();
        _ind = 1;
    }

    /**
     * Set the next binding variable of the currently active prepared statement to write the serialized data of 'field'
     * to a BLOB that is stored as a Byte Array SQL type.
     * <p>
     * This method is specifically added to support PostgreSQL BYTEA and SQLServer IMAGE datatypes.
     *
     * @param field the field value in play
     * @throws SQLException if somethings goes wrong
     */
    public void setByteArrayData(Object field) throws SQLException {
        if (field != null) {
            try {
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(os);
                oos.writeObject(field);
                oos.close();
                _ps.setBytes(_ind, os.toByteArray());
            } catch (IOException ex) {
                throw new SQLException(ex);
            }
        } else {
            _ps.setNull(_ind, Types.LONGVARBINARY);
        }
        recordParameter("BLOB");

        _ind++;
    }


    @Override
    public String toString() {
        return "SQLProcessor[commitMode=" + _commitMode + ",connection=" + _connection + ",sql=" + _sql +
                ",parameters=" + _parameterValues + ']';
    }

}
