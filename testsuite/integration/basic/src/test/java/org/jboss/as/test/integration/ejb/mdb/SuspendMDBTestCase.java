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

package org.jboss.as.test.integration.ejb.mdb;

import static org.jboss.as.test.shared.integration.ejb.security.PermissionUtils.createPermissionsXmlAsset;

import java.util.PropertyPermission;

import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.naming.Context;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.test.integration.common.jms.JMSOperations;
import org.jboss.as.test.integration.common.jms.JMSOperationsProvider;
import org.jboss.dmr.ModelNode;
import org.jboss.remoting3.security.RemotingPermission;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests MDB deployments
 *
 * User: Jaikiran Pai
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup({SuspendMDBTestCase.JmsQueueSetup.class})
public class SuspendMDBTestCase {

    @ContainerResource
    private Context remoteContext;

    @ContainerResource
    private ManagementClient managementClient;


    static class JmsQueueSetup implements ServerSetupTask {

        private JMSOperations jmsAdminOperations;

        @Override
        public void setup(ManagementClient managementClient, String containerId) throws Exception {
            jmsAdminOperations = JMSOperationsProvider.getInstance(managementClient.getControllerClient());
            jmsAdminOperations.createJmsQueue("mdbtest/queue", "java:jboss/mdbtest/queue", "java:jboss/exported/mdbtest/queue");
            jmsAdminOperations.createJmsQueue("mdbtest/replyQueue", "java:jboss/mdbtest/replyQueue", "java:jboss/exported/mdbtest/replyQueue");
        }

        @Override
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            if (jmsAdminOperations != null) {
                jmsAdminOperations.removeJmsQueue("mdbtest/queue");
                jmsAdminOperations.removeJmsQueue("mdbtest/replyQueue");
                jmsAdminOperations.close();
            }
        }
    }

    @Deployment
    public static Archive getDeployment() {
        final JavaArchive ejbJar = ShrinkWrap.create(JavaArchive.class, "SuspendMDBTestCase.jar")
                .addClasses(DDBasedMDB.class, BMTSLSB.class, JMSMessagingUtil.class)
                .addPackage(JMSOperations.class.getPackage())
                .addClass(JmsQueueSetup.class)
                .addAsManifestResource(SuspendMDBTestCase.class.getPackage(), "ejb-jar.xml", "ejb-jar.xml")
                .addAsManifestResource(new StringAsset("Dependencies: org.jboss.as.controller-client, org.jboss.dmr \n"), "MANIFEST.MF")
                .addAsResource(createPermissionsXmlAsset(RemotingPermission.CONNECT,
                        new PropertyPermission("ts.timeout.factor", "read")),
                        "META-INF/jboss-permissions.xml");

        return ejbJar;
    }

    /**
     * Test a deployment descriptor based MDB
     * @throws Exception
     */
    @Test
    public void testSuspendResumeWithMDB() throws Exception {
        boolean resumed = false;
        ModelNode op = new ModelNode();
        try {

            //suspend the server
            op.get(ModelDescriptionConstants.OP).set("suspend");
            managementClient.getControllerClient().execute(op);

            ConnectionFactory cf = (ConnectionFactory) remoteContext.lookup("jms/RemoteConnectionFactory");
            Assert.assertNotNull(cf);

            try (JMSContext context = cf.createContext("guest", "guest")) {
                Destination queue = (Destination) remoteContext.lookup("mdbtest/queue");
                Destination replyQueue = (Destination) remoteContext.lookup("mdbtest/replyQueue");

                String text = "Say hello to " + DDBasedMDB.class.getName();
                context.createProducer()
                        .setJMSReplyTo(replyQueue)
                        .send(queue, text);
                JMSConsumer consumer = context.createConsumer(replyQueue);

                String reply = consumer.receiveBody(String.class, 5000);
                Assert.assertNull("Reply message was not null on reply queue: " + replyQueue, reply);

                resumed = true;
                op = new ModelNode();
                op.get(ModelDescriptionConstants.OP).set("resume");
                managementClient.getControllerClient().execute(op);

                reply = consumer.receiveBody(String.class, 5000);
                Assert.assertNotNull("Reply message was null on reply queue: " + replyQueue, reply);
            }
        } finally {
            if(!resumed) {
                op = new ModelNode();
                op.get(ModelDescriptionConstants.OP).set("resume");
                managementClient.getControllerClient().execute(op);
            }
        }
    }
}
