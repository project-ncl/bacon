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
import org.aesh.command.GroupCommandDefinition;
import org.jboss.pnc.bacon.common.cli.AbstractCommand;
import org.jboss.pnc.bacon.common.cli.AbstractGetSpecificCommand;
import org.jboss.pnc.bacon.common.cli.AbstractListCommand;
import org.jboss.pnc.bacon.common.cli.AbstractNotImplementedCommand;
import org.jboss.pnc.bacon.pnc.client.PncClientHelper;
import org.jboss.pnc.client.ClientException;
import org.jboss.pnc.client.ProductClient;
import org.jboss.pnc.client.RemoteCollection;
import org.jboss.pnc.client.RemoteResourceException;
import org.jboss.pnc.dto.Product;

import java.util.Optional;

@GroupCommandDefinition(
        name = "product",
        description = "Product",
        groupCommands = {
                ProductCli.Create.class,
                ProductCli.Get.class,
                ProductCli.List.class,
                ProductCli.Update.class
        })
public class ProductCli extends AbstractCommand {

    private static ProductClient clientCache;

    private static ProductClient getClient() {
        if (clientCache == null) {
            clientCache = new ProductClient(PncClientHelper.getPncConfiguration());
        }
        return clientCache;
    }

    @CommandDefinition(name = "create", description = "Create a product")
    public class Create extends AbstractNotImplementedCommand {

    }

    @CommandDefinition(name = "get", description = "Get product")
    public class Get extends AbstractGetSpecificCommand<Product> {

        @Override
        public Product getSpecific(String id) throws ClientException {
            return getClient().getSpecific(id);
        }
    }

    @CommandDefinition(name = "list", description = "List products")
    public class List extends AbstractListCommand<Product> {

        @Override
        public RemoteCollection<Product> getAll(String sort, String query) throws RemoteResourceException {

            return getClient().getAll(Optional.ofNullable(sort), Optional.ofNullable(query));
        }
    }

    @CommandDefinition(name = "update", description = "Update a product")
    public class Update extends AbstractNotImplementedCommand {
    }
}
