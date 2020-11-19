package org.wildfly.extension.metrics.internal;

import static java.util.Objects.requireNonNull;

import java.io.Closeable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;


public class WildFlyMetricRegistry implements Closeable {

    /* Key is the metric name */
    private Map<String, WildFlyMetricMetadata> metadataMap = new HashMap();
    private Map<MetricID, Metric> metricMap = new TreeMap<>();

    public void register(WildFlyMetricMetadata metadata, Metric metric, MetricCollector.MetricTag... tags) {
        requireNonNull(metadata);
        requireNonNull(metric);
        requireNonNull(tags);

        MetricID metricID = new MetricID(metadata.getName(), tags);
        if (!metadataMap.containsKey(metadata.getName())) {
            metadataMap.put(metadata.getName(), metadata);
        }
        metricMap.put(metricID, metric);
    }

    public void remove(MetricID metricID) {
        metricMap.remove(metricID);
    }

    @Override
    public void close() {
        metricMap.clear();
        metadataMap.clear();
    }

    public Map<MetricID, Metric> getMetrics() {
        return metricMap;
    }

    public Map<String, WildFlyMetricMetadata> getMetricMetadata() {
        return metadataMap;
    }


    public static class MetricID implements Comparable<MetricID>{
        private final String metricName;
        private final Map<String, String> tags = new TreeMap<>();

        MetricID(String metricName, MetricCollector.MetricTag... tags) {
            this.metricName = metricName;
            for (int i = 0; i < tags.length; i++) {
                this.tags.put(tags[i].getKey(), tags[i].getValue());
            }
        }

        public String getName() {
            return metricName;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MetricID metricID = (MetricID) o;
            return metricName.equals(metricID.metricName) &&
                    tags.equals(metricID.tags);
        }

        @Override
        public int hashCode() {
            return Objects.hash(metricName, tags);
        }

        public String getTagsAsAString() {
            if (tags.isEmpty()) {
                return "";
            }
            String tagsAsString = tags.entrySet().stream().map(e -> e.getKey() + "=\"" + e.getValue() + "\"").collect(Collectors.joining(","));
            return "{" + tagsAsString + "}";
        }

        @Override
        public int compareTo(MetricID other) {
            int compareVal = this.metricName.compareTo(other.metricName);
            if (compareVal == 0) {
                compareVal = this.tags.size() - other.tags.size();
                if (compareVal == 0) {
                    Iterator<Map.Entry<String, String>> thisIterator = tags.entrySet().iterator();
                    Iterator<Map.Entry<String, String>> otherIterator = other.tags.entrySet().iterator();
                    while (thisIterator.hasNext() && otherIterator.hasNext()) {
                        Map.Entry<String, String> thisEntry = thisIterator.next();
                        Map.Entry<String, String> otherEntry = otherIterator.next();
                        compareVal = thisEntry.getKey().compareTo(otherEntry.getKey());
                        if (compareVal != 0) {
                            return compareVal;
                        } else {
                            compareVal = thisEntry.getValue().compareTo(otherEntry.getValue());
                            if (compareVal != 0) {
                                return compareVal;
                            }
                        }
                    }
                }
            }
            return compareVal;
        }
    }
}
