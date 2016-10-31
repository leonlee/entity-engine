/*
 *  Copyright (c) 2001 The Open For Business Project - www.ofbiz.org
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

package org.ofbiz.core.entity;


import org.ofbiz.core.entity.jdbc.SQLProcessor;
import org.ofbiz.core.entity.jdbc.SqlJdbcUtil;
import org.ofbiz.core.entity.model.ModelEntity;
import org.ofbiz.core.entity.model.ModelField;
import org.ofbiz.core.entity.model.ModelFieldTypeReader;
import org.ofbiz.core.util.GeneralRuntimeException;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;


/**
 * Generic Entity Cursor List Iterator for Handling Cursored DB Results
 *
 * Note that you should *not* rely on this for streaming large datasets, as the backing ResultSet will pull the entire
 * dataset into memory anyway. For more information, see <a href="https://extranet.atlassian.com/display/JIRADEV/2015/08/04/PSA%3A+OfBizListIterator+Is+Not+Good+Enough">PSA: OfBizListIterator is not good enough</a>
 *
 * @author     <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 * @created    July 12, 2002
 */
public class EntityListIterator implements ListIterator<GenericValue> {

    protected SQLProcessor sqlp;
    protected ResultSet resultSet;
    protected ModelEntity modelEntity;
    protected List<ModelField> selectFields;
    protected ModelFieldTypeReader modelFieldTypeReader;
    protected boolean closed = false;
    protected boolean haveMadeValue = false;
    protected GenericDelegator delegator = null;

    public EntityListIterator(SQLProcessor sqlp, ModelEntity modelEntity, List<ModelField> selectFields, ModelFieldTypeReader modelFieldTypeReader) {
        this.sqlp = sqlp;
        this.resultSet = sqlp.getResultSet();
        this.modelEntity = modelEntity;
        this.selectFields = selectFields;
        this.modelFieldTypeReader = modelFieldTypeReader;
    }

    public void setDelegator(GenericDelegator delegator) {
        this.delegator = delegator;
    }

    /**
     * Detect whether or not the column data is case sensitive.
     *
     * @param entityFieldName the field to check.
     * @return true if the field is case sensitive or false otherwise.
     * @throws GenericEntityException if the information could not be found.
     */
    public boolean isCaseSensitive(final String entityFieldName) throws GenericEntityException {
        int targetColumnIndex = getJdbcColumnIndex(entityFieldName);
        if (targetColumnIndex == -1) {
            throw new GenericEntityException("The field is not defined in the entity model " + entityFieldName);
        }
        try {
            final ResultSetMetaData metaData = resultSet.getMetaData();
            if (metaData != null) {
                return metaData.isCaseSensitive(targetColumnIndex);
            } else {
                throw new GenericEntityException("Unable to determine column case senstivity on field '" + entityFieldName + "'.");
            }
        } catch (SQLException e) {
            throw new GenericEntityException("Unable to determine column case senstivity on field '" + entityFieldName + "'.", e);
        }
    }

    /**
     * Get the column index for the given field name.
     *
     * @param entityFieldName the field name to find.
     * @return the index of the column if found or -1 if not.
     */
    private int getJdbcColumnIndex(final String entityFieldName) {
        int targetColumnIndex = -1;
        for (int j = 0; j < selectFields.size(); j++) {
            ModelField curField = selectFields.get(j);
            if (curField.getName().equals(entityFieldName)) {
                targetColumnIndex = j + 1;
                break;
            }
        }
        return targetColumnIndex;
    }


    /**
     * Sets the cursor position to just after the last result so that previous() will return the last result
     */
    public void afterLast() throws GenericEntityException {
        try {
            resultSet.afterLast();
        } catch (SQLException e) {
            throw new GenericEntityException("Error setting the cursor to afterLast", e);
        }
    }

    /**
     * Sets the cursor position to just before the first result so that next() will return the first result
     */
    public void beforeFirst() throws GenericEntityException {
        try {
            resultSet.beforeFirst();
        } catch (SQLException e) {
            throw new GenericEntityException("Error setting the cursor to beforeFirst", e);
        }
    }

