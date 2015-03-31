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

                    migrateConnectionFactory(server.getModel());
                    migratePooledConnectionFactory(server.getModel());
                }
            }
        }

        System.out.println("newModel = " + Resource.Tools.readModel(newMessaging));
        // remove the legacy messaging subsystem
        ModelNode removeLegacyMessagingSubsystemOperation = Util.createRemoveOperation(LEGACY_MESSAGING_SUBSYSTEM_ADDRESS);
        context.addStep(removeLegacyMessagingSubsystemOperation, context.getRootResourceRegistration().getOperationHandler(LEGACY_MESSAGING_SUBSYSTEM_ADDRESS, REMOVE), OperationContext.Stage.MODEL);
    }

    private void migrateConnectionFactory(ModelNode serverModel) {
        for (Property connectionFactory : serverModel.get(CONNECTION_FACTORY).asPropertyList()) {
            // legacy connector is a property list where the name is the connector and the value is undefined
            List<Property> connector = connectionFactory.getValue().get("connector").asPropertyList();
            for (Property connectorProp : connector) {
                serverModel.get(CONNECTION_FACTORY, connectionFactory.getName()).get("connectors").add(connectorProp.getName());
            }
            serverModel.get(CONNECTION_FACTORY, connectionFactory.getName()).remove("connector");
        }
    }

    private void migratePooledConnectionFactory(ModelNode serverModel) {
        for (Property pooledConnectionFactory : serverModel.get(POOLED_CONNECTION_FACTORY).asPropertyList()) {
            // legacy connector is a property list where the name is the connector and the value is undefined
            List<Property> connector = pooledConnectionFactory.getValue().get("connector").asPropertyList();
            for (Property connectorProp : connector) {
                serverModel.get(POOLED_CONNECTION_FACTORY, pooledConnectionFactory.getName()).get("connectors").add(connectorProp.getName());
            }
            serverModel.get(POOLED_CONNECTION_FACTORY, pooledConnectionFactory.getName()).remove("connector");
        }
    }
}
