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
import org.jboss.pnc.bacon.common.ObjectHelper;
import org.jboss.pnc.bacon.common.cli.AbstractCommand;
import org.jboss.pnc.bacon.common.cli.AbstractGetSpecificCommand;
import org.jboss.pnc.bacon.common.cli.AbstractListCommand;
import org.jboss.pnc.bacon.pnc.common.ClientCreator;
import org.jboss.pnc.client.ClientException;
import org.jboss.pnc.client.RemoteCollection;
import org.jboss.pnc.client.RemoteResourceException;
import org.jboss.pnc.dto.BuildConfiguration;
import org.jboss.pnc.dto.SCMRepository;
import org.jboss.pnc.dto.requests.CreateAndSyncSCMRequest;
import org.jboss.pnc.restclient.AdvancedSCMRepositoryClient;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.jboss.pnc.bacon.common.exception.FatalException;
import org.jboss.pnc.restclient.AdvancedSCMRepositoryClient.SCMCreationResult;

import static org.apache.commons.lang.StringUtils.isNotEmpty;

@GroupCommandDefinition(
        name = "scm-repository",
        description = "Scm repository",
        groupCommands = {
                ScmRepositoryCli.CreateAndSync.class,
                ScmRepositoryCli.Get.class,
                ScmRepositoryCli.List.class,
                ScmRepositoryCli.Update.class,
                ScmRepositoryCli.ListBuildConfigs.class, })
@Slf4j
public class ScmRepositoryCli extends AbstractCommand {

    private static final ClientCreator<AdvancedSCMRepositoryClient> CREATOR = new ClientCreator<>(
            AdvancedSCMRepositoryClient::new);

    @CommandDefinition(
            name = "create-and-sync",
            description = "Create a repository, and wait for PNC to give us the status of creation")
    public class CreateAndSync extends AbstractCommand {

        @Argument(required = true, description = "SCM URL")
        private String scmUrl;

        @Option(
                name = "no-pre-build-sync",
                description = "Disable the pre-build sync of external repo.",
                hasValue = false)
        private boolean noPreBuildSync = false;

        @Option(
                shortName = 'o',
                overrideRequired = false,
                hasValue = false,
                description = "use json for output (default to yaml)")
        private boolean jsonOutput = false;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation)
                throws CommandException, InterruptedException {

            // TODO: do the same for pig side
            return super.executeHelper(commandInvocation, () -> {

                RemoteCollection<SCMRepository> existing = CREATOR.getClient().getAll(scmUrl, null);

                // if already exists, don't try to create!
                if (existing != null && existing.size() > 0) {

                    log.warn("Repository already exists on PNC! No creation needed");
                    ObjectHelper.print(jsonOutput, existing.getAll().iterator());
                    return 0;
                }
                CreateAndSyncSCMRequest createAndSyncSCMRequest = CreateAndSyncSCMRequest.builder()
                        .preBuildSyncEnabled(!noPreBuildSync)
                        .scmUrl(scmUrl)
                        .build();

                CompletableFuture<SCMCreationResult> futureResult = CREATOR.getClientAuthenticated()
                        .createNewAndWait(createAndSyncSCMRequest);
                if (!futureResult.isDone()) {
                    log.info("Waiting for repository '{}' to be created on PNC...", scmUrl);
                }
                final SCMCreationResult result = futureResult.join();
                if (result.isSuccess()) {
                    ObjectHelper.print(jsonOutput, result.getScmRepositoryCreationSuccess().getScmRepository());
                } else {
                    throw new FatalException(
                            "Failure while creating repository: {}",
                            result.getRepositoryCreationFailure());
                }
                return 0;
            });
        }

        @Override
        public String exampleText() {
            return "$ bacon pnc scm-repository create-and-sync --no-pre-build-sync http://github.com/project-ncl/pnc.git";
        }
    }

    @CommandDefinition(name = "get", description = "Get a repository by its id")
    public class Get extends AbstractGetSpecificCommand<SCMRepository> {

        @Override
        public SCMRepository getSpecific(String id) throws ClientException {
            return CREATOR.getClient().getSpecific(id);
        }
    }

    @CommandDefinition(name = "update", description = "Update an SCM Repository")
    public class Update extends AbstractCommand {
        @Argument(required = true, description = "SCM Repository Id")
        private String id;

        @Option(name = "external-scm", description = "External SCM URL")
        private String externalScm;

        @Option(name = "pre-build", description = "Enable / Disable pre-build")
        private Boolean preBuild;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation)
                throws CommandException, InterruptedException {

            return super.executeHelper(commandInvocation, () -> {

                SCMRepository scmRepository = CREATOR.getClient().getSpecific(id);
                SCMRepository.Builder updated = scmRepository.toBuilder();
                if (isNotEmpty(externalScm)) {
                    updated.externalUrl(externalScm);
                }
                if (preBuild != null) {
                    updated.preBuildSyncEnabled(preBuild);
                }
                log.debug("SCM Repository updated to: {}", updated);

                CREATOR.getClientAuthenticated().update(id, updated.build());
                return 0;
            });
        }

        @Override
        public String exampleText() {
            return "bacon pnc scm-repository update 5 --pre-build=false --external-scm=\"http://hello.com/test.git\"";
        }
    }

    @CommandDefinition(name = "list", description = "List repositories")
    public class List extends AbstractListCommand<SCMRepository> {

        @Option(name = "match-url", description = "Exact URL to search")
        private String matchUrl;

        @Option(name = "search-url", description = "Part of the URL to search")
        private String searchUrl;

        @Override
        public RemoteCollection<SCMRepository> getAll(String sort, String query) throws RemoteResourceException {
            return CREATOR.getClient()
                    .getAll(matchUrl, searchUrl, Optional.ofNullable(sort), Optional.ofNullable(query));
        }
    }

    @CommandDefinition(
            name = "list-build-configs",
            description = "List build configs that use a particular SCM repository")
    public class ListBuildConfigs extends AbstractListCommand<BuildConfiguration> {

        @Argument(description = "SCM Repository ID")
        private String scmRepositoryId;

        @Override
        public RemoteCollection<BuildConfiguration> getAll(String sort, String query) throws RemoteResourceException {
            return CREATOR.getClient()
                    .getBuildConfigs(scmRepositoryId, Optional.ofNullable(sort), Optional.ofNullable(query));
        }
    }
}
