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

package org.jboss.as.test.manualmode.messaging.ha;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INCLUDE_RUNTIME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PATH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.test.integration.common.jms.JMSOperations;
import org.jboss.as.test.integration.common.jms.JMSOperationsProvider;
import org.jboss.dmr.ModelNode;
import org.jboss.util.file.Files;
import org.junit.Test;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2015 Red Hat inc.
 */
public class SharedStoreTestCase extends AbstractMessagingHATestCase {

    private final String jmsQueueName = "SharedStoreTestCase-Queue";

    private static final File SHARED_STORE_DIR = new File("activemq", System.getProperty("java.io.tmpdir"));

    private void configureSharedStore(ModelControllerClient client) throws Exception {
        ModelNode operation = new ModelNode();
        operation.get(OP_ADDR).add("subsystem", "messaging-activemq");
        operation.get(OP_ADDR).add("server", "default");
        operation.get(OP).set(READ_RESOURCE_OPERATION);
        operation.get(INCLUDE_RUNTIME).set(true);
        execute(client, operation);

        for (String path : new String[] {"journal-directory",
                "large-messages-directory",
                "bindings-directory",
                "paging-directory"
        }) {
            // /subsystem=messaging-activemq/server=default/path=journal-directory:add(path=xxx)
            ModelNode undefineRelativeToAttribute = new ModelNode();
            undefineRelativeToAttribute.get(OP_ADDR).add("subsystem", "messaging-activemq");
            undefineRelativeToAttribute.get(OP_ADDR).add("server", "default");
            undefineRelativeToAttribute.get(OP_ADDR).add("path", path);
            undefineRelativeToAttribute.get(OP).set(ADD);
            File f = new File(SHARED_STORE_DIR, path);
            undefineRelativeToAttribute.get(PATH).set(f.getAbsolutePath());
            execute(client, undefineRelativeToAttribute);
        }
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();

        assertTrue(container.isStarted(SERVER1));
        assertTrue(container.isStarted(SERVER2));
    }

    @Override
    public void tearDown() {
        // remove shared store files
        Files.delete(SHARED_STORE_DIR);

        super.tearDown();
    }

    @Override
    protected void setUpServer1(ModelControllerClient client) throws Exception {
        // /subsystem=messaging-activemq/server=default/ha-policy=shared-store-master:add(failover-on-server-shutdown=true)
        ModelNode operation = new ModelNode();
        operation.get(OP_ADDR).add("subsystem", "messaging-activemq");
        operation.get(OP_ADDR).add("server", "default");
        operation.get(OP_ADDR).add("ha-policy", "shared-store-master");
        operation.get(OP).set(ADD);
        operation.get("failover-on-server-shutdown").set(true);
        execute(client, operation);

        configureSharedStore(client);

        JMSOperations jmsOperations = JMSOperationsProvider.getInstance(client);
        jmsOperations.createJmsQueue(jmsQueueName, "java:jboss/exported/jms/" + jmsQueueName);
    }

    @Override
    protected void setUpServer2(ModelControllerClient client) throws Exception {
        // /subsystem=messaging-activemq/server=default/ha-policy=shared-store-slave:add(restart-backup=true)
        ModelNode operation = new ModelNode();
        operation.get(OP_ADDR).add("subsystem", "messaging-activemq");
        operation.get(OP_ADDR).add("server", "default");
        operation.get(OP_ADDR).add("ha-policy", "shared-store-slave");
        operation.get(OP).set(ADD);
        operation.get("restart-backup").set(true);
        execute(client, operation);

        configureSharedStore(client);

        JMSOperations jmsOperations = JMSOperationsProvider.getInstance(client);
        jmsOperations.createJmsQueue(jmsQueueName, "java:jboss/exported/jms/" + jmsQueueName);

    }

    private void checkJMSQueue(JMSOperations operations, String jmsQueueName, boolean active) throws Exception {
        ModelNode address = operations.getServerAddress().add("jms-queue", jmsQueueName);
        checkQueue0(operations.getControllerClient(), address, "queue-address", active);
    }

