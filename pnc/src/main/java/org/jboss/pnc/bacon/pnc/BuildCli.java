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
import org.aesh.command.shell.Shell;
import org.jboss.pnc.bacon.common.ObjectHelper;
import org.jboss.pnc.bacon.common.cli.AbstractCommand;
import org.jboss.pnc.bacon.common.cli.AbstractGetSpecificCommand;
import org.jboss.pnc.bacon.common.cli.AbstractListCommand;
import org.jboss.pnc.bacon.common.exception.FatalException;
import org.jboss.pnc.bacon.config.Config;
import org.jboss.pnc.bacon.pnc.client.BifrostClient;
import org.jboss.pnc.bacon.pnc.common.ClientCreator;
import org.jboss.pnc.client.*;
import org.jboss.pnc.dto.Artifact;
import org.jboss.pnc.dto.Build;
import org.jboss.pnc.enums.RebuildMode;
import org.jboss.pnc.rest.api.parameters.BuildParameters;

import javax.ws.rs.core.Response;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

@GroupCommandDefinition(
        name = "build",
        description = "build",
        groupCommands = { BuildCli.Start.class, BuildCli.Cancel.class, BuildCli.List.class,
                BuildCli.ListBuildArtifacts.class, BuildCli.ListDependencies.class, BuildCli.Get.class,
                BuildCli.GetLog.class, BuildCli.DownloadSources.class })
@Slf4j
public class BuildCli extends AbstractCommand {

    private static final ClientCreator<BuildClient> CREATOR = new ClientCreator<>(BuildClient::new);
    private static final ClientCreator<BuildConfigurationClient> BC_CREATOR = new ClientCreator<>(
            BuildConfigurationClient::new);

    @CommandDefinition(name = "start", description = "Start a new build")
    public class Start extends AbstractCommand {

        @Argument(required = true, description = "Build Config ID")
        private String buildConfigId;

        @Option(
                name = "rebuild-mode",
                description = "Default: IMPLICIT_DEPENDENCY_CHECK. Other options are: EXPLICIT_DEPENDENCY_CHECK, FORCE")
        private String rebuildMode;
        @Option(name = "keep-pod-on-failure", description = "Default: false", defaultValue = "false")
        private String keepPodOnFailure;
        @Option(name = "timestamp-alignment", description = "Default: false", defaultValue = "false")
        private String timestampAlignment;
        @Option(name = "temporary-build", description = "Temporary build, default: false", defaultValue = "false")
        private String temporaryBuild;
        @Option(
                shortName = 'o',
                overrideRequired = false,
                hasValue = false,
                description = "use json for output (default to yaml)")
        private boolean jsonOutput = false;

        public Start() {
        }

        @Override
        public CommandResult execute(CommandInvocation commandInvocation)
                throws CommandException, InterruptedException {

            BuildParameters buildParams = new BuildParameters();
            if (rebuildMode == null) {
                rebuildMode = RebuildMode.IMPLICIT_DEPENDENCY_CHECK.name();
            }
            checkRebuildModeOption(rebuildMode);

            buildParams.setRebuildMode(RebuildMode.valueOf(rebuildMode));
            buildParams.setKeepPodOnFailure(Boolean.parseBoolean(keepPodOnFailure));
            buildParams.setTimestampAlignment(Boolean.parseBoolean(timestampAlignment));
            buildParams.setTemporaryBuild(Boolean.parseBoolean(temporaryBuild));

            return super.executeHelper(commandInvocation, () -> {
                ObjectHelper.print(jsonOutput, BC_CREATOR.getClientAuthenticated().trigger(buildConfigId, buildParams));
            });
        }

        private void checkRebuildModeOption(String rebuildMode) {

            try {
                RebuildMode.valueOf(rebuildMode);
            } catch (IllegalArgumentException | NullPointerException e) {
                log.error("The rebuild flag contains an illegal option. Possibilities are: ");
                for (RebuildMode mode : RebuildMode.values()) {
                    log.error(mode.toString());
                }
                throw new FatalException();
            }
        }
    }

    @CommandDefinition(name = "cancel", description = "Cancel build")
    public class Cancel extends AbstractCommand {

