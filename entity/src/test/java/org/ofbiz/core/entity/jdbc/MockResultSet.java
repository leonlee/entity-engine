package org.ofbiz.core.entity.jdbc;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.NClob;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

/**
 * Map backed result set that attempts to be very permissive with types.
 */
public class MockResultSet implements ResultSet {

    private final List<Map<String, Object>> data = new ArrayList<Map<String, Object>>();
    private int index = -1;
    private Statement statement;

    public MockResultSet() {
    }

    public MockResultSet(final Statement statement) {
        this.statement = statement;
    }

    public void addRow(Map<String, Object> row) {
        data.add(row);
    }

    public boolean absolute(final int row) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public boolean next() throws SQLException {
        return ++index < data.size();
    }

    public void close() throws SQLException {
    }

    public boolean wasNull() throws SQLException {
        return false;
    }

    public String getString(final int columnIndex) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public boolean getBoolean(final int columnIndex) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public byte getByte(final int columnIndex) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public short getShort(final int columnIndex) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public int getInt(final int columnIndex) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public long getLong(final int columnIndex) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public float getFloat(final int columnIndex) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public double getDouble(final int columnIndex) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public BigDecimal getBigDecimal(final int columnIndex, final int scale) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public byte[] getBytes(final int columnIndex) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public Date getDate(final int columnIndex) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public Time getTime(final int columnIndex) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public Timestamp getTimestamp(final int columnIndex) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public InputStream getAsciiStream(final int columnIndex) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public InputStream getUnicodeStream(final int columnIndex) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public InputStream getBinaryStream(final int columnIndex) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public String getString(final String columnName) throws SQLException {
        return data.get(index).get(columnName).toString();
    }

    public boolean getBoolean(final String columnName) throws SQLException {
        final Object o = getObject(columnName);
        if (o instanceof Boolean) {
            return (Boolean) o;
        }
        throw new UnsupportedOperationException();
    }

    public byte getByte(final String columnName) throws SQLException {
        final Object o = getObject(columnName);
        if (o instanceof Byte) {
            return (Byte) o;
        }
        throw new UnsupportedOperationException();
    }

    public short getShort(final String columnName) throws SQLException {
        final Object o = getObject(columnName);
        if (o instanceof Short) {
            return (Short) o;
        }
        throw new UnsupportedOperationException();
    }

    public int getInt(final String columnName) throws SQLException {
        final Object o = getObject(columnName);
        if (o instanceof Integer) {
            return (Integer) o;
        }
        throw new UnsupportedOperationException();
    }

    public long getLong(final String columnName) throws SQLException {
        final Object o = getObject(columnName);
        if (o instanceof Long) {
            return (Long) o;
        }
        throw new UnsupportedOperationException();
    }

    public float getFloat(final String columnName) throws SQLException {
        final Object o = getObject(columnName);
        if (o instanceof Float) {
            return (Float) o;
        }
        throw new UnsupportedOperationException();
    }

    public double getDouble(final String columnName) throws SQLException {
        final Object o = getObject(columnName);
        if (o instanceof Double) {
            return (Double) o;
        }
        throw new UnsupportedOperationException();
    }

    public BigDecimal getBigDecimal(final String columnName, final int scale) throws SQLException {
        final Object o = getObject(columnName);
        if (o instanceof BigDecimal) {
            return (BigDecimal) o;
        }
        throw new UnsupportedOperationException();
    }

    public byte[] getBytes(final String columnName) throws SQLException {
        final Object o = getObject(columnName);
        if (o instanceof byte[]) {
            return (byte[]) o;
        }
        throw new UnsupportedOperationException();
    }

    public Date getDate(final String columnName) throws SQLException {
        final Object o = getObject(columnName);
        if (o instanceof Date) {
            return (Date) o;
        }
        throw new UnsupportedOperationException();
    }

