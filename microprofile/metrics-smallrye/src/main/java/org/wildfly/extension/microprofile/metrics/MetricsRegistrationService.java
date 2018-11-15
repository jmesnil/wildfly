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
package org.wildfly.extension.microprofile.metrics;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ATTRIBUTE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ATTRIBUTES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBDEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.UNIT;
import static org.wildfly.extension.microprofile.config.smallrye.ServiceNames.CONFIG_PROVIDER;
import static org.wildfly.extension.microprofile.metrics.MicroProfileMetricsSubsystemDefinition.CLIENT_FACTORY_CAPABILITY;
import static org.wildfly.extension.microprofile.metrics.MicroProfileMetricsSubsystemDefinition.MANAGEMENT_EXECUTOR;
import static org.wildfly.extension.microprofile.metrics.MicroProfileMetricsSubsystemDefinition.WILDFLY_REGISTRATION_SERVICE;
import static org.wildfly.extension.microprofile.metrics._private.MicroProfileMetricsLogger.LOGGER;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.function.Supplier;

import io.smallrye.metrics.ExtendedMetadata;
import io.smallrye.metrics.MetricRegistries;
import io.smallrye.metrics.setup.JmxRegistrar;
import org.eclipse.microprofile.metrics.Gauge;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.Metric;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.MetricType;
import org.eclipse.microprofile.metrics.MetricUnits;
import org.jboss.as.controller.LocalModelControllerClient;
import org.jboss.as.controller.ModelControllerClientFactory;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ImmutableManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

public class MetricsRegistrationService implements Service<MetricsRegistrationService> {

    private final ImmutableManagementResourceRegistration rootResourceRegistration;
    private final Resource rootResource;
    private final Supplier<ModelControllerClientFactory> modelControllerClientFactory;
    private final Supplier<Executor> managementExecutor;
    private final List<String> exposedSubsystems;
    private final boolean exposeAnySubsystem;
    private JmxRegistrar jmxRegistrar;
    private LocalModelControllerClient modelControllerClient;

    static void install(OperationContext context, List<String> exposedSubsystems) {
        ImmutableManagementResourceRegistration rootResourceRegistration = context.getRootResourceRegistration();
        Resource rootResource = context.readResourceFromRoot(PathAddress.EMPTY_ADDRESS);

        ServiceBuilder<?> serviceBuilder = context.getServiceTarget().addService(WILDFLY_REGISTRATION_SERVICE);
        Supplier<ModelControllerClientFactory> modelControllerClientFactory = serviceBuilder.requires(context.getCapabilityServiceName(CLIENT_FACTORY_CAPABILITY, ModelControllerClientFactory.class));
        Supplier<Executor> managementExecutor = serviceBuilder.requires(context.getCapabilityServiceName(MANAGEMENT_EXECUTOR, Executor.class));
        serviceBuilder.requires(CONFIG_PROVIDER);
        MetricsRegistrationService service = new MetricsRegistrationService(rootResourceRegistration, rootResource, modelControllerClientFactory, managementExecutor, exposedSubsystems);
        serviceBuilder.setInstance(service)
                .install();
    }

    public MetricsRegistrationService(ImmutableManagementResourceRegistration rootResourceRegistration, Resource rootResource, Supplier<ModelControllerClientFactory> modelControllerClientFactory, Supplier<Executor> managementExecutor, List<String> exposedSubsystems) {
        this.rootResourceRegistration = rootResourceRegistration;
        this.rootResource = rootResource;
        this.modelControllerClientFactory = modelControllerClientFactory;
        this.managementExecutor = managementExecutor;
        this.exposedSubsystems = exposedSubsystems;
        this.exposeAnySubsystem = exposedSubsystems.remove("*");
    }

