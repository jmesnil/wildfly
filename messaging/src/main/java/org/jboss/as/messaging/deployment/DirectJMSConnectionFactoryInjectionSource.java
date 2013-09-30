/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.jboss.as.messaging.deployment;

import static org.jboss.as.messaging.CommonAttributes.CONNECTOR;
import static org.jboss.as.messaging.CommonAttributes.DEFAULT;
import static org.jboss.as.messaging.CommonAttributes.HORNETQ_SERVER;
import static org.jboss.as.messaging.CommonAttributes.JGROUPS_CHANNEL;
import static org.jboss.as.messaging.CommonAttributes.NO_TX;
import static org.jboss.as.messaging.CommonAttributes.POOLED_CONNECTION_FACTORY;
import static org.jboss.as.messaging.CommonAttributes.XA_TX;
import static org.jboss.as.messaging.jms.ConnectionFactoryAttributes.Common.DISCOVERY_GROUP_NAME;
import static org.jboss.as.messaging.jms.ConnectionFactoryAttributes.Pooled.MAX_POOL_SIZE;
import static org.jboss.as.messaging.jms.ConnectionFactoryAttributes.Pooled.MIN_POOL_SIZE;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.as.connector.services.resourceadapters.ConnectionFactoryReferenceFactoryService;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.ee.component.InjectionSource;
import org.jboss.as.messaging.CommonAttributes;
import org.jboss.as.messaging.MessagingExtension;
import org.jboss.as.messaging.MessagingLogger;
import org.jboss.as.messaging.jms.ConnectionFactoryAttribute;
import org.jboss.as.messaging.jms.ConnectionFactoryAttributes;
import org.jboss.as.messaging.jms.PooledConnectionFactoryConfigProperties;
import org.jboss.as.messaging.jms.PooledConnectionFactoryConfigurationRuntimeHandler;
import org.jboss.as.messaging.jms.PooledConnectionFactoryDefinition;
import org.jboss.as.messaging.jms.PooledConnectionFactoryService;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2013 Red Hat inc.
 */
public class DirectJMSConnectionFactoryInjectionSource extends InjectionSource {

    /*
    String description() default "";
    String name();
    String interfaceName() default "javax.jms.ConnectionFactory";
    String className() default "";
    String resourceAdapter() default "";
    String user() default "";
    String password() default "";
    String clientId() default "";
    String[] properties() default {};
    boolean transactional() default true;
    int maxPoolSize() default -1;
    int minPoolSize() default -1;
    */

    private final String name;
    // not used: HornetQ CF implements all JMS CF interfaces
    private String interfaceName;
    // not used
    private String className;
    private String resourceAdapter;
    private String user;
    private String password;
    private String clientId;
    private Map<String, String> properties = new HashMap<String, String>();
    private boolean transactional;
    private int maxPoolSize;
    private int minPoolSize;

    public DirectJMSConnectionFactoryInjectionSource(String name) {
        this.name = name;
    }

    void setInterfaceName(String interfaceName) {
        this.interfaceName = interfaceName;
    }

    void setClassName(String className) {
        this.className = className;
    }

    void setResourceAdapter(String resourceAdapter) {
        this.resourceAdapter = resourceAdapter;
    }

    void setUser(String user) {
        this.user = user;
    }

    void setPassword(String password) {
        this.password = password;
    }

    void setClientId(String clientId) {
        this.clientId = clientId;
    }

    void addProperty(String key, String value) {
        properties.put(key, value);
    }

    void setTransactional(boolean transactional) {
        this.transactional = transactional;
    }

    void setMaxPoolSize(int maxPoolSize) {
        this.maxPoolSize = maxPoolSize;
    }

    void setMinPoolSize(int minPoolSize) {
        this.minPoolSize = minPoolSize;
    }


    @Override
    public void getResourceValue(ResolutionContext context, ServiceBuilder<?> serviceBuilder, DeploymentPhaseContext phaseContext, Injector<ManagedReferenceFactory> injector) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();

