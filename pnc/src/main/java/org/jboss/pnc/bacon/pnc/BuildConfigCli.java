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

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandException;
import org.aesh.command.CommandResult;
import org.aesh.command.GroupCommandDefinition;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Argument;
import org.aesh.command.option.Option;
import org.aesh.command.option.OptionGroup;
import org.aesh.command.option.OptionList;
import org.jboss.pnc.bacon.common.ObjectHelper;
import org.jboss.pnc.bacon.common.cli.AbstractCommand;
import org.jboss.pnc.bacon.common.cli.AbstractGetSpecificCommand;
import org.jboss.pnc.bacon.common.cli.AbstractListCommand;
import org.jboss.pnc.bacon.pnc.common.AbstractBuildListCommand;
import org.jboss.pnc.bacon.pnc.common.ClientCreator;
import org.jboss.pnc.client.BuildConfigurationClient;
import org.jboss.pnc.client.ClientException;
import org.jboss.pnc.client.RemoteCollection;
import org.jboss.pnc.client.RemoteResourceException;
import org.jboss.pnc.dto.Build;
import org.jboss.pnc.dto.BuildConfiguration;
import org.jboss.pnc.dto.BuildConfigurationRef;
import org.jboss.pnc.dto.BuildConfigurationRevision;
import org.jboss.pnc.dto.Environment;
import org.jboss.pnc.dto.ProductVersionRef;
import org.jboss.pnc.dto.ProjectRef;
import org.jboss.pnc.dto.SCMRepository;
import org.jboss.pnc.dto.requests.BuildConfigWithSCMRequest;
import org.jboss.pnc.enums.BuildType;
import org.jboss.pnc.rest.api.parameters.BuildsFilterParameters;

import java.util.Map;
import java.util.Optional;

import static org.apache.commons.lang.StringUtils.isNotEmpty;

@GroupCommandDefinition(
        name = "build-config",
        description = "Build Config",
        groupCommands = {
                BuildConfigCli.Create.class,
                BuildConfigCli.CreateWithSCM.class,
                BuildConfigCli.Get.class,
                BuildConfigCli.GetRevision.class,
                BuildConfigCli.List.class,
                BuildConfigCli.ListRevision.class,
                BuildConfigCli.ListBuilds.class,
                BuildConfigCli.Update.class,
                BuildConfigCli.CreateRevision.class,
                BuildConfigCli.AddDependency.class,
                BuildConfigCli.RemoveDependency.class })
@Slf4j
public class BuildConfigCli extends AbstractCommand {

    private static final ClientCreator<BuildConfigurationClient> CREATOR = new ClientCreator<>(
            BuildConfigurationClient::new);

    @CommandDefinition(name = "create", description = "Create a build config")
    public class Create extends AbstractCommand {

        @Argument(required = true, description = "Name of build config")
        private String buildConfigName;

        @Option(name = "description", description = "Description of build config")
        private String description;
        @Option(required = true, name = "environment-id", description = "Environment ID of build config")
        private String environmentId;
        @Option(required = true, name = "project-id", description = "Project ID of build config")
        private String projectId;
        @Option(required = true, name = "build-script", description = "Build Script to build project")
        private String buildScript;
        @Option(required = true, name = "scm-repository-id", description = "SCM Repository ID to use")
        private String scmRepositoryId;
        @Option(required = true, name = "scm-revision", description = "SCM Revision")
        private String scmRevision;
        @OptionGroup(shortName = 'P', name = "parameter", description = "Parameter. Format: -PKEY=VALUE")
        private Map<String, String> parameters;
        @Option(name = "product-version-id", description = "Product Version ID")
        private String productVersionId;
        @Option(
                name = "build-type",
                description = "Build Type. Options are: MVN,GRADLE,NPM. Default: MVN",
                defaultValue = "MVN")
        private String buildType;
        @Option(
                shortName = 'o',
                hasValue = false,
                description = "use json for output (default to yaml)")
        private boolean jsonOutput = false;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation)
                throws CommandException, InterruptedException {

            return super.executeHelper(commandInvocation, () -> {

                BuildConfiguration.Builder buildConfigurationBuilder = BuildConfiguration.builder()
                        .name(buildConfigName)
                        .description(description)
                        .environment(Environment.builder().id(environmentId).build())
                        .project(ProjectRef.refBuilder().id(projectId).build())
                        .buildScript(buildScript)
                        .scmRepository(SCMRepository.builder().id(scmRepositoryId).build())
                        .scmRevision(scmRevision)
                        .buildType(BuildType.valueOf(buildType))
                        .parameters(parameters);

                if (isNotEmpty(productVersionId)) {
                    buildConfigurationBuilder
                            .productVersion(ProductVersionRef.refBuilder().id(productVersionId).build());
                }
                try (BuildConfigurationClient client = CREATOR.newClientAuthenticated()) {
                    ObjectHelper.print(jsonOutput, client.createNew(buildConfigurationBuilder.build()));
                    return 0;
                }
            });
        }