    @Override
    public void start(StartContext context) throws StartException {
        jmxRegistrar = new JmxRegistrar();
        try {
            jmxRegistrar.init();
        } catch (IOException e) {
            throw LOGGER.failedInitializeJMXRegistrar(e);
        }

        modelControllerClient = modelControllerClientFactory.get().createClient(managementExecutor.get());
        // register metrics from WildFly subsystems in the VENDOR metric registry
        registerMetrics(rootResource,
                rootResourceRegistration,
                MetricRegistries.get(MetricRegistry.Type.VENDOR),
                Function.identity());
    }

    @Override
    public void stop(StopContext context) {
        for (MetricRegistry registry : new MetricRegistry[]{
                MetricRegistries.get(MetricRegistry.Type.BASE),
                MetricRegistries.get(MetricRegistry.Type.VENDOR)}) {
            for (String name : registry.getNames()) {
                registry.remove(name);
            }
        }

        modelControllerClient.close();
        jmxRegistrar = null;
    }

    @Override
    public MetricsRegistrationService getValue() {
        return this;
    }

    public Set<String> registerMetrics(Resource rootResource,
                                       ImmutableManagementResourceRegistration managementResourceRegistration,
                                       MetricRegistry metricRegistry,
                                       Function<PathAddress, PathAddress> resourceAddressResolver) {
        Map<PathAddress, Map<String, ModelNode>> metrics = findMetrics(managementResourceRegistration);
        logMetrics(metrics);
        Set<String> registeredMetrics = registerMetrics(metrics, metricRegistry, resourceAddressResolver);
        return registeredMetrics;
    }

    private void logMetrics(Map<PathAddress, Map<String, ModelNode>> metrics) {
        List<String> metricsNames = new ArrayList<>();
        Map<String, List<PathAddress>> occurrences = new TreeMap<>();

        for (Map.Entry<PathAddress, Map<String, ModelNode>> entry : metrics.entrySet()) {
            PathAddress address = entry.getKey();
            for (Map.Entry<String, ModelNode> wildflyMetric : entry.getValue().entrySet()) {
                String attributeName = wildflyMetric.getKey();
                String metricName = buildMetricName(address, attributeName, false);
                metricsNames.add(metricName);

                List<PathAddress> dups = occurrences.getOrDefault(metricName, new ArrayList<>());
                if (dups.size() == 1) {
                    PathAddress duplicateAddress = dups.get(0);
                    if (isDuplicateDFromDeployment(address, duplicateAddress)) {
                        // that's ok, we accept that the same resource from deployment/subdeployment and subsystems use the same metric name
                        break;
                    }
                    String newMetricName1 = buildMetricName(address, attributeName, true);
                    String newUnduplicatedMetricName = buildMetricName(duplicateAddress, attributeName, true);
                    occurrences.remove(metricName);
                    occurrences.put(newMetricName1, Arrays.asList(address));
                    occurrences.put(newUnduplicatedMetricName, Arrays.asList(duplicateAddress));
                } else {
                    dups.add(address);
                    occurrences.put(metricName, dups);
                }
            }
        }

        LOGGER.info("number of metrics " + metricsNames.size());

        for (Map.Entry<String, List<PathAddress>> entry : occurrences.entrySet()) {
            List<PathAddress> addresses = entry.getValue();
            LOGGER.info(((addresses.size() > 1) ? "[DUP]" : "") + entry.getKey());
            for (PathAddress address : entry.getValue()) {
                LOGGER.info("\t* " + address.toPathStyleString());
            }
        }
    }

    private boolean isDuplicateDFromDeployment(PathAddress address1, PathAddress address2) {
        PathAddress address1StrippedFromDeployment = stripDeployment(address1);
        PathAddress address2StrippedFromDeployment = stripDeployment(address2);
        return address1StrippedFromDeployment.equals(address2StrippedFromDeployment);
    }

    private PathAddress stripDeployment(PathAddress address) {
        if (address.size() == 0) {
            return address;
        }

        PathAddress strippedAddress = address;
        if (address.getElement(0).getKey().equals(DEPLOYMENT)) {
            strippedAddress = strippedAddress.subAddress(1);

            if (strippedAddress.size() > 0 && strippedAddress.getElement(0).getKey().equals(SUBDEPLOYMENT)) {
                strippedAddress = strippedAddress.subAddress(1);
            }
        }
        return strippedAddress;
    }

