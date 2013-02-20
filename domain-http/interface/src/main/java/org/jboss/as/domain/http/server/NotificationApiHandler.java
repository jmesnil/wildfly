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

import static org.jboss.as.domain.http.server.Constants.CREATED;
import static org.jboss.as.domain.http.server.Constants.GET;
import static org.jboss.as.domain.http.server.Constants.INTERNAL_SERVER_ERROR;
import static org.jboss.as.domain.http.server.Constants.LOCATION;
import static org.jboss.as.domain.http.server.Constants.METHOD_NOT_ALLOWED;
import static org.jboss.as.domain.http.server.Constants.NOT_FOUND;
import static org.jboss.as.domain.http.server.Constants.NO_CONTENT;
import static org.jboss.as.domain.http.server.Constants.OK;
import static org.jboss.as.domain.http.server.Constants.ORIGIN;
import static org.jboss.as.domain.http.server.Constants.POST;
import static org.jboss.as.domain.http.server.DomainUtil.safeClose;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.ModelControllerClient;
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

    private static final String NOTIFICATION_API_CONTEXT = "/notification";
    public static final String HANDLER_PREFIX = "handler";
    public static final String DELETE = "DELETE";

    private final ModelControllerClient modelController;
    private final Authenticator authenticator;
    private final AtomicLong handlerCounter;

    private final Map<String, Set<PathAddress>> handlers = new HashMap<String, Set<PathAddress>>();
    private final Map<String, List<ModelNode>> notifications = new HashMap<String, List<ModelNode>>();

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
        notifications.clear();
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
        System.out.println("requestURI = " + requestURI.getPath());
        if (requestURI.getPath().equals(NOTIFICATION_API_CONTEXT)) {
            if (method.equals(POST)) {
                String handlerID = HANDLER_PREFIX + handlerCounter.incrementAndGet();
                ModelNode operation = ModelNode.fromJSONStream(http.getRequestBody());
                // create the listener based on the address in the operation
                registerNotificationHandler(handlerID, operation);
                http.getResponseHeaders().add(LOCATION, NOTIFICATION_API_CONTEXT + "/" + handlerID);
                sendResponse(http, CREATED);
            }
        } else {
            final String handlerID = requestURI.getPath().substring((NOTIFICATION_API_CONTEXT + "/").length());
            if (POST.equals(method)) {
                ModelNode notifications = fetchNotifications(handlerID);
                sendResponse(http, OK, notifications.toJSONString(true));
            } else if (DELETE.equals(method)) {
                unregisterNotificationHandler(handlerID);
                sendResponse(http, NO_CONTENT);
            } else if (GET.equals(method)) {
                ModelNode addresses = getAddressesListeningTo(handlerID);
                sendResponse(http, OK, addresses.toJSONString(true));
            } else {
                sendResponse(http, METHOD_NOT_ALLOWED);
            }
            return;
        }
        sendResponse(http, NOT_FOUND);
    }

    private ModelNode getAddressesListeningTo(String handlerID) {
        System.out.println(">>> NotificationApiHandler.getAddressesListeningTo handlerID = [" + handlerID + "]");
        final ModelNode node = new ModelNode();
        if (handlers.containsKey(handlerID)) {
            for (PathAddress address : handlers.get(handlerID)) {
                node.add(address.toModelNode());
            }
        }
        return node;
    }

    private ModelNode fetchNotifications(String handlerID) {
        System.out.println(">>> NotificationApiHandler.fetchNotifications handlerID = [" + handlerID + "]");
        final ModelNode node = new ModelNode();
        if (notifications.containsKey(handlerID)) {
            for (ModelNode notification : notifications.remove(handlerID)) {
                node.add(notification);
            }
        }
        return node;
    }

    private void registerNotificationHandler(String handlerID, ModelNode operation) {
        System.out.println("NotificationApiHandler.registerNotificationHandler handlerID = [" + handlerID + "], operation = [" + operation + "]");
        Set<PathAddress> resourceAddresses = new HashSet<PathAddress>();
        for (ModelNode resource : operation.get("resources").asList()) {
            resourceAddresses.add(PathAddress.pathAddress(resource));
        }
        handlers.put(handlerID, resourceAddresses);
    }

    private void unregisterNotificationHandler(String handlerID) {
        System.out.println(">>> NotificationApiHandler.unregisterNotificationHandler handlerID = [" + handlerID + "]");
        notifications.remove(handlerID);
        handlers.remove(handlerID);
    }

    private void sendResponse(final HttpExchange exchange, final int responseCode) throws IOException {
        sendResponse(exchange, responseCode, null);
    }

    private void sendResponse(final HttpExchange exchange, final int responseCode, final String body) throws IOException {
        exchange.sendResponseHeaders(responseCode, 0);
        final PrintWriter out = new PrintWriter(exchange.getResponseBody());
        try {
            out.print(body);
            out.flush();
        } finally {
            safeClose(out);
        }
    }
}
