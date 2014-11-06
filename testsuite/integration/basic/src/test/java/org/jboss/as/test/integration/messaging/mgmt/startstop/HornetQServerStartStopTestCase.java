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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.HashMap;
import java.util.UUID;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TemporaryQueue;
import javax.jms.TextMessage;

import org.hornetq.api.core.TransportConfiguration;
import org.hornetq.api.jms.HornetQJMSClient;
import org.hornetq.api.jms.JMSFactoryType;
import org.hornetq.core.remoting.impl.netty.NettyConnectorFactory;
import org.hornetq.core.remoting.impl.netty.TransportConstants;
import org.hornetq.jms.client.HornetQConnectionFactory;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.dmr.ModelNode;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2013 Red Hat inc.
 */
@RunWith(Arquillian.class)
@RunAsClient
public class HornetQServerStartStopTestCase {

    private static final ModelNode hornetQServerAddress;

    static {
        hornetQServerAddress = new ModelNode();
        hornetQServerAddress.add("subsystem", "messaging");
        hornetQServerAddress.add("hornetq-server", "default");
    }

    @ContainerResource
    private ManagementClient managementClient;

    private static HornetQConnectionFactory connectionFactory;

    @BeforeClass
    public static void beforeClass() throws Exception {
        HashMap<String, Object> map = new HashMap<String, Object>();
        map.put("host", TestSuiteEnvironment.getServerAddress());
        map.put("port", 8080);
        map.put(TransportConstants.HTTP_UPGRADE_ENABLED_PROP_NAME, true);
        map.put(TransportConstants.HTTP_UPGRADE_ENDPOINT_PROP_NAME, "http-acceptor");
        TransportConfiguration transportConfiguration =
                new TransportConfiguration(NettyConnectorFactory.class.getName(), map);
        connectionFactory = HornetQJMSClient.createConnectionFactoryWithoutHA(JMSFactoryType.CF, transportConfiguration);
        connectionFactory.setBlockOnDurableSend(true);
        connectionFactory.setBlockOnNonDurableSend(true);
    }

    @AfterClass
    public static void afterClass() throws Exception {

        if (connectionFactory != null) {
            connectionFactory.close();
        }
    }
    private boolean execute(ManagementClient managementClient, final ModelNode address, final String operationName) throws IOException {
        ModelNode operation = new ModelNode();
        operation.get(OP_ADDR).set(address);
        operation.get(OP).set(operationName);
        ModelNode response = managementClient.getControllerClient().execute(operation);
        final String outcome = response.get(OUTCOME).asString();
        return ModelDescriptionConstants.SUCCESS.equals(outcome);
    }

    @Test
    public void testForceFailoverAndStart() throws Exception {

        sendAndReceiveMessage();

        assertFalse(execute(managementClient, hornetQServerAddress, "force-failover"));

        try {
            sendAndReceiveMessage();
            fail("HornetQ server must be stopped after it was forced to fail over");
        } catch (JMSException e) {

        }

        assertTrue(execute(managementClient, hornetQServerAddress, "start"));

        sendAndReceiveMessage();
    }

    private void sendAndReceiveMessage() throws JMSException {
        try( Connection connection = connectionFactory.createConnection("guest", "guest") ) {
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            TemporaryQueue temporaryQueue = session.createTemporaryQueue();
            MessageConsumer consumer = session.createConsumer(temporaryQueue);
            connection.start();

            MessageProducer producer = session.createProducer(temporaryQueue);
            String text = UUID.randomUUID().toString();
            producer.send(session.createTextMessage(text));

            Message reply = consumer.receive(2000);
            assertNotNull(reply);
            assertTrue(reply instanceof TextMessage);
            assertEquals(text, ((TextMessage) reply).getText());
        }
    }
}
