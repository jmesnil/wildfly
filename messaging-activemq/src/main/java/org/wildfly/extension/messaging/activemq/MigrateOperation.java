/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.messaging.activemq;

import static org.jboss.as.controller.PathAddress.EMPTY_ADDRESS;
import static org.jboss.as.controller.PathAddress.pathAddress;
import static org.jboss.as.controller.PathElement.pathElement;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.BRIDGE;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.CLUSTER_CONNECTION;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.CONNECTION_FACTORY;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.POOLED_CONNECTION_FACTORY;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.SERVER;

import java.util.List;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.RunningMode;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;

/**
 * Operation to migrate from the legacy messaging subsystem to the new messaging-activemq subsystem.
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2015 Red Hat inc.
 */

public class MigrateOperation implements OperationStepHandler {

    private static final OperationStepHandler INSTANCE = new MigrateOperation();

    private static final PathAddress LEGACY_MESSAGING_SUBSYSTEM_ADDRESS = PathAddress.pathAddress(SUBSYSTEM, "messaging");

    private MigrateOperation() {

    }

    static void registerOperation(ManagementResourceRegistration registry, ResourceDescriptionResolver resourceDescriptionResolver) {
        registry.registerOperationHandler(new SimpleOperationDefinitionBuilder("migrate", resourceDescriptionResolver)
                        .setRuntimeOnly()
                        .build(),
                MigrateOperation.INSTANCE);

    }

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        System.out.println("MessagingSubsystemRootResourceDefinition.execute");
        System.out.println("context = [" + context + "], operation = [" + operation + "]");

        if (context.getRunningMode() != RunningMode.ADMIN_ONLY) {
            throw new OperationFailedException("the messaging migration can be performed when the server is in admin-only mode");
        }

        Resource newMessaging = context.readResourceForUpdate(EMPTY_ADDRESS);
        ModelNode newModel = Resource.Tools.readModel(newMessaging);
        if (!newModel.asPropertyList().isEmpty()) {
            throw new OperationFailedException("can not migrate: the new messaging-subsystem is already defined");
        }

        Resource legacyMessaging = context.readResourceFromRoot(LEGACY_MESSAGING_SUBSYSTEM_ADDRESS);
        ModelNode legacyModel = Resource.Tools.readModel(legacyMessaging).clone();

        for (Property property : legacyModel.asPropertyList()) {
            if (property.getName().equals("hornetq-server")) {
                for (Property serverProp : property.getValue().asPropertyList()) {
                    Resource server = context.createResource(pathAddress(pathElement(SERVER, serverProp.getName())));
                    server.writeModel(serverProp.getValue());

                    ModelNode serverModel = server.getModel();

                    migrateConnectionFactory(serverModel);
                    migratePooledConnectionFactory(serverModel);
                    migrateClusterConnection(serverModel);
                    migrateBridge(serverModel);
                }
            }
        }

        System.out.println("newModel = " + Resource.Tools.readModel(newMessaging));
        // remove the legacy messaging subsystem
        ModelNode removeLegacyMessagingSubsystemOperation = Util.createRemoveOperation(LEGACY_MESSAGING_SUBSYSTEM_ADDRESS);
        context.addStep(removeLegacyMessagingSubsystemOperation, context.getRootResourceRegistration().getOperationHandler(LEGACY_MESSAGING_SUBSYSTEM_ADDRESS, REMOVE), OperationContext.Stage.MODEL);
    }

    private void migrateConnectionFactory(ModelNode serverModel) {
        for (Property connectionFactoryProp : serverModel.get(CONNECTION_FACTORY).asPropertyList()) {
            ModelNode connectionFactory = connectionFactoryProp.getValue();

            migrateConnectorAttribute(connectionFactory);
            migrateDiscoveryGroupNameAttribute(connectionFactory);

            serverModel.get(CONNECTION_FACTORY, connectionFactoryProp.getName()).set(connectionFactory);
        }
    }

    private void migratePooledConnectionFactory(ModelNode serverModel) {
        for (Property pooledConnectionFactoryProp : serverModel.get(POOLED_CONNECTION_FACTORY).asPropertyList()) {
            ModelNode pooledConnectionFactory = pooledConnectionFactoryProp.getValue();

            migrateConnectorAttribute(pooledConnectionFactory);
            migrateDiscoveryGroupNameAttribute(pooledConnectionFactory);

            serverModel.get(POOLED_CONNECTION_FACTORY, pooledConnectionFactoryProp.getName()).set(pooledConnectionFactory);
        }
    }

    private void migrateClusterConnection(ModelNode serverModel) {
        for (Property clusterConnectionProp: serverModel.get(CLUSTER_CONNECTION).asPropertyList()) {
            ModelNode clusterConnection = clusterConnectionProp.getValue();
            // connector-ref attribute has been renamed to connector-name
            clusterConnection.get("connector-name").set(clusterConnection.get("connector-ref"));
            clusterConnection.remove("connector-ref");
            serverModel.get(CLUSTER_CONNECTION, clusterConnectionProp.getName()).set(clusterConnection);
        }
    }

    private void migrateConnectorAttribute(ModelNode model) {
        ModelNode connector = model.get("connector");
        if (connector.isDefined()) {
            // legacy connector is a property list where the name is the connector and the value is undefined
            List<Property> connectorProps = connector.asPropertyList();
            for (Property connectorProp : connectorProps) {
                model.get("connectors").add(connectorProp.getName());
            }
            model.remove("connector");
        }
    }
    private void migrateDiscoveryGroupNameAttribute(ModelNode model) {
        ModelNode discoveryGroup = model.get("discovery-group-name");
        if (discoveryGroup.isDefined()) {
            // discovery-group-name attribute has been renamed to discovery-group
            model.get("discovery-group").set(discoveryGroup);
            model.remove("discovery-group-name");
        }
    }

    private void migrateBridge(ModelNode serverModel) {
        for (Property bridgeProp: serverModel.get(BRIDGE).asPropertyList()) {
            ModelNode bridge = bridgeProp.getValue();

            migrateDiscoveryGroupNameAttribute(bridge);

            serverModel.get(BRIDGE, bridgeProp.getName()).set(bridge);

        }
    }
}
