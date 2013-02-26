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

package org.jboss.as.domain.http.server;

import static org.jboss.as.domain.http.server.Constants.APPLICATION_JSON;
import static org.jboss.as.domain.http.server.Constants.CONTENT_TYPE;
import static org.jboss.as.domain.http.server.Constants.CREATED;
import static org.jboss.as.domain.http.server.Constants.DELETE;
import static org.jboss.as.domain.http.server.Constants.GET;
import static org.jboss.as.domain.http.server.Constants.INTERNAL_SERVER_ERROR;
import static org.jboss.as.domain.http.server.Constants.LINK;
import static org.jboss.as.domain.http.server.Constants.LOCATION;
import static org.jboss.as.domain.http.server.Constants.METHOD_NOT_ALLOWED;
import static org.jboss.as.domain.http.server.Constants.NOT_FOUND;
import static org.jboss.as.domain.http.server.Constants.NO_CONTENT;
import static org.jboss.as.domain.http.server.Constants.OK;
import static org.jboss.as.domain.http.server.Constants.POST;
import static org.jboss.as.domain.http.server.DomainUtil.safeClose;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.notification.NotificationHandler;
import org.jboss.as.controller.notification.NotificationSupport;
import org.jboss.as.domain.http.server.security.SubjectAssociationHandler;
import org.jboss.as.domain.management.AuthenticationMechanism;
import org.jboss.as.domain.management.SecurityRealm;
import org.jboss.com.sun.net.httpserver.Authenticator;
import org.jboss.com.sun.net.httpserver.Filter;
import org.jboss.com.sun.net.httpserver.HttpContext;
import org.jboss.com.sun.net.httpserver.HttpExchange;
import org.jboss.com.sun.net.httpserver.HttpServer;
import org.jboss.dmr.ModelNode;

/**
 *
 * The HTTP notification handler.
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) Red Hat, inc
 */
public class NotificationApiHandler implements ManagementHttpHandler {

    /**
     * Maximum number of notifications held *per handler*
     */
    public static final int MAX_NOTIFICATIONS = 1024;

    private static final String NOTIFICATION_API_CONTEXT = "/notification";
    private static final String HANDLER_PREFIX = "handler";
    private static final String NOTIFICATIONS = "notifications";

    private final NotificationSupport notificationSupport;
    private final Authenticator authenticator;
    /**
     * Map of HttpNotificationHandler holding the notifications for a given handlerID
     */
    private final Map<String, HttpNotificationHandler> handlers = new HashMap<String, HttpNotificationHandler>();
    private final AtomicLong handlerCounter = new AtomicLong();

    public NotificationApiHandler(NotificationSupport notificationSupport, Authenticator authenticator) {
        this.notificationSupport = notificationSupport;
        this.authenticator = authenticator;
    }

    public void start(final HttpServer httpServer, final SecurityRealm securityRealm) {
        // The SubjectAssociationHandler wraps all calls to this HttpHandler to ensure the Subject has been associated
        // with the security context.
        HttpContext context = httpServer.createContext(NOTIFICATION_API_CONTEXT, new SubjectAssociationHandler(this));
        // Once there is a trust store we can no longer rely on users being defined so skip
        // any redirects.
        if (authenticator != null) {
            context.setAuthenticator(authenticator);
            List<Filter> filters = context.getFilters();
            if (securityRealm != null && !securityRealm.getSupportedAuthenticationMechanisms().contains(AuthenticationMechanism.CLIENT_CERT)) {
                filters.add(new DmrFailureReadinessFilter(securityRealm, ErrorHandler.getRealmRedirect()));
            }
        }
    }

    public void stop(final HttpServer httpServer) {
        httpServer.removeContext(NOTIFICATION_API_CONTEXT);
        handlers.clear();
    }

