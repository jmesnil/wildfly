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

package org.wildfly.extension.messaging.activemq.jms;

import static org.jboss.as.controller.SimpleAttributeDefinitionBuilder.create;
import static org.jboss.dmr.ModelType.STRING;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.PAUSED;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.TEMPORARY;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.StringListAttributeDefinition;
import org.jboss.as.controller.access.constraint.ApplicationTypeConfig;
import org.jboss.as.controller.access.management.AccessConstraintDefinition;
import org.jboss.as.controller.access.management.ApplicationTypeAccessConstraintDefinition;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.wildfly.extension.messaging.activemq.CommonAttributes;
import org.wildfly.extension.messaging.activemq.MessagingExtension;

/**
 * JMS Queue resource definition
 *
 * @author <a href="http://jmesnil.net">Jeff Mesnil</a> (c) 2012 Red Hat Inc.
 */
public class JMSQueueDefinition extends PersistentResourceDefinition {

    public static final PathElement PATH = PathElement.pathElement(CommonAttributes.JMS_QUEUE);

    public static final AttributeDefinition[] ATTRIBUTES = {
            CommonAttributes.DESTINATION_ENTRIES,
            CommonAttributes.SELECTOR,
            CommonAttributes.DURABLE
    };

    /**
     * Attributes for deployed JMS queue are stored in runtime
     */
    private static AttributeDefinition[] DEPLOYMENT_ATTRIBUTES = {
            new StringListAttributeDefinition.Builder(CommonAttributes.DESTINATION_ENTRIES)
                    .setStorageRuntime()
                    .build(),
            SimpleAttributeDefinitionBuilder.create(CommonAttributes.SELECTOR)
                    .setStorageRuntime()
                    .build(),
            SimpleAttributeDefinitionBuilder.create(CommonAttributes.DURABLE)
                    .setStorageRuntime()
                    .build()
    };

    static final AttributeDefinition QUEUE_ADDRESS = create("queue-address", STRING)
            .setStorageRuntime()
            .build();

    static final AttributeDefinition DEAD_LETTER_ADDRESS = create("dead-letter-address", STRING)
            .setAllowNull(true)
            .setStorageRuntime()
            .build();

    static final AttributeDefinition EXPIRY_ADDRESS = create("expiry-address", STRING)
            .setAllowNull(true)
            .setStorageRuntime()
            .build();

    static final AttributeDefinition[] READONLY_ATTRIBUTES = {
            QUEUE_ADDRESS,
            EXPIRY_ADDRESS,
            DEAD_LETTER_ADDRESS,
            PAUSED,
            TEMPORARY
    };

    static final AttributeDefinition[] METRICS = {
            CommonAttributes.MESSAGE_COUNT,
            CommonAttributes.DELIVERING_COUNT,
            CommonAttributes.MESSAGES_ADDED,
            CommonAttributes.SCHEDULED_COUNT,
            CommonAttributes.CONSUMER_COUNT
    };

    public static final JMSQueueDefinition INSTANCE = new JMSQueueDefinition(false);

    public static final JMSQueueDefinition DEPLOYMENT_INSTANCE = new JMSQueueDefinition(true);

    private final boolean deployed;

    private static final List<AccessConstraintDefinition> ACCESS_CONSTRAINTS;

    static {
        ApplicationTypeConfig atc = new ApplicationTypeConfig(MessagingExtension.SUBSYSTEM_NAME, CommonAttributes.JMS_QUEUE);
        ACCESS_CONSTRAINTS = new ApplicationTypeAccessConstraintDefinition(atc).wrapAsList();
    }

    private JMSQueueDefinition(final boolean deployed) {
        super(PATH,
                MessagingExtension.getResourceDescriptionResolver(CommonAttributes.JMS_QUEUE),
                deployed ? null : JMSQueueAdd.INSTANCE,
                deployed ? null : JMSQueueRemove.INSTANCE);
        this.deployed = deployed;
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        if (deployed) {
            return Arrays.asList(DEPLOYMENT_ATTRIBUTES);
        } else{
            return Arrays.asList(ATTRIBUTES);
        }
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration registry) {
        ReloadRequiredWriteAttributeHandler handler = new ReloadRequiredWriteAttributeHandler(getAttributes());
        for (AttributeDefinition attr : getAttributes()) {
            if (deployed) {
                registry.registerReadOnlyAttribute(attr, JMSQueueConfigurationRuntimeHandler.INSTANCE);
            } else {
                if (attr == CommonAttributes.DESTINATION_ENTRIES) {
                    registry.registerReadWriteAttribute(attr, null, handler);
                } else {
                    registry.registerReadOnlyAttribute(attr, null);
                }
            }
        }

        for (AttributeDefinition attr : READONLY_ATTRIBUTES) {
            registry.registerReadOnlyAttribute(attr, JMSQueueReadAttributeHandler.INSTANCE);
        }

        for (AttributeDefinition metric : METRICS) {
            registry.registerMetric(metric, JMSQueueReadAttributeHandler.INSTANCE);
        }
    }


    @Override
    public void registerOperations(ManagementResourceRegistration registry) {
        super.registerOperations(registry);

        JMSQueueControlHandler.INSTANCE.registerOperations(registry, getResourceDescriptionResolver());

        if (!deployed) {
            JMSQueueUpdateJndiHandler.registerOperations(registry, getResourceDescriptionResolver());
        }
    }

    @Override
    public List<AccessConstraintDefinition> getAccessConstraints() {
        return ACCESS_CONSTRAINTS;
    }
}
