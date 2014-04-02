/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.ee.appclient.jms.basic;

import static org.jboss.shrinkwrap.api.ShrinkWrap.create;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.UUID;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.ee.appclient.util.AppClientWrapper;
import org.jboss.as.test.integration.messaging.jms.definitions.MessagingBean;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests that an application client can launch and use JMS with defined destination.
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2014 Red Hat Inc.
 */
@RunWith(Arquillian.class)
@RunAsClient
public class SimpleJMSApplicationClientTestCase {

    private static Archive archive;

    private static final String APPCLIENT_JAR = "appclient.jar";

    @ArquillianResource
    private ManagementClient managementClient;

    @Deployment(testable = false)
    public static Archive<?> deploy() {
        final JavaArchive ejb = create(JavaArchive.class, "ejb.jar")
                .addAsManifestResource(EmptyAsset.INSTANCE, ArchivePaths.create("beans.xml"))
                .addClass(MessagingDefinitions.class)
                .addClasses(MDBFromApp.class, MDBFromGlobal.class);

        final JavaArchive appClient = create(JavaArchive.class, APPCLIENT_JAR)
                .addClass(AppClientMain.class)
                .addAsManifestResource(new StringAsset("Main-Class: " + AppClientMain.class.getName() + "\n"), "MANIFEST.MF")
                .addAsManifestResource(SimpleJMSApplicationClientTestCase.class.getPackage(), "application-client.xml", "application-client.xml")
                .addAsManifestResource(SimpleJMSApplicationClientTestCase.class.getPackage(), "jboss-client.xml", "jboss-client.xml");

        final EnterpriseArchive ear = create(EnterpriseArchive.class, "simple-jms-appclient-test.ear")
                .addAsModule(ejb)
                .addAsModule(appClient);

        archive = ear;
        return ear;
    }

    @Test
    public void simpleAppClientTestWithQueueInGlobal() throws Exception {
        runSimpleAppClientTestWithQueue("global");
    }

    @Test
    public void simpleAppClientTestWithQueueInApp() throws Exception {
        runSimpleAppClientTestWithQueue("app");
    }

    private void runSimpleAppClientTestWithQueue(String namespace) throws Exception {
        String payload = UUID.randomUUID().toString();
        System.out.println("payload = " + payload);
        final AppClientWrapper wrapper = new AppClientWrapper(archive, "--host=" + managementClient.getRemoteEjbURL(), APPCLIENT_JAR, namespace + " " + payload);
        try {
            String text = wrapper.readAllUnformated(10000);
            assertTrue(text.contains(payload));
        } finally {
            wrapper.quit();
        }
    }

}
