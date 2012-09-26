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


import java.sql.*;


/**
 * Contains a number of variables used to select certain advanced finding options.
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
 *@author     <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 *@created    Aug 8, 2002
 *@version    1.0
 */
public class EntityFindOptions implements java.io.Serializable {

    /** Type constant from the java.sql.ResultSet object for convenience */
    public static final int TYPE_FORWARD_ONLY = ResultSet.TYPE_FORWARD_ONLY;

    /** Type constant from the java.sql.ResultSet object for convenience */
    public static final int TYPE_SCROLL_INSENSITIVE = ResultSet.TYPE_SCROLL_INSENSITIVE;

    /** Type constant from the java.sql.ResultSet object for convenience */
    public static final int TYPE_SCROLL_SENSITIVE = ResultSet.TYPE_SCROLL_SENSITIVE;

    /** Concurrency constant from the java.sql.ResultSet object for convenience */
    public static final int CONCUR_READ_ONLY = ResultSet.CONCUR_READ_ONLY;

    /** Concurrency constant from the java.sql.ResultSet object for convenience */
    public static final int CONCUR_UPDATABLE = ResultSet.CONCUR_UPDATABLE;

    protected boolean specifyTypeAndConcur = true;
    protected int resultSetType = TYPE_FORWARD_ONLY;
    protected int resultSetConcurrency = CONCUR_READ_ONLY;
    protected boolean distinct = false;
    /** maximum results to obtain from DB - negative values mean no limit */
    protected int maxResults = -1;
    protected int fetchSize = -1;

    /** Default constructor. Defaults are as follows:
     *      specifyTypeAndConcur = true
     *      resultSetType = TYPE_FORWARD_ONLY
     *      resultSetConcurrency = CONCUR_READ_ONLY
     *      distinct = false
     *      maxResults = -1  (no limit)
     *      fetchSize = -1  (use driver's default setting)
     */
    public EntityFindOptions() {}

    /**
     *
     * @param specifyTypeAndConcur if {@code false}, then resultSetType and resultSetConcurrency are ignored, and the
     *                             JDBC driver's defaults are used for these fields, instead
     * @param resultSetType either {@link #TYPE_FORWARD_ONLY}, {@link #TYPE_SCROLL_INSENSITIVE}, or {@link #TYPE_SCROLL_SENSITIVE}
     * @param resultSetConcurrency either {@link #CONCUR_READ_ONLY}, or {@link #CONCUR_UPDATABLE}
     * @param distinct if {@code true}, then the {@code DISTINCT} SQL keyword is used in the query
     * @param maxResults if specified, then this value is used to limit the number of results retrieved,
     *                   by using {@code LIMIT} on MySQL, {@code ROWNUM} on Oracle, and so on
     * @deprecated since 1.0.27 - Please use the chained form as shown in the {@link EntityFindOptions examples}.
     */
    @Deprecated
    public EntityFindOptions(boolean specifyTypeAndConcur, int resultSetType, int resultSetConcurrency, boolean distinct, int maxResults) {
        this.specifyTypeAndConcur = specifyTypeAndConcur;
        this.resultSetType = resultSetType;
        this.resultSetConcurrency = resultSetConcurrency;
        this.distinct = distinct;
        this.maxResults = maxResults;
    }

    /** If true the following two parameters (resultSetType and resultSetConcurrency) will be used to specify 
     *      how the results will be used; if false the default values for the JDBC driver will be used
     */
    public boolean getSpecifyTypeAndConcur() {
        return specifyTypeAndConcur;
    }

    /** If true the following two parameters (resultSetType and resultSetConcurrency) will be used to specify 
     *      how the results will be used; if false the default values for the JDBC driver will be used
     */
    public void setSpecifyTypeAndConcur(boolean specifyTypeAndConcur) {
        this.specifyTypeAndConcur = specifyTypeAndConcur;
    }

    /** Specifies how the ResultSet will be traversed. Available values: ResultSet.TYPE_FORWARD_ONLY, 
     *      ResultSet.TYPE_SCROLL_INSENSITIVE or ResultSet.TYPE_SCROLL_SENSITIVE. See the java.sql.ResultSet JavaDoc for 
     *      more information. If you want it to be fast, use the common default: ResultSet.TYPE_FORWARD_ONLY.
     */
    public int getResultSetType() {
        return resultSetType;
    }

    /** Specifies how the ResultSet will be traversed. Available values: ResultSet.TYPE_FORWARD_ONLY, 
     *      ResultSet.TYPE_SCROLL_INSENSITIVE or ResultSet.TYPE_SCROLL_SENSITIVE. See the java.sql.ResultSet JavaDoc for 
     *      more information. If you want it to be fast, use the common default: ResultSet.TYPE_FORWARD_ONLY.
     */
    public void setResultSetType(int resultSetType) {
        this.resultSetType = resultSetType;
    }

