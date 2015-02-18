/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

import static java.util.Arrays.asList;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.operations.common.Util.getEmptyOperation;
import static org.jboss.as.controller.parsing.ParseUtils.missingRequired;
import static org.jboss.as.controller.parsing.ParseUtils.readStringAttributeElement;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoContent;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedElement;
import static org.wildfly.extension.messaging.activemq.Attribute.HTTP_LISTENER;
import static org.wildfly.extension.messaging.activemq.Attribute.SOCKET_BINDING;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.ACCEPTOR;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.CONFIGURATION;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.CONNECTOR;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.HA_POLICY;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.HTTP_ACCEPTOR;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.HTTP_CONNECTOR;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.IN_VM_ACCEPTOR;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.IN_VM_CONNECTOR;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.LIVE_ONLY;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.MASTER;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.REMOTE_ACCEPTOR;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.REMOTE_CONNECTOR;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.REPLICATION_COLOCATED;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.REPLICATION_MASTER;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.REPLICATION_SLAVE;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.SHARED_STORE_COLOCATED;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.SHARED_STORE_MASTER;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.SHARED_STORE_SLAVE;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.SLAVE;
import static org.wildfly.extension.messaging.activemq.Element.DISCOVERY_GROUP_REF;
import static org.wildfly.extension.messaging.activemq.Element.STATIC_CONNECTORS;
import static org.wildfly.extension.messaging.activemq.ha.ScaleDownAttributes.SCALE_DOWN_CONNECTORS;
import static org.wildfly.extension.messaging.activemq.ha.ScaleDownAttributes.SCALE_DOWN_DISCOVERY_GROUP_NAME;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.parsing.ParseUtils;
import org.wildfly.extension.messaging.activemq.ha.HAAttributes;
import org.wildfly.extension.messaging.activemq.ha.ScaleDownAttributes;
import org.wildfly.extension.messaging.activemq.logging.MessagingLogger;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLExtendedStreamReader;


/**
 * Messaging subsystem 1.3 XML parser.
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a>
 *
 */
public class Messaging13SubsystemParser extends Messaging12SubsystemParser {

    private static final Messaging13SubsystemParser INSTANCE = new Messaging13SubsystemParser();

    public static MessagingSubsystemParser getInstance() {
        return INSTANCE;
    }

    protected Messaging13SubsystemParser() {
    }

    @Override
    protected void checkClusterConnectionConstraints(XMLExtendedStreamReader reader, Set<Element> seen) throws XMLStreamException {
        // AS7-5598 relax constraints on the cluster-connection to accept one without static-connectors or discovery-group-ref
        // however it is still not valid to have both
        checkNotBothElements(reader, seen, STATIC_CONNECTORS, DISCOVERY_GROUP_REF);
    }

    @Override
    protected void checkBroadcastGroupConstraints(XMLExtendedStreamReader reader, Set<Element> seen) throws XMLStreamException {
        checkNotBothElements(reader, seen, Element.SOCKET_BINDING, Element.JGROUPS_STACK);
    }

    @Override
    protected void checkDiscoveryGroupConstraints(XMLExtendedStreamReader reader, Set<Element> seen) throws XMLStreamException {
        checkNotBothElements(reader, seen, Element.SOCKET_BINDING, Element.JGROUPS_STACK);
    }

    protected void handleUnknownConnectionFactoryAttribute(XMLExtendedStreamReader reader, Element element, ModelNode connectionFactory, boolean pooled)
            throws XMLStreamException {
        switch (element) {
            case CALL_FAILOVER_TIMEOUT:
            case COMPRESS_LARGE_MESSAGES:
                handleElementText(reader, element, connectionFactory);
                break;
            case USE_AUTO_RECOVERY:
            case INITIAL_MESSAGE_PACKET_SIZE:
                if (!pooled) {
                    throw unexpectedElement(reader);
                }
                handleElementText(reader, element, connectionFactory);
                break;
            case INITIAL_CONNECT_ATTEMPTS:
                if (!pooled) {
                    throw unexpectedElement(reader);
                }
                handleElementText(reader, element, "pooled", connectionFactory);
                break;
            default: {
                super.handleUnknownConnectionFactoryAttribute(reader, element, connectionFactory, pooled);
            }
        }
    }

