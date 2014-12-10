/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.common.jms;

import static org.jboss.as.controller.client.helpers.ClientConstants.ADD;
import static org.jboss.as.controller.client.helpers.ClientConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.client.helpers.ClientConstants.OUTCOME;
import static org.jboss.as.controller.client.helpers.ClientConstants.SUCCESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;

import java.io.IOException;

import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;

/**
 * A default implementation of JMSOperations for Apache ActiveMQ 6.
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2014 Red Hat inc.
 */
public class DefaultActiveMQ6ProviderJMSOperations implements JMSOperations {
    private final ManagementClient client;

    private static final Logger logger = Logger.getLogger(DefaultActiveMQ6ProviderJMSOperations.class);

    public DefaultActiveMQ6ProviderJMSOperations(ManagementClient client) {
        this.client = client;
    }

    @Override
    public void createJmsQueue(String queueName, String jndiName) {
        createJmsDestination("jms-queue", queueName, jndiName);
    }

    @Override
    public void createJmsTopic(String topicName, String jndiName) {
        createJmsDestination("jms-topic", topicName, jndiName);
    }

    @Override
    public void removeJmsQueue(String queueName) {
        removeJmsDestination("jms-queue", queueName);
    }

    @Override
    public void removeJmsTopic(String topicName) {
        removeJmsDestination("jms-topic", topicName);
    }

    @Override
    public void close() {
        // no-op
        // DO NOT close the management client. Whoever passed it into the constructor should close it
    }

    private ModelControllerClient getModelControllerClient() {
        return client.getControllerClient();
    }

    private void createJmsDestination(final String destinationType, final String destinationName, final String jndiName) {
        final ModelNode operation = new ModelNode();
        operation.get(OP).set(ADD);
        operation.get(OP_ADDR).add("subsystem", "messaging-activemq6");
        operation.get(OP_ADDR).add("server", "default");
        operation.get(OP_ADDR).add(destinationType, destinationName);
        operation.get("entries").add(jndiName);
        try {
            this.execute(operation);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void removeJmsDestination(final String destinationType, final String destinationName) {
        final ModelNode operation = new ModelNode();
        operation.get(OP).set("remove");
        operation.get(OP_ADDR).add("subsystem", "messaging-activemq6");
        operation.get(OP_ADDR).add("server", "default");
        operation.get(OP_ADDR).add(destinationType, destinationName);
        try {
            this.execute(operation);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void execute(final ModelNode operation) throws IOException, JMSOperationsException {
        ModelNode result = this.getModelControllerClient().execute(operation);
        if (result.hasDefined(OUTCOME) && SUCCESS.equals(result.get(OUTCOME).asString())) {
            logger.info("Operation successful for operation = " + operation.toString());
        } else if (result.hasDefined(FAILURE_DESCRIPTION)) {
            final String failure = result.get(FAILURE_DESCRIPTION).toString();
            throw new JMSOperationsException(failure);
        } else {
            throw new JMSOperationsException("Operation not successful; outcome = " + result.get(OUTCOME));
        }
    }

    @Override
    public void setSystemProperties(String destination, String resourceAdapter) {
        final ModelNode enableSubstitutionOp = new ModelNode();
        enableSubstitutionOp.get(OP_ADDR).set(SUBSYSTEM, "ee");
        enableSubstitutionOp.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
        enableSubstitutionOp.get(NAME).set("annotation-property-replacement");
        enableSubstitutionOp.get(VALUE).set(true);

        final ModelNode setDestinationOp = new ModelNode();
        setDestinationOp.get(OP).set(ADD);
        setDestinationOp.get(OP_ADDR).add("system-property", "destination");
        setDestinationOp.get("value").set(destination);
        final ModelNode setResourceAdapterOp = new ModelNode();
        setResourceAdapterOp.get(OP).set(ADD);
        setResourceAdapterOp.get(OP_ADDR).add("system-property", "resource.adapter");
        setResourceAdapterOp.get("value").set(resourceAdapter);

        try {
            execute(enableSubstitutionOp);
            execute(setDestinationOp);
            execute(setResourceAdapterOp);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void removeSystemProperties() {
        final ModelNode removeDestinationOp = new ModelNode();
        removeDestinationOp.get(OP).set("remove");
        removeDestinationOp.get(OP_ADDR).add("system-property", "destination");
        final ModelNode removeResourceAdapterOp = new ModelNode();
        removeResourceAdapterOp.get(OP).set("remove");
        removeResourceAdapterOp.get(OP_ADDR).add("system-property", "resource.adapter");

        final ModelNode disableSubstitutionOp = new ModelNode();
        disableSubstitutionOp.get(OP_ADDR).set(SUBSYSTEM, "ee");
        disableSubstitutionOp.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
        disableSubstitutionOp.get(NAME).set("annotation-property-replacement");
        disableSubstitutionOp.get(VALUE).set(false);

        try {
            execute(removeDestinationOp);
            execute(removeResourceAdapterOp);
            execute(disableSubstitutionOp);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
