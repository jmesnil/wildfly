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

package org.jboss.as.controller.operations.global;

import static org.jboss.as.controller.SimpleAttributeDefinitionBuilder.create;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.LIST_NOTIFICATION_LISTENERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REGISTER_NOTIFICATION_LISTENER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.UNREGISTER_NOTIFICATION_LISTENER;
import static org.jboss.as.controller.descriptions.common.ControllerResolver.getResolver;

import org.jboss.as.controller.AbstractRuntimeOnlyHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.SimpleMapAttributeDefinition;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.StringListAttributeDefinition;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Handles notification listener registration and unregistation.
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2013 Red Hat Inc.
 */

public class NotificationHandlers {

    public static final NotificationHandlers INSTANCE = new NotificationHandlers();

    private static final AttributeDefinition LISTENER = new SimpleMapAttributeDefinition.Builder("listener", false)
            .setAllowNull(false)
            .build();

    private NotificationHandlers() {
    }

    static class RegisterNotificationListenerHandler {

        public static final OperationDefinition DEFINITION = new SimpleOperationDefinitionBuilder(REGISTER_NOTIFICATION_LISTENER, getResolver("global"))
                .addParameter(LISTENER)
                .build();

        public static final OperationStepHandler INSTANCE = new AbstractRuntimeOnlyHandler() {
            @Override
            protected void executeRuntimeStep(OperationContext context, ModelNode operation) throws OperationFailedException {
                PathAddress sourceAddress = PathAddress.pathAddress(operation.require(OP_ADDR));
                PathAddress listenerAddress = PathAddress.pathAddress(operation.require(LISTENER.getName()));
                NotificationService.INSTANCE.registerNotificationListener(sourceAddress, listenerAddress);
                context.stepCompleted();
            }
        };
    }

    static class UnregisterNotificationListenerHandler {

        public static final OperationDefinition DEFINITION = new SimpleOperationDefinitionBuilder(UNREGISTER_NOTIFICATION_LISTENER, getResolver("global"))
                .addParameter(LISTENER)
                .build();

        public static final OperationStepHandler INSTANCE = new AbstractRuntimeOnlyHandler() {
            @Override
            protected void executeRuntimeStep(OperationContext context, ModelNode operation) throws OperationFailedException {
                PathAddress sourceAddress = PathAddress.pathAddress(operation.require(OP_ADDR));
                PathAddress listenerAddress = PathAddress.pathAddress(operation.require(LISTENER.getName()));
                NotificationService.INSTANCE.unregisterNotificationListener(sourceAddress, listenerAddress);
                context.stepCompleted();
            }
        };
    }

    static class ListNotificationListenersHandler {

        public static final OperationDefinition DEFINITION = new SimpleOperationDefinitionBuilder(LIST_NOTIFICATION_LISTENERS, getResolver("global"))
                .build();

        public static final OperationStepHandler INSTANCE = new AbstractRuntimeOnlyHandler() {
            @Override
            protected void executeRuntimeStep(OperationContext context, ModelNode operation) throws OperationFailedException {
                PathAddress sourceAddress = PathAddress.pathAddress(operation.require(OP_ADDR));
                for (PathAddress listener : NotificationService.INSTANCE.listNotificationListeners(sourceAddress)) {
                    context.getResult().add(listener.toModelNode());
                }
                context.stepCompleted();
            }
        };
    }

    public abstract static class HandleNotificationHandler implements OperationStepHandler {
        static final AttributeDefinition RESOURCE = new SimpleMapAttributeDefinition.Builder("resource", false)
                .setAllowNull(false)
                .build();

        static final AttributeDefinition TYPE = create("type", ModelType.STRING)
                .setAllowNull(false)
                .build();

        static final AttributeDefinition MESSAGE = create("message", ModelType.STRING)
                .setAllowNull(false)
                .build();

        static final AttributeDefinition TIMESTAMP = create("timestamp", ModelType.LONG)
                .setMeasurementUnit(MeasurementUnit.EPOCH_MILLISECONDS)
                .setAllowNull(false)
                .build();

        static final AttributeDefinition DATA = create("data", ModelType.OBJECT)
                .setAllowNull(false)
                .build();

        public static OperationDefinition DEFINITION = new SimpleOperationDefinitionBuilder("handle-notification",  getResolver("global"))
                .addParameter(RESOURCE)
                .addParameter(TYPE)
                .addParameter(TIMESTAMP)
                .addParameter(MESSAGE)
                // FIXME how to describe an *opaque* ModelNode attribute?
                //.addParameter(DATA)
                .build();

        @Override
        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
            try {
                handleNotification(operation);
            } finally {
                context.stepCompleted();
            }
        }

        public abstract void handleNotification(ModelNode notification);
    }
}
