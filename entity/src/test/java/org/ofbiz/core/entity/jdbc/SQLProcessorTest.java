package org.ofbiz.core.entity.jdbc;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Nullable;

import org.junit.Rule;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.stubbing.Answer;
import org.ofbiz.core.entity.GenericEntityException;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;

/**
 * @since v1.0.65
 */
public class SQLProcessorTest
{
    private static final int ATTEMPTS = 100;

    @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Test
    public void testConnectionGuard() throws Exception
    {
        final AtomicBoolean success = abandonConnection();

        for (int i = 1; i <= ATTEMPTS; ++i)
        {
            System.gc();
            SQLProcessor fixture = new SQLProcessor("defaultDS");
            fixture.prepareStatement("SELECT 1 FROM INFORMATION_SCHEMA.SYSTEM_USERS");
            fixture.close();

            if (success.get())
            {
                System.out.println("Successful reclaimed leaked connection on attempt #" + i);
                return;
            }
        }

        fail("Unsuccessful at collecting leaked connection even in " + ATTEMPTS + " attempts!");
    }

    private static AtomicBoolean abandonConnection() throws Exception
    {
        final AtomicBoolean closed = new AtomicBoolean();
        final SQLProcessor sqlProcessor = new SQLProcessor("defaultDS")
        {
            @Override
            ConnectionGuard guard(Connection connection)
            {
                return super.guard(recordClose(connection, closed));
            }
        };
        sqlProcessor.prepareStatement("SELECT 1 FROM INFORMATION_SCHEMA.SYSTEM_USERS");
        return closed;
    }

    static Connection recordClose(final Connection rawConnection, final AtomicBoolean closed)
    {
        final Connection spyConnection = spy(rawConnection);
        try
        {
            doAnswer(new Answer<Void>()
            {
                @Nullable
                @Override
                public Void answer(InvocationOnMock invocation) throws Throwable
                {
                    closed.set(true);
                    rawConnection.close();
                    return null;
                }
            }).when(spyConnection).close();
        }
        catch (SQLException e)
        {
            throw new AssertionError(e);
        }
        return spyConnection;
    }
}
