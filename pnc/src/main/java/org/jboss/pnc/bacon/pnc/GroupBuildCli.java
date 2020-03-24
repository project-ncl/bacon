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
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandException;
import org.aesh.command.CommandResult;
import org.aesh.command.GroupCommandDefinition;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Argument;
import org.aesh.command.option.Option;
import org.jboss.pnc.bacon.common.ObjectHelper;
import org.jboss.pnc.bacon.common.cli.AbstractCommand;
import org.jboss.pnc.bacon.common.cli.AbstractGetSpecificCommand;
import org.jboss.pnc.bacon.common.cli.AbstractListCommand;
import org.jboss.pnc.bacon.common.exception.FatalException;
import org.jboss.pnc.bacon.pnc.common.ClientCreator;
import org.jboss.pnc.client.*;
import org.jboss.pnc.dto.Build;
import org.jboss.pnc.dto.GroupBuild;
import org.jboss.pnc.enums.RebuildMode;
import org.jboss.pnc.rest.api.parameters.GroupBuildParameters;
import org.jboss.pnc.restclient.AdvancedGroupConfigurationClient;

import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@GroupCommandDefinition(
        name = "group-build",
        description = "Group builds",
        groupCommands = { GroupBuildCli.Cancel.class, GroupBuildCli.List.class, GroupBuildCli.ListBuilds.class,
                GroupBuildCli.Get.class })
@Slf4j
public class GroupBuildCli extends AbstractCommand {

    private static final ClientCreator<GroupBuildClient> CREATOR = new ClientCreator<>(GroupBuildClient::new);
    private static final ClientCreator<AdvancedGroupConfigurationClient> GC_CREATOR = new ClientCreator<>(
            AdvancedGroupConfigurationClient::new);

    @CommandDefinition(name = "start", description = "Start a new Group build")
    public class Start extends AbstractCommand {

        @Argument(required = true, description = "Group Build Config ID")
        private String groupBuildConfigId;

        @Option(
                name = "rebuild-mode",
                description = "Default: IMPLICIT_DEPENDENCY_CHECK. Other options are: EXPLICIT_DEPENDENCY_CHECK, FORCE")
        private String rebuildMode;
        @Option(name = "timestamp-alignment", description = "Default: false", defaultValue = "false")
        private String timestampAlignment;
        @Option(name = "temporary-build", description = "Temporary build, default: false", defaultValue = "false")
        private String temporaryBuild;
        @Option(
                name = "wait",
                overrideRequired = false,
                hasValue = false,
                description = "Wait for group build to complete")
        private boolean wait = false;
        @Option(name = "timeout", description = "Time in minutes the command waits for Group Build completion")
        private String timeout;
        @Option(
                shortName = 'o',
                overrideRequired = false,
                hasValue = false,
                description = "use json for output (default to yaml)")
        private boolean jsonOutput = false;

        public Start() {
        }

        @Override
        public CommandResult execute(CommandInvocation commandInvocation)
                throws CommandException, InterruptedException {

            GroupBuildParameters groupBuildParams = new GroupBuildParameters();
            if (rebuildMode == null) {
                rebuildMode = RebuildMode.IMPLICIT_DEPENDENCY_CHECK.name();
            }
            checkRebuildModeOption(rebuildMode);

            groupBuildParams.setRebuildMode(RebuildMode.valueOf(rebuildMode));
            groupBuildParams.setTimestampAlignment(Boolean.parseBoolean(timestampAlignment));
            groupBuildParams.setTemporaryBuild(Boolean.parseBoolean(temporaryBuild));

            // TODO add GroupBuildRequest with an option to specify BC revisions

            return super.executeHelper(commandInvocation, () -> {
                if (timeout != null) {
                    ObjectHelper.print(
                            jsonOutput,
                            GC_CREATOR.getClientAuthenticated()
                                    .executeGroupBuild(
                                            groupBuildConfigId,
                                            groupBuildParams,
                                            Long.parseLong(timeout),
                                            TimeUnit.MINUTES));
                    return;
                }

                if (wait) {
                    ObjectHelper.print(
                            jsonOutput,
                            GC_CREATOR.getClientAuthenticated()
                                    .executeGroupBuild(groupBuildConfigId, groupBuildParams)
                                    .join());
                } else {
                    ObjectHelper.print(
                            jsonOutput,
                            GC_CREATOR.getClientAuthenticated().trigger(groupBuildConfigId, groupBuildParams, null));
                }
            });
        }

        private void checkRebuildModeOption(String rebuildMode) {

            try {
                RebuildMode.valueOf(rebuildMode);
            } catch (IllegalArgumentException | NullPointerException e) {
                log.error("The rebuild flag contains an illegal option. Possibilities are: ");
                for (RebuildMode mode : RebuildMode.values()) {
                    log.error(mode.toString());
                }
                throw new FatalException();
            }
        }
    }

    @CommandDefinition(name = "cancel", description = "Cancel a group build")
    public class Cancel extends AbstractCommand {

        @Argument(required = true, description = "Group Build ID")
        private String groupBuildId;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation)
                throws CommandException, InterruptedException {

            return super.executeHelper(commandInvocation, () -> {
                CREATOR.getClientAuthenticated().cancel(groupBuildId);
            });
        }
    }

    @CommandDefinition(name = "list", description = "List group builds")
    public class List extends AbstractListCommand<GroupBuild> {

        @Override
        public RemoteCollection<GroupBuild> getAll(String sort, String query) throws RemoteResourceException {
            return CREATOR.getClient().getAll(Optional.ofNullable(sort), Optional.ofNullable(query));
        }
    }

    @CommandDefinition(name = "list-builds", description = "List builds associated with the group build")
    public class ListBuilds extends AbstractListCommand<Build> {

        @Argument(required = true, description = "Group Build ID")
        private String groupBuildId;

        @Override
        public RemoteCollection<Build> getAll(String sort, String query) throws RemoteResourceException {
            return CREATOR.getClient()
                    .getBuilds(groupBuildId, null, Optional.ofNullable(sort), Optional.ofNullable(query));
        }
    }

    @CommandDefinition(name = "get", description = "Get group build")
    public class Get extends AbstractGetSpecificCommand<GroupBuild> {

        @Override
        public GroupBuild getSpecific(String id) throws ClientException {
            return CREATOR.getClient().getSpecific(id);
        }
    }
}
