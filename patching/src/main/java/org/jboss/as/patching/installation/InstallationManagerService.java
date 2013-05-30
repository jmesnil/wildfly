package org.jboss.as.patching.installation;

import static org.jboss.as.patching.Constants.JBOSS_PATCHING;
import static org.jboss.as.patching.Constants.JBOSS_PRODUCT_CONFIG_SERVICE;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import org.jboss.as.version.ProductConfig;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * @author Emanuel Muckenhuber
 */
public class InstallationManagerService implements Service<InstallationManager> {

    public static ServiceName NAME = JBOSS_PATCHING.append("manager");

    private static final String MODULE_PATH = "module.path";
    private static final String BUNDLES_DIR = "jboss.bundles.dir";

    private volatile InstallationManager manager;
    private final InjectedValue<ProductConfig> productConfig = new InjectedValue<ProductConfig>();
    private InstalledImage installedImage;

    /**
     * Install the installation manager service.
     *
     * @param serviceTarget
     * @return the service controller for the installed installation manager
     */
    public static ServiceController<InstallationManager> installService(ServiceTarget serviceTarget) {
        final InstallationManagerService service = new InstallationManagerService();
        return serviceTarget.addService(InstallationManagerService.NAME, service)
                .addDependency(JBOSS_PRODUCT_CONFIG_SERVICE, ProductConfig.class, service.productConfig)
                .setInitialMode(ServiceController.Mode.ACTIVE)
                .install();
    }

    private InstallationManagerService() {

    }

    @Override
    public synchronized void start(StartContext startContext) throws StartException {
        try {

            final File jbossHome = new File(System.getProperty("jboss.home.dir"));
            this.installedImage = InstalledIdentity.installedImage(jbossHome);
            final List<File> moduleRoots = getModulePath();
            final List<File> bundlesRoots = getBundlePath(installedImage);
            final InstalledIdentity identity = LayersFactory.load(installedImage, productConfig.getValue(), moduleRoots, bundlesRoots);

            this.manager = new InstallationManagerImpl(identity);

        } catch (Exception e) {
            throw new StartException(e);
        }
    }

    @Override
    public synchronized void stop(StopContext stopContext) {
        this.manager = null;
    }

    @Override
    public InstallationManager getValue() throws IllegalStateException, IllegalArgumentException {
        final InstallationManager manager = this.manager;
        if (manager == null) {
            throw new IllegalStateException();
        }
        return manager;
    }

    public InstalledImage getInstalledImage() {
        return installedImage;
    }

    public static List<File> getModulePath() {
        final List<File> path = new ArrayList<File>();
        final String modulePath = System.getProperty(MODULE_PATH, System.getenv("JAVA_MODULEPATH"));
        if (modulePath != null) {
            final String[] paths = modulePath.split(Pattern.quote(File.pathSeparator));
            for (final String s : paths) {
                final File file = new File(s);
                path.add(file);
            }
        }
        return path;
    }

    public static List<File> getBundlePath(final InstalledImage image) {
        final String prop = System.getProperty(BUNDLES_DIR);
        final File bundleRoots = prop != null ? new File(prop) : image.getBundlesDir();
        return Collections.singletonList(bundleRoots);
    }

}
