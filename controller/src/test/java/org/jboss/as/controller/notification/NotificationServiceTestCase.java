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

import static org.jboss.as.controller.PathAddress.pathAddress;
import static org.jboss.as.controller.PathElement.pathElement;
import static org.jboss.as.controller.notification.NotificationService.matches;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.jboss.as.controller.PathAddress;
import org.junit.Test;

/**
 * Tests for NotificationService.
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2013 Red Hat Inc.
 */
public class NotificationServiceTestCase {

    @Test
    public void testPathAddressMatches() {
        PathAddress address = pathAddress(pathElement("subsystem", "messaging"), pathElement("hornetq-server", "default"));
        assertTrue(matches(address, address));

        PathAddress parent = address.subAddress(0, address.size() - 1);
        assertFalse(matches(address, parent));

        PathAddress child = address.append("jms-queue", "myQueue");
        assertFalse(matches(address, child));

        PathAddress pattern = pathAddress(pathElement("subsystem", "messaging"), pathElement("hornetq-server", "*"));
        assertTrue(matches(address, pattern));
        PathAddress pattern2 = pathAddress(pathElement("subsystem", "*"), pathElement("hornetq-server", "default"));
        assertTrue(matches(address, pattern2));
        PathAddress pattern3 = pathAddress(pathElement("subsystem", "*"), pathElement("hornetq-server", "*"));
        assertTrue(matches(address, pattern3));
    }
}
