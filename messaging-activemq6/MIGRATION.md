# Migration Guide

The goal of this guide is to help migration from messaging (with HornetQ) subsystem to the new messaging-activemq6 subsystem.

# Domain Model

* management model starts at `1.0.0`
  * there is __no__ resource transformation from the `messaging-activemq6` resources to the legacy `messaging` resources.
* extension module: `org.jboss.as.messaging` -> `org.wildfly.extension.messaging-activemq6`
* server address: `/subsystem=messaging/hornetq-server=<name>` -> `/subsystem=messaging-activemq6/server=<name>`

# XML

* namespace:
  * `urn:jboss:domain:messaging:3.0` -> `urn:jboss:domain:messaging-activemq6:1.0`
  * `urn:jboss:messaging-deployment:1.0` -> `urn:jboss:messaging-activemq6-deployment:1.0`

# Logging

* prefix: `WFLYMSG` -> `WFLYMSGAMQ6`

# Data

* relative to `jboss.server.data.dir`
  * `messagingbindings/` -> `activemq6/bindings/`
  * `messagingjournal/` -> `activemq6/journal/`
  * `messaginglargemessages/` -> `activemq6/largemessages/`
  * `messagingpaging/` -> `activemq6/paging/`

# Build Process

run the smoke test suite:

    cd testsuite/integration/smoke
    mvn test -Djmsoperations.implementation.class=org.jboss.as.test.integration.common.jms.DefaultActiveMQ6ProviderJMSOperations \
         -Djboss.server.config.file.name=standalone-full-activemq6.xml