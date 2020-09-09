/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018 Red Hat, Inc., and individual contributors
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
package org.jboss.pnc.bacon.pnc;

import lombok.extern.slf4j.Slf4j;
import org.jboss.pnc.bacon.common.ObjectHelper;
import org.jboss.pnc.bacon.common.cli.AbstractBuildListCommand;
import org.jboss.pnc.bacon.common.cli.AbstractGetSpecificCommand;
import org.jboss.pnc.bacon.common.cli.AbstractListCommand;
import org.jboss.pnc.bacon.pnc.common.ClientCreator;
import org.jboss.pnc.bacon.pnc.common.ParameterChecker;
import org.jboss.pnc.client.ClientException;
import org.jboss.pnc.client.GroupBuildClient;
import org.jboss.pnc.client.RemoteCollection;
import org.jboss.pnc.client.RemoteResourceException;
import org.jboss.pnc.dto.Build;
import org.jboss.pnc.dto.GroupBuild;
import org.jboss.pnc.enums.RebuildMode;
import org.jboss.pnc.rest.api.parameters.BuildsFilterParameters;
import org.jboss.pnc.rest.api.parameters.GroupBuildParameters;
import org.jboss.pnc.restclient.AdvancedGroupConfigurationClient;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

@Command(
        name = "group-build",
        description = "Group builds",
        subcommands = {
                GroupBuildCli.Start.class,
                GroupBuildCli.Cancel.class,
                GroupBuildCli.List.class,
                GroupBuildCli.ListBuilds.class,
                GroupBuildCli.Get.class })
@Slf4j
public class GroupBuildCli {

    private static final ClientCreator<GroupBuildClient> CREATOR = new ClientCreator<>(GroupBuildClient::new);
    private static final ClientCreator<AdvancedGroupConfigurationClient> GC_CREATOR = new ClientCreator<>(
            AdvancedGroupConfigurationClient::new);

    @Command(name = "start", description = "Start a new Group build")
    public static class Start implements Callable<Integer> {

        @Parameters(description = "Group Build Config ID")
        private String groupBuildConfigId;

        @Option(
                names = "--rebuild-mode",
                description = "Default: IMPLICIT_DEPENDENCY_CHECK. Other options are: EXPLICIT_DEPENDENCY_CHECK, FORCE")
        private String rebuildMode;
        @Option(names = "--timestamp-alignment", description = "Do timestamp alignment with temporary builds")
        private boolean timestampAlignment = false;
        @Option(names = "--temporary-build", description = "Perform temporary builds")
        private boolean temporaryBuild = false;
        @Option(names = "--wait", description = "Wait for group build to complete")
        private boolean wait = false;
        @Option(names = "--timeout", description = "Time in minutes the command waits for Group Build completion")
        private String timeout;
        @Option(names = "-o", description = "use json for output (default to yaml)")
        private boolean jsonOutput = false;

        /**
         * Computes a result, or throws an exception if unable to do so.
         *
         * @return computed result
         * @throws Exception if unable to compute a result
         */
        @Override
        public Integer call() throws Exception {
            GroupBuildParameters groupBuildParams = new GroupBuildParameters();
            if (rebuildMode == null) {
                rebuildMode = RebuildMode.IMPLICIT_DEPENDENCY_CHECK.name();
            }
            ParameterChecker.checkRebuildModeOption(rebuildMode);

            groupBuildParams.setRebuildMode(RebuildMode.valueOf(rebuildMode));
            groupBuildParams.setTimestampAlignment(timestampAlignment);
            groupBuildParams.setTemporaryBuild(temporaryBuild);

            // TODO add GroupBuildRequest with an option to specify BC revisions
            try (AdvancedGroupConfigurationClient advancedGroupConfigurationClient = GC_CREATOR
                    .newClientAuthenticated()) {
                if (timeout != null) {
                    GroupBuild gb = advancedGroupConfigurationClient.executeGroupBuild(
                            groupBuildConfigId,
                            groupBuildParams,
                            Long.parseLong(timeout),
                            TimeUnit.MINUTES);
                    ObjectHelper.print(jsonOutput, gb);
                    return gb.getStatus().completedSuccessfully() ? 0 : gb.getStatus().ordinal();
                }

                if (wait) {
                    GroupBuild gb = advancedGroupConfigurationClient
                            .executeGroupBuild(groupBuildConfigId, groupBuildParams)
                            .join();
                    ObjectHelper.print(jsonOutput, gb);
                    return gb.getStatus().completedSuccessfully() ? 0 : gb.getStatus().ordinal();
                } else {
                    GroupBuild gb = advancedGroupConfigurationClient
                            .trigger(groupBuildConfigId, groupBuildParams, null);
                    ObjectHelper.print(jsonOutput, gb);
                    return gb.getStatus().completedSuccessfully() ? 0 : gb.getStatus().ordinal();
                }
            }
        }

        // TODO: @Override
        public String exampleText() {
            return "$ bacon pnc group-build start --temporary-build 23";
        }
    }

    @Command(name = "cancel", description = "Cancel a group build")
    public static class Cancel implements Callable<Integer> {

        @Parameters(description = "Group Build ID")
        private String groupBuildId;

        // TODO: @Override
        public String exampleText() {
            return "$ bacon pnc group-build cancel 42";
        }

        /**
         * Computes a result, or throws an exception if unable to do so.
         *
         * @return computed result
         * @throws Exception if unable to compute a result
         */
        @Override
        public Integer call() throws Exception {
            try (GroupBuildClient client = CREATOR.newClientAuthenticated()) {
                client.cancel(groupBuildId);
                return 0;
            }
        }
    }

    @Command(name = "list", description = "List group builds")
    public static class List extends AbstractListCommand<GroupBuild> {

        @Override
        public RemoteCollection<GroupBuild> getAll(String sort, String query) throws RemoteResourceException {
            try (GroupBuildClient client = CREATOR.newClient()) {
                return client.getAll(Optional.ofNullable(sort), Optional.ofNullable(query));
            }
        }
    }

    @Command(name = "list-builds", description = "List builds associated with the group build")
    public static class ListBuilds extends AbstractBuildListCommand {

        @Parameters(description = "Group Build ID")
        private String groupBuildId;

        @Override
        public RemoteCollection<Build> getAll(BuildsFilterParameters buildsFilter, String sort, String query)
                throws RemoteResourceException {
            try (GroupBuildClient client = CREATOR.newClient()) {
                return client
                        .getBuilds(groupBuildId, buildsFilter, Optional.ofNullable(sort), Optional.ofNullable(query));
            }
        }
    }

    @Command(name = "get", description = "Get a group build by its id")
    public static class Get extends AbstractGetSpecificCommand<GroupBuild> {

        @Override
        public GroupBuild getSpecific(String id) throws ClientException {
            try (GroupBuildClient client = CREATOR.newClient()) {
                return client.getSpecific(id);
            }
        }
    }
}
