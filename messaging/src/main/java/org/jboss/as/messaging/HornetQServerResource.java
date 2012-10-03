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

import static org.jboss.as.messaging.CommonAttributes.CORE_ADDRESS;
import static org.jboss.as.messaging.CommonAttributes.QUEUE;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.hornetq.api.core.management.AddressControl;
import org.hornetq.api.core.management.QueueControl;
import org.hornetq.api.core.management.ResourceNames;
import org.hornetq.core.server.HornetQServer;
import org.hornetq.core.server.management.ManagementService;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;

/**
 * Resource representing a HornetQ server.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class HornetQServerResource implements Resource {

    private final Resource delegate;
    private ServiceController<HornetQServer> hornetQServerServiceController;

    public HornetQServerResource() {
        this(Resource.Factory.create());
    }

    public HornetQServerResource(final Resource delegate) {
        this.delegate = delegate;
    }

    public ServiceController<HornetQServer> getHornetQServerServiceController() {
        return hornetQServerServiceController;
    }

    public void setHornetQServerServiceController(ServiceController<HornetQServer> hornetQServerServiceController) {
        this.hornetQServerServiceController = hornetQServerServiceController;
    }

    @Override
    public ModelNode getModel() {
        return delegate.getModel();
    }

    @Override
    public void writeModel(ModelNode newModel) {
        delegate.writeModel(newModel);
    }

    @Override
    public boolean isModelDefined() {
        return delegate.isModelDefined();
    }

    @Override
    public boolean hasChild(PathElement element) {
        if (CORE_ADDRESS.equals(element.getKey())) {
            return hasAddressControl(element);
        } else if (QUEUE.equals(element.getKey())) {
            System.out.println("HornetQServerResource.hasChild() " + element);
            if (delegate.hasChild(element)) {
                return true;
            } else {
                return hasCoreQueueControl(element);
            }
        } else {
            return delegate.hasChild(element);
        }
    }

    @Override
    public Resource getChild(PathElement element) {
        System.out.println("HornetQServerResource.getChild()" + element);
        if (CORE_ADDRESS.equals(element.getKey())) {
            return hasAddressControl(element) ? new CoreAddressResource(element.getValue(), getManagementService()) : null;
        } else if (QUEUE.equals(element.getKey())) {
            System.out.println("HornetQServerResource.getChild()");
            // check whether there is a configure core queue
            if (delegate.hasChild(element)) {
                System.out.println("got real core queue " + element);
                return delegate.getChild(element);
            } else {
                if (hasCoreQueueControl(element)) {
                    System.out.println("got underlying core queue " + element);
                    return new QueueResource(element.getValue(), getManagementService());
                } else {
                    System.out.println("no core queue " + element);
                    return null;
                }
            }
        } else {
            return delegate.getChild(element);
        }
    }

    @Override
    public Resource requireChild(PathElement element) {
        if (CORE_ADDRESS.equals(element.getKey())) {
            if (hasAddressControl(element)) {
                return new CoreAddressResource(element.getValue(), getManagementService());
            }
            throw new NoSuchResourceException(element);
        } else if (QUEUE.equals(element.getKey())) {
            if (delegate.hasChild(element)) {
                return delegate.requireChild(element);
            } else {
                if (hasCoreQueueControl(element)) {
                    return new QueueResource(element.getValue(), getManagementService());
                }
                throw new NoSuchResourceException(element);
            }
        } else {
            return delegate.requireChild(element);
        }
    }

    @Override
    public boolean hasChildren(String childType) {
        System.out.println("HornetQServerResource.hasChildren()");
        if (CORE_ADDRESS.equals(childType)) {
            return getChildrenNames(CORE_ADDRESS).size() > 0;
        } else if (QUEUE.equals(childType)) {
            return getChildrenNames(QUEUE).size() > 0;
        } else {
            return delegate.hasChildren(childType);
        }
    }

    @Override
    public Resource navigate(PathAddress address) {
        System.out.println("HornetQServerResource.navigate() " + address);
        if (address.size() > 0 && CORE_ADDRESS.equals(address.getElement(0).getKey())) {
            if (address.size() > 1) {
                throw new NoSuchResourceException(address.getElement(1));
            }
            return new CoreAddressResource(address.getElement(0).getValue(), getManagementService());
        } else if (address.size() > 0 && QUEUE.equals(address.getElement(0).getKey())) {
            if (address.size() > 1) {
                throw new NoSuchResourceException(address.getElement(1));
            }
            if (delegate.hasChild(address.getElement(0))) {
                return delegate.navigate(address);
            } else {
                return new QueueResource(address.getElement(0).getValue(), getManagementService());
            }
        } else {
            return delegate.navigate(address);
        }
    }

    @Override
    public Set<String> getChildTypes() {
        Set<String> result = new HashSet<String>(delegate.getChildTypes());
        result.add(CORE_ADDRESS);
        result.add(QUEUE);
        return result;
    }

    @Override
    public Set<String> getChildrenNames(String childType) {
        if (CORE_ADDRESS.equals(childType)) {
            return getCoreAddressNames();
        } else if (QUEUE.equals(childType)) {
            return getCoreQueueNames();
        } else {
            return delegate.getChildrenNames(childType);
        }
    }

    @Override
    public Set<ResourceEntry> getChildren(String childType) {
        if (CORE_ADDRESS.equals(childType)) {
            Set<ResourceEntry> result = new HashSet<ResourceEntry>();
            for (String name : getCoreAddressNames()) {
                result.add(new CoreAddressResource.CoreAddressResourceEntry(name, getManagementService()));
            }
            return result;
        } else if (QUEUE.equals(childType)) {
            Set<ResourceEntry> result = new LinkedHashSet<Resource.ResourceEntry>(delegate.getChildren(QUEUE));
            Set<String> queueNames = new HashSet<String>();
            for (ResourceEntry resourceEntry : result) {
                queueNames.add(resourceEntry.getName());
            }
            System.out.println("childre from the delegate " + queueNames);
            System.out.println("childre from the controls " + getCoreQueueNames());
            for (String name : getCoreQueueNames()) {
                if (!queueNames.contains(name)) {
                    result.add(new QueueResource.QueueResourceEntry(name, getManagementService()));
                }
            }
            System.out.println("children = " + result);
            return result;
        } else {
            return delegate.getChildren(childType);
        }
    }

    @Override
    public void registerChild(PathElement address, Resource resource) {
        if (CORE_ADDRESS.equals(address.getKey())) {
            throw new UnsupportedOperationException(String.format("Resources of type %s cannot be registered", CORE_ADDRESS));
        } else {
            delegate.registerChild(address, resource);
        }
    }

    @Override
    public Resource removeChild(PathElement address) {
        if (CORE_ADDRESS.equals(address.getKey())) {
            throw new UnsupportedOperationException(String.format("Resources of type %s cannot be removed", CORE_ADDRESS));
        } else {
            return delegate.removeChild(address);
        }
    }

    @Override
    public boolean isRuntime() {
        return delegate.isRuntime();
    }

    @Override
    public boolean isProxy() {
        return delegate.isProxy();
    }

    @Override
    public Resource clone() {
        HornetQServerResource clone = new HornetQServerResource(delegate.clone());
        clone.setHornetQServerServiceController(hornetQServerServiceController);
        return clone;
    }

    private boolean hasAddressControl(PathElement element) {
        final ManagementService managementService = getManagementService();
        return managementService == null ? false : managementService.getResource(ResourceNames.CORE_ADDRESS + element.getValue()) != null;
    }

    private boolean hasCoreQueueControl(PathElement element) {
        System.out.println("HornetQServerResource.hasCoreQueueControl() " + element);
        if (element.getValue().startsWith("jms.")) {
            return false;
        }
        final ManagementService managementService = getManagementService();
        return managementService == null ? false : managementService.getResource(ResourceNames.CORE_QUEUE + element.getValue()) != null;
    }

    private Set<String> getCoreAddressNames() {
        final ManagementService managementService = getManagementService();
        if (managementService == null) {
            return Collections.emptySet();
        } else {
            Set<String> result = new HashSet<String>();
            for (Object obj : managementService.getResources(AddressControl.class)) {
                AddressControl ac = AddressControl.class.cast(obj);
                result.add(ac.getAddress());
            }
            return result;
        }
    }

    /**
     * @return the name of core queues <em>that are not related to JMS resources</em>.
     */
    private Set<String> getCoreQueueNames() {
        System.out.println("HornetQServerResource.getCoreQueueNames()");
        final ManagementService managementService = getManagementService();
        if (managementService == null) {
            return Collections.emptySet();
        } else {
            Set<String> result = new HashSet<String>();
            for (Object obj : managementService.getResources(QueueControl.class)) {
                QueueControl qc = QueueControl.class.cast(obj);
                String queueName = qc.getName();
                if (!queueName.startsWith("jms.")) {
                    result.add(queueName);
                }
            }
            return result;
        }
    }

    private ManagementService getManagementService() {
        if (hornetQServerServiceController == null
                || hornetQServerServiceController.getState() != ServiceController.State.UP) {
            return null;
        } else {
            return hornetQServerServiceController.getValue().getManagementService();
        }
    }
}
