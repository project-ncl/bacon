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

import lombok.extern.slf4j.Slf4j;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandException;
import org.aesh.command.CommandResult;
import org.aesh.command.GroupCommandDefinition;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Argument;
import org.aesh.command.option.Option;
import org.jboss.pnc.bacon.common.Fail;
import org.jboss.pnc.bacon.common.ObjectHelper;
import org.jboss.pnc.bacon.common.cli.AbstractCommand;
import org.jboss.pnc.bacon.common.cli.AbstractGetSpecificCommand;
import org.jboss.pnc.bacon.common.cli.AbstractListCommand;
import org.jboss.pnc.bacon.pnc.common.ClientCreator;
import org.jboss.pnc.client.BuildConfigurationClient;
import org.jboss.pnc.client.ClientException;
import org.jboss.pnc.client.RemoteCollection;
import org.jboss.pnc.client.RemoteResourceException;
import org.jboss.pnc.dto.Build;
import org.jboss.pnc.dto.BuildConfiguration;
import org.jboss.pnc.dto.Environment;
import org.jboss.pnc.dto.ProductVersionRef;
import org.jboss.pnc.dto.ProjectRef;
import org.jboss.pnc.dto.SCMRepository;
import org.jboss.pnc.enums.BuildType;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@GroupCommandDefinition(
        name = "build-config",
        description = "build-config",
        groupCommands = { BuildConfigCli.Create.class, BuildConfigCli.Get.class, BuildConfigCli.List.class,
                BuildConfigCli.ListBuilds.class, BuildConfigCli.Update.class, })
@Slf4j
public class BuildConfigCli extends AbstractCommand {

    private static final ClientCreator<BuildConfigurationClient> CREATOR = new ClientCreator<>(
            BuildConfigurationClient::new);

    @CommandDefinition(name = "create", description = "Create a build configuration")
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
        @Option(name = "generic-parameters", description = "Generic parameters. Format: KEY=VALUE,KEY=VALUE")
        private String genericParameters;
        @Option(name = "product-version-id", description = "Product Version ID")
        private String productVersionId;
        @Option(
                name = "build-type",
                description = "Build Type. Options are: MVN,GRADLE. Default: MVN",
                defaultValue = "MVN")
        private String buildType;
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

                BuildConfiguration.Builder buildConfigurationBuilder = BuildConfiguration.builder()
                        .name(buildConfigName)
                        .description(description)
                        .environment(Environment.builder().id(environmentId).build())
                        .project((ProjectRef.refBuilder().id(projectId).build()))
                        .buildScript(buildScript)
                        .scmRepository(SCMRepository.builder().id(scmRepositoryId).build())
                        .scmRevision(scmRevision)
                        .buildType(BuildType.valueOf(buildType))
                        .parameters(processGenericParameters(genericParameters));

                ObjectHelper.executeIfNotNull(
                        productVersionId,
                        () -> buildConfigurationBuilder
                                .productVersion(ProductVersionRef.refBuilder().id(productVersionId).build()));

                ObjectHelper.print(
                        jsonOutput,
                        CREATOR.getClientAuthenticated().createNew(buildConfigurationBuilder.build()));
            });
        }

        @Override
        public String exampleText() {
            StringBuilder builder = new StringBuilder();
            builder.append("$ bacon pnc build-config create \\ \n")
                    .append("\t--environment-id=100 --project-id=164 --build-script \"mvn clean deploy\" \\ \n")
                    .append("\t--scm-repository-id 176 --scm-revision master \\ \n")
                    .append("\t--generic-parameters \"TEST=TRUE,CUSTOM_PME_PARAMETERS=-Dignore=true\" \\ \n")
                    .append("\t--build-type MVN buildconfigname");
            return builder.toString();
        }
    }

    @CommandDefinition(name = "update", description = "Update a build configuration")
    public class Update extends AbstractCommand {

        @Argument(required = true, description = "Build config ID")
        private String buildConfigId;

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
        @Option(name = "generic-parameters", description = "Generic parameters. Format: KEY=VALUE,KEY=VALUE")
        private String genericParameters;
        @Option(name = "build-type", description = "Build Type. Options are: Maven,Gradle. Default: Maven")
        private String buildType;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation)
                throws CommandException, InterruptedException {

            return super.executeHelper(commandInvocation, () -> {

                BuildConfiguration buildConfiguration = CREATOR.getClient().getSpecific(buildConfigId);
                BuildConfiguration.Builder updated = buildConfiguration.toBuilder();

                ObjectHelper.executeIfNotNull(buildConfigName, () -> updated.name(buildConfigName));
                ObjectHelper.executeIfNotNull(description, () -> updated.description(description));
                ObjectHelper.executeIfNotNull(
                        environmentId,
                        () -> updated.environment(Environment.builder().id(environmentId).build()));
                ObjectHelper.executeIfNotNull(buildScript, () -> updated.buildScript(buildScript));
                ObjectHelper.executeIfNotNull(
                        scmRepositoryId,
                        () -> updated.scmRepository(SCMRepository.builder().id(scmRepositoryId).build()));
                ObjectHelper.executeIfNotNull(scmRevision, () -> updated.scmRevision(scmRevision));
                ObjectHelper.executeIfNotNull(
                        genericParameters,
                        () -> updated.parameters(processGenericParameters(genericParameters)));
                ObjectHelper.executeIfNotNull(buildType, () -> updated.buildType(BuildType.valueOf(buildType)));

                CREATOR.getClientAuthenticated().update(buildConfigId, updated.build());
            });
        }

        @Override
        public String exampleText() {
            return "$ bacon pnc build-config update --description \"new me new description\" 50";
        }
    }

    @CommandDefinition(name = "get", description = "Get build configuration")
    public class Get extends AbstractGetSpecificCommand<BuildConfiguration> {

        @Override
        public BuildConfiguration getSpecific(String id) throws ClientException {
            return CREATOR.getClient().getSpecific(id);
        }
    }

    @CommandDefinition(name = "list", description = "List build configurations")
    public class List extends AbstractListCommand<BuildConfiguration> {

        @Override
        public RemoteCollection<BuildConfiguration> getAll(String sort, String query) throws RemoteResourceException {
            return CREATOR.getClient().getAll(Optional.ofNullable(sort), Optional.ofNullable(query));
        }
    }

    @CommandDefinition(name = "list-builds", description = "List builds of build configurations")
    public class ListBuilds extends AbstractListCommand<Build> {

        @Argument(required = true, description = "Build config id")
        private String buildConfigId;

        @Override
        public RemoteCollection<Build> getAll(String sort, String query) throws RemoteResourceException {
            return CREATOR.getClient()
                    .getBuilds(buildConfigId, null, Optional.ofNullable(sort), Optional.ofNullable(query));
        }
    }

    private static Map<String, String> processGenericParameters(String genericParameters) {

        if (genericParameters == null) {
            return null;
        } else {

            Map<String, String> params = new LinkedHashMap<>();

            for (String keyValue : genericParameters.split(",")) {

                keyValue = keyValue.trim();

                if (keyValue.contains("=")) {
                    String[] keyValueResult = keyValue.split("=", 2);
                    if (keyValueResult.length == 2) {
                        log.debug(
                                "Adding parameter with key: '{}' and value: '{}'",
                                keyValueResult[0],
                                keyValueResult[1]);
                        params.put(keyValueResult[0], keyValueResult[1]);
                    } else {
                        log.error("Generic parameter format is in the form: KEY1=VALUE1,KEY2=VALUE2");
                        Fail.fail("Invalid format in the generic parameters: " + genericParameters);
                    }
                }
            }
            return params;
        }
    }
}
