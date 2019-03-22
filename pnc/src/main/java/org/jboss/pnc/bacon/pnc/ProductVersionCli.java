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
import org.aesh.command.option.Argument;
import org.jboss.pnc.bacon.common.cli.AbstractCommand;
import org.jboss.pnc.bacon.common.cli.AbstractGetSpecificCommand;
import org.jboss.pnc.bacon.common.cli.AbstractListCommand;
import org.jboss.pnc.bacon.pnc.client.PncClientHelper;
import org.jboss.pnc.client.ClientException;
import org.jboss.pnc.client.ProductVersionClient;
import org.jboss.pnc.client.RemoteCollection;
import org.jboss.pnc.client.RemoteResourceException;
import org.jboss.pnc.dto.BuildConfiguration;
import org.jboss.pnc.dto.GroupConfiguration;
import org.jboss.pnc.dto.ProductMilestone;
import org.jboss.pnc.dto.ProductRelease;
import org.jboss.pnc.dto.ProductVersion;

import java.util.Optional;

@GroupCommandDefinition(
        name = "product-version",
        description = "Product Version",
        groupCommands = {
                ProductVersionCli.Get.class,
                ProductVersionCli.ListBuildConfigurations.class,
                ProductVersionCli.ListGroupConfigurations.class,
                ProductVersionCli.ListMilestones.class,
                ProductVersionCli.ListReleases.class
        })
public class ProductVersionCli extends AbstractCommand {

    private static ProductVersionClient client = new ProductVersionClient(PncClientHelper.getPncConfiguration());

    @CommandDefinition(name = "get", description = "Get product version")
    public class Get extends AbstractGetSpecificCommand<ProductVersion> {

        @Override
        public ProductVersion getSpecific(int id) throws ClientException {
            return client.getSpecific(id);
        }
    }

    @CommandDefinition(name = "list-build-configurations",
                       description = "List Build configurations for a particular product version")
    public class ListBuildConfigurations extends AbstractListCommand<BuildConfiguration> {

        @Argument(required = true, description = "Product version id")
        private int id;

        @Override
        public RemoteCollection<BuildConfiguration> getAll(String sort, String query)
                throws RemoteResourceException {

            return client.getBuildConfigurations(id, Optional.ofNullable(sort), Optional.ofNullable(query));
        }
    }

    @CommandDefinition(name = "list-group-configurations",
                       description = "List Group configurations for a particular product version")
    public class ListGroupConfigurations extends AbstractListCommand<GroupConfiguration> {

        @Argument(required = true, description = "Product version id")
        private int id;

        @Override
        public RemoteCollection<GroupConfiguration> getAll(String sort, String query) throws RemoteResourceException {

            return client.getGroupConfigurations(id, Optional.ofNullable(sort), Optional.ofNullable(query));
        }
    }

    @CommandDefinition(name = "list-milestones",
                       description = "List milestones for a particular product version")
    public class ListMilestones extends AbstractListCommand<ProductMilestone> {

        @Argument(required = true, description = "Product version id")
        private int id;

        @Override
        public RemoteCollection<org.jboss.pnc.dto.ProductMilestone> getAll(String sort, String query)
                throws RemoteResourceException {

            return client.getMilestones(id, Optional.ofNullable(sort), Optional.ofNullable(query));
        }
    }

    @CommandDefinition(name = "list-releases",
            description = "List releases for a particular product version")
    public class ListReleases extends AbstractListCommand<ProductRelease> {

        @Argument(required = true, description = "Product version id")
        private int id;

        @Override
        public RemoteCollection<ProductRelease> getAll(String sort, String query)
                throws RemoteResourceException {

            return client.getReleases(id, Optional.ofNullable(sort), Optional.ofNullable(query));
        }
    }
}
