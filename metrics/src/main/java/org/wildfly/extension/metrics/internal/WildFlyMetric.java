package org.wildfly.extension.metrics.internal;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.wildfly.extension.metrics._private.MetricsLogger.LOGGER;

import java.util.OptionalDouble;

import org.jboss.as.controller.LocalModelControllerClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;

public class WildFlyMetric {

    private LocalModelControllerClient modelControllerClient;
    private final PathAddress address;
    private final String attributeName;

    public WildFlyMetric(LocalModelControllerClient modelControllerClient, PathAddress address, String attributeName) {
        this.modelControllerClient = modelControllerClient;
        this.address = address;
        this.attributeName = attributeName;
    }

    public OptionalDouble getValue() {
        ModelNode result = readAttributeValue(address, attributeName);
        if (result.isDefined()) {
            try {
                return OptionalDouble.of(result.asDouble());
            } catch (Exception e) {
                throw LOGGER.unableToConvertAttribute(attributeName, address, e);
            }
        }
        return OptionalDouble.empty();
    }

    private ModelNode readAttributeValue(PathAddress address, String attributeName) {
        final ModelNode readAttributeOp = new ModelNode();
        readAttributeOp.get(OP).set(READ_ATTRIBUTE_OPERATION);
        readAttributeOp.get(OP_ADDR).set(address.toModelNode());
        readAttributeOp.get(ModelDescriptionConstants.INCLUDE_UNDEFINED_METRIC_VALUES).set(false);
        readAttributeOp.get(NAME).set(attributeName);
        ModelNode response = modelControllerClient.execute(readAttributeOp);
        String error = getFailureDescription(response);
        if (error != null) {
            throw LOGGER.unableToReadAttribute(attributeName, address, error);
        }
        return  response.get(RESULT);
    }

    private String getFailureDescription(ModelNode result) {
        if (result.hasDefined(FAILURE_DESCRIPTION)) {
            return result.get(FAILURE_DESCRIPTION).toString();
        }
        return null;
    }
}
