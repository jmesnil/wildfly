package org.wildfly.extension.metrics.internal;

import java.util.HashSet;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.Set;

import org.jboss.as.controller.client.helpers.MeasurementUnit;

public class PrometheusExporter {

    private static final String LF = "\n";

    public String export(WildFlyMetricRegistry registry) {
        Set<String> alreadyExportedMetrics = new HashSet<String>();

        StringBuilder out = new StringBuilder();

        for (Map.Entry<WildFlyMetricRegistry.MetricID, WildFlyMetric> entry : registry.getMetrics().entrySet()) {
            WildFlyMetricRegistry.MetricID metricID = entry.getKey();
            String metricName = metricID.getName();
            WildFlyMetricMetadata metadata = registry.getMetricMetadata().get(metricName);
            String prometheusMetricName = toPrometheusMetricName(metricID, metadata);
            OptionalDouble metricValue = entry.getValue().getValue();
            // if the metric does not return a value, we skip printing the HELP and TYPE
            if (!metricValue.isPresent()) {
                break;
            }
            if (!alreadyExportedMetrics.contains(metricName)) {
                out.append("# HELP " + prometheusMetricName + " " + metadata.getDescription());
                out.append(LF);
                out.append("# TYPE " + prometheusMetricName + " " + metadata.getType());
                out.append(LF);
                alreadyExportedMetrics.add(metricName);
            }
            double scaledValue = scaleToBaseUnit(metricValue.getAsDouble(), metadata.getUnit());
            out.append(prometheusMetricName + metricID.getTagsAsAString() + " " + scaledValue);
            out.append(LF);
        }

        return out.toString();
    }

    private static double scaleToBaseUnit(double value, MeasurementUnit unit) {
        return value * MeasurementUnit.calculateOffset(unit, unit.getBaseUnits());
    }

    private String toPrometheusMetricName(WildFlyMetricRegistry.MetricID metricID, WildFlyMetricMetadata metadata) {
        String prometheusName = metricID.getName();
        // change the Prometheus name depending on type and measurement unit
        if (metadata.getType() == WildFlyMetricMetadata.Type.Counter) {
            prometheusName += "_total";
        } else {
            // if it's a gauge, let's add the base unit to the prometheus name
            String baseUnit = metadata.getBaseMetricUnit();
            if (baseUnit != "none") {
                prometheusName += "_" + baseUnit;
            }
        }
        return prometheusName;
    }
}
