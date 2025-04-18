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

import static java.util.Optional.of;
import static org.jboss.pnc.bacon.pig.impl.utils.PncClientUtils.query;
import static org.jboss.pnc.bacon.pnc.client.PncClientHelper.getPncConfiguration;

import java.io.Closeable;
import java.util.Collection;

import org.jboss.pnc.api.enums.AlignmentPreference;
import org.jboss.pnc.bacon.common.exception.FatalException;
import org.jboss.pnc.bacon.pig.impl.utils.SleepUtils;
import org.jboss.pnc.bacon.pnc.common.UrlGenerator;
import org.jboss.pnc.client.ClientException;
import org.jboss.pnc.client.GroupBuildClient;
import org.jboss.pnc.client.GroupConfigurationClient;
import org.jboss.pnc.client.RemoteResourceException;
import org.jboss.pnc.dto.Build;
import org.jboss.pnc.dto.GroupBuild;
import org.jboss.pnc.dto.GroupConfigurationRef;
import org.jboss.pnc.dto.requests.GroupBuildRequest;
import org.jboss.pnc.enums.BuildStatus;
import org.jboss.pnc.enums.RebuildMode;
import org.jboss.pnc.rest.api.parameters.BuildsFilterParameters;
import org.jboss.pnc.rest.api.parameters.GroupBuildParameters;
import org.jboss.pnc.rest.api.parameters.GroupBuildsFilterParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com <br>
 *         Date: 11/14/17
 */
public class PncBuilder implements Closeable {
    private static final Logger log = LoggerFactory.getLogger(PncBuilder.class);
    private final GroupBuildClient groupBuildClient;
    private final GroupBuildClient anonymousGroupBuildClient;
    private final GroupConfigurationClient groupConfigClient;
    private final GroupConfigurationClient anonymousGroupConfigClient;

    public PncBuilder() {
        groupBuildClient = new GroupBuildClient(getPncConfiguration());
        anonymousGroupBuildClient = new GroupBuildClient(getPncConfiguration(false));
        groupConfigClient = new GroupConfigurationClient(getPncConfiguration());
        anonymousGroupConfigClient = new GroupConfigurationClient(getPncConfiguration(false));
    }

    /**
     * Created to inject mocks for testing
     *
     * @param gb
     * @param gc
     */
    PncBuilder(
            GroupBuildClient gb,
            GroupBuildClient anonymousGroupBuildClient,
            GroupConfigurationClient gc,
            GroupConfigurationClient anonymousGroupConfigClient) {
        groupBuildClient = gb;
        this.anonymousGroupBuildClient = anonymousGroupBuildClient;
        groupConfigClient = gc;
        this.anonymousGroupConfigClient = anonymousGroupConfigClient;
    }

    public GroupBuild build(
            GroupConfigurationRef group,
            boolean tempBuild,
            boolean tempBuildTS,
            RebuildMode rebuildMode,
            boolean wait,
            boolean dryRun) {
        GroupBuild groupBuild = run(group, tempBuild, tempBuildTS, rebuildMode, dryRun);
        if (wait) {
            waitForSuccessfulFinish(groupBuild.getId());
        }
        return groupBuild;
    }

    private GroupBuild run(
            GroupConfigurationRef group,
            boolean tempBuild,
            boolean tempBuildTS,
            RebuildMode rebuildMode,
            boolean dryRun) {
        log.info(
                "Performing builds of group config {} in PNC ( {} )",
                group.getId(),
                UrlGenerator.generateGroupConfigUrl(group.getId()));
        if (tempBuildTS)
            log.warn(
                    "Temporary builds with timestamp alignment are not supported, running temporary builds instead...");
        GroupBuildParameters buildParams = new GroupBuildParameters();
        buildParams.setRebuildMode(rebuildMode);
        buildParams.setTemporaryBuild(tempBuild);
        if (dryRun) {
            buildParams.setTemporaryBuild(true);
            buildParams.setAlignmentPreference(AlignmentPreference.PREFER_PERSISTENT);
            log.info("Build running as: temporary build with persistent dependency preference");
        } else if (tempBuild) {
            log.info("Build running as: temporary build");
        } else
            log.info("Build running as: persistent build");

        GroupBuildRequest request = GroupBuildRequest.builder().build();

        try {
            return groupConfigClient.trigger(group.getId(), buildParams, request);
        } catch (ClientException e) {
            throw new RuntimeException("Failed to trigger build group " + group.getId(), e);
        }
    }

