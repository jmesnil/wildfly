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

import static org.jboss.as.controller.PathElement.pathElement;
import static org.jboss.as.controller.SimpleAttributeDefinitionBuilder.create;
import static org.jboss.as.controller.client.helpers.MeasurementUnit.BYTES;
import static org.jboss.as.controller.client.helpers.MeasurementUnit.DAYS;
import static org.jboss.as.controller.client.helpers.MeasurementUnit.MILLISECONDS;
import static org.jboss.as.controller.client.helpers.MeasurementUnit.PERCENTAGE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MAX_OCCURS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MIN_OCCURS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PATH;
import static org.jboss.dmr.ModelType.BOOLEAN;
import static org.jboss.dmr.ModelType.INT;
import static org.jboss.dmr.ModelType.LONG;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.HA_POLICY;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.REPLICATION_MASTER;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.REPLICATION_SLAVE;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.SHARED_STORE_MASTER;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.SHARED_STORE_SLAVE;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import org.apache.activemq.api.config.ActiveMQDefaultConfiguration;
import org.apache.activemq.core.config.impl.ConfigurationImpl;
import org.apache.activemq.core.server.JournalType;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.as.controller.descriptions.DefaultResourceDescriptionProvider;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.validation.EnumValidator;
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.as.controller.registry.ImmutableManagementResourceRegistration;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.extension.messaging.activemq.ha.LiveOnlyDefinition;
import org.wildfly.extension.messaging.activemq.ha.ReplicationColocatedDefinition;
import org.wildfly.extension.messaging.activemq.ha.ReplicationMasterDefinition;
import org.wildfly.extension.messaging.activemq.ha.ReplicationSlaveDefinition;
import org.wildfly.extension.messaging.activemq.ha.SharedStoreColocatedDefinition;
import org.wildfly.extension.messaging.activemq.ha.SharedStoreMasterDefinition;
import org.wildfly.extension.messaging.activemq.ha.SharedStoreSlaveDefinition;
import org.wildfly.extension.messaging.activemq.jms.ConnectionFactoryDefinition;
import org.wildfly.extension.messaging.activemq.jms.JMSQueueDefinition;
import org.wildfly.extension.messaging.activemq.jms.JMSServerControlHandler;
import org.wildfly.extension.messaging.activemq.jms.JMSTopicDefinition;
import org.wildfly.extension.messaging.activemq.jms.PooledConnectionFactoryDefinition;

