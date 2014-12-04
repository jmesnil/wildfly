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

package org.wildfly.extension.messaging.activemq.jms;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jms.ConnectionFactory;

import org.apache.activemq.artemis.api.config.ActiveMQDefaultConfiguration;
import org.apache.activemq.artemis.core.server.ActiveMQServer;
import org.apache.activemq.artemis.jms.server.JMSServerManager;
import org.apache.activemq.artemis.jms.server.config.ConnectionFactoryConfiguration;
import org.hornetq.api.config.HornetQDefaultConfiguration;
import org.hornetq.api.jms.HornetQJMSClient;
import org.hornetq.jms.client.HornetQConnectionFactory;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.extension.messaging.activemq.ActiveMQActivationService;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2015 Red Hat inc.
 */
public class LegacyConnectionFactoryService implements Service<ConnectionFactory> {

    /**
     * Map ActiveMQ parameters key (using CameCalse convention) to HornetQ parameter keys (using lisp-case convention)
     */
    private static final Map<String, String> PARAM_KEY_MAPPING = new HashMap<>();

    private static final Map<String, String> LOAD_BALANCING_CLASS_NAME_MAPPING = new HashMap<>();

    static {
        PARAM_KEY_MAPPING.put(
                org.apache.activemq.artemis.core.remoting.impl.netty.TransportConstants.SSL_ENABLED_PROP_NAME,
                org.hornetq.core.remoting.impl.netty.TransportConstants.SSL_ENABLED_PROP_NAME);
        PARAM_KEY_MAPPING.put(
                org.apache.activemq.artemis.core.remoting.impl.netty.TransportConstants.HTTP_ENABLED_PROP_NAME,
                org.hornetq.core.remoting.impl.netty.TransportConstants.HTTP_ENABLED_PROP_NAME);
        PARAM_KEY_MAPPING.put(
                org.apache.activemq.artemis.core.remoting.impl.netty.TransportConstants.HTTP_CLIENT_IDLE_PROP_NAME,
                org.hornetq.core.remoting.impl.netty.TransportConstants.HTTP_CLIENT_IDLE_PROP_NAME);
        PARAM_KEY_MAPPING.put(
                org.apache.activemq.artemis.core.remoting.impl.netty.TransportConstants.HTTP_CLIENT_IDLE_SCAN_PERIOD,
                org.hornetq.core.remoting.impl.netty.TransportConstants.HTTP_CLIENT_IDLE_SCAN_PERIOD);
        PARAM_KEY_MAPPING.put(
                org.apache.activemq.artemis.core.remoting.impl.netty.TransportConstants.HTTP_REQUIRES_SESSION_ID,
                org.hornetq.core.remoting.impl.netty.TransportConstants.HTTP_REQUIRES_SESSION_ID);
        PARAM_KEY_MAPPING.put(
                org.apache.activemq.artemis.core.remoting.impl.netty.TransportConstants.HTTP_UPGRADE_ENABLED_PROP_NAME,
                org.hornetq.core.remoting.impl.netty.TransportConstants.HTTP_UPGRADE_ENABLED_PROP_NAME);
        PARAM_KEY_MAPPING.put(
                org.apache.activemq.artemis.core.remoting.impl.netty.TransportConstants.HTTP_UPGRADE_ENDPOINT_PROP_NAME,
                org.hornetq.core.remoting.impl.netty.TransportConstants.HTTP_UPGRADE_ENDPOINT_PROP_NAME);
        PARAM_KEY_MAPPING.put(
                org.apache.activemq.artemis.core.remoting.impl.netty.TransportConstants.USE_SERVLET_PROP_NAME,
                org.hornetq.core.remoting.impl.netty.TransportConstants.USE_SERVLET_PROP_NAME);
        PARAM_KEY_MAPPING.put(
                org.apache.activemq.artemis.core.remoting.impl.netty.TransportConstants.SERVLET_PATH,
                org.hornetq.core.remoting.impl.netty.TransportConstants.SERVLET_PATH);
        PARAM_KEY_MAPPING.put(
                org.apache.activemq.artemis.core.remoting.impl.netty.TransportConstants.USE_NIO_PROP_NAME,
                org.hornetq.core.remoting.impl.netty.TransportConstants.USE_NIO_PROP_NAME);
        PARAM_KEY_MAPPING.put(
                org.apache.activemq.artemis.core.remoting.impl.netty.TransportConstants.USE_NIO_GLOBAL_WORKER_POOL_PROP_NAME,
                org.hornetq.core.remoting.impl.netty.TransportConstants.USE_NIO_GLOBAL_WORKER_POOL_PROP_NAME);
        PARAM_KEY_MAPPING.put(
                org.apache.activemq.artemis.core.remoting.impl.netty.TransportConstants.LOCAL_ADDRESS_PROP_NAME,
                org.hornetq.core.remoting.impl.netty.TransportConstants.LOCAL_ADDRESS_PROP_NAME);
        PARAM_KEY_MAPPING.put(
                org.apache.activemq.artemis.core.remoting.impl.netty.TransportConstants.KEYSTORE_PROVIDER_PROP_NAME,
                org.hornetq.core.remoting.impl.netty.TransportConstants.KEYSTORE_PROVIDER_PROP_NAME);
        PARAM_KEY_MAPPING.put(
                org.apache.activemq.artemis.core.remoting.impl.netty.TransportConstants.KEYSTORE_PATH_PROP_NAME,
                org.hornetq.core.remoting.impl.netty.TransportConstants.KEYSTORE_PATH_PROP_NAME);
        PARAM_KEY_MAPPING.put(
                org.apache.activemq.artemis.core.remoting.impl.netty.TransportConstants.KEYSTORE_PASSWORD_PROP_NAME,
                org.hornetq.core.remoting.impl.netty.TransportConstants.KEYSTORE_PASSWORD_PROP_NAME);
        PARAM_KEY_MAPPING.put(
                org.apache.activemq.artemis.core.remoting.impl.netty.TransportConstants.TRUSTSTORE_PROVIDER_PROP_NAME,
                org.hornetq.core.remoting.impl.netty.TransportConstants.TRUSTSTORE_PROVIDER_PROP_NAME);
        PARAM_KEY_MAPPING.put(
                org.apache.activemq.artemis.core.remoting.impl.netty.TransportConstants.TRUSTSTORE_PATH_PROP_NAME,
                org.hornetq.core.remoting.impl.netty.TransportConstants.TRUSTSTORE_PATH_PROP_NAME);
        PARAM_KEY_MAPPING.put(
                org.apache.activemq.artemis.core.remoting.impl.netty.TransportConstants.TRUSTSTORE_PASSWORD_PROP_NAME,
                org.hornetq.core.remoting.impl.netty.TransportConstants.TRUSTSTORE_PASSWORD_PROP_NAME);
        PARAM_KEY_MAPPING.put(
                org.apache.activemq.artemis.core.remoting.impl.netty.TransportConstants.ENABLED_CIPHER_SUITES_PROP_NAME,
                org.hornetq.core.remoting.impl.netty.TransportConstants.ENABLED_CIPHER_SUITES_PROP_NAME);
        PARAM_KEY_MAPPING.put(
                org.apache.activemq.artemis.core.remoting.impl.netty.TransportConstants.ENABLED_PROTOCOLS_PROP_NAME,
                org.hornetq.core.remoting.impl.netty.TransportConstants.ENABLED_PROTOCOLS_PROP_NAME);
        PARAM_KEY_MAPPING.put(
                org.apache.activemq.artemis.core.remoting.impl.netty.TransportConstants.TCP_NODELAY_PROPNAME,
                org.hornetq.core.remoting.impl.netty.TransportConstants.TCP_NODELAY_PROPNAME);
        PARAM_KEY_MAPPING.put(
                org.apache.activemq.artemis.core.remoting.impl.netty.TransportConstants.TCP_SENDBUFFER_SIZE_PROPNAME,
                org.hornetq.core.remoting.impl.netty.TransportConstants.TCP_SENDBUFFER_SIZE_PROPNAME);
        PARAM_KEY_MAPPING.put(
                org.apache.activemq.artemis.core.remoting.impl.netty.TransportConstants.TCP_RECEIVEBUFFER_SIZE_PROPNAME,
                org.hornetq.core.remoting.impl.netty.TransportConstants.TCP_RECEIVEBUFFER_SIZE_PROPNAME);
        PARAM_KEY_MAPPING.put(
                org.apache.activemq.artemis.core.remoting.impl.netty.TransportConstants.NIO_REMOTING_THREADS_PROPNAME,
                org.hornetq.core.remoting.impl.netty.TransportConstants.NIO_REMOTING_THREADS_PROPNAME);
        PARAM_KEY_MAPPING.put(
                org.apache.activemq.artemis.core.remoting.impl.netty.TransportConstants.BATCH_DELAY,
                org.hornetq.core.remoting.impl.netty.TransportConstants.BATCH_DELAY);
        PARAM_KEY_MAPPING.put(
                org.apache.activemq.artemis.core.remoting.impl.netty.TransportConstants.NIO_REMOTING_THREADS_PROPNAME,
                org.hornetq.core.remoting.impl.netty.TransportConstants.NIO_REMOTING_THREADS_PROPNAME);
        PARAM_KEY_MAPPING.put(
                ActiveMQDefaultConfiguration.getPropMaskPassword(),
                HornetQDefaultConfiguration.getPropMaskPassword());
        PARAM_KEY_MAPPING.put(
                ActiveMQDefaultConfiguration.getPropPasswordCodec(),
                HornetQDefaultConfiguration.getPropPasswordCodec());
        PARAM_KEY_MAPPING.put(
                org.apache.activemq.artemis.core.remoting.impl.netty.TransportConstants.NETTY_CONNECT_TIMEOUT,
                org.hornetq.core.remoting.impl.netty.TransportConstants.NETTY_CONNECT_TIMEOUT);

        LOAD_BALANCING_CLASS_NAME_MAPPING.put(
                org.apache.activemq.artemis.api.core.client.loadbalance.FirstElementConnectionLoadBalancingPolicy.class.getCanonicalName(),
                org.hornetq.api.core.client.loadbalance.FirstElementConnectionLoadBalancingPolicy.class.getCanonicalName());
        LOAD_BALANCING_CLASS_NAME_MAPPING.put(
                org.apache.activemq.artemis.api.core.client.loadbalance.RandomConnectionLoadBalancingPolicy.class.getCanonicalName(),
                org.hornetq.api.core.client.loadbalance.RandomConnectionLoadBalancingPolicy.class.getCanonicalName());
        LOAD_BALANCING_CLASS_NAME_MAPPING.put(
                org.apache.activemq.artemis.api.core.client.loadbalance.RandomStickyConnectionLoadBalancingPolicy.class.getCanonicalName(),
                org.hornetq.api.core.client.loadbalance.RandomStickyConnectionLoadBalancingPolicy.class.getCanonicalName());
        LOAD_BALANCING_CLASS_NAME_MAPPING.put(
                org.apache.activemq.artemis.api.core.client.loadbalance.RoundRobinConnectionLoadBalancingPolicy.class.getCanonicalName(),
                org.hornetq.api.core.client.loadbalance.RoundRobinConnectionLoadBalancingPolicy.class.getCanonicalName());

    }

