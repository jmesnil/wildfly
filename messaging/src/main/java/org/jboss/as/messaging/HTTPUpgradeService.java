/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

import static org.jboss.as.messaging.CommonAttributes.CORE;
import static org.jboss.as.messaging.HandshakeUtil.createHttpUpgradeHandshake;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

import io.undertow.server.ListenerRegistry;
import io.undertow.server.handlers.ChannelUpgradeHandler;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.remoting.HttpListenerRegistryService;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.xnio.ChannelListener;
import org.xnio.StreamConnection;
import org.xnio.conduits.ConduitStreamSourceChannel;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2013 Red Hat inc.
 */
public class HTTPUpgradeService implements Service<HTTPUpgradeService> {

    // same magic number than jboss-remoting. Might be better to change it?
    public static final String MAGIC_NUMBER = "CF70DEB8-70F9-4FBA-8B4F-DFC3E723B4CD";

    //headers
    public static final String SEC_HORNETQ_REMOTING_KEY = "Sec-HornetQRemoting-Key";
    public static final String SEC_HORNETQ_REMOTING_ACCEPT= "Sec-HornetQRemoting-Accept";

    public static final ServiceName HTTP_UPGRADE_REGISTRY = ServiceName.JBOSS.append("http-upgrade-registry");
    public static final ServiceName UPGRADE_SERVICE_NAME = MessagingServices.JBOSS_MESSAGING.append("messaging-http-upgrade-service");

    public static final String HORNETQ_REMOTING = "hornetq-remoting";

    private final String hornetQServerName;
    private final String httpConnectorName;
    private InjectedValue<ChannelUpgradeHandler> injectedRegistry = new InjectedValue<>();
    private InjectedValue<ListenerRegistry> listenerRegistry = new InjectedValue<>();

    private ListenerRegistry.HttpUpgradeMetadata httpUpgradeMetadata;

    public HTTPUpgradeService(String hornetQServerName, String httpConnectorName) {
        this.hornetQServerName = hornetQServerName;
        this.httpConnectorName = httpConnectorName;
    }

    public static void installService(final ServiceTarget serviceTarget, String hornetQServerName, final String messagingConnectorName, final String httpConnectorName, final ServiceVerificationHandler verificationHandler, final List<ServiceController<?>> newControllers) {

        final HTTPUpgradeService service = new HTTPUpgradeService(hornetQServerName, httpConnectorName);

        ServiceBuilder<HTTPUpgradeService> builder = serviceTarget.addService(UPGRADE_SERVICE_NAME.append(messagingConnectorName), service)
                .addDependency(HTTP_UPGRADE_REGISTRY.append(httpConnectorName), ChannelUpgradeHandler.class, service.injectedRegistry)
                .addDependency(HttpListenerRegistryService.SERVICE_NAME, ListenerRegistry.class, service.listenerRegistry)
                .addDependency(HornetQActivationService.getHornetQActivationServiceName(MessagingServices.getHornetQServiceName(hornetQServerName)));

        if (verificationHandler != null) {
            builder.addListener(verificationHandler);
        }

        builder.setInitialMode(ServiceController.Mode.PASSIVE);

        ServiceController<HTTPUpgradeService> controller = builder.install();
        if(newControllers != null) {
            newControllers.add(controller);
        }
    }

    @Override
    public void start(StartContext context) throws StartException {
        ListenerRegistry.Listener listenerInfo = listenerRegistry.getValue().getListener(httpConnectorName);
        assert listenerInfo != null;
        httpUpgradeMetadata = new ListenerRegistry.HttpUpgradeMetadata(HORNETQ_REMOTING, CORE);
        listenerInfo.addHttpUpgradeMetadata(httpUpgradeMetadata);

        injectedRegistry.getValue().addProtocol(HORNETQ_REMOTING,
                switchToHornetQProtocol(),
                createHttpUpgradeHandshake(MAGIC_NUMBER, SEC_HORNETQ_REMOTING_KEY, SEC_HORNETQ_REMOTING_ACCEPT));
    }

    @Override
    public void stop(StopContext context) {
        listenerRegistry.getValue().getListener(httpConnectorName).removeHttpUpgradeMetadata(httpUpgradeMetadata);
        httpUpgradeMetadata = null;
        injectedRegistry.getValue().removeProtocol(HORNETQ_REMOTING);
    }

    @Override
    public HTTPUpgradeService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    private static ChannelListener<StreamConnection> switchToHornetQProtocol() {
        return new ChannelListener<StreamConnection>() {
            @Override
            public void handleEvent(final StreamConnection channel) {
                channel.getSourceChannel().setReadListener(new ChannelListener<ConduitStreamSourceChannel>() {
                    @Override
                    public void handleEvent(ConduitStreamSourceChannel channel) {
                        ByteBuffer buffer = ByteBuffer.allocate(128);
                        try {
                            int read = channel.read(buffer);
                            if (read > 0) {
                                String str = new String(buffer.array(), "UTF-8");
                                System.out.println("str = " + str);
                                channel.resumeReads();
                            }
                        } catch (IOException e1) {
                            e1.printStackTrace();
                        }
                    }
                });
                channel.getSourceChannel().resumeReads();
            }
        };
    }

}
