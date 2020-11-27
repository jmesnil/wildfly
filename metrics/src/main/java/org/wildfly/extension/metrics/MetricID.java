/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.wildfly.extension.metrics;

import java.util.Arrays;
import java.util.Objects;

public class MetricID {
    private final String metricName;
    private final WildFlyMetricMetadata.MetricTag[] tags;

    public MetricID(String metricName, WildFlyMetricMetadata.MetricTag[] tags) {
        this.metricName = metricName;
        this.tags = tags;
    }

    public String getMetricName() {
        return metricName;
    }
    public WildFlyMetricMetadata.MetricTag[] getTags() {
        return tags;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MetricID metricID = (MetricID) o;
        return Objects.equals(metricName, metricID.metricName) &&
                Arrays.equals(tags, metricID.tags);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(metricName);
        result = 31 * result + Arrays.hashCode(tags);
        return result;
    }
}