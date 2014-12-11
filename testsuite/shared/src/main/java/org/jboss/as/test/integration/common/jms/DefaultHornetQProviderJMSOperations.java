/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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

import static org.jboss.as.controller.PathAddress.pathAddress;
import static org.jboss.as.controller.client.helpers.ClientConstants.ADD;
import static org.jboss.as.controller.client.helpers.ClientConstants.REMOVE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;
import static org.jboss.as.test.integration.common.jms.JMSOperationsProvider.execute;

import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.logging.Logger;

/**
 * A default implementation of JMSOperations used with hornetq
 * @author jpai, refactored by <a href="jmartisk@redhat.com">Jan Martiska</a>
 */
public class DefaultHornetQProviderJMSOperations implements JMSOperations {

    private final ModelControllerClient client;

    private static final Logger logger = Logger.getLogger(DefaultHornetQProviderJMSOperations.class);

    public DefaultHornetQProviderJMSOperations(ManagementClient client) {
        this.client = client.getControllerClient();
    }

    public DefaultHornetQProviderJMSOperations(ModelControllerClient client) {
        this.client = client;
    }

    private ModelControllerClient getModelControllerClient() {
        return client;
    }

    @Override
    public String getProviderName() {
        return "hornetq";
    }

    @Override
    public void createJmsQueue(String queueName, String jndiName) {
        PathAddress address = pathAddress("subsystem", "messaging")
                .append("hornetq-server", "default")
                .append("jms-queue", queueName);
        ModelNode attributes = new ModelNode();
        attributes.get("entries").add(jndiName);
        executeOperation(address, ADD, attributes);
    }

    @Override
    public void createJmsTopic(String topicName, String jndiName) {
        PathAddress address = pathAddress("subsystem", "messaging")
                .append("hornetq-server", "default")
                .append("jms-topic", topicName);
        ModelNode attributes = new ModelNode();
        attributes.get("entries").add(jndiName);
        executeOperation(address, ADD, attributes);
    }

    @Override
    public void removeJmsQueue(String queueName) {
        PathAddress address = pathAddress("subsystem", "messaging")
                .append("hornetq-server", "default")
                .append("jms-queue", queueName);
        executeOperation(address, REMOVE_OPERATION, null);
    }

    @Override
    public void removeJmsTopic(String topicName) {
        PathAddress address = pathAddress("subsystem", "messaging")
                .append("hornetq-server", "default")
                .append("jms-topic", topicName);
        executeOperation(address, REMOVE_OPERATION, null);
    }

    @Override
    public void addJmsConnectionFactory(String name, String jndiName, ModelNode attributes) {
        PathAddress address = pathAddress("subsystem", "messaging")
                .append("hornetq-server", "default")
                .append("connection-factory", name);
        attributes.get("entries").add(jndiName);
        executeOperation(address, ADD, attributes);
    }

    @Override
    public void removeJmsConnectionFactory(String name) {
        PathAddress address = pathAddress("subsystem", "messaging")
                .append("hornetq-server", "default")
                .append("connection-factory", name);
        executeOperation(address, REMOVE_OPERATION, null);
    }

    @Override
    public void addJmsBridge (String name, ModelNode attributes) {
        PathAddress address = pathAddress("subsystem", "messaging")
                .append("jms-bridge", name);
        executeOperation(address, ADD, attributes);
    }

    @Override
            public void removeJmsBridge(String name) {
        PathAddress address = pathAddress("subsystem", "messaging")
                .append("jms-bridge", name);
        executeOperation(address, REMOVE_OPERATION, null);
    }

    @Override
    public void close() {
        // no-op
        // DO NOT close the management client. Whoever passed it into the constructor should close it
    }

    private void executeOperation(final PathAddress address, final String opName, ModelNode attributes) {
        final ModelNode operation = new ModelNode();
        operation.get(OP).set(opName);
        operation.get(OP_ADDR).set(address.toModelNode());
        if (attributes != null) {
            for (Property property : attributes.asPropertyList()) {
                operation.get(property.getName()).set(property.getValue());
            }
        }
        try {
            execute(client, operation);
        } catch (Exception e) {
            throw new RuntimeException(e);
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
            execute(client, enableSubstitutionOp);
            execute(client, setDestinationOp);
            execute(client, setResourceAdapterOp);
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
            execute(client, removeDestinationOp);
            execute(client, removeResourceAdapterOp);
            execute(client, disableSubstitutionOp);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
