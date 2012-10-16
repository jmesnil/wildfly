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

import static org.jboss.as.patching.Constants.APPLIED_TO;
import static org.jboss.as.patching.Constants.PREVIOUS_CUMULATIVE;
import static org.jboss.as.patching.Constants.RESULTING_VERSION;
import static org.jboss.as.patching.Constants.TIMESTAMP;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.jboss.as.boot.DirectoryStructure;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.patching.runner.PatchUtils;
import org.jboss.as.version.ProductConfig;
import org.jboss.dmr.ModelNode;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2012, Red Hat Inc
 */
public final class LocalShowHistoryHandler implements OperationStepHandler {
    public static final OperationStepHandler INSTANCE = new LocalShowHistoryHandler();

    @Override
    public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {
        context.acquireControllerLock();
        // Setup
        final PatchInfoService service = (PatchInfoService) context.getServiceRegistry(false).getRequiredService(PatchInfoService.NAME).getValue();

        try {
            final PatchInfo info = service.getPatchInfo();

            // history is composed of 3 parts stacked on top of each other, the most recent applied patch being at the top:
            //
            // +--------------------+
            // |  one-off patches   | * always applied at the top of a base version or a cumulative patch
            // |                    | * can be empty if no one-off patches have been applied
            // +--------------------+
            // | cumulative patches | * stack of applied cumulative patches
            // |                    | * can be empty if no cumulative patches have been appplied
            // +--------------------+
            // |    base version    | * the base version of this AS7 installation
            // |                    | * always present
            // +--------------------+
            ModelNode result = new ModelNode();
            result.get("current-version").set(info.getVersion());
            fillOneOffPatchesHistory(result, info);
            fillCumulativePatchesHistory(result, info);
            fillBaseVersionHistory(result, service.getProductConfig());

            context.getResult().set(result);
            context.stepCompleted();
        } catch (Throwable t) {
            t.printStackTrace();
            throw PatchMessages.MESSAGES.failedToShowHistory(t);
        }
    }

    private void fillBaseVersionHistory(ModelNode result, ProductConfig productConfig) {
        result.get("base-version").set(productConfig.resolveVersion());
    }

    private void fillCumulativePatchesHistory(ModelNode result, PatchInfo info) throws Exception {
        String cumulativeID = info.getCumulativeID();
        if (cumulativeID == PatchInfo.BASE) {
            // no applied cumulative patches
            return;
        }

        ModelNode cumulativePatches = new ModelNode();
        cumulativePatches.setEmptyList();
        do {
            cumulativeID = createCumulativePatchMetadata(cumulativePatches, cumulativeID, info.getEnvironment());
        } while (cumulativeID != PatchInfo.BASE);
        result.get("cumulatives").set(cumulativePatches);
    }

    private String createCumulativePatchMetadata(ModelNode cumulativePatches, String cumulativeID, DirectoryStructure environment) throws Exception {
        File patchHistoryDir = environment.getHistoryDir(cumulativeID);
        String timestamp = readTimeStamp(patchHistoryDir);
        String resultingVersion = PatchUtils.readRef(new File(patchHistoryDir, RESULTING_VERSION));
        final String appliedToVersion = PatchUtils.readRef(new File(patchHistoryDir, APPLIED_TO));

        ModelNode patch = new ModelNode();
        patch.get("patchID").set(cumulativeID);
        patch.get("resulting-version").set(resultingVersion);
        patch.get("applied-to").set(appliedToVersion);
        patch.get("applied-at").set(timestamp);
        cumulativePatches.add(patch);

        File previousCumulativeRefFile = new File(patchHistoryDir, PREVIOUS_CUMULATIVE);
        if (previousCumulativeRefFile.exists()) {
            return PatchUtils.readRef(previousCumulativeRefFile);
        } else {
            return PatchInfo.BASE;
        }
    }

    private void fillOneOffPatchesHistory(ModelNode result, PatchInfo info) throws IOException {
        List<String> patchIDs = info.getPatchIDs();
        if (patchIDs.isEmpty()) {
            return;
        }
        ModelNode oneOffPatches = new ModelNode();
        oneOffPatches.setEmptyList();
        for (String patchID : patchIDs) {
            oneOffPatches.add(createOneOffPatchHistory(patchID, info.getEnvironment()));
        }
        result.get("one-offs").set(oneOffPatches);
    }

    private ModelNode createOneOffPatchHistory(String patchID, DirectoryStructure environment) throws IOException {
        File patchHistoryDir = environment.getHistoryDir(patchID);
        String timestamp = readTimeStamp(patchHistoryDir);
        final String appliedToVersion = PatchUtils.readRef(new File(patchHistoryDir, APPLIED_TO));

        ModelNode patch = new ModelNode();
        patch.get("patchID").set(patchID);
        patch.get("applied-to").set(appliedToVersion);
        patch.get("applied-at").set(timestamp);
        return patch;
    }

    private String readTimeStamp(File historyDir) throws IOException {
        File timestampFile = new File(historyDir, TIMESTAMP);
        String timestamp = PatchUtils.readRef(timestampFile);
        return timestamp;
    }

}