    private final InjectedValue<JMSServerManager> jmsServer = new InjectedValue<>();

    private final ConnectionFactoryConfiguration newConfiguration;

    private HornetQConnectionFactory connectionFactory;

    private LegacyConnectionFactoryService(ConnectionFactoryConfiguration newConfiguration) {
        this.newConfiguration = newConfiguration;
    }

    @Override
    public void start(StartContext context) throws StartException {
        ActiveMQServer activeMQServer = jmsServer.getValue().getActiveMQServer();
        Map<String, org.apache.activemq.artemis.api.core.TransportConfiguration> newConnectorConfigurations = activeMQServer.getConfiguration().getConnectorConfigurations();


        String newDiscoveryGroupName = newConfiguration.getDiscoveryGroupName();
        org.hornetq.api.core.DiscoveryGroupConfiguration legacyDiscoveryGroupConfiguration = null;
        if (newDiscoveryGroupName != null) {
            org.apache.activemq.artemis.api.core.DiscoveryGroupConfiguration newDiscoveryGroupConfiguration = activeMQServer.getConfiguration().getDiscoveryGroupConfigurations().get(newDiscoveryGroupName);
            legacyDiscoveryGroupConfiguration = translateDiscoveryGroupConfiguration(newDiscoveryGroupConfiguration);
        }
        List<String> connectorNames = newConfiguration.getConnectorNames();
        org.hornetq.api.core.TransportConfiguration[] legacyConnectorConfigurations = translateConnectorConfigurations(connectorNames, newConnectorConfigurations);

        org.hornetq.api.jms.JMSFactoryType legacyFactoryType = translateFactoryType(newConfiguration.getFactoryType());

        if (newConfiguration.isHA()) {
            if (legacyDiscoveryGroupConfiguration != null) {
                connectionFactory = HornetQJMSClient.createConnectionFactoryWithHA(legacyDiscoveryGroupConfiguration, legacyFactoryType);
            } else {
                connectionFactory = HornetQJMSClient.createConnectionFactoryWithHA(legacyFactoryType, legacyConnectorConfigurations);
            }
        } else {
            if (legacyDiscoveryGroupConfiguration != null) {
                connectionFactory = HornetQJMSClient.createConnectionFactoryWithoutHA(legacyDiscoveryGroupConfiguration, legacyFactoryType);
            } else {
                connectionFactory = HornetQJMSClient.createConnectionFactoryWithoutHA(legacyFactoryType, legacyConnectorConfigurations);
            }
        }

        connectionFactory.setAutoGroup(newConfiguration.isAutoGroup());
        connectionFactory.setBlockOnAcknowledge(newConfiguration.isBlockOnAcknowledge());
        connectionFactory.setBlockOnDurableSend(newConfiguration.isBlockOnDurableSend());
        connectionFactory.setBlockOnNonDurableSend(newConfiguration.isBlockOnNonDurableSend());
        connectionFactory.setCacheLargeMessagesClient(newConfiguration.isCacheLargeMessagesClient());
        connectionFactory.setCallFailoverTimeout(newConfiguration.getCallFailoverTimeout());
        connectionFactory.setCallTimeout(newConfiguration.getCallTimeout());
        connectionFactory.setClientFailureCheckPeriod(newConfiguration.getClientFailureCheckPeriod());
        connectionFactory.setClientID(newConfiguration.getClientID());
        connectionFactory.setCompressLargeMessage(newConfiguration.isCompressLargeMessages());
        connectionFactory.setConfirmationWindowSize(newConfiguration.getConfirmationWindowSize());
        connectionFactory.setConnectionLoadBalancingPolicyClassName(LOAD_BALANCING_CLASS_NAME_MAPPING.get(newConfiguration.getLoadBalancingPolicyClassName()));
        connectionFactory.setConnectionTTL(newConfiguration.getConnectionTTL());
        connectionFactory.setConsumerMaxRate(newConfiguration.getConsumerMaxRate());
        connectionFactory.setConsumerWindowSize(newConfiguration.getConsumerWindowSize());
        connectionFactory.setDupsOKBatchSize(newConfiguration.getDupsOKBatchSize());
        connectionFactory.setFailoverOnInitialConnection(newConfiguration.isFailoverOnInitialConnection());
        connectionFactory.setGroupID(newConfiguration.getGroupID());
        // no equivalent:
        // connectionFactory.setInitialConnectAttempts( ? );
        // connectionFactory.setInitialMessagePacketSize( ? );
        connectionFactory.setMaxRetryInterval(newConfiguration.getMaxRetryInterval());
        connectionFactory.setMinLargeMessageSize(newConfiguration.getMinLargeMessageSize());
        connectionFactory.setPreAcknowledge(newConfiguration.isPreAcknowledge());
        connectionFactory.setProducerMaxRate(newConfiguration.getProducerMaxRate());
        connectionFactory.setProducerWindowSize(newConfiguration.getProducerWindowSize());
        connectionFactory.setReconnectAttempts(newConfiguration.getReconnectAttempts());
        connectionFactory.setRetryInterval(newConfiguration.getRetryInterval());
        connectionFactory.setRetryIntervalMultiplier(newConfiguration.getRetryIntervalMultiplier());
        connectionFactory.setScheduledThreadPoolMaxSize(newConfiguration.getScheduledThreadPoolMaxSize());
        connectionFactory.setThreadPoolMaxSize(newConfiguration.getThreadPoolMaxSize());
        connectionFactory.setTransactionBatchSize(newConfiguration.getTransactionBatchSize());
        connectionFactory.setUseGlobalPools(newConfiguration.isUseGlobalPools());


    }

