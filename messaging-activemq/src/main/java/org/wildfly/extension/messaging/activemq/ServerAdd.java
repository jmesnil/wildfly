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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PATH;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.ADDRESS_SETTING;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.BINDINGS_DIRECTORY;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.BROADCAST_GROUP;
import static org.wildfly.extension.messaging.activemq.ServerDefinition.CREATE_BINDINGS_DIR;
import static org.wildfly.extension.messaging.activemq.ServerDefinition.CREATE_JOURNAL_DIR;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.DISCOVERY_GROUP;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.HTTP_ACCEPTOR;
import static org.wildfly.extension.messaging.activemq.ServerDefinition.ID_CACHE_SIZE;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.JGROUPS_CHANNEL;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.JGROUPS_STACK;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.JOURNAL_DIRECTORY;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.LARGE_MESSAGES_DIRECTORY;
import static org.wildfly.extension.messaging.activemq.ServerDefinition.PAGE_MAX_CONCURRENT_IO;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.PAGING_DIRECTORY;
import static org.wildfly.extension.messaging.activemq.ServerDefinition.PERSIST_DELIVERY_COUNT_BEFORE_DELIVERY;
import static org.wildfly.extension.messaging.activemq.ServerDefinition.PERSIST_ID_CACHE;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.SECURITY_SETTING;
import static org.wildfly.extension.messaging.activemq.ServerDefinition.ASYNC_CONNECTION_EXECUTION_ENABLED;
import static org.wildfly.extension.messaging.activemq.ServerDefinition.CLUSTER_PASSWORD;
import static org.wildfly.extension.messaging.activemq.ServerDefinition.CLUSTER_USER;
import static org.wildfly.extension.messaging.activemq.ServerDefinition.CONNECTION_TTL_OVERRIDE;
import static org.wildfly.extension.messaging.activemq.ServerDefinition.JMX_DOMAIN;
import static org.wildfly.extension.messaging.activemq.ServerDefinition.JMX_MANAGEMENT_ENABLED;
import static org.wildfly.extension.messaging.activemq.ServerDefinition.JOURNAL_BUFFER_SIZE;
import static org.wildfly.extension.messaging.activemq.ServerDefinition.JOURNAL_BUFFER_TIMEOUT;
import static org.wildfly.extension.messaging.activemq.ServerDefinition.JOURNAL_COMPACT_MIN_FILES;
import static org.wildfly.extension.messaging.activemq.ServerDefinition.JOURNAL_COMPACT_PERCENTAGE;
import static org.wildfly.extension.messaging.activemq.ServerDefinition.JOURNAL_FILE_SIZE;
import static org.wildfly.extension.messaging.activemq.ServerDefinition.JOURNAL_MAX_IO;
import static org.wildfly.extension.messaging.activemq.ServerDefinition.JOURNAL_MIN_FILES;
import static org.wildfly.extension.messaging.activemq.ServerDefinition.JOURNAL_SYNC_NON_TRANSACTIONAL;
import static org.wildfly.extension.messaging.activemq.ServerDefinition.JOURNAL_SYNC_TRANSACTIONAL;
import static org.wildfly.extension.messaging.activemq.ServerDefinition.JOURNAL_TYPE;
import static org.wildfly.extension.messaging.activemq.ServerDefinition.LOG_JOURNAL_WRITE_RATE;
import static org.wildfly.extension.messaging.activemq.ServerDefinition.MANAGEMENT_ADDRESS;
import static org.wildfly.extension.messaging.activemq.ServerDefinition.MANAGEMENT_NOTIFICATION_ADDRESS;
import static org.wildfly.extension.messaging.activemq.ServerDefinition.MEMORY_MEASURE_INTERVAL;
import static org.wildfly.extension.messaging.activemq.ServerDefinition.MEMORY_WARNING_THRESHOLD;
import static org.wildfly.extension.messaging.activemq.ServerDefinition.MESSAGE_COUNTER_MAX_DAY_HISTORY;
import static org.wildfly.extension.messaging.activemq.ServerDefinition.MESSAGE_COUNTER_SAMPLE_PERIOD;
import static org.wildfly.extension.messaging.activemq.ServerDefinition.MESSAGE_EXPIRY_SCAN_PERIOD;
import static org.wildfly.extension.messaging.activemq.ServerDefinition.MESSAGE_EXPIRY_THREAD_PRIORITY;
import static org.wildfly.extension.messaging.activemq.ServerDefinition.OVERRIDE_IN_VM_SECURITY;
import static org.wildfly.extension.messaging.activemq.ServerDefinition.PERF_BLAST_PAGES;
import static org.wildfly.extension.messaging.activemq.ServerDefinition.PERSISTENCE_ENABLED;
import static org.wildfly.extension.messaging.activemq.ServerDefinition.RUN_SYNC_SPEED_TEST;
import static org.wildfly.extension.messaging.activemq.ServerDefinition.SCHEDULED_THREAD_POOL_MAX_SIZE;
import static org.wildfly.extension.messaging.activemq.ServerDefinition.SECURITY_DOMAIN;
import static org.wildfly.extension.messaging.activemq.ServerDefinition.SECURITY_ENABLED;
import static org.wildfly.extension.messaging.activemq.ServerDefinition.SECURITY_INVALIDATION_INTERVAL;
import static org.wildfly.extension.messaging.activemq.ServerDefinition.SERVER_DUMP_INTERVAL;
import static org.wildfly.extension.messaging.activemq.ServerDefinition.STATISTICS_ENABLED;
import static org.wildfly.extension.messaging.activemq.ServerDefinition.THREAD_POOL_MAX_SIZE;
import static org.wildfly.extension.messaging.activemq.ServerDefinition.TRANSACTION_TIMEOUT;
import static org.wildfly.extension.messaging.activemq.ServerDefinition.TRANSACTION_TIMEOUT_SCAN_PERIOD;
import static org.wildfly.extension.messaging.activemq.ServerDefinition.WILD_CARD_ROUTING_ENABLED;
import static org.wildfly.extension.messaging.activemq.PathDefinition.PATHS;
import static org.wildfly.extension.messaging.activemq.PathDefinition.RELATIVE_TO;
import static org.wildfly.extension.messaging.activemq.ha.HAPolicyConfigurationBuilder.addHAPolicyConfiguration;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.management.MBeanServer;

