package org.ofbiz.core.entity.jdbc.interceptors.connection;

import com.google.common.collect.ImmutableList;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.stubbing.Answer;

import javax.annotation.Nullable;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * @since v1.0.67 / v1.1.7
 */
public class SafeDelegatingSqlConnectionInterceptorTest {
    private static final String SQL = "SELECT * FROM schema.table WHERE col1 = ? AND col2 = ?";
    private static final List<String> PARAMETERS = ImmutableList.of("hello", "world");

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private Connection connection;
    @Mock
    private ConnectionPoolState connectionPoolState;
    @Mock
    private Statement statement;
    @Mock
    private ResultSet resultSet;

    private Throwable throwMe;
    private SQLConnectionInterceptor delegate = mock(SQLConnectionInterceptor.class, new Answer<Object>() {
        @Nullable
        @Override
        public Object answer(InvocationOnMock invocation) throws Throwable {
            if (throwMe != null) {
                throw throwMe;
            }
            return null;
        }
    });
    private SafeDelegatingSqlConnectionInterceptor interceptor = new SafeDelegatingSqlConnectionInterceptor(delegate);

    @Test
    public void testDelegateThatIsWellBehaved() {
        throwMe = null;
        callAndVerifyAll();
    }

    @Test
    public void testDelegateThatThrowsRuntimeException() {
        throwMe = new IllegalArgumentException("Just testing!");
        callAndVerifyAll();
    }

    @Test
    public void testDelegateThatThrowsLinkageError() {
        throwMe = new AbstractMethodError("Just testing!");
        callAndVerifyAll();
    }

    @Test(expected = StackOverflowError.class)
    public void testDelegateThatThrowsSomeOtherError() {
        throwMe = new StackOverflowError("Just testing!");
        interceptor.onConnectionTaken(connection, connectionPoolState);
    }

    private void callAndVerifyAll() {
        interceptor.onConnectionTaken(connection, connectionPoolState);
        verify(delegate).onConnectionTaken(connection, connectionPoolState);
        interceptor.onConnectionReplaced(connection, connectionPoolState);
        verify(delegate).onConnectionReplaced(connection, connectionPoolState);
        interceptor.beforeExecution(SQL, PARAMETERS, statement);
        verify(delegate).beforeExecution(SQL, PARAMETERS, statement);
        interceptor.afterSuccessfulExecution(SQL, PARAMETERS, statement, resultSet, 42);
        verify(delegate).afterSuccessfulExecution(SQL, PARAMETERS, statement, resultSet, 42);
        final SQLException sqle = new SQLException("Just testing!");
        interceptor.onException(SQL, PARAMETERS, statement, sqle);
        verify(delegate).onException(SQL, PARAMETERS, statement, sqle);
        verifyNoMoreInteractions(delegate);
    }
}