    /** Specifies whether or not the ResultSet can be updated. Available values:
     *      ResultSet.CONCUR_READ_ONLY or ResultSet.CONCUR_UPDATABLE. Should pretty much always be 
     *      ResultSet.CONCUR_READ_ONLY with the Entity Engine.
     */
    public int getResultSetConcurrency() {
        return resultSetConcurrency;
    }

    /** Specifies whether or not the ResultSet can be updated. Available values: 
     *      ResultSet.CONCUR_READ_ONLY or ResultSet.CONCUR_UPDATABLE. Should pretty much always be 
     *      ResultSet.CONCUR_READ_ONLY with the Entity Engine.
     */
    public void setResultSetConcurrency(int resultSetConcurrency) {
        this.resultSetConcurrency = resultSetConcurrency;
    }

    /** Specifies whether the values returned should be filtered to remove duplicate values. */
    public boolean getDistinct() {
        return distinct;
    }

    /** Specifies whether the values returned should be filtered to remove duplicate values. */
    public void setDistinct(boolean distinct) {
        this.distinct = distinct;
    }

    /** Specifies the maximum number of results to be returned by the query. */
    public int getMaxResults() {
        return maxResults;
    }

    /** Specifies the maximum number of results to be returned by the query. */
    public void setMaxResults(int maxResults) {
        this.maxResults = maxResults;
    }

    /** Specifies the value to use for the fetch size on the prepared statement.  Note that
     * the values that may be used are database-dependent.  For example, you can use
     * {@code Integer.MIN_VALUE} to get streaming result sets on MySQL, but this value will
     * be rejected by most other databases.
     */
    public int getFetchSize() {
        return fetchSize;
    }

    /** Specifies the value to use for the fetch size on the prepared statement.  Note that
     * the values that may be used are database-dependent.  Use this with caution!
     * <p>
     * <b>WARNING</b>: This setting is a <em>hint</em> for the database driver, and the driver
     * is <em>not</em> required to honour it!  Several databases put other restrictions on its
     * use as well.  MySQL and Postgres will definitely ignore this setting when the database
     * connection is in auto-commit mode, so you will probably have to use the {@link TransactionUtil}
     * if you want this to work.
     * </p>
     * <p>
     * <b>WARNING</b>: This value may need to be set to a database-dependent value.  For example,
     * the most useful value on MySQL is {@code Integer.MIN_VALUE} to get a streaming result
     * set, but this value will be rejected by most other databases.
     * </p>
     */
    public void setFetchSize(int fetchSize)
    {
        this.fetchSize = fetchSize;
    }


    //================================================================
    // Builder methods
    //================================================================

    /**
     * Creates a new {@link EntityFindOptions}.  This is equivalent to the
     * {@link #EntityFindOptions() default constructor} but is implemented as a static method
     * so that it can be used more conveniently by other classes which {@code static import}
     * it.
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
    public static EntityFindOptions findOptions()
    {
        return new EntityFindOptions();
    }

    /**
     * Same as using both {@link #setSpecifyTypeAndConcur(boolean) setSpecifyTypeAndConcur(true)}
     * and {@link #setResultSetType(int) setResultSetType(TYPE_FORWARD_ONLY)}.  Note that you
     * should also use either {@link #readOnly()} or {@link #updatable()} for maximum driver
     * compatibility.
     *
     * @return {@code this}, for convenient use as a chained builder
     */
    public EntityFindOptions forwardOnly()
    {
        specifyTypeAndConcur = true;
        resultSetType = TYPE_FORWARD_ONLY;
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
    public EntityFindOptions scrollSensitive()
    {
        specifyTypeAndConcur = true;
        resultSetType = TYPE_SCROLL_SENSITIVE;
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
    public EntityFindOptions scrollInsensitive()
    {
        specifyTypeAndConcur = true;
        resultSetType = TYPE_SCROLL_INSENSITIVE;
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
    public EntityFindOptions readOnly()
    {
        specifyTypeAndConcur = true;
        resultSetConcurrency = CONCUR_READ_ONLY;
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
    public EntityFindOptions updatable()
    {
        specifyTypeAndConcur = true;
        resultSetConcurrency = CONCUR_UPDATABLE;
        return this;
    }

    /** Same as {@link #setDistinct(boolean) setDistinct(true)}.
     *
     * @return {@code this}, for convenient use as a chained builder
     */
    public EntityFindOptions distinct()
    {
        distinct = true;
        return this;
    }

    /** Same as {@link #setMaxResults(int)}.
     *
     * @return {@code this}, for convenient use as a chained builder
     */
    public EntityFindOptions maxResults(int maxResults)
    {
        this.maxResults = maxResults;
        return this;
    }

    /** Same as {@link #setFetchSize(int)}.
     *
     * @return {@code this}, for convenient use as a chained builder
     */
    public EntityFindOptions fetchSize(int fetchSize)
    {
        this.fetchSize = fetchSize;
        return this;
    }
}

