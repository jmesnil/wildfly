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

package org.jboss.as.test.integration.messaging.mgmt.startstop;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.UUID;

import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.JMSDestinationDefinition;
import javax.jms.JMSSessionMode;
import javax.jms.Queue;
import javax.jms.Topic;


/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2013 Red Hat inc.
 */

@JMSDestinationDefinition(
        name="java:comp/env/myQueue",
        interfaceName="javax.jms.Queue"
)

@Stateless
public class MessagingBean {

    @Inject
    private JMSContext context;

    @Resource(lookup = "java:comp/env/myQueue")
    private Queue queue;

    public void sendMessage(String text) {
        System.out.println("context = " + context);
        System.out.println("topic = " + queue);

        context.createProducer().send(queue, text);
    }

    public String receiveResponse() {
        try( JMSConsumer consumer = context.createConsumer(queue) ) {
            String response = consumer.receiveBody(String.class, 500);
            System.out.println("response = " + response);
            return response;
        }
    }
}
