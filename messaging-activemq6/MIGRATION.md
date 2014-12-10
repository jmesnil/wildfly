# Migration Guide

The goal of this guide is to help migration from messaging (with HornetQ) subsystem to the new messaging-activemq6 subsystem.

# Domain Model

* extension module: `org.jboss.as.messaging` -> `org.wildfly.extension.messaging-activemq6`
* server address: `/subsystem=messaging/hornetq-server=<name>` -> `/subsystem=messaging-activemq6/server=<name>`

# XML

* namespace:
  * `urn:jboss:domain:messaging:3.0` -> `urn:jboss:domain:messaging-activemq6:1.0`
  * `urn:jboss:messaging-deployment:1.0` -> `urn:jboss:messaging-activemq6-deployment:1.0`

# Logging

* prefix: `WFLYMSG` -> `WFLYMSGAMQ6`