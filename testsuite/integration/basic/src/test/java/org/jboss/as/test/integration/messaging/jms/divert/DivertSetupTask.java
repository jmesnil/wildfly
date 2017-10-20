/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.messaging.jms.divert;


import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.helpers.ClientConstants;
import org.jboss.as.test.integration.common.jms.JMSOperations;
import org.jboss.as.test.integration.common.jms.JMSOperationsProvider;
import org.jboss.as.test.module.util.TestModule;
import org.jboss.dmr.ModelNode;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2017 Red Hat inc.
 */
public class DivertSetupTask implements ServerSetupTask {

    static final String QUEUE_A_LOOKUP = "java:/jms/divertTransformer/queueA";
    static final String QUEUE_B_LOOKUP = "java:/jms/divertTransformer/queueB";

    private static final String MODULE_NAME = "test.divert-transformer";
    private static final String DIVERT_NAME = "divert-with-transformer";

    private static TestModule testModule;
    static final String TRANSFORMER_PROP = "my-prefix";
    static final String PREFIX = "AAA-";

    @Override
    public void setup(ManagementClient managementClient, String containerId) throws Exception {
        System.out.println("DivertSetupTask.setup");


        URL url = DivertTransformerTestCase.class.getResource("module.xml");
        File moduleXmlFile = new File(url.toURI());
        testModule = new TestModule(MODULE_NAME, moduleXmlFile);
        testModule.addResource("transformer.jar")
                .addClass(DivertTransformer.class);
        testModule.create();

        try (JMSOperations jmsOperations = JMSOperationsProvider.getInstance(managementClient.getControllerClient())) {
            // create queue A
            jmsOperations.createJmsQueue("a", QUEUE_A_LOOKUP);
            // create queue B
            jmsOperations.createJmsQueue("b", QUEUE_B_LOOKUP);
            // create an exclusive divert from A to B with the transformer
            addDivert(jmsOperations);
        }
    }

    @Override
    public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
        System.out.println("DivertSetupTask.tearDown");
        try (JMSOperations jmsOperations = JMSOperationsProvider.getInstance(managementClient.getControllerClient())) {
            // remove divert
            removeDivert(jmsOperations);
            // remove queue B
            jmsOperations.removeJmsQueue("b");
            // remove queue A
            jmsOperations.removeJmsQueue("a");
        }

        testModule.remove();
    }


    /**
     * The divert redirects messages from JMS Queue a to b.
     */
    private static void addDivert(JMSOperations jmsOperations) throws IOException {
        ModelNode addDivertOp = new ModelNode();
        ModelNode divertAddress = jmsOperations.getServerAddress().add("divert", DIVERT_NAME);
        addDivertOp.get(OP).set(ADD);
        addDivertOp.get(OP_ADDR).set(divertAddress);
        ModelNode transformer = new ModelNode();
        transformer.get("name").set(DivertTransformer.class.getName());
        transformer.get("module").set(MODULE_NAME);
        transformer.get("properties").add(TRANSFORMER_PROP, PREFIX);
        addDivertOp.get("transformer-class").set(transformer);
        addDivertOp.get("divert-address").set("a");
        addDivertOp.get("forwarding-address").set("b");
        addDivertOp.get("exclusive").set(true);

        execute(jmsOperations.getControllerClient(), addDivertOp);
    }

    private static void removeDivert(JMSOperations jmsOperations) throws IOException {
        ModelNode removeDivertOp = new ModelNode();
        ModelNode divertAddress = jmsOperations.getServerAddress().add("divert", DIVERT_NAME);
        removeDivertOp.get(OP).set(REMOVE);
        removeDivertOp.get(OP_ADDR).set(divertAddress);

        execute(jmsOperations.getControllerClient(), removeDivertOp);
    }

    static void execute(ModelControllerClient controllerClient, final ModelNode operation) throws IOException {
        ModelNode result = controllerClient.execute(operation);
        if (result.hasDefined(ClientConstants.OUTCOME) && ClientConstants.SUCCESS.equals(result.get(ClientConstants.OUTCOME).asString())) {
            return;
        } else if (result.hasDefined(ClientConstants.FAILURE_DESCRIPTION)) {
            final String failureDesc = result.get(ClientConstants.FAILURE_DESCRIPTION).toString();
            throw new RuntimeException(failureDesc);
        } else {
            throw new RuntimeException("Operation not successful; outcome = " + result.get(ClientConstants.OUTCOME));
        }
    }

}
