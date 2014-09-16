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
import static org.jboss.shrinkwrap.api.ArchivePaths.create;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.UUID;

import javax.ejb.EJB;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2013 Red Hat inc.
 */
@RunWith(Arquillian.class)
public class HornetQServerStartStopTestCase {

    private static final ModelNode hornetQServerAddress;

    static {
        hornetQServerAddress = new ModelNode();
        hornetQServerAddress.add("subsystem", "messaging");
        hornetQServerAddress.add("hornetq-server", "default");
    }

    @ArquillianResource
    private ManagementClient managementClient;

    @EJB
    private MessagingBean bean;

    @Deployment
    public static JavaArchive createArchive() {
        JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "JMSResourceDefinitionsTestCase.jar")
                .addPackage(MessagingBean.class.getPackage())
                .addAsManifestResource(new StringAsset("Manifest-Version: 1.0\n" +
                        "Dependencies: org.jboss.dmr,org.jboss.as.controller-client\n"), "MANIFEST.MF")
                .addAsManifestResource(
                        EmptyAsset.INSTANCE,
                        create("beans.xml"));
        System.out.println("archive = " + archive.toString(true));
        return archive;
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
    public void testStartStop() throws Exception {
        String text = UUID.randomUUID().toString();
        bean.sendMessage(text);
        String response = bean.receiveResponse();
        assertEquals(text, response);

        assertTrue(execute(managementClient, hornetQServerAddress, "stop"));

        text = UUID.randomUUID().toString();
        try {
            bean.sendMessage(text);
            fail("HornetQ server is stopped");
        } catch (Exception e) {
            e.printStackTrace();
        }

        assertTrue(execute(managementClient, hornetQServerAddress, "start"));

        text = UUID.randomUUID().toString();
        bean.sendMessage(text);
        response = bean.receiveResponse();
        assertEquals(text, response);

    }
}
