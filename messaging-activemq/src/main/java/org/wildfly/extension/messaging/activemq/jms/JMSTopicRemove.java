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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import org.apache.activemq.artemis.api.core.management.ResourceNames;
import org.apache.activemq.artemis.api.jms.management.JMSServerControl;
import org.apache.activemq.artemis.core.server.ActiveMQServer;
import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.wildfly.extension.messaging.activemq.CommonAttributes;
import org.wildfly.extension.messaging.activemq.MessagingServices;

/**
 * Update handler removing a topic from the JMS subsystem. The
 * runtime action will remove the corresponding {@link JMSTopicService}.
 *
 * @author Emanuel Muckenhuber
 * @author <a href="mailto:andy.taylor@jboss.com">Andy Taylor</a>
 */
public class JMSTopicRemove extends AbstractRemoveStepHandler {

    public static final JMSTopicRemove INSTANCE = new JMSTopicRemove();

    private JMSTopicRemove() {
    }

    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        final ServiceName hqServiceName = MessagingServices.getActiveMQServiceName(PathAddress.pathAddress(operation.get(ModelDescriptionConstants.OP_ADDR)));
        final PathAddress address = PathAddress.pathAddress(operation.require(OP_ADDR));
        final String name = address.getLastElement().getValue();

        ServiceController<?> hqService = context.getServiceRegistry(false).getService(hqServiceName);
        ActiveMQServer hqServer = ActiveMQServer.class.cast(hqService.getValue());
        JMSServerControl control = JMSServerControl.class.cast(hqServer.getManagementService().getResource(ResourceNames.JMS_SERVER));
        if (control != null) {
            try {
                control.destroyTopic(name, true);
            } catch (Exception e) {
                throw new OperationFailedException(e);
            }
        }

        context.removeService(JMSServices.getJmsTopicBaseServiceName(hqServiceName).append(name));

        final ModelNode entries = CommonAttributes.DESTINATION_ENTRIES.resolveModelAttribute(context, model);
        final String[] jndiBindings = JMSServices.getJndiBindings(entries);
        for (String jndiBinding : jndiBindings) {
            final ContextNames.BindInfo bindInfo = ContextNames.bindInfoFor(jndiBinding);
            ServiceName binderServiceName = bindInfo.getBinderServiceName();
            context.removeService(binderServiceName);
        }
    }

    protected void recoverServices(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        JMSTopicAdd.INSTANCE.performRuntime(context, operation, model);
    }
}