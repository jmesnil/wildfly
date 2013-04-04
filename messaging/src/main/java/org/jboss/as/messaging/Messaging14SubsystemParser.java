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

package org.jboss.as.messaging;

import static org.jboss.as.controller.parsing.ParseUtils.unexpectedAttribute;
import static org.jboss.as.messaging.CommonAttributes.HTTP_CONNECTOR;
import static org.jboss.as.messaging.CommonAttributes.PARAM;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLExtendedStreamReader;

/**
 * Messaging subsystem 1.4 XML parser.
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a>
 *
 */
public class Messaging14SubsystemParser extends Messaging13SubsystemParser {

    private static final Messaging14SubsystemParser INSTANCE = new Messaging14SubsystemParser();

    public static MessagingSubsystemParser getInstance() {
        return INSTANCE;
    }

    protected Messaging14SubsystemParser() {
    }

    @Override
    protected void handleUnknownConnector(XMLExtendedStreamReader reader, Element element, ModelNode operation, ModelNode connectorAddress, String name, String socketBinding) throws XMLStreamException {
        switch (element) {
            case HTTP_CONNECTOR:
                connectorAddress.add(HTTP_CONNECTOR, name);
                HttpConnectorDefinition.SOCKET_BINDING.parseAndSetParameter(socketBinding, operation, reader);
                parseHttpConnectorConfiguration(reader, operation);
                break;
            default: {
                super.handleUnknownConfigurationAttribute(reader, element, connectorAddress);
            }
        }
    }

    private void parseHttpConnectorConfiguration(final XMLExtendedStreamReader reader, final ModelNode operation) throws XMLStreamException {
        while(reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            Element element = Element.forName(reader.getLocalName());
            switch(element) {
                case PARAM: {
                    int count = reader.getAttributeCount();
                    String key = null;
                    String value = null;
                    for (int n = 0; n < count; n++) {
                        String attrName = reader.getAttributeLocalName(n);
                        Attribute attribute = Attribute.forName(attrName);
                        switch (attribute) {
                            case KEY:
                                key = reader.getAttributeValue(n);
                                break;
                            case VALUE:
                                value = reader.getAttributeValue(n);
                                break;
                            default:
                                throw unexpectedAttribute(reader, n);
                        }
                    }
                    operation.get(PARAM).add(key, TransportParamDefinition.VALUE.parse(value, reader));
                    ParseUtils.requireNoContent(reader);
                    break;
                }
                case HOST: {
                    handleElementText(reader, element, operation);
                    break;
                } default: {
                    throw ParseUtils.unexpectedElement(reader);
                }
            }
        }
    }
}
