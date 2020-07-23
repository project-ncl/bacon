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
import org.aesh.command.option.Option;
import org.jboss.pnc.bacon.common.ObjectHelper;
import org.jboss.pnc.bacon.common.cli.AbstractCommand;
import org.jboss.pnc.bacon.common.cli.AbstractGetSpecificCommand;
import org.jboss.pnc.bacon.common.cli.AbstractListCommand;
import org.jboss.pnc.bacon.pnc.common.ClientCreator;
import org.jboss.pnc.client.ClientException;
import org.jboss.pnc.client.ProjectClient;
import org.jboss.pnc.client.RemoteCollection;
import org.jboss.pnc.client.RemoteResourceException;
import org.jboss.pnc.dto.Build;
import org.jboss.pnc.dto.BuildConfiguration;
import org.jboss.pnc.dto.Project;

import java.util.Optional;
import org.jboss.pnc.bacon.pnc.common.AbstractBuildListCommand;
import org.jboss.pnc.rest.api.parameters.BuildsFilterParameters;

@GroupCommandDefinition(
        name = "project",
        description = "Project",
        groupCommands = {
                ProjectCli.Create.class,
                ProjectCli.Get.class,
                ProjectCli.List.class,
                ProjectCli.ListBuildConfigurations.class,
                ProjectCli.ListBuilds.class,
                ProjectCli.Update.class, })
public class ProjectCli extends AbstractCommand {

    private static final ClientCreator<ProjectClient> CREATOR = new ClientCreator<>(ProjectClient::new);

    @CommandDefinition(name = "create", description = "Create a project")
    public class Create extends AbstractCommand {

        @Argument(required = true, description = "Name of project")
        private String name;
        @Option(name = "description", description = "Description of project", defaultValue = "")
        private String description;
        @Option(name = "project-url", description = "Project-URL of project", defaultValue = "")
        private String projectUrl;
        @Option(name = "issue-tracker-url", description = "Issue-Tracker-URL of project", defaultValue = "")
        private String issueTrackerUrl;
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

                Project project = Project.builder()
                        .name(name)
                        .description(description)
                        .projectUrl(projectUrl)
                        .issueTrackerUrl(issueTrackerUrl)
                        .build();

                ObjectHelper.print(jsonOutput, CREATOR.getClientAuthenticated().createNew(project));
            });
        }

        @Override
        public String exampleText() {
            return "$ bacon pnc project create --description \"Morning sunshine\" best-project-ever";
        }
    }

    @CommandDefinition(name = "get", description = "Get a project")
    public class Get extends AbstractGetSpecificCommand<Project> {

        @Override
        public Project getSpecific(String id) throws ClientException {
            return CREATOR.getClient().getSpecific(id);
        }
    }

    @CommandDefinition(name = "list", description = "List projects")
    public class List extends AbstractListCommand<Project> {

        @Override
        public RemoteCollection<Project> getAll(String sort, String query) throws RemoteResourceException {
            return CREATOR.getClient().getAll(Optional.ofNullable(sort), Optional.ofNullable(query));
        }
    }

    @CommandDefinition(name = "list-build-configs", description = "List build configs for a project")
    public class ListBuildConfigurations extends AbstractListCommand<BuildConfiguration> {

        @Argument(required = true, description = "Project id")
        private String id;

        @Override
        public RemoteCollection<BuildConfiguration> getAll(String sort, String query) throws RemoteResourceException {
            return CREATOR.getClient()
                    .getBuildConfigurations(id, Optional.ofNullable(sort), Optional.ofNullable(query));
        }
    }

    @CommandDefinition(name = "list-builds", description = "List builds for a project")
    public class ListBuilds extends AbstractBuildListCommand {

        @Argument(required = true, description = "Project id")
        private String id;

        @Override
        public RemoteCollection<Build> getAll(BuildsFilterParameters buildsFilter, String sort, String query)
                throws RemoteResourceException {
            return CREATOR.getClient()
                    .getBuilds(id, buildsFilter, Optional.ofNullable(sort), Optional.ofNullable(query));
        }
    }

    @CommandDefinition(name = "update", description = "Update a project")
    public class Update extends AbstractCommand {

        @Argument(required = true, description = "Project id")
        private String id;

        @Option(name = "name", description = "Name of project")
        private String name;
        @Option(name = "description", description = "Description of project")
        private String description;
        @Option(name = "project-url", description = "Project-URL of project")
        private String projectUrl;
        @Option(name = "issue-tracker-url", description = "Issue-Tracker-URL of project")
        private String issueTrackerUrl;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation)
                throws CommandException, InterruptedException {

            return super.executeHelper(commandInvocation, () -> {
                Project project = CREATOR.getClient().getSpecific(id);
                Project.Builder updated = project.toBuilder();

                ObjectHelper.executeIfNotNull(name, () -> updated.name(name));
                ObjectHelper.executeIfNotNull(description, () -> updated.description(description));
                ObjectHelper.executeIfNotNull(projectUrl, () -> updated.projectUrl(projectUrl));
                ObjectHelper.executeIfNotNull(issueTrackerUrl, () -> updated.issueTrackerUrl(issueTrackerUrl));

                CREATOR.getClientAuthenticated().update(id, updated.build());
            });
        }

        @Override
        public String exampleText() {
            return "bacon pnc project update --name \"bad-guy\" 1";
        }
    }
}
