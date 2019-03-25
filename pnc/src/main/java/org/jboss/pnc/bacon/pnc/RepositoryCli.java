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
import org.aesh.command.option.Option;
import org.jboss.pnc.bacon.common.cli.AbstractCommand;
import org.jboss.pnc.bacon.common.cli.AbstractGetSpecificCommand;
import org.jboss.pnc.bacon.common.cli.AbstractListCommand;
import org.jboss.pnc.bacon.common.cli.AbstractNotImplementedCommand;
import org.jboss.pnc.bacon.pnc.client.PncClientHelper;
import org.jboss.pnc.client.ClientException;
import org.jboss.pnc.client.RemoteCollection;
import org.jboss.pnc.client.RemoteResourceException;
import org.jboss.pnc.client.SCMRepositoryClient;
import org.jboss.pnc.dto.SCMRepository;

import java.util.Optional;

@GroupCommandDefinition(
        name = "repository",
        description = "Repository",
        groupCommands = {
                RepositoryCli.Create.class,
                RepositoryCli.Get.class,
                RepositoryCli.List.class,
                RepositoryCli.Update.class,
                RepositoryCli.Delete.class
        })
public class RepositoryCli extends AbstractCommand {

    private SCMRepositoryClient client = new SCMRepositoryClient(PncClientHelper.getPncConfiguration());

    @CommandDefinition(name = "create", description = "Create a repository")
    public class Create extends AbstractNotImplementedCommand {
    }

    @CommandDefinition(name = "get", description = "Get a repository")
    public class Get extends AbstractGetSpecificCommand<SCMRepository> {

        @Override
        public SCMRepository getSpecific(int id) throws ClientException {
            return client.getSpecific(id);
        }
    }

    @CommandDefinition(name = "list", description = "List repositories")
    public class List extends AbstractListCommand<SCMRepository> {

        @Option(description = "Exact URL to search")
        private String matchUrl;

        @Option(description = "Part of the URL to search")
        private String searchUrl;

        @Override
        public RemoteCollection<SCMRepository> getAll(String sort, String query) throws RemoteResourceException {
            return client.getAll(matchUrl, searchUrl, Optional.ofNullable(sort), Optional.ofNullable(query));
        }
    }

    @CommandDefinition(name = "update", description = "Update a repository")
    public class Update extends AbstractNotImplementedCommand {
    }

    @CommandDefinition(name = "delete", description = "Delete a repository")
    public class Delete extends AbstractNotImplementedCommand {
    }
}
