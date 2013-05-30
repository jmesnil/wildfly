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

package org.jboss.as.patching.installation;

import static org.jboss.as.patching.runner.TestUtils.tree;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.net.URL;
import java.util.Collection;
import java.util.List;

import org.jboss.as.patching.runner.AbstractTaskTestCase;
import org.jboss.as.version.ProductConfig;
import org.junit.Test;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2013 Red Hat inc.
 */
public class InstalledIdentityTestCase extends AbstractTaskTestCase {

    @Test
    public void testInstalledIdentity() throws Exception {
        final URL resource = this.getClass().getResource("jbossHome");
        File jbossHome = new File(resource.toURI());
        tree(jbossHome);
        InstalledImage installedImage = InstalledIdentity.installedImage(jbossHome);

        ProductConfig productConfig = new ProductConfig("productName", "productVersion", "consoleSlot");
        InstalledIdentity installedIdentity = InstalledIdentity.load(jbossHome, productConfig, installedImage.getModulesDir());

        List<Layer> layers = installedIdentity.getLayers();
        assertEquals(layers.toString(), 2, layers.size());
        assertEquals("xyz", layers.get(0).getName());
        assertEquals("vuw", layers.get(1).getName());

        Collection<AddOn> addOns = installedIdentity.getAddOns();
        assertEquals(1, addOns.size());
        AddOn addOn = addOns.iterator().next();
        assertEquals("def", addOn.getName());

        Identity identity = installedIdentity.getIdentity();
        assertEquals(productConfig.getProductName(), identity.getName());
        assertEquals(productConfig.resolveVersion(), identity.getVersion());
    }
}
