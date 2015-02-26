/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

import java.util.List;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.AttributeMarshaller;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;

/**
 * XML marshallers for messaging custom attributes.
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2012 Red Hat Inc.
 */
public final class AttributeMarshallers {

    public static final AttributeMarshaller JNDI_CONTEXT_MARSHALLER = new AttributeMarshaller() {
        public void marshallAsElement(AttributeDefinition attribute, ModelNode resourceModel, boolean marshallDefault, XMLStreamWriter writer) throws XMLStreamException {
            if (resourceModel.hasDefined(attribute.getName())) {
                ModelNode context = resourceModel.get(attribute.getName());

                writer.writeStartElement(attribute.getXmlName());
                for (Property property : context.asPropertyList()) {
                    writer.writeStartElement("property");
                    writer.writeAttribute("key", property.getName());
                    writer.writeAttribute("value", property.getValue().asString());
                    writer.writeEndElement();
                }
                writer.writeEndElement();
            }
        }
    };

    public static final AttributeMarshaller JNDI_RESOURCE_MARSHALLER = new AttributeMarshaller() {
        public void marshallAsElement(AttributeDefinition attribute, ModelNode resourceModel, boolean marshallDefault, XMLStreamWriter writer) throws XMLStreamException {
            if (resourceModel.hasDefined(attribute.getName())) {
                String name = resourceModel.get(attribute.getName()).asString();
                writer.writeEmptyElement(attribute.getXmlName());
                writer.writeAttribute("name", name);
            }
        }
    };


    public static final AttributeMarshaller INTERCEPTOR_MARSHALLER = new AttributeMarshaller() {
        @Override
        public void marshallAsElement(AttributeDefinition attribute, ModelNode resourceModel, boolean marshallDefault, XMLStreamWriter writer) throws XMLStreamException {
            if (resourceModel.hasDefined(attribute.getName())) {
                List<ModelNode> list = resourceModel.get(attribute.getName()).asList();
                if (list.size() > 0) {
                    writer.writeStartElement(attribute.getXmlName());

                    for (ModelNode child : list) {
                        writer.writeStartElement("class-name");
                        writer.writeCharacters(child.asString());
                        writer.writeEndElement();
                    }

                    writer.writeEndElement();
                }
            }
        }
    };
}
