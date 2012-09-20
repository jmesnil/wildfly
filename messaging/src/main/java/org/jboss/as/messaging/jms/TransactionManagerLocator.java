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

package org.jboss.as.messaging.jms;


import static org.jboss.as.messaging.MessagingLogger.MESSAGING_LOGGER;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.transaction.TransactionManager;

import org.jboss.as.txn.service.TxnServices;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;

/**
 * Locates the transaction manager for pooled connection resource adapters
 *
 * @author Jason T. Greene
 */
public class TransactionManagerLocator {
    private static final String TRANSACTION_MANAGER_JNDI_NAME = "java:jboss/TransactionManager";

    static volatile ServiceContainer container;

    /**
     * This method can be called from 2 different contexts:
     * 
     * 1. when a PooledConnectionFactoryService is created, in that case, the container is not null
     * 2. when HornetQ RA is deployed. In that case, the container is null and we default to looking up the TM using JNDI
     */
    public static TransactionManager getTransactionManager() {
        if (container == null) {
            try {
                InitialContext context = new InitialContext();
                return (TransactionManager) context.lookup(TRANSACTION_MANAGER_JNDI_NAME);
            } catch (NamingException e) {
                MESSAGING_LOGGER.debug("Unable to lookup transaction manager from " + TRANSACTION_MANAGER_JNDI_NAME, e);
                return null;
            }
        } else {
            @SuppressWarnings("unchecked")
            ServiceController<TransactionManager> service = (ServiceController<TransactionManager>) container.getService(TxnServices.JBOSS_TXN_TRANSACTION_MANAGER);
            return service == null ? null : service.getValue();
        }
    }
}