    private org.hornetq.api.core.DiscoveryGroupConfiguration translateDiscoveryGroupConfiguration(org.apache.activemq.artemis.api.core.DiscoveryGroupConfiguration newDiscoveryGroupConfiguration) throws StartException {
        org.apache.activemq.artemis.api.core.BroadcastEndpointFactory newBroadcastEndpointFactory = newDiscoveryGroupConfiguration.getBroadcastEndpointFactory();
        org.hornetq.api.core.BroadcastEndpointFactoryConfiguration legacyBroadcastEndpointFactory;

        if (newBroadcastEndpointFactory instanceof org.apache.activemq.artemis.api.core.UDPBroadcastEndpointFactory) {
            org.apache.activemq.artemis.api.core.UDPBroadcastEndpointFactory factory = (org.apache.activemq.artemis.api.core.UDPBroadcastEndpointFactory) newBroadcastEndpointFactory;
            legacyBroadcastEndpointFactory = new org.hornetq.api.core.UDPBroadcastGroupConfiguration(
                    factory.getGroupAddress(),
                    factory.getGroupPort(),
                    factory.getLocalBindAddress(),
                    factory.getLocalBindPort());
        } else if (newBroadcastEndpointFactory instanceof org.apache.activemq.artemis.api.core.ChannelBroadcastEndpointFactory) {
            org.apache.activemq.artemis.api.core.ChannelBroadcastEndpointFactory factory = (org.apache.activemq.artemis.api.core.ChannelBroadcastEndpointFactory) newBroadcastEndpointFactory;
            legacyBroadcastEndpointFactory = null;
            // FIXME
            /*
            legacyBroadcastEndpointFactory = new org.hornetq.api.core.JGroupsBroadcastGroupConfiguration(
                    factory.getChannel(),
                    factory.getChannelName());
                    */
        } else {
            // FIXME I18N
            throw new StartException("unsupported broadcast group configuration: " + newBroadcastEndpointFactory);
        }

        return new org.hornetq.api.core.DiscoveryGroupConfiguration(newDiscoveryGroupConfiguration.getName(),
                newDiscoveryGroupConfiguration.getRefreshTimeout(),
                newDiscoveryGroupConfiguration.getDiscoveryInitialWaitTimeout(),
                legacyBroadcastEndpointFactory);
    }


