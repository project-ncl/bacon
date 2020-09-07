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
import org.jboss.pnc.bacon.common.exception.FatalException;
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
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.util.Optional;
import java.util.concurrent.Callable;

import static org.apache.commons.lang.StringUtils.isNotEmpty;

@Command(
        name = "product-version",
        description = "Product Version",
        subcommands = {
                ProductVersionCli.Create.class,
                ProductVersionCli.Get.class,
                ProductVersionCli.Update.class,
                ProductVersionCli.ListBuildConfigs.class,
                ProductVersionCli.ListGroupConfigs.class,
                ProductVersionCli.ListMilestones.class,
                ProductVersionCli.ListReleases.class })
@Slf4j
public class ProductVersionCli {

    private static final ClientCreator<ProductVersionClient> CREATOR = new ClientCreator<>(ProductVersionClient::new);

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

    @Command(
            name = "create",
            description = "Create a product version",
            footer = Constant.EXAMPLE_TEXT + "$ bacon pnc product-version create --product-id 20 2.1")
    public static class Create implements Callable<Integer> {

        @Parameters(description = "Version of product version")
        private String productVersion;
        @Option(required = true, names = "--product-id", description = "Product ID of product version")
        private String productId;
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
            if (!validateProductVersion(productVersion)) {
                throw new FatalException(
                        "Version specified '" + productVersion + "' is not valid! "
                                + "The version should consist of two numeric parts separated by a dot (e.g 7.0)");
            }

            ProductRef productRef = ProductRef.refBuilder().id(productId).build();

            ProductVersion productVersion = ProductVersion.builder()
                    .product(productRef)
                    .version(this.productVersion)
                    .build();

            try (ProductVersionClient client = CREATOR.newClientAuthenticated()) {
                ObjectHelper.print(jsonOutput, client.createNew(productVersion));
                return 0;
            }
        }
    }

    @Command(name = "get", description = "Get a product version by its id")
    public static class Get extends AbstractGetSpecificCommand<ProductVersion> {

        @Override
        public ProductVersion getSpecific(String id) throws ClientException {
            try (ProductVersionClient client = CREATOR.newClient()) {
                return client.getSpecific(id);
            }
        }
    }

    @Command(name = "list-build-configs", description = "List Build configs for a particular product version")
    public static class ListBuildConfigs extends AbstractListCommand<BuildConfiguration> {

        @Parameters(description = "Product version id")
        private String id;

        @Override
        public RemoteCollection<BuildConfiguration> getAll(String sort, String query) throws RemoteResourceException {

            try (ProductVersionClient client = CREATOR.newClient()) {
                return client.getBuildConfigs(id, Optional.ofNullable(sort), Optional.ofNullable(query));
            }
        }
    }

    @Command(name = "list-group-configs", description = "List Group configs for a particular product version")
    public static class ListGroupConfigs extends AbstractListCommand<GroupConfiguration> {

        @Parameters(description = "Product version id")
        private String id;

        @Override
        public RemoteCollection<GroupConfiguration> getAll(String sort, String query) throws RemoteResourceException {

            try (ProductVersionClient client = CREATOR.newClient()) {
                return client.getGroupConfigs(id, Optional.ofNullable(sort), Optional.ofNullable(query));
            }
        }
    }

    @Command(name = "list-milestones", description = "List milestones for a particular product version")
    public static class ListMilestones extends AbstractListCommand<ProductMilestone> {

        @Parameters(description = "Product version id")
        private String id;

        @Override
        public RemoteCollection<org.jboss.pnc.dto.ProductMilestone> getAll(String sort, String query)
                throws RemoteResourceException {

            try (ProductVersionClient client = CREATOR.newClient()) {
                return client.getMilestones(id, Optional.ofNullable(sort), Optional.ofNullable(query));
            }
        }
    }

    @Command(name = "list-releases", description = "List releases for a particular product version")
    public static class ListReleases extends AbstractListCommand<ProductRelease> {

        @Parameters(description = "Product version id")
        private String id;

        @Override
        public RemoteCollection<ProductRelease> getAll(String sort, String query) throws RemoteResourceException {

            try (ProductVersionClient client = CREATOR.newClient()) {
                return client.getReleases(id, Optional.ofNullable(sort), Optional.ofNullable(query));
            }
        }
    }

    @Command(
            name = "update",
            description = "Update a particular product version",
            footer = Constant.EXAMPLE_TEXT + "$ bacon pnc product-version update --product-version 2.2 42")
    public static class Update implements Callable<Integer> {

        @Parameters(description = "Product Version ID to update")
        private String id;

        @Option(names = "--product-version", description = "Version of product version")
        private String productVersion;

        /**
         * Computes a result, or throws an exception if unable to do so.
         *
         * @return computed result
         * @throws Exception if unable to compute a result
         */
        @Override
        public Integer call() throws Exception {
            try (ProductVersionClient client = CREATOR.newClient()) {
                ProductVersion pV = client.getSpecific(id);
                ProductVersion.Builder updated = pV.toBuilder();
                if (isNotEmpty(productVersion)) {
                    if (!validateProductVersion(productVersion)) {
                        throw new FatalException(
                                "Version specified ('{}') is not valid! The version should consist of two numeric parts separated by a dot (e.g 7.0)",
                                productVersion);
                    }
                    updated.version(productVersion);
                }
                try (ProductVersionClient clientAuthenticated = CREATOR.newClientAuthenticated()) {
                    clientAuthenticated.update(id, updated.build());
                    return 0;
                }
            }
        }
    }
}
