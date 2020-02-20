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

@GroupCommandDefinition(name = "group-config", description = "Group configuration", groupCommands = {
        GroupConfigCli.Create.class, GroupConfigCli.Update.class, GroupConfigCli.List.class, GroupConfigCli.Get.class, })
public class GroupConfigCli extends AbstractCommand {

    private static final ClientCreator<GroupConfigurationClient> CREATOR = new ClientCreator<>(GroupConfigurationClient::new);

    @CommandDefinition(name = "create", description = "Create a group configuration")
    public class Create extends AbstractCommand {

        @Argument(required = true, description = "Name of group configuration")
        private String groupConfigName;

        @Option(name = "product-version-id", description = "Product Version ID")
        private String productVersionId;

        @Option(name = "build-configuration-ids", description = "Build configuration ids in Group Config. Comma separated")
        private String buildConfigurationIds;

        @Option(shortName = 'o', overrideRequired = false, hasValue = false, description = "use json for output (default to yaml)")
        private boolean jsonOutput = false;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {

            return super.executeHelper(commandInvocation, () -> {

                GroupConfiguration.Builder groupConfigurationBuilder = GroupConfiguration.builder().name(groupConfigName);

                ObjectHelper.executeIfNotNull(productVersionId, () -> groupConfigurationBuilder
                        .productVersion(ProductVersionRef.refBuilder().id(productVersionId).build()));

                ObjectHelper.executeIfNotNull(buildConfigurationIds,
                        () -> groupConfigurationBuilder.buildConfigs(addBuildConfigs(buildConfigurationIds)));

                ObjectHelper.print(jsonOutput, CREATOR.getClientAuthenticated().createNew(groupConfigurationBuilder.build()));
            });
        }
    }

    @CommandDefinition(name = "update", description = "Update a group configuration")
    public class Update extends AbstractCommand {

        @Argument(required = true, description = "Group Configuration ID")
        private String groupConfigId;

        @Option(name = "name", description = "Name of group configuration")
        private String groupConfigName;

        @Option(name = "product-version-id", description = "Product Version ID")
        private String productVersionId;

        @Option(name = "build-configuration-ids", description = "Build configuration ids in Group Config. Comma separated")
        private String buildConfigurationIds;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {

            return super.executeHelper(commandInvocation, () -> {

                GroupConfiguration groupConfiguration = CREATOR.getClient().getSpecific(groupConfigId);
                GroupConfiguration.Builder updated = groupConfiguration.toBuilder();

                ObjectHelper.executeIfNotNull(groupConfigName, () -> updated.name(groupConfigName));
                ObjectHelper.executeIfNotNull(productVersionId,
                        () -> updated.productVersion(ProductVersionRef.refBuilder().id(productVersionId).build()));

                ObjectHelper.executeIfNotNull(buildConfigurationIds,
                        () -> updated.buildConfigs(addBuildConfigs(buildConfigurationIds)));

                CREATOR.getClientAuthenticated().update(groupConfigId, updated.build());
            });
        }
    }

    @CommandDefinition(name = "get", description = "Get group configuration")
    public class Get extends AbstractGetSpecificCommand<GroupConfiguration> {

        @Override
        public GroupConfiguration getSpecific(String id) throws ClientException {
            return CREATOR.getClient().getSpecific(id);
        }
    }

    @CommandDefinition(name = "list", description = "List group configurations")
    public class List extends AbstractListCommand<GroupConfiguration> {

        @Override
        public RemoteCollection<GroupConfiguration> getAll(String sort, String query) throws RemoteResourceException {
            return CREATOR.getClient().getAll(Optional.ofNullable(sort), Optional.ofNullable(query));
        }
    }

    private static java.util.Map<String, BuildConfigurationRef> addBuildConfigs(String buildConfigurationIds) {

        java.util.Map<String, BuildConfigurationRef> buildConfigurationRefList = new HashMap<>();

        for (String id : buildConfigurationIds.split(",")) {

            id = id.trim();
            buildConfigurationRefList.put(id, BuildConfigurationRef.refBuilder().id(id).build());
        }

        return buildConfigurationRefList;
    }
}