    @Override
    protected void handleComplexConfigurationAttribute(XMLExtendedStreamReader reader, Element element, ModelNode operation) throws XMLStreamException {
        switch (element) {
            case REMOTING_INCOMING_INTERCEPTORS:
                processRemotingIncomingInterceptors(reader, operation);
                break;
            case REMOTING_OUTGOING_INTERCEPTORS:
                processRemotingOutgoingInterceptors(reader, operation);
                break;
            default: {
                super.handleComplexConfigurationAttribute(reader, element, operation);
            }
        }
    }

    @Override
    protected void handleUnknownBroadcastGroupAttribute(XMLExtendedStreamReader reader, Element element, ModelNode operation)
            throws XMLStreamException {
        switch (element) {
            case JGROUPS_STACK:
            case JGROUPS_CHANNEL:
                handleElementText(reader, element, operation);
                break;
            default: {
                super.handleUnknownBroadcastGroupAttribute(reader, element, operation);
            }
        }
    }

    @Override
    protected void handleUnknownDiscoveryGroupAttribute(XMLExtendedStreamReader reader, Element element, ModelNode operation)
            throws XMLStreamException {
        switch (element) {
            case JGROUPS_STACK:
            case JGROUPS_CHANNEL:
                handleElementText(reader, element, operation);
                break;
            default: {
                super.handleUnknownDiscoveryGroupAttribute(reader, element, operation);
            }
        }
    }

    /**
     * [AS7-5808] Support space-separated roles names for backwards compatibility and comma-separated ones for compatibility with
     * HornetQ configuration.
     *
     * Roles are persisted using space character delimiter in {@link MessagingXMLWriter}.
     */
    @Override
    protected List<String> parseRolesAttribute(XMLExtendedStreamReader reader, int index) throws XMLStreamException {
        String roles = reader.getAttributeValue(index);
        return asList(roles.split("[,\\s]+"));
    }

    /**
     * Check that not both elements have been defined
     */
    protected static void checkNotBothElements(XMLExtendedStreamReader reader, Set<Element> seen, Element element1, Element element2) throws XMLStreamException {
        if (seen.contains(element1) && seen.contains(element2)) {
            throw new XMLStreamException(MessagingLogger.ROOT_LOGGER.onlyOneRequired(element1.getLocalName(), element2.getLocalName()), reader.getLocation());
        }
    }

    @Override
    protected void handleUnknownGroupingHandlerAttribute(XMLExtendedStreamReader reader, Element element, ModelNode operation) throws XMLStreamException {
        switch(element) {
            case GROUP_TIMEOUT:
            case REAPER_PERIOD:
                handleElementText(reader, element, operation);
                break;
            default:
                super.handleUnknownGroupingHandlerAttribute(reader, element, operation);
        }
    }

