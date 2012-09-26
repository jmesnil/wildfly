/*
* JBoss, Home of Professional Open Source.
* Copyright 2010, Red Hat, Inc., and individual contributors
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

import org.hornetq.api.core.BroadcastEndpoint;
import org.hornetq.api.core.BroadcastEndpointFactory;
import org.jgroups.JChannel;

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:andy.taylor@jboss.org">Andy Taylor</a>
 *         9/25/12
 */
public class JGroupsChannelEndpointFactory implements BroadcastEndpointFactory {
    private static Map<String, BroadcastEndpoint> sharedEndpoints = new HashMap<String, BroadcastEndpoint>();

    private final JChannel channel;

    private final String channelName;

    public JGroupsChannelEndpointFactory(JChannel channel, String channelName) {
        this.channel = channel;
        this.channelName = channelName;
    }

    public synchronized BroadcastEndpoint createBroadcastEndpoint() {
        BroadcastEndpoint point = sharedEndpoints.get(channelName);
        if (point != null) {
            return point;
        }
        return new JGroupsBroadcastEndpointWithChannel(channel, channelName);
    }
}
