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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PORT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING_GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import java.io.File;

import javax.naming.InitialContext;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.test.integration.common.jms.JMSOperations;
import org.jboss.as.test.integration.common.jms.JMSOperationsProvider;
import org.jboss.dmr.ModelNode;
import org.jboss.util.file.Files;
import org.junit.Ignore;
import org.junit.Test;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2015 Red Hat inc.
 */
@Ignore
public class ReplicationTestCase extends AbstractMessagingHATestCase {

    private final String jmsQueueName = "ReplicationTestCase-Queue";
    private final String jmsQueueLookup = "jms/" + jmsQueueName;

    @Override
    protected void setUpServer1(ModelControllerClient client) throws Exception {
        // /socket-binding-group=standard-sockets/socket-binding=http-slave:add(port=8180)
        // /subsystem=messaging-activemq/server=default/http-connector=slave-connector:add(socket-binding=http-slave)
        // /subsystem=messaging-activemq/server=default/cluster-connection=my-cluster:add(connector-name=http-connector, static-connectors=[slave-connector], cluster-connection-address=jms)
        // /subsystem=messaging-activemq/server=default/ha-policy=replication-master:add(cluster-name=my-cluster)

        ModelNode operation = new ModelNode();
        operation.get(OP_ADDR).add(SOCKET_BINDING_GROUP, "standard-sockets");
        operation.get(OP_ADDR).add(SOCKET_BINDING, "http-slave");
        operation.get(OP).set(ADD);
        operation.get(PORT).set("8180");
        execute(client, operation);

        operation = new ModelNode();
        operation.get(OP_ADDR).add(SUBSYSTEM, "messaging-activemq");
        operation.get(OP_ADDR).add("server", "default");
        operation.get(OP_ADDR).add("http-connector", "slave-connector");
        operation.get(OP).set(ADD);
        operation.get(SOCKET_BINDING).set("http-slave");
        execute(client, operation);

        operation = new ModelNode();
        operation.get(OP_ADDR).add(SUBSYSTEM, "messaging-activemq");
        operation.get(OP_ADDR).add("server", "default");
        operation.get(OP_ADDR).add("cluster-connection", "my-cluster");
        operation.get(OP).set(ADD);
        operation.get("cluster-connection-address").set("jms");
        operation.get("connector-name").set("http-connector");
        operation.get("static-connectors").add("slave-connector");
        execute(client, operation);

        operation = new ModelNode();
        operation.get(OP_ADDR).add(SUBSYSTEM, "messaging-activemq");
        operation.get(OP_ADDR).add("server", "default");
        operation.get(OP_ADDR).add("ha-policy", "replication-master");
        operation.get(OP).set(ADD);
        operation.get("cluster-name").set("my-cluster");
        execute(client, operation);

        JMSOperations jmsOperations = JMSOperationsProvider.getInstance(client);
        jmsOperations.createJmsQueue(jmsQueueName, "java:jboss/exported/" + jmsQueueLookup);
    }

    @Override
    protected void setUpServer2(ModelControllerClient client) throws Exception {
        // /socket-binding-group=standard-sockets/socket-binding=http-master:add(port=8080)
        // /subsystem=messaging-activemq/server=default/http-connector=master-connector:add(socket-binding=http-master)
        // /subsystem=messaging-activemq/server=default/cluster-connection=my-cluster:add(connector-name=http-connector, static-connectors=[master-connector], cluster-connection-address=jms)
        // /subsystem=messaging-activemq/server=default/ha-policy=replication-slave:add(cluster-name=my-cluster)

        ModelNode operation = new ModelNode();
        operation.get(OP_ADDR).add(SOCKET_BINDING_GROUP, "standard-sockets");
        operation.get(OP_ADDR).add(SOCKET_BINDING, "http-master");
        operation.get(OP).set(ADD);
        operation.get(PORT).set("8080");
        execute(client, operation);

        operation = new ModelNode();
        operation.get(OP_ADDR).add(SUBSYSTEM, "messaging-activemq");
        operation.get(OP_ADDR).add("server", "default");
        operation.get(OP_ADDR).add("http-connector", "master-connector");
        operation.get(OP).set(ADD);
        operation.get(SOCKET_BINDING).set("http-master");
        execute(client, operation);

        operation = new ModelNode();
        operation.get(OP_ADDR).add(SUBSYSTEM, "messaging-activemq");
        operation.get(OP_ADDR).add("server", "default");
        operation.get(OP_ADDR).add("cluster-connection", "my-cluster");
        operation.get(OP).set(ADD);
        operation.get("cluster-connection-address").set("jms");
        operation.get("connector-name").set("http-connector");
        operation.get("static-connectors").add("master-connector");
        execute(client, operation);

        operation = new ModelNode();
        operation.get(OP_ADDR).add("subsystem", "messaging-activemq");
        operation.get(OP_ADDR).add("server", "default");
        operation.get(OP_ADDR).add("ha-policy", "replication-slave");
        operation.get(OP).set(ADD);
        operation.get("cluster-name").set("my-cluster");
        operation.get("restart-backup").set(true);
        execute(client, operation);

        JMSOperations jmsOperations = JMSOperationsProvider.getInstance(client);
        jmsOperations.createJmsQueue(jmsQueueName, "java:jboss/exported/" + jmsQueueLookup);

    }

