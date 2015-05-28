package org.ofbiz.core.entity.config;

/**
 * Connection pool configuration
 */
public class ConnectionPoolInfo
{
    public static final int DEFAULT_POOL_MAX_SIZE = 8;  // maxActive
    public static final int DEFAULT_POOL_MIN_SIZE = 2;  // minIdle
    public static final long DEFAULT_POOL_MAX_WAIT = 60000L;
    public static final long DEFAULT_POOL_SLEEP_TIME = 300000L;
    public static final long DEFAULT_POOL_LIFE_TIME = 600000L;
    public static final long DEFAULT_DEADLOCK_MAX_WAIT = 600000L;
    public static final long DEFAULT_DEADLOCK_RETRY_WAIT = 10000L;

    private final int maxSize;  // maxActive for DBCP
    private final int minSize;  // minIdle for DBCP
    private final int maxIdle;
    private final long maxWait;

    // XAPool settings (ignored by DBCP)
    private final long sleepTime;
    private final long lifeTime;
    private final long deadLockMaxWait;
    private final long deadLockRetryWait;

    // Standard optional settings for DBCP
    private final Long minEvictableTimeMillis;
    private final Long timeBetweenEvictionRunsMillis;

    // Advanced optional settings for DBCP
    private final String defaultCatalog;
    private final Integer initialSize;
    private final Integer maxOpenPreparedStatements;
    private final Integer numTestsPerEvictionRun;
    private final Boolean poolPreparedStatements;
    /**
     * @deprecated Since 1.2.x use {@link #removeAbandonedOnBorrow}
     */
    private final Boolean removeAbandoned;
    private final Boolean removeAbandonedOnBorrow;
    private final Boolean removeAbandonedOnMaintanance;
    private final Integer removeAbandonedTimeout;
    private final Boolean testOnBorrow;
    private final Boolean testOnReturn;
    private final Boolean testWhileIdle;
    private final Integer validationQueryTimeout;
    private final String validationQuery;

    public static Builder builder()
    {
        return new Builder();
    }

    public ConnectionPoolInfo(Integer maxSize, Integer minSize, Long maxWait, long sleepTime, long lifeTime, long deadLockMaxWait,
            long deadLockRetryWait, String validationQuery, Long minEvictableTimeMillis, Long timeBetweenEvictionRunsMillis)
    {
        this(new Builder()
            .setPoolMaxSize(maxSize)
            .setPoolMinSize(minSize)
            .setPoolMaxWait(maxWait)
            .setPoolSleepTime(sleepTime)
            .setPoolLifeTime(lifeTime)
            .setDeadLockMaxWait(deadLockMaxWait)
            .setDeadLockRetryWait(deadLockRetryWait)
            .setValidationQuery(validationQuery)
            .setMinEvictableTimeMillis(minEvictableTimeMillis)
            .setTimeBetweenEvictionRunsMillis(timeBetweenEvictionRunsMillis));
    }

