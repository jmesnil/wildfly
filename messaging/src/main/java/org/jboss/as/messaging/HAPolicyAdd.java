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
package org.jboss.as.messaging;

import org.hornetq.core.config.BackupStrategy;
import org.hornetq.core.config.Configuration;
import org.hornetq.core.server.HornetQServer;
import org.hornetq.core.server.cluster.ha.HAPolicy;
import org.hornetq.core.server.cluster.ha.HAPolicyTemplate;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;

import java.util.ArrayList;
import java.util.List;

import static org.jboss.as.messaging.HornetQActivationService.getHornetQServer;

public class HAPolicyAdd extends AbstractAddStepHandler {

    public static final HAPolicyAdd INSTANCE = new HAPolicyAdd();

    private HAPolicyAdd() {
    }

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        model.setEmptyObject();

        AlternativeAttributeCheckHandler.checkAlternatives(operation, CommonAttributes.SCALE_DOWN_CONNECTORS,
                HAPolicyDefinition.SCALE_DOWN_DISCOVERY_GROUP.getName(), true);

        for (final AttributeDefinition attributeDefinition : HAPolicyDefinition.ATTRIBUTES) {
            attributeDefinition.validateAndSet(operation, model);
        }
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) throws OperationFailedException {
        final HornetQServer server = getHornetQServer(context, operation);
        if (server != null){
            final PathAddress address = PathAddress.pathAddress(operation.require(ModelDescriptionConstants.OP_ADDR));
            addHAPolicyConfig(context, server.getConfiguration(), model, address.getLastElement().getValue());
        }
    }

    public static void addHAPolicyConfig(OperationContext context, Configuration configuration, ModelNode model, String template) throws OperationFailedException {
        HAPolicy policyTemplate = null;
        if (template != null) {
            policyTemplate = HAPolicyTemplate.valueOf(template).getHaPolicy();
        }
        boolean allowFailback = HAPolicyDefinition.ALLOW_FAILBACK.resolveModelAttribute(context, model).asBoolean();
        String backupGroupName = HAPolicyDefinition.BACKUP_GROUP_NAME.resolveModelAttribute(context, model).asString();
        int portOffset = HAPolicyDefinition.BACKUP_PORT_OFFSET.resolveModelAttribute(context, model).asInt();
        int backupRequestRetries = HAPolicyDefinition.BACKUP_REQUEST_RETRIES.resolveModelAttribute(context, model).asInt();
        long backupRequestRetryInterval = HAPolicyDefinition.BACKUP_REQUEST_RETRY_INTERVAL.resolveModelAttribute(context, model).asLong();
        BackupStrategy backupStrategy = null;
        ModelNode modelNode = HAPolicyDefinition.BACKUP_STRATEGY_TYPE.resolveModelAttribute(context, model);
        if (modelNode.isDefined()) {
            backupStrategy = BackupStrategy.valueOf(modelNode.asString());
        }
        boolean checkForLiveServer = HAPolicyDefinition.CHECK_FOR_LIVE_SERVER.resolveModelAttribute(context, model).asBoolean();
        long failbackDelay = HAPolicyDefinition.FAILBACK_DELAY.resolveModelAttribute(context, model).asLong();
        boolean failoverOnShutdown = HAPolicyDefinition.FAILOVER_ON_SERVER_SHUTDOWN.resolveModelAttribute(context, model).asBoolean();
        int maxBackups = HAPolicyDefinition.MAX_BACKUPS.resolveModelAttribute(context, model).asInt();
        int maxSavedReplicatedJournalSize = HAPolicyDefinition.MAX_SAVED_REPLICATED_JOURNAL_SIZE.resolveModelAttribute(context, model).asInt();
        HAPolicy.POLICY_TYPE policyType = null;
        modelNode = HAPolicyDefinition.POLICY_TYPE.resolveModelAttribute(context, model);
        if (modelNode.isDefined()) {
            policyType = HAPolicy.POLICY_TYPE.valueOf(modelNode.asString());
        }
        List<String> remoteConnectors = getRemoteConnectors(model);
        String replicationClustername = HAPolicyDefinition.REPLICATION_CLUSTERNAME.resolveModelAttribute(context, model).asString();
        boolean requestBackup = HAPolicyDefinition.REQUEST_BACKUP.resolveModelAttribute(context, model).asBoolean();
        boolean restartBackup = HAPolicyDefinition.RESTART_BACKUP.resolveModelAttribute(context, model).asBoolean();
        boolean scaleDown = HAPolicyDefinition.SCALE_DOWN.resolveModelAttribute(context, model).asBoolean();
        String scaleDownClusterName = HAPolicyDefinition.SCALE_DOWN_CLUSTERNAME.resolveModelAttribute(context, model).asString();
        List<String> scaleDownConnectors = getScaleDownConnectors(model);
        String scaleDownDiscoveryGroup = HAPolicyDefinition.SCALE_DOWN_DISCOVERY_GROUP.resolveModelAttribute(context, model).asString();
        String scaleDownGroupName = HAPolicyDefinition.SCALE_DOWN_GROUP_NAME.resolveModelAttribute(context, model).asString();
        HAPolicy policy;
        if (policyTemplate != null) {
            policy = policyTemplate;
        }
        else if (scaleDownDiscoveryGroup != null) {
            policy = new HAPolicy(policyType,
                    requestBackup,
                    backupRequestRetries,
                    backupRequestRetryInterval,
                    maxBackups,
                    portOffset,
                    backupStrategy,
                    scaleDownDiscoveryGroup,
                    scaleDownGroupName,
                    backupGroupName,
                    remoteConnectors,
                    checkForLiveServer,
                    allowFailback,
                    failbackDelay,
                    failoverOnShutdown,
                    replicationClustername,
                    scaleDownClusterName,
                    maxSavedReplicatedJournalSize,
                    scaleDown,
                    restartBackup);
        } else{
            policy = new HAPolicy(policyType,
                    requestBackup,
                    backupRequestRetries,
                    backupRequestRetryInterval,
                    maxBackups,
                    portOffset,
                    backupStrategy,
                    scaleDownConnectors,
                    scaleDownGroupName,
                    backupGroupName,
                    remoteConnectors,
                    checkForLiveServer,
                    allowFailback,
                    failbackDelay,
                    failoverOnShutdown,
                    replicationClustername,
                    scaleDownClusterName,
                    maxSavedReplicatedJournalSize,
                    scaleDown,
                    restartBackup);
        }
        configuration.setHAPolicy(policy);
    }

    private static List<String> getRemoteConnectors(ModelNode model) {
        if (!model.hasDefined(CommonAttributes.STATIC_CONNECTORS))
            return null;

        List<String> result = new ArrayList<String>();
        for (ModelNode connector : model.require(CommonAttributes.REMOTE_CONNECTORS).asList()) {
            result.add(connector.asString());
        }
        return result;
    }

    private static List<String> getScaleDownConnectors(ModelNode model) {
        if (!model.hasDefined(CommonAttributes.STATIC_CONNECTORS))
            return null;

        List<String> result = new ArrayList<String>();
        for (ModelNode connector : model.require(CommonAttributes.SCALE_DOWN_CONNECTORS).asList()) {
            result.add(connector.asString());
        }
        return result;
    }
}
