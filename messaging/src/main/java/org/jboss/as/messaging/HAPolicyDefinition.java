package org.jboss.as.messaging;

import static org.jboss.as.controller.SimpleAttributeDefinitionBuilder.create;
import static org.jboss.as.messaging.CommonAttributes.CONNECTOR_REF_STRING;
import static org.jboss.dmr.ModelType.BOOLEAN;
import static org.jboss.dmr.ModelType.INT;
import static org.jboss.dmr.ModelType.LONG;
import static org.jboss.dmr.ModelType.STRING;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.hornetq.api.config.HornetQDefaultConfiguration;
import org.hornetq.core.config.BackupStrategy;
import org.hornetq.core.server.cluster.ha.HAPolicy;
import org.hornetq.core.server.cluster.ha.HAPolicyTemplate;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.PrimitiveListAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.operations.validation.EnumValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;

public class HAPolicyDefinition extends SimpleResourceDefinition {

    public static final PathElement PATH = PathElement.pathElement(CommonAttributes.HA_POLICY);

    private final boolean registerRuntimeOnly;

    public static final SimpleAttributeDefinition TEMPLATE = create("template", STRING)
            .setAllowNull(true)
            .setAllowExpression(true)
            .setValidator(new EnumValidator<HAPolicyTemplate>(HAPolicyTemplate.class, false, true))
            .build();

    public static final SimpleAttributeDefinition POLICY_TYPE = create("policy-type", STRING)
            .setAllowNull(true)
            .setAllowExpression(true)
            .setValidator(new EnumValidator<HAPolicy.POLICY_TYPE>(HAPolicy.POLICY_TYPE.class, false, true))
            .setRestartAllServices()
            .build();

    public static final SimpleAttributeDefinition REQUEST_BACKUP = create("request-backup", BOOLEAN)
            .setDefaultValue(new ModelNode(HornetQDefaultConfiguration.isDefaultHapolicyRequestBackup()))
            .setAllowExpression(true)
            .build();

    public static final SimpleAttributeDefinition BACKUP_REQUEST_RETRIES = create("backup-request-retries", INT)
            .setDefaultValue(new ModelNode(HornetQDefaultConfiguration.getDefaultHapolicyBackupRequestRetries()))
            .setAllowExpression(true)
            .build();

    public static final SimpleAttributeDefinition BACKUP_REQUEST_RETRY_INTERVAL = create("backup-request-retry-interval", LONG)
            .setDefaultValue(new ModelNode(HornetQDefaultConfiguration.getDefaultHapolicyBackupRequestRetryInterval()))
            .setAllowExpression(true)
            .build();

    public static final SimpleAttributeDefinition MAX_BACKUPS = create("max-backups", INT)
            .setDefaultValue(new ModelNode(HornetQDefaultConfiguration.getDefaultHapolicyMaxBackups()))
            .setAllowExpression(true)
            .build();

    public static final SimpleAttributeDefinition BACKUP_PORT_OFFSET = create("backup-port-offset", INT)
            .setDefaultValue(new ModelNode(HornetQDefaultConfiguration.getDefaultHapolicyBackupPortOffset()))
            .setAllowExpression(true)
            .build();

    public static final SimpleAttributeDefinition BACKUP_STRATEGY_TYPE = create("backup-strategy", STRING)
            .setAllowNull(true)
            .setAllowExpression(true)
            .setValidator(new EnumValidator<BackupStrategy>(BackupStrategy.class, false, true))
            .setRestartAllServices()
            .build();

    public static final PrimitiveListAttributeDefinition SCALEDOWN_CONNECTOR_REFS = PrimitiveListAttributeDefinition.Builder.of(CommonAttributes.SCALE_DOWN_CONNECTORS, STRING)
            .setAllowNull(true)
            .setElementValidator(new StringLengthValidator(1))
            .setXmlName(CONNECTOR_REF_STRING)
            .setAttributeMarshaller(new AttributeMarshallers.WrappedListAttributeMarshaller(null))
                    // disallow expressions since the attribute references other configuration items
            .setAllowExpression(false)
            .setRestartAllServices()
            .build();

    public static final SimpleAttributeDefinition SCALE_DOWN_DISCOVERY_GROUP = create("scale-down-discovery-group", STRING)
            .setAllowNull(true)
            .setDefaultValue(null)
            .setAllowExpression(true)
            .build();

    public static final SimpleAttributeDefinition SCALE_DOWN_GROUP_NAME = create("scale-down-group-name", STRING)
            .setAllowNull(true)
            .setDefaultValue(null)
            .setAllowExpression(true)
            .build();

    public static final SimpleAttributeDefinition BACKUP_GROUP_NAME = create("backup-group-name", STRING)
            .setAllowNull(true)
            .setDefaultValue(null)
            .setAllowExpression(true)
            .build();

