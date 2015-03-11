/*
 * $Id: EntityFindOptions.java,v 1.1 2005/04/01 05:58:02 sfarquhar Exp $
 *
 *  Copyright (c) 2002 The Open For Business Project - www.ofbiz.org
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

import java.io.Serializable;
import java.sql.ResultSet;

/**
 * Advanced options for finding entities.
 * Examples:
 * <p>
 * <pre><code>
 *     EntityFindOptions options1 = new EntityFindOptions().distinct().limit(10);
 *
 *     EntityFindOptions options2 = EntityFindOptions.findOptions()
 *          .scrollInsensitive()
 *          .updatable()
 *          .fetchSize(30);
 * </code></pre>
 * </p>
 *
 * @author     <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 * @since      Aug 8, 2002
 * @version    1.0
 */
public class EntityFindOptions implements Serializable {

    /**
     * Type constant from the java.sql.ResultSet object for convenience.
     *
     * @deprecated use the original constant instead
     */
    @Deprecated
    public static final int TYPE_FORWARD_ONLY = ResultSet.TYPE_FORWARD_ONLY;

    /**
     * Type constant from the java.sql.ResultSet object for convenience.
     *
     * @deprecated use the original constant instead
     */
    @Deprecated
    public static final int TYPE_SCROLL_INSENSITIVE = ResultSet.TYPE_SCROLL_INSENSITIVE;

    /**
     * Type constant from the java.sql.ResultSet object for convenience.
     *
     * @deprecated use the original constant instead
     */
    @Deprecated
    public static final int TYPE_SCROLL_SENSITIVE = ResultSet.TYPE_SCROLL_SENSITIVE;

    /**
     * Type constant from the java.sql.ResultSet object for convenience.
     *
     * @deprecated use the original constant instead
     */
    @Deprecated
    public static final int CONCUR_READ_ONLY = ResultSet.CONCUR_READ_ONLY;

    /**
     * Type constant from the java.sql.ResultSet object for convenience.
     *
     * @deprecated use the original constant instead
     */
    @Deprecated
    public static final int CONCUR_UPDATABLE = ResultSet.CONCUR_UPDATABLE;

    /**
     * Creates a new {@link EntityFindOptions}.  This is equivalent to the
     * {@link #EntityFindOptions() default constructor}, but is implemented as
     * a static method so that it can be used more conveniently by other
     * classes that {@code static import} it.
     * <p>
     * Example:
     * <code><pre>
     *     import org.ofbiz.core.entity.EntityFindOptions;
     *     import static org.ofbiz.core.entity.EntityFindOptions.findOptions;
     *
     *     [...]
     *     {
     *         EntityFindOptions options = findOptions().distinct().maxEntries(5);
     *         [...]
     *     }
     * </pre></code>
     * </p>
     *
     * @return the new options
     */
    @SuppressWarnings("unused")
    public static EntityFindOptions findOptions() {
        return new EntityFindOptions();
    }

    protected boolean specifyTypeAndConcurrency = true;
    protected int resultSetType = ResultSet.TYPE_FORWARD_ONLY;
    protected int resultSetConcurrency = ResultSet.CONCUR_READ_ONLY;
    protected boolean distinct;
    /** maximum results to obtain from DB - negative values mean no limit */
    protected int maxResults = -1;
    protected long offset;
    protected int fetchSize = -1;

    /**
     * Default constructor. Defaults are as follows:
     * specifyTypeAndConcurrency = true
     * resultSetType = TYPE_FORWARD_ONLY
     * resultSetConcurrency = CONCUR_READ_ONLY
     * distinct = false
     * maxResults = -1  (no limit)
     * fetchSize = -1  (use driver's default setting)
     */
    public EntityFindOptions() {}

