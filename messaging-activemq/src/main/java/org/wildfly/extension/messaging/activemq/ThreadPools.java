/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.messaging.activemq;

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.threads.ScheduledThreadPoolResourceDefinition;
import org.jboss.as.threads.ThreadFactoryResolver;
import org.jboss.as.threads.ThreadsServices;
import org.jboss.msc.service.ServiceName;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2016 Red Hat inc.
 */
public class ThreadPools {
    public static PathElement SCHEDULED_THREAD_POOL_PATH = PathElement.pathElement(org.jboss.as.threads.CommonAttributes.SCHEDULED_THREAD_POOL);

    static ServiceName SCHEDULED_THREAD_POOL_BASE_NAME = ThreadsServices.executorName("messaging-activemq").append("scheduled-executor");

    public static PersistentResourceDefinition SCHEDULED_THREAD_POOL = ScheduledThreadPoolResourceDefinition.create(org.jboss.as.threads.CommonAttributes.SCHEDULED_THREAD_POOL,
            ArtemisThreadFactoryResolver.SCHEDULED_INSTANCE,
            SCHEDULED_THREAD_POOL_BASE_NAME,
            false);

    private static class ArtemisThreadFactoryResolver extends ThreadFactoryResolver.SimpleResolver {
        static final ArtemisThreadFactoryResolver SCHEDULED_INSTANCE = new ArtemisThreadFactoryResolver("ActiveMQ Server Scheduled Thread");
        private final String threadGroupName;

        private ArtemisThreadFactoryResolver(String threadGroupName) {
            super(ThreadsServices.FACTORY);
            this.threadGroupName = threadGroupName;
        }

        @Override
        protected String getThreadGroupName(String threadPoolName) {
            return threadGroupName;
        }
    }
}
