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

import static org.jboss.as.messaging.CommonAttributes.DEFAULT;
import static org.jboss.as.messaging.CommonAttributes.HORNETQ_SERVER;
import static org.jboss.as.messaging.CommonAttributes.POOLED_CONNECTION_FACTORY;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.ee.component.InjectionSource;
import org.jboss.as.messaging.MessagingExtension;
import org.jboss.as.messaging.MessagingServices;
import org.jboss.as.messaging.jms.PooledConnectionFactoryConfigProperties;
import org.jboss.as.messaging.jms.PooledConnectionFactoryService;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
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
    private String interfaceName;
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

    public void setInterfaceName(String interfaceName) {
        this.interfaceName = interfaceName;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public void setResourceAdapter(String resourceAdapter) {
        this.resourceAdapter = resourceAdapter;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }

    public void setTransactional(boolean transactional) {
        this.transactional = transactional;
    }

    public void setMaxPoolSize(int maxPoolSize) {
        this.maxPoolSize = maxPoolSize;
    }

    public void setMinPoolSize(int minPoolSize) {
        this.minPoolSize = minPoolSize;
    }


    @Override
    public void getResourceValue(ResolutionContext context, ServiceBuilder<?> serviceBuilder, DeploymentPhaseContext phaseContext, Injector<ManagedReferenceFactory> injector) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final String uniqueName = uniqueName(context, name);

        try {
            ServiceName hqServiceName = MessagingServices.getHornetQServiceName(getHornetQServerName());

            startedPooledConnectionFactory(context, uniqueName, serviceBuilder, phaseContext.getServiceTarget(), hqServiceName, deploymentUnit, injector);
        } catch (Exception e) {
            throw new DeploymentUnitProcessingException(e);
        }
    }

    private void startedPooledConnectionFactory(ResolutionContext context, String pcfName, ServiceBuilder<?> serviceBuilder, ServiceTarget serviceTarget, ServiceName hqServiceName, DeploymentUnit deploymentUnit, Injector<ManagedReferenceFactory> injector) {
        List<String> connectors = new ArrayList<>();
        String discoveryGroupName = null;
        String jgroupsChannelName = null;
        List<PooledConnectionFactoryConfigProperties> adapterParams  = new ArrayList<>();
        String txSupport = null;

        final ContextNames.BindInfo bindInfo = ContextNames.bindInfoForEnvEntry(context.getApplicationName(), context.getModuleName(), context.getComponentName(), !context.isCompUsesModule(), name);
        PooledConnectionFactoryService.installService(null, null, serviceTarget, pcfName, getHornetQServerName(), connectors,
                discoveryGroupName, jgroupsChannelName, adapterParams,
                bindInfo,
                txSupport, minPoolSize, maxPoolSize);

        //create the management registration
        final PathElement serverElement = PathElement.pathElement(HORNETQ_SERVER, getHornetQServerName());
        deploymentUnit.createDeploymentSubModel(MessagingExtension.SUBSYSTEM_NAME, serverElement);
        final PathElement dest = PathElement.pathElement(POOLED_CONNECTION_FACTORY, pcfName);
        PathAddress registration = PathAddress.pathAddress(serverElement, dest);
        MessagingXmlInstallDeploymentUnitProcessor.createDeploymentSubModel(registration, deploymentUnit);
    }

    private String uniqueName(InjectionSource.ResolutionContext context, final String jndiName) {
        StringBuilder uniqueName = new StringBuilder();
        uniqueName.append(context.getApplicationName() + "_");
        uniqueName.append(context.getModuleName() + "_");
        if (context.getComponentName() != null) {
            uniqueName.append(context.getComponentName() + "_");
        }
        uniqueName.append(jndiName.replace(':', '_'));
        return uniqueName.toString();
    }


    /**
     * The JMS connection factory can specify another hornetq-server to deploy its destinations
     * by passing a property hornetq-server=&lt;name of the server>. Otherwise, "default" is used by default.
     */
    private String getHornetQServerName() {
        return properties.containsKey(HORNETQ_SERVER) ? properties.get(HORNETQ_SERVER) : DEFAULT;
    }
}