package org.ofbiz.core.entity.jdbc;

import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Struct;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;

/**
 */
public class MockConnection implements Connection {
    private boolean closeCalled = false;


    //
    // wrapped methods
    //

    public Statement createStatement() throws SQLException {
        throw new UnsupportedOperationException("Not implemented");
    }

    public PreparedStatement prepareStatement(final String sql) throws SQLException {
        throw new UnsupportedOperationException("Not implemented");
    }

    public CallableStatement prepareCall(final String sql) throws SQLException {
        throw new UnsupportedOperationException("Not implemented");
    }

    public String nativeSQL(final String sql) throws SQLException {
        throw new UnsupportedOperationException("Not implemented");
    }

    public void setAutoCommit(final boolean autoCommit) throws SQLException {
        throw new UnsupportedOperationException("Not implemented");
    }

    public boolean getAutoCommit() throws SQLException {
        throw new UnsupportedOperationException("Not implemented");
    }

    public void commit() throws SQLException {
        throw new UnsupportedOperationException("Not implemented");
    }

    public void rollback() throws SQLException {
        throw new UnsupportedOperationException("Not implemented");
    }

    public void close() throws SQLException {
        this.closeCalled = true;
    }

    public boolean isClosed() throws SQLException {
        return closeCalled;
    }

    public DatabaseMetaData getMetaData() throws SQLException {
        throw new UnsupportedOperationException("Not implemented");
    }

    public void setReadOnly(final boolean readOnly) throws SQLException {
        throw new UnsupportedOperationException("Not implemented");
    }

    public boolean isReadOnly() throws SQLException {
        throw new UnsupportedOperationException("Not implemented");
    }

    public void setCatalog(final String catalog) throws SQLException {
        throw new UnsupportedOperationException("Not implemented");
    }

    public String getCatalog() throws SQLException {
        throw new UnsupportedOperationException("Not implemented");
    }

    public void setTransactionIsolation(final int level) throws SQLException {
        throw new UnsupportedOperationException("Not implemented");
    }

    public int getTransactionIsolation() throws SQLException {
        throw new UnsupportedOperationException("Not implemented");
    }

    public SQLWarning getWarnings() throws SQLException {
        throw new UnsupportedOperationException("Not implemented");
    }

    public void clearWarnings() throws SQLException {
        throw new UnsupportedOperationException("Not implemented");
    }

    public Statement createStatement(final int resultSetType, final int resultSetConcurrency) throws SQLException {
        throw new UnsupportedOperationException("Not implemented");
    }

    public PreparedStatement prepareStatement(final String sql, final int resultSetType, final int resultSetConcurrency)
            throws SQLException {
        throw new UnsupportedOperationException("Not implemented");
    }

    public CallableStatement prepareCall(final String sql, final int resultSetType, final int resultSetConcurrency)
            throws SQLException {
        throw new UnsupportedOperationException("Not implemented");
    }

    public Map<String, Class<?>> getTypeMap() throws SQLException {
        throw new UnsupportedOperationException("Not implemented");
    }

    public void setTypeMap(final Map<String, Class<?>> map) throws SQLException {
        throw new UnsupportedOperationException("Not implemented");
    }

    public void setHoldability(final int holdability) throws SQLException {
        throw new UnsupportedOperationException("Not implemented");
    }

    public int getHoldability() throws SQLException {
        throw new UnsupportedOperationException("Not implemented");
    }

    public Savepoint setSavepoint() throws SQLException {
        throw new UnsupportedOperationException("Not implemented");
    }

    public Savepoint setSavepoint(final String name) throws SQLException {
        throw new UnsupportedOperationException("Not implemented");
    }

    public void rollback(final Savepoint savepoint) throws SQLException {
        throw new UnsupportedOperationException("Not implemented");
    }

    public void releaseSavepoint(final Savepoint savepoint) throws SQLException {
        throw new UnsupportedOperationException("Not implemented");
    }

    public Statement createStatement(final int resultSetType, final int resultSetConcurrency, final int resultSetHoldability)
            throws SQLException {
        throw new UnsupportedOperationException("Not implemented");
    }

    public PreparedStatement prepareStatement(final String sql, final int resultSetType, final int resultSetConcurrency, final int resultSetHoldability)
            throws SQLException {
        throw new UnsupportedOperationException("Not implemented");
    }

    public CallableStatement prepareCall(final String sql, final int resultSetType, final int resultSetConcurrency, final int resultSetHoldability)
            throws SQLException {
        throw new UnsupportedOperationException("Not implemented");
    }

    public PreparedStatement prepareStatement(final String sql, final int autoGeneratedKeys) throws SQLException {
        throw new UnsupportedOperationException("Not implemented");
    }

    public PreparedStatement prepareStatement(final String sql, final int[] columnIndexes) throws SQLException {
        throw new UnsupportedOperationException("Not implemented");
    }

    public PreparedStatement prepareStatement(final String sql, final String[] columnNames) throws SQLException {
        throw new UnsupportedOperationException("Not implemented");
    }

    public Clob createClob() throws SQLException {
        throw new UnsupportedOperationException("Not implemented");
    }

    public Blob createBlob() throws SQLException {
        throw new UnsupportedOperationException("Not implemented");
    }

    public NClob createNClob() throws SQLException {
        throw new UnsupportedOperationException("Not implemented");
    }

    public SQLXML createSQLXML() throws SQLException {
        throw new UnsupportedOperationException("Not implemented");
    }

    public boolean isValid(final int timeout) throws SQLException {
        throw new UnsupportedOperationException("Not implemented");
    }

    public void setClientInfo(final String name, final String value) throws SQLClientInfoException {
        throw new UnsupportedOperationException("Not implemented");
    }

    public void setClientInfo(final Properties properties) throws SQLClientInfoException {
        throw new UnsupportedOperationException("Not implemented");
    }

    public String getClientInfo(final String name) throws SQLException {
        throw new UnsupportedOperationException("Not implemented");
    }

    public Properties getClientInfo() throws SQLException {
        throw new UnsupportedOperationException("Not implemented");
    }

    public Array createArrayOf(final String typeName, final Object[] elements) throws SQLException {
        throw new UnsupportedOperationException("Not implemented");
    }

    public Struct createStruct(final String typeName, final Object[] attributes) throws SQLException {
        throw new UnsupportedOperationException("Not implemented");
    }

    public void setSchema(final String schema) throws SQLException {
        throw new UnsupportedOperationException("Not implemented");
    }

    public String getSchema() throws SQLException {
        throw new UnsupportedOperationException("Not implemented");
    }

    public void abort(final Executor executor) throws SQLException {
        throw new UnsupportedOperationException("Not implemented");
    }

    public void setNetworkTimeout(final Executor executor, final int milliseconds) throws SQLException {
        throw new UnsupportedOperationException("Not implemented");
    }

    public int getNetworkTimeout() throws SQLException {
        throw new UnsupportedOperationException("Not implemented");
    }

    public <T> T unwrap(final Class<T> iface) throws SQLException {
        throw new UnsupportedOperationException("Not implemented");
    }

    public boolean isWrapperFor(final Class<?> iface) throws SQLException {
        throw new UnsupportedOperationException("Not implemented");
    }
}