    ConnectionPoolInfo(final Builder builder)
    {
        deadLockMaxWait = longWithDefault(builder.getDeadLockMaxWait(), DEFAULT_DEADLOCK_MAX_WAIT);
        deadLockRetryWait = longWithDefault(builder.getDeadLockRetryWait(), DEFAULT_DEADLOCK_RETRY_WAIT);
        defaultCatalog = builder.getDefaultCatalog();
        initialSize = builder.getPoolInitialSize();
        lifeTime = longWithDefault(builder.getPoolLifeTime(), DEFAULT_POOL_LIFE_TIME);
        maxOpenPreparedStatements = builder.getMaxOpenPreparedStatements();
        maxSize = intWithDefault(builder.getPoolMaxSize(), DEFAULT_POOL_MAX_SIZE);
        maxIdle = intWithDefault(builder.getPoolMaxIdle(), maxSize);
        maxWait = longWithDefault(builder.getPoolMaxWait(), DEFAULT_POOL_MAX_WAIT);
        minEvictableTimeMillis = builder.getMinEvictableTimeMillis();
        minSize = intWithDefault(builder.getPoolMinSize(), DEFAULT_POOL_MIN_SIZE);
        numTestsPerEvictionRun = builder.getNumTestsPerEvictionRun();
        poolPreparedStatements = builder.getPoolPreparedStatements();
        removeAbandoned = builder.getRemoveAbandoned();
        removeAbandonedTimeout = builder.getRemoveAbandonedTimeout();
        removeAbandonedOnBorrow = builder.getRemoveAbandonedOnBorrow();
        removeAbandonedOnMaintanance = builder.getRemoveAbandonedOnMaintenance();
        sleepTime = longWithDefault(builder.getPoolSleepTime(), DEFAULT_POOL_SLEEP_TIME);
        testOnBorrow = builder.getTestOnBorrow();
        testOnReturn = builder.getTestOnReturn();
        testWhileIdle = builder.getTestWhileIdle();
        timeBetweenEvictionRunsMillis = builder.getTimeBetweenEvictionRunsMillis();
        validationQuery = builder.getValidationQuery();
        validationQueryTimeout = builder.getValidationQueryTimeout();
    }

    public Builder toBuilder()
    {
        return builder()
                .setDeadLockMaxWait(nullIfDefault(deadLockMaxWait, DEFAULT_DEADLOCK_MAX_WAIT))
                .setDeadLockRetryWait(nullIfDefault(deadLockRetryWait, DEFAULT_DEADLOCK_RETRY_WAIT))
                .setDefaultCatalog(defaultCatalog)
                .setPoolInitialSize(initialSize)
                .setPoolLifeTime(nullIfDefault(lifeTime, DEFAULT_POOL_LIFE_TIME))
                .setMaxOpenPreparedStatements(maxOpenPreparedStatements)
                .setPoolMaxSize(maxSize)
                .setPoolMaxIdle(nullIfDefault(maxIdle, maxSize))
                .setPoolMaxWait(nullIfDefault(maxWait, DEFAULT_POOL_MAX_WAIT))
                .setMinEvictableTimeMillis(minEvictableTimeMillis)
                .setPoolMinSize(nullIfDefault(minSize, DEFAULT_POOL_MIN_SIZE))
                .setNumTestsPerEvictionRun(numTestsPerEvictionRun)
                .setPoolPreparedStatements(poolPreparedStatements)
                .setRemoveAbandoned(removeAbandoned)
                .setRemoveAbandonedTimeout(removeAbandonedTimeout)
                .setRemoveAbandonedOnBorrow(removeAbandonedOnBorrow)
                .setRemoveAbandonedOnMaintenance(removeAbandonedOnMaintanance)
                .setPoolSleepTime(nullIfDefault(sleepTime, DEFAULT_POOL_SLEEP_TIME))
                .setTestOnBorrow(testOnBorrow)
                .setTestOnReturn(testOnReturn)
                .setTestWhileIdle(testWhileIdle)
                .setTimeBetweenEvictionRunsMillis(timeBetweenEvictionRunsMillis)
                .setValidationQuery(validationQuery)
                .setValidationQueryTimeout(validationQueryTimeout);
    }

    private Integer nullIfDefault(int value, int defaultValue)
    {
        return (value != defaultValue) ? value : null;
    }

    private int intWithDefault(Integer value, int defaultValue)
    {
        return (value != null) ? value : defaultValue;
    }

    private Long nullIfDefault(long value, long defaultValue)
    {
        return (value != defaultValue) ? value : null;
    }

    private long longWithDefault(Long value, long defaultValue)
    {
        return (value != null) ? value : defaultValue;
    }

    // Becomes maxActive
    public int getMaxSize()
    {
        return maxSize;
    }

    // Becomes minIdle
    public int getMinSize()
    {
        return minSize;
    }

    public int getMaxIdle()
    {
        return maxIdle;
    }

    public Integer getInitialSize()
    {
        return initialSize;
    }

