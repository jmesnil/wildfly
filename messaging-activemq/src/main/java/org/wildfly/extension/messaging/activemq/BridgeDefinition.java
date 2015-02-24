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
import static org.wildfly.extension.messaging.activemq.CommonAttributes.CONNECTORS;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.CONNECTOR_REF_STRING;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.MESSAGING_SECURITY_DEF;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.STATIC_CONNECTORS;
import static org.jboss.dmr.ModelType.BOOLEAN;
import static org.jboss.dmr.ModelType.INT;
import static org.jboss.dmr.ModelType.STRING;

import java.util.Arrays;
import java.util.Collection;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.apache.activemq.api.config.ActiveMQDefaultConfiguration;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.AttributeParser;
import org.jboss.as.controller.DefaultAttributeMarshaller;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.PrimitiveListAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.StringListAttributeDefinition;
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;

/**
 * Bridge resource definition
 *
 * @author <a href="http://jmesnil.net">Jeff Mesnil</a> (c) 2012 Red Hat Inc.
 */
public class BridgeDefinition extends PersistentResourceDefinition {

    public static final PathElement PATH = PathElement.pathElement(CommonAttributes.BRIDGE);

    private final boolean registerRuntimeOnly;

    public static final PrimitiveListAttributeDefinition CONNECTOR_REFS = new StringListAttributeDefinition.Builder(CommonAttributes.STATIC_CONNECTORS)
            .setAllowNull(true)
            .setElementValidator(new StringLengthValidator(1))
                    //.setXmlName(CONNECTOR_REF_STRING)
                    //.setAttributeMarshaller(new AttributeMarshallers.WrappedListAttributeMarshaller(null))
                    // disallow expressions since the attribute references other configuration items
            .setAttributeParser(new AttributeParser() {
                @Override
                public void parseAndSetParameter(AttributeDefinition attribute, String value, ModelNode operation, XMLStreamReader reader) throws XMLStreamException {
                    if (value == null) {
                        return;
                    }
                    for (String element : value.split(",")) {
                        ModelNode paramVal = parse(attribute, element, reader);
                        operation.get(attribute.getName()).add(paramVal);
                    }
                }
            })
            .setAttributeMarshaller(new DefaultAttributeMarshaller() {
                @Override
                public void marshallAsAttribute(AttributeDefinition attribute, ModelNode resourceModel, boolean marshallDefault, XMLStreamWriter writer) throws XMLStreamException {

                    StringBuilder builder = new StringBuilder();
                    if (resourceModel.hasDefined(attribute.getName())) {
                        for (ModelNode p : resourceModel.get(attribute.getName()).asList()) {
                            builder.append(p.asString()).append(", ");
                        }
                    }
                    if (builder.length() > 3) {
                        builder.setLength(builder.length() - 2);
                    }
                    if (builder.length() > 0) {
                        writer.writeAttribute(attribute.getXmlName(), builder.toString());
                    }
                }
            })
            .setAllowExpression(false)
            .setRestartAllServices()
            .build();

    public static final SimpleAttributeDefinition DISCOVERY_GROUP_NAME = create(CommonAttributes.DISCOVERY_GROUP, STRING)
            .setAllowNull(true)
            .setAlternatives(STATIC_CONNECTORS)
            .setRestartAllServices()
            .build();

