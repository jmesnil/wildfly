/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
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
package org.wildfly.extension.microprofile.metrics;

import java.util.ArrayList;
import java.util.List;

import io.smallrye.metrics.MetricRegistries;
import org.eclipse.microprofile.metrics.MetricID;
import org.eclipse.microprofile.metrics.MetricRegistry;

public class MicroProfileMetricRegistration implements MetricRegistration {

    private final List<Runnable> registrationTasks = new ArrayList<>();
    private final List<MetricID> unregistrationTasks = new ArrayList<>();

    public MicroProfileMetricRegistration() {
    }

    @Override
    public synchronized void register() { // synchronized to avoid registering same thing twice. Shouldn't really be possible; just being cautious
        for (Runnable task : registrationTasks) {
            task.run();
        }
        // This object will last until undeploy or server stop,
        // so clean up and save memory
        registrationTasks.clear();
    }

     @Override
     public void unregister() {
        MetricRegistry registry = MetricRegistries.get(MetricRegistry.Type.VENDOR);
        for (MetricID id : unregistrationTasks) {
            registry.remove(id);
        }
    }

    @Override
    public synchronized void addRegistrationTask(Runnable task) {
        registrationTasks.add(task);
    }

    @Override
    public void addUnregistrationTask(MetricID metricID) {
        unregistrationTasks.add(metricID);
    }
}

