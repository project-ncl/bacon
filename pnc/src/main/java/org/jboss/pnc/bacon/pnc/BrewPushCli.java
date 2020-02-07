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
import org.jboss.pnc.bacon.pnc.client.PncClientHelper;
import org.jboss.pnc.client.BuildClient;
import org.jboss.pnc.client.GroupBuildClient;
import org.jboss.pnc.dto.requests.BuildPushRequest;
import org.jboss.pnc.dto.requests.GroupBuildPushRequest;

@GroupCommandDefinition(name = "brew-push", description = "brew-push", groupCommands = { BrewPushCli.Build.class,
        BrewPushCli.GroupBuild.class, BrewPushCli.Status.class, })
public class BrewPushCli extends AbstractCommand {

    @CommandDefinition(name = "build", description = "Push build to Brew")
    public class Build extends AbstractCommand {

        @Argument(required = true, description = "Id of build")
        private String id;
        @Option(required = true, name = "tag-prefix", description = "Brew Tag Prefix")
        private String tagPrefix;

        @Option(name = "reimport", description = "Should we re-import the build in case it was already imported?", defaultValue = "false")
        private String reimport;

        @Option(shortName = 'o', overrideRequired = false, hasValue = false, description = "use json for output (default to yaml)")
        private boolean jsonOutput = false;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {

            return super.executeHelper(commandInvocation, () -> {

                BuildClient client = new BuildClient(PncClientHelper.getPncConfiguration(true));

                BuildPushRequest request = BuildPushRequest.builder().tagPrefix(tagPrefix).reimport(Boolean.valueOf(reimport))
                        .build();

                ObjectHelper.print(jsonOutput, client.push(id, request));
            });
        }

        @Override
        public String exampleText() {
            return "$ java -jar bacon.jar brew-push build 8 --tag-prefix=\"1.0-pnc\" --reimport=false";
        }
    }

    @CommandDefinition(name = "group-build", description = "Push group build to Brew")
    public class GroupBuild extends AbstractCommand {

        @Argument(required = true, description = "Id of group-build")
        private String id;
        @Option(required = true, name = "tag-prefix", description = "Brew Tag Prefix")
        private String tagPrefix;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {

            return super.executeHelper(commandInvocation, () -> {

                GroupBuildClient client = new GroupBuildClient(PncClientHelper.getPncConfiguration(true));

                GroupBuildPushRequest request = GroupBuildPushRequest.builder().tagPrefix(tagPrefix).build();

                client.brewPush(id, request);
            });
        }

        @Override
        public String exampleText() {
            return "$ java -jar bacon.jar brew-push group-build 8 --tag-prefix=\"1.0-pnc\"";
        }
    }

    @CommandDefinition(name = "status", description = "Brew Push Status")
    public class Status extends AbstractCommand {

        @Argument(required = true, description = "Brew Push ID")
        private String id;

        @Option(shortName = 'o', overrideRequired = false, hasValue = false, description = "use json for output (default to yaml)")
        private boolean jsonOutput = false;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {

            return super.executeHelper(commandInvocation, () -> {

                BuildClient client = new BuildClient(PncClientHelper.getPncConfiguration(false));

                ObjectHelper.print(jsonOutput, client.getPushResult(id));
            });
        }
    }
}
