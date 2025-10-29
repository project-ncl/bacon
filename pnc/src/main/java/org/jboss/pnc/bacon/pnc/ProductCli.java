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

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.Callable;

import org.jboss.pnc.bacon.common.Constant;
import org.jboss.pnc.bacon.common.ObjectHelper;
import org.jboss.pnc.bacon.common.cli.AbstractGetSpecificCommand;
import org.jboss.pnc.bacon.common.cli.AbstractListCommand;
import org.jboss.pnc.bacon.common.cli.JSONCommandHandler;
import org.jboss.pnc.bacon.pnc.common.ClientCreator;
import org.jboss.pnc.client.ClientException;
import org.jboss.pnc.client.ProductClient;
import org.jboss.pnc.client.RemoteResourceException;
import org.jboss.pnc.dto.Product;
import org.jboss.pnc.dto.ProductVersion;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(
        name = "product",
        description = "Product",
        subcommands = {
                ProductCli.Create.class,
                ProductCli.Get.class,
                ProductCli.List.class,
                ProductCli.ListVersions.class,
                ProductCli.Update.class })
public class ProductCli {

    private static final ClientCreator<ProductClient> CREATOR = new ClientCreator<>(ProductClient::new);

    @Command(
            name = "create",
            description = "Create a product",
            footer = Constant.EXAMPLE_TEXT + "$ bacon pnc product create --abbreviation testing Testing")
    public static class Create extends JSONCommandHandler implements Callable<Integer> {

        @Parameters(description = "Name of product")
        private String name;

        @Option(required = true, names = "--abbreviation", description = "Abbreviation of product")
        private String abbreviation;

        @Option(names = "--description", description = "Description of product", defaultValue = "")
        private String description;

        /**
         * Computes a result, or throws an exception if unable to do so.
         *
         * @return computed result
         * @throws Exception if unable to compute a result
         */
        @Override
        public Integer call() throws Exception {
            Product product = Product.builder().name(name).abbreviation(abbreviation).description(description).build();

            try (ProductClient client = CREATOR.newClientAuthenticated()) {
                ObjectHelper.print(getJsonOutput(), client.createNew(product));
                return 0;
            }
        }
    }

    @Command(name = "get", description = "Get a product by its id")
    public static class Get extends AbstractGetSpecificCommand<Product> {

        @Override
        public Product getSpecific(String id) throws ClientException {
            try (ProductClient client = CREATOR.newClient()) {
                return client.getSpecific(id);
            }
        }
    }

    @Command(name = "list", description = "List products")
    public static class List extends AbstractListCommand<Product> {

        @Override
        public Collection<Product> getAll(String sort, String query) throws RemoteResourceException {

            try (ProductClient client = CREATOR.newClient()) {
                return client.getAll(Optional.ofNullable(sort), Optional.ofNullable(query)).getAll();
            }
        }
    }

    @Command(name = "list-versions", description = "List versions of product")
    public static class ListVersions extends AbstractListCommand<ProductVersion> {

        @Parameters(description = "Product Id")
        private String id;

        @Override
        public Collection<ProductVersion> getAll(String sort, String query) throws RemoteResourceException {

            try (ProductClient client = CREATOR.newClient()) {
                return client.getProductVersions(id, Optional.ofNullable(sort), Optional.ofNullable(query)).getAll();
            }
        }
    }

    @Command(
            name = "update",
            description = "Update a product",
            footer = Constant.EXAMPLE_TEXT + "$ bacon pnc product update --abbreviation testingme2 42")
    public static class Update implements Callable<Integer> {

        @Parameters(description = "Product Id")
        private String id;

        @Option(names = "--name", description = "Name of product")
        private String name;

        @Option(names = "--abbreviation", description = "Abbreviation of product")
        private String abbreviation;

        @Option(names = "--description", description = "Description of product", defaultValue = "")
        private String description;

        /**
         * Computes a result, or throws an exception if unable to do so.
         *
         * @return computed result
         * @throws Exception if unable to compute a result
         */
        @Override
        public Integer call() throws Exception {
            try (ProductClient client = CREATOR.newClient()) {
                Product product = client.getSpecific(id);
                Product.Builder updated = product.toBuilder();

                if (isNotEmpty(name)) {
                    updated.name(name);
                }
                if (isNotEmpty(abbreviation)) {
                    updated.abbreviation(abbreviation);
                }
                if (isNotEmpty(description)) {
                    updated.description(description);
                }

                try (ProductClient authenticatedClient = CREATOR.newClientAuthenticated()) {
                    authenticatedClient.update(id, updated.build());
                    return 0;
                }
            }
        }
    }
}
