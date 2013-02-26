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

package org.jboss.as.controller.notification;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;


/**
 * Manages notification listener registration and broadcast notifications.
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2013 Red Hat Inc.
 */
public class NotificationService implements Service<NotificationSupport> {

    public static final String RESOURCE = "resource";
    public static final String TYPE = "type";
    public static final String MESSAGE = "message";
    public static final String TIMESTAMP = "timestamp";
    public static final String DATA = "data";
    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("notification");

    private NotificationSupport emitter;

    private NotificationService() {
        emitter = new NotificationEmitterImpl();
    }

    public static void installNotificationService(ServiceTarget serviceTarget) {
        serviceTarget.addService(SERVICE_NAME, new NotificationService())
                .setInitialMode(ServiceController.Mode.ACTIVE)
                .install();
    }

    @Override
    public void start(StartContext startContext) throws StartException {
        emitter = new NotificationEmitterImpl();
    }

    @Override
    public void stop(StopContext stopContext) {
        emitter = null;
    }

    @Override
    public NotificationSupport getValue() throws IllegalStateException, IllegalArgumentException {
        return emitter;
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

    class NotificationEmitterImpl implements NotificationSupport {

        private Map<PathAddress, Set<NotificationHandler>> notificationHandlers = new HashMap<PathAddress, Set<NotificationHandler>>();

        public NotificationEmitterImpl() {
        }

        @Override
        public void registerNotificationHandler(PathAddress source, NotificationHandler handler) {
            Set<NotificationHandler> handlers = notificationHandlers.get(source);
            if (handlers == null) {
                handlers = new HashSet<NotificationHandler>();
            }
            handlers.add(handler);
            notificationHandlers.put(source, handlers);
        }

        @Override
        public void unregisterNotificationHandler(PathAddress source, NotificationHandler handler) {
            Set<NotificationHandler> handlers = notificationHandlers.get(source);
            if (notificationHandlers != null) {
                notificationHandlers.remove(handler);
            }
        }

        @Override
        public void emit(PathAddress address, String type, String message) {
            emit(address, type, message, new ModelNode());
        }

        // TODO emit notifications asynchronously
        public void emit(final PathAddress address, final String type, final String message, final ModelNode data) {
            //System.out.println("address = [" + address + "], type = [" + type + "], message = [" + message + "], data = [" + data + "]");
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
            if (data.isDefined()) {
                notification.get(DATA).set(data);
            }

            for (NotificationHandler handler : handlers) {
                handler.handleNotification(notification);
            }
        }
    }
}
