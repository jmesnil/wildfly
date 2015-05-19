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

package org.jboss.as.test.manualmode.messaging.migration;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.EXTENSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;
import static org.jboss.as.test.manualmode.messaging.migration.LegacyMigrationTestCase.createClient;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.test.integration.common.jms.JMSOperationsProvider;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.dmr.ModelNode;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2015 Red Hat inc.
 */
@RunWith(Arquillian.class)
@RunAsClient
public class LegacyMigrationTestCase {

    // maximum time to reload a server
    protected static int RELOAD_TIMEOUT = 30000;

    public static final String SERVER = "jbossas-messaging-hornetq-server-full-ha";

    @ArquillianResource
    protected static ContainerController container;

    private String snapshotForServer;

    protected static ModelControllerClient createClient() {
        return TestSuiteEnvironment.getModelControllerClient();
    }


    @Before
    public void setUp() throws Exception {
        container.start(SERVER);
        assertTrue(container.isStarted(SERVER));

        ModelControllerClient client = createClient();
        snapshotForServer = takeSnapshot(client);
        reload(client, true);
        client = waitFoServerToReload(client);
        client.close();
    }

    @After
    public void tearDown() {
        if (container.isStarted(SERVER)) {
            container.stop(SERVER);
            assertFalse(container.isStarted(SERVER));
        }

        restoreSnapshot(snapshotForServer);
    }

    @Test
    public void testMigrationWithoutLegacy() throws Exception {
        doMigration(false);
    }

    @Test
    public void testMigrationWithLegacy() throws Exception {
        doMigration(true);
    }

    private void doMigration(boolean enableLegacy) throws Exception {
        assertTrue(container.isStarted(SERVER));

        ModelControllerClient client = createClient();

        ModelNode readLegacyMessagingExgensionOp = new ModelNode();
        readLegacyMessagingExgensionOp.get(OP_ADDR).add(EXTENSION, "org.jboss.as.messaging");
        readLegacyMessagingExgensionOp.get(OP).set(READ_RESOURCE_OPERATION);

        ModelNode readLegacyMessagingSubsystemOp = new ModelNode();
        readLegacyMessagingSubsystemOp.get(OP_ADDR).add(SUBSYSTEM, "messaging");
        readLegacyMessagingSubsystemOp.get(OP).set(READ_RESOURCE_OPERATION);

        ModelNode readNewMessagingExtensionOp = new ModelNode();
        readNewMessagingExtensionOp.get(OP_ADDR).add(EXTENSION, "org.wildfly.extension.messaging-activemq");
        readNewMessagingExtensionOp.get(OP).set(READ_RESOURCE_OPERATION);

        ModelNode readNewMessagingSubsystemOp = new ModelNode();
        readNewMessagingSubsystemOp.get(OP_ADDR).add(SUBSYSTEM, "messaging-activemq");
        readNewMessagingSubsystemOp.get(OP).set(READ_RESOURCE_OPERATION);

        // before migration,
        // the /extension=org.jboss.as.messaging and /subsystem=messaging resources are in the model
        execute(client, readLegacyMessagingExgensionOp);
        execute(client, readLegacyMessagingSubsystemOp);
        // the /extension=org.wildfly.extension.messaging-activemq and /subsystem=messaging-activemq are not in the model
        executeWithFailure(client, readNewMessagingExtensionOp);
        executeWithFailure(client, readNewMessagingSubsystemOp);

        ModelNode migrateOp = new ModelNode();
        migrateOp.get(OP_ADDR).add(SUBSYSTEM, "messaging");
        migrateOp.get(OP).set("migrate");
        migrateOp.get("enable-legacy").set(enableLegacy);

        execute(client, migrateOp);

        // after migration,
        // the /extension=org.jboss.as.messaging and /subsystem=messaging resources are not in the model
        executeWithFailure(client, readLegacyMessagingExgensionOp);
        executeWithFailure(client, readLegacyMessagingSubsystemOp);
        // the /extension=org.wildfly.extension.messaging-activemq and /subsystem=messaging-activemq are in the model
        execute(client, readNewMessagingExtensionOp);
        execute(client, readNewMessagingSubsystemOp);

        // reload the server in running mode to ensure that the new messaging-activemq subsystem is loaded properly
        // and its runtime is working.
        reload(client, false);
        client = waitFoServerToReload(client);

        assertTrue(container.isStarted(SERVER));

        client.close();
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
        File standaloneConfiguration = new File(configurationDir, "standalone-full-ha-hornetq.xml");
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
        assertFalse(result.has(RESULT));
    }

    private ModelControllerClient waitFoServerToReload(ModelControllerClient initialClient) throws Exception {
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
            client = createClient();
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
        } while (now - start < RELOAD_TIMEOUT);

        throw new Exception("Server did not reload in the imparted time.");
    }

}
