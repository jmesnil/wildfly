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

package org.jboss.as.test.integration.messaging.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Resource;
import javax.jms.ConnectionFactory;
import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.TemporaryQueue;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.integration.jca.security.AbstractLoginModuleSecurityDomainTestCaseSetup;
import org.jboss.as.test.integration.security.common.AbstractSecurityDomainSetup;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test for [WFLY-6644] pooled-connection-factory with security domain
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2013 Red Hat inc.
 */
@RunWith(Arquillian.class)
@ServerSetup(PooledConnectionFactoryWithSecurityDomainTestCase.RaWithSecurityDomainTestCaseSetup.class)
public class PooledConnectionFactoryWithSecurityDomainTestCase {

    private static final String SECURITY_DOMAIN = "PCFRealm";

    private static final String PCF_ENTRY = "java:jboss/pcf";

    /**
     * Create a messaging pooled-connection-factory with a security domain and configure this security domain.
     */
    static class RaWithSecurityDomainTestCaseSetup extends AbstractLoginModuleSecurityDomainTestCaseSetup {

        private static PathAddress PCF_ADDRESS = PathAddress.pathAddress("subsystem", "messaging-activemq")
                .append("server", "default")
                .append("pooled-connection-factory", "my-pcf");

        @Override
        protected String getSecurityDomainName() {
            return SECURITY_DOMAIN;
        }

        @Override
        protected String getLoginModuleName() {
            return "ConfiguredIdentity";
        }

        @Override
        protected boolean isRequired() {
            return true;
        }

        /*
         * Use the guest/guest user that is configured with the test suite
         * so that the subject created for the pooled-connection-factory will
         * be authenticated by Artemis using the WildFly authentication manager
         */
        @Override
        protected Map<String, String> getModuleOptions() {
            Map<String, String> moduleOptions = new HashMap<String, String>();
            moduleOptions.put("userName", "guest");
            moduleOptions.put("password", "guest");
            moduleOptions.put("principal", "sa");
            return moduleOptions;
        }

        @Override
        public void setup(ManagementClient managementClient, String containerId) throws Exception {
            super.setup(managementClient, containerId);

            ModelNode addOperation = Util.createAddOperation(PCF_ADDRESS);
            addOperation.get("connectors").add("http-connector");
            addOperation.get("entries").add(PCF_ENTRY);
            addOperation.get("security-domain").set(SECURITY_DOMAIN);

            managementClient.getControllerClient().execute(addOperation);
        }

        @Override
        public void tearDown(ManagementClient managementClient, String containerId) {
            ModelNode removeOperation = Util.createRemoveOperation(PCF_ADDRESS);
            try {
                managementClient.getControllerClient().execute(removeOperation);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Deployment
    public static Archive<?> deploymentSingleton() {
        return ShrinkWrap.create(JavaArchive.class, "PooledConnectionFactoryWithSecurityDomainTestCase.jar")
                .addClass(PooledConnectionFactoryWithSecurityDomainTestCase.class)
                .addClass(AbstractLoginModuleSecurityDomainTestCaseSetup.class)
                .addClass(AbstractSecurityDomainSetup.class);
    }

    @Resource(mappedName = PCF_ENTRY)
    private ConnectionFactory connectionFactory;

    @Test
    public void deploymentTest() throws Exception {
        assertNotNull(connectionFactory);
        try (
                JMSContext context = connectionFactory.createContext()
        ) {
            assertNotNull(context);
            // send and receive a message to trigger user authentication on the server side
            // using the subject from the security domain
            TemporaryQueue temporaryQueue = context.createTemporaryQueue();
            JMSConsumer consumer = context.createConsumer(temporaryQueue);

            String text = UUID.randomUUID().toString();
            context.createProducer().send(temporaryQueue, text);
            String reply = consumer.receiveBody(String.class, 1000);
            assertEquals(text, reply);
        }
    }
}
