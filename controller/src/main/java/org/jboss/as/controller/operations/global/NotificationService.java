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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.dmr.ModelNode;


/**
 * Manages notification listener registration and broadcast notifications.
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2013 Red Hat Inc.
 */
public class NotificationService {

    public static final NotificationService INSTANCE = new NotificationService();
    private static final String RESOURCE = "resource";
    private static final String TYPE = "type";
    private static final String MESSAGE = "message";
    private static final String TIMESTAMP = "timestamp";
    private static final String DATA = "data";

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

    public void unregisterNotificationHandler(PathAddress source, NotificationHandler handler) {
        Set<NotificationHandler> handlers = notificationHandlers.get(source);
        if (notificationHandlers != null) {
            notificationHandlers.remove(handler);
        }
    }

    // TODO emit notifications asynchronously
    public void emit(OperationContext context, final PathAddress address, final String type, final String message, final ModelNode data) {
        System.out.println("NotificationService.emit");
        System.out.println("context = [" + context + "], address = [" + address + "], type = [" + type + "], message = [" + message + "], data = [" + data + "]");
        long timestamp = System.currentTimeMillis();
        List<NotificationHandler> handlers = new ArrayList<NotificationHandler>();
        for (Map.Entry<PathAddress, Set<NotificationHandler>> entry : notificationHandlers.entrySet()) {
            PathAddress entryAddress = entry.getKey();
            if (matches(address, entryAddress)) {
                handlers.addAll(entry.getValue());
            }
        }
        if (handlers.isEmpty()) {
            return;
        }

        ModelNode notification = new ModelNode();
        notification.get(RESOURCE).set(address.toModelNode());
        notification.get(TYPE).set(type);
        notification.get(MESSAGE).set(message);
        notification.get(TIMESTAMP).set(timestamp);
        notification.get(DATA).set(data);

        for (NotificationHandler handler : handlers) {
            handler.handleNotification(notification);
        }
    }

    public static boolean matches(PathAddress address, PathAddress other) {
        if (!other.isMultiTarget()) {
            return address.equals(other);
        }
        if (address.size() != other.size()) {
            return false;
        }
        ListIterator<PathElement> addressIter = address.iterator();
        ListIterator<PathElement> otherIterator = other.iterator();
        while (addressIter.hasNext() && otherIterator.hasNext()) {
            PathElement element = addressIter.next();
            PathElement otherElement = otherIterator.next();
            if (!otherElement.isMultiTarget()) {
                if (!element.equals(otherElement)) {
                    return false;
                }
            } else {
                if (!element.getKey().equals(otherElement.getKey())) {
                    return false;
                }
            }
        }
        return true;
    }

    public interface NotificationHandler {
        void handleNotification(ModelNode notification);
    }

}