    public long getMaxWait()
    {
        return maxWait;
    }

    public long getSleepTime()
    {
        return sleepTime;
    }

    public long getLifeTime()
    {
        return lifeTime;
    }

    public long getDeadLockMaxWait()
    {
        return deadLockMaxWait;
    }

    public long getDeadLockRetryWait()
    {
        return deadLockRetryWait;
    }

    public String getValidationQuery()
    {
        return validationQuery;
    }

    public Long getMinEvictableTimeMillis()
    {
        return minEvictableTimeMillis;
    }

    public Long getTimeBetweenEvictionRunsMillis()
    {
        return timeBetweenEvictionRunsMillis;
    }

    public Boolean getPoolPreparedStatements()
    {
        return poolPreparedStatements;
    }

    public Boolean getRemoveAbandonedOnBorrow()
    {
        return removeAbandonedOnBorrow;
    }
    public Boolean getRemoveAbandonedOnMaintanance()
    {
        return removeAbandonedOnMaintanance;
    }

    /**
     * @deprecated Since 1.2.x use {@link #removeAbandonedOnBorrow}
     */
    @Deprecated
    public Boolean getRemoveAbandoned()
    {
        return removeAbandoned;
    }

    public Boolean getTestOnBorrow()
    {
        return testOnBorrow;
    }

    public Boolean getTestOnReturn()
    {
        return testOnReturn;
    }

    public Boolean getTestWhileIdle()
    {
        return testWhileIdle;
    }

    public Integer getMaxOpenPreparedStatements()
    {
        return maxOpenPreparedStatements;
    }

    public Integer getNumTestsPerEvictionRun()
    {
        return numTestsPerEvictionRun;
    }

    public Integer getRemoveAbandonedTimeout()
    {
        return removeAbandonedTimeout;
    }

    public Integer getValidationQueryTimeout()
    {
        return validationQueryTimeout;
    }

