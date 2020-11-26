package org.wildfly.extension.microprofile.metrics;

import org.eclipse.microprofile.metrics.MetricID;

public interface MetricRegistration {
    void unregister();

    void register();

    void addRegistrationTask(Runnable task);

    void addUnregistrationTask(MetricID metricID);
}