    @Override
    void processConnectors(final XMLExtendedStreamReader reader, final ModelNode address, final List<ModelNode> updates) throws XMLStreamException {
        while(reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            String name = null;
            String socketBinding = null;
            String serverId = null;

            int count = reader.getAttributeCount();
            for (int i = 0; i < count; i++) {
                final String attrValue = reader.getAttributeValue(i);
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case NAME: {
                        name = attrValue;
                        break;
                    } case SOCKET_BINDING: {
                        socketBinding = attrValue;
                        break;
                    } case SERVER_ID: {
                        serverId = attrValue;
                        break;
                    } default: {
                        throw ParseUtils.unexpectedAttribute(reader, i);
                    }
                }
            }
            if(name == null) {
                throw missingRequired(reader, Collections.singleton(Attribute.NAME));
            }

            final ModelNode connectorAddress = address.clone();
            final ModelNode operation = new ModelNode();
            operation.get(OP).set(ADD);

            boolean generic = false;
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case CONNECTOR: {
                    operation.get(OP_ADDR).set(connectorAddress.add(CONNECTOR, name));
                    if (socketBinding != null) {
                        operation.get(RemoteTransportDefinition.SOCKET_BINDING.getName()).set(socketBinding);
                    }
                    generic = true;
                    break;
                } case NETTY_CONNECTOR: {
                    operation.get(OP_ADDR).set(connectorAddress.add(REMOTE_CONNECTOR, name));
                    if (socketBinding == null) {
                        throw missingRequired(reader, Collections.singleton(Attribute.SOCKET_BINDING));
                    }
                    operation.get(RemoteTransportDefinition.SOCKET_BINDING.getName()).set(socketBinding);
                    break;
                } case IN_VM_CONNECTOR: {
                    operation.get(OP_ADDR).set(connectorAddress.add(IN_VM_CONNECTOR, name));
                    if (serverId != null) {
                        InVMTransportDefinition.SERVER_ID.parseAndSetParameter(serverId, operation, reader);
                    }
                    break;
                } case HTTP_CONNECTOR: {
                    if (socketBinding == null) {
                        throw missingRequired(reader, Collections.singleton(SOCKET_BINDING));
                    }
                    operation.get(OP_ADDR).set(connectorAddress.add(HTTP_CONNECTOR, name));
                    HTTPConnectorDefinition.SOCKET_BINDING.parseAndSetParameter(socketBinding, operation, reader);
                    break;
                } default: {
                    throw unexpectedElement(reader);
                }
            }

