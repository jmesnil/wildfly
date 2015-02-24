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

import static org.jboss.as.controller.PersistentResourceXMLDescription.builder;
import static org.wildfly.extension.messaging.activemq.Namespace.CURRENT;

import java.util.List;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PersistentResourceXMLDescription;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2015 Red Hat inc.
 */
public class MessagingSubsystemParser_1_1  implements XMLStreamConstants, XMLElementReader<List<ModelNode>>, XMLElementWriter<SubsystemMarshallingContext> {

    protected static final MessagingSubsystemParser_1_1 INSTANCE = new MessagingSubsystemParser_1_1();

    private static final PersistentResourceXMLDescription xmlDescription;

    static {
        xmlDescription = builder(MessagingSubsystemRootResourceDefinition.INSTANCE)
                .addChild(
                        builder(HornetQServerResourceDefinition.INSTANCE)
                                .addAttributes(
                                        // no attribute groups
                                        HornetQServerResourceDefinition.PERSISTENCE_ENABLED,
                                        HornetQServerResourceDefinition.PERSIST_ID_CACHE,
                                        HornetQServerResourceDefinition.PERSIST_DELIVERY_COUNT_BEFORE_DELIVERY,
                                        HornetQServerResourceDefinition.ID_CACHE_SIZE,
                                        HornetQServerResourceDefinition.PAGE_MAX_CONCURRENT_IO,
                                        HornetQServerResourceDefinition.SCHEDULED_THREAD_POOL_MAX_SIZE,
                                        HornetQServerResourceDefinition.THREAD_POOL_MAX_SIZE,
                                        HornetQServerResourceDefinition.WILD_CARD_ROUTING_ENABLED,
                                        HornetQServerResourceDefinition.CONNECTION_TTL_OVERRIDE,
                                        HornetQServerResourceDefinition.ASYNC_CONNECTION_EXECUTION_ENABLED,
                                        // security
                                        HornetQServerResourceDefinition.SECURITY_DOMAIN,
                                        HornetQServerResourceDefinition.SECURITY_ENABLED,
                                        HornetQServerResourceDefinition.SECURITY_INVALIDATION_INTERVAL,
                                        HornetQServerResourceDefinition.OVERRIDE_IN_VM_SECURITY,
                                        // cluster
                                        HornetQServerResourceDefinition.CLUSTER_USER,
                                        HornetQServerResourceDefinition.CLUSTER_PASSWORD,
                                        // management
                                        HornetQServerResourceDefinition.MANAGEMENT_ADDRESS,
                                        HornetQServerResourceDefinition.MANAGEMENT_NOTIFICATION_ADDRESS,
                                        HornetQServerResourceDefinition.JMX_MANAGEMENT_ENABLED,
                                        HornetQServerResourceDefinition.JMX_DOMAIN,
                                        // journal
                                        HornetQServerResourceDefinition.JOURNAL_TYPE,
                                        HornetQServerResourceDefinition.JOURNAL_BUFFER_TIMEOUT,
                                        HornetQServerResourceDefinition.JOURNAL_BUFFER_SIZE,
                                        HornetQServerResourceDefinition.JOURNAL_SYNC_TRANSACTIONAL,
                                        HornetQServerResourceDefinition.JOURNAL_SYNC_NON_TRANSACTIONAL,
                                        HornetQServerResourceDefinition.LOG_JOURNAL_WRITE_RATE,
                                        HornetQServerResourceDefinition.JOURNAL_FILE_SIZE,
                                        HornetQServerResourceDefinition.JOURNAL_MIN_FILES,
                                        HornetQServerResourceDefinition.JOURNAL_COMPACT_PERCENTAGE,
                                        HornetQServerResourceDefinition.JOURNAL_COMPACT_MIN_FILES,
                                        HornetQServerResourceDefinition.JOURNAL_MAX_IO,
                                        HornetQServerResourceDefinition.CREATE_BINDINGS_DIR,
                                        HornetQServerResourceDefinition.CREATE_JOURNAL_DIR,
                                        // statistics
                                        HornetQServerResourceDefinition.STATISTICS_ENABLED,
                                        HornetQServerResourceDefinition.MESSAGE_COUNTER_SAMPLE_PERIOD,
                                        HornetQServerResourceDefinition.MESSAGE_COUNTER_MAX_DAY_HISTORY,
                                        // transaction
                                        HornetQServerResourceDefinition.TRANSACTION_TIMEOUT,
                                        HornetQServerResourceDefinition.TRANSACTION_TIMEOUT_SCAN_PERIOD,
                                        // message expiry
                                        HornetQServerResourceDefinition.MESSAGE_EXPIRY_SCAN_PERIOD,
                                        HornetQServerResourceDefinition.MESSAGE_EXPIRY_THREAD_PRIORITY,
                                        // debug
                                        HornetQServerResourceDefinition.PERF_BLAST_PAGES,
                                        HornetQServerResourceDefinition.RUN_SYNC_SPEED_TEST,
                                        HornetQServerResourceDefinition.SERVER_DUMP_INTERVAL,
                                        HornetQServerResourceDefinition.MEMORY_MEASURE_INTERVAL,
                                        HornetQServerResourceDefinition.MEMORY_WARNING_THRESHOLD
                                )
                                .addChild(builder(QueueDefinition.newQueueDefinition(false))
                                                .addAttributes(QueueDefinition.ADDRESS,
                                                        CommonAttributes.DURABLE,
                                                        CommonAttributes.FILTER)
                                ) // End of QueueDefinition
                                .addChild(builder(SecuritySettingDefinition.INSTANCE)
                                        .addChild(builder(SecurityRoleDefinition.INSTANCE)
                                                        .addAttributes(
                                                                SecurityRoleDefinition.SEND,
                                                                SecurityRoleDefinition.CONSUME,
                                                                SecurityRoleDefinition.CREATE_DURABLE_QUEUE,
                                                                SecurityRoleDefinition.DELETE_DURABLE_QUEUE,
                                                                SecurityRoleDefinition.CREATE_NON_DURABLE_QUEUE,
                                                                SecurityRoleDefinition.DELETE_NON_DURABLE_QUEUE,
                                                                SecurityRoleDefinition.MANAGE
                                                        )
                                        ) // End of SecurityRoleDefinition
                                ) // End of SecuritySettingDefinition
                                .addChild(builder(AddressSettingDefinition.INSTANCE)
                                        .addAttributes(
                                                CommonAttributes.DEAD_LETTER_ADDRESS,
                                                CommonAttributes.EXPIRY_ADDRESS,
                                                AddressSettingDefinition.EXPIRY_DELAY,
                                                AddressSettingDefinition.REDELIVERY_DELAY,
                                                AddressSettingDefinition.REDELIVERY_MULTIPLIER,
                                                AddressSettingDefinition.MAX_DELIVERY_ATTEMPTS,
                                                AddressSettingDefinition.MAX_REDELIVERY_DELAY,
                                                AddressSettingDefinition.MAX_SIZE_BYTES,
                                                AddressSettingDefinition.PAGE_SIZE_BYTES,
                                                AddressSettingDefinition.PAGE_MAX_CACHE_SIZE,
                                                AddressSettingDefinition.ADDRESS_FULL_MESSAGE_POLICY,
                                                AddressSettingDefinition.MESSAGE_COUNTER_HISTORY_DAY_LIMIT,
                                                AddressSettingDefinition.LAST_VALUE_QUEUE,
                                                AddressSettingDefinition.REDISTRIBUTION_DELAY,
                                                AddressSettingDefinition.SEND_TO_DLA_ON_NO_ROUTE

                                        )
                                ) // End of AddressSettingDefinition

                ) // End of HornetQServerResourceDefinition
                .build();
    }

    private MessagingSubsystemParser_1_1() {
    }

    @Override
    public void writeContent(XMLExtendedStreamWriter writer, SubsystemMarshallingContext context) throws XMLStreamException {
        ModelNode model = new ModelNode();
        model.get(MessagingSubsystemRootResourceDefinition.INSTANCE.getPathElement().getKeyValuePair()).set(context.getModelNode());
        xmlDescription.persist(writer, model, CURRENT.getUriString());
    }

    @Override
    public void readElement(XMLExtendedStreamReader reader, List<ModelNode> list) throws XMLStreamException {
        xmlDescription.parse(reader, PathAddress.EMPTY_ADDRESS, list);
    }
}
