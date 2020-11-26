package org.wildfly.extension.microprofile.metrics;

public interface MetricRegistration {
    void unregister();

    void register();

    void addRegistrationTask(Runnable task);

    void addUnregistrationTask(MetricID metricID);

    void registerMetric(WildFlyMetric metric, WildFlyMetricMetadata metadata);
}