    /**
     * Sets the cursor position to last result; if result set is empty returns false
     */
    public boolean last() throws GenericEntityException {
        try {
            return resultSet.last();
        } catch (SQLException e) {
            throw new GenericEntityException("Error setting the cursor to last", e);
        }
    }

    /**
     * Sets the cursor position to last result; if result set is empty returns false
     */
    public boolean first() throws GenericEntityException {
        try {
            return resultSet.first();
        } catch (SQLException e) {
            throw new GenericEntityException("Error setting the cursor to first", e);
        }
    }

    public void close() throws GenericEntityException {
        if (closed)
            throw new GenericResultSetClosedException("This EntityListIterator has been closed, this operation cannot be performed");

        sqlp.close();
        closed = true;
    }

    /**
     * NOTE: Calling this method does return the current value, but so does calling next() or previous(), so calling one of those AND this method will cause the value to be created twice
     */
    public GenericValue currentGenericValue() throws GenericEntityException {
        if (closed)
            throw new GenericResultSetClosedException("This EntityListIterator has been closed, this operation cannot be performed");

        GenericValue value = new GenericValue(modelEntity);

        for (int j = 0; j < selectFields.size(); j++) {
            ModelField curField = selectFields.get(j);

            SqlJdbcUtil.getValue(resultSet, j + 1, curField, value, modelFieldTypeReader);
        }

        value.modified = false;
        value.copyOriginalDbValues();
        value.setDelegator(this.delegator);
        this.haveMadeValue = true;
        return value;
    }

    public int currentIndex() throws GenericEntityException {
        if (closed)
            throw new GenericResultSetClosedException("This EntityListIterator has been closed, this operation cannot be performed");

        try {
            return resultSet.getRow();
        } catch (SQLException e) {
            throw new GenericEntityException("Error getting the current index", e);
        }
    }

    /**
     * performs the same function as the ResultSet.absolute method;
     * if rowNum is positive, goes to that position relative to the beginning of the list;
     * if rowNum is negative, goes to that position relative to the end of the list;
     * a rowNum of 1 is the same as first(); a rowNum of -1 is the same as last()
     */
    public boolean absolute(int rowNum) throws GenericEntityException {
        if (closed)
            throw new GenericResultSetClosedException("This EntityListIterator has been closed, this operation cannot be performed");

        try {
            return resultSet.absolute(rowNum);
        } catch (SQLException e) {
            throw new GenericEntityException("Error setting the absolute index to " + rowNum, e);
        }
    }

    /**
     * PLEASE NOTE: Because of the nature of the JDBC ResultSet interface this method can be very inefficient; it is much better to just use next() until it returns null
     */
    public boolean hasNext() {
        try {
            if (resultSet.isLast() || resultSet.isAfterLast()) {
                return false;
            } else {
                // do a quick game to see if the resultSet is empty:
                // if we are not in the first or beforeFirst positions and we haven't made any values yet, the result set is empty so return false
                if (!haveMadeValue && !resultSet.isBeforeFirst() && !resultSet.isFirst()) {
                    return false;
                } else {
                    return true;
                }
            }
        } catch (SQLException e) {
            throw new GeneralRuntimeException("Error while checking to see if this is the last result", e);
        }
    }

    /**
     * PLEASE NOTE: Because of the nature of the JDBC ResultSet interface this method can be very inefficient; it is much better to just use previous() until it returns null
     */
    public boolean hasPrevious() {
        try {
            if (resultSet.isFirst() || resultSet.isBeforeFirst()) {
                return false;
            } else {
                // do a quick game to see if the resultSet is empty:
                // if we are not in the last or afterLast positions and we haven't made any values yet, the result set is empty so return false
                if (!haveMadeValue && !resultSet.isAfterLast() && !resultSet.isLast()) {
                    return false;
                } else {
                    return true;
                }
            }
        } catch (SQLException e) {
            throw new GeneralRuntimeException("Error while checking to see if this is the first result", e);
        }
    }