    @Test
    public void testBackupActivation() throws Exception {
        ModelControllerClient client2 = createClient2();
        JMSOperations jmsOperations2 = JMSOperationsProvider.getInstance(client2);
        checkJMSQueue(jmsOperations2, jmsQueueName, false);

        InitialContext context1 = createJNDIContextFromServer1();
        sendAndReceiveMessage(context1, jmsQueueLookup);
        // send a message to server1 before it is stopped
        String text = "sent to server1, received from server2 (after failover)";
        sendMessage(context1, jmsQueueLookup, text);
        context1.close();

        System.out.println("===================");
        System.out.println("STOP SERVER1...");
        System.out.println("===================");
        container.stop(SERVER1);

        // let some time for the backup to detect the failure
        waitForHornetQServerActivation(jmsOperations2, true);
        checkJMSQueue(jmsOperations2, jmsQueueName, true);

        InitialContext context2 = createJNDIContextFromServer2();
        // receive the message that was sent to server1 before failover occurs
        receiveMessage(context2, jmsQueueLookup, text);
        sendAndReceiveMessage(context2, jmsQueueLookup);
        String text2 = "sent to server2, received from server 1 (after failback)";
        sendMessage(context2, jmsQueueLookup, text2);
        context2.close();

        System.out.println("====================");
        System.out.println("START SERVER1...");
        System.out.println("====================");
        // restart the live server
        container.start(SERVER1);

        // let some time for the backup to detect the live node and failback
        ModelControllerClient client1 = createClient1();
        JMSOperations jmsOperations1 = JMSOperationsProvider.getInstance(client1);
        waitForHornetQServerActivation(jmsOperations1, true);
        checkHornetQServerStartedAndActiveAttributes(jmsOperations1, true, true);

        // let some time for the backup to detect the failure
        waitForHornetQServerActivation(jmsOperations2, false);
        // backup server has been restarted in passive mode
        checkHornetQServerStartedAndActiveAttributes(jmsOperations2, true, false);

        checkJMSQueue(jmsOperations2, jmsQueueName, false);

        context1 = createJNDIContextFromServer1();
        // receive the message that was sent to server2 before failback
        receiveMessage(context1, jmsQueueLookup, text2);
        // send & receive a message from server1
        sendAndReceiveMessage(context1, jmsQueueLookup);
        context1.close();

        System.out.println("=============================");
        System.out.println("RETURN TO NORMAL OPERATION...");
        System.out.println("=============================");
    }

    @Test
    public void testBackupFailoverAfterFailback() throws Exception {
        ModelControllerClient client2 = createClient2();
        JMSOperations backupJMSOperations = JMSOperationsProvider.getInstance(client2);
        checkJMSQueue(backupJMSOperations, jmsQueueName, false);

        InitialContext context1 = createJNDIContextFromServer1();
        String text = "sent to server1, received from server2 (after failover)";
        sendMessage(context1, jmsQueueLookup, text);
        context1.close();

        System.out.println("===================");
        System.out.println("STOP SERVER1...");
        System.out.println("===================");
        container.stop(SERVER1);

        // let some time for the backup to detect the failure
        waitForHornetQServerActivation(backupJMSOperations, true);
        checkJMSQueue(backupJMSOperations, jmsQueueName, true);

        InitialContext context2 = createJNDIContextFromServer2();
        // receive the message that was sent to server1 before failover occurs
        receiveMessage(context2, jmsQueueLookup, text);
        // send a message to server2 before server1 fails back
        String text2 = "sent to server2, received from server 1 (after failback)";
        sendMessage(context2, jmsQueueLookup, text2);
        context2.close();

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

        context1 = createJNDIContextFromServer1();
        // receive the message that was sent to server2 before failback
        receiveMessage(context1, jmsQueueLookup, text2);
        String text3 = "sent to server1, received from server2 (after 2nd failover)";
        // send a message to server1 before it is stopped a 2nd time
        sendMessage(context1, jmsQueueLookup, text3);
        context1.close();

        System.out.println("==============================");
        System.out.println("STOP SERVER1 A 2ND TIME...");
        System.out.println("==============================");
        // shutdown server1 a 2nd time
        container.stop(SERVER1);

        // let some time for the backup to detect the failure
        waitForHornetQServerActivation(backupJMSOperations, true);
        checkHornetQServerStartedAndActiveAttributes(backupJMSOperations, true, true);
        checkJMSQueue(backupJMSOperations, jmsQueueName, true);

        context2 = createJNDIContextFromServer2();
        // receive the message that was sent to server1 before failover occurs a 2nd time
        receiveMessage(context2, jmsQueueLookup, text3);
        context2.close();

    }

}
