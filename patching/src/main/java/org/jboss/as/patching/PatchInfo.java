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

package org.jboss.as.patching;

import org.jboss.as.boot.DirectoryStructure;
import org.jboss.as.version.ProductConfig;

import java.io.File;
import java.util.List;

/**
 * Basic information about a patch.
 *
 * @author Emanuel Muckenhuber
 */
public interface PatchInfo {

    /** The default if no patches are active. */
    String BASE = "base";

    /**
     * Get the current version.
     *
     * @return the current version
     */
    String getVersion();

    /**
     * The patch id of the latest applied cumulative patch
     * or {@link #BASE} if a cumulative patch has
     * never been applied to the installation
     *
     * @return the cp id
     */
    String getCumulativeID();

    /**
     * Get the list of one-off patch ids applied on top of the current version.
     *
     * The list is ordered by date, the most recent applied patch being the first element
     *
     * @return the patch ids
     */
    // FIXME should be a stack instead
    List<String> getPatchIDs();

    /**
     * Get the local patch environment.
     *
     * @return the patch environment
     */
    DirectoryStructure getEnvironment();

    /**
     * Get the current path for patching.
     * Note this does not include the module.path.
     *
     * @return the module path
     */
    File[] getPatchingPath();

    /**
     * Get the resolved module path.
     *
     * @return the module path
     */
    File[] getModulePath();

}