import org.apache.activemq.api.config.ActiveMQDefaultConfiguration;
import org.apache.activemq.api.core.BroadcastGroupConfiguration;
import org.apache.activemq.api.core.DiscoveryGroupConfiguration;
import org.apache.activemq.api.core.SimpleString;
import org.apache.activemq.core.config.Configuration;
import org.apache.activemq.core.config.impl.ConfigurationImpl;
import org.apache.activemq.core.security.Role;
import org.apache.activemq.core.server.ActiveMQServer;
import org.apache.activemq.core.server.JournalType;
import org.apache.activemq.core.settings.impl.AddressSettings;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.services.path.PathManager;
import org.jboss.as.controller.services.path.PathManagerService;
import org.jboss.as.network.OutboundSocketBinding;
import org.jboss.as.network.SocketBinding;
import org.jboss.as.security.plugins.SecurityDomainContext;
import org.jboss.as.security.service.SecurityDomainService;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceBuilder.DependencyType;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.jgroups.spi.ChannelFactory;
import org.wildfly.clustering.jgroups.spi.service.ProtocolStackServiceName;
import org.wildfly.extension.messaging.activemq.jms.JMSService;


/**
 * Add handler for a HornetQ server instance.
 *
 * @author Emanuel Muckenhuber
 * @author <a href="mailto:andy.taylor@jboss.com">Andy Taylor</a>
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 * @author <a href="http://jmesnil.net">Jeff Mesnil</a> (c) 2012 Red Hat Inc.
 */