        @Override
        public String exampleText() {
            StringBuilder builder = new StringBuilder();
            builder.append("$ bacon pnc build-config create \\ \n")
                    .append("\t--environment-id=100 --project-id=164 --build-script \"mvn clean deploy\" \\ \n")
                    .append("\t--scm-repository-id 176 --scm-revision master \\ \n")
                    .append("\t-PTEST=TRUE -PALIGNMENT_PARAMETERS=\"-Dignore=true\" \\ \n")
                    .append("\t--build-type MVN buildconfigname");
            return builder.toString();
        }
    }

    @CommandDefinition(name = "create-with-scm", description = "Create BC with SCM")
    public class CreateWithSCM extends AbstractCommand {

        @Argument(required = true, description = "Name of build config")
        private String buildConfigName;

        @Option(name = "description", description = "Description of build config")
        private String description;
        @Option(required = true, name = "environment-id", description = "Environment ID of build config")
        private String environmentId;
        @Option(required = true, name = "project-id", description = "Project ID of build config")
        private String projectId;
        @Option(required = true, name = "build-script", description = "Build Script to build project")
        private String buildScript;
        @OptionGroup(shortName = 'P', name = "parameter", description = "Parameter. Format: -PKEY=VALUE")
        private Map<String, String> parameters;
        @Option(name = "product-version-id", description = "Product Version ID")
        private String productVersionId;
        @Option(
                name = "build-type",
                description = "Build Type. Options are: MVN,GRADLE,NPM. Default: MVN",
                defaultValue = "MVN")
        private String buildType;

        @Option(required = true, name = "scm-url", description = "SCM URL")
        private String scmUrl;
        @Option(required = true, name = "scm-revision", description = "SCM Revision")
        private String scmRevision;

        @Option(
                name = "no-pre-build-sync",
                description = "Disable the pre-build sync of external repo.",
                hasValue = false)
        private boolean noPreBuildSync = false;

        @Option(
                shortName = 'o',
                hasValue = false,
                description = "use json for output (default to yaml)")
        private boolean jsonOutput = false;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation)
                throws CommandException, InterruptedException {

            return super.executeHelper(commandInvocation, () -> {
                BuildConfiguration buildConfiguration = BuildConfiguration.builder()
                        .name(buildConfigName)
                        .description(description)
                        .environment(Environment.builder().id(environmentId).build())
                        .project((ProjectRef.refBuilder().id(projectId).build()))
                        .scmRevision(scmRevision)
                        .buildScript(buildScript)
                        .buildType(BuildType.valueOf(buildType))
                        .parameters(parameters)
                        .build();

                BuildConfigWithSCMRequest request = BuildConfigWithSCMRequest.builder()
                        .scmUrl(scmUrl)
                        .buildConfig(buildConfiguration)
                        .preBuildSyncEnabled(!noPreBuildSync)
                        .build();

                try (BuildConfigurationClient client = CREATOR.newClientAuthenticated()) {
                    ObjectHelper.print(jsonOutput, client.createWithSCM(request));
                    return 0;
                }
            });
        }

        @Override
        public String exampleText() {
            StringBuilder builder = new StringBuilder();
            builder.append("$ bacon pnc build-config create-with-scm \\ \n")
                    .append("\t--environment-id=100 --project-id=164 --build-script \"mvn clean deploy\" \\ \n")
                    .append("\t-PTEST=TRUE -PALIGNMENT_PARAMETERS=\"-Dignore=true\" \\ \n")
                    .append("\t--build-type MVN buildconfigname \\ \n")
                    .append("\t--scm-url=http://github.com/project-ncl/pnc.git \\ \n")
                    .append("\t--scm-revision=master")
                    .append("\t--no-prebuild-sync");
            return builder.toString();
        }
    }

    @CommandDefinition(name = "update", description = "Update a build config")
    public class Update extends AbstractCommand {

        @Argument(required = true, description = "Build config ID")
        protected String buildConfigId;

        @Option(description = "Build config name")
        private String buildConfigName;

        @Option(name = "description", description = "Description of build config")
        private String description;
        @Option(name = "environment-id", description = "Environment ID of build config")
        private String environmentId;
        @Option(name = "build-script", description = "Build Script to build project")
        private String buildScript;
        @Option(name = "scm-repository-id", description = "SCM Repository ID to use")
        private String scmRepositoryId;
        @Option(name = "scm-revision", description = "SCM Revision")
        private String scmRevision;
        @OptionGroup(shortName = 'P', name = "parameter", description = "Parameter. Format: -PKEY=VALUE -PKEY1=VALUE1")
        private Map<String, String> parameters;
        @OptionList(
                name = "remove-parameters",
                description = "Parameters to remove. Format: --remove-parameters=key1,key2,key3")
        private java.util.List<String> parametersToRemove;
        @Option(name = "build-type", description = "Build Type. Options are: MVN,GRADLE,NPM. Default: MVN")
        private String buildType;
        @Option(name = "product-version-id", description = "Product Version ID")
        private String productVersionId;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation)
                throws CommandException, InterruptedException {

            return super.executeHelper(commandInvocation, () -> {

                try (BuildConfigurationClient client = CREATOR.newClient()) {
                    BuildConfiguration buildConfiguration = client.getSpecific(buildConfigId);
                    BuildConfiguration.Builder updated = buildConfiguration.toBuilder();
                    if (isNotEmpty(buildConfigName)) {
                        updated.name(buildConfigName);
                    }
                    if (isNotEmpty(description)) {
                        updated.description(description);
                    }
                    if (isNotEmpty(environmentId)) {
                        updated.environment(Environment.builder().id(environmentId).build());
                    }
                    if (isNotEmpty(buildScript)) {
                        updated.buildScript(buildScript);
                    }
                    if (isNotEmpty(scmRepositoryId)) {
                        updated.scmRepository(SCMRepository.builder().id(scmRepositoryId).build());
                    }
                    if (isNotEmpty(scmRevision)) {
                        updated.scmRevision(scmRevision);
                    }
                    if (isNotEmpty(buildType)) {
                        updated.buildType(BuildType.valueOf(buildType));
                    }
                    if (isNotEmpty(productVersionId)) {
                        updated.productVersion(ProductVersionRef.refBuilder().id(productVersionId).build());
                    }
                    if (parameters != null) {
                        // update the content of the existing parameters
                        Map<String, String> existing = buildConfiguration.getParameters();
                        parameters.forEach(existing::put);
                        updated.parameters(existing);
                    }
                    if (parametersToRemove != null && parametersToRemove.size() > 0) {
                        Map<String, String> existing = buildConfiguration.getParameters();
                        parametersToRemove.forEach(existing::remove);
                        updated.parameters(existing);
                    }
                    callUpdate(updated.build());
                    return 0;
                }
            });
        }

        protected void callUpdate(BuildConfiguration updated) throws JsonProcessingException, RemoteResourceException {
            try (BuildConfigurationClient client = CREATOR.newClientAuthenticated()) {
                client.update(buildConfigId, updated);
            }
        }

        @Override
        public String exampleText() {
            return "$ bacon pnc build-config update --description \"new me new description\" 50";
        }
    }

    @CommandDefinition(name = "create-revision", description = "Create a new revision for a build configuration")
    public class CreateRevision extends Update {

        @Option(
                shortName = 'o',
                hasValue = false,
                description = "use json for output (default to yaml)")
        private boolean jsonOutput = false;

        @Override
        protected void callUpdate(BuildConfiguration updated) throws JsonProcessingException, RemoteResourceException {
            try (BuildConfigurationClient client = CREATOR.newClientAuthenticated()) {
                ObjectHelper.print(jsonOutput, client.createRevision(buildConfigId, updated));
            }
        }

        @Override
        public String exampleText() {
            return "$ bacon pnc build-config create-revision --description \"new me new description\" 50";
        }
    }

    @CommandDefinition(name = "get", description = "Get a build config by its id")
    public class Get extends AbstractGetSpecificCommand<BuildConfiguration> {

        @Override
        public BuildConfiguration getSpecific(String id) throws ClientException {
            try (BuildConfigurationClient client = CREATOR.newClient()) {
                return client.getSpecific(id);
            }
        }
    }

    @CommandDefinition(name = "get-revision", description = "Get build config revision")
    public class GetRevision extends AbstractGetSpecificCommand<BuildConfigurationRevision> {

        @Option(required = true, description = "Revision Id of build configuration")
        private int revisionId;

        @Override
        public BuildConfigurationRevision getSpecific(String id) throws ClientException {
            try (BuildConfigurationClient client = CREATOR.newClient()) {
                return client.getRevision(id, revisionId);
            }
        }
    }

    @CommandDefinition(name = "list", description = "List build configs")
    public class List extends AbstractListCommand<BuildConfiguration> {

        @Override
        public RemoteCollection<BuildConfiguration> getAll(String sort, String query) throws RemoteResourceException {
            try (BuildConfigurationClient client = CREATOR.newClient()) {
                return client.getAll(Optional.ofNullable(sort), Optional.ofNullable(query));
            }
        }
    }

    @CommandDefinition(name = "list-revisions", description = "List revisions of build config")
    public class ListRevision extends AbstractListCommand<BuildConfigurationRevision> {

        @Argument(required = true, description = "Build configuration id")
        private String id;

        @Override
        public RemoteCollection<BuildConfigurationRevision> getAll(String sort, String query)
                throws RemoteResourceException {
            try (BuildConfigurationClient client = CREATOR.newClient()) {
                return client.getRevisions(id, Optional.ofNullable(sort), Optional.ofNullable(query));
            }
        }
    }

    @CommandDefinition(name = "list-builds", description = "List builds of build configs")
    public class ListBuilds extends AbstractBuildListCommand {

        @Argument(required = true, description = "Build config id")
        private String buildConfigId;

        @Override
        public RemoteCollection<Build> getAll(BuildsFilterParameters buildsFilter, String sort, String query)
                throws RemoteResourceException {
            try (BuildConfigurationClient client = CREATOR.newClient()) {
                return client
                        .getBuilds(buildConfigId, buildsFilter, Optional.ofNullable(sort), Optional.ofNullable(query));
            }
        }
    }

    @CommandDefinition(name = "add-dependency", description = "Adds a dependency to a BuildConfig")
    public class AddDependency extends AbstractCommand {

        @Argument(required = true, description = "Build config id")
        private String buildConfigId;

        @Option(name = "dependency-id", required = true, description = "ID of BuildConfig to add as dependency")
        private String dependencyConfigId;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation)
                throws CommandException, InterruptedException {
            return super.executeHelper(commandInvocation, () -> {
                try (BuildConfigurationClient client = CREATOR.newClientAuthenticated()) {
                    client.addDependency(
                            buildConfigId,
                            BuildConfigurationRef.refBuilder().id(dependencyConfigId).build());
                    return 0;
                }
            });
        }
    }

    @CommandDefinition(name = "remove-dependency", description = "Removes a dependency from a BuildConfig")
    public class RemoveDependency extends AbstractCommand {

        @Argument(required = true, description = "Build config id")
        private String buildConfigId;

        @Option(name = "dependency-id", required = true, description = "ID of BuildConfig to remove as dependency")
        private String dependencyConfigId;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation)
                throws CommandException, InterruptedException {
            return super.executeHelper(commandInvocation, () -> {
                try (BuildConfigurationClient client = CREATOR.newClientAuthenticated()) {
                    client.removeDependency(buildConfigId, dependencyConfigId);
                    return 0;
                }
            });
        }
    }

}
