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
import org.jboss.pnc.bacon.common.cli.AbstractCommand;
import org.jboss.pnc.bacon.common.cli.AbstractGetSpecificCommand;
import org.jboss.pnc.bacon.common.cli.AbstractListCommand;
import org.jboss.pnc.bacon.pnc.client.PncClientHelper;
import org.jboss.pnc.client.ClientException;
import org.jboss.pnc.client.GroupBuildClient;
import org.jboss.pnc.client.RemoteCollection;
import org.jboss.pnc.client.RemoteResourceException;
import org.jboss.pnc.dto.Build;
import org.jboss.pnc.dto.GroupBuild;

import java.util.Optional;

@GroupCommandDefinition(name = "group-build", description = "Group builds", groupCommands = {
        GroupBuildCli.Cancel.class,
        GroupBuildCli.List.class,
        GroupBuildCli.ListBuilds.class,
        GroupBuildCli.Get.class
})
public class GroupBuildCli extends AbstractCommand {

    private static GroupBuildClient clientCache;

    private static GroupBuildClient getClient() {
        if (clientCache == null) {
            clientCache = new GroupBuildClient(PncClientHelper.getPncConfiguration());
        }
        return clientCache;
    }

    @CommandDefinition(name = "cancel", description = "Cancel a group build")
    public class Cancel extends AbstractCommand {

        @Argument(required = true, description = "Group Build ID")
        private String groupBuildId;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {

            return super.executeHelper(commandInvocation, () -> {
                getClient().cancel(groupBuildId);
            });
        }
    }

    @CommandDefinition(name = "list", description = "List group builds")
    public class List extends AbstractListCommand<GroupBuild> {

        @Override
        public RemoteCollection<GroupBuild> getAll(String sort, String query) throws RemoteResourceException {
            return getClient().getAll(Optional.ofNullable(sort), Optional.ofNullable(query));
        }
    }

    @CommandDefinition(name = "list-builds", description = "List builds associated with the group build")
    public class ListBuilds extends AbstractListCommand<Build> {

        @Argument(required = true, description = "Group Build ID")
        private String groupBuildId;

        @Override
        public RemoteCollection<Build> getAll(String sort, String query) throws RemoteResourceException {
            return getClient().getBuilds(groupBuildId, null, Optional.ofNullable(sort), Optional.ofNullable(query));
        }
    }

    @CommandDefinition(name = "get", description = "Get group build")
    public class Get extends AbstractGetSpecificCommand<GroupBuild> {

        @Override
        public GroupBuild getSpecific(String id) throws ClientException {
            return getClient().getSpecific(id);
        }
    }
}
