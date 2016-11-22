/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.messaging.mgmt;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STATISTICS_ENABLED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.UNDEFINE_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;
import static org.jboss.shrinkwrap.api.ArchivePaths.create;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.UUID;

import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.TemporaryQueue;
import javax.naming.Context;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.common.jms.JMSOperations;
import org.jboss.as.test.integration.common.jms.JMSOperationsProvider;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests the statistics for pooled-connection-factory.
 */
@RunAsClient
@RunWith(Arquillian.class)
public class PooledConnectionFactoryStatisticsTestCase {

    private static final String QUEUE_NAME = "PooledConnectionFactoryStatisticsTestCase-Queue";
    private static final String EXPORTED_QUEUE_NAME = "java:jboss/exported/PooledConnectionFactoryStatisticsTestCase-Queue";
    @ContainerResource
    private ManagementClient managementClient;

    @ContainerResource
    private Context context;

    @Deployment
    public static JavaArchive deploy() {
        return ShrinkWrap.create(JavaArchive.class, "PooledConnectionFactoryStatisticsTestCase.jar")
                .addClass(ConnectionHoldingBean.class)
                .addClass(RemoteConnectionHolding.class)
                .addClass(StatisticsMDB.class)
                .addAsManifestResource(EmptyAsset.INSTANCE,  create("beans.xml"));
    }

    @Before
    public void setUp() throws IOException {
        JMSOperations jmsOperations = JMSOperationsProvider.getInstance(managementClient.getControllerClient());
        jmsOperations.createJmsQueue(QUEUE_NAME, EXPORTED_QUEUE_NAME);
    }

    @After
    public void tearDown() throws IOException {
        JMSOperations jmsOperations = JMSOperationsProvider.getInstance(managementClient.getControllerClient());
        jmsOperations.removeJmsQueue(QUEUE_NAME);
    }

    @Test
    public void testStatistics() throws Exception {
        try {
            checkStatisticsAreDisabled();

            enableStatistics();
            assertEquals(0, readStatistic("InUseCount"));
            // check XA resources
            assertEquals(0, readStatistic("XACommitCount"));

            RemoteConnectionHolding bean = (RemoteConnectionHolding) context.lookup("PooledConnectionFactoryStatisticsTestCase/ConnectionHoldingBean!org.jboss.as.test.integration.messaging.mgmt.RemoteConnectionHolding");
            bean.createConnection();
            assertEquals(1, readStatistic("InUseCount"));
            // the createConnection method of the bean involves one XAResource: the creation of a JMS connection
            // involves the exchange of ActiveMQ Core packets in a transaction.
            assertEquals(1, readStatistic("XACommitCount"));


            bean.closeConnection();
            assertEquals(0, readStatistic("InUseCount"));
            // the closeConnection method of the bean does not involve a transaction
            assertEquals(1, readStatistic("XACommitCount"));

            // send a JMS message to a MDB and wait for its reply
            // we use the regular remote connection factory that is not related to the JmsXA pooled-connection-factory.
            sendAndReceiveMessage();
            // the delivery of the message in the MDB and its reply involves 2 XAResources from the JmsXA pool
            // (1 from the MDB, 1 from the JMSContext used to send the reply).
            assertEquals(3 , readStatistic("XACommitCount"));

        } finally {
            disableStatistics();
            checkStatisticsAreDisabled();
        }
    }

    private void sendAndReceiveMessage() throws NamingException {
        ConnectionFactory cf = (ConnectionFactory) context.lookup("jms/RemoteConnectionFactory");
        Destination destination = (Destination) context.lookup(QUEUE_NAME);
        try (JMSContext jmsContext = cf.createContext("guest", "guest")) {
            TemporaryQueue replyTo = jmsContext.createTemporaryQueue();
            JMSConsumer consumer = jmsContext.createConsumer(replyTo);
            String text = UUID.randomUUID().toString();
            jmsContext.createProducer()
                    .setJMSReplyTo(replyTo)
                    .send(destination, text);
            String reply = consumer.receiveBody(String.class, 1000);
            assertEquals(text, reply);
        }
    }


    ModelNode getPooledConnectionFactoryAddress() {
        ModelNode address = new ModelNode();
        address.add("subsystem", "messaging-activemq");
        address.add("server", "default");
        address.add("pooled-connection-factory", "activemq-ra");
        return address;
    }

    ModelNode getStatisticsAddress() {
        return getPooledConnectionFactoryAddress().add("statistics", "pool");
    }

    private void checkStatisticsAreDisabled() throws IOException {
        ModelNode op = new ModelNode();
        op.get(OP_ADDR).set(getStatisticsAddress());
        op.get(OP).set(READ_RESOURCE_OPERATION);
        execute(op, false);
    }

    private void enableStatistics() throws IOException {
        ModelNode op = new ModelNode();
        op.get(OP_ADDR).set(getPooledConnectionFactoryAddress());
        op.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
        op.get(NAME).set(STATISTICS_ENABLED);
        op.get(VALUE).set(true);
        execute(op, true);
        ServerReload.executeReloadAndWaitForCompletion(managementClient.getControllerClient());
    }

    private void disableStatistics() throws IOException {
        ModelNode op = new ModelNode();
        op.get(OP_ADDR).set(getPooledConnectionFactoryAddress());
        op.get(OP).set(UNDEFINE_ATTRIBUTE_OPERATION);
        op.get(NAME).set(STATISTICS_ENABLED);
        execute(op, true);
        ServerReload.executeReloadAndWaitForCompletion(managementClient.getControllerClient());
    }

    private int readStatistic(String name) throws IOException {
        ModelNode op = new ModelNode();
        op.get(OP_ADDR).set(getStatisticsAddress());
        op.get(OP).set(READ_ATTRIBUTE_OPERATION);
        op.get(NAME).set(name);
        ModelNode result = execute(op, true);
        return result.asInt();
    }

    private ModelNode execute(final ModelNode op, final boolean expectSuccess) throws IOException {
        ModelNode response = managementClient.getControllerClient().execute(op);
        final String outcome = response.get("outcome").asString();
        if (expectSuccess) {
            if (!"success".equals(outcome)) {
                System.out.println(response);
            }
            assertEquals("success", outcome);
            return response.get("result");
        } else {
            if ("success".equals(outcome)) {
                System.out.println(response);
            }
            assertEquals("failed", outcome);
            return response.get("failure-description");
        }
    }
}