    public static final SimpleAttributeDefinition INITIAL_CONNECT_ATTEMPTS = create("initial-connect-attempts", INT)
            .setAllowNull(true)
            .setDefaultValue(new ModelNode().set(ActiveMQDefaultConfiguration.getDefaultBridgeInitialConnectAttempts()))
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    public static final SimpleAttributeDefinition QUEUE_NAME = create(CommonAttributes.QUEUE_NAME, STRING)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    public static final SimpleAttributeDefinition PASSWORD = create("password", STRING)
            .setAllowNull(true)
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode().set(ActiveMQDefaultConfiguration.getDefaultClusterPassword()))
            .setRestartAllServices()
            .addAccessConstraint(SensitiveTargetAccessConstraintDefinition.CREDENTIAL)
            .addAccessConstraint(MESSAGING_SECURITY_DEF)
            .build();

    public static final SimpleAttributeDefinition USER = create("user", STRING)
            .setAllowNull(true)
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode().set(ActiveMQDefaultConfiguration.getDefaultClusterUser()))
            .setRestartAllServices()
            .addAccessConstraint(SensitiveTargetAccessConstraintDefinition.CREDENTIAL)
            .addAccessConstraint(MESSAGING_SECURITY_DEF)
            .build();

    public static final SimpleAttributeDefinition USE_DUPLICATE_DETECTION = create("use-duplicate-detection", BOOLEAN)
            .setAllowNull(true)
            .setDefaultValue(new ModelNode().set(ActiveMQDefaultConfiguration.isDefaultBridgeDuplicateDetection()))
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    public static final SimpleAttributeDefinition RECONNECT_ATTEMPTS = create("reconnect-attempts", INT)
            .setAllowNull(true)
            .setDefaultValue(new ModelNode().set(ActiveMQDefaultConfiguration.getDefaultBridgeReconnectAttempts()))
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    public static final SimpleAttributeDefinition RECONNECT_ATTEMPTS_ON_SAME_NODE = create("reconnect-attempts-on-same-node", INT)
            .setAllowNull(true)
            .setDefaultValue(new ModelNode().set(ActiveMQDefaultConfiguration.getDefaultBridgeConnectSameNode()))
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    public static final SimpleAttributeDefinition FORWARDING_ADDRESS = create("forwarding-address", STRING)
            .setAllowNull(true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    public static final AttributeDefinition[] ATTRIBUTES = {
            QUEUE_NAME, FORWARDING_ADDRESS, CommonAttributes.HA,
            CommonAttributes.FILTER, CommonAttributes.TRANSFORMER_CLASS_NAME,
            CommonAttributes.MIN_LARGE_MESSAGE_SIZE, CommonAttributes.CHECK_PERIOD, CommonAttributes.CONNECTION_TTL,
            CommonAttributes.RETRY_INTERVAL, CommonAttributes.RETRY_INTERVAL_MULTIPLIER, CommonAttributes.MAX_RETRY_INTERVAL,
            INITIAL_CONNECT_ATTEMPTS,
            RECONNECT_ATTEMPTS,
            RECONNECT_ATTEMPTS_ON_SAME_NODE,
            USE_DUPLICATE_DETECTION, CommonAttributes.BRIDGE_CONFIRMATION_WINDOW_SIZE,
            USER, PASSWORD,
            CONNECTOR_REFS, DISCOVERY_GROUP_NAME
    };


    static final BridgeDefinition INSTANCE = new BridgeDefinition(false);

    public BridgeDefinition(final boolean registerRuntimeOnly) {
        super(PATH,
                MessagingExtension.getResourceDescriptionResolver(CommonAttributes.BRIDGE),
                BridgeAdd.INSTANCE,
                BridgeRemove.INSTANCE);
        this.registerRuntimeOnly = registerRuntimeOnly;
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return Arrays.asList(ATTRIBUTES);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration registry) {
        for (AttributeDefinition attr : ATTRIBUTES) {
            if (registerRuntimeOnly || !attr.getFlags().contains(AttributeAccess.Flag.STORAGE_RUNTIME)) {
                registry.registerReadWriteAttribute(attr, null, BridgeWriteAttributeHandler.INSTANCE);
            }
        }

        if (registerRuntimeOnly) {
            BridgeControlHandler.INSTANCE.registerAttributes(registry);
        }
    }

    @Override
    public void registerOperations(ManagementResourceRegistration registry) {
        if (registerRuntimeOnly) {
            BridgeControlHandler.INSTANCE.registerOperations(registry, getResourceDescriptionResolver());
        }

        super.registerOperations(registry);
    }
}
