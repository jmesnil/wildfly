/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
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
package org.wildfly.extension.microprofile.metrics;

import static org.wildfly.extension.microprofile.metrics.WildFlyMetricMetadata.Type.COUNTER;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;

import io.smallrye.metrics.ExtendedMetadata;
import io.smallrye.metrics.MetricRegistries;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Gauge;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.Metric;
import org.eclipse.microprofile.metrics.MetricID;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.MetricType;
import org.eclipse.microprofile.metrics.MetricUnits;
import org.eclipse.microprofile.metrics.Tag;
import org.jboss.as.controller.client.helpers.MeasurementUnit;

public class MicroProfileMetricRegistration implements MetricRegistration {

    private final List<Runnable> registrationTasks = new ArrayList<>();
    private final List<MetricID> unregistrationTasks = new ArrayList<>();

    public MicroProfileMetricRegistration() {
    }

    @Override
    public synchronized void register() { // synchronized to avoid registering same thing twice. Shouldn't really be possible; just being cautious
        for (Runnable task : registrationTasks) {
            task.run();
        }
        // This object will last until undeploy or server stop,
        // so clean up and save memory
        registrationTasks.clear();
    }

     @Override
     public void unregister() {
        MetricRegistry registry = MetricRegistries.get(MetricRegistry.Type.VENDOR);
        for (MetricID id : unregistrationTasks) {
            registry.remove(id);
        }
    }

    public void registerMetric(WildFlyMetric metric, WildFlyMetricMetadata metadata) {
        final Metric mpMetric;
        if (metadata.getType() == COUNTER) {
            mpMetric = new Counter() {
                @Override
                public void inc() {
                }

                @Override
                public void inc(long n) {
                }

                @Override
                public long getCount() {
                    OptionalDouble value = metric.getValue();
                    return  Double.valueOf(value.orElse(0)).longValue();
                }
            };
        } else {
            mpMetric = new Gauge<Number>() {
                @Override
                public Double getValue() {
                    return metric.getValue().orElse(0);
                }
            };
        }
        final Metadata mpMetadata;
        MetricRegistry vendorRegistry = MetricRegistries.get(MetricRegistry.Type.VENDOR);
        synchronized (vendorRegistry) {
            Metadata existingMetadata = vendorRegistry.getMetadata().get(metadata.getMetricName());
            if (existingMetadata != null) {
                mpMetadata = existingMetadata;
            } else {
                mpMetadata = new ExtendedMetadata(metadata.getMetricName(), metadata.getMetricName(), metadata.getDescription(),
                        metadata.getType() == COUNTER ? MetricType.COUNTER : MetricType.GAUGE, metricUnit(metadata.getMeasurementUnit()),
                        null, false,
                        // for WildFly subsystem metrics, the microprofile scope is put in the OpenMetrics tags
                        // so that the name of the metric does not change ("vendor_" will not be prepended to it).
                        Optional.of(false));
            }
            Tag[] mpTags = toMicroProfileMetricsTags(metadata.getTags());
            vendorRegistry.register(mpMetadata, mpMetric, mpTags);
        }
    }

    @Override
    public synchronized void addRegistrationTask(Runnable task) {
        registrationTasks.add(task);
    }

    @Override
    public void addUnregistrationTask(org.wildfly.extension.microprofile.metrics.MetricID metricID) {
        unregistrationTasks.add(toMicroProfileMetricID(metricID));
    }

    private MetricID toMicroProfileMetricID(org.wildfly.extension.microprofile.metrics.MetricID metricID) {
        return new MetricID(metricID.getMetricName(), toMicroProfileMetricsTags(metricID.getTags()));
    }

    private Tag[] toMicroProfileMetricsTags(WildFlyMetricMetadata.MetricTag[] tags) {
        if (tags == null || tags.length == 0) {
            return new Tag[0];
        }
        Tag[] mpTags = new Tag[tags.length];
        for (int i = 0; i < tags.length; i++) {
            mpTags[i] = new Tag(tags[i].getKey(), tags[i].getValue());
        }
        return mpTags;
    }

    private String metricUnit(MeasurementUnit unit) {
        if (unit == null) {
            return MetricUnits.NONE;
        }
        switch (unit) {

            case PERCENTAGE:
                return MetricUnits.PERCENT;
            case BYTES:
                return MetricUnits.BYTES;
            case KILOBYTES:
                return MetricUnits.KILOBYTES;
            case MEGABYTES:
                return MetricUnits.MEGABYTES;
            case GIGABYTES:
                return MetricUnits.GIGABYTES;
            case TERABYTES:
                return "terabytes";
            case PETABYTES:
                return "petabytes";
            case BITS:
                return MetricUnits.BITS;
            case KILOBITS:
                return MetricUnits.KILOBITS;
            case MEGABITS:
                return MetricUnits.MEBIBITS;
            case GIGABITS:
                return MetricUnits.GIGABITS;
            case TERABITS:
                return "terabits";
            case PETABITS:
                return "petabits";
            case EPOCH_MILLISECONDS:
                return MetricUnits.MILLISECONDS;
            case EPOCH_SECONDS:
                return MetricUnits.SECONDS;
            case JIFFYS:
                return "jiffys";
            case NANOSECONDS:
                return MetricUnits.NANOSECONDS;
            case MICROSECONDS:
                return MetricUnits.MICROSECONDS;
            case MILLISECONDS:
                return MetricUnits.MILLISECONDS;
            case SECONDS:
                return MetricUnits.SECONDS;
            case MINUTES:
                return MetricUnits.MINUTES;
            case HOURS:
                return MetricUnits.HOURS;
            case DAYS:
                return MetricUnits.DAYS;
            case PER_JIFFY:
                return "per-jiffy";
            case PER_NANOSECOND:
                return "per_nanoseconds";
            case PER_MICROSECOND:
                return "per_microseconds";
            case PER_MILLISECOND:
                return "per_milliseconds";
            case PER_SECOND:
                return MetricUnits.PER_SECOND;
            case PER_MINUTE:
                return "per_minutes";
            case PER_HOUR:
                return "per_hour";
            case PER_DAY:
                return "per_day";
            case CELSIUS:
                return "degree_celsius";
            case KELVIN:
                return "kelvin";
            case FAHRENHEIGHT:
                return "degree_fahrenheit";
            case NONE:
            default:
                return "none";
        }
    }
}

