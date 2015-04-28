/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.messaging.activemq.jms;

import java.util.List;

import org.apache.activemq.api.core.management.ResourceNames;
import org.apache.activemq.api.jms.management.JMSServerControl;
import org.apache.activemq.core.server.ActiveMQServer;
import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.wildfly.extension.messaging.activemq.CommonAttributes;
import org.wildfly.extension.messaging.activemq.MessagingServices;

/**
 * Update handler removing a connection factory from the JMS subsystem. The
 * runtime action will remove the corresponding {@link ConnectionFactoryService}.
 *
 * @author Emanuel Muckenhuber
 * @author <a href="mailto:andy.taylor@jboss.com">Andy Taylor</a>
 */
public class ConnectionFactoryRemove extends AbstractRemoveStepHandler {

    public static final ConnectionFactoryRemove INSTANCE = new ConnectionFactoryRemove();

    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        final String name = context.getCurrentAddressValue();
        final ServiceName hqServiceName = MessagingServices.getActiveMQServiceName(context.getCurrentAddress());
        context.removeService(JMSServices.getConnectionFactoryBaseServiceName(hqServiceName).append(name));

        ServiceController<?> hqService = context.getServiceRegistry(false).getService(hqServiceName);
        ActiveMQServer hqServer = ActiveMQServer.class.cast(hqService.getValue());
        JMSServerControl control = JMSServerControl.class.cast(hqServer.getManagementService().getResource(ResourceNames.JMS_SERVER));
        if (control != null) {
            try {
                control.destroyConnectionFactory(name);
            } catch (Exception e) {
                throw new OperationFailedException(e);
            }
        }

        List<String> legacyEntries = CommonAttributes.LEGACY_ENTRIES.unwrap(context, model);
        if (!legacyEntries.isEmpty()) {
            context.removeService(JMSServices.getConnectionFactoryBaseServiceName(hqServiceName).append(name, CommonAttributes.LEGACY));
            for (String legacyEntry : legacyEntries) {
                final ContextNames.BindInfo bindInfo = ContextNames.bindInfoFor(legacyEntry);
                ServiceName binderServiceName = bindInfo.getBinderServiceName();
                context.removeService(binderServiceName);
            }
        }

    }

    protected void recoverServices(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        ConnectionFactoryAdd.INSTANCE.performRuntime(context, operation, model);
    }
}
