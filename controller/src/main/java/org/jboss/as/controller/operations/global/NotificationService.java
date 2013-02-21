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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.operations.global.NotificationHandlers.HandleNotificationHandler;
import static org.jboss.as.controller.operations.global.NotificationHandlers.HandleNotificationHandler.DATA;
import static org.jboss.as.controller.operations.global.NotificationHandlers.HandleNotificationHandler.MESSAGE;
import static org.jboss.as.controller.operations.global.NotificationHandlers.HandleNotificationHandler.RESOURCE;
import static org.jboss.as.controller.operations.global.NotificationHandlers.HandleNotificationHandler.TIMESTAMP;
import static org.jboss.as.controller.operations.global.NotificationHandlers.HandleNotificationHandler.TYPE;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;


/**
 * Manages notification listener registration and broadcast notifications.
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2013 Red Hat Inc.
 */
public class NotificationService {

    public static final NotificationService INSTANCE = new NotificationService();

    private Map<PathAddress, Set<PathAddress>> notificationListeners = new HashMap<PathAddress, Set<PathAddress>>();
    private Map<PathAddress, Set<NotificationHandler>> notificationHandlers = new HashMap<PathAddress, Set<NotificationHandler>>();

    private NotificationService() {
    }

    public void registerNotificationHandler(PathAddress source, NotificationHandler handler) {
        Set<NotificationHandler> handlers = notificationHandlers.get(source);
        if (handlers == null) {
            handlers = new HashSet<NotificationHandler>();
        }
        handlers.add(handler);
        notificationHandlers.put(source, handlers);
    }

    public void unregisterNotificationListener(PathAddress source, NotificationHandler handler) {
        Set<NotificationHandler> handlers = notificationHandlers.get(source);
        if (notificationHandlers != null) {
            notificationHandlers.remove(handler);
        }
    }

    void registerNotificationListener(PathAddress source, PathAddress listener) {
        Set<PathAddress> listeners = notificationListeners.get(source);
        if (listeners == null) {
            listeners = new HashSet<PathAddress>();
        }
        listeners.add(listener);
        notificationListeners.put(source, listeners);
    }

    public void unregisterNotificationListener(PathAddress source, PathAddress listener) {
        Set<PathAddress> listeners = notificationListeners.get(source);
        if (listeners != null) {
            listeners.remove(listener);
        }
    }

    public Set<PathAddress> listNotificationListeners(PathAddress source) {
        Set<PathAddress> listeners = notificationListeners.get(source);
        if (listeners != null) {
            return listeners;
        } else {
            return Collections.emptySet();
        }
    }

    public void emit(OperationContext context, final PathAddress address, final String type, final String message, final ModelNode data) {
        System.out.println("NotificationService.emit");
        System.out.println("context = [" + context + "], address = [" + address + "], type = [" + type + "], message = [" + message + "], data = [" + data + "]");
        long timestamp = System.currentTimeMillis();
        Set<NotificationHandler> handlers = notificationHandlers.get(address);
        if (handlers == null || handlers.isEmpty()) {
            if (address.size() <= 1) {
                return;
            }
            PathAddress parent = PathAddress.pathAddress(address.subAddress(0, address.size() - 1));
            PathAddress wildcard = parent.append(address.getLastElement().getKey(), "#");
            handlers = notificationHandlers.get(wildcard);
            if (handlers == null || handlers.isEmpty()) {
                return;
            }
        }
        System.out.println("handlers = " + handlers);

        ModelNode notification = new ModelNode();
        notification.get(RESOURCE.getName()).set(address.toModelNode());
        notification.get(TYPE.getName()).set(type);
        notification.get(MESSAGE.getName()).set(message);
        notification.get(TIMESTAMP.getName()).set(timestamp);
        notification.get(DATA.getName()).set(data);

        for (NotificationHandler handler : handlers) {
            handler.handleNotification(notification);
        }
    }

    public interface NotificationHandler {
        void handleNotification(ModelNode notification);
    }

}
