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

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.activemq.artemis.core.config.ConnectorServiceConfiguration;
import org.apache.activemq.artemis.utils.ClassloadingUtil;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.AttributeMarshaller;
import org.jboss.as.controller.ObjectTypeAttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.extension.messaging.activemq.logging.MessagingLogger;

/**
 * Connector service resource definition
 *
 * @author <a href="http://jmesnil.net">Jeff Mesnil</a> (c) 2012 Red Hat Inc.
 */
public class ConnectorServiceDefinition extends PersistentResourceDefinition {

    static ObjectTypeAttributeDefinition CLASS = ObjectTypeAttributeDefinition.Builder.of("class",
            create(CommonAttributes.NAME, ModelType.STRING, false)
                    .setAllowExpression(false)
                    .build(),
            create(CommonAttributes.MODULE, ModelType.STRING, false)
                    .setAllowExpression(false)
                    .build())
            .setRestartAllServices()
            .setAllowNull(false)
            .setAttributeMarshaller(new AttributeMarshaller() {
                @Override
                public boolean isMarshallableAsElement() {
                    return true;
                }

                @Override
                public void marshallAsElement(AttributeDefinition attribute, ModelNode resourceModel, boolean marshallDefault, XMLStreamWriter writer) throws XMLStreamException {
                    if (!resourceModel.hasDefined(attribute.getName())) {
                        return;
                    }
                    resourceModel = resourceModel.get(attribute.getName());
                    writer.writeEmptyElement(attribute.getXmlName());
                    writer.writeAttribute(CommonAttributes.NAME, resourceModel.get(CommonAttributes.NAME).asString());
                    writer.writeAttribute(CommonAttributes.MODULE, resourceModel.get(CommonAttributes.MODULE).asString());
                }
            })
            .build();

    private static final AttributeDefinition[] ATTRIBUTES = {
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
        System.out.println("model = " + model);
        if (model.hasDefined(CommonAttributes.CONNECTOR_SERVICE)) {
            for (ModelNode connectorService : model.get(CommonAttributes.CONNECTOR_SERVICE).asList()) {
                System.out.println("connectorService = " + connectorService);
                String name = connectorService.require(CommonAttributes.NAME).asString();
                ConnectorServiceConfiguration config = createConnectorServiceConfiguration(context, name, connectorService);
                Class clazz = ServerAdd.unwrapClass(connectorService.get("class"));
                System.out.println("name = " + name);
                System.out.println("config = " + config);
                System.out.println("clazz = " + clazz);
//                serverService.putConnectorServices(name, config, clazz);
            }
        }
    }

    private static ConnectorServiceConfiguration createConnectorServiceConfiguration(final OperationContext context, final String name, final ModelNode model) throws OperationFailedException {
        final String factoryClass = CommonAttributes.FACTORY_CLASS.resolveModelAttribute(context, model).asString();
        Map<String, String> unwrappedParameters = CommonAttributes.PARAMS.unwrap(context, model);
        Map<String, Object> parameters = new HashMap<String, Object>(unwrappedParameters);
        return new ConnectorServiceConfiguration()
                .setFactoryClassName(factoryClass)
                .setParams(parameters)
                .setName(name);
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

    private static void checkFactoryClass(final String factoryClass) throws OperationFailedException {
        try {
            ClassloadingUtil.newInstanceFromClassLoader(factoryClass);
        } catch (Throwable t) {
            throw MessagingLogger.ROOT_LOGGER.unableToLoadConnectorServiceFactoryClass(factoryClass);
        }
    }

    static class ConnectorServiceWriteAttributeHandler extends ReloadRequiredWriteAttributeHandler {
        ConnectorServiceWriteAttributeHandler(AttributeDefinition... attributes) {
            super(attributes);
        }
        @Override
        protected boolean applyUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName,
                ModelNode resolvedValue, ModelNode currentValue,
                org.jboss.as.controller.AbstractWriteAttributeHandler.HandbackHolder<Void> voidHandback)
                throws OperationFailedException {
            if (CommonAttributes.FACTORY_CLASS.getName().equals(attributeName)) {
                checkFactoryClass(resolvedValue.asString());
            }
            return super.applyUpdateToRuntime(context, operation, attributeName, resolvedValue, currentValue, voidHandback);
        }
    }

    static class ConnectorServiceAddHandler extends ActiveMQReloadRequiredHandlers.AddStepHandler {
        ConnectorServiceAddHandler(AttributeDefinition... attributes) {
            super(attributes);
        }

        @Override
        protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model)
                throws OperationFailedException {
            final String factoryClass = CommonAttributes.FACTORY_CLASS.resolveModelAttribute(context, model).asString();
            checkFactoryClass(factoryClass);
            super.performRuntime(context, operation, model);
        }

    }
}