    /**
     * Constructor that allows some options to be provided at construction time.
     *
     * @param specifyTypeAndConcurrency if {@code false}, then resultSetType and resultSetConcurrency are ignored, and the
     *                             JDBC driver's defaults are used for these fields, instead
     * @param resultSetType either {@link #TYPE_FORWARD_ONLY}, {@link #TYPE_SCROLL_INSENSITIVE}, or {@link #TYPE_SCROLL_SENSITIVE}
     * @param resultSetConcurrency either {@link #CONCUR_READ_ONLY}, or {@link #CONCUR_UPDATABLE}
     * @param distinct if {@code true}, then the {@code DISTINCT} SQL keyword is used in the query
     * @param maxResults if specified, then this value is used to limit the number of results retrieved,
     *                   by using {@code LIMIT} on MySQL, {@code ROWNUM} on Oracle, and so on
     * @deprecated since 1.0.27 - Please use the chained form as shown in the {@link EntityFindOptions examples}.
     */
    @Deprecated
    @SuppressWarnings("unused")
    public EntityFindOptions(final boolean specifyTypeAndConcurrency,
            final int resultSetType,
            final int resultSetConcurrency,
            final boolean distinct,
            final int maxResults)
    {
        this.specifyTypeAndConcurrency = specifyTypeAndConcurrency;
        this.resultSetType = resultSetType;
        this.resultSetConcurrency = resultSetConcurrency;
        this.distinct = distinct;
        this.maxResults = maxResults;
    }

    /**
     * Indicates whether the {@code resultSetType} and {@code resultSetConcurrency}
     * fields will be used to specify how the results will be used; false means that
     * the JDBC driver's default values will be used.
     *
     * @return see above
     * @deprecated use {@link #isCustomResultSetTypeAndConcurrency()} instead; better named
     */
    @Deprecated
    public boolean getSpecifyTypeAndConcur() {
        return isCustomResultSetTypeAndConcurrency();
    }

    /**
     * Indicates whether the {@code resultSetType} and {@code resultSetConcurrency}
     * fields will be used to specify how the results will be used; false means that
     * the JDBC driver's default values will be used.
     *
     * @return see above
     */
    public boolean isCustomResultSetTypeAndConcurrency() {
        return specifyTypeAndConcurrency;
    }

    /**
     * Setting this to true means that the {@code resultSetType} and
     * {@code resultSetConcurrency} parameters will be used to specify how the
     * results will be used; if false, the JDBC driver's default values will be
     * used.
     *
     * @param specifyTypeAndConcurrency see above
     * @deprecated there's no valid use case for this method; call {@link #useDriverDefaultsForTypeAndConcurrency()} to
     * revert to the driver's default settings for concurrency and type of result set
     */
    @Deprecated
    public void setSpecifyTypeAndConcur(final boolean specifyTypeAndConcurrency) {
        this.specifyTypeAndConcurrency = specifyTypeAndConcurrency;
    }

    /**
     * Specifies that the JDBC driver's default values should be used for concurrency and type of result set, in other
     * words any custom settings for those options should be ignored.
     */
    public void useDriverDefaultsForTypeAndConcurrency() {
        this.specifyTypeAndConcurrency = false;
    }

    /**
     * Indicates how the ResultSet will be traversed.
     *
     * @return the result set type
     * @see #setResultSetType(int)
     */
    public int getResultSetType() {
        return resultSetType;
    }

    /**
     * Specifies how the ResultSet will be traversed. Available values:
     * {@link ResultSet#TYPE_FORWARD_ONLY},
     * {@link ResultSet#TYPE_SCROLL_INSENSITIVE}, and
     * {@link ResultSet#TYPE_SCROLL_SENSITIVE}. If you want it to be fast, use
     * the common default, {@link ResultSet#TYPE_FORWARD_ONLY}.
     *
     * @param resultSetType the result set type to set
     */
    public void setResultSetType(final int resultSetType) {
        this.resultSetType = resultSetType;
    }