    private org.hornetq.api.core.TransportConfiguration[] translateConnectorConfigurations(List<String> connectorNames,
                                                                                           Map<String, org.apache.activemq.artemis.api.core.TransportConfiguration> newConnectorConfigurations) throws StartException {
        List<org.hornetq.api.core.TransportConfiguration> legacyConnectorConfigurations = new ArrayList<>();

        for (String connectorName : connectorNames) {
            org.apache.activemq.artemis.api.core.TransportConfiguration newTransportConfiguration = newConnectorConfigurations.get(connectorName);
            String legacyFactoryClassName = translateFactoryClassName(newTransportConfiguration.getFactoryClassName());
            Map legacyParams = translateParams(newTransportConfiguration.getParams());
            org.hornetq.api.core.TransportConfiguration legacyTransportConfiguration = new org.hornetq.api.core.TransportConfiguration(
                    legacyFactoryClassName,
                    legacyParams,
                    newTransportConfiguration.getName());

            legacyConnectorConfigurations.add(legacyTransportConfiguration);
        }

        return legacyConnectorConfigurations.toArray(new org.hornetq.api.core.TransportConfiguration[legacyConnectorConfigurations.size()]);
    }

    private String translateFactoryClassName(String newFactoryClassName) throws StartException {
        if (newFactoryClassName.equals(org.apache.activemq.artemis.core.remoting.impl.netty.NettyConnectorFactory.class.getName())) {
            return org.hornetq.core.remoting.impl.netty.NettyConnectorFactory.class.getName();
        } else {
            throw new StartException("can not translate new connector factory class " + newFactoryClassName + " to a legacy class");
        }
    }

