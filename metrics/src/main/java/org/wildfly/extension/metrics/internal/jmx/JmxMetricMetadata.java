package org.wildfly.extension.metrics.internal.jmx;

import java.util.List;

import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.wildfly.extension.metrics.internal.MetricCollector;
import org.wildfly.extension.metrics.internal.WildFlyMetricMetadata;

public class JmxMetricMetadata extends WildFlyMetricMetadata {
    private final String mbean;
    private List<MetricCollector.MetricTag> tags;

    public JmxMetricMetadata(String name, String description, MeasurementUnit unit, Type type, String mbean, List<MetricCollector.MetricTag> tags) {
        super(name, description, unit, type);
        this.mbean = mbean;
        this.tags = tags;
    }

    public String getMBean() {
        return mbean;
    }

    public List<MetricCollector.MetricTag> getTags() {
        return tags;
    }
}
