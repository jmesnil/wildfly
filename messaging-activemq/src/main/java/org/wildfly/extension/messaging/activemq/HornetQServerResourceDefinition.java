/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CHILDREN;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MAX_OCCURS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MIN_OCCURS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PATH;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.ASYNC_CONNECTION_EXECUTION_ENABLED;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.CONNECTION_TTL_OVERRIDE;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.CREATE_BINDINGS_DIR;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.CREATE_JOURNAL_DIR;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.ID_CACHE_SIZE;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.JMX_DOMAIN;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.JMX_MANAGEMENT_ENABLED;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.JOURNAL_BUFFER_SIZE;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.JOURNAL_BUFFER_TIMEOUT;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.JOURNAL_COMPACT_MIN_FILES;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.JOURNAL_COMPACT_PERCENTAGE;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.JOURNAL_FILE_SIZE;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.JOURNAL_MAX_IO;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.JOURNAL_MIN_FILES;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.JOURNAL_SYNC_NON_TRANSACTIONAL;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.JOURNAL_SYNC_TRANSACTIONAL;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.JOURNAL_TYPE;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.LOG_JOURNAL_WRITE_RATE;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.MANAGEMENT_ADDRESS;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.MANAGEMENT_NOTIFICATION_ADDRESS;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.MESSAGE_COUNTER_ENABLED;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.MESSAGE_COUNTER_MAX_DAY_HISTORY;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.MESSAGE_COUNTER_SAMPLE_PERIOD;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.MESSAGE_EXPIRY_SCAN_PERIOD;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.MESSAGE_EXPIRY_THREAD_PRIORITY;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.PAGE_MAX_CONCURRENT_IO;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.PERF_BLAST_PAGES;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.PERSISTENCE_ENABLED;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.PERSIST_DELIVERY_COUNT_BEFORE_DELIVERY;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.PERSIST_ID_CACHE;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.RUN_SYNC_SPEED_TEST;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.SECURITY_ENABLED;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.SECURITY_INVALIDATION_INTERVAL;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.SERVER_DUMP_INTERVAL;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.STATISTICS_ENABLED;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.TRANSACTION_TIMEOUT;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.TRANSACTION_TIMEOUT_SCAN_PERIOD;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.WILD_CARD_ROUTING_ENABLED;

import java.util.Locale;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.DefaultResourceDescriptionProvider;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.registry.ImmutableManagementResourceRegistration;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.wildfly.extension.messaging.activemq.jms.JMSServerControlHandler;

/**
 * {@link org.jboss.as.controller.ResourceDefinition} for the messaging subsystem HornetQServer resource.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class HornetQServerResourceDefinition extends SimpleResourceDefinition {

    public static final PathElement HORNETQ_SERVER_PATH = PathElement.pathElement(CommonAttributes.SERVER);

    public static final AttributeDefinition[] ATTRIBUTES_WITH_EXPRESSION_ALLOWED_IN_1_2_0 = { ASYNC_CONNECTION_EXECUTION_ENABLED, PERSISTENCE_ENABLED, SECURITY_ENABLED, SECURITY_INVALIDATION_INTERVAL,
            WILD_CARD_ROUTING_ENABLED, MANAGEMENT_ADDRESS, MANAGEMENT_NOTIFICATION_ADDRESS, JMX_MANAGEMENT_ENABLED, JMX_DOMAIN,
            STATISTICS_ENABLED, MESSAGE_COUNTER_ENABLED, MESSAGE_COUNTER_SAMPLE_PERIOD, MESSAGE_COUNTER_MAX_DAY_HISTORY,
            CONNECTION_TTL_OVERRIDE, TRANSACTION_TIMEOUT, TRANSACTION_TIMEOUT_SCAN_PERIOD,
            MESSAGE_EXPIRY_SCAN_PERIOD, MESSAGE_EXPIRY_THREAD_PRIORITY, ID_CACHE_SIZE, PERSIST_ID_CACHE,
            PERSIST_DELIVERY_COUNT_BEFORE_DELIVERY, PAGE_MAX_CONCURRENT_IO,
            CREATE_BINDINGS_DIR, CREATE_JOURNAL_DIR, JOURNAL_TYPE, JOURNAL_BUFFER_TIMEOUT, JOURNAL_BUFFER_SIZE,
            JOURNAL_SYNC_TRANSACTIONAL, JOURNAL_SYNC_NON_TRANSACTIONAL, LOG_JOURNAL_WRITE_RATE,
            JOURNAL_FILE_SIZE, JOURNAL_MIN_FILES, JOURNAL_COMPACT_PERCENTAGE, JOURNAL_COMPACT_MIN_FILES, JOURNAL_MAX_IO,
            PERF_BLAST_PAGES, RUN_SYNC_SPEED_TEST, SERVER_DUMP_INTERVAL};

    private final boolean registerRuntimeOnly;

    HornetQServerResourceDefinition(boolean registerRuntimeOnly) {
        super(HORNETQ_SERVER_PATH,
                MessagingExtension.getResourceDescriptionResolver(CommonAttributes.SERVER),
                HornetQServerAdd.INSTANCE,
                HornetQServerRemove.INSTANCE);
        this.registerRuntimeOnly = registerRuntimeOnly;
    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        super.registerOperations(resourceRegistration);
        if (registerRuntimeOnly) {
            HornetQServerControlHandler.INSTANCE.registerOperations(resourceRegistration, getResourceDescriptionResolver());
            JMSServerControlHandler.INSTANCE.registerOperations(resourceRegistration, getResourceDescriptionResolver());

            AddressSettingsResolveHandler.registerOperationHandler(resourceRegistration, getResourceDescriptionResolver());
        }

        // unsupported runtime operations exposed by HornetQServerControl
        // enableMessageCounters, disableMessageCounters
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        HornetQServerControlWriteHandler.INSTANCE.registerAttributes(resourceRegistration, registerRuntimeOnly);
        if (registerRuntimeOnly) {
            HornetQServerControlHandler.INSTANCE.registerAttributes(resourceRegistration);
        }
        // unsupported READ-ATTRIBUTES
        // getConnectors, getAddressNames, getQueueNames, getDivertNames, getBridgeNames,
        // unsupported JMSServerControlHandler READ-ATTRIBUTES
        // getTopicNames, getQueueNames, getConnectionFactoryNames,
    }

    /**
     * {@inheritDoc}
     * <p/>
     * The resource description has a small tweak from the standard
     */
    @Override
    public DescriptionProvider getDescriptionProvider(ImmutableManagementResourceRegistration resourceRegistration) {
        if (registerRuntimeOnly) {
            return super.getDescriptionProvider(resourceRegistration);
        } else {
            return new DefaultResourceDescriptionProvider(resourceRegistration, getResourceDescriptionResolver()) {
                @Override
                public ModelNode getModelDescription(Locale locale) {
                    ModelNode result = super.getModelDescription(locale);
                    result.get(CHILDREN, PATH, MIN_OCCURS).set(4);
                    result.get(CHILDREN, PATH, MAX_OCCURS).set(4);
                    return result;
                }
            };
        }
    }


}
