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
import org.jboss.pnc.bacon.common.Constant;
import org.jboss.pnc.bacon.common.ObjectHelper;
import org.jboss.pnc.bacon.common.cli.AbstractGetSpecificCommand;
import org.jboss.pnc.bacon.common.cli.AbstractListCommand;
import org.jboss.pnc.bacon.common.cli.JSONCommandHandler;
import org.jboss.pnc.bacon.pnc.common.ClientCreator;
import org.jboss.pnc.client.ClientException;
import org.jboss.pnc.client.GroupConfigurationClient;
import org.jboss.pnc.client.RemoteCollection;
import org.jboss.pnc.client.RemoteResourceException;
import org.jboss.pnc.dto.BuildConfiguration;
import org.jboss.pnc.dto.BuildConfigurationRef;
import org.jboss.pnc.dto.GroupBuild;
import org.jboss.pnc.dto.GroupConfiguration;
import org.jboss.pnc.dto.ProductVersionRef;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.util.Collection;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.Callable;

import static java.util.Optional.of;
import static org.apache.commons.lang.StringUtils.isNotEmpty;

@Slf4j
@Command(
        name = "group-config",
        description = "Group config",
        subcommands = {
                GroupConfigCli.Create.class,
                GroupConfigCli.Update.class,
                GroupConfigCli.List.class,
                GroupConfigCli.ListBuildConfig.class,
                GroupConfigCli.Get.class,
                GroupConfigCli.AddBuildConfig.class,
                GroupConfigCli.RemoveBuildConfig.class,
                GroupConfigCli.ShowLatestBuild.class })
public class GroupConfigCli {

    private static final ClientCreator<GroupConfigurationClient> CREATOR = new ClientCreator<>(
            GroupConfigurationClient::new);

    private static java.util.Map<String, BuildConfigurationRef> addBuildConfigs(String buildConfigIds) {

        java.util.Map<String, BuildConfigurationRef> buildConfigurationRefList = new HashMap<>();

        for (String id : buildConfigIds.split(",")) {

            id = id.trim();
            buildConfigurationRefList.put(id, BuildConfigurationRef.refBuilder().id(id).build());
        }

        return buildConfigurationRefList;
    }

    @Command(
            name = "create",
            description = "Create a group config",
            footer = Constant.EXAMPLE_TEXT
                    + "$ bacon pnc group-config create --build-config-ids 100,200,300 group-config-new-name")
    public static class Create extends JSONCommandHandler implements Callable<Integer> {

        @Parameters(description = "Name of group config")
        private String groupConfigName;

        @Option(names = "--product-version-id", description = "Product Version ID")
        private String productVersionId;

        @Option(names = "--build-config-ids", description = "Build config ids in Group Config. Comma separated")
        private String buildConfigIds;

        /**
         * Computes a result, or throws an exception if unable to do so.
         *
         * @return computed result
         * @throws Exception if unable to compute a result
         */
        @Override
        public Integer call() throws Exception {
            GroupConfiguration.Builder groupConfigurationBuilder = GroupConfiguration.builder().name(groupConfigName);

            if (isNotEmpty(productVersionId)) {
                groupConfigurationBuilder.productVersion(ProductVersionRef.refBuilder().id(productVersionId).build());
            }
            if (isNotEmpty(buildConfigIds)) {
                groupConfigurationBuilder.buildConfigs(addBuildConfigs(buildConfigIds));
            }
            try (GroupConfigurationClient client = CREATOR.newClientAuthenticated()) {
                ObjectHelper.print(getJsonOutput(), client.createNew(groupConfigurationBuilder.build()));
                return 0;
            }
        }
    }

    @Command(
            name = "update",
            description = "Update a group config",
            footer = Constant.EXAMPLE_TEXT + "$ bacon pnc group-config update --name group-config-updated 503")
    public static class Update implements Callable<Integer> {

        @Parameters(description = "Group Config ID")
        private String groupConfigId;

        @Option(names = "--name", description = "Name of group config")
        private String groupConfigName;

        @Option(names = "--product-version-id", description = "Product Version ID")
        private String productVersionId;

        @Option(names = "--build-config-ids", description = "Build config ids in Group Config. Comma separated")
        private String buildConfigIds;

