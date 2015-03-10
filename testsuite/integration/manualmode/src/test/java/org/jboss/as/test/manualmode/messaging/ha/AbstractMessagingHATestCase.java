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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.dmr.ModelNode;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.wildfly.test.api.Authentication;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2015 Red Hat inc.
 */
@RunWith(Arquillian.class)
@RunAsClient
public abstract class AbstractMessagingHATestCase {

    public static final String SERVER1 = "jbossas-messaging-ha-server1";
    public static final String SERVER2 = "jbossas-messaging-ha-server2";

    private String snapshotForServer1;
    private String snapshotForServer2;

    @ArquillianResource
    protected static ContainerController container;

    protected static ModelControllerClient createClient1() {
        return TestSuiteEnvironment.getModelControllerClient();
    }

    protected static ModelControllerClient createClient2() throws UnknownHostException {
        return ModelControllerClient.Factory.create(InetAddress.getByName(TestSuiteEnvironment.getServerAddress()),
                TestSuiteEnvironment.getServerPort() + 100,
                Authentication.getCallbackHandler());
    }

    private static String takeSnapshot(ModelControllerClient client) throws Exception {
        ModelNode operation = new ModelNode();
        operation.get(OP).set("take-snapshot");
        ModelNode result = execute(client, operation);
        String snapshot = result.asString();
        return snapshot;
    }

    private void restoreSnapshot(String snapshot) {
        System.out.println("snapshot = " + snapshot);
        File snapshotFile = new File(snapshot);
        File configurationDir = snapshotFile.getParentFile().getParentFile().getParentFile();
        System.out.println("configurationDir = " + configurationDir);
        File standaloneConfiguration = new File(configurationDir, "standalone-full-activemq.xml");
        snapshotFile.renameTo(standaloneConfiguration);
    }

    protected static ModelNode execute(ModelControllerClient client, ModelNode operation) throws Exception {
        System.out.println("operation = " + operation);
        ModelNode response = client.execute(operation);
        System.out.println("response = " + response);
        boolean success = SUCCESS.equals(response.get(OUTCOME).asString());
        if (success) {
            return response.get(RESULT);
        }
        throw new Exception("Operation failed");
    }

    protected static void executeWithFailure(ModelControllerClient client, ModelNode operation) throws IOException {
        ModelNode result = client.execute(operation);
        assertEquals(result.toJSONString(true), FAILED, result.get(OUTCOME).asString());
        assertTrue(result.toJSONString(true), result.get(FAILURE_DESCRIPTION).asString().contains("WFLYMSG0066"));
        assertFalse(result.has(RESULT));
    }

    @Before
    public void setUp() throws Exception {
        container.start(SERVER1);
        ModelControllerClient client1 = createClient1();
        snapshotForServer1 = takeSnapshot(client1);
        reload(client1, true);
        client1 = waitFoServer1ToReload(client1, 10000);
        setUpServer1(client1);
        reload(client1, false);
        client1 = waitFoServer1ToReload(client1, 10000);
        assertTrue(container.isStarted(SERVER1));
        client1.close();

        container.start(SERVER2);
        ModelControllerClient client2 = createClient2();
        snapshotForServer2 = takeSnapshot(client2);
        reload(client2, true);
        client2 = waitFoServer2ToReload(client2, 10000);
        setUpServer2(client2);
        reload(client2, false);
        client2 = waitFoServer2ToReload(client2, 10000);
        assertTrue(container.isStarted(SERVER2));
        client2.close();
    }

    protected abstract void setUpServer1(ModelControllerClient client) throws Exception;
    protected abstract void setUpServer2(ModelControllerClient client) throws Exception;

    @After
    public void tearDown() {
        if (container.isStarted(SERVER1)) {
            container.stop(SERVER1);
        }
        restoreSnapshot(snapshotForServer1);
        if (container.isStarted(SERVER2)) {
            container.stop(SERVER2);
        }
        restoreSnapshot(snapshotForServer2);
    }

    protected static ModelNode reload(ModelControllerClient client, boolean adminOnly) throws IOException {
        ModelNode operation = new ModelNode();
        operation.get(OP_ADDR).setEmptyList();
        operation.get(OP).set("reload");
        operation.get("admin-only").set(adminOnly);
        try {
            ModelNode result = client.execute(operation);
            return result;
        } catch(IOException e) {
            final Throwable cause = e.getCause();
            if (!(cause instanceof ExecutionException) && !(cause instanceof CancellationException)) {
                throw e;
            } // else ignore, this might happen if the channel gets closed before we got the response
        }
        return operation;
    }

    protected ModelControllerClient waitFoServer1ToReload(ModelControllerClient initialClient, long timeout) throws Exception {
        // FIXME use the CLI high-level reload operation that blocks instead of
        // fiddling with timeouts...
        // leave some time to have the server starts its reload process and change
        // its server-starte from running.
        Thread.sleep(TimeoutUtil.adjust(500));
        long start = System.currentTimeMillis();
        long now;
        ModelControllerClient client = initialClient;
        do {
            client.close();
            client = createClient1();
            ModelNode operation = new ModelNode();
            operation.get(OP_ADDR).setEmptyList();
            operation.get(OP).set(READ_ATTRIBUTE_OPERATION);
            operation.get(NAME).set("server-state");
            try {
                ModelNode result = client.execute(operation);
                boolean normal = "running".equals(result.get(RESULT).asString());
                if (normal) {
                    return client;
                }
            } catch (Exception e) {
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
            }
            now = System.currentTimeMillis();
        } while (now - start < timeout);

        throw new Exception("Server did not reload in the imparted time.");
    }

    private ModelControllerClient waitFoServer2ToReload(ModelControllerClient initialClient, long timeout) throws Exception {
        // FIXME use the CLI high-level reload operation that blocks instead of
        // fiddling with timeouts...
        // leave some time to have the server starts its reload process and change
        // its server-starte from running.
        Thread.sleep(TimeoutUtil.adjust(500));
        long start = System.currentTimeMillis();
        long now;
        ModelControllerClient client = initialClient;
        do {
            client.close();
            client = createClient2();
            ModelNode operation = new ModelNode();
            operation.get(OP_ADDR).setEmptyList();
            operation.get(OP).set(READ_ATTRIBUTE_OPERATION);
            operation.get(NAME).set("server-state");
            try {
                ModelNode result = client.execute(operation);
                boolean normal = "running".equals(result.get(RESULT).asString());
                if (normal) {
                    return client;
                }
            } catch (Exception e) {
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
            }
            now = System.currentTimeMillis();
        } while (now - start < timeout);

        throw new Exception("Server did not reload in the imparted time.");
    }
}