    @Override
    public void handle(final HttpExchange exchange) throws IOException {
        // make sure we send something back
        try {
            doHandle(exchange);
        } catch (Exception e) {
            sendResponse(exchange, INTERNAL_SERVER_ERROR, e.getMessage() + "\n");
        }
    }

    private void doHandle(final HttpExchange http) throws IOException {
        final String method = http.getRequestMethod();
        final URI requestURI = http.getRequestURI();
        if (requestURI.getPath().equals(NOTIFICATION_API_CONTEXT)) {
            if (method.equals(POST)) {
                // POST /notification => create a handler resource that listens to a list of resources
                String handlerID = generateHandlerID();
                ModelNode operation = ModelNode.fromJSONStream(http.getRequestBody());
                registerNotificationHandler(handlerID, operation);
                http.getResponseHeaders().add(LOCATION, getHandlerURL(handlerID));
                http.getResponseHeaders().add(LINK, getHandlerNotificationsURL(handlerID));
                sendResponse(http, CREATED);
                return;
            } else {
                sendResponse(http, METHOD_NOT_ALLOWED);
                return;
            }
        } else {
            String[] splits = requestURI.getPath().split("/");
            final String handlerID = splits[2];
            // /notification/${handlerID}
            if (splits.length == 3) {
                if (GET.equals(method)) {
                    // GET /notification/${handlerID} => returns a representation of the handler
                    ModelNode addresses = getAddressesListeningTo(handlerID);
                    if (addresses == null) {
                        sendResponse(http, NOT_FOUND);
                        return;
                    } else {
                        http.getResponseHeaders().add(LINK, getHandlerNotificationsURL(handlerID));
                        sendResponse(http, OK, addresses.isDefined()? addresses.toJSONString(true) : null);
                        return;
                    }
                } else if (POST.equals(method)) {
                    // POST /notification/${handlerID} => update the resources that the handler listens to.
                    ModelNode operation = ModelNode.fromJSONStream(http.getRequestBody());
                    unregisterNotificationHandler(handlerID);
                    registerNotificationHandler(handlerID, operation);
                    sendResponse(http, OK);
                    return;
                } else if (DELETE.equals(method)) {
                    // DELETE /notification/${handlerID} => unregister the handler and delete it
                    boolean unregistered = unregisterNotificationHandler(handlerID);
                    if (unregistered) {
                        sendResponse(http, NO_CONTENT);
                        return;
                    } else {
                        sendResponse(http, NOT_FOUND);
                        return;
                    }
                } else {
                    sendResponse(http, METHOD_NOT_ALLOWED);
                    return;
                }
            } else if (splits.length == 4 && splits[3].equals(NOTIFICATIONS)) {
                if (POST.equals(method)) {
                    // POST /notification/${handlerID}/notifications => returns a representation of the notifications received by the handler
                    // and clears it.
                    ModelNode notifications = fetchNotifications(handlerID);
                    if (notifications == null) {
                        sendResponse(http, NOT_FOUND);
                        return;
                    } else {
                        sendResponse(http, OK, notifications.isDefined()? notifications.toJSONString(true) : null);
                        return;
                    }
                } else {
                    sendResponse(http, METHOD_NOT_ALLOWED);
                    return;
                }
            }
        }
        sendResponse(http, NOT_FOUND);
    }

    private String generateHandlerID() {
        return HANDLER_PREFIX + handlerCounter.incrementAndGet();
    }

    private ModelNode getAddressesListeningTo(String handlerID) {
        System.out.println(">>> NotificationApiHandler.getAddressesListeningTo handlerID = [" + handlerID + "]");
        if (!handlers.containsKey(handlerID)) {
            return null;
        }
        final ModelNode node = new ModelNode();
        HttpNotificationHandler handler = handlers.get(handlerID);
        for (PathAddress address : handler.getListeningAddresses()) {
            node.add(address.toModelNode());
        }
        return node;
    }

