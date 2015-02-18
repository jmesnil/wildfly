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
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedElement;
import static org.wildfly.extension.messaging.activemq.Element.DISCOVERY_GROUP_REF;
import static org.wildfly.extension.messaging.activemq.Element.STATIC_CONNECTORS;

import java.util.List;
import java.util.Set;

import javax.xml.stream.XMLStreamException;

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
    protected void handleUnknownClusterConnectionAttribute(XMLExtendedStreamReader reader, Element element, ModelNode clusterConnectionAdd)
            throws XMLStreamException {
        switch (element) {
            case CALL_FAILOVER_TIMEOUT:
            case NOTIFICATION_ATTEMPTS:
            case NOTIFICATION_INTERVAL:
                handleElementText(reader, element, clusterConnectionAdd);
                break;
            default: {
                super.handleUnknownClusterConnectionAttribute(reader, element, clusterConnectionAdd);
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
}
