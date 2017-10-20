/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

import static java.util.Collections.emptyMap;
import static org.jboss.as.controller.SimpleAttributeDefinitionBuilder.create;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.PROPERTIES;

import java.util.Map;

import org.apache.activemq.artemis.core.server.transformer.Transformer;
import org.apache.activemq.artemis.utils.ClassloadingUtil;
import org.jboss.as.controller.ObjectTypeAttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PropertiesAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.extension.messaging.activemq.logging.MessagingLogger;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2017 Red Hat inc.
 */
public class TransformerUtil {

    /**
     * DMR representation of a {@link Transformer} implementation.
     */
    static final ObjectTypeAttributeDefinition TRANSFORMER_CLASS = ObjectTypeAttributeDefinition.Builder.of(CommonAttributes.TRANSFORMER_CLASS,
            create(CommonAttributes.NAME, ModelType.STRING, false)
                    .setAllowExpression(false)
                    .build(),
            create(CommonAttributes.MODULE, ModelType.STRING, false)
                    .setAllowExpression(false)
                    .build(),
            new PropertiesAttributeDefinition.Builder(PROPERTIES, true)
                    .setAllowExpression(true)
                    .build())
            .setAlternatives(CommonAttributes.TRANSFORMER_CLASS_NAME)
            .build();

    @Deprecated
    static final SimpleAttributeDefinition TRANSFORMER_CLASS_NAME = create(CommonAttributes.TRANSFORMER_CLASS_NAME, ModelType.STRING)
            .setRequired(false)
            .setAllowExpression(false)
            .setAlternatives(CommonAttributes.TRANSFORMER_CLASS)
            .setRestartAllServices()
            .setDeprecated(MessagingExtension.VERSION_3_0_0)
            .build();

    /**
     * Load a Transformer instance.
     *
     * The resourceModel can either contain:
     * * (deprecated) @{code TRANSFORMER_CLASS_NAME} String attribute
     *
     * or
     *
     * * a complex {@code TRANSFORMER_CLASS} attribute that itself contains:
     * ** name (String attribute that corresponds to the name of the Transformer implementation)
     * ** module (String module that corresponds to the module that contains the transformer implementation)
     * ** properties (optional Properties use to initialize the Transformer instance)
     *
     * The method can return {@code null} if the resourceModel does not define any of these attributes
     * (as the transformer is optional for the given resource).
     */
    static Transformer loadTransformer(OperationContext context, ModelNode resourceModel) throws OperationFailedException {
        if (resourceModel.hasDefined(CommonAttributes.TRANSFORMER_CLASS_NAME)) {
            String className = resourceModel.get(CommonAttributes.TRANSFORMER_CLASS_NAME).asString();
            try {
                Object o = ClassloadingUtil.newInstanceFromClassLoader(className);
                Transformer transformer = Transformer.class.cast(o);
                transformer.init(emptyMap());
                return transformer;
            } catch (Throwable t) {
                throw MessagingLogger.ROOT_LOGGER.unableToLoadClass(className);
            }
        } else if (resourceModel.hasDefined(CommonAttributes.TRANSFORMER_CLASS)){
            Object o = ClassLoaderUtil.instantiate(resourceModel.require(CommonAttributes.TRANSFORMER_CLASS));
            Transformer transformer = Transformer.class.cast(o);
            Map<String, String> properties = PropertiesAttributeDefinition.unwrapModel(context, resourceModel.get(CommonAttributes.TRANSFORMER_CLASS, PROPERTIES));
            transformer.init(properties);
            return transformer;
        } else {
            return null;
        }
    }
}
