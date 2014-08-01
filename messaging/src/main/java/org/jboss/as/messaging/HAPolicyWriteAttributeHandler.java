package org.jboss.as.messaging;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.dmr.ModelNode;

import static org.jboss.as.controller.OperationContext.Stage.MODEL;

public class HAPolicyWriteAttributeHandler extends ReloadRequiredWriteAttributeHandler {

    public static final HAPolicyWriteAttributeHandler INSTANCE = new HAPolicyWriteAttributeHandler();

    private HAPolicyWriteAttributeHandler() {
        super(HAPolicyDefinition.ATTRIBUTES);
    }

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        context.addStep(new AlternativeAttributeCheckHandler(HAPolicyDefinition.ATTRIBUTES), MODEL);

        super.execute(context, operation);
    }
}
