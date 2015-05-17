package org.ofbiz.core.entity.jdbc;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Rule;
import org.junit.Test;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.ofbiz.core.entity.jdbc.SQLProcessor.ConnectionGuard;

import static org.junit.Assert.fail;

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
        final ConnectionGuard guard = abandonConnection();

        final AtomicBoolean success = new AtomicBoolean();
        for (int i = 1; i < ATTEMPTS; ++i)
        {
            System.gc();
            SQLProcessor fixture = new SQLProcessor("defaultDS")
            {
                @Override
                void closeAbandonedProcessor(ConnectionGuard abandoned)
                {
                    success.set(true);
                    super.closeAbandonedProcessor(abandoned);
                }
            };
            fixture.prepareStatement("SELECT 1 FROM INFORMATION_SCHEMA.SYSTEM_USERS");
            fixture.close();

            if (success.get())
            {
                System.out.println("Successful reclaimed leaked connection on attempt #" + i);
                return;
            }
        }

        System.err.println("Forcibly closing guard from the test, since it didn't get closed automatically. :(");
        guard.close();
        fail("Unsuccessful at collecting leaked connection even in " + ATTEMPTS + " attempts!");
    }

    private static ConnectionGuard abandonConnection() throws Exception
    {
        final SQLProcessor sqlProcessor = new SQLProcessor("defaultDS");
        sqlProcessor.prepareStatement("SELECT 1 FROM INFORMATION_SCHEMA.SYSTEM_USERS");
        return sqlProcessor._guard;
    }
}
