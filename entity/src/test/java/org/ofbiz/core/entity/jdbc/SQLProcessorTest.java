package org.ofbiz.core.entity.jdbc;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Rule;
import org.junit.Test;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static org.junit.Assert.fail;

/**
 * @since v1.0.65
 */
public class SQLProcessorTest
{
    private static final int ATTEMPTS = 1000;

    @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Test
    public void testFinalization() throws Exception
    {
        final AtomicBoolean success = abandonConnection();

        for (int i = 1; i < ATTEMPTS; ++i)
        {
            System.gc();
            Thread.yield();
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
            void closeConnection()
            {
                closed.set(true);
                super.closeConnection();
            }
        };
        sqlProcessor.prepareStatement("SELECT 1 FROM INFORMATION_SCHEMA.SYSTEM_USERS");
        return closed;
    }
}