class ServerAdd implements OperationStepHandler {

    static final String PATH_BASE = "paths";

    public static final ServerAdd INSTANCE = new ServerAdd();

    private ServerAdd() {
    }

    /** {@inheritDoc */
    public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {

        // We use a custom Resource impl so we can expose runtime HQ components (e.g. AddressControl) as child resources
        final ActiveMQServerResource resource = new ActiveMQServerResource();
        context.addResource(PathAddress.EMPTY_ADDRESS, resource);
        final ModelNode model = resource.getModel();

        for (final AttributeDefinition attributeDefinition : ServerDefinition.ATTRIBUTES) {
            attributeDefinition.validateAndSet(operation, model);
        }

        if (context.isNormalServer()) {
            // add an operation to create all the messaging paths resources that have not been already been created
            // prior to adding the HornetQ server
            context.addStep(new OperationStepHandler() {
                @Override
                public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                    final ModelNode model = Resource.Tools.readModel(resource);
                    for (String path : PathDefinition.PATHS.keySet()) {
                        if (!model.get(ModelDescriptionConstants.PATH).hasDefined(path)) {
                            PathAddress pathAddress = PathAddress.pathAddress(PathElement.pathElement(ModelDescriptionConstants.PATH, path));
                            context.createResource(pathAddress);
                        }
                    }
                    context.stepCompleted();
                }
            }, OperationContext.Stage.MODEL);
            context.addStep(new OperationStepHandler() {
                public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                    final List<ServiceController<?>> controllers = new ArrayList<ServiceController<?>>();
                    final ServiceVerificationHandler verificationHandler = new ServiceVerificationHandler();
                    performRuntime(context, resource, verificationHandler, controllers);

                    context.addStep(verificationHandler, OperationContext.Stage.VERIFY);

                    context.completeStep(new OperationContext.RollbackHandler() {
                        @Override
                        public void handleRollback(OperationContext context, ModelNode operation) {
                            for(ServiceController<?> controller : controllers) {
                                context.removeService(controller.getName());
                            }
                        }
                    });
                }
            }, OperationContext.Stage.RUNTIME);
        }
        context.stepCompleted();
    }

    private void performRuntime(final OperationContext context, final ActiveMQServerResource resource,
                                  final ServiceVerificationHandler verificationHandler,
                                  final List<ServiceController<?>> newControllers) throws OperationFailedException {
        // Add a RUNTIME step to actually install the HQ Service. This will execute after the runtime step
        // added by any child resources whose ADD handler executes after this one in the model stage.
        context.addStep(new OperationStepHandler() {
            @Override
            public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                final ServiceTarget serviceTarget = context.getServiceTarget();

                final String serverName = PathAddress.pathAddress(operation.require(OP_ADDR)).getLastElement().getValue();

                // Transform the configuration based on the recursive model
                final ModelNode model = Resource.Tools.readModel(resource);
                final Configuration configuration = transformConfig(context, serverName, model);

                // Create path services
                String bindingsPath = PATHS.get(BINDINGS_DIRECTORY).resolveModelAttribute(context, model.get(PATH, BINDINGS_DIRECTORY)).asString();
                String bindingsRelativeToPath = RELATIVE_TO.resolveModelAttribute(context, model.get(PATH, BINDINGS_DIRECTORY)).asString();
                String journalPath = PATHS.get(JOURNAL_DIRECTORY).resolveModelAttribute(context, model.get(PATH, JOURNAL_DIRECTORY)).asString();
                String journalRelativeToPath = RELATIVE_TO.resolveModelAttribute(context, model.get(PATH, JOURNAL_DIRECTORY)).asString();
                String largeMessagePath = PATHS.get(LARGE_MESSAGES_DIRECTORY).resolveModelAttribute(context, model.get(PATH, LARGE_MESSAGES_DIRECTORY)).asString();
                String largeMessageRelativeToPath = RELATIVE_TO.resolveModelAttribute(context, model.get(PATH, LARGE_MESSAGES_DIRECTORY)).asString();
                String pagingPath = PATHS.get(PAGING_DIRECTORY).resolveModelAttribute(context, model.get(PATH, PAGING_DIRECTORY)).asString();
                String pagingRelativeToPath = RELATIVE_TO.resolveModelAttribute(context, model.get(PATH, PAGING_DIRECTORY)).asString();

                // Create the HornetQ Service
                final ActiveMQServerService hqService = new ActiveMQServerService(
                        configuration, new ActiveMQServerService.PathConfig(bindingsPath, bindingsRelativeToPath, journalPath, journalRelativeToPath, largeMessagePath, largeMessageRelativeToPath, pagingPath, pagingRelativeToPath));

                // Add the HornetQ Service
                ServiceName hqServiceName = MessagingServices.getActiveMQServiceName(serverName);
                final ServiceBuilder<ActiveMQServer> serviceBuilder = serviceTarget.addService(hqServiceName, hqService)
                        .addDependency(DependencyType.OPTIONAL, ServiceName.JBOSS.append("mbean", "server"), MBeanServer.class, hqService.getMBeanServer());

                serviceBuilder.addDependency(PathManagerService.SERVICE_NAME, PathManager.class, hqService.getPathManagerInjector());

                // Add security
                String domain = SECURITY_DOMAIN.resolveModelAttribute(context, model).asString();
                serviceBuilder.addDependency(DependencyType.REQUIRED,
                        SecurityDomainService.SERVICE_NAME.append(domain),
                        SecurityDomainContext.class,
                        hqService.getSecurityDomainContextInjector());

                // Process acceptors and connectors
                final Set<String> socketBindings = new HashSet<String>();
                TransportConfigOperationHandlers.processAcceptors(context, configuration, model, socketBindings);

                // if there is any HTTP acceptor, add a dependency on the http-upgrade-registry service to
                // make sure that HornetQ server will be stopped *after* the registry (and its underlying XNIO thread)
                // is stopped.
                if (model.hasDefined(HTTP_ACCEPTOR)) {
                    for (final Property property : model.get(HTTP_ACCEPTOR).asPropertyList()) {
                        String httpListener = HTTPAcceptorDefinition.HTTP_LISTENER.resolveModelAttribute(context, property.getValue()).asString();
                        serviceBuilder.addDependency(HTTPUpgradeService.HTTP_UPGRADE_REGISTRY.append(httpListener));
                    }
                }

                for (final String socketBinding : socketBindings) {
                    final ServiceName socketName = SocketBinding.JBOSS_BINDING_NAME.append(socketBinding);
                    serviceBuilder.addDependency(socketName, SocketBinding.class, hqService.getSocketBindingInjector(socketBinding));
                }

                final Set<String> outboundSocketBindings = new HashSet<String>();
                TransportConfigOperationHandlers.processConnectors(context, configuration, model, outboundSocketBindings);
                for (final String outboundSocketBinding : outboundSocketBindings) {
                    final ServiceName outboundSocketName = OutboundSocketBinding.OUTBOUND_SOCKET_BINDING_BASE_SERVICE_NAME.append(outboundSocketBinding);
                    // Optional dependency so it won't fail if the user used a ref to socket-binding instead of
                    // outgoing-socket-binding
                    serviceBuilder.addDependency(DependencyType.OPTIONAL, outboundSocketName, OutboundSocketBinding.class,
                            hqService.getOutboundSocketBindingInjector(outboundSocketBinding));
                    if (!socketBindings.contains(outboundSocketBinding)) {
                        // Add a dependency on the regular socket binding as well so users don't have to use
                        // outgoing-socket-binding to configure a ref to the local server socket
                        final ServiceName socketName = SocketBinding.JBOSS_BINDING_NAME.append(outboundSocketBinding);
                        serviceBuilder.addDependency(DependencyType.OPTIONAL, socketName, SocketBinding.class,
                                hqService.getSocketBindingInjector(outboundSocketBinding));
                    }
                }
                //this requires connectors
                BroadcastGroupAdd.addBroadcastGroupConfigs(context, configuration, model);

                final List<BroadcastGroupConfiguration> broadcastGroupConfigurations = configuration.getBroadcastGroupConfigurations();
                final Map<String, DiscoveryGroupConfiguration> discoveryGroupConfigurations = configuration.getDiscoveryGroupConfigurations();

                if(broadcastGroupConfigurations != null) {
                    for(final BroadcastGroupConfiguration config : broadcastGroupConfigurations) {
                        final String name = config.getName();
                        final String key = "broadcast" + name;
                        ModelNode broadcastGroupModel = model.get(BROADCAST_GROUP, name);

                        if (broadcastGroupModel.hasDefined(JGROUPS_STACK.getName())) {
                            String jgroupsStack = JGROUPS_STACK.resolveModelAttribute(context, broadcastGroupModel).asString();
                            String channelName = JGROUPS_CHANNEL.resolveModelAttribute(context, broadcastGroupModel).asString();
                            serviceBuilder.addDependency(ProtocolStackServiceName.CHANNEL_FACTORY.getServiceName(jgroupsStack), ChannelFactory.class, hqService.getJGroupsInjector(key));
                            hqService.getJGroupsChannels().put(key, channelName);
                        } else {
                            final ServiceName groupBinding = GroupBindingService.getBroadcastBaseServiceName(hqServiceName).append(name);
                            serviceBuilder.addDependency(groupBinding, SocketBinding.class, hqService.getGroupBindingInjector(key));
                        }
                    }
                }
                if(discoveryGroupConfigurations != null) {
                    for(final DiscoveryGroupConfiguration config : discoveryGroupConfigurations.values()) {
                        final String name = config.getName();
                        final String key = "discovery" + name;
                        ModelNode discoveryGroupModel = model.get(DISCOVERY_GROUP, name);
                        if (discoveryGroupModel.hasDefined(JGROUPS_STACK.getName())) {
                            String jgroupsStack = JGROUPS_STACK.resolveModelAttribute(context, discoveryGroupModel).asString();
                            String channelName = JGROUPS_CHANNEL.resolveModelAttribute(context, discoveryGroupModel).asString();
                            serviceBuilder.addDependency(ProtocolStackServiceName.CHANNEL_FACTORY.getServiceName(jgroupsStack), ChannelFactory.class, hqService.getJGroupsInjector(key));
                            hqService.getJGroupsChannels().put(key, channelName);
                        } else {
                            final ServiceName groupBinding = GroupBindingService.getDiscoveryBaseServiceName(hqServiceName).append(name);
                            serviceBuilder.addDependency(groupBinding, SocketBinding.class, hqService.getGroupBindingInjector(key));
                        }
                    }
                }

                serviceBuilder.addListener(verificationHandler);

                // Install the HornetQ Service
                ServiceController<ActiveMQServer> hqServerServiceController = serviceBuilder.install();
                // Provide our custom Resource impl a ref to the HornetQServer so it can create child runtime resources
                resource.setHornetQServerServiceController(hqServerServiceController);

                newControllers.add(hqServerServiceController);
                boolean overrideInVMSecurity = OVERRIDE_IN_VM_SECURITY.resolveModelAttribute(context, operation).asBoolean();
                newControllers.add(JMSService.addService(serviceTarget, hqServiceName, overrideInVMSecurity, verificationHandler));

                context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
            }
        }, OperationContext.Stage.RUNTIME);
    }

    /**
     * Transform the detyped operation parameters into the hornetQ configuration.
     *
     * @param context the operation context
     * @param serverName the name of the HornetQServer instance
     * @param model the subsystem root resource model
     * @return the hornetQ configuration
     */
    private Configuration transformConfig(final OperationContext context, String serverName, final ModelNode model) throws OperationFailedException {

        Configuration configuration = new ConfigurationImpl();

        configuration.setName(serverName);

        configuration.setEnabledAsyncConnectionExecution(ASYNC_CONNECTION_EXECUTION_ENABLED.resolveModelAttribute(context, model).asBoolean());

        configuration.setClusterPassword(CLUSTER_PASSWORD.resolveModelAttribute(context, model).asString());
        configuration.setClusterUser(CLUSTER_USER.resolveModelAttribute(context, model).asString());
        configuration.setConnectionTTLOverride(CONNECTION_TTL_OVERRIDE.resolveModelAttribute(context, model).asInt());
        configuration.setCreateBindingsDir(CREATE_BINDINGS_DIR.resolveModelAttribute(context, model).asBoolean());
        configuration.setCreateJournalDir(CREATE_JOURNAL_DIR.resolveModelAttribute(context, model).asBoolean());

        configuration.setIDCacheSize(ID_CACHE_SIZE.resolveModelAttribute(context, model).asInt());
        // TODO do we want to allow the jmx configuration ?
        configuration.setJMXDomain(JMX_DOMAIN.resolveModelAttribute(context, model).asString());
        configuration.setJMXManagementEnabled(JMX_MANAGEMENT_ENABLED.resolveModelAttribute(context, model).asBoolean());
        // Journal
        final JournalType journalType = JournalType.valueOf(JOURNAL_TYPE.resolveModelAttribute(context, model).asString());
        configuration.setJournalType(journalType);

        // AIO Journal
        configuration.setJournalBufferSize_AIO(JOURNAL_BUFFER_SIZE.resolveModelAttribute(context, model).asInt(ActiveMQDefaultConfiguration.getDefaultJournalBufferSizeAio()));
        configuration.setJournalBufferTimeout_AIO(JOURNAL_BUFFER_TIMEOUT.resolveModelAttribute(context, model).asInt(ActiveMQDefaultConfiguration.getDefaultJournalBufferTimeoutAio()));
        configuration.setJournalMaxIO_AIO(JOURNAL_MAX_IO.resolveModelAttribute(context, model).asInt(ActiveMQDefaultConfiguration.getDefaultJournalMaxIoAio()));
        // NIO Journal
        configuration.setJournalBufferSize_NIO(JOURNAL_BUFFER_SIZE.resolveModelAttribute(context, model).asInt(ActiveMQDefaultConfiguration.getDefaultJournalBufferSizeNio()));
        configuration.setJournalBufferTimeout_NIO(JOURNAL_BUFFER_TIMEOUT.resolveModelAttribute(context, model).asInt(ActiveMQDefaultConfiguration.getDefaultJournalBufferTimeoutNio()));
        configuration.setJournalMaxIO_NIO(JOURNAL_MAX_IO.resolveModelAttribute(context, model).asInt(ActiveMQDefaultConfiguration.getDefaultJournalMaxIoNio()));
        //
        configuration.setJournalCompactMinFiles(JOURNAL_COMPACT_MIN_FILES.resolveModelAttribute(context, model).asInt());
        configuration.setJournalCompactPercentage(JOURNAL_COMPACT_PERCENTAGE.resolveModelAttribute(context, model).asInt());
        configuration.setJournalFileSize(JOURNAL_FILE_SIZE.resolveModelAttribute(context, model).asInt());
        configuration.setJournalMinFiles(JOURNAL_MIN_FILES.resolveModelAttribute(context, model).asInt());
        configuration.setJournalSyncNonTransactional(JOURNAL_SYNC_NON_TRANSACTIONAL.resolveModelAttribute(context, model).asBoolean());
        configuration.setJournalSyncTransactional(JOURNAL_SYNC_TRANSACTIONAL.resolveModelAttribute(context, model).asBoolean());
        configuration.setLogJournalWriteRate(LOG_JOURNAL_WRITE_RATE.resolveModelAttribute(context, model).asBoolean());

        configuration.setManagementAddress(SimpleString.toSimpleString(MANAGEMENT_ADDRESS.resolveModelAttribute(context, model).asString()));
        configuration.setManagementNotificationAddress(SimpleString.toSimpleString(MANAGEMENT_NOTIFICATION_ADDRESS.resolveModelAttribute(context, model).asString()));

        configuration.setMemoryMeasureInterval(MEMORY_MEASURE_INTERVAL.resolveModelAttribute(context, model).asLong());
        configuration.setMemoryWarningThreshold(MEMORY_WARNING_THRESHOLD.resolveModelAttribute(context, model).asInt());

        configuration.setMessageCounterEnabled(STATISTICS_ENABLED.resolveModelAttribute(context, model).asBoolean());
        configuration.setMessageCounterSamplePeriod(MESSAGE_COUNTER_SAMPLE_PERIOD.resolveModelAttribute(context, model).asInt());
        configuration.setMessageCounterMaxDayHistory(MESSAGE_COUNTER_MAX_DAY_HISTORY.resolveModelAttribute(context, model).asInt());
        configuration.setMessageExpiryScanPeriod(MESSAGE_EXPIRY_SCAN_PERIOD.resolveModelAttribute(context, model).asLong());
        configuration.setMessageExpiryThreadPriority(MESSAGE_EXPIRY_THREAD_PRIORITY.resolveModelAttribute(context, model).asInt());

        configuration.setJournalPerfBlastPages(PERF_BLAST_PAGES.resolveModelAttribute(context, model).asInt());
        configuration.setPersistDeliveryCountBeforeDelivery(PERSIST_DELIVERY_COUNT_BEFORE_DELIVERY.resolveModelAttribute(context, model).asBoolean());

        configuration.setPageMaxConcurrentIO(PAGE_MAX_CONCURRENT_IO.resolveModelAttribute(context, model).asInt());

        configuration.setPersistenceEnabled(PERSISTENCE_ENABLED.resolveModelAttribute(context, model).asBoolean());
        configuration.setPersistIDCache(PERSIST_ID_CACHE.resolveModelAttribute(context, model).asBoolean());

        configuration.setRunSyncSpeedTest(RUN_SYNC_SPEED_TEST.resolveModelAttribute(context, model).asBoolean());

        configuration.setScheduledThreadPoolMaxSize(SCHEDULED_THREAD_POOL_MAX_SIZE.resolveModelAttribute(context, model).asInt());
        configuration.setSecurityEnabled(SECURITY_ENABLED.resolveModelAttribute(context, model).asBoolean());
        configuration.setSecurityInvalidationInterval(SECURITY_INVALIDATION_INTERVAL.resolveModelAttribute(context, model).asLong());
        configuration.setServerDumpInterval(SERVER_DUMP_INTERVAL.resolveModelAttribute(context, model).asLong());
        configuration.setThreadPoolMaxSize(THREAD_POOL_MAX_SIZE.resolveModelAttribute(context, model).asInt());
        configuration.setTransactionTimeout(TRANSACTION_TIMEOUT.resolveModelAttribute(context, model).asLong());
        configuration.setTransactionTimeoutScanPeriod(TRANSACTION_TIMEOUT_SCAN_PERIOD.resolveModelAttribute(context, model).asLong());
        configuration.setWildcardRoutingEnabled(WILD_CARD_ROUTING_ENABLED.resolveModelAttribute(context, model).asBoolean());

        addHAPolicyConfiguration(context, configuration, model);

        processAddressSettings(context, configuration, model);
        processSecuritySettings(context, configuration, model);
        //process new interceptors
        processRemotingIncomingInterceptors(context, configuration, model);
        processRemotingOutgoingInterceptors(context, configuration, model);

        // Add in items from child resources
        GroupingHandlerAdd.addGroupingHandlerConfig(context,configuration, model);
        DiscoveryGroupAdd.addDiscoveryGroupConfigs(context, configuration, model);
        DivertAdd.addDivertConfigs(context, configuration, model);
        QueueAdd.addQueueConfigs(context, configuration, model);
        BridgeAdd.addBridgeConfigs(context, configuration, model);
        ClusterConnectionAdd.addClusterConnectionConfigs(context, configuration, model);
        ConnectorServiceDefinition.addConnectorServiceConfigs(context, configuration, model);

        return configuration;
    }

    /**
     * Process the address settings.
     *
     * @param configuration the hornetQ configuration
     * @param params        the detyped operation parameters
     */
    /**
     * Process the address settings.
     *
     * @param configuration the hornetQ configuration
     * @param params        the detyped operation parameters
     * @throws org.jboss.as.controller.OperationFailedException
     */
    static void processAddressSettings(final OperationContext context, final Configuration configuration, final ModelNode params) throws OperationFailedException {
        if (params.hasDefined(ADDRESS_SETTING)) {
            for (final Property property : params.get(ADDRESS_SETTING).asPropertyList()) {
                final String match = property.getName();
                final ModelNode config = property.getValue();
                final AddressSettings settings = AddressSettingAdd.createSettings(context, config);
                configuration.getAddressesSettings().put(match, settings);
            }
        }
    }

    /**
     * Process the HornetQ server-side incoming interceptors.
     */
    static void processRemotingIncomingInterceptors(final OperationContext context, final Configuration configuration, final ModelNode params) {
        // TODO preemptively check that the interceptor classes can be loaded
        ModelNode interceptors = params.get(CommonAttributes.REMOTING_INCOMING_INTERCEPTORS.getName());
        if (interceptors.isDefined()) {
            final List<String> interceptorClassNames = new ArrayList<String>();
            for (ModelNode child : interceptors.asList()) {
                interceptorClassNames.add(child.asString());
            }
            configuration.setIncomingInterceptorClassNames(interceptorClassNames);
        }
    }

    /**
     * Process the HornetQ server-side outgoing interceptors.
     */
    static void processRemotingOutgoingInterceptors(final OperationContext context, final Configuration configuration, final ModelNode params) {
        // TODO preemptively check that the interceptor classes can be loaded
        ModelNode interceptors = params.get(CommonAttributes.REMOTING_OUTGOING_INTERCEPTORS.getName());
        if (interceptors.isDefined()) {
            final List<String> interceptorClassNames = new ArrayList<String>();
            for (ModelNode child : interceptors.asList()) {
                interceptorClassNames.add(child.asString());
            }
            configuration.setOutgoingInterceptorClassNames(interceptorClassNames);
        }
    }

    /**
     * Process the security settings.
     *
     * @param configuration the hornetQ configuration
     * @param params        the detyped operation parameters
     */
    static void processSecuritySettings(final OperationContext context, final Configuration configuration, final ModelNode params) throws OperationFailedException {
        if (params.get(SECURITY_SETTING).isDefined()) {
            for (final Property property : params.get(SECURITY_SETTING).asPropertyList()) {
                final String match = property.getName();
                final ModelNode config = property.getValue();

                if(config.hasDefined(CommonAttributes.ROLE)) {
                    final Set<Role> roles = new HashSet<Role>();
                    for (final Property role : config.get(CommonAttributes.ROLE).asPropertyList()) {
                        roles.add(SecurityRoleDefinition.transform(context, role.getName(), role.getValue()));
                    }
                    configuration.getSecurityRoles().put(match, roles);
                }
            }
        }
    }
}
