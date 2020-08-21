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
import org.jboss.pnc.bacon.common.Fail;
import org.jboss.pnc.bacon.common.ObjectHelper;
import org.jboss.pnc.bacon.common.cli.AbstractCommand;
import org.jboss.pnc.bacon.common.cli.AbstractGetSpecificCommand;
import org.jboss.pnc.bacon.common.cli.AbstractListCommand;
import org.jboss.pnc.bacon.pnc.common.ClientCreator;
import org.jboss.pnc.client.ClientException;
import org.jboss.pnc.client.ProductVersionClient;
import org.jboss.pnc.client.RemoteCollection;
import org.jboss.pnc.client.RemoteResourceException;
import org.jboss.pnc.dto.BuildConfiguration;
import org.jboss.pnc.dto.GroupConfiguration;
import org.jboss.pnc.dto.ProductMilestone;
import org.jboss.pnc.dto.ProductRef;
import org.jboss.pnc.dto.ProductRelease;
import org.jboss.pnc.dto.ProductVersion;

import java.util.Optional;

@GroupCommandDefinition(
        name = "product-version",
        description = "Product Version",
        groupCommands = {
                ProductVersionCli.Create.class,
                ProductVersionCli.Get.class,
                ProductVersionCli.Update.class,
                ProductVersionCli.ListBuildConfigs.class,
                ProductVersionCli.ListGroupConfigs.class,
                ProductVersionCli.ListMilestones.class,
                ProductVersionCli.ListReleases.class })
@Slf4j
public class ProductVersionCli extends AbstractCommand {

    private static final ClientCreator<ProductVersionClient> CREATOR = new ClientCreator<>(ProductVersionClient::new);

    @CommandDefinition(name = "create", description = "Create a product version")
    public class Create extends AbstractCommand {

        @Argument(required = true, description = "Version of product version")
        private String productVersion;
        @Option(required = true, name = "product-id", description = "Product ID of product version")
        private String productId;
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

                if (!validateProductVersion(productVersion)) {
                    Fail.fail(
                            "Version specified '" + productVersion + "' is not valid! "
                                    + "The version should consist of two numeric parts separated by a dot (e.g 7.0)");
                }

                ProductRef productRef = ProductRef.refBuilder().id(productId).build();

                ProductVersion productVersion = ProductVersion.builder()
                        .product(productRef)
                        .version(this.productVersion)
                        .build();

                ObjectHelper.print(jsonOutput, CREATOR.getClientAuthenticated().createNew(productVersion));
                return 0;
            });
        }

        @Override
        public String exampleText() {
            return "$ bacon pnc product-version create --product-id 20 2.1";
        }
    }

    @CommandDefinition(name = "get", description = "Get a product version by its id")
    public class Get extends AbstractGetSpecificCommand<ProductVersion> {

        @Override
        public ProductVersion getSpecific(String id) throws ClientException {
            return CREATOR.getClient().getSpecific(id);
        }
    }

    @CommandDefinition(name = "list-build-configs", description = "List Build configs for a particular product version")
    public class ListBuildConfigs extends AbstractListCommand<BuildConfiguration> {

        @Argument(required = true, description = "Product version id")
        private String id;

        @Override
        public RemoteCollection<BuildConfiguration> getAll(String sort, String query) throws RemoteResourceException {

            return CREATOR.getClient().getBuildConfigs(id, Optional.ofNullable(sort), Optional.ofNullable(query));
        }
    }

    @CommandDefinition(name = "list-group-configs", description = "List Group configs for a particular product version")
    public class ListGroupConfigs extends AbstractListCommand<GroupConfiguration> {

        @Argument(required = true, description = "Product version id")
        private String id;

        @Override
        public RemoteCollection<GroupConfiguration> getAll(String sort, String query) throws RemoteResourceException {

            return CREATOR.getClient().getGroupConfigs(id, Optional.ofNullable(sort), Optional.ofNullable(query));
        }
    }

    @CommandDefinition(name = "list-milestones", description = "List milestones for a particular product version")
    public class ListMilestones extends AbstractListCommand<ProductMilestone> {

        @Argument(required = true, description = "Product version id")
        private String id;

        @Override
        public RemoteCollection<org.jboss.pnc.dto.ProductMilestone> getAll(String sort, String query)
                throws RemoteResourceException {

            return CREATOR.getClient().getMilestones(id, Optional.ofNullable(sort), Optional.ofNullable(query));
        }
    }

    @CommandDefinition(name = "list-releases", description = "List releases for a particular product version")
    public class ListReleases extends AbstractListCommand<ProductRelease> {

        @Argument(required = true, description = "Product version id")
        private String id;

        @Override
        public RemoteCollection<ProductRelease> getAll(String sort, String query) throws RemoteResourceException {

            return CREATOR.getClient().getReleases(id, Optional.ofNullable(sort), Optional.ofNullable(query));
        }
    }

    @CommandDefinition(name = "update", description = "Update a particular product version")
    public class Update extends AbstractCommand {

        @Argument(required = true, description = "Product Version ID to update")
        private String id;

        @Option(name = "product-version", description = "Version of product version")
        private String productVersion;

        public CommandResult execute(CommandInvocation commandInvocation)
                throws CommandException, InterruptedException {

            return super.executeHelper(commandInvocation, () -> {

                ProductVersion pV = CREATOR.getClient().getSpecific(id);
                ProductVersion.Builder updated = pV.toBuilder();
                ObjectHelper.executeIfNotNull(productVersion, () -> {

                    if (!validateProductVersion(productVersion)) {
                        Fail.fail(
                                "Version specified '" + productVersion + "' is not valid! "
                                        + "The version should consist of two numeric parts separated by a dot (e.g 7.0)");
                    }

                    updated.version(productVersion);
                });
                CREATOR.getClientAuthenticated().update(id, updated.build());
                return 0;
            });
        }

        @Override
        public String exampleText() {
            return "$ bacon pnc product-version update --product-version 2.2 42";
        }
    }

    /**
     * A valid version is one that contains 2 numeric parts seperated by a dot
     *
     * @param version
     * @return
     */
    public static boolean validateProductVersion(String version) {

        String[] items = version.split("\\.");

        if (items.length != 2) {
            return false;
        } else {
            for (String item : items) {
                if (!item.matches("\\d+")) {
                    return false;
                }
            }
        }
        return true;
    }
}
