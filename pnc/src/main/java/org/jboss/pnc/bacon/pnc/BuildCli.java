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
import org.jboss.pnc.api.enums.AlignmentPreference;
import org.jboss.pnc.bacon.common.Constant;
import org.jboss.pnc.bacon.common.ObjectHelper;
import org.jboss.pnc.bacon.common.cli.AbstractBuildListCommand;
import org.jboss.pnc.bacon.common.cli.AbstractGetSpecificCommand;
import org.jboss.pnc.bacon.common.cli.AbstractListCommand;
import org.jboss.pnc.bacon.common.cli.JSONCommandHandler;
import org.jboss.pnc.bacon.config.Config;
import org.jboss.pnc.bacon.pnc.client.BifrostClient;
import org.jboss.pnc.bacon.pnc.client.PncClientHelper;
import org.jboss.pnc.bacon.pnc.common.ClientCreator;
import org.jboss.pnc.bacon.pnc.common.ParameterChecker;
import org.jboss.pnc.client.BuildClient;
import org.jboss.pnc.client.ClientException;
import org.jboss.pnc.client.RemoteResourceException;
import org.jboss.pnc.dto.Artifact;
import org.jboss.pnc.dto.Build;
import org.jboss.pnc.dto.BuildConfigurationRevision;
import org.jboss.pnc.enums.RebuildMode;
import org.jboss.pnc.rest.api.parameters.BuildParameters;
import org.jboss.pnc.rest.api.parameters.BuildsFilterParameters;
import org.jboss.pnc.restclient.AdvancedBuildConfigurationClient;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import javax.ws.rs.core.Response;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

@Command(
        name = "build",
        description = "build",
        subcommands = {
                BuildCli.Start.class,
                BuildCli.Cancel.class,
                BuildCli.List.class,
                BuildCli.ListBuildArtifacts.class,
                BuildCli.ListDependencies.class,
                BuildCli.Get.class,
                BuildCli.GetRevision.class,
                BuildCli.GetLog.class,
                BuildCli.GetAlignLog.class,
                BuildCli.DownloadSources.class })
@Slf4j
public class BuildCli {

    private static final ClientCreator<BuildClient> CREATOR = new ClientCreator<>(BuildClient::new);
    private static final ClientCreator<AdvancedBuildConfigurationClient> BC_CREATOR = new ClientCreator<>(
            AdvancedBuildConfigurationClient::new);

    @Command(
            name = "start",
            description = "Start a new build",
            footer = Constant.EXAMPLE_TEXT + "$ bacon pnc build start \\%n"
                    + "\t--rebuild-mode=FORCE --temporary-build --wait 27")
    public static class Start extends JSONCommandHandler implements Callable<Integer> {

        @Parameters(description = "Build Config ID")
        private String buildConfigId;

        @Option(
                names = "--rebuild-mode",
                description = "Default: IMPLICIT_DEPENDENCY_CHECK. Other options are: EXPLICIT_DEPENDENCY_CHECK, FORCE")
        private String rebuildMode;
        @Option(names = "--keep-pod-on-failure", description = "Keep the builder pod online after the build fails.")
        private boolean keepPodOnFailure = false;
        @Option(
                names = "--timestamp-alignment",
                description = "NOT SUPPORTED - Do timestamp alignment with temporary build.")
        private boolean timestampAlignment = false;
        @Option(names = "--temporary-build", description = "Perform a temporary build.")
        private boolean temporaryBuild = false;
        @Option(names = "--wait", description = "Wait for build to complete")
        private boolean wait = false;
        @Option(names = "--no-build-dependencies", description = "Skip building of dependencies")
        private boolean noBuildDependencies = false;

        @Option(names = "--timeout", description = "Time in minutes the command waits for Build completion")
        private Integer timeout;
        @Option(names = "--revision", description = "Build Config revision to build.")
        private Integer revision;
        @Option(
                names = "--dry-run",
                description = "Perform a dry run. Temporary build with, alignment preference of persistent artifacts.")
        private boolean dryRun = false;

