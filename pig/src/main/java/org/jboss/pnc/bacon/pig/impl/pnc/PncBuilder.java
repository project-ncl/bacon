/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.pnc.bacon.pig.impl.pnc;

import org.jboss.pnc.bacon.pig.impl.utils.SleepUtils;
import org.jboss.pnc.client.ClientException;
import org.jboss.pnc.client.GroupBuildClient;
import org.jboss.pnc.client.GroupConfigurationClient;
import org.jboss.pnc.dto.GroupBuild;
import org.jboss.pnc.dto.GroupConfigurationRef;
import org.jboss.pnc.dto.requests.GroupBuildRequest;
import org.jboss.pnc.enums.RebuildMode;
import org.jboss.pnc.rest.api.parameters.GroupBuildParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.jboss.pnc.bacon.pnc.client.PncClientHelper.getPncConfiguration;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com <br>
 *         Date: 11/14/17
 */
public class PncBuilder {
    private static final Logger log = LoggerFactory.getLogger(PncBuilder.class);
    private final GroupBuildClient groupBuildClient;
    private final GroupConfigurationClient groupConfigClient;

    public PncBuilder() {
        groupBuildClient = new GroupBuildClient(getPncConfiguration());
        groupConfigClient = new GroupConfigurationClient(getPncConfiguration());
    }

    public void buildAndWait(
            GroupConfigurationRef group,
            boolean tempBuild,
            boolean tempBuildTS,
            RebuildMode rebuildMode) {
        GroupBuild groupBuild = run(group, tempBuild, tempBuildTS, rebuildMode);
        waitForSuccessfulFinish(groupBuild.getId());
    }

    private GroupBuild run(
            GroupConfigurationRef group,
            boolean tempBuild,
            boolean tempBuildTS,
            RebuildMode rebuildMode) {
        log.info("Performing builds of build group {} in PNC", group.getId());
        GroupBuildParameters buildParams = new GroupBuildParameters();
        buildParams.setRebuildMode(rebuildMode);
        buildParams.setTemporaryBuild(tempBuild);
        buildParams.setTimestampAlignment(tempBuildTS);
        GroupBuildRequest request = GroupBuildRequest.builder().build();

        try {
            return groupConfigClient.trigger(group.getId(), buildParams, request);
        } catch (ClientException e) {
            throw new RuntimeException("Failed to trigger build group " + group.getId(), e);
        }
    }

    private void waitForSuccessfulFinish(String groupBuildId) {
        log.info("waiting for finish of group build {}", groupBuildId);
        SleepUtils.waitFor(() -> isSuccessfullyFinished(groupBuildId), 30, true);
        log.info("group build finished successfully");
    }

    private Boolean isSuccessfullyFinished(String groupBuildId) {
        try {
            GroupBuild groupBuild = groupBuildClient.getSpecific(groupBuildId);
            switch (groupBuild.getStatus()) {
                case BUILDING:
                    return false;
                case REJECTED: // PNC has already built the project
                case NO_REBUILD_REQUIRED:
                case SUCCESS:
                    return true;
                default:
                    throw new RuntimeException("Build group failed " + groupBuild);
            }
        } catch (ClientException e) {
            log.warn("Failed to check if build is finished for " + groupBuildId + ", assuming it is not finished", e);
            return false;
        }
    }

}
