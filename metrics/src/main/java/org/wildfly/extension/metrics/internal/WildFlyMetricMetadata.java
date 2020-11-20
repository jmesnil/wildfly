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
package org.wildfly.extension.metrics.internal;

import static java.util.Objects.requireNonNull;

import org.jboss.as.controller.client.helpers.MeasurementUnit;

public class WildFlyMetricMetadata {

    private final String name;
    private final String description;
    private final MeasurementUnit unit;
    private final Type type;

    public WildFlyMetricMetadata(String name, String description, MeasurementUnit unit, Type type) {
        requireNonNull(name);
        requireNonNull(description);
        requireNonNull(type);

        this.name = name;
        this.description = description;
        this.unit = unit != null ? unit : MeasurementUnit.NONE;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public MeasurementUnit getUnit() {
        return unit;
    }

    public String getBaseMetricUnit() {
        return baseMetricUnit(unit);
    }

    public Type getType() {
        return type;
    }

    public enum Type {
        COUNTER,
        GAUGE;

        @Override
        public String toString() {
            return this.name().toLowerCase();
        }
    }

    private static String baseMetricUnit(MeasurementUnit unit) {
        if (unit == null) {
            return "none";
        }
        switch (unit.getBaseUnits()) {
            case PERCENTAGE:
                return "percent";
            case BYTES:
            case KILOBYTES:
            case MEGABYTES:
            case GIGABYTES:
            case TERABYTES:
            case PETABYTES:
                return "bytes";
            case BITS:
            case KILOBITS:
            case MEGABITS:
            case GIGABITS:
            case TERABITS:
            case PETABITS:
                return "bits";
            case EPOCH_MILLISECONDS:
            case EPOCH_SECONDS:
            case NANOSECONDS:
            case MILLISECONDS:
            case MICROSECONDS:
            case SECONDS:
            case MINUTES:
            case HOURS:
            case DAYS:
                return "seconds";
            case JIFFYS:
                return "jiffys";
            case PER_JIFFY:
                return "per-jiffy";
            case PER_NANOSECOND:
            case PER_MICROSECOND:
            case PER_MILLISECOND:
            case PER_SECOND:
            case PER_MINUTE:
            case PER_HOUR:
            case PER_DAY:
                return "per_second";
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
