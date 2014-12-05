package org.jboss.as.messaging;

import java.security.AccessController;

import org.apache.activemq.core.server.ActiveMQServer;
import org.jboss.as.server.CurrentServiceContainer;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jgroups.JChannel;


public class JGroupsChannelLocator {
    /**
     * Callback used by HornetQ to locate a JChannel instance corresponding to the channel name
     * passed through the HornetQ RA property {@code channelRefName}
     */
    public JChannel locateChannel(String channelRefName) {
        String[] split = channelRefName.split("/");
        String hornetQServerName = split[0];
        String channelName = split[1];
        ServiceController<ActiveMQServer> controller = (ServiceController<ActiveMQServer>) currentServiceContainer().getService(MessagingServices.getHornetQServiceName(hornetQServerName));
        HornetQService service = (HornetQService) controller.getService();
        return service.getChannels().get(channelName);
    }

    private static ServiceContainer currentServiceContainer() {
        if(System.getSecurityManager() == null) {
            return CurrentServiceContainer.getServiceContainer();
        }
        return AccessController.doPrivileged(CurrentServiceContainer.GET_ACTION);
    }
}