    /**
     * Specifies whether or not the ResultSet can be updated. Available values:
     * ResultSet.CONCUR_READ_ONLY or ResultSet.CONCUR_UPDATABLE. Should pretty
     * much always be ResultSet.CONCUR_READ_ONLY with the Entity Engine.
     *
     * @return see above
     */
    public int getResultSetConcurrency() {
        return resultSetConcurrency;
    }

    /**
     * Specifies whether or not the ResultSet can be updated. Available values:
     * ResultSet.CONCUR_READ_ONLY or ResultSet.CONCUR_UPDATABLE. Should pretty
     * much always be ResultSet.CONCUR_READ_ONLY with the Entity Engine.
     *
     * @param resultSetConcurrency the value to set
     */
    public void setResultSetConcurrency(final int resultSetConcurrency) {
        this.resultSetConcurrency = resultSetConcurrency;
    }

    /**
     * Indicates whether the values returned will be filtered to remove duplicate values.
     *
     * @return see above
     */
    public boolean getDistinct() {
        return distinct;
    }

    /**
     * Specifies whether the values returned should be filtered to remove duplicate values.
     *
     * @param distinct whether to return only unique values
     */
    public void setDistinct(final boolean distinct) {
        this.distinct = distinct;
    }

    /**
     * Specifies the maximum number of results to be returned by the query.
     *
     * @return see above
     */
    public int getMaxResults() {
        return maxResults;
    }

    /**
     * Specifies the maximum number of results to be returned by the query. Must be positive.
     *
     * @param maxResults ignored if zero or less
     */
    public void setMaxResults(final int maxResults) {
        if (maxResults > 0) {
            this.maxResults = maxResults;
        }
    }

    /**
     * Returns the number of rows to be skipped.
     *
     * @return see above
     */
    public long getOffset() {
        return offset;
    }

    /**
     * Specifies the number of rows to be skipped, which is ignored if
     * <code>maxResults</code> is not also set.
     *
     * @param offset if negative, this method does nothing
     */
    @SuppressWarnings("unused")
    public void setOffset(final long offset) {
        if (offset >= 0) {
            this.offset = offset;
        }
    }

    /**
     * Specifies the value to use for the fetch size on the prepared statement.
     * Please see the comments in {@link #setFetchSize(int)} for restrictions.
     *
     * @return the fetch size
     */
    public int getFetchSize() {
        return fetchSize;
    }

    /**
     * Specifies the value to use for the fetch size on the prepared statement.  Note that
     * the values that may be used are database-dependent.  Use this with caution!
     * <p>
     * <b>WARNING</b>: This setting is a <em>hint</em> for the database driver, and the driver
     * is <em>not</em> required to honour it!  Several databases put other restrictions on its
     * use as well.  Postgres will definitely ignore this setting when the database connection
     * is in auto-commit mode, so you will probably have to use the {@link TransactionUtil}
     * if you want this to work.
     * </p>
     * <p>
     * <b>WARNING</b>: This value may need to be set to a database-dependent value.  For example,
     * the most useful value on MySQL is {@code Integer.MIN_VALUE} to get a streaming result
     * set, but this value will be rejected by most other databases.
     * </p>
     */
    public void setFetchSize(final int fetchSize) {
        this.fetchSize = fetchSize;
    }

    /**
     * Same as using both {@link #setSpecifyTypeAndConcur(boolean) setSpecifyTypeAndConcur(true)}
     * and {@link #setResultSetType(int) setResultSetType(TYPE_FORWARD_ONLY)}.  Note that you
     * should also use either {@link #readOnly()} or {@link #updatable()} for maximum driver
     * compatibility.
     *
     * @return {@code this}, for convenient use as a chained builder
     */
    @SuppressWarnings("unused")
    public EntityFindOptions forwardOnly() {
        specifyTypeAndConcurrency = true;
        resultSetType = ResultSet.TYPE_FORWARD_ONLY;
        return this;
    }

