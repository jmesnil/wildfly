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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.operations.common.Util.getEmptyOperation;
import static org.jboss.as.controller.parsing.ParseUtils.requireSingleAttribute;

import java.util.EnumSet;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLExtendedStreamReader;

/**
 * Messaging subsystem 3.0 XML parser.
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a>
 *
 */
public class Messaging30SubsystemParser extends Messaging20SubsystemParser {

    private static final Messaging30SubsystemParser INSTANCE = new Messaging30SubsystemParser();

    protected Messaging30SubsystemParser() {
    }

    public static MessagingSubsystemParser getInstance() {
        return INSTANCE;
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
    protected void handleUnknownAddressSetting(XMLExtendedStreamReader reader, Element element, ModelNode addressSettingsAdd) throws XMLStreamException {
        switch (element) {
            case MAX_REDELIVERY_DELAY:
            case REDELIVERY_MULTIPLIER:
                handleElementText(reader, element, addressSettingsAdd);
                break;
            default:
                super.handleUnknownAddressSetting(reader, element, addressSettingsAdd);
        }
    }

    private static final EnumSet<Element> HA_POLICY_ATTRIBUTES = EnumSet.noneOf(Element.class);

    static {
        for (AttributeDefinition attr : HAPolicyDefinition.ATTRIBUTES) {
            HA_POLICY_ATTRIBUTES.add(Element.forName(attr.getXmlName()));
        }
    }

    @Override
    protected void processHaPolicy(XMLExtendedStreamReader reader, ModelNode address, List<ModelNode> list) throws XMLStreamException {

        requireSingleAttribute(reader, "type");
        String type = reader.getAttributeValue(0);

        ModelNode haPolicyAddOperation = getEmptyOperation(ADD, address.clone().add(CommonAttributes.HA_POLICY, type));

        while(reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            String localName = reader.getLocalName();
            final Element element = Element.forName(localName);

            if (HA_POLICY_ATTRIBUTES.contains(element)) {
                handleElementText(reader, element, haPolicyAddOperation);
            } else {
                throw ParseUtils.unexpectedElement(reader);
            }
        }

        list.add(haPolicyAddOperation);
    }
}
