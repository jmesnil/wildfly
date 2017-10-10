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

package org.wildfly.extension.messaging.activemq;

import static org.wildfly.extension.messaging.activemq.CommonAttributes.MODULE;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.NAME;

import java.util.ArrayList;
import java.util.List;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.wildfly.extension.messaging.activemq.logging.MessagingLogger;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2017 Red Hat inc.
 */
class ClassLoaderUtil {

    /**
     * @param classModel must be a ModelNode with mandatory "name" and "module" String attributes
     */
    static Class unwrapClass(ModelNode classModel) throws OperationFailedException {
        String className = classModel.get(NAME).asString();
        String moduleName = classModel.get(MODULE).asString();
        try {
            ModuleIdentifier moduleID = ModuleIdentifier.create(moduleName);
            Module module = Module.getCallerModuleLoader().loadModule(moduleID);
            Class<?> clazz = module.getClassLoader().loadClass(className);
            return clazz;
        } catch (Exception e) {
            throw MessagingLogger.ROOT_LOGGER.unableToLoadClassFromModule(className, moduleName);
        }
    }

    static List<Class> unwrapClasses(List<ModelNode> classesModel) throws OperationFailedException {
        List<Class> classes = new ArrayList<>();

        for (ModelNode classModel : classesModel) {
            Class clazz = unwrapClass(classModel);
            classes.add(clazz);
        }

        return classes;
    }

    static Object instantiate(ModelNode classModel) throws OperationFailedException {
        Class clazz = unwrapClass(classModel);
        try {
            return clazz.newInstance();
        } catch (Exception e) {
            throw MessagingLogger.ROOT_LOGGER.unableToInstantiateClass(clazz);
        }
    }
}
