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

import static org.wildfly.extension.messaging.activemq.CommonAttributes.LEGACY;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jms.ConnectionFactory;

import org.apache.activemq.jms.server.JMSServerManager;
import org.apache.activemq.jms.server.config.ConnectionFactoryConfiguration;
import org.hornetq.api.jms.HornetQJMSClient;
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

    private static final Map<String, String> PARAM_KEY_MAPPING = new HashMap<>();

    static {
        PARAM_KEY_MAPPING.put(
                org.apache.activemq.core.remoting.impl.netty.TransportConstants.BACKLOG_PROP_NAME,
                org.hornetq.core.remoting.impl.netty.TransportConstants.BACKLOG_PROP_NAME);
        PARAM_KEY_MAPPING.put(
                org.apache.activemq.core.remoting.impl.netty.TransportConstants.HTTP_UPGRADE_ENABLED_PROP_NAME,
                org.hornetq.core.remoting.impl.netty.TransportConstants.HTTP_UPGRADE_ENABLED_PROP_NAME);

    }
    private final InjectedValue<JMSServerManager> jmsServer = new InjectedValue<>();

    private final ConnectionFactoryConfiguration newConfiguration;

    private ConnectionFactory connectionFactory;

    private LegacyConnectionFactoryService(ConnectionFactoryConfiguration newConfiguration) {
        this.newConfiguration = newConfiguration;
    }

    @Override
    public void start(StartContext context) throws StartException {
        Map<String, org.apache.activemq.api.core.TransportConfiguration> newConnectorConfigurations = jmsServer.getValue().getActiveMQServer().getConfiguration().getConnectorConfigurations();

        List<String> connectorNames = newConfiguration.getConnectorNames();
        boolean ha = newConfiguration.isHA();

        org.hornetq.api.jms.JMSFactoryType legacyFactoryType = translateFactoryType(newConfiguration.getFactoryType());
        org.hornetq.api.core.TransportConfiguration[] legacyConnectorConfigurations = translateConnectorConfigurations(connectorNames, newConnectorConfigurations);
        connectionFactory = HornetQJMSClient.createConnectionFactoryWithoutHA(legacyFactoryType, legacyConnectorConfigurations);
    }

    private org.hornetq.api.core.TransportConfiguration[] translateConnectorConfigurations(List<String> connectorNames,
                                                                                           Map<String, org.apache.activemq.api.core.TransportConfiguration> newConnectorConfigurations) throws StartException {
        List<org.hornetq.api.core.TransportConfiguration> legacyConnectorConfigurations = new ArrayList<>();

        for (String connectorName : connectorNames) {
            org.apache.activemq.api.core.TransportConfiguration newTransportConfiguration = newConnectorConfigurations.get(connectorName);
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
        if (newFactoryClassName.equals(org.apache.activemq.core.remoting.impl.netty.NettyConnectorFactory.class.getName())) {
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

    private org.hornetq.api.jms.JMSFactoryType translateFactoryType(org.apache.activemq.api.jms.JMSFactoryType newFactoryType) {
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

    public static Service<ConnectionFactory> installService(final String name, final ServiceTarget serviceTarget, final ServiceName hqServiceName, final ConnectionFactoryConfiguration newConnectionFactoryConfiguration) {
        final LegacyConnectionFactoryService service = new LegacyConnectionFactoryService(newConnectionFactoryConfiguration);
        final ServiceName serviceName = JMSServices.getConnectionFactoryBaseServiceName(hqServiceName).append(name, LEGACY);
        final ServiceBuilder<ConnectionFactory> serviceBuilder = serviceTarget.addService(serviceName, service)
                .addDependency(ActiveMQActivationService.getServiceName(hqServiceName))
                .addDependency(JMSServices.getJmsManagerBaseServiceName(hqServiceName), JMSServerManager.class, service.jmsServer)
                .setInitialMode(ServiceController.Mode.PASSIVE);
        serviceBuilder.install();

        return service;
    }
}
