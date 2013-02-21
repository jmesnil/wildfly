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

import static org.jboss.as.controller.operations.global.NotificationService.NotificationHandler;
import static org.jboss.as.domain.http.server.Constants.APPLICATION_JSON;
import static org.jboss.as.domain.http.server.Constants.CONTENT_TYPE;
import static org.jboss.as.domain.http.server.Constants.CREATED;
import static org.jboss.as.domain.http.server.Constants.GET;
import static org.jboss.as.domain.http.server.Constants.INTERNAL_SERVER_ERROR;
import static org.jboss.as.domain.http.server.Constants.LOCATION;
import static org.jboss.as.domain.http.server.Constants.METHOD_NOT_ALLOWED;
import static org.jboss.as.domain.http.server.Constants.NOT_FOUND;
import static org.jboss.as.domain.http.server.Constants.NOT_MODIFIED;
import static org.jboss.as.domain.http.server.Constants.NO_CONTENT;
import static org.jboss.as.domain.http.server.Constants.OK;
import static org.jboss.as.domain.http.server.Constants.POST;
import static org.jboss.as.domain.http.server.DomainUtil.safeClose;
import static org.jboss.dmr.ModelType.LIST;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.operations.global.NotificationService;
import org.jboss.as.domain.http.server.security.SubjectAssociationHandler;
import org.jboss.as.domain.management.AuthenticationMechanism;
import org.jboss.as.domain.management.SecurityRealm;
import org.jboss.com.sun.net.httpserver.Authenticator;
import org.jboss.com.sun.net.httpserver.Filter;
import org.jboss.com.sun.net.httpserver.HttpContext;
import org.jboss.com.sun.net.httpserver.HttpExchange;
import org.jboss.com.sun.net.httpserver.HttpServer;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 *
 * The HTTP notification handler.
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) Red Hat, inc
 */
public class NotificationApiHandler implements ManagementHttpHandler {

    private static final String NOTIFICATION_API_CONTEXT = "/notification";
    public static final String HANDLER_PREFIX = "handler";
    public static final String DELETE = "DELETE";
    public static final String LINK = "Link";
    public static final String NOTIFICATIONS = "notifications";

    private final ModelControllerClient modelController;
    private final Authenticator authenticator;
    private final AtomicLong handlerCounter;

    private final Map<String, HttpNotificationHandler> handlers = new HashMap<String, HttpNotificationHandler>();

    public NotificationApiHandler(ModelControllerClient modelController, Authenticator authenticator) {
        this.modelController = modelController;
        this.authenticator = authenticator;
        this.handlerCounter = new AtomicLong();
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
            if (securityRealm != null &&  securityRealm.getSupportedAuthenticationMechanisms().contains(AuthenticationMechanism.CLIENT_CERT) == false) {
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
            e.printStackTrace();
            sendResponse(exchange, INTERNAL_SERVER_ERROR, e.getMessage() + "\n");
        }
    }

    private void doHandle(final HttpExchange http) throws IOException {
        final String method = http.getRequestMethod();
        final URI requestURI = http.getRequestURI();
        System.out.println("requestURI = " + requestURI.getPath());
        if (requestURI.getPath().equals(NOTIFICATION_API_CONTEXT)) {
            if (method.equals(POST)) {
                String handlerID = HANDLER_PREFIX + handlerCounter.incrementAndGet();
                ModelNode operation = ModelNode.fromJSONStream(http.getRequestBody());
                // create the listener based on the address in the operation
                registerNotificationHandler(handlerID, operation);
                http.getResponseHeaders().add(LOCATION, NOTIFICATION_API_CONTEXT + "/" + handlerID);
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
                    ModelNode addresses = getAddressesListeningTo(handlerID);
                    if (addresses == null) {
                        sendResponse(http, NOT_FOUND);
                        return;
                    } else {
                        http.getResponseHeaders().add(LINK, String.format("%s/%s/%s; rel=%s",
                                NOTIFICATION_API_CONTEXT,
                                handlerID,
                                NOTIFICATIONS,
                                NOTIFICATIONS));
                        http.getResponseHeaders().add(CONTENT_TYPE, APPLICATION_JSON);
                        sendResponse(http, OK, addresses.isDefined()? addresses.toJSONString(true) : null);
                        return;
                    }
                } else if (POST.equals(method)) {
                    ModelNode operation = ModelNode.fromJSONStream(http.getRequestBody());
                    // update the notification handler registration
                    unregisterNotificationHandler(handlerID);
                    registerNotificationHandler(handlerID, operation);
                    sendResponse(http, OK);
                    return;
                } else if (DELETE.equals(method)) {
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
                    ModelNode notifications = fetchNotifications(handlerID);
                    if (notifications == null) {
                        sendResponse(http, NOT_FOUND);
                        return;
                    } else {
                        http.getResponseHeaders().add(CONTENT_TYPE, APPLICATION_JSON);
                        sendResponse(http, OK, notifications.isDefined()? notifications.toJSONString(true) : null);
                        return;
                    }
                }
            }
        }
        sendResponse(http, NOT_FOUND);
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
        final HttpNotificationHandler handler = new HttpNotificationHandler(handlerID, addresses);
        for (PathAddress address : addresses) {
            NotificationService.INSTANCE.registerNotificationHandler(address, handler);
        }
        handlers.put(handlerID, handler);
    }

    private boolean unregisterNotificationHandler(String handlerID) {
        System.out.println(">>> NotificationApiHandler.unregisterNotificationHandler handlerID = [" + handlerID + "]");
        HttpNotificationHandler handler = handlers.remove(handlerID);
        if (handler != null) {
            for (PathAddress address : handler.getListeningAddresses()) {
                NotificationService.INSTANCE.unregisterNotificationHandler(address, handler);
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
        exchange.sendResponseHeaders(responseCode, 0);
        final PrintWriter out = new PrintWriter(exchange.getResponseBody());
        try {
            out.print(body);
            out.flush();
        } finally {
            safeClose(out);
        }
    }

    private class HttpNotificationHandler implements NotificationHandler {

        private final String handlerID;
        private final Set<PathAddress> addresses;
        private final List<ModelNode> notifications = new ArrayList<ModelNode>();

        public HttpNotificationHandler(String handlerID, Set<PathAddress> addresses) {
            this.handlerID = handlerID;
            this.addresses = addresses;
        }

        public Set<PathAddress> getListeningAddresses() {
            return addresses;
        }

        public List<ModelNode> getNotifications() {
            return notifications;
        }

        public void clear() {
            notifications.clear();
        }

        @Override
        public void handleNotification(ModelNode notification) {
            notifications.add(notification);
        }

        @Override
        public String toString() {
            return "HttpNotificationHandler[" +
                    "handlerID='" + handlerID + '\'' +
                    ", addresses=" + addresses +
                    "]@" + System.identityHashCode(this);
        }
    }

    public static void main(String[] args) {
        ModelNode node = new ModelNode();
        System.out.println("node = " + node.toJSONString(true));
        node.add("susbsystem", "messaging");
        System.out.println("node = " + node.toJSONString(true));

    }
}
