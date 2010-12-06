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

    public ConnectionPoolInfo(final int maxSize, final int minSize, final long sleepTime, final long lifeTime,
            final long deadLockMaxWait, final long deadLockRetryWait)
    {
        this.maxSize = maxSize;
        this.minSize = minSize;
        this.sleepTime = sleepTime;
        this.lifeTime = lifeTime;
        this.deadLockMaxWait = deadLockMaxWait;
        this.deadLockRetryWait = deadLockRetryWait;
    }

    public int getMaxSize()
    {
        return maxSize;
    }

    public int getMinSize()
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
}
