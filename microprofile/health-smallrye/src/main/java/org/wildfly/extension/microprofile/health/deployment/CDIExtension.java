/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.microprofile.health.deployment;

import java.util.function.Consumer;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeShutdown;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.util.AnnotationLiteral;

import org.eclipse.microprofile.health.Health;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.Liveness;
import org.eclipse.microprofile.health.Readiness;
import org.wildfly.extension.microprofile.health.HealthReporter;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2017 Red Hat inc.
 */
public class CDIExtension implements Extension {

    private final HealthReporter reporter;

    static final class HealthLiteral extends AnnotationLiteral<Health> implements Health {

        static final HealthLiteral INSTANCE = new HealthLiteral();

        private static final long serialVersionUID = 1L;

    }
    static final class LivenessLiteral extends AnnotationLiteral<Liveness> implements Liveness {

        static final LivenessLiteral INSTANCE = new LivenessLiteral();

        private static final long serialVersionUID = 1L;

    }
    static final class ReadinessLiteral extends AnnotationLiteral<Readiness> implements Readiness {

        static final ReadinessLiteral INSTANCE = new ReadinessLiteral();

        private static final long serialVersionUID = 1L;

    }

    public CDIExtension(HealthReporter healthReporter) {
        this.reporter = healthReporter;
    }

    /**
     * Get CDI <em>instances</em> of HealthCheck and
     * add them to the {@link HealthReporter}.
     */
    private void afterDeploymentValidation(@Observes final AfterDeploymentValidation avd, BeanManager bm) {
        addHealthCheck(HealthLiteral.INSTANCE, bm, reporter::addHealthCheck);
        addHealthCheck(LivenessLiteral.INSTANCE, bm, reporter::addLivenessCheck);
        addHealthCheck(ReadinessLiteral.INSTANCE, bm, reporter::addReadinessCheck);
    }

    private void addHealthCheck(AnnotationLiteral qualifier, BeanManager bm,
                                Consumer<HealthCheck> healthFunction) {
        Instance<HealthCheck> healthChecks = bm.createInstance().select(HealthCheck.class, qualifier);
        for (HealthCheck healthCheck : healthChecks) {
            healthFunction.accept(healthCheck);
        }
    }

    /**
     * Called when the deployment is undeployed.
     * <p>
     * Remove all the instances of {@link HealthCheck} from the {@link HealthReporter}.
     */
    public void beforeShutdown(@Observes final BeforeShutdown bs, BeanManager bm) {
        removeHealthCheck(HealthLiteral.INSTANCE, bm, reporter::removeHealthCheck);
        removeHealthCheck(LivenessLiteral.INSTANCE, bm, reporter::removeLivenessCheck);
        removeHealthCheck(ReadinessLiteral.INSTANCE, bm, reporter::removeReadinessCheck);
    }

    private void removeHealthCheck(AnnotationLiteral qualifier, BeanManager bm,
                                   Consumer<HealthCheck> healthFunction) {
        Instance<HealthCheck> healthChecks = bm.createInstance().select(HealthCheck.class, qualifier);
        for (HealthCheck healthCheck : healthChecks) {
            healthFunction.accept(healthCheck);
        }
    }
}