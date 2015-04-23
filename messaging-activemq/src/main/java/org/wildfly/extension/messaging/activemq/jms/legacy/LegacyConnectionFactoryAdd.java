/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.messaging.activemq.jms.legacy;

import java.util.List;

import javax.jms.ConnectionFactory;

import org.hornetq.api.jms.HornetQJMSClient;
import org.hornetq.api.jms.JMSFactoryType;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.wildfly.extension.messaging.activemq.BinderServiceUtil;
import org.wildfly.extension.messaging.activemq.CommonAttributes;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2015 Red Hat inc.
 */
public class LegacyConnectionFactoryAdd  extends AbstractAddStepHandler {

    public static final LegacyConnectionFactoryAdd INSTANCE = new LegacyConnectionFactoryAdd();

    private LegacyConnectionFactoryAdd() {
        super(LegacyConnectionFactoryDefinition.ATTRIBUTES);
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        String name = context.getCurrentAddressValue();
        List<String> entries = LegacyConnectionFactoryDefinition.LEGACY_ENTRIES.unwrap(context, model);

        boolean ha = CommonAttributes.HA.resolveModelAttribute(context, model).asBoolean();
        final ConnectionFactory connectionFactory;
        if (ha) {
            connectionFactory = HornetQJMSClient.createConnectionFactoryWithHA(JMSFactoryType.CF);
        } else {
            connectionFactory = HornetQJMSClient.createConnectionFactoryWithoutHA(JMSFactoryType.CF);
        }

        for (String entry : entries) {
            BinderServiceUtil.installBinderService(context.getServiceTarget(), entry, connectionFactory);
        }

    }
}
