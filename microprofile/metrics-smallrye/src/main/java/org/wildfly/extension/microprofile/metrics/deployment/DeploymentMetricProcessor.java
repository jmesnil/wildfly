/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.microprofile.metrics.deployment;

import static org.eclipse.microprofile.metrics.MetricRegistry.Type.APPLICATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBDEPLOYMENT;

import java.util.Set;

import io.smallrye.metrics.MetricRegistries;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.server.deployment.DeploymentModelUtils;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.wildfly.extension.microprofile.metrics.MetricsRegistrationService;

public class DeploymentMetricProcessor implements DeploymentUnitProcessor {

    static final AttachmentKey<MetricsRegistrationService> METRICS_REGISTRATION = AttachmentKey.create(MetricsRegistrationService.class);
    static final AttachmentKey<Set<String>> REGISTERED_METRICS = AttachmentKey.create(Set.class);

    private Resource rootResource;
    private ManagementResourceRegistration managementResourceRegistration;

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) {
        rootResource = phaseContext.getDeploymentUnit().getAttachment(DeploymentModelUtils.DEPLOYMENT_RESOURCE);
        managementResourceRegistration = phaseContext.getDeploymentUnit().getAttachment(DeploymentModelUtils.MUTABLE_REGISTRATION_ATTACHMENT);
        MetricsRegistrationService metricRegistration = phaseContext.getDeploymentUnit().getAttachment(METRICS_REGISTRATION);
        MetricRegistry applicationRegistry = MetricRegistries.get(APPLICATION);
        PathAddress deploymentAddress = createDeploymentAddressPrefix(phaseContext.getDeploymentUnit());
        Set<String> registeredMetrics = metricRegistration.registerMetrics(rootResource, managementResourceRegistration, applicationRegistry,
                new MetricsRegistrationService.MetricNameCreator() {
                    @Override
                    public String createName(PathAddress address, String attributeName) {
                        return deploymentAddress.append(address).toPathStyleString().substring(1) + "/" + attributeName;
                    }

                    @Override
                    public PathAddress getResourceAddress(PathAddress address) {
                        return deploymentAddress.append(address);
                    }
                });
        phaseContext.getDeploymentUnit().putAttachment(REGISTERED_METRICS, registeredMetrics);
    }

    @Override
    public void undeploy(DeploymentUnit context) {
        Set<String> registeredMetrics = context.getAttachment(REGISTERED_METRICS);
        if (registeredMetrics != null) {
            MetricRegistry applicationRegistry = MetricRegistries.get(APPLICATION);
            for (String registeredMetric : registeredMetrics) {
                applicationRegistry.remove(registeredMetric);
            }
        }
    }

    private PathAddress createDeploymentAddressPrefix(DeploymentUnit deploymentUnit) {
        if (deploymentUnit.getParent() == null) {
            return PathAddress.pathAddress(DEPLOYMENT, deploymentUnit.getName());
        } else {
            return createDeploymentAddressPrefix(deploymentUnit.getParent()).append(SUBDEPLOYMENT, deploymentUnit.getName());
        }
    }
}