    public String cancelRunningGroupBuild(String groupConfigurationId) {
        try {
            Collection<GroupBuild> groupBuilds = getRunningGroupBuilds(groupConfigurationId);

            if (groupBuilds.size() > 1) {
                return ("Can't cancel, there are multiple GroupBuilds running for single GroupConfiguration name and we can't decide correct one to cancel from build-config.yaml information. Found:"
                        + groupBuilds);
            }
            if (groupBuilds.isEmpty()) {
                return "No build is running for this group.";
            }
            GroupBuild running = groupBuilds.iterator().next();
            groupBuildClient.cancel(running.getId());
            return "Group build " + running.getId() + " canceled.";
        } catch (RemoteResourceException e) {
            throw new RuntimeException("Failed to get group build info to cancel running build.", e);
        }
    }

    public Collection<GroupBuild> getRunningGroupBuilds(String groupConfigurationId) {
        try {
            Collection<GroupBuild> groupBuilds = anonymousGroupConfigClient
                    .getAllGroupBuilds(
                            groupConfigurationId,
                            new GroupBuildsFilterParameters(),
                            of("=desc=startTime"),
                            query("status==%s", BuildStatus.BUILDING))
                    .getAll();
            return groupBuilds;
        } catch (RemoteResourceException e) {
            throw new RuntimeException("Failed to get group build info.", e);
        }
    }

    void waitForSuccessfulFinish(String groupBuildId) {
        log.info(
                "Waiting for finish of group build {} ( {} )",
                groupBuildId,
                UrlGenerator.generateGroupBuildUrl(groupBuildId));
        SleepUtils.waitFor(() -> isSuccessfullyFinished(groupBuildId), 30, true);
        log.info("Group build finished successfully");
    }

    boolean isSuccessfullyFinished(String groupBuildId) {

        // log set to info for CPaaS to detect infinite loop
        log.info("Checking if group build {} is successfully finished", groupBuildId);
        try {
            GroupBuild groupBuild = anonymousGroupBuildClient.getSpecific(groupBuildId);
            switch (groupBuild.getStatus()) {
                case BUILDING:
                    return false;
                case REJECTED: // PNC has already built the project
                case NO_REBUILD_REQUIRED:
                case SUCCESS:
                    return verifyAllBuildsInGroupBuildInFinalStateWithProperCount(groupBuildId);
                default:
                    throw new FatalException(
                            "Build group failed {} with status {}",
                            UrlGenerator.generateGroupBuildUrl(groupBuildId),
                            groupBuild.getStatus());
            }
        } catch (ClientException e) {
            log.warn(
                    "Failed to check if build is finished for {} ( {} ). Assuming it is not finished",
                    groupBuildId,
                    UrlGenerator.generateGroupBuildUrl(groupBuildId),
                    e);
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

        // log set to info for CPaaS to detect infinite loop
        log.info(
                "Checking if all builds in group build {} are in final state with proper count of builds ( {} )",
                groupBuildId,
                UrlGenerator.generateGroupBuildUrl(groupBuildId));
        BuildsFilterParameters filter = new BuildsFilterParameters();
        filter.setLatest(false);
        filter.setRunning(false);

        try {
            Collection<Build> builds = anonymousGroupBuildClient.getBuilds(groupBuildId, filter).getAll();
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
            GroupBuild gb = anonymousGroupBuildClient.getSpecific(groupBuildId);
            GroupConfigurationRef gc = gb.getGroupConfig();
            return anonymousGroupConfigClient.getBuildConfigs(gc.getId()).size();
        } catch (ClientException e) {
            log.warn("Failed to get count of build configs in the group build {}", groupBuildId, e);
            return -1;
        }
    }

    @Override
    public void close() {
        groupBuildClient.close();
        anonymousGroupBuildClient.close();
        groupConfigClient.close();
        anonymousGroupConfigClient.close();
    }
}
