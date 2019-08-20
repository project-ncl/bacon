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
import org.jboss.pnc.bacon.common.cli.AbstractCommand;
import org.jboss.pnc.bacon.common.cli.AbstractGetSpecificCommand;
import org.jboss.pnc.bacon.pnc.client.PncClientHelper;
import org.jboss.pnc.client.ClientException;
import org.jboss.pnc.client.ProductReleaseClient;
import org.jboss.pnc.dto.ProductRelease;
import org.jboss.pnc.enums.SupportLevel;

@GroupCommandDefinition(
        name = "product-release",
        description = "Product Release",
        groupCommands = {
                ProductReleaseCli.Get.class,
                ProductReleaseCli.ListSupportLevel.class
        })
public class ProductReleaseCli extends AbstractCommand {

    private static ProductReleaseClient clientCache;

    private static ProductReleaseClient getClient() {
        if (clientCache == null) {
           clientCache = new ProductReleaseClient(PncClientHelper.getPncConfiguration());
        }
        return clientCache;
    }

    @CommandDefinition(name = "get", description = "Get product release")
    public class Get extends AbstractGetSpecificCommand<ProductRelease> {

        @Override
        public ProductRelease getSpecific(int id) throws ClientException {
            return getClient().getSpecific(id);
        }
    }

    @CommandDefinition(name = "list-support-levels", description = "List supported levels")
    public class ListSupportLevel extends AbstractCommand {

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {

            return super.executeHelper(commandInvocation, () -> {
                // TODO: YAML or JSON format?
                for (SupportLevel supportLevel : getClient().getAllSupportLevel()) {
                    System.out.println(supportLevel);
                }
            });
        }
    }
}
