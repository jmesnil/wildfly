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
package org.jboss.as.test.smoke.messaging;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.jboss.as.controller.client.helpers.ClientConstants.OP;
import static org.jboss.as.controller.client.helpers.ClientConstants.OP_ADDR;
import static org.jboss.as.controller.client.helpers.ClientConstants.OUTCOME;
import static org.jboss.as.controller.client.helpers.ClientConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.client.helpers.ClientConstants.SUCCESS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import org.hornetq.api.core.HornetQException;
import org.hornetq.api.core.TransportConfiguration;
import org.hornetq.api.core.client.ClientConsumer;
import org.hornetq.api.core.client.ClientMessage;
import org.hornetq.api.core.client.ClientProducer;
import org.hornetq.api.core.client.ClientSession;
import org.hornetq.api.core.client.ClientSessionFactory;
import org.hornetq.api.core.client.HornetQClient;
import org.hornetq.api.core.client.MessageHandler;
import org.hornetq.core.remoting.impl.invm.InVMConnectorFactory;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * [TODO]
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
@RunWith(Arquillian.class)
public class HornetQMessagingTestCase {
    private static final String QUEUE_EXAMPLE_QUEUE = "queue.exampleQueue";

    static final Logger log = Logger.getLogger(HornetQMessagingTestCase.class);

    private static final String BODY = "msg.body";

    private ClientSessionFactory sf;
    private ClientSession session;


    @ArquillianResource
    private ManagementClient managementClient;

    @Deployment
    public static JavaArchive createDeployment() throws Exception {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "messaging-example.jar");
        jar.addAsManifestResource(new StringAsset("Manifest-Version: 1.0\n" +
                "Dependencies: org.hornetq, org.jboss.dmr, org.jboss.as.controller-client\n"), "MANIFEST.MF");
        jar.addClass(HornetQMessagingTestCase.class);
        return jar;
    }

    @Before
    public void start() throws Exception {
        System.out.println("managementClient = " + managementClient);

        Assume.assumeTrue("Test is relevant only when the messaging subsystem with HornetQ is setup", isHornetQServerFound());

        //Not using JNDI so we use the core services directly
        sf = HornetQClient.createServerLocatorWithoutHA(new TransportConfiguration(InVMConnectorFactory.class.getName())).createSessionFactory();
        session = sf.createSession();

        //Create a queue
        ClientSession coreSession = sf.createSession();
        coreSession.createQueue(QUEUE_EXAMPLE_QUEUE, QUEUE_EXAMPLE_QUEUE, false);
        coreSession.close();

        session = sf.createSession();
        session.start();
    }

    @After
    public void stop() throws Exception {
        if (session != null) {
            session.close();
        }

        if (sf != null) {
            ClientSession coreSession = sf.createSession();
            coreSession.deleteQueue(QUEUE_EXAMPLE_QUEUE);
            coreSession.close();

            sf.close();
        }
    }

    @Test
    public void testMessaging() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<ClientMessage> message = new AtomicReference<ClientMessage>();

        ClientConsumer consumer = session.createConsumer(QUEUE_EXAMPLE_QUEUE);
        consumer.setMessageHandler(new MessageHandler() {
            @Override
            public void onMessage(ClientMessage m) {
                try {
                    m.acknowledge();
                    message.set(m);
                    latch.countDown();
                } catch (HornetQException e) {
                    e.printStackTrace();
                }
            }
        });

        String text = UUID.randomUUID().toString();

        sendMessage(text);

        assertTrue(latch.await(1, SECONDS));
        assertEquals(text, message.get().getStringProperty(BODY));
    }

    private void sendMessage(String text) throws Exception {
        ClientProducer producer = session.createProducer(QUEUE_EXAMPLE_QUEUE);
        ClientMessage message = session.createMessage(false);

        message.putStringProperty(BODY, text);
        log.info("-----> Sending message");
        producer.send(message);
    }

    private boolean isHornetQServerFound() throws IOException {
        ModelNode readHornetQServer = new ModelNode();
        readHornetQServer.get(OP).set(READ_RESOURCE_OPERATION);
        readHornetQServer.get(OP_ADDR).add("subsystem", "messaging");
        readHornetQServer.get(OP_ADDR).add("hornetq-server", "default");
        ModelNode result = managementClient.getControllerClient().execute(readHornetQServer);
        System.out.println("result = " + result);
        return  result.hasDefined(OUTCOME) && SUCCESS.equals(result.get(OUTCOME).asString());
    }
}