            updates.add(operation);
            parseTransportConfiguration(reader, operation, generic, updates);
        }
    }

    @Override
    void processAcceptors(XMLExtendedStreamReader reader, ModelNode address, List<ModelNode> updates) throws XMLStreamException {
        while(reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            String name = null;
            String socketBinding = null;
            String serverId = null;
            String httpListener = null;

            int count = reader.getAttributeCount();
            for (int i = 0; i < count; i++) {
                final String attrValue = reader.getAttributeValue(i);
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case NAME: {
                        name = attrValue;
                        break;
                    } case SOCKET_BINDING: {
                        socketBinding = attrValue;
                        break;
                    } case SERVER_ID: {
                        serverId = attrValue;
                        break;
                    } case HTTP_LISTENER: {
                        httpListener = attrValue;
                        break;
                    } default: {
                        throw ParseUtils.unexpectedAttribute(reader, i);
                    }
                }
            }
            if(name == null) {
                throw ParseUtils.missingRequired(reader, Collections.singleton(Attribute.NAME));
            }

            final ModelNode acceptorAddress = address.clone();
            final ModelNode operation = new ModelNode();
            operation.get(OP).set(ADD);

            final Element element = Element.forName(reader.getLocalName());
            boolean generic = false;
            switch (element) {
                case ACCEPTOR: {
                    operation.get(OP_ADDR).set(acceptorAddress.add(ACCEPTOR, name));
                    if(socketBinding != null) {
                        operation.get(RemoteTransportDefinition.SOCKET_BINDING.getName()).set(socketBinding);
                    }
                    generic = true;
                    break;
                } case NETTY_ACCEPTOR: {
                    operation.get(OP_ADDR).set(acceptorAddress.add(REMOTE_ACCEPTOR, name));
                    if(socketBinding == null) {
                        throw ParseUtils.missingRequired(reader, Collections.singleton(Attribute.SOCKET_BINDING));
                    }
                    operation.get(RemoteTransportDefinition.SOCKET_BINDING.getName()).set(socketBinding);
                    break;
                } case IN_VM_ACCEPTOR: {
                    operation.get(OP_ADDR).set(acceptorAddress.add(IN_VM_ACCEPTOR, name));
                    if (serverId != null) {
                        InVMTransportDefinition.SERVER_ID.parseAndSetParameter(serverId, operation, reader);
                    }
                    break;
                } case HTTP_ACCEPTOR: {
                    if (httpListener == null) {
                        throw missingRequired(reader, Collections.singleton(HTTP_LISTENER));
                    }
                    operation.get(OP_ADDR).set(acceptorAddress.add(HTTP_ACCEPTOR, name));
                    HTTPAcceptorDefinition.HTTP_LISTENER.parseAndSetParameter(httpListener, operation, reader);
                    break;
                } default: {
                    throw unexpectedElement(reader);
                }
            }

            updates.add(operation);
            parseTransportConfiguration(reader, operation, generic, updates);
        }
    }

    @Override
    protected void handleUnknownBridgeAttribute(XMLExtendedStreamReader reader, Element element, ModelNode bridgeAdd) throws XMLStreamException {
        switch (element) {
            case RECONNECT_ATTEMPTS_ON_SAME_NODE:
                handleElementText(reader, element, bridgeAdd);
                break;
            case INITIAL_CONNECT_ATTEMPTS:
                handleElementText(reader, element, "bridge", bridgeAdd);
                break;
            default:
                super.handleUnknownBridgeAttribute(reader, element, bridgeAdd);
        }
    }

    @Override
    protected void handleUnknownAddressSetting(XMLExtendedStreamReader reader, Element element, ModelNode addressSettingsAdd) throws XMLStreamException {
        switch (element) {
            case EXPIRY_DELAY:
            case MAX_REDELIVERY_DELAY:
            case REDELIVERY_MULTIPLIER:
                handleElementText(reader, element, addressSettingsAdd);
                break;
            default:
                super.handleUnknownAddressSetting(reader, element, addressSettingsAdd);
        }
    }

    @Override
    protected void handleUnknownConfigurationAttribute(XMLExtendedStreamReader reader, Element element, ModelNode operation) throws XMLStreamException {
        switch (element) {
            case OVERRIDE_IN_VM_SECURITY:
                handleElementText(reader, element, operation);
                break;
            default: {
                super.handleUnknownConfigurationAttribute(reader, element, operation);
            }
        }
    }

    @Override
    protected void processHaPolicy(XMLExtendedStreamReader reader, ModelNode address, List<ModelNode> list) throws XMLStreamException {

        while(reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            String localName = reader.getLocalName();
            final Element element = Element.forName(localName);

            switch (element) {
                case LIVE_ONLY:
                    procesHaPolicyLiveOnly(reader, address, list);
                    break;
                case REPLICATION:
                    procesHaPolicyReplication(reader, address, list);
                    break;
                case SHARED_STORE:
                    processHAPolicySharedStore(reader, address, list);
                    break;
                default:
                    throw unexpectedElement(reader);
            }
        }
    }

    private void processHAPolicySharedStore(XMLExtendedStreamReader reader, ModelNode address, List<ModelNode> list) throws XMLStreamException {
        while(reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            String localName = reader.getLocalName();
            final Element element = Element.forName(localName);

            switch (element) {
                case MASTER:
                    processHAPolicySharedStoreMaster(reader, address.clone().add(HA_POLICY, SHARED_STORE_MASTER), list);
                    break;
                case SLAVE:
                    processHAPolicySharedStoreSlave(reader, address.clone().add(HA_POLICY, SHARED_STORE_SLAVE), list);
                    break;
                case COLOCATED:
                    processHAPolicySharedStoreColocated(reader, address.clone().add(HA_POLICY, SHARED_STORE_COLOCATED), list);
                    break;
                default:
                    throw unexpectedElement(reader);
            }
        }
    }

    private void processHAPolicySharedStoreMaster(XMLExtendedStreamReader reader, ModelNode address, List<ModelNode> list) throws XMLStreamException {
        ModelNode operation = getEmptyOperation(ADD, address);

        int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final String attrValue = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case FAILBACK_DELAY: {
                    HAAttributes.FAILBACK_DELAY.parseAndSetParameter(attrValue, operation, reader);
                    break;
                }
                case FAILOVER_ON_SERVER_SHUTDOWN: {
                    HAAttributes.FAILOVER_ON_SERVER_SHUTDOWN.parseAndSetParameter(attrValue, operation, reader);
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }

        requireNoContent(reader);

        list.add(operation);
    }

    private void processHAPolicySharedStoreSlave(XMLExtendedStreamReader reader, ModelNode address, List<ModelNode> list) throws XMLStreamException {
        ModelNode operation = getEmptyOperation(ADD, address);

        int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final String attrValue = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case ALLOW_FAILBACK: {
                    HAAttributes.ALLOW_FAILBACK.parseAndSetParameter(attrValue, operation, reader);
                    break;
                }
                case FAILBACK_DELAY: {
                    HAAttributes.FAILBACK_DELAY.parseAndSetParameter(attrValue, operation, reader);
                    break;
                }
                case FAILOVER_ON_SERVER_SHUTDOWN: {
                    HAAttributes.FAILOVER_ON_SERVER_SHUTDOWN.parseAndSetParameter(attrValue, operation, reader);
                    break;
                }
                case RESTART_BACKUP: {
                    HAAttributes.RESTART_BACKUP.parseAndSetParameter(attrValue, operation, reader);
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }

        while(reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            String localName = reader.getLocalName();
            final Element element = Element.forName(localName);

            switch (element) {
                case SCALE_DOWN:
                    processScaleDown(reader, operation);
                    break;
                default:
                    throw unexpectedElement(reader);
            }
        }

        list.add(operation);
    }

    private void procesHaPolicyLiveOnly(XMLExtendedStreamReader reader, ModelNode address, List<ModelNode> list) throws XMLStreamException {
        ModelNode haPolicyAdd = getEmptyOperation(ADD, address.clone().add(HA_POLICY, LIVE_ONLY));

        while(reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            String localName = reader.getLocalName();
            final Element element = Element.forName(localName);

            switch (element) {
                case SCALE_DOWN:
                    processScaleDown(reader, haPolicyAdd);
                    break;
                default:
                    throw unexpectedElement(reader);
            }
        }

        list.add(haPolicyAdd);

    }

    private void procesHaPolicyReplication(XMLExtendedStreamReader reader, ModelNode address, List<ModelNode> list) throws XMLStreamException {
        while(reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            String localName = reader.getLocalName();
            final Element element = Element.forName(localName);

            switch (element) {
                case MASTER:
                    procesHaPolicyReplicationMaster(reader, address.clone().add(HA_POLICY, REPLICATION_MASTER), list);
                    break;
                case SLAVE:
                    procesHaPolicyReplicationSlave(reader, address.clone().add(HA_POLICY, REPLICATION_SLAVE), list);
                    break;
                case COLOCATED:
                    procesHaPolicyReplicationColocation(reader, address.clone().add(HA_POLICY, REPLICATION_COLOCATED), list);
                    break;
                default:
                    throw unexpectedElement(reader);
            }
        }
    }

    private void procesHaPolicyReplicationMaster(XMLExtendedStreamReader reader, ModelNode address, List<ModelNode> list) throws XMLStreamException {
        ModelNode operation = getEmptyOperation(ADD, address);

        int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final String attrValue = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case GROUP_NAME: {
                    HAAttributes.GROUP_NAME.parseAndSetParameter(attrValue, operation, reader);
                    break;
                }
                case CLUSTER_NAME: {
                    HAAttributes.CLUSTER_NAME.parseAndSetParameter(attrValue, operation, reader);
                    break;                }
                case CHECK_FOR_LIVE_SERVER: {
                    HAAttributes.CHECK_FOR_LIVE_SERVER.parseAndSetParameter(attrValue, operation, reader);
                    break;
                } default: {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }

        requireNoContent(reader);

        list.add(operation);
    }

    private void procesHaPolicyReplicationSlave(XMLExtendedStreamReader reader, ModelNode address, List<ModelNode> list) throws XMLStreamException {
        ModelNode operation = getEmptyOperation(ADD, address);

        int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final String attrValue = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case GROUP_NAME: {
                    HAAttributes.GROUP_NAME.parseAndSetParameter(attrValue, operation, reader);
                    break;
                }
                case CLUSTER_NAME: {
                    HAAttributes.CLUSTER_NAME.parseAndSetParameter(attrValue, operation, reader);
                    break;
                }
                case ALLOW_FAILBACK: {
                    HAAttributes.ALLOW_FAILBACK.parseAndSetParameter(attrValue, operation, reader);
                    break;
                }
                case RESTART_BACKUP: {
                    HAAttributes.RESTART_BACKUP.parseAndSetParameter(attrValue, operation, reader);
                    break;
                }
                case FAILBACK_DELAY: {
                    HAAttributes.FAILBACK_DELAY.parseAndSetParameter(attrValue, operation, reader);
                    break;
                }
                case MAX_SAVED_REPLICATED_JOURNAL_SIZE: {
                    HAAttributes.MAX_SAVED_REPLICATED_JOURNAL_SIZE.parseAndSetParameter(attrValue, operation, reader);
                    break;
                } default: {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }

        while(reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            String localName = reader.getLocalName();
            final Element element = Element.forName(localName);

            switch (element) {
                case SCALE_DOWN:
                    processScaleDown(reader, operation);
                    break;
                default:
                    throw unexpectedElement(reader);
            }
        }

        list.add(operation);
    }

    private void procesHaPolicyReplicationColocation(XMLExtendedStreamReader reader, ModelNode address, List<ModelNode> list) throws XMLStreamException {
        ModelNode operation = getEmptyOperation(ADD, address);

        int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final String attrValue = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case REQUEST_BACKUP: {
                    HAAttributes.REQUEST_BACKUP.parseAndSetParameter(attrValue, operation, reader);
                    break;
                }
                case BACKUP_PORT_OFFSET: {
                    HAAttributes.BACKUP_PORT_OFFSET.parseAndSetParameter(attrValue, operation, reader);
                    break;
                }
                case BACKUP_REQUEST_RETRIES: {
                    HAAttributes.BACKUP_REQUEST_RETRIES.parseAndSetParameter(attrValue, operation, reader);
                    break;
                }
                case BACKUP_REQUEST_RETRY_INTERVAL: {
                    HAAttributes.BACKUP_REQUEST_RETRY_INTERVAL.parseAndSetParameter(attrValue, operation, reader);
                    break;
                }
                case MAX_BACKUPS: {
                    HAAttributes.MAX_BACKUPS.parseAndSetParameter(attrValue, operation, reader);
                    break;
                } default: {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }

        list.add(operation);

        while(reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            String localName = reader.getLocalName();
            final Element element = Element.forName(localName);

            switch (element) {
                case EXCLUDES:
                    processExcludedConnectors(reader, operation);
                    break;
                case MASTER:
                    procesHaPolicyReplicationMaster(reader, address.clone().add(CONFIGURATION, MASTER), list);
                    break;
                case SLAVE:
                    procesHaPolicyReplicationSlave(reader, address.clone().add(CONFIGURATION, SLAVE), list);
                    break;
                default:
                    throw unexpectedElement(reader);
            }
        }
    }

    private void processHAPolicySharedStoreColocated(XMLExtendedStreamReader reader, ModelNode address, List<ModelNode> list) throws XMLStreamException {
        ModelNode operation = getEmptyOperation(ADD, address);

        int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final String attrValue = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {

                case REQUEST_BACKUP: {
                    HAAttributes.REQUEST_BACKUP.parseAndSetParameter(attrValue, operation, reader);
                    break;
                }
                case BACKUP_PORT_OFFSET: {
                    HAAttributes.BACKUP_PORT_OFFSET.parseAndSetParameter(attrValue, operation, reader);
                    break;
                }
                case BACKUP_REQUEST_RETRIES: {
                    HAAttributes.BACKUP_REQUEST_RETRIES.parseAndSetParameter(attrValue, operation, reader);
                    break;
                }
                case BACKUP_REQUEST_RETRY_INTERVAL: {
                    HAAttributes.BACKUP_REQUEST_RETRY_INTERVAL.parseAndSetParameter(attrValue, operation, reader);
                    break;
                }
                case MAX_BACKUPS: {
                    HAAttributes.MAX_BACKUPS.parseAndSetParameter(attrValue, operation, reader);
                    break;
                } default: {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }

        list.add(operation);

        while(reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            String localName = reader.getLocalName();
            final Element element = Element.forName(localName);

            switch (element) {
                case MASTER:
                    processHAPolicySharedStoreMaster(reader, address.clone().add(CONFIGURATION, MASTER), list);
                    break;
                case SLAVE:
                    processHAPolicySharedStoreSlave(reader, address.clone().add(CONFIGURATION, SLAVE), list);
                    break;
                default:
                    throw unexpectedElement(reader);
            }
        }
    }

    private void processExcludedConnectors(XMLExtendedStreamReader reader, ModelNode operation) throws XMLStreamException {
        while(reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            String localName = reader.getLocalName();
            final Element element = Element.forName(localName);

            switch (element) {
                case CONNECTORS:
                    operation.get(HAAttributes.EXCLUDED_CONNECTORS.getName()).set(processJmsConnectors(reader));
                    break;
                default:
                    throw unexpectedElement(reader);
            }
        }
    }

    private void processScaleDown(XMLExtendedStreamReader reader, ModelNode operation) throws XMLStreamException {
        int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final String attrValue = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case ENABLED: {
                    ScaleDownAttributes.SCALE_DOWN.parseAndSetParameter(attrValue, operation, reader);
                    break;
                }
                case GROUP_NAME: {
                    ScaleDownAttributes.SCALE_DOWN_GROUP_NAME.parseAndSetParameter(attrValue, operation, reader);
                    break;
                }
                case CLUSTER_NAME: {
                    ScaleDownAttributes.SCALE_DOWN_CLUSTER_NAME.parseAndSetParameter(attrValue, operation, reader);
                    break;
                } default: {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }

        while(reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            Set<Element> seen = EnumSet.noneOf(Element.class);

            String localName = reader.getLocalName();
            final Element element = Element.forName(localName);
            if (!seen.add(element)) {
                throw ParseUtils.duplicateNamedElement(reader, element.getLocalName());
            }

            switch (element) {
                case DISCOVERY_GROUP_REF: {
                    checkOtherElementIsNotAlreadyDefined(reader, seen, DISCOVERY_GROUP_REF, Element.CONNECTORS);
                    final String attrValue = readStringAttributeElement(reader, SCALE_DOWN_DISCOVERY_GROUP_NAME.getXmlName());
                    SCALE_DOWN_DISCOVERY_GROUP_NAME.parseAndSetParameter(attrValue, operation, reader);
                    break;
                } case CONNECTORS: {
                    checkOtherElementIsNotAlreadyDefined(reader, seen, Element.CONNECTORS, DISCOVERY_GROUP_REF);
                    operation.get(SCALE_DOWN_CONNECTORS.getName()).set(processJmsConnectors(reader));
                    break;
                }
                default:
                    throw unexpectedElement(reader);
            }
        }
    }
}