        /**
         * Computes a result, or throws an exception if unable to do so.
         *
         * @return computed result
         * @throws Exception if unable to compute a result
         */
        @Override
        public Integer call() throws Exception {
            try (GroupConfigurationClient client = CREATOR.newClient()) {
                GroupConfiguration groupConfiguration = client.getSpecific(groupConfigId);
                GroupConfiguration.Builder updated = groupConfiguration.toBuilder();

                if (isNotEmpty(groupConfigName)) {
                    updated.name(groupConfigName);
                }
                if (isNotEmpty(productVersionId)) {
                    updated.productVersion(ProductVersionRef.refBuilder().id(productVersionId).build());
                }
                if (isNotEmpty(buildConfigIds)) {
                    updated.buildConfigs(addBuildConfigs(buildConfigIds));
                }
                try (GroupConfigurationClient authenticatedClient = CREATOR.newClientAuthenticated()) {
                    authenticatedClient.update(groupConfigId, updated.build());
                    return 0;
                }
            }
        }
    }

    @Command(name = "get", description = "Get a group config by its id")
    public static class Get extends AbstractGetSpecificCommand<GroupConfiguration> {

        @Override
        public GroupConfiguration getSpecific(String id) throws ClientException {
            try (GroupConfigurationClient client = CREATOR.newClient()) {
                return client.getSpecific(id);
            }
        }
    }

    @Command(name = "list", description = "List group configs")
    public static class List extends AbstractListCommand<GroupConfiguration> {

        @Override
        public RemoteCollection<GroupConfiguration> getAll(String sort, String query) throws RemoteResourceException {
            try (GroupConfigurationClient client = CREATOR.newClient()) {
                return client.getAll(Optional.ofNullable(sort), Optional.ofNullable(query));
            }
        }
    }

    @Command(name = "list-build-configs", description = "List build configs of group config")
    public static class ListBuildConfig extends AbstractListCommand<BuildConfiguration> {

        @Parameters(description = "Group Config id")
        private String id;

        @Override
        public RemoteCollection<BuildConfiguration> getAll(String sort, String query) throws RemoteResourceException {
            try (GroupConfigurationClient client = CREATOR.newClient()) {
                return client.getBuildConfigs(id, Optional.ofNullable(sort), Optional.ofNullable(query));
            }
        }
    }

    @Command(name = "add-build-config", description = "Add build config to group config")
    public static class AddBuildConfig implements Callable<Integer> {

        @Parameters(description = "Group config id")
        private String id;

        @Option(
                names = "--bc-id",
                required = true,
                description = "ID of the build configuration to add. You cen enter multiple ids separated by comma.")
        private java.util.List<String> attributes;

        /**
         * Computes a result, or throws an exception if unable to do so.
         *
         * @return computed result
         * @throws Exception if unable to compute a result
         */
        @Override
        public Integer call() throws Exception {
            for (String bcid : attributes) {
                try (GroupConfigurationClient client = CREATOR.newClientAuthenticated()) {
                    client.addBuildConfig(id, BuildConfigurationRef.refBuilder().id(bcid).build());
                }
            }
            return 0;
        }
    }

    @Command(name = "remove-build-config", description = "Remove build config from group config")
    public static class RemoveBuildConfig implements Callable<Integer> {

        @Parameters(description = "Group config id")
        private String id;

        @Option(
                names = "--bc-id",
                required = true,
                description = "ID of the build configuration to remove. You cen enter multiple ids separated by comma.")
        private java.util.List<String> attributes;

        /**
         * Computes a result, or throws an exception if unable to do so.
         *
         * @return computed result
         * @throws Exception if unable to compute a result
         */
        @Override
        public Integer call() throws Exception {
            try (GroupConfigurationClient client = CREATOR.newClientAuthenticated()) {
                for (String bcid : attributes) {
                    client.removeBuildConfig(id, bcid);
                }
                return 0;
            }
        }
    }

    @Command(
            name = "show-latest-build",
            description = "Show the progress of the latest group build for the group config")
    public static class ShowLatestBuild extends JSONCommandHandler implements Callable<Integer> {

        @Parameters(description = "Group config id")
        private String id;

        @Option(names = "--temporary-build", description = "Build is temporary")
        private boolean temporaryBuild;

        /**
         * Computes a result, or throws an exception if unable to do so.
         *
         * @return computed result
         * @throws Exception if unable to compute a result
         */
        @Override
        public Integer call() throws Exception {
            try (GroupConfigurationClient client = CREATOR.newClient()) {
                Collection<GroupBuild> groupBuilds = client
                        .getAllGroupBuilds(id, of("=desc=startTime"), Optional.of("temporaryBuild==" + temporaryBuild))
                        .getAll();
                Optional<GroupBuild> latest = groupBuilds.stream().findFirst();
                if (latest.isPresent()) {
                    ObjectHelper.print(getJsonOutput(), latest.get());
                    return 0;
                } else {
                    log.error("Couldn't find any group build from group config id: {}", id);
                    return 1;
                }
            }
        }
    }
}
