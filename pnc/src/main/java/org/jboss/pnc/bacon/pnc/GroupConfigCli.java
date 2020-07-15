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
import org.jboss.pnc.bacon.common.cli.AbstractGetSpecificCommand;
import org.jboss.pnc.bacon.common.cli.AbstractListCommand;
import org.jboss.pnc.bacon.pnc.common.ClientCreator;
import org.jboss.pnc.client.ClientException;
import org.jboss.pnc.client.GroupConfigurationClient;
import org.jboss.pnc.client.RemoteCollection;
import org.jboss.pnc.client.RemoteResourceException;
import org.jboss.pnc.dto.BuildConfigurationRef;
import org.jboss.pnc.dto.GroupConfiguration;
import org.jboss.pnc.dto.ProductVersionRef;

import java.util.HashMap;
import java.util.Optional;
import org.aesh.command.option.OptionList;

@GroupCommandDefinition(
        name = "group-config",
        description = "Group config",
        groupCommands = { GroupConfigCli.Create.class, GroupConfigCli.Update.class, GroupConfigCli.List.class,
                GroupConfigCli.Get.class, })
public class GroupConfigCli extends AbstractCommand {

    private static final ClientCreator<GroupConfigurationClient> CREATOR = new ClientCreator<>(
            GroupConfigurationClient::new);

    @CommandDefinition(name = "create", description = "Create a group config")
    public class Create extends AbstractCommand {

        @Argument(required = true, description = "Name of group config")
        private String groupConfigName;

        @Option(name = "product-version-id", description = "Product Version ID")
        private String productVersionId;

        @Option(name = "build-config-ids", description = "Build config ids in Group Config. Comma separated")
        private String buildConfigIds;

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

                GroupConfiguration.Builder groupConfigurationBuilder = GroupConfiguration.builder()
                        .name(groupConfigName);

                ObjectHelper.executeIfNotNull(
                        productVersionId,
                        () -> groupConfigurationBuilder
                                .productVersion(ProductVersionRef.refBuilder().id(productVersionId).build()));

                ObjectHelper.executeIfNotNull(
                        buildConfigIds,
                        () -> groupConfigurationBuilder.buildConfigs(addBuildConfigs(buildConfigIds)));

                ObjectHelper.print(
                        jsonOutput,
                        CREATOR.getClientAuthenticated().createNew(groupConfigurationBuilder.build()));
            });
        }

        @Override
        public String exampleText() {
            return "$ bacon pnc group-config create --build-config-ids 100,200,300 group-config-new-name";
        }
    }

    @CommandDefinition(name = "update", description = "Update a group config")
    public class Update extends AbstractCommand {

        @Argument(required = true, description = "Group Config ID")
        private String groupConfigId;

        @Option(name = "name", description = "Name of group config")
        private String groupConfigName;

        @Option(name = "product-version-id", description = "Product Version ID")
        private String productVersionId;

        @Option(name = "build-config-ids", description = "Build config ids in Group Config. Comma separated")
        private String buildConfigIds;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation)
                throws CommandException, InterruptedException {

            return super.executeHelper(commandInvocation, () -> {

                GroupConfiguration groupConfiguration = CREATOR.getClient().getSpecific(groupConfigId);
                GroupConfiguration.Builder updated = groupConfiguration.toBuilder();

                ObjectHelper.executeIfNotNull(groupConfigName, () -> updated.name(groupConfigName));
                ObjectHelper.executeIfNotNull(
                        productVersionId,
                        () -> updated.productVersion(ProductVersionRef.refBuilder().id(productVersionId).build()));

                ObjectHelper
                        .executeIfNotNull(buildConfigIds, () -> updated.buildConfigs(addBuildConfigs(buildConfigIds)));

                CREATOR.getClientAuthenticated().update(groupConfigId, updated.build());
            });
        }

        @Override
        public String exampleText() {
            return "$ bacon pnc group-config update --name group-config-updated 503";
        }
    }

    @CommandDefinition(name = "get", description = "Get group config")
    public class Get extends AbstractGetSpecificCommand<GroupConfiguration> {

        @Override
        public GroupConfiguration getSpecific(String id) throws ClientException {
            return CREATOR.getClient().getSpecific(id);
        }
    }

    @CommandDefinition(name = "list", description = "List group configs")
    public class List extends AbstractListCommand<GroupConfiguration> {

        @Override
        public RemoteCollection<GroupConfiguration> getAll(String sort, String query) throws RemoteResourceException {
            return CREATOR.getClient().getAll(Optional.ofNullable(sort), Optional.ofNullable(query));
        }
    }

    @CommandDefinition(name = "add-build-config", description = "Add build config to group config")
    public class AddBuildConfig extends AbstractCommand {

        @Argument(required = true, description = "Group config id")
        private String id;

        @OptionList(
                name = "bc-id",
                required = true,
                description = "ID of the build configuration to add. You cen enter multiple ids separated by comma.")
        private java.util.List<String> attributes;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation)
                throws CommandException, InterruptedException {

            return super.executeHelper(commandInvocation, () -> {
                for (String bcid : attributes) {
                    CREATOR.getClient().addBuildConfig(id, BuildConfigurationRef.refBuilder().id(bcid).build());
                }
            });
        }
    }

    @CommandDefinition(name = "remove-build-config", description = "Remove build config from group config")
    public class RemoveBuildConfig extends AbstractCommand {

        @Argument(required = true, description = "Group config id")
        private String id;

        @OptionList(
                name = "bc-id",
                required = true,
                description = "ID of the build configuration to remove. You cen enter multiple ids separated by comma.")
        private java.util.List<String> attributes;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation)
                throws CommandException, InterruptedException {

            return super.executeHelper(commandInvocation, () -> {
                for (String bcid : attributes) {
                    CREATOR.getClient().removeBuildConfig(id, bcid);
                }
            });
        }
    }

    private static java.util.Map<String, BuildConfigurationRef> addBuildConfigs(String buildConfigIds) {

        java.util.Map<String, BuildConfigurationRef> buildConfigurationRefList = new HashMap<>();

        for (String id : buildConfigIds.split(",")) {

            id = id.trim();
            buildConfigurationRefList.put(id, BuildConfigurationRef.refBuilder().id(id).build());
        }

        return buildConfigurationRefList;
    }
}
