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

import lombok.extern.slf4j.Slf4j;
import org.jboss.pnc.bacon.pig.impl.config.GroupBuildInfo;
import org.jboss.pnc.bacon.pnc.client.PncClientHelper;
import org.jboss.pnc.client.BuildClient;
import org.jboss.pnc.client.BuildConfigurationClient;
import org.jboss.pnc.client.ClientException;
import org.jboss.pnc.client.GroupBuildClient;
import org.jboss.pnc.client.GroupConfigurationClient;
import org.jboss.pnc.client.RemoteResourceException;
import org.jboss.pnc.dto.Artifact;
import org.jboss.pnc.dto.Build;
import org.jboss.pnc.dto.BuildRef;
import org.jboss.pnc.dto.GroupBuild;
import org.jboss.pnc.enums.BuildStatus;
import org.jboss.pnc.rest.api.parameters.BuildsFilterParameters;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.Optional.of;
import static org.jboss.pnc.bacon.pig.impl.utils.PncClientUtils.query;
import static org.jboss.pnc.bacon.pig.impl.utils.PncClientUtils.toList;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com <br>
 *         Date: 6/3/17
 */
@Slf4j
public class BuildInfoCollector {
    private final BuildClient buildClient;
    private final BuildConfigurationClient buildConfigClient;
    private final GroupBuildClient groupBuildClient;
    private final GroupConfigurationClient groupConfigurationClient;

    public void addDependencies(PncBuild bd, String filter) {
        List<Artifact> artifacts;
        try {
            artifacts = toList(buildClient.getDependencyArtifacts(bd.getId(), Optional.empty(), of(filter)));
        } catch (RemoteResourceException e) {
            throw new RuntimeException("Failed to get dependency artifacts for " + bd.getId(), e);
        }

        bd.addDependencyArtifacts(artifacts);
    }

    public PncBuild getLatestBuild(String configId, BuildSearchType searchType) {
        try {
            BuildsFilterParameters filter = new BuildsFilterParameters();

            Optional<String> queryParam;
            switch (searchType) {
                case ANY:
                case TEMPORARY:
                    // NCL-5943 Cannot ignore permanent(regular) builds because they cause NO_REBUILD_REQUIRED even for
                    // temporary builds. That means "latest build" for a temporary build can be permanent(regular).
                    queryParam = query("status==%s", BuildStatus.SUCCESS);
                    break;
                case PERMANENT:
                    queryParam = query("status==%s;temporaryBuild==%s", BuildStatus.SUCCESS, false);
                    break;
                default:
                    queryParam = Optional.empty();
            }

            // Note: sort by id not allowed
            Iterator<Build> buildIterator = buildConfigClient
                    .getBuilds(configId, filter, of("=desc=submitTime"), queryParam)
                    .iterator();

            if (!buildIterator.hasNext()) {
                throw new NoSuccessfulBuildException(configId);
            }

            Build build = buildIterator.next();

            PncBuild result = new PncBuild(build);

            Optional<InputStream> maybeBuildLogs = buildClient.getBuildLogs(build.getId());

            if (maybeBuildLogs.isPresent()) {
                try (InputStream inputStream = maybeBuildLogs.get()) {
                    String log = readLog(inputStream);
                    result.addBuildLog(log);
                }
            }

            result.addBuiltArtifacts(toList(buildClient.getBuiltArtifacts(build.getId())));
            return result;
        } catch (ClientException | IOException e) {
            throw new RuntimeException("Failed to get latest successful build for " + configId, e);
        }
    }

    private String readLog(InputStream inputStream) throws IOException {
        StringBuilder logBuilder = new StringBuilder();
        try (InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader reader = new BufferedReader(inputStreamReader)) {
            reader.lines().forEach(logBuilder::append);
            return logBuilder.toString();
        }
    }