    private Map translateParams(Map<String, Object> newParams) {
        Map<String, Object> legacyParams = new HashMap<>();

        for (Map.Entry<String, Object> newEntry : newParams.entrySet()) {
            String newKey = newEntry.getKey();
            Object value = newEntry.getValue();
            String legacyKey = PARAM_KEY_MAPPING.getOrDefault(newKey, newKey);
            legacyParams.put(legacyKey, value);
        }
        return legacyParams;
    }

    private org.hornetq.api.jms.JMSFactoryType translateFactoryType(org.apache.activemq.artemis.api.jms.JMSFactoryType newFactoryType) {
        switch (newFactoryType) {
            case XA_CF:
                return org.hornetq.api.jms.JMSFactoryType.CF;
            case QUEUE_XA_CF:
                return org.hornetq.api.jms.JMSFactoryType.QUEUE_XA_CF;
            case TOPIC_XA_CF:
                return org.hornetq.api.jms.JMSFactoryType.TOPIC_XA_CF;
            case QUEUE_CF:
                return org.hornetq.api.jms.JMSFactoryType.QUEUE_CF;
            case TOPIC_CF:
                return org.hornetq.api.jms.JMSFactoryType.TOPIC_CF;
            case CF:
            default:
                return org.hornetq.api.jms.JMSFactoryType.CF;
        }
    }

    @Override
    public void stop(StopContext context) {

    }

    @Override
    public ConnectionFactory getValue() throws IllegalStateException, IllegalArgumentException {
        return connectionFactory;
    }

    public static Service<ConnectionFactory> installService(final String name, final ServiceTarget serviceTarget, final ServiceName activeMQServerServiceName, final ConnectionFactoryConfiguration newConnectionFactoryConfiguration) {
        final LegacyConnectionFactoryService service = new LegacyConnectionFactoryService(newConnectionFactoryConfiguration);
        final ServiceName serviceName = JMSServices.getConnectionFactoryBaseServiceName(activeMQServerServiceName).append(name, LEGACY);
        final ServiceBuilder<ConnectionFactory> serviceBuilder = serviceTarget.addService(serviceName, service)
                .addDependency(ActiveMQActivationService.getServiceName(activeMQServerServiceName))
                .addDependency(JMSServices.getJmsManagerBaseServiceName(activeMQServerServiceName), JMSServerManager.class, service.jmsServer)
                .setInitialMode(ServiceController.Mode.PASSIVE);
        serviceBuilder.install();

        return service;
    }
}
