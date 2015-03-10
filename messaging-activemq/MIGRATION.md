# Migration Guide

The goal of this guide is to help migration from messaging (with HornetQ) subsystem to the new messaging-activemq subsystem.

# Domain Model

* management model starts at `1.0.0`
  * there is __no__ resource transformation from the `messaging-activemq` resources to the legacy `messaging` resources.
* extension module: `org.jboss.as.messaging` -> `org.wildfly.extension.messaging-activemq`
* server address: `/subsystem=messaging/hornetq-server=<name>` -> `/subsystem=messaging-activemq/server=<name>`

# XML

* namespace:
  * `urn:jboss:domain:messaging:3.0` -> `urn:jboss:domain:messaging-activemq:1.0`
  * `urn:jboss:messaging-deployment:1.0` -> `urn:jboss:messaging-activemq-deployment:1.0`

# Logging

* prefix: `WFLYMSG` -> `WFLYMSGAMQ`

# Data

* relative to `jboss.server.data.dir`
  * `messagingbindings/` -> `activemq/bindings/`
  * `messagingjournal/` -> `activemq/journal/`
  * `messaginglargemessages/` -> `activemq/largemessages/`
  * `messagingpaging/` -> `activemq/paging/`

# Build Process

run the smoke test suite:

    cd testsuite/integration/smoke
    mvn test -Djmsoperations.implementation.class=org.jboss.as.test.integration.common.jms.ActiveMQProviderJMSOperations \
         -Djboss.server.config.file.name=standalone-full-activemq.xml

# Subsystem CLI

Configure a new messaging-activemq server from the CLI (similar to the one provided by `standalone-full-activemq.xml`):

    reload --admin-only=true
    /subsystem=messaging-activemq/server=default:add(journal-file-size=102400)
    /subsystem=messaging-activemq/server=default/security-setting=#:add()
    /subsystem=messaging-activemq/server=default/security-setting=#/role=guest:add(send=true, consume=true, create-non-durable-queue=true, delete-non-durable-queue=true)
    /subsystem=messaging-activemq/server=default/address-setting=#:add(dead-letter-address=jms.queue.DLQ, expiry-address=jms.queue.ExpiryQueue, max-size-bytes=10485760, page-size-bytes=2097152, message-counter-history-day-limit=10)
    /subsystem=messaging-activemq/server=default/http-connector=http-connector:add(socket-binding=http,  params=[http-upgrade-endpoint=>http-acceptor])
    /subsystem=messaging-activemq/server=default/in-vm-connector=in-vm:add(server-id=0)
    /subsystem=messaging-activemq/server=default/http-acceptor=http-acceptor:add(http-listener=default)
    /subsystem=messaging-activemq/server=default/in-vm-acceptor=in-vm:add(server-id=0)
    /subsystem=messaging-activemq/server=default/jms-queue=ExpiryQueue:add(entries=[java:/jms/queue/ExpiryQueue])
    /subsystem=messaging-activemq/server=default/jms-queue=DLQ:add(entries=[java:/jms/queue/DLQ])
    /subsystem=messaging-activemq/server=default/connection-factory=InVmConnectionFactory:add(connectors=[in-vm], entries=[java:/ConnectionFactory])
    /subsystem=messaging-activemq/server=default/connection-factory=RemoteConnectionFactory:add(connectors=[http-connector], entries=[java:jboss/exported/jms/RemoteConnectionFactory])
    /subsystem=messaging-activemq/server=default/pooled-connection-factory=activemq-ra:add(transaction=xa, connectors=[in-vm], entries=[java:/JmsXA java:jboss/DefaultJMSConnectionFactory])
    reload