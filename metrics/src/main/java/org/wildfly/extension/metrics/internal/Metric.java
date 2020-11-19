package org.wildfly.extension.metrics.internal;

import java.util.OptionalDouble;

public interface Metric {

    OptionalDouble getValue();
}