    private String buildMetricName(PathAddress address, String attributeName, boolean addRightMostElementValueName) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < address.size(); i++) {
            PathElement element = address.getElement(i);
            if (element.getKey().equals(DEPLOYMENT) ||
                    element.getKey().equals(SUBDEPLOYMENT)) {
                // do not append the deployment or subdeployments
                continue;
            }
            if (element.getKey().equals(SUBSYSTEM)) {
                sb.append(element.getValue());
            } else {
                sb.append(element.getKey());
                if (addRightMostElementValueName && i == address.size() -1) {
                    sb.append("_").append(element.getValue());
                }
            }
            sb.append("_");
        }
        String metricName = sb.append(attributeName).toString().replace("-", "_");
        return metricName;
    }


    private Map<PathAddress, Map<String, ModelNode>> findMetrics(ImmutableManagementResourceRegistration managementResourceRegistration) {
        Map<PathAddress, Map<String, ModelNode>> metrics = new HashMap<>();
        collectMetrics(managementResourceRegistration, PathAddress.EMPTY_ADDRESS, metrics);
        return metrics;
    }

    private void collectMetrics(ImmutableManagementResourceRegistration managementResourceRegistration, final PathAddress address, Map<PathAddress, Map<String, ModelNode>> collectedMetrics) {

        if (!isExposingMetrics(address)) {
            return;
        }

        Map<String, AttributeAccess> attributes = managementResourceRegistration.getAttributes(address);
        ModelNode description = null;
        for (Map.Entry<String, AttributeAccess> entry : attributes.entrySet()) {
            String attributeName = entry.getKey();
            AttributeAccess attributeAccess = entry.getValue();
            if (attributeAccess.getAccessType() == AttributeAccess.AccessType.METRIC
                    && attributeAccess.getStorageType() == AttributeAccess.Storage.RUNTIME) {
                if (description == null) {
                    DescriptionProvider modelDescription = managementResourceRegistration.getModelDescription(address);
                    description = modelDescription.getModelDescription(Locale.getDefault());
                }

                Map<String, ModelNode> metricsForResource = collectedMetrics.get(address);
                if (metricsForResource == null) {
                    metricsForResource = new HashMap<>();
                }
                metricsForResource.put(attributeName, description.get(ATTRIBUTES, attributeName));
                collectedMetrics.put(address, metricsForResource);
            }
        }

        for (PathElement childAddress : managementResourceRegistration.getChildAddresses(address)) {
            collectMetrics(managementResourceRegistration, address.append(childAddress), collectedMetrics);
        }

        /*
        for (String type : current.getChildTypes()) {
            if (current.hasChildren(type)) {
                for (Resource.ResourceEntry entry : current.getChildren(type)) {
                    final PathElement pathElement = entry.getPathElement();
                    final PathAddress childAddress = address.append(pathElement);
                    collectMetrics(entry, managementResourceRegistration, childAddress, collectedMetrics);
                }
            }
        }
        */
    }

    public Set<String> registerMetrics(Map<PathAddress, Map<String, ModelNode>> metrics, MetricRegistry registry, Function<PathAddress, PathAddress> resourceAddressResolver) {
        Set<String> registeredMetricNames = new HashSet<>();

        for (Map.Entry<PathAddress, Map<String, ModelNode>> entry : metrics.entrySet()) {
            PathAddress resourceAddress = resourceAddressResolver.apply(entry.getKey());
            for (Map.Entry<String, ModelNode> wildflyMetric : entry.getValue().entrySet()) {
                String attributeName = wildflyMetric.getKey();
                ModelNode attributeDescription = wildflyMetric.getValue();

                String metricName = resourceAddress.toPathStyleString().substring(1) + "/" + attributeName;
                String unit = attributeDescription.get(UNIT).asString(MetricUnits.NONE).toLowerCase();
                String description = attributeDescription.get(DESCRIPTION).asStringOrNull();

                // fill the MP Metric tags with the address key/value pairs (that will be not preserve order)
                // and an attribute=<attributeName> tag
                HashMap<String, String> tags = new HashMap<>();
                for (PathElement element: resourceAddress) {
                    tags.put(element.getKey(), element.getValue());
                }
                tags.put(ATTRIBUTE, attributeName);

                Metadata metadata = new ExtendedMetadata(metricName, attributeName + " for " + resourceAddress.toHttpStyleString(), description, MetricType.GAUGE, unit);
                metadata.setTags(tags);

                ModelType type = attributeDescription.get(TYPE).asType();

                switch (type) {
                    // simple numerical type
                    case BIG_DECIMAL:
                    case BIG_INTEGER:
                    case DOUBLE:
                    case INT:
                    case LONG:
                        break;
                    case BYTES:
                    case LIST:
                    case OBJECT:
                    case PROPERTY:
                    case EXPRESSION:
                    case BOOLEAN:
                    case STRING:
                    case TYPE:
                    case UNDEFINED:
                    default:
                        LOGGER.debugf("Type %s is not supported for MicroProfile Metrics, the attribute %s on %s will not be registered.", type, attributeName, resourceAddress);
                        continue;
                }
                Metric metric = new Gauge() {
                    @Override
                    public Number getValue() {
                        final ModelNode readAttributeOp = new ModelNode();
                        readAttributeOp.get(OP).set(READ_ATTRIBUTE_OPERATION);
                        readAttributeOp.get(OP_ADDR).set(resourceAddress.toModelNode());
                        readAttributeOp.get(ModelDescriptionConstants.INCLUDE_UNDEFINED_METRIC_VALUES).set(true);
                        readAttributeOp.get(NAME).set(attributeName);
                        ModelNode response = modelControllerClient.execute(readAttributeOp);
                        String error = getFailureDescription(response);
                        if (error != null) {
                            registry.remove(metricName);
                            throw LOGGER.unableToReadAttribute(attributeName, resourceAddress, error);
                        }
                        ModelNode result = response.get(RESULT);
                        if (result.isDefined()) {
                            try {
                                switch (type) {
                                    case INT:
                                        return result.asInt();
                                    case LONG:
                                        return result.asLong();
                                    default:
                                        // handle other numerical types as Double
                                        return result.asDouble();
                                }
                            } catch (Exception e) {
                                throw LOGGER.unableToConvertAttribute(attributeName, resourceAddress, e);
                            }
                        } else {
                            registry.remove(metricName);
                            throw LOGGER.undefinedMetric(attributeName, resourceAddress);
                        }
                    }
                };
                registry.register(metadata, metric);
                registeredMetricNames.add(metadata.getName());
            }
        }
        return registeredMetricNames;
    }

    private boolean isExposingMetrics(PathAddress address) {
        // root resource
        if (address.size() == 0) {
            return true;
        }
        if (address.getElement(0).getKey().equals(DEPLOYMENT)) {
            return true;
        }
        if (address.getElement(0).getKey().equals(SUBSYSTEM)) {
            String subsystemName = address.getElement(0).getValue();
            return exposeAnySubsystem || exposedSubsystems.contains(subsystemName);
        }
        // do not expose metrics for resources outside the subsystems and deployments.
        return false;
    }

    private boolean isMetric(AttributeAccess attributeAccess) {
        if (attributeAccess.getAccessType() == AttributeAccess.AccessType.METRIC && attributeAccess.getStorageType() == AttributeAccess.Storage.RUNTIME) {
            return true;
        }
        return false;
    }

    private String getFailureDescription(ModelNode result) {
        if (result.hasDefined(FAILURE_DESCRIPTION)) {
            return result.get(FAILURE_DESCRIPTION).toString();
        }
        return null;
    }
}