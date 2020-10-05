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
        Counter,
        Gauge;

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
