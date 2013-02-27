/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.controller.operations.global;

import static org.jboss.as.controller.ControllerMessages.MESSAGES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.operations.global.GlobalOperationHandlers.NAME;
import static org.jboss.as.controller.operations.global.GlobalOperationHandlers.VALUE;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.common.ControllerResolver;
import org.jboss.as.controller.notification.NotificationService;
import org.jboss.as.controller.notification.NotificationSupport;
import org.jboss.as.controller.operations.validation.ParametersValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;

/**
 * {@link org.jboss.as.controller.OperationStepHandler} writing a single attribute. The required request parameter "name" represents the attribute name.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class WriteAttributeHandler implements OperationStepHandler {

    public static final OperationDefinition DEFINITION = new SimpleOperationDefinitionBuilder(ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION, ControllerResolver.getResolver("global"))
            .setParameters(NAME, VALUE)
            .setRuntimeOnly()
            .build();

    public static final OperationStepHandler INSTANCE = new WriteAttributeHandler();

    private ParametersValidator nameValidator = new ParametersValidator();

    WriteAttributeHandler() {
        nameValidator.registerValidator(GlobalOperationHandlers.NAME.getName(), new StringLengthValidator(1));
    }

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        nameValidator.validate(operation);
        final String attributeName = operation.require(GlobalOperationHandlers.NAME.getName()).asString();
        final AttributeAccess attributeAccess = context.getResourceRegistration().getAttributeAccess(PathAddress.EMPTY_ADDRESS, attributeName);
        if (attributeAccess == null) {
            throw new OperationFailedException(new ModelNode().set(MESSAGES.unknownAttribute(attributeName)));
        } else if (attributeAccess.getAccessType() != AttributeAccess.AccessType.READ_WRITE) {
            throw new OperationFailedException(new ModelNode().set(MESSAGES.attributeNotWritable(attributeName)));
        } else {
            OperationStepHandler handler = attributeAccess.getWriteHandler();
            ClassLoader oldTccl = SecurityActions.setThreadContextClassLoader(handler.getClass());
            try {
                handler.execute(context, operation);
                System.out.println("++ context = " + context.getResult());
            } finally {
                SecurityActions.setThreadContextClassLoader(oldTccl);
            }
        }
    }

    public static OperationStepHandler WRAPPED_INSTANCE = new OperationStepHandler() {
        @Override
        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
            final String attributeName = operation.require(GlobalOperationHandlers.NAME.getName()).asString();

            ModelNode readAttributeOperation = operation.clone();
            readAttributeOperation.get(OPERATION_NAME).set(READ_ATTRIBUTE_OPERATION);
            final ModelNode readOldValueAttributeResponse = new ModelNode();
            context.addStep(readOldValueAttributeResponse, readAttributeOperation, ReadAttributeHandler.INSTANCE, context.getCurrentStage());
            context.addStep(context.getResult(), operation, INSTANCE, context.getCurrentStage());
            context.addStep(new OperationStepHandler() {
                @Override
                public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                    try {
                        System.out.println("oldValue = " + readOldValueAttributeResponse);
                        if (context.hasFailureDescription()) {
                            return;
                        }
                        // FIXME is it a runtime or configuration attribute?
                        ModelNode data = new ModelNode();
                        data.get("old-value").set(readOldValueAttributeResponse.get(RESULT));
                        data.get("new-value").set(operation.get(VALUE.getName()));
                        data.get(OPERATION_NAME).set(operation);

                        PathAddress sourceAddress = PathAddress.pathAddress(operation.get(ModelDescriptionConstants.OP_ADDR));
                        ServiceController<?> notificationService = context.getServiceRegistry(false).getService(NotificationService.SERVICE_NAME);
                        if (notificationService != null) {
                            NotificationSupport notificationSupport = NotificationSupport.class.cast(notificationService.getValue());
                            notificationSupport.emit(sourceAddress,
                                    "ATTRIBUTE_VALUE_CHANGED",
                                    // TODO i18n
                                    "attribute " + attributeName + " value written from " + data.get("old-value") + " to " + data.get("new-value"),
                                    data);
                        }
                    } finally {
                        context.stepCompleted();
                    }
                }
            }, OperationContext.Stage.VERIFY);
            context.stepCompleted();
        }
    };
}