        /**
         * Computes a result, or throws an exception if unable to do so.
         *
         * @return computed result
         * @throws Exception if unable to compute a result
         */
        @Override
        public Integer call() throws Exception {
            BuildParameters buildParams = new BuildParameters();
            if (rebuildMode == null) {
                rebuildMode = RebuildMode.IMPLICIT_DEPENDENCY_CHECK.name();
            }
            if (timestampAlignment) {
                log.warn("Temporary builds with timestamp alignment are not supported");
            }
            ParameterChecker.checkRebuildModeOption(rebuildMode);
            buildParams.setRebuildMode(RebuildMode.valueOf(rebuildMode));
            buildParams.setKeepPodOnFailure(keepPodOnFailure);
            buildParams.setTemporaryBuild(temporaryBuild);
            buildParams.setBuildDependencies(!noBuildDependencies);

            if (dryRun) {
                buildParams.setTemporaryBuild(true);
                buildParams.setAlignmentPreference(AlignmentPreference.PREFER_PERSISTENT);
            }

            try (AdvancedBuildConfigurationClient client = BC_CREATOR.newClientAuthenticated()) {

                final Build build;
                if (revision == null) {
                    if (timeout != null) {
                        build = client.executeBuild(buildConfigId, buildParams, timeout, TimeUnit.MINUTES);
                    } else if (wait) {
                        build = client.executeBuild(buildConfigId, buildParams).join();
                    } else {
                        build = client.trigger(buildConfigId, buildParams);
                    }
                } else {
                    if (timeout != null) {
                        build = client.executeBuild(buildConfigId, revision, buildParams, timeout, TimeUnit.MINUTES);
                    } else if (wait) {
                        build = client.executeBuild(buildConfigId, revision, buildParams).join();
                    } else {
                        build = client.triggerRevision(buildConfigId, revision, buildParams);
                    }
                }
                ObjectHelper.print(getJsonOutput(), build);
                return build.getStatus().completedSuccessfully() ? 0 : build.getStatus().ordinal();
            }
        }
    }

    @Command(
            name = "cancel",
            description = "Cancel build",
            footer = Constant.EXAMPLE_TEXT + "$ bacon pnc build cancel 10000")
    public static class Cancel implements Callable<Integer> {

        @Parameters(description = "Build ID")
        private String buildId;

        /**
         * Computes a result, or throws an exception if unable to do so.
         *
         * @return computed result
         * @throws Exception if unable to compute a result
         */
        @Override
        public Integer call() throws Exception {
            try (BuildClient client = CREATOR.newClientAuthenticated()) {
                client.cancel(buildId);
                return 0;
            }
        }
    }

    @Command(name = "list", description = "List builds")
    public static class List extends AbstractBuildListCommand {

        @Option(
                names = "--attributes",
                description = "Get only builds with given attributes. Format: KEY=VALUE,KEY=VALUE")
        private java.util.List<String> attributes;

        @Override
        public Collection<Build> getAll(BuildsFilterParameters buildsFilter, String sort, String query)
                throws RemoteResourceException {
            try (BuildClient client = CREATOR.newClient()) {
                return client.getAll(buildsFilter, attributes, Optional.ofNullable(sort), Optional.ofNullable(query))
                        .getAll();
            }
        }
    }

    @Command(
            name = "list-built-artifacts",
            description = "List built artifacts",
            footer = Constant.EXAMPLE_TEXT + "$ bacon pnc build list-built-artifacts 10000")
    public static class ListBuildArtifacts extends AbstractListCommand<Artifact> {

        @Parameters(description = "Build ID")
        private String buildId;

        @Override
        public Collection<Artifact> getAll(String sort, String query) throws RemoteResourceException {
            try (BuildClient client = CREATOR.newClient()) {
                return client.getBuiltArtifacts(buildId, Optional.ofNullable(sort), Optional.ofNullable(query))
                        .getAll();
            }
        }
    }

