/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.manualmode.messaging.ha;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.test.integration.common.jms.JMSOperations;
import org.jboss.as.test.integration.common.jms.JMSOperationsProvider;
import org.jboss.dmr.ModelNode;
import org.junit.Before;

/**
 * Failover and failback tests using 2 Artemis nodes (with replication).
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2015 Red Hat inc.
 */
public class ReplicatedFailoverTestCase extends FailoverTestCase {

    @Override
    protected void setUpServer1(ModelControllerClient client) throws Exception {
        super.setUpServer1(client);

        addDebug(client, "SERVER #1");
        configureCluster(client);
        useTCPStack(client);

        // /subsystem=messaging-activemq/server=default/ha-policy=replication-master:add(cluster-name=my-cluster, check-for-live-server=true)
        ModelNode operation = new ModelNode();
        operation.get(OP_ADDR).add(SUBSYSTEM, "messaging-activemq");
        operation.get(OP_ADDR).add("server", "default");
        operation.get(OP_ADDR).add("ha-policy", "replication-master");
        operation.get(OP).set(ADD);
        operation.get("cluster-name").set("my-cluster");
        operation.get("check-for-live-server").set(true);
        execute(client, operation, true);
    }

    @Override
    protected void setUpServer2(ModelControllerClient client) throws Exception {
        super.setUpServer2(client);

        addDebug(client, "SERVER #2");
        configureCluster(client);
        useTCPStack(client);

        // /subsystem=messaging-activemq/server=default/ha-policy=replication-slave:add(cluster-name=my-cluster, restart-backup=true, failback-delay=2000)

        ModelNode operation = new ModelNode();
        operation.get(OP_ADDR).add("subsystem", "messaging-activemq");
        operation.get(OP_ADDR).add("server", "default");
        operation.get(OP_ADDR).add("ha-policy", "replication-slave");
        operation.get(OP).set(ADD);
        operation.get("cluster-name").set("my-cluster");
        operation.get("failback-delay").set(2000);
        operation.get("restart-backup").set(true);
        execute(client, operation, true);
    }

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();

        // leave some time after servers are setup and reloaded so that the cluster is formed
        Thread.sleep(60000);
    }

    protected void configureCluster(ModelControllerClient client) throws Exception {
        // /subsystem=messaging-activemq/server=default:write-attribute(name=cluster-user, value=clusteruser)
        // /subsystem=messaging-activemq/server=default:write-attribute(name=cluster-password, value=clusterpwd)

        ModelNode operation = new ModelNode();
        operation.get(OP_ADDR).add(SUBSYSTEM, "messaging-activemq");
        operation.get(OP_ADDR).add("server", "default");
        operation.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
        operation.get(NAME).set("cluster-password");
        operation.get(VALUE).set("clusterpassword");
        execute(client, operation, true);

        operation = new ModelNode();
        operation.get(OP_ADDR).add(SUBSYSTEM, "messaging-activemq");
        operation.get(OP_ADDR).add("server", "default");
        operation.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
        operation.get(NAME).set("cluster-user");
        operation.get(VALUE).set("clusteruser");
        execute(client, operation, true);
    }

    private void useTCPStack(ModelControllerClient client) throws Exception {
        // /subsystem=messaging-activemq/server=default/broadcast-group=bg-group1:write-attribute(name=jgroups-stack, value=tcp)
        // /subsystem=messaging-activemq/server=default/discovery-group=dg-group1:write-attribute(name=jgroups-stack, value=tcp)
        ModelNode operation = new ModelNode();
        operation.get(OP_ADDR).add("subsystem", "messaging-activemq");
        operation.get(OP_ADDR).add("server", "default");
        operation.get(OP_ADDR).add("broadcast-group", "bg-group1");
        operation.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
        operation.get(NAME).set("jgroups-stack");
        operation.get(VALUE).set("tcp");
        execute(client, operation, true);

        operation.get(OP_ADDR).setEmptyList();
        operation.get(OP_ADDR).add("subsystem", "messaging-activemq");
        operation.get(OP_ADDR).add("server", "default");
        operation.get(OP_ADDR).add("discovery-group", "dg-group1");
        execute(client, operation, true);
    }
}
