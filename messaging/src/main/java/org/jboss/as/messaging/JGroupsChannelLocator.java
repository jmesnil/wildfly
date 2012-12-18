package org.jboss.as.messaging;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.jboss.as.clustering.jgroups.ChannelFactory;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jgroups.JChannel;

public class JGroupsChannelLocator {

    static volatile Map<String, JChannel> channels = new HashMap<String, JChannel>();

    /**
     * Callback used by HornetQ to locate a JChannel instance corresponding to the channel name
     * passed through the HornetQ RA property {@code channelRefName}
     */
    public JChannel locateChannel(String channelRefName) {
        return channels.get(channelRefName);
    }

    static void putChannel(String channelName, JChannel channel) {
        channels.put(channelName, channel);
    }

    static void removeChannels() {
        channels.clear();
    }
}
