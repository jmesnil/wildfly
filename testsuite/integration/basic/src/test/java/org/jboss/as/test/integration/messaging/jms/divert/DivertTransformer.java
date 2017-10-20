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

package org.jboss.as.test.integration.messaging.jms.divert;

import java.util.HashMap;
import java.util.Map;

import org.apache.activemq.artemis.api.core.Message;
import org.apache.activemq.artemis.api.core.SimpleString;
import org.apache.activemq.artemis.core.server.transformer.Transformer;
import org.apache.activemq.artemis.reader.TextMessageUtil;

/**
 * This transformer prepends a prefix (passed by properties) to a message text.
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2017 Red Hat inc.
 */
public class DivertTransformer implements Transformer {

    private Map<String, String> properties = new HashMap<>();

    public DivertTransformer() {
        System.out.println("DivertTransformer.DivertTransformer");
    }

    @Override
    public void init(Map<String, String> properties) {
        System.out.println("DivertTransformer.init");
        this.properties.putAll(properties);
    }

    @Override
    public Message transform(Message message) {
        System.out.println("DivertTransformer.transform");
        System.out.println("message = [" + message + "]");
        String prefix = properties.get("my-prefix");
        SimpleString text = TextMessageUtil.readBodyText(message.getBodyBuffer());
        SimpleString newText = SimpleString.toSimpleString(prefix + text);
        TextMessageUtil.writeBodyText(message.getBodyBuffer(), newText);
        return message;
    }
}
