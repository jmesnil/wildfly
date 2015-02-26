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
import org.wildfly.extension.messaging.activemq.jms.ConnectionFactoryAttributes;
import org.wildfly.extension.messaging.activemq.jms.ConnectionFactoryDefinition;
import org.wildfly.extension.messaging.activemq.jms.JMSQueueDefinition;
import org.wildfly.extension.messaging.activemq.jms.JMSTopicDefinition;
import org.wildfly.extension.messaging.activemq.jms.PooledConnectionFactoryDefinition;
import org.wildfly.extension.messaging.activemq.jms.bridge.JMSBridgeDefinition;

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
                                        // remoting-interceptors
                                        CommonAttributes.REMOTING_INCOMING_INTERCEPTORS,
                                        CommonAttributes.REMOTING_OUTGOING_INTERCEPTORS,
                                        // debug
                                        HornetQServerResourceDefinition.PERF_BLAST_PAGES,
                                        HornetQServerResourceDefinition.RUN_SYNC_SPEED_TEST,
                                        HornetQServerResourceDefinition.SERVER_DUMP_INTERVAL,
                                        HornetQServerResourceDefinition.MEMORY_MEASURE_INTERVAL,
                                        HornetQServerResourceDefinition.MEMORY_WARNING_THRESHOLD)
                                .addChild(
                                        builder(PathDefinition.BINDINGS_INSTANCE)
                                                .addAttributes(
                                                        PathDefinition.PATHS.get(CommonAttributes.BINDINGS_DIRECTORY),
                                                        PathDefinition.RELATIVE_TO))
                                .addChild(
                                        builder(PathDefinition.JOURNAL_INSTANCE)
                                                .addAttributes(
                                                        PathDefinition.PATHS.get(CommonAttributes.JOURNAL_DIRECTORY),
                                                        PathDefinition.RELATIVE_TO))
                                .addChild(
                                        builder(PathDefinition.LARGE_MESSAGES_INSTANCE)
                                                .addAttributes(
                                                        PathDefinition.PATHS.get(CommonAttributes.LARGE_MESSAGES_DIRECTORY),
                                                        PathDefinition.RELATIVE_TO))
                                .addChild(
                                        builder(PathDefinition.PAGING_INSTANCE)
                                                .addAttributes(
                                                        PathDefinition.PATHS.get(CommonAttributes.PAGING_DIRECTORY),
                                                        PathDefinition.RELATIVE_TO))
                                .addChild(
                                        builder(QueueDefinition.INSTANCE)
                                                .addAttributes(
                                                        QueueDefinition.ADDRESS,
                                                        CommonAttributes.DURABLE,
                                                        CommonAttributes.FILTER))
                                .addChild(
                                        builder(SecuritySettingDefinition.INSTANCE)
                                                .addChild(
                                                        builder(SecurityRoleDefinition.INSTANCE)
                                                                .addAttributes(
                                                                        SecurityRoleDefinition.SEND,
                                                                        SecurityRoleDefinition.CONSUME,
                                                                        SecurityRoleDefinition.CREATE_DURABLE_QUEUE,
                                                                        SecurityRoleDefinition.DELETE_DURABLE_QUEUE,
                                                                        SecurityRoleDefinition.CREATE_NON_DURABLE_QUEUE,
                                                                        SecurityRoleDefinition.DELETE_NON_DURABLE_QUEUE,
                                                                        SecurityRoleDefinition.MANAGE)))
                                .addChild(
                                        builder(AddressSettingDefinition.INSTANCE)
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
                                                        AddressSettingDefinition.SEND_TO_DLA_ON_NO_ROUTE))
                                .addChild(
                                        builder(HTTPConnectorDefinition.INSTANCE)
                                                .addAttributes(
                                                        HTTPConnectorDefinition.SOCKET_BINDING,
                                                        CommonAttributes.PARAMS))
                                .addChild(
                                        builder(RemoteTransportDefinition.CONNECTOR_INSTANCE)
                                                .addAttributes(
                                                        RemoteTransportDefinition.SOCKET_BINDING,
                                                        CommonAttributes.PARAMS))
                                .addChild(
                                        builder(InVMTransportDefinition.CONNECTOR_INSTANCE)
                                                .addAttributes(
                                                        InVMTransportDefinition.SERVER_ID,
                                                        CommonAttributes.PARAMS))
                                .addChild(
                                        builder(GenericTransportDefinition.CONNECTOR_INSTANCE)
                                                .addAttributes(
                                                        GenericTransportDefinition.SOCKET_BINDING,
                                                        CommonAttributes.FACTORY_CLASS,
                                                        CommonAttributes.PARAMS))
                                .addChild(
                                        builder(HTTPAcceptorDefinition.INSTANCE)
                                                .addAttributes(
                                                        HTTPAcceptorDefinition.HTTP_LISTENER,
                                                        CommonAttributes.PARAMS))
                                .addChild(
                                        builder(RemoteTransportDefinition.ACCEPTOR_INSTANCE)
                                                .addAttributes(
                                                        RemoteTransportDefinition.SOCKET_BINDING,
                                                        CommonAttributes.PARAMS))
                                .addChild(
                                        builder(InVMTransportDefinition.ACCEPTOR_INSTANCE)
                                                .addAttributes(
                                                        InVMTransportDefinition.SERVER_ID,
                                                        CommonAttributes.PARAMS))
                                .addChild(
                                        builder(GenericTransportDefinition.ACCEPTOR_INSTANCE)
                                                .addAttributes(
                                                        GenericTransportDefinition.SOCKET_BINDING,
                                                        CommonAttributes.FACTORY_CLASS,
                                                        CommonAttributes.PARAMS))
                                .addChild(
                                        builder(BroadcastGroupDefinition.INSTANCE)
                                                .addAttributes(
                                                        CommonAttributes.JGROUPS_STACK,
                                                        CommonAttributes.JGROUPS_CHANNEL,
                                                        CommonAttributes.SOCKET_BINDING,
                                                        BroadcastGroupDefinition.BROADCAST_PERIOD,
                                                        BroadcastGroupDefinition.CONNECTOR_REFS))
                                .addChild(
                                        builder(DiscoveryGroupDefinition.INSTANCE)
                                                .addAttributes(
                                                        CommonAttributes.JGROUPS_STACK,
                                                        CommonAttributes.JGROUPS_CHANNEL,
                                                        CommonAttributes.SOCKET_BINDING,
                                                        DiscoveryGroupDefinition.REFRESH_TIMEOUT,
                                                        DiscoveryGroupDefinition.INITIAL_WAIT_TIMEOUT))
                                .addChild(
                                        builder(ClusterConnectionDefinition.INSTANCE)
                                                .addAttributes(
                                                        ClusterConnectionDefinition.ADDRESS,
                                                        ClusterConnectionDefinition.CONNECTOR_NAME,
                                                        ClusterConnectionDefinition.CHECK_PERIOD,
                                                        ClusterConnectionDefinition.CONNECTION_TTL,
                                                        CommonAttributes.MIN_LARGE_MESSAGE_SIZE,
                                                        CommonAttributes.CALL_TIMEOUT,
                                                        CommonAttributes.CALL_FAILOVER_TIMEOUT,
                                                        ClusterConnectionDefinition.RETRY_INTERVAL,
                                                        ClusterConnectionDefinition.RETRY_INTERVAL_MULTIPLIER,
                                                        ClusterConnectionDefinition.MAX_RETRY_INTERVAL,
                                                        ClusterConnectionDefinition.INITIAL_CONNECT_ATTEMPTS,
                                                        ClusterConnectionDefinition.RECONNECT_ATTEMPTS,
                                                        ClusterConnectionDefinition.USE_DUPLICATE_DETECTION,
                                                        ClusterConnectionDefinition.FORWARD_WHEN_NO_CONSUMERS,
                                                        ClusterConnectionDefinition.MAX_HOPS,
                                                        CommonAttributes.BRIDGE_CONFIRMATION_WINDOW_SIZE,
                                                        ClusterConnectionDefinition.NOTIFICATION_ATTEMPTS,
                                                        ClusterConnectionDefinition.NOTIFICATION_INTERVAL,
                                                        ClusterConnectionDefinition.CONNECTOR_REFS,
                                                        ClusterConnectionDefinition.ALLOW_DIRECT_CONNECTIONS_ONLY,
                                                        ClusterConnectionDefinition.DISCOVERY_GROUP_NAME))
                                .addChild(
                                        builder(GroupingHandlerDefinition.INSTANCE)
                                                .addAttributes(
                                                        GroupingHandlerDefinition.TYPE,
                                                        GroupingHandlerDefinition.GROUPING_HANDLER_ADDRESS,
                                                        GroupingHandlerDefinition.TIMEOUT,
                                                        GroupingHandlerDefinition.GROUP_TIMEOUT,
                                                        GroupingHandlerDefinition.REAPER_PERIOD))
                                .addChild(
                                        builder(DivertDefinition.INSTANCE)
                                                .addAttributes(
                                                        DivertDefinition.ROUTING_NAME,
                                                        DivertDefinition.ADDRESS,
                                                        DivertDefinition.FORWARDING_ADDRESS,
                                                        CommonAttributes.FILTER,
                                                        CommonAttributes.TRANSFORMER_CLASS_NAME,
                                                        DivertDefinition.EXCLUSIVE))
                                .addChild(
                                        builder(BridgeDefinition.INSTANCE)
                                                .addAttributes(
                                                        BridgeDefinition.QUEUE_NAME,
                                                        BridgeDefinition.FORWARDING_ADDRESS,
                                                        CommonAttributes.HA,
                                                        CommonAttributes.FILTER,
                                                        CommonAttributes.TRANSFORMER_CLASS_NAME,
                                                        CommonAttributes.MIN_LARGE_MESSAGE_SIZE,
                                                        CommonAttributes.CHECK_PERIOD,
                                                        CommonAttributes.CONNECTION_TTL,
                                                        CommonAttributes.RETRY_INTERVAL,
                                                        CommonAttributes.RETRY_INTERVAL_MULTIPLIER,
                                                        CommonAttributes.MAX_RETRY_INTERVAL,
                                                        BridgeDefinition.INITIAL_CONNECT_ATTEMPTS,
                                                        BridgeDefinition.RECONNECT_ATTEMPTS,
                                                        BridgeDefinition.RECONNECT_ATTEMPTS_ON_SAME_NODE,
                                                        BridgeDefinition.USE_DUPLICATE_DETECTION,
                                                        CommonAttributes.BRIDGE_CONFIRMATION_WINDOW_SIZE,
                                                        BridgeDefinition.USER,
                                                        BridgeDefinition.PASSWORD,
                                                        BridgeDefinition.CONNECTOR_REFS,
                                                        BridgeDefinition.DISCOVERY_GROUP_NAME))
                                .addChild(
                                        builder(ConnectorServiceDefinition.INSTANCE)
                                                .addAttributes(
                                                        CommonAttributes.FACTORY_CLASS,
                                                        CommonAttributes.PARAMS))
                                                .addChild(
                                                        builder(JMSQueueDefinition.INSTANCE)
                                                                .addAttributes(
                                                                        CommonAttributes.DESTINATION_ENTRIES,
                                                                        CommonAttributes.SELECTOR,
                                                                        CommonAttributes.DURABLE))
                                                .addChild(
                                                        builder(JMSTopicDefinition.INSTANCE)
                                                                .addAttributes(
                                                                        CommonAttributes.DESTINATION_ENTRIES))
                                                .addChild(
                                                        builder(ConnectionFactoryDefinition.INSTANCE)
                                                                .addAttributes(
                                                                        // common
                                                                        ConnectionFactoryAttributes.Common.DISCOVERY_GROUP_NAME,
                                                                        ConnectionFactoryAttributes.Common.CONNECTOR,
                                                                        ConnectionFactoryAttributes.Common.ENTRIES,
                                                                        CommonAttributes.HA,
                                                                        ConnectionFactoryAttributes.Common.CLIENT_FAILURE_CHECK_PERIOD,
                                                                        ConnectionFactoryAttributes.Common.CONNECTION_TTL,
                                                                        CommonAttributes.CALL_TIMEOUT,
                                                                        CommonAttributes.CALL_FAILOVER_TIMEOUT,
                                                                        ConnectionFactoryAttributes.Common.CONSUMER_WINDOW_SIZE,
                                                                        ConnectionFactoryAttributes.Common.CONSUMER_MAX_RATE,
                                                                        ConnectionFactoryAttributes.Common.CONFIRMATION_WINDOW_SIZE,
                                                                        ConnectionFactoryAttributes.Common.PRODUCER_WINDOW_SIZE,
                                                                        ConnectionFactoryAttributes.Common.PRODUCER_MAX_RATE,
                                                                        ConnectionFactoryAttributes.Common.COMPRESS_LARGE_MESSAGES,
                                                                        ConnectionFactoryAttributes.Common.CACHE_LARGE_MESSAGE_CLIENT,
                                                                        CommonAttributes.MIN_LARGE_MESSAGE_SIZE,
                                                                        CommonAttributes.CLIENT_ID,
                                                                        ConnectionFactoryAttributes.Common.DUPS_OK_BATCH_SIZE,
                                                                        ConnectionFactoryAttributes.Common.TRANSACTION_BATCH_SIZE,
                                                                        ConnectionFactoryAttributes.Common.BLOCK_ON_ACKNOWLEDGE,
                                                                        ConnectionFactoryAttributes.Common.BLOCK_ON_DURABLE_SEND,
                                                                        ConnectionFactoryAttributes.Common.BLOCK_ON_NON_DURABLE_SEND,
                                                                        ConnectionFactoryAttributes.Common.AUTO_GROUP,
                                                                        ConnectionFactoryAttributes.Common.PRE_ACKNOWLEDGE,
                                                                        ConnectionFactoryAttributes.Common.RETRY_INTERVAL,
                                                                        ConnectionFactoryAttributes.Common.RETRY_INTERVAL_MULTIPLIER,
                                                                        CommonAttributes.MAX_RETRY_INTERVAL,
                                                                        ConnectionFactoryAttributes.Common.RECONNECT_ATTEMPTS,
                                                                        ConnectionFactoryAttributes.Common.FAILOVER_ON_INITIAL_CONNECTION,
                                                                        ConnectionFactoryAttributes.Common.CONNECTION_LOAD_BALANCING_CLASS_NAME,
                                                                        ConnectionFactoryAttributes.Common.USE_GLOBAL_POOLS,
                                                                        ConnectionFactoryAttributes.Common.SCHEDULED_THREAD_POOL_MAX_SIZE,
                                                                        ConnectionFactoryAttributes.Common.THREAD_POOL_MAX_SIZE,
                                                                        ConnectionFactoryAttributes.Common.GROUP_ID,
                                                                        // regular
                                                                        ConnectionFactoryAttributes.Regular.FACTORY_TYPE))
                                                .addChild(
                                                        builder(PooledConnectionFactoryDefinition.INSTANCE)
                                                                .addAttributes(
                                                                        // common
                                                                        ConnectionFactoryAttributes.Common.DISCOVERY_GROUP_NAME,
                                                                        ConnectionFactoryAttributes.Common.CONNECTOR,
                                                                        ConnectionFactoryAttributes.Common.ENTRIES,
                                                                        CommonAttributes.HA,
                                                                        ConnectionFactoryAttributes.Common.CLIENT_FAILURE_CHECK_PERIOD,
                                                                        ConnectionFactoryAttributes.Common.CONNECTION_TTL,
                                                                        CommonAttributes.CALL_TIMEOUT,
                                                                        CommonAttributes.CALL_FAILOVER_TIMEOUT,
                                                                        ConnectionFactoryAttributes.Common.CONSUMER_WINDOW_SIZE,
                                                                        ConnectionFactoryAttributes.Common.CONSUMER_MAX_RATE,
                                                                        ConnectionFactoryAttributes.Common.CONFIRMATION_WINDOW_SIZE,
                                                                        ConnectionFactoryAttributes.Common.PRODUCER_WINDOW_SIZE,
                                                                        ConnectionFactoryAttributes.Common.PRODUCER_MAX_RATE,
                                                                        ConnectionFactoryAttributes.Common.COMPRESS_LARGE_MESSAGES,
                                                                        ConnectionFactoryAttributes.Common.CACHE_LARGE_MESSAGE_CLIENT,
                                                                        CommonAttributes.MIN_LARGE_MESSAGE_SIZE,
                                                                        CommonAttributes.CLIENT_ID,
                                                                        ConnectionFactoryAttributes.Common.DUPS_OK_BATCH_SIZE,
                                                                        ConnectionFactoryAttributes.Common.TRANSACTION_BATCH_SIZE,
                                                                        ConnectionFactoryAttributes.Common.BLOCK_ON_ACKNOWLEDGE,
                                                                        ConnectionFactoryAttributes.Common.BLOCK_ON_DURABLE_SEND,
                                                                        ConnectionFactoryAttributes.Common.BLOCK_ON_NON_DURABLE_SEND,
                                                                        ConnectionFactoryAttributes.Common.AUTO_GROUP,
                                                                        ConnectionFactoryAttributes.Common.PRE_ACKNOWLEDGE,
                                                                        ConnectionFactoryAttributes.Common.RETRY_INTERVAL,
                                                                        ConnectionFactoryAttributes.Common.RETRY_INTERVAL_MULTIPLIER,
                                                                        CommonAttributes.MAX_RETRY_INTERVAL,
                                                                        ConnectionFactoryAttributes.Common.RECONNECT_ATTEMPTS,
                                                                        ConnectionFactoryAttributes.Common.FAILOVER_ON_INITIAL_CONNECTION,
                                                                        ConnectionFactoryAttributes.Common.CONNECTION_LOAD_BALANCING_CLASS_NAME,
                                                                        ConnectionFactoryAttributes.Common.USE_GLOBAL_POOLS,
                                                                        ConnectionFactoryAttributes.Common.SCHEDULED_THREAD_POOL_MAX_SIZE,
                                                                        ConnectionFactoryAttributes.Common.THREAD_POOL_MAX_SIZE,
                                                                        ConnectionFactoryAttributes.Common.GROUP_ID,
                                                                        // pooled
                                                                        ConnectionFactoryAttributes.Pooled.USE_JNDI,
                                                                        ConnectionFactoryAttributes.Pooled.JNDI_PARAMS,
                                                                        ConnectionFactoryAttributes.Pooled.USE_LOCAL_TX,
                                                                        ConnectionFactoryAttributes.Pooled.SETUP_ATTEMPTS,
                                                                        ConnectionFactoryAttributes.Pooled.SETUP_INTERVAL,
                                                                        ConnectionFactoryAttributes.Pooled.TRANSACTION,
                                                                        ConnectionFactoryAttributes.Pooled.USER,
                                                                        ConnectionFactoryAttributes.Pooled.PASSWORD,
                                                                        ConnectionFactoryAttributes.Pooled.MIN_POOL_SIZE,
                                                                        ConnectionFactoryAttributes.Pooled.MAX_POOL_SIZE,
                                                                        ConnectionFactoryAttributes.Pooled.USE_AUTO_RECOVERY,
                                                                        ConnectionFactoryAttributes.Pooled.INITIAL_MESSAGE_PACKET_SIZE,
                                                                        ConnectionFactoryAttributes.Pooled.INITIAL_CONNECT_ATTEMPTS)))
                                .addChild(
                                        builder(JMSBridgeDefinition.INSTANCE)
                                                .addAttributes(
                                                        JMSBridgeDefinition.MODULE,
                                                        JMSBridgeDefinition.QUALITY_OF_SERVICE,
                                                        JMSBridgeDefinition.FAILURE_RETRY_INTERVAL,
                                                        JMSBridgeDefinition.MAX_RETRIES,
                                                        JMSBridgeDefinition.MAX_BATCH_SIZE,
                                                        JMSBridgeDefinition.MAX_BATCH_TIME,
                                                        CommonAttributes.SELECTOR,
                                                        JMSBridgeDefinition.SUBSCRIPTION_NAME,
                                                        CommonAttributes.CLIENT_ID,
                                                        JMSBridgeDefinition.ADD_MESSAGE_ID_IN_HEADER,
                                                        JMSBridgeDefinition.SOURCE_CONNECTION_FACTORY,
                                                        JMSBridgeDefinition.SOURCE_DESTINATION,
                                                        JMSBridgeDefinition.SOURCE_USER,
                                                        JMSBridgeDefinition.SOURCE_PASSWORD,
                                                        JMSBridgeDefinition.TARGET_CONNECTION_FACTORY,
                                                        JMSBridgeDefinition.TARGET_DESTINATION,
                                                        JMSBridgeDefinition.TARGET_USER,
                                                        JMSBridgeDefinition.TARGET_PASSWORD,
                                                        JMSBridgeDefinition.SOURCE_CONTEXT,
                                                        JMSBridgeDefinition.TARGET_CONTEXT))
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