    /**
     * Same as using both {@link #setSpecifyTypeAndConcur(boolean) setSpecifyTypeAndConcur(true)}
     * and {@link #setResultSetType(int) setResultSetType(TYPE_SCROLL_SENSITIVE)}.  Note that you
     * should also use either {@link #readOnly()} or {@link #updatable()} for maximum driver
     * compatibility.
     *
     * @return {@code this}, for convenient use as a chained builder
     */
    @SuppressWarnings("unused")
    public EntityFindOptions scrollSensitive() {
        specifyTypeAndConcurrency = true;
        resultSetType = ResultSet.TYPE_SCROLL_SENSITIVE;
        return this;
    }

    /**
     * Same as using both {@link #setSpecifyTypeAndConcur(boolean) setSpecifyTypeAndConcur(true)}
     * and {@link #setResultSetType(int) setResultSetType(TYPE_SCROLL_INSENSITIVE)}.  Note that you
     * should also use either {@link #readOnly()} or {@link #updatable()} for maximum driver
     * compatibility.
     *
     * @return {@code this}, for convenient use as a chained builder
     */
    @SuppressWarnings("unused")
    public EntityFindOptions scrollInsensitive() {
        specifyTypeAndConcurrency = true;
        resultSetType = ResultSet.TYPE_SCROLL_INSENSITIVE;
        return this;
    }

    /**
     * Same as using both {@link #setSpecifyTypeAndConcur(boolean) setSpecifyTypeAndConcur(true)}
     * and {@link #setResultSetConcurrency(int)} setResultSetConcurrency(CONCUR_READ_ONLY)}.  Note that you
     * should also use {@link #forwardOnly()}, {@link #scrollSensitive()} or {@link #scrollInsensitive()}
     * for maximum driver compatibility.
     *
     * @return {@code this}, for convenient use as a chained builder
     */
    public EntityFindOptions readOnly() {
        specifyTypeAndConcurrency = true;
        resultSetConcurrency = ResultSet.CONCUR_READ_ONLY;
        return this;
    }

    /**
     * Same as using both {@link #setSpecifyTypeAndConcur(boolean) setSpecifyTypeAndConcur(true)}
     * and {@link #setResultSetConcurrency(int)} setResultSetConcurrency(CONCUR_UPDATABLE)}.  Note that you
     * should also use {@link #forwardOnly()}, {@link #scrollSensitive()} or {@link #scrollInsensitive()}
     * for maximum driver compatibility.
     *
     * @return {@code this}, for convenient use as a chained builder
     */
    public EntityFindOptions updatable() {
        specifyTypeAndConcurrency = true;
        resultSetConcurrency = ResultSet.CONCUR_UPDATABLE;
        return this;
    }

    /**
     * Same as {@link #setDistinct(boolean) setDistinct(true)}.
     *
     * @return {@code this}, for convenient use as a chained builder
     */
    public EntityFindOptions distinct() {
        distinct = true;
        return this;
    }

    /**
     * Same as {@link #setMaxResults(int)}.
     *
     * @return {@code this}, for convenient use as a chained builder
     */
    @SuppressWarnings("unused")
    public EntityFindOptions maxResults(final int maxResults) {
        this.maxResults = maxResults;
        return this;
    }

    /**
     * Same as {@link #setFetchSize(int)}.
     *
     * @return {@code this}, for convenient use as a chained builder
     */
    @SuppressWarnings("unused")
    public EntityFindOptions fetchSize(final int fetchSize) {
        this.fetchSize = fetchSize;
        return this;
    }

    /**
     * Specifies the range of results to find.
     *
     * @param offset the offset from which to start
     * @param maxResults the maximum number of results to find
     * @return {@code this}, for convenient use as a chained builder
     */
    @SuppressWarnings("unused")
    public EntityFindOptions range(final long offset, final int maxResults) {
        this.maxResults = maxResults;
        this.offset = offset;
        return this;
    }
}