/**
 * {@link org.jboss.as.controller.ResourceDefinition} for the messaging subsystem HornetQServer resource.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class HornetQServerResourceDefinition extends PersistentResourceDefinition {

    public static final SimpleAttributeDefinition CLUSTER_PASSWORD = create("cluster-password", ModelType.STRING)
            .setAttributeGroup("cluster")
            .setXmlName("password")
            .setDefaultValue(new ModelNode(ActiveMQDefaultConfiguration.getDefaultClusterPassword()))
            .setAllowNull(true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .addAccessConstraint(SensitiveTargetAccessConstraintDefinition.CREDENTIAL)
            .addAccessConstraint(CommonAttributes.MESSAGING_SECURITY_DEF)
            .build();
    public static final SimpleAttributeDefinition CLUSTER_USER = create("cluster-user", ModelType.STRING)
            .setAttributeGroup("cluster")
            .setXmlName("user")
            .setDefaultValue(new ModelNode(ActiveMQDefaultConfiguration.getDefaultClusterUser()))
            .setAllowNull(true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .addAccessConstraint(SensitiveTargetAccessConstraintDefinition.CREDENTIAL)
            .addAccessConstraint(CommonAttributes.MESSAGING_SECURITY_DEF)
            .build();
    public static final AttributeDefinition SCHEDULED_THREAD_POOL_MAX_SIZE = create("scheduled-thread-pool-max-size", INT)
            .setDefaultValue(new ModelNode().set(ActiveMQDefaultConfiguration.getDefaultScheduledThreadPoolMaxSize()))
            .setAllowNull(true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();
    public static final SimpleAttributeDefinition SECURITY_DOMAIN = create("security-domain", ModelType.STRING)
            .setAttributeGroup("security")
            .setXmlName("domain")
            .setDefaultValue(new ModelNode("other"))
            .setAllowNull(true)
            .setAllowExpression(false) // references the security domain service name
            .setRestartAllServices()
            .addAccessConstraint(SensitiveTargetAccessConstraintDefinition.SECURITY_DOMAIN_REF)
            .addAccessConstraint(CommonAttributes.MESSAGING_SECURITY_DEF)
            .build();
    public static final AttributeDefinition THREAD_POOL_MAX_SIZE = create("thread-pool-max-size", INT)
            .setDefaultValue(new ModelNode().set(ActiveMQDefaultConfiguration.getDefaultThreadPoolMaxSize()))
            .setAllowNull(true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();
    public static final SimpleAttributeDefinition OVERRIDE_IN_VM_SECURITY = create("override-in-vm-security", BOOLEAN)
            .setAttributeGroup("security")
            .setDefaultValue(new ModelNode(true))
            .setAllowNull(true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();
    public static final SimpleAttributeDefinition WILD_CARD_ROUTING_ENABLED = create("wild-card-routing-enabled", BOOLEAN)
            .setDefaultValue(new ModelNode(ActiveMQDefaultConfiguration.isDefaultWildcardRoutingEnabled()))
            .setAllowNull(true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();
    public static final SimpleAttributeDefinition STATISTICS_ENABLED = create(ModelDescriptionConstants.STATISTICS_ENABLED, BOOLEAN)
            .setAttributeGroup("statistics")
            .setXmlName("enabled")
            .setDefaultValue(new ModelNode(false))
            .setAllowNull(true)
            .setAllowExpression(true)
            .build();
    // no default values, depends on whether NIO or AIO is used.
    public static final SimpleAttributeDefinition JOURNAL_BUFFER_SIZE = create("journal-buffer-size", LONG)
            .setAttributeGroup("journal")
            .setXmlName("buffer-size")
            .setMeasurementUnit(BYTES)
            .setAllowNull(true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();
    // no default values, depends on whether NIO or AIO is used.
    public static final SimpleAttributeDefinition JOURNAL_BUFFER_TIMEOUT = create("journal-buffer-timeout", LONG)
            .setAttributeGroup("journal")
            .setXmlName("buffer-timeout")
            .setMeasurementUnit(MILLISECONDS)
            .setAllowNull(true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();
    public static final SimpleAttributeDefinition JOURNAL_COMPACT_MIN_FILES = create("journal-compact-min-files", INT)
            .setAttributeGroup("journal")
            .setXmlName("compact-min-files")
            .setDefaultValue(new ModelNode(ActiveMQDefaultConfiguration.getDefaultJournalCompactMinFiles()))
            .setAllowNull(true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();
    public static final SimpleAttributeDefinition JOURNAL_COMPACT_PERCENTAGE = create("journal-compact-percentage", INT)
            .setAttributeGroup("journal")
            .setXmlName("compact-percentage")
            .setDefaultValue(new ModelNode(ActiveMQDefaultConfiguration.getDefaultJournalCompactPercentage()))
            .setMeasurementUnit(PERCENTAGE)
            .setAllowNull(true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();
    public static final SimpleAttributeDefinition JOURNAL_FILE_SIZE = create("journal-file-size", LONG)
            .setAttributeGroup("journal")
            .setXmlName("file-size")
            .setDefaultValue(new ModelNode(ActiveMQDefaultConfiguration.getDefaultJournalFileSize()))
            .setMeasurementUnit(BYTES)
            .setAllowNull(true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();
    // no default values, depends on whether NIO or AIO is used.
    public static final SimpleAttributeDefinition JOURNAL_MAX_IO = create("journal-max-io", INT)
            .setAttributeGroup("journal")
            .setXmlName("max-io")
            .setAllowNull(true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();
    public static final SimpleAttributeDefinition JOURNAL_MIN_FILES = create("journal-min-files", INT)
            .setAttributeGroup("journal")
            .setXmlName("min-files")
            .setDefaultValue(new ModelNode(ActiveMQDefaultConfiguration.getDefaultJournalMinFiles()))
            .setAllowNull(true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();
    public static final SimpleAttributeDefinition JOURNAL_SYNC_NON_TRANSACTIONAL = create("journal-sync-non-transactional", BOOLEAN)
            .setAttributeGroup("journal")
            .setXmlName("sync-non-transactional")
            .setDefaultValue(new ModelNode(ActiveMQDefaultConfiguration.isDefaultJournalSyncNonTransactional()))
            .setAllowNull(true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();
    public static final SimpleAttributeDefinition JOURNAL_SYNC_TRANSACTIONAL = create("journal-sync-transactional", BOOLEAN)
            .setAttributeGroup("journal")
            .setXmlName("sync-transactional")
            .setDefaultValue(new ModelNode(ActiveMQDefaultConfiguration.isDefaultJournalSyncTransactional()))
            .setAllowNull(true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();
    public static final SimpleAttributeDefinition JOURNAL_TYPE = create("journal-type", ModelType.STRING)
            .setAttributeGroup("journal")
            .setXmlName("type")
            .setDefaultValue(new ModelNode(ConfigurationImpl.DEFAULT_JOURNAL_TYPE.toString()))
            .setAllowNull(true)
            .setAllowExpression(true)
            .setValidator(new EnumValidator<JournalType>(JournalType.class, true, true))
            .setRestartAllServices()
            .build();
    public static final SimpleAttributeDefinition LOG_JOURNAL_WRITE_RATE = create("log-journal-write-rate", BOOLEAN)
            .setAttributeGroup("journal")
            .setXmlName("log-write-rate")
            .setDefaultValue(new ModelNode(ActiveMQDefaultConfiguration.isDefaultJournalLogWriteRate()))
            .setAllowNull(true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();
    public static final SimpleAttributeDefinition CONNECTION_TTL_OVERRIDE = create("connection-ttl-override", LONG)
            .setDefaultValue(new ModelNode(ActiveMQDefaultConfiguration.getDefaultConnectionTtlOverride()))
            .setMeasurementUnit(MILLISECONDS)
            .setAllowNull(true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();
    public static final SimpleAttributeDefinition ASYNC_CONNECTION_EXECUTION_ENABLED = create("async-connection-execution-enabled", BOOLEAN)
            .setDefaultValue(new ModelNode(ActiveMQDefaultConfiguration.isDefaultAsyncConnectionExecutionEnabled()))
            .setAllowNull(true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();
    public static final SimpleAttributeDefinition MESSAGE_COUNTER_MAX_DAY_HISTORY = create("message-counter-max-day-history", INT)
            .setAttributeGroup("statistics")
            .setDefaultValue(new ModelNode(ActiveMQDefaultConfiguration.getDefaultMessageCounterMaxDayHistory()))
            .setMeasurementUnit(DAYS)
            .setAllowNull(true)
            .setAllowExpression(true)
            .build();
    public static final SimpleAttributeDefinition MESSAGE_COUNTER_SAMPLE_PERIOD = create("message-counter-sample-period", LONG)
            .setAttributeGroup("statistics")
            .setDefaultValue(new ModelNode(ActiveMQDefaultConfiguration.getDefaultMessageCounterSamplePeriod()))
            .setMeasurementUnit(MILLISECONDS)
            .setAllowNull(true)
            .setAllowExpression(true)
            .build();
    public static final SimpleAttributeDefinition TRANSACTION_TIMEOUT = create("transaction-timeout", LONG)
            .setAttributeGroup("transaction")
            .setXmlName("timeout")
            .setDefaultValue(new ModelNode(ActiveMQDefaultConfiguration.getDefaultTransactionTimeout()))
            .setMeasurementUnit(MILLISECONDS)
            .setAllowNull(true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();
    public static final SimpleAttributeDefinition TRANSACTION_TIMEOUT_SCAN_PERIOD = create("transaction-timeout-scan-period", LONG)
            .setAttributeGroup("transaction")
            .setXmlName("scan-period")
            .setDefaultValue(new ModelNode(ActiveMQDefaultConfiguration.getDefaultTransactionTimeoutScanPeriod()))
            .setMeasurementUnit(MILLISECONDS)
            .setAllowNull(true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();
    public static final SimpleAttributeDefinition MESSAGE_EXPIRY_SCAN_PERIOD = create("message-expiry-scan-period", LONG)
            .setAttributeGroup("message-expiry")
            .setXmlName("scan-period")
            .setDefaultValue(new ModelNode(ActiveMQDefaultConfiguration.getDefaultMessageExpiryScanPeriod()))
            .setMeasurementUnit(MILLISECONDS)
            .setAllowNull(true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();
    public static final SimpleAttributeDefinition MESSAGE_EXPIRY_THREAD_PRIORITY = create("message-expiry-thread-priority", INT)
            .setAttributeGroup("message-expiry")
            .setXmlName("thread-priority")
            .setDefaultValue(new ModelNode(ActiveMQDefaultConfiguration.getDefaultMessageExpiryThreadPriority()))
            .setAllowNull(true)
            .setAllowExpression(true)
            .setValidator(new IntRangeValidator(Thread.MIN_PRIORITY, Thread.MAX_PRIORITY, true, true))
            .setRestartAllServices()
            .build();
    public static final SimpleAttributeDefinition PERF_BLAST_PAGES = create("perf-blast-pages", INT)
            .setAttributeGroup("debug")
            .setDefaultValue(new ModelNode(ActiveMQDefaultConfiguration.getDefaultJournalPerfBlastPages()))
            .setAllowNull(true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();
    public static final SimpleAttributeDefinition RUN_SYNC_SPEED_TEST = create("run-sync-speed-test", BOOLEAN)
            .setAttributeGroup("debug")
            .setDefaultValue(new ModelNode(ActiveMQDefaultConfiguration.isDefaultRunSyncSpeedTest()))
            .setAllowNull(true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();
    public static final SimpleAttributeDefinition SERVER_DUMP_INTERVAL = create("server-dump-interval", LONG)
            .setAttributeGroup("debug")
            .setDefaultValue(new ModelNode(ActiveMQDefaultConfiguration.getDefaultServerDumpInterval()))
            .setMeasurementUnit(MILLISECONDS)
            .setAllowNull(true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();
    public static final SimpleAttributeDefinition MEMORY_MEASURE_INTERVAL = create("memory-measure-interval", LONG)
            .setAttributeGroup("debug")
            .setDefaultValue(new ModelNode(ActiveMQDefaultConfiguration.getDefaultMemoryMeasureInterval()))
            .setMeasurementUnit(MILLISECONDS)
            .setAllowNull(true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();
    public static final SimpleAttributeDefinition MEMORY_WARNING_THRESHOLD = create("memory-warning-threshold", INT)
            .setAttributeGroup("debug")
            .setDefaultValue(new ModelNode(ActiveMQDefaultConfiguration.getDefaultMemoryWarningThreshold()))
            .setMeasurementUnit(PERCENTAGE)
            .setAllowNull(true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();
    public static final SimpleAttributeDefinition SECURITY_INVALIDATION_INTERVAL = create("security-invalidation-interval", LONG)
            .setAttributeGroup("security")
            .setXmlName("invalidation-interval")
            .setDefaultValue(new ModelNode(ActiveMQDefaultConfiguration.getDefaultSecurityInvalidationInterval()))
            .setMeasurementUnit(MILLISECONDS)
            .setAllowNull(true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .addAccessConstraint(CommonAttributes.MESSAGING_SECURITY_DEF)
            .build();
    public static final SimpleAttributeDefinition SECURITY_ENABLED = create("security-enabled", BOOLEAN)
            .setAttributeGroup("security")
            .setXmlName("enabled")
            .setDefaultValue(new ModelNode(ActiveMQDefaultConfiguration.isDefaultSecurityEnabled()))
            .setAllowNull(true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .addAccessConstraint(CommonAttributes.MESSAGING_SECURITY_DEF)
            .build();
    public static final SimpleAttributeDefinition PERSISTENCE_ENABLED = create("persistence-enabled", BOOLEAN)
            .setDefaultValue(new ModelNode(ActiveMQDefaultConfiguration.isDefaultPersistenceEnabled()))
            .setAllowNull(true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();
    public static final SimpleAttributeDefinition MANAGEMENT_NOTIFICATION_ADDRESS = create("management-notification-address", ModelType.STRING)
            .setAttributeGroup("management")
            .setXmlName("notification-address")
            .setDefaultValue(new ModelNode(ActiveMQDefaultConfiguration.getDefaultManagementNotificationAddress().toString()))
            .setAllowNull(true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .addAccessConstraint(CommonAttributes.MESSAGING_MANAGEMENT_DEF)
            .build();
    public static final SimpleAttributeDefinition MANAGEMENT_ADDRESS = create("management-address", ModelType.STRING)
            .setAttributeGroup("management")
            .setXmlName("address")
            .setDefaultValue(new ModelNode(ActiveMQDefaultConfiguration.getDefaultManagementAddress().toString()))
            .setAllowNull(true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .addAccessConstraint(CommonAttributes.MESSAGING_MANAGEMENT_DEF)
            .build();
    public static final SimpleAttributeDefinition JMX_MANAGEMENT_ENABLED = create("jmx-management-enabled", BOOLEAN)
            .setAttributeGroup("management")
            .setXmlName("jmx-enabled")
            .setDefaultValue(new ModelNode(false))
            .setAllowNull(true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .addAccessConstraint(CommonAttributes.MESSAGING_MANAGEMENT_DEF)
            .build();
    public static final SimpleAttributeDefinition JMX_DOMAIN = create("jmx-domain", ModelType.STRING)
            .setAttributeGroup("management")
            .setDefaultValue(new ModelNode(ActiveMQDefaultConfiguration.getDefaultJmxDomain()))
            .setAllowNull(true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .addAccessConstraint(CommonAttributes.MESSAGING_MANAGEMENT_DEF)
            .build();

    public static final SimpleAttributeDefinition PERSIST_ID_CACHE = create("persist-id-cache", BOOLEAN)
            .setDefaultValue(new ModelNode(ActiveMQDefaultConfiguration.isDefaultPersistIdCache()))
            .setAllowNull(true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();
    public static final SimpleAttributeDefinition PERSIST_DELIVERY_COUNT_BEFORE_DELIVERY = create("persist-delivery-count-before-delivery", BOOLEAN)
            .setDefaultValue(new ModelNode(ActiveMQDefaultConfiguration.isDefaultPersistDeliveryCountBeforeDelivery()))
            .setAllowNull(true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();
    public static final SimpleAttributeDefinition ID_CACHE_SIZE = create("id-cache-size", INT)
            .setDefaultValue(new ModelNode(ActiveMQDefaultConfiguration.getDefaultIdCacheSize()))
            .setAllowNull(true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();
    public static final SimpleAttributeDefinition PAGE_MAX_CONCURRENT_IO = create("page-max-concurrent-io", INT)
            .setDefaultValue(new ModelNode(ActiveMQDefaultConfiguration.getDefaultMaxConcurrentPageIo()))
            .setAllowNull(true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();
    public static final SimpleAttributeDefinition CREATE_BINDINGS_DIR = create("create-bindings-dir", BOOLEAN)
            .setAttributeGroup("journal")
            .setDefaultValue(new ModelNode(ActiveMQDefaultConfiguration.isDefaultCreateBindingsDir()))
            .setAllowNull(true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();
    public static final SimpleAttributeDefinition CREATE_JOURNAL_DIR = create("create-journal-dir", BOOLEAN)
            .setAttributeGroup("journal")
            .setDefaultValue(new ModelNode(ActiveMQDefaultConfiguration.isDefaultCreateJournalDir()))
            .setAllowNull(true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    public static final AttributeDefinition[] ATTRIBUTES = {PERSISTENCE_ENABLED, SCHEDULED_THREAD_POOL_MAX_SIZE,
            THREAD_POOL_MAX_SIZE, SECURITY_DOMAIN, SECURITY_ENABLED, SECURITY_INVALIDATION_INTERVAL, OVERRIDE_IN_VM_SECURITY, WILD_CARD_ROUTING_ENABLED,
            MANAGEMENT_ADDRESS, MANAGEMENT_NOTIFICATION_ADDRESS, CLUSTER_USER, CLUSTER_PASSWORD, JMX_MANAGEMENT_ENABLED,
            JMX_DOMAIN, STATISTICS_ENABLED, MESSAGE_COUNTER_SAMPLE_PERIOD, MESSAGE_COUNTER_MAX_DAY_HISTORY,
            CONNECTION_TTL_OVERRIDE, ASYNC_CONNECTION_EXECUTION_ENABLED, TRANSACTION_TIMEOUT, TRANSACTION_TIMEOUT_SCAN_PERIOD,
            MESSAGE_EXPIRY_SCAN_PERIOD, MESSAGE_EXPIRY_THREAD_PRIORITY, ID_CACHE_SIZE, PERSIST_ID_CACHE,
            CommonAttributes.REMOTING_INCOMING_INTERCEPTORS, CommonAttributes.REMOTING_OUTGOING_INTERCEPTORS,
            PERSIST_DELIVERY_COUNT_BEFORE_DELIVERY,
            PAGE_MAX_CONCURRENT_IO, CREATE_BINDINGS_DIR, CREATE_JOURNAL_DIR, JOURNAL_TYPE, JOURNAL_BUFFER_TIMEOUT,
            JOURNAL_BUFFER_SIZE, JOURNAL_SYNC_TRANSACTIONAL, JOURNAL_SYNC_NON_TRANSACTIONAL, LOG_JOURNAL_WRITE_RATE,
            JOURNAL_FILE_SIZE, JOURNAL_MIN_FILES, JOURNAL_COMPACT_PERCENTAGE, JOURNAL_COMPACT_MIN_FILES, JOURNAL_MAX_IO,
            PERF_BLAST_PAGES, RUN_SYNC_SPEED_TEST, SERVER_DUMP_INTERVAL, MEMORY_WARNING_THRESHOLD, MEMORY_MEASURE_INTERVAL,
    };

    private static PersistentResourceDefinition[] CHILDREN = {
            LiveOnlyDefinition.INSTANCE,
            new ReplicationMasterDefinition(pathElement(HA_POLICY, REPLICATION_MASTER), false),
            new ReplicationSlaveDefinition(pathElement(HA_POLICY, REPLICATION_SLAVE), false),
            ReplicationColocatedDefinition.INSTANCE,
            new SharedStoreMasterDefinition(pathElement(HA_POLICY, SHARED_STORE_MASTER), false),
            new SharedStoreSlaveDefinition(pathElement(HA_POLICY, SHARED_STORE_SLAVE), false),
            SharedStoreColocatedDefinition.INSTANCE,
            AddressSettingDefinition.INSTANCE,
            SecuritySettingDefinition.INSTANCE,
            HTTPConnectorDefinition.INSTANCE,
            RemoteTransportDefinition.CONNECTOR_INSTANCE,
            InVMTransportDefinition.CONNECTOR_INSTANCE,
            GenericTransportDefinition.CONNECTOR_INSTANCE,
            HTTPAcceptorDefinition.INSTANCE,
            RemoteTransportDefinition.ACCEPTOR_INSTANCE,
            InVMTransportDefinition.ACCEPTOR_INSTANCE,
            GenericTransportDefinition.ACCEPTOR_INSTANCE,
            QueueDefinition.INSTANCE,
            BroadcastGroupDefinition.INSTANCE,
            DiscoveryGroupDefinition.INSTANCE,
            BridgeDefinition.INSTANCE,
            ClusterConnectionDefinition.INSTANCE,
            DivertDefinition.INSTANCE,
            ConnectorServiceDefinition.INSTANCE,
            GroupingHandlerDefinition.INSTANCE,
            JMSQueueDefinition.INSTANCE,
            JMSTopicDefinition.INSTANCE,
            ConnectionFactoryDefinition.INSTANCE,
            PooledConnectionFactoryDefinition.INSTANCE
    };

    protected static final PersistentResourceDefinition INSTANCE = new HornetQServerResourceDefinition(false);
    private final boolean registerRuntimeOnly;

    HornetQServerResourceDefinition(boolean registerRuntimeOnly) {
        super(MessagingExtension.SERVER_PATH,
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
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        HornetQServerControlWriteHandler.INSTANCE.registerAttributes(resourceRegistration, registerRuntimeOnly);
        if (registerRuntimeOnly) {
            HornetQServerControlHandler.INSTANCE.registerAttributes(resourceRegistration);
        }
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return Arrays.asList(ATTRIBUTES);
    }

    @Override
    protected List<? extends PersistentResourceDefinition> getChildren() {
        return Arrays.asList(CHILDREN);
    }

    @Override
    public void registerChildren(ManagementResourceRegistration resourceRegistration) {
        super.registerChildren(resourceRegistration);

        ManagementResourceRegistration runtimeQueue = resourceRegistration.registerSubModel(QueueDefinition.newRuntimeQueueDefinition(true));
        runtimeQueue.setRuntimeOnly(true);
        ManagementResourceRegistration coreAddress = resourceRegistration.registerSubModel(CoreAddressDefinition.INSTANCE);
        coreAddress.setRuntimeOnly(true);
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
                    result.get(ModelDescriptionConstants.CHILDREN, PATH, MIN_OCCURS).set(4);
                    result.get(ModelDescriptionConstants.CHILDREN, PATH, MAX_OCCURS).set(4);
                    return result;
                }
            };
        }
    }


}
