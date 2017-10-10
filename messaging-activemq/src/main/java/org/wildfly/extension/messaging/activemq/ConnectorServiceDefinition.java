/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

import static org.jboss.as.controller.SimpleAttributeDefinitionBuilder.create;
import static org.jboss.as.controller.registry.AttributeAccess.Flag.STORAGE_RUNTIME;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.activemq.artemis.core.config.ConnectorServiceConfiguration;
import org.apache.activemq.artemis.core.server.ConnectorServiceFactory;
import org.apache.activemq.artemis.utils.ClassloadingUtil;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ObjectTypeAttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.Property;
import org.wildfly.extension.messaging.activemq.logging.MessagingLogger;

/**
 * Connector service resource definition
 *
 * @author <a href="http://jmesnil.net">Jeff Mesnil</a> (c) 2012 Red Hat Inc.
 */
public class ConnectorServiceDefinition extends PersistentResourceDefinition {

    @Deprecated
    static final SimpleAttributeDefinition FACTORY_CLASS = create("factory-class", ModelType.STRING)
            .setAllowExpression(false)
            .setAlternatives("class")
            .setRestartAllServices()
            .setRequired(false)
            .setDeprecated(MessagingExtension.VERSION_3_0_0)
            .build();

    static final ObjectTypeAttributeDefinition CLASS = ObjectTypeAttributeDefinition.Builder.of("class",
            create(CommonAttributes.NAME, ModelType.STRING, false)
                    .setAllowExpression(false)
                    .build(),
            create(CommonAttributes.MODULE, ModelType.STRING, false)
                    .setAllowExpression(false)
                    .build())
            .setAlternatives(FACTORY_CLASS.getName())
            .build();

    private static final AttributeDefinition[] ATTRIBUTES = {
            FACTORY_CLASS,
            CLASS,
            CommonAttributes.PARAMS };

    static final ConnectorServiceDefinition INSTANCE = new ConnectorServiceDefinition();

    private ConnectorServiceDefinition() {
        super(MessagingExtension.CONNECTOR_SERVICE_PATH,
                MessagingExtension.getResourceDescriptionResolver(false, CommonAttributes.CONNECTOR_SERVICE),
                new ConnectorServiceAddHandler(ATTRIBUTES),
                new ActiveMQReloadRequiredHandlers.RemoveStepHandler());
    }

    static void processConnectorServices(final OperationContext context, final ModelNode model, final ActiveMQServerService serverService)  throws OperationFailedException {
        if (model.hasDefined(CommonAttributes.CONNECTOR_SERVICE)) {
            for (Property connectorService : model.get(CommonAttributes.CONNECTOR_SERVICE).asPropertyList()) {
                String name = connectorService.getName();
                ConnectorServiceFactory factory = loadClass(context, connectorService.getValue());
                Map<String, String> unwrappedParameters = CommonAttributes.PARAMS.unwrap(context, connectorService.getValue());
                Map<String, Object> parameters = new HashMap<>(unwrappedParameters);

                ConnectorServiceConfiguration config = new ConnectorServiceConfiguration()
                        .setFactoryClassName(factory.getClass().getSimpleName())
                        .setParams(parameters)
                        .setName(name);
                serverService.addConnectorService(factory, config);
            }
        }
    }

    private static ConnectorServiceFactory loadClass(OperationContext context, ModelNode connectorServiceModel) throws OperationFailedException {
        if (connectorServiceModel.hasDefined(FACTORY_CLASS.getName())) {
            String className = FACTORY_CLASS.resolveModelAttribute(context, connectorServiceModel).asString();
            try {
                Object o = ClassloadingUtil.newInstanceFromClassLoader(className);
                return ConnectorServiceFactory.class.cast(o);
            } catch (Throwable t) {
                throw MessagingLogger.ROOT_LOGGER.unableToLoadConnectorServiceFactoryClass(className);
            }
        } else {
            Object o = ClassLoaderUtil.instantiate(connectorServiceModel.require(CLASS.getName()));
            return ConnectorServiceFactory.class.cast(o);
        }
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return Arrays.asList(ATTRIBUTES);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration registry) {
        OperationStepHandler writeHandler = new ConnectorServiceWriteAttributeHandler(ATTRIBUTES);
        for (AttributeDefinition attr : ATTRIBUTES) {
            if (!attr.getFlags().contains(STORAGE_RUNTIME)) {
                registry.registerReadWriteAttribute(attr, null, writeHandler);
            }
        }
    }

    private static class ConnectorServiceWriteAttributeHandler extends ReloadRequiredWriteAttributeHandler {
        ConnectorServiceWriteAttributeHandler(AttributeDefinition... attributes) {
            super(attributes);
        }
        @Override
        protected boolean applyUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName,
                ModelNode resolvedValue, ModelNode currentValue,
                org.jboss.as.controller.AbstractWriteAttributeHandler.HandbackHolder<Void> voidHandback)
                throws OperationFailedException {
            return super.applyUpdateToRuntime(context, operation, attributeName, resolvedValue, currentValue, voidHandback);
        }


    }

    private static class ConnectorServiceAddHandler extends ActiveMQReloadRequiredHandlers.AddStepHandler {
        ConnectorServiceAddHandler(AttributeDefinition... attributes) {
            super(attributes);
        }

        @Override
        protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model)
                throws OperationFailedException {
            // the ConnectorService will not be taken into account until Artemis is reloaded but we
            // try to load the class when the resource is added so that if a problem occurs, the user
            // is warned during this :add operation and not during the runtime start of Artemis after reload.
            loadClass(context, model);
            super.performRuntime(context, operation, model);
        }

    }
}