        try {
            startedPooledConnectionFactory(context, name, serviceBuilder, phaseContext.getServiceTarget(), deploymentUnit, injector);
        } catch (OperationFailedException e) {
            throw new DeploymentUnitProcessingException(e);
        }
    }

    private void startedPooledConnectionFactory(ResolutionContext context, String name, ServiceBuilder<?> serviceBuilder, ServiceTarget serviceTarget, DeploymentUnit deploymentUnit, Injector<ManagedReferenceFactory> injector) throws DeploymentUnitProcessingException, OperationFailedException {
        Map<String, String> props = new HashMap<>(properties);
        List<String> connectors = getConnectors(props);
        clearUnknownProperties(properties);

        ModelNode model = new ModelNode();
        for (String connector : connectors) {
            model.get(CONNECTOR).add(connector);
        }
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            model.get(entry.getKey()).set(entry.getValue());
        }
        model.get(MIN_POOL_SIZE.getName()).set(minPoolSize);
        model.get(MAX_POOL_SIZE.getName()).set(maxPoolSize);
        if (user != null && !user.isEmpty()) {
            model.get(ConnectionFactoryAttributes.Pooled.USER.getName()).set(user);
        }
        if (password != null && !password.isEmpty()) {
            model.get(ConnectionFactoryAttributes.Pooled.PASSWORD.getName()).set(password);
        }
        if (clientId != null && !clientId.isEmpty()) {
            model.get(CommonAttributes.CLIENT_ID.getName()).set(clientId);
        }

        String discoveryGroupName = model.hasDefined(DISCOVERY_GROUP_NAME.getName()) ? model.get(DISCOVERY_GROUP_NAME.getName()).asString() : null;
        String jgroupsChannelName = model.hasDefined(JGROUPS_CHANNEL.getName()) ? model.get(JGROUPS_CHANNEL.getName()).asString() : null;

        List<PooledConnectionFactoryConfigProperties> adapterParams = getAdapterParams(model);
        String txSupport = transactional ? XA_TX : NO_TX;

        final String pcfName = uniqueName(context, name);
        final ContextNames.BindInfo bindInfo = ContextNames.bindInfoForEnvEntry(context.getApplicationName(), context.getModuleName(), context.getComponentName(), !context.isCompUsesModule(), name);
        PooledConnectionFactoryService.installService(null, null, serviceTarget, pcfName, getHornetQServerName(), connectors,
                discoveryGroupName, jgroupsChannelName, adapterParams,
                bindInfo,
                txSupport, minPoolSize, maxPoolSize);

        final ServiceName referenceFactoryServiceName = ConnectionFactoryReferenceFactoryService.SERVICE_NAME_BASE
                .append(bindInfo.getBinderServiceName());
        serviceBuilder.addDependency(referenceFactoryServiceName, ManagedReferenceFactory.class, injector);

        //create the management registration
        String managementName = managementName(context, name);
        final PathElement serverElement = PathElement.pathElement(HORNETQ_SERVER, getHornetQServerName());
        deploymentUnit.createDeploymentSubModel(MessagingExtension.SUBSYSTEM_NAME, serverElement);
        final PathElement pcfPath = PathElement.pathElement(POOLED_CONNECTION_FACTORY, managementName);
        PathAddress registration = PathAddress.pathAddress(serverElement, pcfPath);
        MessagingXmlInstallDeploymentUnitProcessor.createDeploymentSubModel(registration, deploymentUnit);
        PooledConnectionFactoryConfigurationRuntimeHandler.INSTANCE.registerResource(getHornetQServerName(), managementName, model);
    }

    private List<String> getConnectors(Map<String, String> props) {
        List<String> connectors = new ArrayList<>();
        if (!props.containsKey(CONNECTOR)) {
            connectors.add("http");
        } else {
            String connectorsStr = properties.remove(CONNECTOR);
            for (String s : connectorsStr.split(",")) {
                String connector = s.trim();
                if (!connector.isEmpty()) {
                    connectors.add(connector);
                }
            }
        }
        return  connectors;
    }

    void clearUnknownProperties(final Map<String, String> props) {
        Set<String> attributeNames = PooledConnectionFactoryDefinition.getAttributes().keySet();

        final Iterator<Map.Entry<String, String>> it = props.entrySet().iterator();
        while (it.hasNext()) {
            final Map.Entry<String, String> entry = it.next();
            String value = entry.getKey();
            if (value == null || "".equals(value)) {
                it.remove();
            } else if (!attributeNames.contains(entry.getKey())) {
                MessagingLogger.MESSAGING_LOGGER.unknownPooledConnectionFactoryAttribute(entry.getKey());
                it.remove();
            }
        }
    }

    private static String uniqueName(InjectionSource.ResolutionContext context, final String jndiName) {
        StringBuilder uniqueName = new StringBuilder();
        return uniqueName.append(context.getApplicationName() + "_")
                .append(managementName(context, jndiName))
                .toString();
    }

    private static String managementName(InjectionSource.ResolutionContext context, final String jndiName) {
        StringBuilder uniqueName = new StringBuilder();
        uniqueName.append(context.getModuleName() + "_");
        if (context.getComponentName() != null) {
            uniqueName.append(context.getComponentName() + "_");
        }
        return uniqueName
                .append(jndiName.replace(':', '_'))
                .toString();
    }

    private List<PooledConnectionFactoryConfigProperties> getAdapterParams(ModelNode model) {
        Map<String, ConnectionFactoryAttribute> attributes = PooledConnectionFactoryDefinition.getAttributes();
        List<PooledConnectionFactoryConfigProperties> props = new ArrayList<>();

        for (Property property : model.asPropertyList()) {
            ConnectionFactoryAttribute attribute = attributes.get(property.getName());

            if (attribute.getPropertyName() == null) {
                // not a RA property
                continue;
            }

            props.add(new PooledConnectionFactoryConfigProperties(attribute.getPropertyName(), property.getValue().asString(), attribute.getClassType()));
        }
        return props;
    }

    /**
     * The JMS connection factory can specify another hornetq-server to deploy its destinations
     * by passing a property hornetq-server=&lt;name of the server>. Otherwise, "default" is used by default.
     */
    private String getHornetQServerName() {
        return properties.containsKey(HORNETQ_SERVER) ? properties.get(HORNETQ_SERVER) : DEFAULT;
    }
}