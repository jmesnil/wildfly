package org.jboss.as.patching.runner;

import static org.jboss.as.patching.IoUtils.safeClose;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.jboss.as.patching.DirectoryStructure;
import org.jboss.as.patching.IoUtils;
import org.jboss.as.patching.PatchInfo;
import org.jboss.as.patching.PatchLogger;
import org.jboss.as.patching.ZipUtils;
import org.jboss.as.patching.installation.AddOn;
import org.jboss.as.patching.installation.Identity;
import org.jboss.as.patching.installation.InstallationManager;
import org.jboss.as.patching.installation.InstallationManagerImpl;
import org.jboss.as.patching.installation.InstalledIdentity;
import org.jboss.as.patching.installation.InstalledImage;
import org.jboss.as.patching.installation.Layer;
import org.jboss.as.patching.metadata.IdentityPatch;
import org.jboss.as.patching.metadata.Patch;
import org.jboss.as.patching.metadata.PatchXml;

/**
 * @author Emanuel Muckenhuber
 */
public interface LegacyPatchingRunnerWrapper {

    static class Factory {

        private Factory() {
            //
        }

        public static PatchingRunner create(final PatchInfo patchInfo, final DirectoryStructure structure) {
            final InstalledIdentity identity = new InstalledIdentity() {
                @Override
                public List<Layer> getLayers() {
                    return Collections.emptyList();
                }

                @Override
                public Identity getIdentity() {
                    return new Identity() {
                        @Override
                        public String getName() {
                            return "test";
                        }

                        @Override
                        public String getVersion() {
                            return patchInfo.getVersion();
                        }

                        @Override
                        public TargetInfo loadTargetInfo() throws IOException {
                            return new TargetInfo() {
                                @Override
                                public String getCumulativeID() {
                                    return patchInfo.getCumulativeID();
                                }

                                @Override
                                public List<String> getPatchIDs() {
                                    return patchInfo.getPatchIDs();
                                }

                                @Override
                                public DirectoryStructure getDirectoryStructure() {
                                    return structure;
                                }
                            };
                        }
                    };
                }

                @Override
                public Collection<AddOn> getAddOns() {
                    return Collections.emptyList();
                }
            };
            return new LegacyPatchRunner(structure.getInstalledImage(), identity);
        }


    }

    static class LegacyPatchRunner implements PatchingRunner {

        private final IdentityPatchRunner runner;
        private final InstallationManager manager;

        public LegacyPatchRunner(InstalledImage installedImage, final InstalledIdentity identity) {
            this.runner = new IdentityPatchRunner(installedImage);
            try {
                this.manager = new InstallationManagerImpl(identity);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public PatchingResult apply(InputStream patchStream, ContentVerificationPolicy policy) throws PatchingException {
            File workDir = null;
            try {
                // Create a working dir
                workDir = runner.createTempDir();

                // Save the content
                final File cachedContent = new File(workDir, "content");
                IoUtils.copy(patchStream, cachedContent);
                // Unpack to the work dir
                ZipUtils.unzip(cachedContent, workDir);

                // Execute
                return execute(workDir, policy);
            } catch (IOException e) {
                throw new PatchingException(e);
            } finally {
                if (workDir != null && !IoUtils.recursiveDelete(workDir)) {
                    PatchLogger.ROOT_LOGGER.debugf("failed to remove work directory (%s)", workDir);
                }
            }
        }

        private PatchingResult execute(File workDir, ContentVerificationPolicy policy) throws PatchingException {
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
                final InstallationManager.InstallationModification modification = manager.modifyInstallation(runner);
                return runner.applyPatch(wrappedPatch, provider, policy, modification);
            } catch (Exception e) {
                throw IdentityPatchRunner.rethrowException(e);
            }
        }

        @Override
        public PatchingResult rollback(String patchId, ContentVerificationPolicy contentPolicy, boolean rollbackTo, boolean restoreConfiguration) throws PatchingException {
            final InstallationManager.InstallationModification modification = manager.modifyInstallation(runner);
            return runner.rollbackPatch(patchId, contentPolicy, rollbackTo, restoreConfiguration, modification);
        }
    }
}
