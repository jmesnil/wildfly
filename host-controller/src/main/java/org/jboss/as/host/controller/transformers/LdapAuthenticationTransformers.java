/*
* JBoss, Home of Professional Open Source.
* Copyright 2012, Red Hat Middleware LLC, and individual contributors
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

import org.jboss.as.controller.transform.RejectExpressionValuesTransformer;
import org.jboss.as.controller.transform.ResourceTransformer;
import org.jboss.as.controller.transform.TransformersSubRegistration;
import org.jboss.as.domain.management.security.LdapAuthenticationResourceDefinition;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;
import static org.jboss.as.domain.management.security.LdapAuthenticationResourceDefinition.ADVANCED_FILTER;
import static org.jboss.as.domain.management.security.LdapAuthenticationResourceDefinition.BASE_DN;
import static org.jboss.as.domain.management.security.LdapAuthenticationResourceDefinition.RECURSIVE;
import static org.jboss.as.domain.management.security.LdapAuthenticationResourceDefinition.USER_DN;
import static org.jboss.as.domain.management.security.LdapAuthenticationResourceDefinition.USERNAME_FILTER;

/**
 * The older versions of the model do not allow expressions for the ldap authentication resource's attributes.
 * Reject the attributes if they contain an expression.
 *
 * @author <a href="http://jmesnil.net">Jeff Mesnil</a> (c) 2012 Red Hat, inc
 */
class LdapAuthenticationTransformers {
    static TransformersSubRegistration registerTransformers(TransformersSubRegistration parent) {
        TransformersSubRegistration reg = parent.registerSubResource(LdapAuthenticationResourceDefinition.PATH, ResourceTransformer.DEFAULT);

        RejectExpressionValuesTransformer rejectExpression = new RejectExpressionValuesTransformer(ADVANCED_FILTER, BASE_DN, RECURSIVE, USER_DN, USERNAME_FILTER);

        reg.registerOperationTransformer(ADD, rejectExpression);
        reg.registerOperationTransformer(WRITE_ATTRIBUTE_OPERATION, rejectExpression.getWriteAttributeTransformer());

        return reg;
    }
}
