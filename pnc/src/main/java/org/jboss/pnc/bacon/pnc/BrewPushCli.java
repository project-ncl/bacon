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

import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandException;
import org.aesh.command.CommandResult;
import org.aesh.command.GroupCommandDefinition;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Argument;
import org.aesh.command.option.Option;
import org.jboss.pnc.bacon.common.ObjectHelper;
import org.jboss.pnc.bacon.common.cli.AbstractCommand;
import org.jboss.pnc.bacon.pnc.common.ClientCreator;
import org.jboss.pnc.client.GroupBuildClient;
import org.jboss.pnc.dto.requests.BuildPushRequest;
import org.jboss.pnc.dto.requests.GroupBuildPushRequest;
import org.jboss.pnc.restclient.AdvancedBuildClient;

import java.util.concurrent.TimeUnit;

@GroupCommandDefinition(
        name = "brew-push",
        description = "brew-push",
        groupCommands = { BrewPushCli.Build.class, BrewPushCli.GroupBuild.class, BrewPushCli.Status.class, })
public class BrewPushCli extends AbstractCommand {

    private static final ClientCreator<AdvancedBuildClient> BUILD_CREATOR = new ClientCreator<>(
            AdvancedBuildClient::new);
    private static final ClientCreator<GroupBuildClient> GROUP_BUILD_CREATOR = new ClientCreator<>(
            GroupBuildClient::new);

    @CommandDefinition(name = "build", description = "Push build to Brew")
    public class Build extends AbstractCommand {

        @Argument(required = true, description = "Id of build")
        private String id;
        @Option(required = true, name = "tag-prefix", description = "Brew Tag Prefix")
        private String tagPrefix;

        @Option(
                name = "reimport",
                description = "Should we re-import the build in case it was already imported?",
                defaultValue = "false")
        private String reimport;

        @Option(
                name = "wait",
                overrideRequired = false,
                hasValue = false,
                description = "wait for BrewPush to complete")
        private boolean wait = false;

        @Option(name = "timeout", description = "Time in milliseconds the command waits for Group Build completion")
        private String timeout;

        @Option(
                shortName = 'o',
                overrideRequired = false,
                hasValue = false,
                description = "use json for output (default to yaml)")
        private boolean jsonOutput = false;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation)
                throws CommandException, InterruptedException {

            return super.executeHelper(commandInvocation, () -> {

                BuildPushRequest request = BuildPushRequest.builder()
                        .tagPrefix(tagPrefix)
                        .reimport(Boolean.valueOf(reimport))
                        .build();

                if (timeout != null) {
                    try {
                        ObjectHelper.print(
                                jsonOutput,
                                BUILD_CREATOR.getClientAuthenticated()
                                        .executeBrewPush(id, request)
                                        .get(Long.parseLong(timeout), TimeUnit.MILLISECONDS));
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    return;
                }

                if (wait)
                    ObjectHelper.print(
                            jsonOutput,
                            BUILD_CREATOR.getClientAuthenticated().executeBrewPush(id, request).join());
                else
                    ObjectHelper.print(jsonOutput, BUILD_CREATOR.getClientAuthenticated().push(id, request));
            });
        }

        @Override
        public String exampleText() {
            return "$ bacon brew-push build 8 --tag-prefix=\"1.0-pnc\" --reimport=false";
        }
    }

    @CommandDefinition(name = "group-build", description = "Push group build to Brew")
    public class GroupBuild extends AbstractCommand {

        @Argument(required = true, description = "Id of group-build")
        private String id;
        @Option(required = true, name = "tag-prefix", description = "Brew Tag Prefix")
        private String tagPrefix;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation)
                throws CommandException, InterruptedException {
            // TODO add wait option for GroupPush
            return super.executeHelper(commandInvocation, () -> {

                GroupBuildPushRequest request = GroupBuildPushRequest.builder().tagPrefix(tagPrefix).build();
                GROUP_BUILD_CREATOR.getClientAuthenticated().brewPush(id, request);
            });
        }

        @Override
        public String exampleText() {
            return "$ bacon brew-push group-build 8 --tag-prefix=\"1.0-pnc\"";
        }
    }

    @CommandDefinition(name = "status", description = "Brew Push Status")
    public class Status extends AbstractCommand {

        @Argument(required = true, description = "Brew Push ID")
        private String id;

        @Option(
                shortName = 'o',
                overrideRequired = false,
                hasValue = false,
                description = "use json for output (default to yaml)")
        private boolean jsonOutput = false;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation)
                throws CommandException, InterruptedException {

            return super.executeHelper(commandInvocation, () -> {

                ObjectHelper.print(jsonOutput, BUILD_CREATOR.getClient().getPushResult(id));
            });
        }
    }
}
