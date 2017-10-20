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
import static org.jboss.dmr.ModelType.BOOLEAN;
import static org.jboss.dmr.ModelType.STRING;

import java.util.Arrays;
import java.util.Collection;

import org.apache.activemq.artemis.api.config.ActiveMQDefaultConfiguration;
import org.apache.activemq.artemis.core.server.transformer.Transformer;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ObjectTypeAttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.Property;

/**
 * Divert resource definition
 *
 * @author <a href="http://jmesnil.net">Jeff Mesnil</a> (c) 2012 Red Hat Inc.
 */
public class DivertDefinition extends PersistentResourceDefinition {

    public static final SimpleAttributeDefinition ROUTING_NAME = create("routing-name", STRING)
            .setRequired(false)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    public static final SimpleAttributeDefinition ADDRESS = create("divert-address", STRING)
            .setXmlName("address")
            .setDefaultValue(null)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    public static final SimpleAttributeDefinition FORWARDING_ADDRESS = create("forwarding-address", STRING)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    public static final SimpleAttributeDefinition EXCLUSIVE = create("exclusive", BOOLEAN)
            .setDefaultValue(new ModelNode(ActiveMQDefaultConfiguration.isDefaultDivertExclusive()))
            .setRequired(false)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    @Deprecated
    static final SimpleAttributeDefinition TRANSFORMER_CLASS_NAME = create("transformer-class-name", ModelType.STRING)
            .setRequired(false)
            .setAllowExpression(false)
            .setAlternatives("transformer-class")
            .setRestartAllServices()
            .setDeprecated(MessagingExtension.VERSION_3_0_0)
            .build();

    static final ObjectTypeAttributeDefinition TRANSFORMER_CLASS = ObjectTypeAttributeDefinition.Builder.of("transformer-class",
            create(CommonAttributes.NAME, ModelType.STRING, false)
                    .setAllowExpression(false)
                    .build(),
            create(CommonAttributes.MODULE, ModelType.STRING, false)
                    .setAllowExpression(false)
                    .build())
            .setAlternatives(TRANSFORMER_CLASS_NAME.getName())
            .build();

    public static final AttributeDefinition[] ATTRIBUTES = { ROUTING_NAME, ADDRESS, FORWARDING_ADDRESS, CommonAttributes.FILTER,
            TRANSFORMER_CLASS_NAME, TRANSFORMER_CLASS, EXCLUSIVE };

    private final boolean registerRuntimeOnly;

    static final DivertDefinition INSTANCE = new DivertDefinition(false);

    public DivertDefinition(boolean registerRuntimeOnly) {
        super(MessagingExtension.DIVERT_PATH,
                MessagingExtension.getResourceDescriptionResolver(CommonAttributes.DIVERT),
                DivertAdd.INSTANCE,
                DivertRemove.INSTANCE);
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
                registry.registerReadWriteAttribute(attr, null, DivertConfigurationWriteHandler.INSTANCE);
            }
        }
    }

    public static void processDiverts(OperationContext context, ModelNode model, ActiveMQServerService serverService) throws OperationFailedException {
        if (model.hasDefined(CommonAttributes.DIVERT)) {
            for (Property prop : model.get(CommonAttributes.DIVERT).asPropertyList()) {
                Transformer transformer = TransformerUtil.loadTransformer(context, prop.getValue());
                if (transformer != null) {
                    serverService.addDivertTransformer(prop.getName(), transformer);
                }
            }
        }
    }
}