    public static final PrimitiveListAttributeDefinition REMOTE_CONNECTOR_REFS = PrimitiveListAttributeDefinition.Builder.of(CommonAttributes.REMOTE_CONNECTORS, STRING)
            .setAllowNull(true)
            .setElementValidator(new StringLengthValidator(1))
            .setXmlName(CONNECTOR_REF_STRING)
            .setAttributeMarshaller(new AttributeMarshallers.WrappedListAttributeMarshaller(null))
                    // disallow expressions since the attribute references other configuration items
            .setAllowExpression(false)
            .setRestartAllServices()
            .build();
    public static final SimpleAttributeDefinition CHECK_FOR_LIVE_SERVER = create("check-for-live-server", BOOLEAN)
            .setDefaultValue(new ModelNode(HornetQDefaultConfiguration.isDefaultCheckForLiveServer()))
            .setAllowExpression(true)
            .build();

    public static final SimpleAttributeDefinition ALLOW_FAILBACK = create("allow-failback", BOOLEAN)
            .setDefaultValue(new ModelNode(HornetQDefaultConfiguration.isDefaultAllowAutoFailback()))
            .setAllowExpression(true)
            .build();

    public static final SimpleAttributeDefinition FAILBACK_DELAY = create("failback-delay", LONG)
            .setDefaultValue(new ModelNode(HornetQDefaultConfiguration.getDefaultFailbackDelay()))
            .setAllowExpression(true)
            .build();

    public static final SimpleAttributeDefinition FAILOVER_ON_SERVER_SHUTDOWN = create("failover-on-shutdown", BOOLEAN)
            .setDefaultValue(new ModelNode(HornetQDefaultConfiguration.isDefaultFailoverOnServerShutdown()))
            .setAllowExpression(true)
            .build();

    public static final SimpleAttributeDefinition REPLICATION_CLUSTERNAME = create("replication-clustername", STRING)
            .setDefaultValue(new ModelNode(HornetQDefaultConfiguration.getDefaultFailbackDelay()))
            .setAllowExpression(true)
            .build();

    public static final SimpleAttributeDefinition SCALE_DOWN_CLUSTERNAME = create("scale-down-clustername", STRING)
            .setDefaultValue(new ModelNode(HornetQDefaultConfiguration.getDefaultFailbackDelay()))
            .setAllowExpression(true)
            .build();

    public static final SimpleAttributeDefinition MAX_SAVED_REPLICATED_JOURNAL_SIZE = create("max-saved-replicated-journal-size", INT)
            .setDefaultValue(new ModelNode(HornetQDefaultConfiguration.getDefaultMaxSavedReplicatedJournalsSize()))
            .setAllowExpression(true)
            .build();

    public static final SimpleAttributeDefinition SCALE_DOWN = create("scale-down", BOOLEAN)
            .setDefaultValue(new ModelNode(HornetQDefaultConfiguration.isDefaultScaleDown()))
            .setAllowExpression(true)
            .build();

    public static final SimpleAttributeDefinition RESTART_BACKUP = create("restart-backup", BOOLEAN)
            .setDefaultValue(new ModelNode(HornetQDefaultConfiguration.isDefaultRestartBackup()))
            .setAllowExpression(true)
            .build();

    public HAPolicyDefinition(final boolean registerRuntimeOnly) {
        super(PATH,
                MessagingExtension.getResourceDescriptionResolver(CommonAttributes.HA_POLICY),
                HAPolicyAdd.INSTANCE,
                HAPolicyRemove.INSTANCE);
        this.registerRuntimeOnly = registerRuntimeOnly;
    }

    public static final List<AttributeDefinition> ATTRIBUTES = new ArrayList<>();

    static{
        AttributeDefinition[] list = new AttributeDefinition[]{
                TEMPLATE,
                POLICY_TYPE,
                REQUEST_BACKUP,
                BACKUP_REQUEST_RETRIES,
                BACKUP_REQUEST_RETRY_INTERVAL,
                MAX_BACKUPS,
                BACKUP_PORT_OFFSET,
                BACKUP_STRATEGY_TYPE,
                SCALEDOWN_CONNECTOR_REFS,
                SCALE_DOWN_DISCOVERY_GROUP,
                SCALE_DOWN_GROUP_NAME,
                BACKUP_GROUP_NAME,
                REMOTE_CONNECTOR_REFS,
                CHECK_FOR_LIVE_SERVER,
                ALLOW_FAILBACK,
                FAILBACK_DELAY,
                FAILOVER_ON_SERVER_SHUTDOWN,
                REPLICATION_CLUSTERNAME,
                SCALE_DOWN_CLUSTERNAME,
                MAX_SAVED_REPLICATED_JOURNAL_SIZE,
                SCALE_DOWN,
                RESTART_BACKUP
        };
        ATTRIBUTES.addAll(Arrays.asList(list));
    };

    @Override
    public void registerAttributes(ManagementResourceRegistration registry) {
        super.registerAttributes(registry);
        for (AttributeDefinition attr : ATTRIBUTES) {
            if (registerRuntimeOnly || !attr.getFlags().contains(AttributeAccess.Flag.STORAGE_RUNTIME)) {
                registry.registerReadWriteAttribute(attr, null, HAPolicyWriteAttributeHandler.INSTANCE);
            }
        }
    }

}
