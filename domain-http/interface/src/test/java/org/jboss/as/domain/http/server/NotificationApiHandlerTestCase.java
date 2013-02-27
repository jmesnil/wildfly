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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.jboss.modules.LocalModuleLoader;
import org.jboss.modules.ModuleClassLoader;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoader;
import org.junit.Test;


/**
 *
 * @author <a href="http://jmesnil.net">Jeff Mesnil</a> (c) 2013 Red Hat, inc
 */
public class NotificationApiHandlerTestCase {

    @Test
    public void testRollingNotifications() {
        String handlerID = UUID.randomUUID().toString();
        Set<PathAddress> addresses = new HashSet<PathAddress>();
        NotificationApiHandler.HttpNotificationHandler handler = new NotificationApiHandler.HttpNotificationHandler(handlerID, addresses);
        assertTrue(handler.getNotifications().isEmpty());

        // fill the handler
        ModelNode notification = new ModelNode();
        String key = "n";
        int i = 0;
        for (; i < NotificationApiHandler.MAX_NOTIFICATIONS; i++) {
            notification.get(key).set(i);
            handler.handleNotification(notification);
            assertEquals(i, handler.getNotifications().get(i).get(key).asInt());
        }
        assertEquals(NotificationApiHandler.MAX_NOTIFICATIONS, handler.getNotifications().size());
        assertEquals(0, handler.getNotifications().get(0).get(key).asInt());
        assertEquals(i - 1, handler.getNotifications().get(handler.getNotifications().size() - 1).get(key).asInt());

        // one more notification
        notification.get(key).set(i++);
        handler.handleNotification(notification);

        assertEquals(NotificationApiHandler.MAX_NOTIFICATIONS, handler.getNotifications().size());
        // the oldest element has been ditched...
        assertEquals(1, handler.getNotifications().get(0).get(key).asInt());
        // ... and the most recent element has been added.
        assertEquals(i - 1, handler.getNotifications().get(handler.getNotifications().size() - 1).get(key).asInt());

        handler.clear();

        assertTrue(handler.getNotifications().isEmpty());
    }
}
