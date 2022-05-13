package org.ofbiz.core.entity.metrics;

import com.atlassian.util.profiling.Metrics;
import com.atlassian.util.profiling.MetricsFilter;
import com.atlassian.util.profiling.micrometer.MicrometerStrategy;
import com.atlassian.util.profiling.strategy.MetricStrategy;
import io.micrometer.core.instrument.MockClock;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

import static com.atlassian.util.profiling.Metrics.getConfiguration;
import static com.atlassian.util.profiling.StrategiesRegistry.addMetricStrategy;
import static com.atlassian.util.profiling.StrategiesRegistry.removeMetricStrategy;
import static io.micrometer.core.instrument.simple.SimpleConfig.DEFAULT;

/**
 * A JUnit rule for testing Profiling code.
 * It sets up atlassian-profiling with Micrometer {@link SimpleMeterRegistry} around each test.
 *
 * @since 2.0
 */
public class ProfilingMetricsRule extends TestWatcher {

    public final MockClock clock = new MockClock();
    public final SimpleMeterRegistry registry = new SimpleMeterRegistry(DEFAULT, clock);
    private final MetricStrategy strategy = new MicrometerStrategy(registry);
    private final MetricsFilter filter;

    public ProfilingMetricsRule() {
        this.filter = Metrics.getConfiguration().getFilter();
    }

    @Override
    protected void starting(Description description) {
        addMetricStrategy(strategy);
        getConfiguration().setEnabled(true);
        getConfiguration().setFilter(MetricsFilter.ACCEPT_ALL);
    }

    @Override
    protected void finished(Description description) {
        removeMetricStrategy(strategy);
        getConfiguration().setFilter(filter);
    }
}
