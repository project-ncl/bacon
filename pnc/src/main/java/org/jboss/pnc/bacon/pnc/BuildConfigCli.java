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
import org.jboss.pnc.bacon.common.Constant;
import org.jboss.pnc.bacon.common.ObjectHelper;
import org.jboss.pnc.bacon.common.cli.AbstractBuildListCommand;
import org.jboss.pnc.bacon.common.cli.AbstractGetSpecificCommand;
import org.jboss.pnc.bacon.common.cli.AbstractListCommand;
import org.jboss.pnc.bacon.common.cli.JSONCommandHandler;
import org.jboss.pnc.bacon.pnc.common.ClientCreator;
import org.jboss.pnc.client.BuildConfigurationClient;
import org.jboss.pnc.client.ClientException;
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
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;

import static org.apache.commons.lang.StringUtils.isNotEmpty;

@Command(
        name = "build-config",
        description = "Build Config",
        subcommands = {
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
public class BuildConfigCli {

    private static final ClientCreator<BuildConfigurationClient> CREATOR = new ClientCreator<>(
            BuildConfigurationClient::new);

    @Command(
            name = "create",
            description = "Create a build config",
            footer = Constant.EXAMPLE_TEXT + "$ bacon pnc build-config create \\%n"
                    + "\t--environment-id=100 --project-id=164 --build-script \"mvn clean deploy\" \\%n"
                    + "\t--scm-repository-id 176 --scm-revision master \\%n"
                    + "\t-PTEST=TRUE -PALIGNMENT_PARAMETERS=\"-Dignore=true\" \\%n"
                    + "\t--build-type MVN buildconfigname")
    public static class Create extends JSONCommandHandler implements Callable<Integer> {

        @Parameters(description = "Name of build config")
        private String buildConfigName;

        @Option(names = "--description", description = "Description of build config")
        private String description;
        @Option(required = true, names = "--environment-id", description = "Environment ID of build config")
        private String environmentId;
        @Option(required = true, names = "--project-id", description = "Project ID of build config")
        private String projectId;
        @Option(required = true, names = "--build-script", description = "Build Script to build project")
        private String buildScript;
        @Option(required = true, names = "--scm-repository-id", description = "SCM Repository ID to use")
        private String scmRepositoryId;
        @Option(required = true, names = "--scm-revision", description = "SCM Revision")
        private String scmRevision;
        @Option(names = { "-P", "--parameter" }, description = "Parameter. Format: -PKEY=VALUE")
        private Map<String, String> parameters;
        @Option(names = "--product-version-id", description = "Product Version ID")
        private String productVersionId;
        @Option(
                names = "--build-type",
                description = "Build Type. Options are: MVN,GRADLE,NPM. Default: MVN",
                defaultValue = "MVN")
        private String buildType;
        @Option(
                names = "--brew-pull-active",
                description = "Enable to look for dependencies also in brew not just in pnc. (Slows down build)")
        private boolean brewPullActive = false;

        /**
         * Computes a result, or throws an exception if unable to do so.
         *
         * @return computed result
         * @throws Exception if unable to compute a result
         */
        @Override
        public Integer call() throws Exception {
            BuildConfiguration.Builder buildConfigurationBuilder = BuildConfiguration.builder()
                    .name(buildConfigName)
                    .description(description)
                    .environment(Environment.builder().id(environmentId).build())
                    .project((ProjectRef.refBuilder().id(projectId).build()))
                    .buildScript(buildScript)
                    .scmRepository(SCMRepository.builder().id(scmRepositoryId).build())
                    .scmRevision(scmRevision)
                    .buildType(BuildType.valueOf(buildType))
                    .brewPullActive(brewPullActive)
                    .parameters(parameters);

            if (isNotEmpty(productVersionId)) {
                buildConfigurationBuilder.productVersion(ProductVersionRef.refBuilder().id(productVersionId).build());
            }
            try (BuildConfigurationClient client = CREATOR.newClientAuthenticated()) {
                ObjectHelper.print(getJsonOutput(), client.createNew(buildConfigurationBuilder.build()));
                return 0;
            }
        }
    }

    @Command(
            name = "create-with-scm",
            description = "Create BC with SCM",
            footer = Constant.EXAMPLE_TEXT + "$ bacon pnc build-config create-with-scm \\%n"
                    + "\t--environment-id=100 --project-id=164 --build-script \"mvn clean deploy\" \\%n"
                    + "\t-PTEST=TRUE -PALIGNMENT_PARAMETERS=\"-Dignore=true\" \\%n"
                    + "\t--build-type MVN buildconfigname \\%n"
                    + "\t--scm-url=http://github.com/project-ncl/pnc.git \\%n\t--scm-revision=master"
                    + "\t--no-prebuild-sync")
    public static class CreateWithSCM extends JSONCommandHandler implements Callable<Integer> {

        @Parameters(description = "Name of build config")
        private String buildConfigName;

        @Option(names = "--description", description = "Description of build config")
        private String description;
        @Option(required = true, names = "--environment-id", description = "Environment ID of build config")
        private String environmentId;
        @Option(required = true, names = "--project-id", description = "Project ID of build config")
        private String projectId;
        @Option(required = true, names = "--build-script", description = "Build Script to build project")
        private String buildScript;
        @Option(names = { "-P", "--parameter" }, description = "Parameter. Format: -PKEY=VALUE")
        private Map<String, String> parameters;
        @Option(names = "--product-version-id", description = "Product Version ID")
        private String productVersionId;
        @Option(
                names = "--build-type",
                description = "Build Type. Options are: MVN,GRADLE,NPM. Default: MVN",
                defaultValue = "MVN")
        private String buildType;

        @Option(required = true, names = "--scm-url", description = "SCM URL")
        private String scmUrl;
        @Option(required = true, names = "--scm-revision", description = "SCM Revision")
        private String scmRevision;
        @Option(names = "--no-pre-build-sync", description = "Disable the pre-build sync of external repo.")
        private boolean noPreBuildSync = false;
        @Option(
                names = "--brew-pull-active",
                description = "Enable to look for dependencies also in brew not just in pnc. (Slows down build)")
        private boolean brewPullActive = false;

        /**
         * Computes a result, or throws an exception if unable to do so.
         *
         * @return computed result
         * @throws Exception if unable to compute a result
         */
        @Override
        public Integer call() throws Exception {
            BuildConfiguration buildConfiguration = BuildConfiguration.builder()
                    .name(buildConfigName)
                    .description(description)
                    .environment(Environment.builder().id(environmentId).build())
                    .project((ProjectRef.refBuilder().id(projectId).build()))
                    .scmRevision(scmRevision)
                    .buildScript(buildScript)
                    .buildType(BuildType.valueOf(buildType))
                    .brewPullActive(brewPullActive)
                    .parameters(parameters)
                    .build();

            BuildConfigWithSCMRequest request = BuildConfigWithSCMRequest.builder()
                    .scmUrl(scmUrl)
                    .buildConfig(buildConfiguration)
                    .preBuildSyncEnabled(!noPreBuildSync)
                    .build();

            try (BuildConfigurationClient client = CREATOR.newClientAuthenticated()) {
                ObjectHelper.print(getJsonOutput(), client.createWithSCM(request));
                return 0;
            }
        }
    }

    @Command(
            name = "update",
            description = "Update a build config",
            footer = Constant.EXAMPLE_TEXT
                    + "$ bacon pnc build-config update --description \"new me new description\" 50")
    public static class Update extends JSONCommandHandler implements Callable<Integer> {

        @Parameters(description = "Build config ID")
        protected String buildConfigId;

        @Option(names = "--buildConfigName", description = "Build config name")
        private String buildConfigName;

        @Option(names = "--description", description = "Description of build config")
        private String description;
        @Option(names = "--environment-id", description = "Environment ID of build config")
        private String environmentId;
        @Option(names = "--build-script", description = "Build Script to build project")
        private String buildScript;
        @Option(names = "--scm-repository-id", description = "SCM Repository ID to use")
        private String scmRepositoryId;
        @Option(names = "--scm-revision", description = "SCM Revision")
        private String scmRevision;
        @Option(names = { "-P", "--parameter" }, description = "Parameter. Format: -PKEY=VALUE -PKEY1=VALUE1")
        private Map<String, String> parameters;
        @Option(
                names = "--remove-parameters",
                description = "Parameters to remove. Format: --remove-parameters=key1,key2,key3")
        private java.util.List<String> parametersToRemove;
        @Option(names = "--build-type", description = "Build Type. Options are: MVN,GRADLE,NPM. Default: MVN")
        private String buildType;
        @Option(names = "--product-version-id", description = "Product Version ID")
        private String productVersionId;
        @Option(
                names = "--brew-pull-active",
                description = "Enable to look for dependencies also in brew not just in pnc. (Slows down build)")
        private Boolean brewPullActive;

        /**
         * Computes a result, or throws an exception if unable to do so.
         *
         * @return computed result
         * @throws Exception if unable to compute a result
         */
        @Override
        public Integer call() throws Exception {
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
                if (brewPullActive != null) {
                    updated.brewPullActive(brewPullActive);
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
        }

        protected void callUpdate(BuildConfiguration updated) throws JsonProcessingException, RemoteResourceException {
            try (BuildConfigurationClient client = CREATOR.newClientAuthenticated()) {
                client.update(buildConfigId, updated);
            }
        }
    }

    @Command(
            name = "create-revision",
            description = "Create a new revision for a build configuration",
            footer = Constant.EXAMPLE_TEXT
                    + "$ bacon pnc build-config create-revision --description \"new me new description\" 50")
    public static class CreateRevision extends Update implements Callable<Integer> {

        @Override
        protected void callUpdate(BuildConfiguration updated) throws JsonProcessingException, RemoteResourceException {
            try (BuildConfigurationClient client = CREATOR.newClientAuthenticated()) {
                ObjectHelper.print(getJsonOutput(), client.createRevision(buildConfigId, updated));
            }
        }
    }

    @Command(name = "get", description = "Get a build config by its id")
    public static class Get extends AbstractGetSpecificCommand<BuildConfiguration> {

        @Override
        public BuildConfiguration getSpecific(String id) throws ClientException {
            try (BuildConfigurationClient client = CREATOR.newClient()) {
                return client.getSpecific(id);
            }
        }
    }

    @Command(name = "get-revision", description = "Get build config revision")
    public static class GetRevision extends AbstractGetSpecificCommand<BuildConfigurationRevision> {

        @Option(names = "--revisionId", description = "Revision Id of build configuration")
        private int revisionId;

        @Override
        public BuildConfigurationRevision getSpecific(String id) throws ClientException {
            try (BuildConfigurationClient client = CREATOR.newClient()) {
                return client.getRevision(id, revisionId);
            }
        }
    }

    @Command(name = "list", description = "List build configs")
    public static class List extends AbstractListCommand<BuildConfiguration> {

        @Override
        public Collection<BuildConfiguration> getAll(String sort, String query) throws RemoteResourceException {
            try (BuildConfigurationClient client = CREATOR.newClient()) {
                return client.getAll(Optional.ofNullable(sort), Optional.ofNullable(query)).getAll();
            }
        }
    }

    @Command(name = "list-revisions", description = "List revisions of build config")
    public static class ListRevision extends AbstractListCommand<BuildConfigurationRevision> {

        @Parameters(description = "Build configuration id")
        private String id;

        @Override
        public Collection<BuildConfigurationRevision> getAll(String sort, String query) throws RemoteResourceException {
            try (BuildConfigurationClient client = CREATOR.newClient()) {
                return client.getRevisions(id, Optional.ofNullable(sort), Optional.ofNullable(query)).getAll();
            }
        }
    }

    @Command(name = "list-builds", description = "List builds of build configs")
    public static class ListBuilds extends AbstractBuildListCommand {

        @Parameters(description = "Build config id")
        private String buildConfigId;

        @Override
        public Collection<Build> getAll(BuildsFilterParameters buildsFilter, String sort, String query)
                throws RemoteResourceException {
            try (BuildConfigurationClient client = CREATOR.newClient()) {
                return client
                        .getBuilds(buildConfigId, buildsFilter, Optional.ofNullable(sort), Optional.ofNullable(query))
                        .getAll();
            }
        }
    }

    @Command(name = "add-dependency", description = "Adds a dependency to a BuildConfig")
    public static class AddDependency implements Callable<Integer> {

        @Parameters(description = "Build config id")
        private String buildConfigId;

        @Option(names = "--dependency-id", required = true, description = "ID of BuildConfig to add as dependency")
        private String dependencyConfigId;

        /**
         * Computes a result, or throws an exception if unable to do so.
         *
         * @return computed result
         * @throws Exception if unable to compute a result
         */
        @Override
        public Integer call() throws Exception {
            try (BuildConfigurationClient client = CREATOR.newClientAuthenticated()) {
                client.addDependency(buildConfigId, BuildConfigurationRef.refBuilder().id(dependencyConfigId).build());
                return 0;
            }
        }
    }

    @Command(name = "remove-dependency", description = "Removes a dependency from a BuildConfig")
    public static class RemoveDependency implements Callable<Integer> {

        @Parameters(description = "Build config id")
        private String buildConfigId;

        @Option(names = "--dependency-id", required = true, description = "ID of BuildConfig to remove as dependency")
        private String dependencyConfigId;

        /**
         * Computes a result, or throws an exception if unable to do so.
         *
         * @return computed result
         * @throws Exception if unable to compute a result
         */
        @Override
        public Integer call() throws Exception {
            try (BuildConfigurationClient client = CREATOR.newClientAuthenticated()) {
                client.removeDependency(buildConfigId, dependencyConfigId);
                return 0;
            }
        }
    }
}
