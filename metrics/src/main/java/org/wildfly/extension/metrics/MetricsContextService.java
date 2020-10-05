/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.metrics;


import static org.wildfly.extension.metrics.MetricsSubsystemDefinition.HTTP_CONTEXT_SERVICE;
import static org.wildfly.extension.metrics.MetricsSubsystemDefinition.HTTP_EXTENSIBILITY_CAPABILITY;

import java.util.function.Supplier;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.server.mgmt.domain.ExtensibleHttpManagement;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;
import org.wildfly.extension.metrics.internal.PrometheusExporter;
import org.wildfly.extension.metrics.internal.WildFlyMetricRegistry;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2018 Red Hat inc.
 */
public class MetricsContextService implements Service {

    private static final String CONTEXT_NAME = "/base-metrics";

    private final Supplier<ExtensibleHttpManagement> extensibleHttpManagement;
    private final Supplier<WildFlyMetricRegistry> wildFlyMetricRegistry;
    private final boolean securityEnabled;
    private final PrometheusExporter prometheusExporter = new PrometheusExporter();

    static void install(OperationContext context, boolean securityEnabled) {
        ServiceBuilder<?> serviceBuilder = context.getServiceTarget().addService(HTTP_CONTEXT_SERVICE);

        Supplier<ExtensibleHttpManagement> extensibleHttpManagement = serviceBuilder.requires(context.getCapabilityServiceName(HTTP_EXTENSIBILITY_CAPABILITY, ExtensibleHttpManagement.class));
        Supplier<WildFlyMetricRegistry> wildFlyMetricRegistry = serviceBuilder.requires(WildFlyMetricRegistryService.WILDFLY_METRIC_REGISTRY_SERVICE);

        Service healthContextService = new MetricsContextService(extensibleHttpManagement, wildFlyMetricRegistry, securityEnabled);

        serviceBuilder.setInstance(healthContextService)
                .install();
    }

    MetricsContextService(Supplier<ExtensibleHttpManagement> extensibleHttpManagement, Supplier<WildFlyMetricRegistry> wildFlyMetricRegistry, boolean securityEnabled) {
        this.extensibleHttpManagement = extensibleHttpManagement;
        this.wildFlyMetricRegistry = wildFlyMetricRegistry;
        this.securityEnabled = securityEnabled;
    }

    @Override
    public void start(StartContext context) {
        extensibleHttpManagement.get().addManagementHandler(CONTEXT_NAME, securityEnabled,
                new HttpHandler() {
                    @Override
                    public void handleRequest(HttpServerExchange exchange) throws Exception {
                        String wildFlyMetrics = prometheusExporter.export(wildFlyMetricRegistry.get());
                        exchange.getResponseSender().send(wildFlyMetrics);
                    }
                });
    }

    @Override
    public void stop(StopContext context) {
        extensibleHttpManagement.get().removeContext(CONTEXT_NAME);
    }
}
