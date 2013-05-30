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

package org.jboss.as.patching.runner;

import static org.jboss.as.patching.Constants.CONTENT;
import static org.jboss.as.patching.IoUtils.recursiveDelete;
import static org.jboss.as.patching.IoUtils.safeClose;
import static org.jboss.as.patching.PatchLogger.ROOT_LOGGER;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.jboss.as.patching.IoUtils;
import org.jboss.as.patching.ZipUtils;
import org.jboss.as.patching.installation.InstallationManager;
import org.jboss.as.patching.installation.InstalledImage;
import org.jboss.as.patching.metadata.IdentityPatch;
import org.jboss.as.patching.metadata.Patch;
import org.jboss.as.patching.metadata.PatchXml;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2013 Red Hat inc.
 */
public class PatchingRunnerFactory {

    public static PatchingRunner create(final InstalledImage installedImage, final InstallationManager installationManager) {
        return new PatchingRunner() {

            private IdentityPatchRunner runner = new IdentityPatchRunner(installedImage);

            @Override
            public PatchingResult apply(InputStream patchStream, ContentVerificationPolicy policy) throws PatchingException {
                File workDir = null;
                try {
                    workDir = extractPatch(patchStream);
                    return applyPatch(workDir, policy);
                } finally {
                    if (workDir != null && !recursiveDelete(workDir)) {
                        ROOT_LOGGER.debugf("failed to remove work directory (%s)", workDir);
                    }
                }
            }

            @Override
            public PatchingResult rollback(String patchId, ContentVerificationPolicy contentPolicy, boolean rollbackTo, boolean restoreConfiguration) throws PatchingException {
                final InstallationManager.InstallationModification modification = installationManager.modifyInstallation(runner);
                return runner.rollbackPatch(patchId, contentPolicy, rollbackTo, restoreConfiguration, modification);
            }

            private PatchingResult applyPatch(File workDir, ContentVerificationPolicy policy) throws PatchingException {
                final PatchContentLoader loader = PatchContentLoader.create(workDir);
                final PatchContentProvider provider = new PatchContentProvider() {
                    @Override
                    public PatchContentLoader getLoader(String patchId) {
                        return loader;
                    }

                    @Override
                    public void cleanup() {
                        // gets cleaned up somewhere else
                    }
                };
                try {
                    // Parse the xml
                    final File patchXml = new File(workDir, PatchXml.PATCH_XML);
                    final InputStream patchIS = new FileInputStream(patchXml);
                    final Patch patch;
                    try {
                        patch = PatchXml.parse(patchIS);
                        patchIS.close();
                    } finally {
                        safeClose(patchIS);
                    }

                    // run, run, RUN !!!!
                    final IdentityPatch wrappedPatch = IdentityPatch.Wrapper.wrap(patch);
                    final InstallationManager.InstallationModification modification = installationManager.modifyInstallation(runner);
                    return runner.applyPatch(wrappedPatch, provider, policy, modification);
                } catch (Exception e) {
                    throw IdentityPatchRunner.rethrowException(e);
                }
            }

            private File extractPatch(InputStream patchStream) throws PatchingException {
                try {
                    // Create a working dir
                    File workDir = runner.createTempDir();

                    // save the content
                    final File cachedContent = new File(workDir, CONTENT);
                    IoUtils.copy(patchStream, cachedContent);
                    // Unpack to the work dir
                    ZipUtils.unzip(cachedContent, workDir);

                    return workDir;
                } catch (IOException e) {
                    throw new PatchingException(e);
                }
            }
        };
    }
}