    public BuildInfoCollector() {
        buildClient = new BuildClient(PncClientHelper.getPncConfiguration());
        buildConfigClient = new BuildConfigurationClient(PncClientHelper.getPncConfiguration());
        groupBuildClient = new GroupBuildClient(PncClientHelper.getPncConfiguration());
        groupConfigurationClient = new GroupConfigurationClient(PncClientHelper.getPncConfiguration());
    }

    /**
     * Get the latest GroupBuildInfo from the groupConfiguration id. If there are no group builds, a runtime exception
     * is thrown.
     *
     * @param groupConfigurationId the group configuration id
     * @param temporaryBuild whether the group build is temporary or not
     * @return GroupBuildInfo data of the group build and the list of builds
     */
    public GroupBuildInfo getBuildsFromLatestGroupConfiguration(String groupConfigurationId, boolean temporaryBuild) {
        try {
            // we have to sort by startTime since group builds with 'NO_REBUILD_REQUIRED' don't have the endTime set
            Collection<GroupBuild> groupBuilds = groupConfigurationClient
                    .getAllGroupBuilds(
                            groupConfigurationId,
                            of("=desc=startTime"),
                            query("temporaryBuild==%s", temporaryBuild))
                    .getAll();

            Optional<GroupBuild> latest = groupBuilds.stream().filter(gb -> gb.getStatus().isFinal()).findFirst();
            if (latest.isPresent()) {
                // first one will be the latest build
                return getBuildsFromGroupBuild(latest.get());
            } else {
                // no builds!
                throw new RuntimeException(
                        "There are no group builds for group configuration id" + groupConfigurationId);
            }
        } catch (RemoteResourceException e) {
            throw new RuntimeException(
                    "Cannot get list of group builds for group configuration " + groupConfigurationId);
        }

    }

    /**
     * Get all the builds done in a build group. If the build finished with 'NO_REBUILD_REQUIRED', get the 'original'
     * successful build and return it instead If the build was successful, we don't grab the logs since they can be
     * quite long.
     *
     * @param groupBuild the group build to get the builds
     * @return The information on the group build and the builds performed
     */
    public GroupBuildInfo getBuildsFromGroupBuild(GroupBuild groupBuild) {

        Map<String, PncBuild> result = new HashMap<>();

        BuildsFilterParameters filter = new BuildsFilterParameters();
        filter.setLatest(false);
        filter.setRunning(false);

        try {
            Collection<Build> builds = groupBuildClient.getBuilds(groupBuild.getId(), filter).getAll();

            for (Build build : builds) {
                PncBuild pncBuild;

                if (build.getStatus() == BuildStatus.NO_REBUILD_REQUIRED) {
                    BuildRef buildRef = build.getNoRebuildCause();
                    Build realBuild = buildClient.getSpecific(buildRef.getId());
                    pncBuild = new PncBuild(realBuild);
                } else {
                    pncBuild = new PncBuild(build);
                }

                Optional<InputStream> maybeBuildLogs = buildClient.getBuildLogs(pncBuild.getId());
                if (maybeBuildLogs.isPresent()) {
                    try (InputStream inputStream = maybeBuildLogs.get()) {
                        String log = readLog(inputStream);
                        pncBuild.addBuildLog(log);
                    }
                }

                pncBuild.addBuiltArtifacts(toList(buildClient.getBuiltArtifacts(pncBuild.getId())));
                result.put(pncBuild.getName(), pncBuild);
            }
            return new GroupBuildInfo(groupBuild, result);
        } catch (IOException | RemoteResourceException e) {
            throw new RuntimeException("Failed to get group build info for " + groupBuild.getId(), e);
        }
    }

    /**
     * When using getLatestBuild, specify what latest build you want to find
     */
    public enum BuildSearchType {
        /**
         * Find latest build whether it's permanent or temporary
         */
        ANY,

        /**
         * Find latest permanent build
         */
        PERMANENT,

        /**
         * Find latest temporary build
         */
        TEMPORARY
    }
}