    /**
     * Moves the cursor to the next position and returns the GenericValue object for that position; if there is no next, returns null
     */
    public GenericValue next() {
        try {
            if (resultSet.next()) {
                return currentGenericValue();
            } else {
                return null;
            }
        } catch (SQLException e) {
            throw new GeneralRuntimeException("Error getting the next result", e);
        } catch (GenericEntityException e) {
            throw new GeneralRuntimeException("Error creating GenericValue", e);
        }
    }

    /**
     * Returns the index of the next result, but does not guarantee that there will be a next result
     */
    public int nextIndex() {
        try {
            return currentIndex() + 1;
        } catch (GenericEntityException e) {
            throw new GeneralRuntimeException(e.getNonNestedMessage(), e.getNested());
        }
    }

    /**
     * Moves the cursor to the previous position and returns the GenericValue object for that position; if there is no previous, returns null
     */
    public GenericValue previous() {
        try {
            if (resultSet.previous()) {
                return currentGenericValue();
            } else {
                return null;
            }
        } catch (SQLException e) {
            throw new GeneralRuntimeException("Error getting the previous result", e);
        } catch (GenericEntityException e) {
            throw new GeneralRuntimeException("Error creating GenericValue", e);
        }
    }

    /**
     * Returns the index of the previous result, but does not guarantee that there will be a previous result
     */
    public int previousIndex() {
        try {
            return currentIndex() - 1;
        } catch (GenericEntityException e) {
            throw new GeneralRuntimeException("Error getting the current index", e);
        }
    }

    public void setFetchSize(int rows) throws GenericEntityException {
        try {
            resultSet.setFetchSize(rows);
        } catch (SQLException e) {
            throw new GenericEntityException("Error getting the next result", e);
        }
    }

    public List<GenericValue> getCompleteList() throws GenericEntityException {
        try {
            // if the resultSet has been moved forward at all, move back to the beginning
            if (haveMadeValue && !resultSet.isBeforeFirst()) {
                // do a quick check to see if the ResultSet is empty
                resultSet.beforeFirst();
            }
            List<GenericValue> list = new LinkedList<GenericValue>();
            GenericValue nextValue = null;

            while ((nextValue = this.next()) != null) {
                list.add(nextValue);
            }
            return list;
        } catch (SQLException e) {
            throw new GeneralRuntimeException("Error getting results", e);
        } catch (GeneralRuntimeException e) {
            throw new GenericEntityException(e.getNonNestedMessage(), e.getNested());
        }
    }

    /**
     * Gets a partial list of results starting at start and containing at most number elements.
     * Start is a one based value, ie 1 is the first element.
     */
    public List<GenericValue> getPartialList(int start, int number) throws GenericEntityException {
        try {
            if (number == 0) return new ArrayList<GenericValue>();
            List<GenericValue> list = new ArrayList<GenericValue>(number);

            // if can't reposition to desired index, throw exception
            if (!resultSet.absolute(start)) {
                throw new GenericEntityException("Could not move to the start position of " + start + ", there are probably not that many results for this find.");
            }

            // get the first as the current one
            list.add(this.currentGenericValue());

            GenericValue nextValue = null;
            // init numRetreived to one since we have already grabbed the initial one
            int numRetreived = 1;

            //number > numRetreived comparison goes first to avoid the unwanted call to next
            while (number > numRetreived && (nextValue = this.next()) != null) {
                list.add(nextValue);
                numRetreived++;
            }
            return list;
        } catch (SQLException e) {
            throw new GeneralRuntimeException("Error getting results", e);
        } catch (GeneralRuntimeException e) {
            throw new GenericEntityException(e.getNonNestedMessage(), e.getNested());
        }
    }

    public void add(GenericValue obj) {
        throw new GeneralRuntimeException("CursorListIterator currently only supports read-only access");
    }

    public void remove() {
        throw new GeneralRuntimeException("CursorListIterator currently only supports read-only access");
    }

    public void set(GenericValue obj) {
        throw new GeneralRuntimeException("CursorListIterator currently only supports read-only access");
    }
}