    public Time getTime(final String columnName) throws SQLException {
        final Object o = getObject(columnName);
        if (o instanceof Time) {
            return (Time) o;
        }
        throw new UnsupportedOperationException();
    }

    public Timestamp getTimestamp(final String columnName) throws SQLException {
        final Object o = getObject(columnName);
        if (o instanceof Timestamp) {
            return (Timestamp) o;
        }
        throw new UnsupportedOperationException();
    }

    public InputStream getAsciiStream(final String columnName) throws SQLException {
        final Object o = getObject(columnName);
        if (o instanceof InputStream) {
            return (InputStream) o;
        }
        throw new UnsupportedOperationException();
    }

    public InputStream getUnicodeStream(final String columnName) throws SQLException {
        final Object o = getObject(columnName);
        if (o instanceof InputStream) {
            return (InputStream) o;
        }
        throw new UnsupportedOperationException();
    }

    public InputStream getBinaryStream(final String columnName) throws SQLException {
        final Object o = getObject(columnName);
        if (o instanceof InputStream) {
            return (InputStream) o;
        }
        throw new UnsupportedOperationException();
    }

    public SQLWarning getWarnings() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public void clearWarnings() throws SQLException {
    }

    public String getCursorName() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public ResultSetMetaData getMetaData() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public Object getObject(final int columnIndex) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public Object getObject(final String columnName) throws SQLException {
        if (index >= data.size()) {
            throw new SQLException("not enough data");
        }
        return data.get(index).get(columnName);
    }

    public int findColumn(final String columnName) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public Reader getCharacterStream(final int columnIndex) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public Reader getCharacterStream(final String columnName) throws SQLException {
        return null;
    }

    public BigDecimal getBigDecimal(final int columnIndex) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public BigDecimal getBigDecimal(final String columnName) throws SQLException {
        return null;
    }

    public boolean isBeforeFirst() throws SQLException {
        return index == -1;
    }

    public boolean isAfterLast() throws SQLException {
        return index == data.size();
    }

    public boolean isFirst() throws SQLException {
        return index == 0;
    }

    public boolean isLast() throws SQLException {
        return index == data.size() - 1;
    }

    public void beforeFirst() throws SQLException {
        index = -1;
    }

    public void afterLast() throws SQLException {
        index = data.size();
    }

    public boolean first() throws SQLException {
        index = 0;
        return !data.isEmpty();
    }

    public boolean last() throws SQLException {
        index = data.size() - 1;
        return !data.isEmpty();
    }

    public int getRow() throws SQLException {
        return index;
    }

    public boolean relative(final int rows) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public boolean previous() throws SQLException {
        index --;
        return index > -1 && !data.isEmpty();
    }

