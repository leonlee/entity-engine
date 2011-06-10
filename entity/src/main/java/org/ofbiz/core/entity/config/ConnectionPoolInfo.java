package org.ofbiz.core.entity.config;

/**
 * Connection pool configuration
 */
public class ConnectionPoolInfo
{
    private final int maxSize;
    private final int minSize;
    private final long sleepTime;
    private final long lifeTime;
    private final long deadLockMaxWait;
    private final long deadLockRetryWait;
    private final String validationQuery;
    private final Long minEvictableTimeMillis;
    private final Long timeBetweenEvictionRunsMillis;

    public ConnectionPoolInfo(Integer maxSize, Integer minSize, long sleepTime, long lifeTime, long deadLockMaxWait,
            long deadLockRetryWait, String validationQuery, Long minEvictableTimeMillis, Long timeBetweenEvictionRunsMillis)
    {
        this.maxSize = maxSize;
        this.minSize = minSize;
        this.sleepTime = sleepTime;
        this.lifeTime = lifeTime;
        this.deadLockMaxWait = deadLockMaxWait;
        this.deadLockRetryWait = deadLockRetryWait;
        this.validationQuery = validationQuery;
        this.minEvictableTimeMillis = minEvictableTimeMillis;
        this.timeBetweenEvictionRunsMillis = timeBetweenEvictionRunsMillis;
    }

    public Integer getMaxSize()
    {
        return maxSize;
    }

    public Integer getMinSize()
    {
        return minSize;
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
}
