/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.health;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.Optional;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonValue;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test that the HealthCheck probes of a WAR deployment are properly
 * registered when the WAR is deployed withing an EAR
 */
@RunWith(Arquillian.class)
@RunAsClient
public class EARDeploymentTestCase {

    private static final String DISCOVER_ALL_BEANS = """
            <beans bean-discovery-mode="all"></beans>
            """;

    @ContainerResource
    ManagementClient managementClient;

    @Deployment
    public static Archive createDeployment() {

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class,
                EARDeploymentTestCase.class.getSimpleName() + ".ear");

        WebArchive war = ShrinkWrap.create(WebArchive.class, "dep1.war")
                .addClasses(MyReadyProbe.class, MyLiveProbe.class)
                .addAsWebInfResource(new StringAsset(DISCOVER_ALL_BEANS), "beans.xml");

        ear.addAsModule(war);

        ear.setApplicationXML(new StringAsset("""
                <?xml version="1.0" encoding="UTF-8"?>
                <application xmlns="http://xmlns.jcp.org/xml/ns/javaee"
                             xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                             xsi:schemaLocation="https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/application_10.xsd"
                             version="10">
                  <display-name>ear-with-healthchecks</display-name>
                  <module>
                    <web>
                      <web-uri>dep1.war</web-uri>
                      <context-root>/dep</context-root>
                    </web>
                  </module>
                </application>
                """));

        return ear;
    }

    @Test
    @RunAsClient
    public void testHealthCheckFromWARinEAR() throws IOException {

        final String healthURL = "http://" + managementClient.getMgmtAddress() + ":" + managementClient.getMgmtPort() + "/health/ready";

        try (CloseableHttpClient client = HttpClients.createDefault()) {

            CloseableHttpResponse resp = client.execute(new HttpGet(healthURL));
            String content = EntityUtils.toString(resp.getEntity());
            resp.close();

            assertEquals(content, 200, resp.getStatusLine().getStatusCode());

            try (JsonReader jsonReader = Json.createReader(new StringReader(content))) {
                JsonObject payload = jsonReader.readObject();
                String outcome = payload.getString("status");
                assertEquals("UP", outcome);


                Assert.assertTrue(payload.containsKey("checks"));
                List<JsonValue> checks = payload.getJsonArray("checks");
                // The readiness probe "myReadyProbe" must be part of the checks payload
                Optional<JsonValue> probeFromWarInEAR = checks.stream()
                        .filter(v -> "myReadyProbe".equals(v.asJsonObject().getString("name")))
                        .findFirst();
                Assert.assertTrue("Probe not found for the war inside the ear", probeFromWarInEAR.isPresent());
            }
        }
    }
}