    @Command(
            name = "list-dependencies",
            description = "List dependencies",
            footer = Constant.EXAMPLE_TEXT + "$ bacon pnc build list-dependencies 10000")
    public static class ListDependencies extends AbstractListCommand<Artifact> {

        @Parameters(description = "Build ID")
        private String buildId;

        @Override
        public Collection<Artifact> getAll(String sort, String query) throws RemoteResourceException {
            try (BuildClient client = CREATOR.newClient()) {
                return client.getDependencyArtifacts(buildId, Optional.ofNullable(sort), Optional.ofNullable(query))
                        .getAll();
            }
        }
    }

    @Command(name = "get", description = "Get a build by its id")
    public static class Get extends AbstractGetSpecificCommand<Build> {

        @Override
        public Build getSpecific(String id) throws ClientException {
            try (BuildClient client = CREATOR.newClient()) {
                return client.getSpecific(id);
            }
        }
    }

    @Command(name = "get-revision", description = "Get build-config revision of build")
    public static class GetRevision extends AbstractGetSpecificCommand<BuildConfigurationRevision> {

        @Override
        public BuildConfigurationRevision getSpecific(String id) throws ClientException {
            try (BuildClient client = CREATOR.newClient()) {
                return client.getBuildConfigRevision(id);
            }
        }
    }

    @Command(name = "get-log", description = "Get build log.")
    public static class GetLog implements Callable<Integer> {
        @Parameters(description = "Build id.")
        private String buildId;

        @Option(names = "--follow", description = "Follow the live log.")
        private boolean follow;

        /**
         * Computes a result, or throws an exception if unable to do so.
         *
         * @return computed result
         * @throws Exception if unable to compute a result
         */
        @Override
        public Integer call() throws Exception {
            String bifrostBase = Config.instance().getActiveProfile().getPnc().getBifrostBaseurl();
            URI bifrostUri = URI.create(bifrostBase);
            BifrostClient logProcessor = new BifrostClient(bifrostUri);
            logProcessor.writeLog(
                    buildId,
                    follow,
                    log::info,
                    follow ? BifrostClient.LogType.COMPLETE : BifrostClient.LogType.BUILD);
            return 0;
        }
    }

    @Command(name = "get-align-log", description = "Get alignment log.")
    public static class GetAlignLog implements Callable<Integer> {
        @Parameters(description = "Build id.")
        private String buildId;

        /**
         * Computes a result, or throws an exception if unable to do so.
         *
         * @return computed result
         * @throws Exception if unable to compute a result
         */
        @Override
        public Integer call() throws Exception {
            try (BuildClient buildClient = new BuildClient(PncClientHelper.getPncConfiguration(false))) {
                Optional<InputStream> streamLogs = buildClient.getAlignLogs(buildId);
                if (streamLogs.isPresent()) {
                    for (String line : new BufferedReader(
                            new InputStreamReader(streamLogs.get(), StandardCharsets.UTF_8)).lines().toList()) {
                        log.info(line);
                    }
                }
            }
            return 0;
        }
    }

    @Command(name = "download-sources", description = "Download SCM sources used for the build")
    public static class DownloadSources implements Callable<Integer> {

        @Parameters(description = "Id of build")
        private String id;

        /**
         * Computes a result, or throws an exception if unable to do so.
         *
         * @return computed result
         * @throws Exception if unable to compute a result
         */
        @Override
        public Integer call() throws Exception {
            String filename = id + "-sources.tar.gz";

            try (BuildClient client = CREATOR.newClient(); Response response = client.getInternalScmArchiveLink(id)) {
                InputStream in = (InputStream) response.getEntity();

                Path path = Paths.get(filename);

                try {
                    Files.copy(in, path);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                return response.getStatus();
            }
        }
    }
}
