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

import org.jboss.pnc.bacon.common.Constant;
import org.jboss.pnc.bacon.common.ObjectHelper;
import org.jboss.pnc.bacon.common.cli.AbstractBuildListCommand;
import org.jboss.pnc.bacon.common.cli.AbstractGetSpecificCommand;
import org.jboss.pnc.bacon.common.cli.AbstractListCommand;
import org.jboss.pnc.bacon.common.cli.JSONCommandHandler;
import org.jboss.pnc.bacon.pnc.common.ClientCreator;
import org.jboss.pnc.client.ClientException;
import org.jboss.pnc.client.ProjectClient;
import org.jboss.pnc.client.RemoteResourceException;
import org.jboss.pnc.dto.Build;
import org.jboss.pnc.dto.BuildConfiguration;
import org.jboss.pnc.dto.Project;
import org.jboss.pnc.rest.api.parameters.BuildsFilterParameters;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.Callable;

import static org.apache.commons.lang.StringUtils.isNotEmpty;

@Command(
        name = "project",
        description = "Project",
        subcommands = {
                ProjectCli.Create.class,
                ProjectCli.Get.class,
                ProjectCli.List.class,
                ProjectCli.ListBuildConfigurations.class,
                ProjectCli.ListBuilds.class,
                ProjectCli.Update.class, })
public class ProjectCli {

    private static final ClientCreator<ProjectClient> CREATOR = new ClientCreator<>(ProjectClient::new);

    @Command(
            name = "create",
            description = "Create a project",
            footer = Constant.EXAMPLE_TEXT
                    + "$ bacon pnc project create --description \"Morning sunshine\" best-project-ever")
    public static class Create extends JSONCommandHandler implements Callable<Integer> {

        @Parameters(description = "Name of project")
        private String name;
        @Option(names = "--description", description = "Description of project", defaultValue = "")
        private String description;
        @Option(names = "--project-url", description = "Project-URL of project", defaultValue = "")
        private String projectUrl;
        @Option(names = "--issue-tracker-url", description = "Issue-Tracker-URL of project", defaultValue = "")
        private String issueTrackerUrl;

        /**
         * Computes a result, or throws an exception if unable to do so.
         *
         * @return computed result
         * @throws Exception if unable to compute a result
         */
        @Override
        public Integer call() throws Exception {
            Project project = Project.builder()
                    .name(name)
                    .description(description)
                    .projectUrl(projectUrl)
                    .issueTrackerUrl(issueTrackerUrl)
                    .build();

            try (ProjectClient client = CREATOR.newClientAuthenticated()) {
                ObjectHelper.print(getJsonOutput(), client.createNew(project));
                return 0;
            }
        }
    }

    @Command(name = "get", description = "Get a project by its id")
    public static class Get extends AbstractGetSpecificCommand<Project> {

        @Override
        public Project getSpecific(String id) throws ClientException {
            try (ProjectClient client = CREATOR.newClient()) {
                return client.getSpecific(id);
            }
        }
    }

    @Command(name = "list", description = "List projects")
    public static class List extends AbstractListCommand<Project> {

        @Override
        public Collection<Project> getAll(String sort, String query) throws RemoteResourceException {
            try (ProjectClient client = CREATOR.newClient()) {
                return client.getAll(Optional.ofNullable(sort), Optional.ofNullable(query)).getAll();
            }
        }
    }

    @Command(name = "list-build-configs", description = "List build configs for a project")
    public static class ListBuildConfigurations extends AbstractListCommand<BuildConfiguration> {

        @Parameters(description = "Project id")
        private String id;

        @Override
        public Collection<BuildConfiguration> getAll(String sort, String query) throws RemoteResourceException {
            try (ProjectClient client = CREATOR.newClient()) {
                return client.getBuildConfigurations(id, Optional.ofNullable(sort), Optional.ofNullable(query))
                        .getAll();
            }
        }
    }

    @Command(name = "list-builds", description = "List builds for a project")
    public static class ListBuilds extends AbstractBuildListCommand {

        @Parameters(description = "Project id")
        private String id;

        @Override
        public Collection<Build> getAll(BuildsFilterParameters buildsFilter, String sort, String query)
                throws RemoteResourceException {
            try (ProjectClient client = CREATOR.newClient()) {
                return client.getBuilds(id, buildsFilter, Optional.ofNullable(sort), Optional.ofNullable(query))
                        .getAll();
            }
        }
    }

    @Command(
            name = "update",
            description = "Update a project",
            footer = Constant.EXAMPLE_TEXT + "bacon pnc project update --name \"bad-guy\" 1")
    public static class Update implements Callable<Integer> {

        @Parameters(description = "Project id")
        private String id;

        @Option(names = "--name", description = "Name of project")
        private String name;
        @Option(names = "--description", description = "Description of project")
        private String description;
        @Option(names = "--project-url", description = "Project-URL of project")
        private String projectUrl;
        @Option(names = "--issue-tracker-url", description = "Issue-Tracker-URL of project")
        private String issueTrackerUrl;

        /**
         * Computes a result, or throws an exception if unable to do so.
         *
         * @return computed result
         * @throws Exception if unable to compute a result
         */
        @Override
        public Integer call() throws Exception {
            try (ProjectClient client = CREATOR.newClient()) {
                Project project = client.getSpecific(id);
                Project.Builder updated = project.toBuilder();

                if (isNotEmpty(name)) {
                    updated.name(name);
                }
                if (isNotEmpty(description)) {
                    updated.description(description);
                }
                if (isNotEmpty(projectUrl)) {
                    updated.projectUrl(projectUrl);
                }
                if (isNotEmpty(issueTrackerUrl)) {
                    updated.issueTrackerUrl(issueTrackerUrl);
                }

                try (ProjectClient clientAuthenticated = CREATOR.newClientAuthenticated()) {
                    clientAuthenticated.update(id, updated.build());
                    return 0;
                }
            }
        }
    }
}
