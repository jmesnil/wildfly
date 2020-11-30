package org.wildfly.extension.metrics;

import java.util.OptionalDouble;

public interface Metric {
    OptionalDouble getValue();
}
