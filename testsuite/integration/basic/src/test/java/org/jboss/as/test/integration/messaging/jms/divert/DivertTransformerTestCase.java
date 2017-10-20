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

import static org.jboss.shrinkwrap.api.ArchivePaths.create;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.UUID;

import javax.inject.Inject;
import javax.jms.Message;
import javax.jms.TextMessage;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Add a divert with a transformer and test that is is correctly used by Artemis
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2017 Red Hat inc.
 */
@RunWith(Arquillian.class)
@ServerSetup(DivertSetupTask.class)
public class DivertTransformerTestCase {

    @Deployment
    public static JavaArchive createArchive() throws Exception {
        JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "DivertTransformerTest.jar")
                .addClasses(DivertSetupTask.class, MessagingBean.class)
                .addAsManifestResource(
                        EmptyAsset.INSTANCE,
                        create("beans.xml"));
        return archive;
    }

    @Inject
    private MessagingBean bean;

    @Test
    public void testDivertTransformer() throws Exception {
        assertNotNull(bean);

        // send message to queue A
        String text = UUID.randomUUID().toString();
        bean.sendMessageToQueueA(text);

        // check that no message was received on queue A
        Message message = bean.receiveMessage(true);
        assertNull(message);

        // check that the message was received on queue B...
        message = bean.receiveMessage(false);
        assertNotNull(message);
        // ... and its text is prepended by the divert transformer prefix
        TextMessage m = (TextMessage)message;
        assertEquals(DivertSetupTask.PREFIX + text, m.getText());
    }
}
