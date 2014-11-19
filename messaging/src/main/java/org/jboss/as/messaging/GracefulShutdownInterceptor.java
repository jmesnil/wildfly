/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

import org.hornetq.api.core.HornetQException;
import org.hornetq.api.core.Interceptor;
import org.hornetq.core.protocol.core.Packet;
import org.hornetq.core.remoting.impl.netty.NettyServerConnection;
import org.hornetq.spi.core.protocol.RemotingConnection;
import org.jboss.as.server.suspend.ServerActivity;
import org.jboss.as.server.suspend.ServerActivityCallback;
import org.jboss.as.server.suspend.SuspendController;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2014 Red Hat inc.
 */
public class GracefulShutdownInterceptor implements Interceptor {


    static volatile SuspendController suspendController;

    private volatile boolean allowPacket = true;

    public GracefulShutdownInterceptor() {

        suspendController.registerActivity(new ServerActivity() {
            @Override
            public void preSuspend(ServerActivityCallback serverActivityCallback) {
                serverActivityCallback.done();

            }

            @Override
            public void suspended(ServerActivityCallback serverActivityCallback) {
                allowPacket = false;
                serverActivityCallback.done();
            }

            @Override
            public void resume() {
                allowPacket = true;
            }
        });
    }

    @Override
    public boolean intercept(Packet packet, RemotingConnection remotingConnection) throws HornetQException {
        System.out.println("connection = [" + remotingConnection.getTransportConnection() + "]");
        System.out.println("allowPacket = [" + allowPacket + "]");

        if ((remotingConnection.getTransportConnection() instanceof NettyServerConnection) && !allowPacket) {
            System.out.println("server is suspended");
            return false;
        }

        return true;
    }
}