    private ModelNode fetchNotifications(String handlerID) {
        System.out.println(">>> NotificationApiHandler.fetchNotifications handlerID = [" + handlerID + "]");
        if (!handlers.containsKey(handlerID)) {
            return null;
        }
        ModelNode node = new ModelNode();
        HttpNotificationHandler handler = handlers.get(handlerID);
        for (ModelNode notification : handler.getNotifications()) {
            node.add(notification);
        }
        handler.clear();
        return node;
    }

    private void registerNotificationHandler(final String handlerID, final ModelNode operation) {
        System.out.println("NotificationApiHandler.registerNotificationHandler handlerID = [" + handlerID + "], operation = [" + operation + "]");
        final Set<PathAddress> addresses = new HashSet<PathAddress>();
        for (ModelNode resource : operation.get("resources").asList()) {
            addresses.add(PathAddress.pathAddress(resource));
        }
        System.out.println("addresses = " + addresses);
        for (PathAddress address : addresses) {
            if (address.isMultiTarget()) {
                System.out.println("got pattern:" + address);
            }
        }
        final HttpNotificationHandler handler = new HttpNotificationHandler(handlerID, addresses);
        for (PathAddress address : addresses) {
            notificationSupport.registerNotificationHandler(address, handler);
        }
        handlers.put(handlerID, handler);
    }

    private boolean unregisterNotificationHandler(String handlerID) {
        System.out.println(">>> NotificationApiHandler.unregisterNotificationHandler handlerID = [" + handlerID + "]");
        HttpNotificationHandler handler = handlers.remove(handlerID);
        if (handler != null) {
            for (PathAddress address : handler.getListeningAddresses()) {
                notificationSupport.unregisterNotificationHandler(address, handler);
            }
        }
        return handler != null;
    }

    private void sendResponse(final HttpExchange exchange, final int responseCode) throws IOException {
        exchange.sendResponseHeaders(responseCode, -1);
    }

    private void sendResponse(final HttpExchange exchange, final int responseCode, final String body) throws IOException {
        if (body == null) {
            exchange.sendResponseHeaders(responseCode, -1);
            return;
        }
        exchange.getResponseHeaders().add(CONTENT_TYPE, APPLICATION_JSON);
        exchange.sendResponseHeaders(responseCode, 0);
        final PrintWriter out = new PrintWriter(exchange.getResponseBody());
        try {
            out.print(body);
            out.flush();
        } finally {
            safeClose(out);
        }
    }

   static class HttpNotificationHandler implements NotificationHandler {

        private final String handlerID;
        private final Set<PathAddress> addresses;
        private final Queue<ModelNode> notifications = new ArrayBlockingQueue<ModelNode>(MAX_NOTIFICATIONS);

        public HttpNotificationHandler(String handlerID, Set<PathAddress> addresses) {
            this.handlerID = handlerID;
            this.addresses = addresses;
        }

        public Set<PathAddress> getListeningAddresses() {
            return addresses;
        }

        public List<ModelNode> getNotifications() {
            return new ArrayList<ModelNode>(notifications);
        }

        public void clear() {
            notifications.clear();
        }

        @Override
        public void handleNotification(ModelNode notification) {
            // keep only the most recent notifications
            if (notifications.size() == MAX_NOTIFICATIONS) {
                notifications.poll();
            }
            notifications.add(notification.clone());
        }

        @Override
        public String toString() {
            return "HttpNotificationHandler[" +
                    "handlerID='" + handlerID + '\'' +
                    ", addresses=" + addresses +
                    "]@" + System.identityHashCode(this);
        }
    }

    private static String getHandlerURL(final String handlerID) {
        return NOTIFICATION_API_CONTEXT + "/" + handlerID;
    }

    private static String getHandlerNotificationsURL(final String handlerID) {
        return String.format("%s/%s/%s; rel=%s",
                NOTIFICATION_API_CONTEXT,
                handlerID,
                NOTIFICATIONS,
                NOTIFICATIONS);
    }
}
