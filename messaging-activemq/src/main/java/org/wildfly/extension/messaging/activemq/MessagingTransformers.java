/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.MODULE;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.NAME;
import static org.wildfly.extension.messaging.activemq.MessagingExtension.VERSION_1_0_0;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.transform.CombinedTransformer;
import org.jboss.as.controller.transform.OperationRejectionPolicy;
import org.jboss.as.controller.transform.OperationResultTransformer;
import org.jboss.as.controller.transform.ResourceTransformationContext;
import org.jboss.as.controller.transform.TransformationContext;
import org.jboss.as.controller.transform.description.ChainedTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.TransformationDescriptionBuilder;
import org.jboss.dmr.ModelNode;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2016 Red Hat inc.
 */
public class MessagingTransformers {

    public static void registerTransformers(SubsystemRegistration subsystem) {
        ChainedTransformationDescriptionBuilder chainedBuilder = TransformationDescriptionBuilder.Factory.createChainedSubystemInstance(subsystem.getSubsystemVersion());

        // Current
        // 2.0.0 -> 1.0.0 (WildFly 10.0.Final)
        buildTransformers2_0_0(chainedBuilder.createBuilder(subsystem.getSubsystemVersion(), VERSION_1_0_0));

        chainedBuilder.buildAndRegister(subsystem, new ModelVersion[]{
                VERSION_1_0_0
        });

    }

    private static void buildTransformers2_0_0(ResourceTransformationDescriptionBuilder builder) {
        ResourceTransformationDescriptionBuilder server = builder.addChildResource(MessagingExtension.SERVER_PATH);

        ResourceTransformationDescriptionBuilder connectorService = server.addChildResource(MessagingExtension.CONNECTOR_SERVICE_PATH);
        // transform connector-service class attribute into the legacy factory-class attribute
        CombinedTransformer connectorServiceTransformer = new ClassTransformer(ConnectorServiceDefinition.CLASS.getName(), CommonAttributes.FACTORY_CLASS.getName());
        connectorService.setCustomResourceTransformer(connectorServiceTransformer);
        connectorService.addOperationTransformationOverride(ADD)
                .inheritResourceAttributeDefinitions()
                .setCustomOperationTransformer(connectorServiceTransformer);

        ResourceTransformationDescriptionBuilder divert = server.addChildResource(MessagingExtension.DIVERT_PATH);
        // transform divert transformer-class attribute into the legacy transformer-class-name attribute
        CombinedTransformer divertTransformer = new ClassTransformer(CommonAttributes.TRANSFORMER_CLASS.getName(), CommonAttributes.TRANSFORMER_CLASS_NAME.getName());
        divert.setCustomResourceTransformer(divertTransformer);
        divert.addOperationTransformationOverride(ADD)
                .inheritResourceAttributeDefinitions()
                .setCustomOperationTransformer(divertTransformer);

        ResourceTransformationDescriptionBuilder bridge = server.addChildResource(MessagingExtension.BRIDGE_PATH);
        // transform bridge transformer-class attribute into the legacy transformer-class-name attribute
        CombinedTransformer bridgeTransformer = new ClassTransformer(CommonAttributes.TRANSFORMER_CLASS.getName(), CommonAttributes.TRANSFORMER_CLASS_NAME.getName());
        bridge.setCustomResourceTransformer(bridgeTransformer);
        bridge.addOperationTransformationOverride(ADD)
                .inheritResourceAttributeDefinitions()
                .setCustomOperationTransformer(bridgeTransformer);
    }

    /**
     * Transform a resource with <attribute name>={name=>x, module=>y} to a legacy resource service with <legacy attribute name>=x.
     * Reject if the module y != org.apache.activemq.artemis
     *
     */
    private static class ClassTransformer implements CombinedTransformer {

        private final String attributeName;
        private final String legacyAttributeName;

        public ClassTransformer(String attributeName, String legacyAttributeName) {
            this.attributeName = attributeName;
            this.legacyAttributeName = legacyAttributeName;
        }

        @Override
        public TransformedOperation transformOperation(TransformationContext context, PathAddress address, ModelNode operation) throws OperationFailedException {
            ModelNode classModel = operation.get(attributeName);
            String module = classModel.get(MODULE).asString();
            if (MessagingExtension.ACTIVEMQ_ARTEMIS_MODULE_ID.equals(module)) {
                return new TransformedOperation(operation, new OperationRejectionPolicy() {
                    @Override
                    public boolean rejectOperation(ModelNode preparedResult) {
                        return true;
                    }

                    @Override
                    public String getFailureDescription() {
                        return context.getLogger().getRejectedResourceWarning(address, operation);
                    }
                }, OperationResultTransformer.ORIGINAL_RESULT);
            }
            operation.remove(attributeName);
            operation.get(legacyAttributeName).set(classModel.get(NAME));
            return new TransformedOperation(operation, OperationResultTransformer.ORIGINAL_RESULT);
        }

        @Override
        public void transformResource(ResourceTransformationContext context, PathAddress address, Resource resource) throws OperationFailedException {
            ModelNode transformed = resource.getModel().clone();
            ModelNode classModel = transformed.remove(attributeName);
            String module = classModel.get(MODULE).asString();
            if (MessagingExtension.ACTIVEMQ_ARTEMIS_MODULE_ID.equals(module)) {
                throw new OperationFailedException("can not transform a connector service with a specific module");
            }
            transformed.get(legacyAttributeName).set(classModel.get(NAME));
            context.addTransformedResource(PathAddress.EMPTY_ADDRESS, resource);
        }
    }
}