    private void checkQueue0(ModelControllerClient client, ModelNode address, String runtimeAttributeName, boolean active) throws Exception {
        ModelNode operation = new ModelNode();
        operation.get(OP_ADDR).set(address);
        operation.get(OP).set(READ_RESOURCE_OPERATION);
        operation.get(INCLUDE_RUNTIME).set(true);
       // ModelNode result = execute(client, operation);
       // System.out.println(runtimeAttributeName + " = " + result.get(runtimeAttributeName));
        //assertEquals(result.toJSONString(true), active, result.get(runtimeAttributeName).isDefined());

        // runtime operation
        operation.get(OP).set("list-messages");
        if (active) {
            execute(client, operation);
        } else {
            executeWithFailure(client, operation);
        }
    }

    @Test
    public void testBackupActivation() throws Exception {
        ModelControllerClient client2 = createClient2();
        JMSOperations backupJMSOperations = JMSOperationsProvider.getInstance(client2);
        checkJMSQueue(backupJMSOperations, jmsQueueName, false);

        System.out.println("===================");
        System.out.println("STOP SERVER1...");
        System.out.println("===================");
        container.stop(SERVER1);

        // let some time for the backup to detect the failure
        waitForHornetQServerActivation(backupJMSOperations, true);
        checkJMSQueue(backupJMSOperations, jmsQueueName, true);

        System.out.println("====================");
        System.out.println("START SERVER1...");
        System.out.println("====================");
        // restart the live server
        container.start(SERVER1);
        // let some time for the backup to detect the live node and failback
        ModelControllerClient client1 = createClient1();
        JMSOperations liveJMSOperations = JMSOperationsProvider.getInstance(client1);
        waitForHornetQServerActivation(liveJMSOperations, true);
        checkHornetQServerStartedAndActiveAttributes(liveJMSOperations, true, true);

        // let some time for the backup to detect the failure
        waitForHornetQServerActivation(backupJMSOperations, false);
        // backup server has been restarted in passive mode
        checkHornetQServerStartedAndActiveAttributes(backupJMSOperations, true, false);

        checkJMSQueue(backupJMSOperations, jmsQueueName, false);

        System.out.println("=============================");
        System.out.println("RETURN TO NORMAL OPERATION...");
        System.out.println("=============================");
    }

    @Test
    public void testBackupFailoverAfterFailback() throws Exception {
        ModelControllerClient client2 = createClient2();
        JMSOperations backupJMSOperations = JMSOperationsProvider.getInstance(client2);
        checkJMSQueue(backupJMSOperations, jmsQueueName, false);

        System.out.println("===================");
        System.out.println("STOP SERVER1...");
        System.out.println("===================");
        container.stop(SERVER1);

        // let some time for the backup to detect the failure
        waitForHornetQServerActivation(backupJMSOperations, true);
        checkJMSQueue(backupJMSOperations, jmsQueueName, true);

        System.out.println("====================");
        System.out.println("START SERVER1...");
        System.out.println("====================");
        // restart the live server
        container.start(SERVER1);
        // let some time for the backup to detect the live node and failback
        ModelControllerClient client1 = createClient1();
        JMSOperations liveJMSOperations = JMSOperationsProvider.getInstance(client1);
        waitForHornetQServerActivation(liveJMSOperations, true);
        checkHornetQServerStartedAndActiveAttributes(liveJMSOperations, true, true);

        // let some time for the backup to detect the failure
        waitForHornetQServerActivation(backupJMSOperations, false);
        // backup server has been restarted in passive mode
        checkHornetQServerStartedAndActiveAttributes(backupJMSOperations, true, false);

        checkJMSQueue(backupJMSOperations, jmsQueueName, false);

        System.out.println("==============================");
        System.out.println("STOP SERVER1 A 2ND TIME...");
        System.out.println("==============================");
        // shutdown server1 a 2nd time
        container.stop(SERVER1);

        // let some time for the backup to detect the failure
        waitForHornetQServerActivation(backupJMSOperations, true);
        checkHornetQServerStartedAndActiveAttributes(backupJMSOperations, true, true);
        checkJMSQueue(backupJMSOperations, jmsQueueName, true);
    }

}
