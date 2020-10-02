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
import org.jboss.pnc.dto.Build;
import org.jboss.pnc.dto.GroupBuild;
import org.jboss.pnc.dto.GroupConfigurationRef;
import org.jboss.pnc.dto.requests.GroupBuildRequest;
import org.jboss.pnc.enums.RebuildMode;
import org.jboss.pnc.rest.api.parameters.BuildsFilterParameters;
import org.jboss.pnc.rest.api.parameters.GroupBuildParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.util.Collection;

import static org.jboss.pnc.bacon.pnc.client.PncClientHelper.getPncConfiguration;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com <br>
 *         Date: 11/14/17
 */
public class PncBuilder implements Closeable {
    private static final Logger log = LoggerFactory.getLogger(PncBuilder.class);
    private final GroupBuildClient groupBuildClient;
    private final GroupConfigurationClient groupConfigClient;

    public PncBuilder() {
        groupBuildClient = new GroupBuildClient(getPncConfiguration());
        groupConfigClient = new GroupConfigurationClient(getPncConfiguration());
    }

    /**
     * Created to inject mocks for testing
     *
     * @param gb
     * @param gc
     */
    PncBuilder(GroupBuildClient gb, GroupConfigurationClient gc) {
        groupBuildClient = gb;
        groupConfigClient = gc;
    }

    public GroupBuild build(
            GroupConfigurationRef group,
            boolean tempBuild,
            boolean tempBuildTS,
            RebuildMode rebuildMode,
            boolean wait) {
        GroupBuild groupBuild = run(group, tempBuild, tempBuildTS, rebuildMode);
        if (wait) {
            waitForSuccessfulFinish(groupBuild.getId());
        }
        return groupBuild;
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

    void waitForSuccessfulFinish(String groupBuildId) {
        log.info("Waiting for finish of group build {}", groupBuildId);
        SleepUtils.waitFor(() -> isSuccessfullyFinished(groupBuildId), 30, true);
        log.info("Group build finished successfully");
    }

    boolean isSuccessfullyFinished(String groupBuildId) {

        log.debug("Checking if group build {} is successfully finished", groupBuildId);
        try {
            GroupBuild groupBuild = groupBuildClient.getSpecific(groupBuildId);
            switch (groupBuild.getStatus()) {
                case BUILDING:
                    return false;
                case REJECTED: // PNC has already built the project
                case NO_REBUILD_REQUIRED:
                case SUCCESS:
                    return verifyAllBuildsInGroupBuildInFinalStateWithProperCount(groupBuildId);
                default:
                    throw new RuntimeException("Build group failed " + groupBuild);
            }
        } catch (ClientException e) {
            log.warn("Failed to check if build is finished for {}. Assuming it is not finished", groupBuildId, e);
            return false;
        }
    }

    /**
     * This check makes sure that even when a group build is marked as done, we also need to make sure all its builds
     * are also done (NCL-6044). We also make sure that the number of builds done is the same as the number of build
     * configs in the group build's group config (NCL-6041)
     *
     * This is done to handle a weird timing error which happens when a group build finishes, the group build status is
     * updated before the last individual build status is updated to their final state. This may cause inconsistency in
     * the last build data.
     *
     * This is especially true when the Group build status is NO_REBUILD_REQUIRED, where it is also essential that all
     * the individual builds are also in their final state (and logically NO_REBUILD_REQUIRED status) so that we can get
     * the no rebuild cause.
     *
     * @param groupBuildId the group build id
     * @return whether all the builds have a final status or not
     */
    boolean verifyAllBuildsInGroupBuildInFinalStateWithProperCount(String groupBuildId) {

        log.debug(
                "Checking if all builds in group build {} are in final state with proper count of builds",
                groupBuildId);
        BuildsFilterParameters filter = new BuildsFilterParameters();
        filter.setLatest(false);
        filter.setRunning(false);

        try {
            Collection<Build> builds = groupBuildClient.getBuilds(groupBuildId, filter).getAll();
            boolean allFinal = builds.stream().allMatch(b -> b.getStatus().isFinal());
            return allFinal && getCountOfBuildConfigsForGroupBuild(groupBuildId) == builds.size();
        } catch (ClientException e) {
            log.warn(
                    "Failed to check if all builds in group build {} have a final status. Assuming it is not finished",
                    groupBuildId,
                    e);
            return false;
        }
    }

    /**
     * Try to get the count of build configs for a group build's group config. Returns -1 if it couldn't do the request
     *
     * @param groupBuildId
     * @return count, -1 if an error happened
     */
    int getCountOfBuildConfigsForGroupBuild(String groupBuildId) {

        try {
            GroupBuild gb = groupBuildClient.getSpecific(groupBuildId);
            GroupConfigurationRef gc = gb.getGroupConfig();
            return groupConfigClient.getBuildConfigs(gc.getId()).size();
        } catch (ClientException e) {
            log.warn("Failed to get count of build configs in the group build {}", groupBuildId, e);
            return -1;
        }
    }

    @Override
    public void close() {
        groupBuildClient.close();
        groupConfigClient.close();
    }
}
