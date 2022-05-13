package org.ofbiz.core.entity.metrics;

import com.atlassian.util.profiling.Ticker;
import io.micrometer.core.instrument.Timer;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.sql.Connection;
import java.sql.SQLException;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.times;

@RunWith(MockitoJUnitRunner.class)
public class TestMetricEmittingConnection {
    @Rule
    public final ProfilingMetricsRule profiling = new ProfilingMetricsRule();

    @Mock
    Ticker ticker;
    @Mock
    Connection connection;

    @Test
    public void delegateAndTickerAreClosed_whenConnectionIsClosed() throws SQLException {
        final MetricEmittingConnection underTest = new MetricEmittingConnection(this.connection, ticker);

        underTest.close();
        Mockito.verify(connection, times(1)).close();
        Mockito.verify(ticker, times(1)).close();
    }

    @Test
    public void testConnectionEmitsMetric() throws SQLException {
        final Connection underTest = MetricEmittingConnection.wrapWithMetrics(connection);
        underTest.close();

        final Timer timer = profiling.registry.get("start.ofbiz.timer").timer();
        assertThat(timer.count(), equalTo(1L));
    }
}