        @Argument(required = true, description = "Build ID")
        private String buildId;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation)
                throws CommandException, InterruptedException {

            return super.executeHelper(commandInvocation, () -> {
                CREATOR.getClientAuthenticated().cancel(buildId);
            });
        }
    }

    @CommandDefinition(name = "list", description = "List builds")
    public class List extends AbstractListCommand<Build> {

        @Override
        public RemoteCollection<Build> getAll(String sort, String query) throws RemoteResourceException {
            return CREATOR.getClient().getAll(null, null, Optional.ofNullable(sort), Optional.ofNullable(query));
        }
    }

    @CommandDefinition(name = "list-built-artifacts", description = "List built artifacts")
    public class ListBuildArtifacts extends AbstractListCommand<Artifact> {

        @Argument(required = true, description = "Build ID")
        private String buildId;

        @Override
        public RemoteCollection<Artifact> getAll(String sort, String query) throws RemoteResourceException {
            return CREATOR.getClient()
                    .getBuiltArtifacts(buildId, Optional.ofNullable(sort), Optional.ofNullable(query));
        }
    }

    @CommandDefinition(name = "list-dependencies", description = "List dependencies")
    public class ListDependencies extends AbstractListCommand<Artifact> {

        @Argument(required = true, description = "Build ID")
        private String buildId;

        @Override
        public RemoteCollection<Artifact> getAll(String sort, String query) throws RemoteResourceException {
            return CREATOR.getClient()
                    .getDependencyArtifacts(buildId, Optional.ofNullable(sort), Optional.ofNullable(query));
        }
    }

    @CommandDefinition(name = "get", description = "Get build")
    public class Get extends AbstractGetSpecificCommand<Build> {

        @Override
        public Build getSpecific(String id) throws ClientException {
            return CREATOR.getClient().getSpecific(id);
        }
    }

    @CommandDefinition(name = "get-log", description = "Get build log.")
    public class GetLog extends AbstractCommand {
        @Argument(required = true, description = "Build id.")
        private String buildId;

        @Option(name = "follow", description = "Follow the live log.")
        private boolean follow;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation)
                throws CommandException, InterruptedException {
            return super.executeHelper(commandInvocation, () -> printLog(commandInvocation.getShell()));
        }

        private void printLog(Shell shell) throws ClientException {
            try {
                Optional<InputStream> buildLogs;
                try {
                    buildLogs = CREATOR.getClient().getBuildLogs(buildId);
                } catch (Exception e) {
                    e.printStackTrace(); // TODO, client should return Optional.empty not throw the exception (NCL-5348)
                    buildLogs = Optional.empty();
                }
                // is there a stored record
                if (buildLogs.isPresent()) {
                    try (InputStream inputStream = buildLogs.get();
                            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                            BufferedReader reader = new BufferedReader(inputStreamReader);) {
                        reader.lines().forEach(l -> shell.writeln(l));
                    } catch (IOException e) {
                        throw new ClientException("Cannot read log stream.", e);
                    }
                } else {
                    // print live log
                    String bifrostBase = Config.instance().getPnc().getBifrostBaseurl();
                    URI bifrostUri = URI.create(bifrostBase);
                    BifrostClient logProcessor = new BifrostClient(bifrostUri);
                    logProcessor.writeLog(buildId, follow, line -> shell.writeln(line));
                }
            } catch (RemoteResourceException | IOException e) {
                throw new ClientException("Cannot read remote resource.", e);
            }
        }
    }

    @CommandDefinition(name = "download-sources", description = "Download SCM sources used for the build")
    public class DownloadSources extends AbstractCommand {

        @Argument(required = true, description = "Id of build")
        private String id;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation)
                throws CommandException, InterruptedException {

            return super.executeHelper(commandInvocation, () -> {

                String filename = id + "-sources.tar.gz";

                Response response = CREATOR.getClient().getInternalScmArchiveLink(id);

                InputStream in = (InputStream) response.getEntity();

                Path path = Paths.get(filename);

                try {
                    Files.copy(in, path);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }
}
