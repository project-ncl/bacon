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
import org.jboss.pnc.client.BuildConfigurationClient;
import org.jboss.pnc.client.ClientException;
import org.jboss.pnc.client.RemoteCollection;
import org.jboss.pnc.client.RemoteResourceException;
import org.jboss.pnc.dto.BuildConfiguration;

import java.util.Optional;

@GroupCommandDefinition(
        name = "build-config",
        description = "build-config",
        groupCommands = {
                BuildConfigCli.Create.class,
                BuildConfigCli.Get.class,
                BuildConfigCli.List.class,
                BuildConfigCli.Update.class,
                BuildConfigCli.Delete.class
        })
public class BuildConfigCli extends AbstractCommand {

    private BuildConfigurationClient client = new BuildConfigurationClient(PncClientHelper.getPncConfiguration());

    @CommandDefinition(name = "create", description = "Create a build configuration")
    public class Create extends AbstractNotImplementedCommand {
    }

    @CommandDefinition(name = "get", description = "Get build configuration")
    public class Get extends AbstractGetSpecificCommand<BuildConfiguration> {

        @Override
        public BuildConfiguration getSpecific(int id) throws ClientException {
            return client.getSpecific(id);
        }
    }

    @CommandDefinition(name = "list", description = "List build configurations")
    public class List extends AbstractListCommand<BuildConfiguration> {

        @Override
        public RemoteCollection<BuildConfiguration> getAll(String sort, String query) throws RemoteResourceException {
            return client.getAll(Optional.ofNullable(sort), Optional.ofNullable(query));
        }
    }

    @CommandDefinition(name = "update", description = "Update a build configuration")
    public class Update extends AbstractNotImplementedCommand {
    }

    @CommandDefinition(name = "delete", description = "Delete a build configuration")
    public class Delete extends AbstractNotImplementedCommand {
    }

}