    public String getDefaultCatalog()
    {
        return defaultCatalog;
    }

    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append("ConnectionPoolInfo");
        sb.append("{maxSize=").append(maxSize);
        sb.append(", minSize=").append(minSize);
        sb.append(", initialSize=").append(initialSize);
        sb.append(", maxIdle=").append(maxIdle);
        sb.append(", maxWait=").append(maxWait);
        sb.append(", sleepTime=").append(sleepTime);
        sb.append(", lifeTime=").append(lifeTime);
        sb.append(", deadLockMaxWait=").append(deadLockMaxWait);
        sb.append(", deadLockRetryWait=").append(deadLockRetryWait);
        sb.append(", validationQuery=");
        if (validationQuery != null)
        {
            sb.append('\'').append(validationQuery).append('\'');
        }
        else
        {
            sb.append("null");
        }
        sb.append(", minEvictableTimeMillis=").append(minEvictableTimeMillis);
        sb.append(", timeBetweenEvictionRunsMillis=").append(timeBetweenEvictionRunsMillis);
        sb.append(", poolPreparedStatements=").append(poolPreparedStatements);
        sb.append(", testOnBorrow=").append(testOnBorrow);
        sb.append(", testOnReturn=").append(testOnReturn);
        sb.append(", testWhileIdle=").append(testWhileIdle);
        sb.append(", maxOpenPreparedStatements=").append(maxOpenPreparedStatements);
        sb.append(", numTestsPerEvictionRun=").append(numTestsPerEvictionRun);
        sb.append(", removeAbandoned=").append(removeAbandoned);
        sb.append(", removeAbandonedOnBorrow=").append(removeAbandonedOnBorrow);
        sb.append(", removeAbandonedOnMaintanance=").append(removeAbandonedOnMaintanance);
        sb.append(", removeAbandonedTimeout=").append(removeAbandonedTimeout);
        sb.append(", validationQueryTimeout=").append(validationQueryTimeout);
        sb.append(", defaultCatalog=").append(defaultCatalog);
        sb.append('}');
        return sb.toString();
    }

    @Override
    public boolean equals(Object o)
    {
        if (! (o instanceof ConnectionPoolInfo))
        {
            return false;
        }
        final ConnectionPoolInfo other = (ConnectionPoolInfo)o;
        return maxIdle == other.maxIdle &&
               maxSize == other.maxSize &&
               maxWait == other.maxWait &&
               minSize == other.minSize &&
               lifeTime == other.lifeTime &&
               sleepTime == other.sleepTime &&
               equals(deadLockMaxWait, other.deadLockMaxWait) &&
               equals(deadLockRetryWait, other.deadLockRetryWait) &&
               equals(defaultCatalog, other.defaultCatalog) &&
               equals(maxOpenPreparedStatements, other.maxOpenPreparedStatements) &&
               equals(minEvictableTimeMillis, other.minEvictableTimeMillis) &&
               equals(numTestsPerEvictionRun, other.numTestsPerEvictionRun) &&
               equals(initialSize, other.initialSize) &&
               equals(removeAbandoned, other.removeAbandoned) &&
               equals(removeAbandonedOnBorrow, other.removeAbandonedOnBorrow) &&
               equals(removeAbandonedOnMaintanance, other.removeAbandonedOnMaintanance) &&
               equals(removeAbandonedTimeout, other.removeAbandonedTimeout) &&
               equals(testOnBorrow, other.testOnBorrow) &&
               equals(testOnReturn, other.testOnReturn) &&
               equals(testWhileIdle, other.testWhileIdle) &&
               equals(timeBetweenEvictionRunsMillis, other.timeBetweenEvictionRunsMillis) &&
               equals(validationQuery, other.validationQuery) &&
               equals(validationQueryTimeout, other.validationQueryTimeout);
    }

    @Override
    public int hashCode()
    {
        int h = 0x1F2E3D4C;
        h = hash(h, deadLockMaxWait);
        h = hash(h, deadLockRetryWait);
        h = hash(h, defaultCatalog);
        h = hash(h, maxOpenPreparedStatements);
        h = hash(h, minEvictableTimeMillis);
        h = hash(h, numTestsPerEvictionRun);
        h = hash(h, initialSize);
        h = hash(h, lifeTime);
        h = hash(h, maxIdle);
        h = hash(h, maxSize);
        h = hash(h, maxWait);
        h = hash(h, minSize);
        h = hash(h, sleepTime);
        h = hash(h, removeAbandoned);
        h = hash(h, removeAbandonedOnBorrow);
        h = hash(h, removeAbandonedOnMaintanance);
        h = hash(h, removeAbandonedTimeout);
        h = hash(h, testOnBorrow);
        h = hash(h, testOnReturn);
        h = hash(h, testWhileIdle);
        h = hash(h, timeBetweenEvictionRunsMillis);
        h = hash(h, validationQueryTimeout);
        h = hash(h, validationQuery);
        return h;
    }

    private static int hash(final int h, final Object o)
    {
        return h * 17 + ((o != null) ? o.hashCode() : 0);
    }

    private static <T> boolean equals(T o1, T o2)
    {
        return (o1 != null) ? o1.equals(o2) : (o2 == null);
    }

    /**
     * This is a builder class for constructing a <tt>ConnectionPoolInfo</tt> manually.
     * Use the {@link ConnectionPoolInfo#builder()} factory to obtain an instance
     * of this class and call the various <tt>setXyzzy</tt> methods to
     * populate the fields.  Those that are left unset or are explicitly
     * set to <tt>null</tt> will use default values.
     */
    public static class Builder
    {
        private Boolean poolPreparedStatements;
        /**
         * @deprecated Since 1.2.x use {@link #removeAbandonedOnBorrow}
         */
        @Deprecated
        private Boolean removeAbandoned;
        private Boolean removeAbandonedOnBorrow;
        private Boolean removeAbandonedOnMaintenance;
        private Boolean testOnBorrow;
        private Boolean testOnReturn;
        private Boolean testWhileIdle;
        private Integer poolMinSize;
        private Integer poolMaxSize;
        private Integer poolInitialSize;
        private Integer poolMaxIdle;
        private Long poolMaxWait;
        private Long poolSleepTime;
        private Long poolLifeTime;
        private Long deadLockMaxWait;
        private Long deadLockRetryWait;
        private Integer maxOpenPreparedStatements;
        private Integer numTestsPerEvictionRun;
        private Integer removeAbandonedTimeout;
        private Integer validationQueryTimeout;
        private String validationQuery;
        private String defaultCatalog;
        private Long minEvictableTimeMillis;
        private Long timeBetweenEvictionRunsMillis;

        /**
         * Returns a new <tt>ConnectionPoolInfo</tt> as specified by the current state
         * of this builder.
         *
         * @return the new <tt>ConnectionPoolInfo</tt>
         * @throws IllegalArgumentException if the configuration is invalid
         */
        public ConnectionPoolInfo build()
        {
            return new ConnectionPoolInfo(this);
        }


        public String getValidationQuery()
        {
            return validationQuery;
        }

        public Builder setValidationQuery(String validationQuery)
        {
            this.validationQuery = ignoreBlank(validationQuery);
            return this;
        }

        public Long getMinEvictableTimeMillis()
        {
            return minEvictableTimeMillis;
        }

        public Builder setMinEvictableTimeMillis(Long minEvictableTimeMillis)
        {
            this.minEvictableTimeMillis = minEvictableTimeMillis;
            return this;
        }

        public Long getTimeBetweenEvictionRunsMillis()
        {
            return timeBetweenEvictionRunsMillis;
        }

        public Builder setTimeBetweenEvictionRunsMillis(Long timeBetweenEvictionRunsMillis)
        {
            this.timeBetweenEvictionRunsMillis = timeBetweenEvictionRunsMillis;
            return this;
        }

        public Long getPoolMaxWait()
        {
            return poolMaxWait;
        }

        public Builder setPoolMaxWait(Long poolMaxWait)
        {
            this.poolMaxWait = poolMaxWait;
            return this;
        }

        public Integer getPoolMinSize()
        {
            return poolMinSize;
        }

        public Builder setPoolMinSize(Integer poolMinSize)
        {
            this.poolMinSize = poolMinSize;
            return this;
        }

        public Integer getPoolMaxSize()
        {
            return poolMaxSize;
        }

        public Builder setPoolMaxSize(Integer poolMaxSize)
        {
            this.poolMaxSize = poolMaxSize;
            return this;
        }

        public Integer getPoolMaxIdle()
        {
            return poolMaxIdle;
        }

        public Builder setPoolMaxIdle(Integer poolMaxIdle)
        {
            this.poolMaxIdle = poolMaxIdle;
            return this;
        }

        public Integer getPoolInitialSize()
        {
            return poolInitialSize;
        }

        public Builder setPoolInitialSize(Integer poolInitialSize)
        {
            this.poolInitialSize = poolInitialSize;
            return this;
        }

        public Boolean getPoolPreparedStatements()
        {
            return poolPreparedStatements;
        }

        public Builder setPoolPreparedStatements(Boolean poolPreparedStatements)
        {
            this.poolPreparedStatements = poolPreparedStatements;
            return this;
        }

        /**
         * @deprecated Since 1.2.x use {@link #getRemoveAbandonedOnBorrow}
         */
        @Deprecated
        public Boolean getRemoveAbandoned()
        {
            return removeAbandoned;
        }

        /**
         * @deprecated Since 1.2.x use {@link #setRemoveAbandonedOnBorrow}
         */
        @Deprecated
        public Builder setRemoveAbandoned(Boolean removeAbandoned)
        {
            this.removeAbandoned = removeAbandoned;
            return this;
        }

        public Boolean getRemoveAbandonedOnBorrow() { return removeAbandonedOnBorrow; }

        public Builder setRemoveAbandonedOnBorrow(Boolean removeAbandonedOnBorrow)
        {
            this.removeAbandonedOnBorrow = removeAbandonedOnBorrow;
            return this;
        }

        public Boolean getRemoveAbandonedOnMaintenance() { return removeAbandonedOnMaintenance; }

        public Builder setRemoveAbandonedOnMaintenance(Boolean removeAbandonedOnMaintenance) {
            this.removeAbandonedOnMaintenance = removeAbandonedOnMaintenance;
            return this;
        }

        public Boolean getTestOnBorrow()
        {
            return testOnBorrow;
        }

        public Builder setTestOnBorrow(Boolean testOnBorrow)
        {
            this.testOnBorrow = testOnBorrow;
            return this;
        }

        public Boolean getTestOnReturn()
        {
            return testOnReturn;
        }

        public Builder setTestOnReturn(Boolean testOnReturn)
        {
            this.testOnReturn = testOnReturn;
            return this;
        }

        public Boolean getTestWhileIdle()
        {
            return testWhileIdle;
        }

        public Builder setTestWhileIdle(Boolean testWhileIdle)
        {
            this.testWhileIdle = testWhileIdle;
            return this;
        }

        public Integer getMaxOpenPreparedStatements()
        {
            return maxOpenPreparedStatements;
        }

        public Builder setMaxOpenPreparedStatements(Integer maxOpenPreparedStatements)
        {
            this.maxOpenPreparedStatements = maxOpenPreparedStatements;
            return this;
        }

        public Integer getNumTestsPerEvictionRun()
        {
            return numTestsPerEvictionRun;
        }

        public Builder setNumTestsPerEvictionRun(Integer numTestsPerEvictionRun)
        {
            this.numTestsPerEvictionRun = numTestsPerEvictionRun;
            return this;
        }

        public Integer getRemoveAbandonedTimeout()
        {
            return removeAbandonedTimeout;
        }

        public Builder setRemoveAbandonedTimeout(Integer removeAbandonedTimeout)
        {
            this.removeAbandonedTimeout = removeAbandonedTimeout;
            return this;
        }

        public Integer getValidationQueryTimeout()
        {
            return validationQueryTimeout;
        }

        public Builder setValidationQueryTimeout(Integer validationQueryTimeout)
        {
            this.validationQueryTimeout = validationQueryTimeout;
            return this;
        }

        public String getDefaultCatalog()
        {
            return defaultCatalog;
        }

        public Builder setDefaultCatalog(String defaultCatalog)
        {
            this.defaultCatalog = ignoreBlank(defaultCatalog);
            return this;
        }

        public Long getPoolSleepTime()
        {
            return poolSleepTime;
        }

        public Builder setPoolSleepTime(Long poolSleepTime)
        {
            this.poolSleepTime = poolSleepTime;
            return this;
        }

        public Long getPoolLifeTime()
        {
            return poolLifeTime;
        }

        public Builder setPoolLifeTime(Long poolLifeTime)
        {
            this.poolLifeTime = poolLifeTime;
            return this;
        }

        public Long getDeadLockMaxWait()
        {
            return deadLockMaxWait;
        }

        public Builder setDeadLockMaxWait(Long deadLockMaxWait)
        {
            this.deadLockMaxWait = deadLockMaxWait;
            return this;
        }

        public Long getDeadLockRetryWait()
        {
            return deadLockRetryWait;
        }

        public Builder setDeadLockRetryWait(Long deadLockRetryWait)
        {
            this.deadLockRetryWait = deadLockRetryWait;
            return this;
        }
        
        private String ignoreBlank(String s)
        {
            if (s != null)
            {
                s = s.trim();
                if (s.length() > 0)
                {
                    return s;
                }
            }
            return null;
        }

        // These are "expensive" but provided for completeness.
        @Override
        public boolean equals(Object o)
        {
            return o instanceof Builder && build().equals(((Builder)o).build());
        }

        @Override
        public int hashCode()
        {
            return build().hashCode();
        }
    }
}

