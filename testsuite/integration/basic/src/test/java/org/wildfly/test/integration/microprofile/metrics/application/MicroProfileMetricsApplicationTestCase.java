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

package org.wildfly.test.integration.microprofile.metrics.application;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STATISTICS_ENABLED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.test.shared.ServerReload.reloadIfRequired;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.wildfly.test.integration.microprofile.metrics.MetricsHelper.getJSONMetrics;
import static org.wildfly.test.integration.microprofile.metrics.MetricsHelper.getMetricValueFromJSONOutput;
import static org.wildfly.test.integration.microprofile.metrics.MetricsHelper.getMetricValueFromPrometheusOutput;
import static org.wildfly.test.integration.microprofile.metrics.MetricsHelper.getPrometheusMetrics;

import java.io.IOException;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test application metrics that are provided by a deployment.
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2018 Red Hat inc.
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup({MicroProfileMetricsApplicationTestCase.EnablesUndertowStatistics.class})
public class MicroProfileMetricsApplicationTestCase {

    static class EnablesUndertowStatistics implements ServerSetupTask {

        @Override
        public void setup(ManagementClient managementClient, String containerId) throws Exception {
            managementClient.getControllerClient().execute(enableStatistics(true));
            reloadIfRequired(managementClient);
        }

        @Override
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            managementClient.getControllerClient().execute(enableStatistics(false));
            reloadIfRequired(managementClient);
        }

        private ModelNode enableStatistics(boolean enabled) {
            final ModelNode address = Operations.createAddress(SUBSYSTEM, "undertow");
            return Operations.createWriteAttributeOperation(address, STATISTICS_ENABLED, enabled);
        }
    }

    @Deployment(name = "MicroProfileMetricsApplicationTestCase", managed = false)
    public static Archive<?> deploy() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "MicroProfileMetricsApplicationTestCase.war")
                .addClasses(TestApplication.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
        return war;
    }

    @ContainerResource
    ManagementClient managementClient;

    @ArquillianResource
    private Deployer deployer;

    private static int requestCalled = 0;

    @Test
    @InSequence(1)
    public void testApplicationMetricBeforeDeployment() throws Exception {
        getPrometheusMetrics(managementClient, "application", false);
        getJSONMetrics(managementClient, "application", false);

        // deploy the archive
        deployer.deploy("MicroProfileMetricsApplicationTestCase");
    }

    @Test
    @InSequence(2)
    @OperateOnDeployment("MicroProfileMetricsApplicationTestCase")
    public void testApplicationMetricWithPrometheusAfterDeployment(@ArquillianResource URL url) throws Exception {
        getPrometheusMetrics(managementClient, "application", false);

        String text = performCall(url);
        assertNotNull(text);
        assertTrue(text.contains("Hello From WildFly!"));

        String metrics = getPrometheusMetrics(managementClient, "application", true);
        double counter = getMetricValueFromPrometheusOutput(metrics, "application", "hello");
        assertEquals(1.0, counter, 0.0);

        performCall(url);
        assertNotNull(text);
        assertTrue(text.contains("Hello From WildFly!"));

        metrics = getPrometheusMetrics(managementClient, "application", true);
        counter = getMetricValueFromPrometheusOutput(metrics, "application", "hello");
        assertEquals(2.0, counter, 0.0);
    }

    @Test
    @InSequence(3)
    @OperateOnDeployment("MicroProfileMetricsApplicationTestCase")
    public void testApplicationMetricWithJSONAfterDeployment(@ArquillianResource URL url) throws Exception {
        // hello counter is at 2.0 from previous testApplicationMetricWithPrometheusAfterDeployment invocations
        String metrics = getJSONMetrics(managementClient, "application", true);
        double counter = getMetricValueFromJSONOutput(metrics, "hello");
        assertEquals(2.0, counter, 0.0);

        String text = performCall(url);
        assertNotNull(text);
        assertTrue(text.contains("Hello From WildFly!"));

        metrics = getJSONMetrics(managementClient, "application", true);
        counter = getMetricValueFromJSONOutput(metrics, "hello");
        assertEquals(3.0, counter, 0.0);

        performCall(url);
        assertNotNull(text);
        assertTrue(text.contains("Hello From WildFly!"));

        metrics = getJSONMetrics(managementClient, "application", true);
        counter = getMetricValueFromJSONOutput(metrics, "hello");
        assertEquals(4.0, counter, 0.0);
    }


    @Test
    @InSequence(4)
    @OperateOnDeployment("MicroProfileMetricsApplicationTestCase")
    public void testDeploymentWildFlyMetrics(@ArquillianResource URL url) throws Exception {
        // test the request-count metric on the deployment's undertow resources
        checkRequestCount(requestCalled, true);
        performCall(url);
        checkRequestCount(requestCalled, true);
    }

    @Test
    @InSequence(5)
    public void tesApplicationMetricAfterUndeployment() throws Exception {
        deployer.undeploy("MicroProfileMetricsApplicationTestCase");

        checkRequestCount(requestCalled, false);
        getPrometheusMetrics(managementClient, "application", false);
        getJSONMetrics(managementClient, "application", false);
    }

    private static String performCall(URL url) throws Exception {
        requestCalled++;
        URL appURL = new URL(url.toExternalForm() + "microprofile-metrics-app/hello");
        return HttpRequest.get(appURL.toExternalForm(), 10, TimeUnit.SECONDS);
    }

    private void checkRequestCount(int expectedCount, boolean deploymentMetricMustExist) throws IOException {
        String prometheusMetricName = "wildfly_undertow_request_count";

        String metrics = getPrometheusMetrics(managementClient, "", true);
        for (String line : metrics.split("\\R")) {
            if (line.startsWith(prometheusMetricName)) {
                String[] split = line.split("\\s+");
                String labels = split[0].substring((prometheusMetricName).length());

                // we are only interested by the metric for this deployment
                if (labels.contains("deployment=\"MicroProfileMetricsApplicationTestCase.war\"")) {
                    if (deploymentMetricMustExist) {
                        Double value = Double.valueOf(split[1]);

                        assertTrue(labels.contains("deployment=\"MicroProfileMetricsApplicationTestCase.war\""));
                        assertTrue(labels.contains("subdeployment=\"MicroProfileMetricsApplicationTestCase.war\""));
                        assertTrue(labels.contains("servlet=\"org.wildfly.test.integration.microprofile.metrics.application.TestApplication\""));
                        assertEquals(Integer.valueOf(expectedCount).doubleValue(), value, 0);

                        return;
                    } else {
                        fail("Metric for the deployment must not exist");
                    }
                }
            }
        }

        if (deploymentMetricMustExist) {
            fail(prometheusMetricName + "metric not found for deployment");
        }
    }

}
