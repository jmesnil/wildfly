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

package org.jboss.as.host.controller.transformers;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.transform.TransformerRegistry;
import org.jboss.as.controller.transform.TransformersSubRegistration;
import org.jboss.as.domain.management.security.SecurityRealmResourceDefinition;
import org.jboss.as.domain.management.security.XmlAuthenticationResourceDefinition;
import org.jboss.as.host.controller.model.host.CoreServiceResourceDefinition;

/**
 * Global transformation rules for the host model.
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2012 Red Hat, inc.
 */
public class HostTransformers {

    //FIXME should the host share the same version than DomainTransformers?
    //AS 7.1.2.Final
    private static final ModelVersion VERSION_1_2 = ModelVersion.create(1, 2, 0);
    //AS 7.1.3.Final
    private static final ModelVersion VERSION_1_3 = ModelVersion.create(1, 3, 0);
    //AS 7.2.0.Final
    private static final ModelVersion VERSION_1_4 = ModelVersion.create(1, 4, 0);

    /**
     * Initialize the host registry.
     *
     * @param registry the host registry
     */
    public static void initializeHostRegistry(final TransformerRegistry registry) {
        initializeHostRegistry(registry, VERSION_1_2);
        initializeHostRegistry(registry, VERSION_1_3);
    }

    private static void initializeHostRegistry(final TransformerRegistry registry, ModelVersion modelVersion) {
        if (modelVersion == VERSION_1_2 || modelVersion == VERSION_1_3) {
            TransformersSubRegistration host = registry.getHostRegistration(modelVersion);

            TransformersSubRegistration management =  host.registerSubResource(CoreServiceResourceDefinition.PATH);
            TransformersSubRegistration securityRealm = management.registerSubResource(SecurityRealmResourceDefinition.PATH);

            LdapAuthenticationTransformers.registerTransformers(securityRealm);
            LocalAuthenticationTransformers.registerTransformers(securityRealm);
            PropertiesAuthenticationTransformers.registerTransformers(securityRealm);
            TruststoreAuthenticationTransformers.registerTransformers(securityRealm);

            TransformersSubRegistration usersAuthentication = securityRealm.registerSubResource(XmlAuthenticationResourceDefinition.PATH);
            UserAuthenticationTransformers.registerTransformers(usersAuthentication);

            PropertiesAuthorizationTransformers.registerTransformers(securityRealm);

            SSLServerIdentityTransformers.registerTransformers(securityRealm);

            VaultTransformers.registerTransformers(host);
        }
    }
}
