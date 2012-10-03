/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.messaging;

import static java.util.Collections.emptySet;
import static org.jboss.as.messaging.MessagingMessages.MESSAGES;

import java.util.Set;

import org.hornetq.api.core.management.QueueControl;
import org.hornetq.api.core.management.ResourceNames;
import org.hornetq.core.server.management.ManagementService;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;

/**
 * Resource for a runtime core HornetQ queue.
 *
 * These resources can be created as a side-effect of the creation of JMS resources.
 *
 * * 1 JMS queue will create underneath 1 core queue
 * * 1 JMS topic will create underneath 1 core queue for each subscriber.
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2012 Red Hat Inc.
 */
public class QueueResource implements Resource {

    private final String name;
    final ManagementService managementService;

    public QueueResource(final String name, final ManagementService managementService) {
        this.name = name;
        this.managementService = managementService;
    }

    @Override
    public ModelNode getModel() {
        System.out.println("QueueResource.getModel()");
        ModelNode model = new ModelNode();
        Object obj = managementService.getResource(ResourceNames.CORE_QUEUE + name);
        QueueControl control = QueueControl.class.cast(obj);
        model.get("queue-address").set(control.getAddress());
        ModelNode filterNode = model.get("filter");
        String filter = control.getFilter();
        if (filter != null) {
            filterNode.set(filter);
        }
        model.get("durable").set(control.isDurable());
        return model;
    }

    @Override
    public void writeModel(ModelNode newModel) {
        System.out.println("QueueResource.writeModel()");
        throw MESSAGES.immutableResource();
    }

    @Override
    public boolean isModelDefined() {
        return true;
    }

    @Override
    public boolean hasChild(PathElement element) {
        return false;
    }

    @Override
    public Resource getChild(PathElement element) {
        return null;
    }

    @Override
    public Resource requireChild(PathElement element) {
        throw new NoSuchResourceException(element);
    }

    @Override
    public boolean hasChildren(String childType) {
        return false;
    }

    @Override
    public Resource navigate(PathAddress address) {
        return Resource.Tools.navigate(this, address);
    }

    @Override
    public Set<String> getChildTypes() {
        return emptySet();
    }

    @Override
    public Set<String> getChildrenNames(String childType) {
        return emptySet();
    }

    @Override
    public Set<ResourceEntry> getChildren(String childType) {
        return emptySet();
    }

    @Override
    public void registerChild(PathElement address, Resource resource) {
        throw MESSAGES.immutableResource();
    }

    @Override
    public Resource removeChild(PathElement address) {
        throw MESSAGES.immutableResource();
    }

    @Override
    public boolean isRuntime() {
        return false;
    }

    @Override
    public boolean isProxy() {
        return false;
    }

    @Override
    public Resource clone() {
        return new QueueResourceEntry(name, managementService);
    }

    public static class QueueResourceEntry extends QueueResource implements ResourceEntry {

        final PathElement path;

        public QueueResourceEntry(final String name, final ManagementService managementService) {
            super(name, managementService);
            path = PathElement.pathElement(CommonAttributes.QUEUE, name);
        }


        @Override
        public String getName() {
            return path.getValue();
        }

        @Override
        public PathElement getPathElement() {
            return path;
        }

        @Override
        public QueueResourceEntry clone() {
            return new QueueResourceEntry(path.getValue(), managementService);
        }

        @Override
        public String toString() {
            return "QueueResourceEntry[path=" + path + "]@" + System.identityHashCode(this);
        }
    }
}
