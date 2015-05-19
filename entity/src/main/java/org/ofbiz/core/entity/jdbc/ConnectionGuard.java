package org.ofbiz.core.entity.jdbc;

import java.io.Closeable;
import java.lang.ref.PhantomReference;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.concurrent.ThreadSafe;

import org.ofbiz.core.util.Debug;

/**
 * A guardian for possibly leaked {@code SQLProcessor} instances.
 * <p>
 * A {@link SQLProcessor} must be {@link SQLProcessor#close() closed} when the caller is done with, or
 * a database connection can be leaked.  This guards the {@code SQLProcessor} with a phantom reference
 * to ensure that it gets closed and an error message gets logged if this every happens.
 * </p>
 *
 * @since v1.0.65
 */
@ThreadSafe
class ConnectionGuard extends PhantomReference<SQLProcessor> implements Closeable
{
    /**
     * Map for holding existing connection guards.
     * <p>
     * This is necessary to guarantee that a leaked connection does actually become <em>phantom-reachable</em>,
     * which in turn is necessary to guarantee that it gets enqueued.  Note that {@code ConnectionGuard} does
     * not override {@code equals} or {@code hashCode}.  This is intentional, as this map needs to operate on
     * identity.
     * </p>
     */
    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")  // Not what I'm using it for...
    private static final ConcurrentMap<ConnectionGuard,ConnectionGuard> GUARDS = new ConcurrentHashMap<>(64);

    /**
     * A reference queue for holding the phantom reference guards for the connection that were cleared by the GC
     * rather than an explicit close.  This queue should always be empty; finding a reference in it indicates
     * a serious programming error.
     */
    static final ReferenceQueue<SQLProcessor> ABANDONED = new ReferenceQueue<>();

    private final AtomicReference<Connection> connectionRef;
    private volatile String sql;

    private ConnectionGuard(SQLProcessor owner, Connection connection)
    {
        super(owner, ABANDONED);
        this.connectionRef = new AtomicReference<>(connection);
    }

    /**
     * Registers a guard for the association between the lifecycle of the given {@code SQLProcessor} with the
     * given database {@code Connection}.
     * <p>
     * If this guard is not explicitly {@link #close() closed}, and if at some later time the {@code SQLProcessor}
     * is garbage collected, then the {@code ConnectionGuard} will log an error and close the connection.  The
     * {@code SQLProcessor} should call {@link #setSql(String)} whenever the {@code SQLProcessor} creates a new
     * {@code PreparedStatement} (or {@code Statement}) so that this information will be available for debugging
     * purposes in the event of a connection leak.
     * </p>
     *
     * @param owner the {@code SQLProcessor} that allocated the connection
     * @param connection the allocated connection
     * @return the newly created connection guard
     */
    static ConnectionGuard register(SQLProcessor owner, Connection connection)
    {
        final ConnectionGuard guard = new ConnectionGuard(owner, connection);
        GUARDS.put(guard, guard);
        return guard;
    }

    /**
     * Called by {@link SQLProcessor#close()} to confirm that the guard is no longer needed.
     * This clears all internal references, which prevents the reference from getting cleared by the GC
     * instead.  This in turn prevents it from ever showing up in {@link #ABANDONED}.
     */
    @Override
    public void clear()
    {
        GUARDS.remove(this);
        connectionRef.set(null);
        sql = null;
        super.clear();
    }

    /**
     * Called by {@link #closeAbandonedProcessors()} if this reference is found in the
     * {@link #ABANDONED} reference queue, which means it must have been collected by the GC instead
     * of by a proper call to {@link SQLProcessor#close()}.
     */
    @Override
    public void close()
    {
        GUARDS.remove(this);
        final Connection connection = connectionRef.getAndSet(null);
        if (connection != null)
        {
            close(connection);
        }
    }

    private void close(Connection connection)
    {
        Debug.logError("!!! ABANDONED SQLProcessor DETECTED !!!" +
                "\n\tThis probably means that somebody forgot to close an EntityListIterator." +
                "\n\tConnection: " + connection +
                "\n\tSQL: " + sql, SQLProcessor.module);
        try
        {
            connection.close();
        }
        catch (SQLException sqle)
        {
            Debug.logError(sqle, "ConnectionGuard.close() failed", SQLProcessor.module);
        }
    }

    void setSql(String sql)
    {
        this.sql = sql;
    }

    @SuppressWarnings("CastToConcreteClass")  // Have to; the reference queue isn't more specific than that.
    static void closeAbandonedProcessors()
    {
        Reference<? extends SQLProcessor> abandoned = ABANDONED.poll();
        while (abandoned != null)
        {
            ((ConnectionGuard)abandoned).close();
            abandoned = ABANDONED.poll();
        }
    }

    @Override
    public String toString()
    {
        return "ConnectionGuard[connection=" + connectionRef.get() + ",sql=" + sql + ']';
    }
}