    public void setFetchDirection(final int direction) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public int getFetchDirection() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public void setFetchSize(final int rows) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public int getFetchSize() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public int getType() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public int getConcurrency() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public boolean rowUpdated() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public boolean rowInserted() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public boolean rowDeleted() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public void updateNull(final int columnIndex) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public void updateBoolean(final int columnIndex, final boolean x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public void updateByte(final int columnIndex, final byte x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public void updateShort(final int columnIndex, final short x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public void updateInt(final int columnIndex, final int x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public void updateLong(final int columnIndex, final long x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public void updateFloat(final int columnIndex, final float x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public void updateDouble(final int columnIndex, final double x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public void updateBigDecimal(final int columnIndex, final BigDecimal x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public void updateString(final int columnIndex, final String x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public void updateBytes(final int columnIndex, final byte[] x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public void updateDate(final int columnIndex, final Date x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public void updateTime(final int columnIndex, final Time x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public void updateTimestamp(final int columnIndex, final Timestamp x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public void updateAsciiStream(final int columnIndex, final InputStream x, final int length) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public void updateBinaryStream(final int columnIndex, final InputStream x, final int length) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public void updateCharacterStream(final int columnIndex, final Reader x, final int length) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public void updateObject(final int columnIndex, final Object x, final int scale) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public void updateObject(final int columnIndex, final Object x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public void updateNull(final String columnName) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public void updateBoolean(final String columnName, final boolean x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public void updateByte(final String columnName, final byte x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public void updateShort(final String columnName, final short x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public void updateInt(final String columnName, final int x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public void updateLong(final String columnName, final long x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public void updateFloat(final String columnName, final float x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public void updateDouble(final String columnName, final double x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public void updateBigDecimal(final String columnName, final BigDecimal x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public void updateString(final String columnName, final String x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public void updateBytes(final String columnName, final byte[] x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public void updateDate(final String columnName, final Date x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public void updateTime(final String columnName, final Time x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public void updateTimestamp(final String columnName, final Timestamp x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public void updateAsciiStream(final String columnName, final InputStream x, final int length) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public void updateBinaryStream(final String columnName, final InputStream x, final int length) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public void updateCharacterStream(final String columnName, final Reader reader, final int length)
            throws SQLException {
        throw new UnsupportedOperationException();
    }

    public void updateObject(final String columnName, final Object x, final int scale) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public void updateObject(final String columnName, final Object x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public void insertRow() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public void updateRow() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public void deleteRow() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public void refreshRow() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public void cancelRowUpdates() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public void moveToInsertRow() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public void moveToCurrentRow() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public Statement getStatement() throws SQLException {
        return statement;
    }

    public Object getObject(final int i, final Map<String, Class<?>> map) throws SQLException {
        return null;
    }

    public Ref getRef(final int i) throws SQLException {
        return null;
    }

    public Blob getBlob(final int i) throws SQLException {
        return null;
    }

    public Clob getClob(final int i) throws SQLException {
        return null;
    }

    public Array getArray(final int i) throws SQLException {
        return null;
    }

    public Object getObject(final String colName, final Map<String, Class<?>> map) throws SQLException {
        return null;
    }

    public Ref getRef(final String colName) throws SQLException {
        return null;
    }

    public Blob getBlob(final String colName) throws SQLException {
        return null;
    }

    public Clob getClob(final String colName) throws SQLException {
        return null;
    }

    public Array getArray(final String colName) throws SQLException {
        return null;
    }

    public Date getDate(final int columnIndex, final Calendar cal) throws SQLException {
        return null;
    }

    public Date getDate(final String columnName, final Calendar cal) throws SQLException {
        return null;
    }

    public Time getTime(final int columnIndex, final Calendar cal) throws SQLException {
        return null;
    }

    public Time getTime(final String columnName, final Calendar cal) throws SQLException {
        return null;
    }

    public Timestamp getTimestamp(final int columnIndex, final Calendar cal) throws SQLException {
        return null;
    }

    public Timestamp getTimestamp(final String columnName, final Calendar cal) throws SQLException {
        return null;
    }

    public URL getURL(final int columnIndex) throws SQLException {
        return null;
    }

    public URL getURL(final String columnName) throws SQLException {
        return null;
    }

    public void updateRef(final int columnIndex, final Ref x) throws SQLException {
    }

    public void updateRef(final String columnName, final Ref x) throws SQLException {
    }

    public void updateBlob(final int columnIndex, final Blob x) throws SQLException {
    }

    public void updateBlob(final String columnName, final Blob x) throws SQLException {
    }

    public void updateClob(final int columnIndex, final Clob x) throws SQLException {
    }

    public void updateClob(final String columnName, final Clob x) throws SQLException {
    }

    public void updateArray(final int columnIndex, final Array x) throws SQLException {
    }

    public void updateArray(final String columnName, final Array x) throws SQLException {
    }

    public RowId getRowId(final int i) throws SQLException {
        return null;
    }

    public RowId getRowId(final String s) throws SQLException {
        return null;
    }

    public void updateRowId(final int i, final RowId rowId) throws SQLException {
    }

    public void updateRowId(final String s, final RowId rowId) throws SQLException {
    }

    public int getHoldability() throws SQLException {
        return 0;
    }

    public boolean isClosed() throws SQLException {
        return false;
    }

    public void updateNString(final int i, final String s) throws SQLException {
    }

    public void updateNString(final String s, final String s1) throws SQLException {
    }

    public void updateNClob(final int i, final NClob nClob) throws SQLException {
    }

    public void updateNClob(final String s, final NClob nClob) throws SQLException {
    }

    public NClob getNClob(final int i) throws SQLException {
        return null;
    }

    public NClob getNClob(final String s) throws SQLException {
        return null;
    }

    public SQLXML getSQLXML(final int i) throws SQLException {
        return null;
    }

    public SQLXML getSQLXML(final String s) throws SQLException {
        return null;
    }

    public void updateSQLXML(final int i, final SQLXML sqlxml) throws SQLException {
    }

    public void updateSQLXML(final String s, final SQLXML sqlxml) throws SQLException {
    }

    public String getNString(final int i) throws SQLException {
        return null;
    }

    public String getNString(final String s) throws SQLException {
        return null;
    }

    public Reader getNCharacterStream(final int i) throws SQLException {
        return null;
    }

    public Reader getNCharacterStream(final String s) throws SQLException {
        return null;
    }

    public void updateNCharacterStream(final int i, final Reader reader, final long l) throws SQLException {
    }

    public void updateNCharacterStream(final String s, final Reader reader, final long l) throws SQLException {
    }

    public void updateAsciiStream(final int i, final InputStream inputStream, final long l) throws SQLException {
    }

    public void updateBinaryStream(final int i, final InputStream inputStream, final long l) throws SQLException {
    }

    public void updateCharacterStream(final int i, final Reader reader, final long l) throws SQLException {
    }

    public void updateAsciiStream(final String s, final InputStream inputStream, final long l) throws SQLException {
    }

    public void updateBinaryStream(final String s, final InputStream inputStream, final long l) throws SQLException {
    }

    public void updateCharacterStream(final String s, final Reader reader, final long l) throws SQLException {
    }

    public void updateBlob(final int i, final InputStream inputStream, final long l) throws SQLException {
    }

    public void updateBlob(final String s, final InputStream inputStream, final long l) throws SQLException {
    }

    public void updateClob(final int i, final Reader reader, final long l) throws SQLException {
    }

    public void updateClob(final String s, final Reader reader, final long l) throws SQLException {
    }

    public void updateNClob(final int i, final Reader reader, final long l) throws SQLException {
    }

    public void updateNClob(final String s, final Reader reader, final long l) throws SQLException {
    }

    public void updateNCharacterStream(final int i, final Reader reader) throws SQLException {
    }

    public void updateNCharacterStream(final String s, final Reader reader) throws SQLException {
    }

    public void updateAsciiStream(final int i, final InputStream inputStream) throws SQLException {
    }

    public void updateBinaryStream(final int i, final InputStream inputStream) throws SQLException {
    }

    public void updateCharacterStream(final int i, final Reader reader) throws SQLException {
    }

    public void updateAsciiStream(final String s, final InputStream inputStream) throws SQLException {
    }

    public void updateBinaryStream(final String s, final InputStream inputStream) throws SQLException {
    }

    public void updateCharacterStream(final String s, final Reader reader) throws SQLException {
    }

    public void updateBlob(final int i, final InputStream inputStream) throws SQLException {
    }

    public void updateBlob(final String s, final InputStream inputStream) throws SQLException {
    }

    public void updateClob(final int i, final Reader reader) throws SQLException {
    }

    public void updateClob(final String s, final Reader reader) throws SQLException {
    }

    public void updateNClob(final int i, final Reader reader) throws SQLException {
    }

    public void updateNClob(final String s, final Reader reader) throws SQLException {
    }

    public <T> T unwrap(final Class<T> tClass) throws SQLException {
        return null;
    }

    public boolean isWrapperFor(final Class<?> aClass) throws SQLException {
        return false;
    }
}
