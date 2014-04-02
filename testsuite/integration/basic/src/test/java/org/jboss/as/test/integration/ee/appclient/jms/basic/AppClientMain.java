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

import javax.annotation.Resource;
import javax.jms.ConnectionFactory;
import javax.jms.JMSContext;
import javax.jms.Queue;
import javax.jms.TemporaryQueue;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.logging.Logger;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2014 Red Hat Inc.
 */
public class AppClientMain {
    private static final Logger logger = Logger.getLogger(AppClientMain.class.getPackage().getName());

    @Resource(lookup = "java:comp/env/jms/queueInApp")
    private static Queue queueInApp;

    @Resource(lookup = "java:comp/env/jms/queueInGlobal")
    private static Queue queueInGlobal;

    public static void main(final String[] params) throws NamingException {
        logger.info("Main method invoked");

        boolean useQueueInApp = "app".equals(params[0]);
        String text = params[1];

        InitialContext ctx = new InitialContext();
        ConnectionFactory cf = (ConnectionFactory) ctx.lookup("java:/ConnectionFactory");

        Queue queue = useQueueInApp ? queueInApp : queueInGlobal;
        System.out.println("Using queue = " + queue);

        try (JMSContext context = cf.createContext("guest", "guest")) {
            TemporaryQueue replyTo = context.createTemporaryQueue();
            context.createProducer()
                    .setJMSReplyTo(replyTo)
                    .send(queue, text);

            String reply = context.createConsumer(replyTo).receiveBody(String.class, 5000);
            if (text.equals(reply)) {
                logger.info("got expected reply");
                System.out.println(reply);
            }
        }
    }

}
