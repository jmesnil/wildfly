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

package org.wildfly.extension.metrics.deployment;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBDEPLOYMENT;

import java.util.List;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentModelUtils;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.wildfly.extension.metrics.MetricCollector;
import org.wildfly.extension.metrics.MetricRegistration;
import org.wildfly.extension.metrics.WildFlyMetricRegistry;

public class DeploymentMetricProcessor implements DeploymentUnitProcessor {

    public static final AttachmentKey<MetricCollector> METRICS_COLLECTOR = AttachmentKey.create(MetricCollector.class);
    static final AttachmentKey<WildFlyMetricRegistry> WILDFLY_METRIC_REGISTRY = AttachmentKey.create(WildFlyMetricRegistry.class);
    static final AttachmentKey<MetricRegistration> METRIC_REGISTRATION = AttachmentKey.create(MetricRegistration.class);

    private final boolean exposeAnySubsystem;
    private final List<String> exposedSubsystems;
    private final String prefix;

    private Resource rootResource;
    private ManagementResourceRegistration managementResourceRegistration;

    public DeploymentMetricProcessor(boolean exposeAnySubsystem, List<String> exposedSubsystems, String prefix) {

        this.exposeAnySubsystem = exposeAnySubsystem;
        this.exposedSubsystems = exposedSubsystems;
        this.prefix = prefix;
    }

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) {
        rootResource = phaseContext.getDeploymentUnit().getAttachment(DeploymentModelUtils.DEPLOYMENT_RESOURCE);
        managementResourceRegistration = phaseContext.getDeploymentUnit().getAttachment(DeploymentModelUtils.MUTABLE_REGISTRATION_ATTACHMENT);
        MetricCollector metricCollector = phaseContext.getDeploymentUnit().getAttachment(METRICS_COLLECTOR);
        WildFlyMetricRegistry metricRegistry = phaseContext.getDeploymentUnit().getAttachment(WILDFLY_METRIC_REGISTRY);

        PathAddress deploymentAddress = createDeploymentAddressPrefix(phaseContext.getDeploymentUnit());

        MetricRegistration registration = new MetricRegistration(metricRegistry);
        metricCollector.collectResourceMetrics(rootResource,
                managementResourceRegistration,
                // prepend the deployment address to the subsystem resource address
                address -> deploymentAddress.append(address),
                exposeAnySubsystem, exposedSubsystems, prefix,
                registration);

        phaseContext.getDeploymentUnit().putAttachment(METRIC_REGISTRATION, registration);
    }

    @Override
    public void undeploy(DeploymentUnit context) {
        MetricRegistration registration = context.removeAttachment(METRIC_REGISTRATION);
        registration.unregister();
    }

    private static PathAddress createDeploymentAddressPrefix(DeploymentUnit deploymentUnit) {
        if (deploymentUnit.getParent() == null) {
            return PathAddress.pathAddress(DEPLOYMENT, deploymentUnit.getAttachment(Attachments.MANAGEMENT_NAME));
        } else {
            return createDeploymentAddressPrefix(deploymentUnit.getParent()).append(SUBDEPLOYMENT, deploymentUnit.getName());
        }
    }

}
