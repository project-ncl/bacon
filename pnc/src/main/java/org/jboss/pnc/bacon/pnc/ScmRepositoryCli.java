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
import org.jboss.pnc.bacon.common.Constant;
import org.jboss.pnc.bacon.common.ObjectHelper;
import org.jboss.pnc.bacon.common.cli.AbstractGetSpecificCommand;
import org.jboss.pnc.bacon.common.cli.AbstractListCommand;
import org.jboss.pnc.bacon.common.cli.JSONCommandHandler;
import org.jboss.pnc.bacon.common.exception.FatalException;
import org.jboss.pnc.bacon.pnc.common.ClientCreator;
import org.jboss.pnc.client.ClientException;
import org.jboss.pnc.client.RemoteCollection;
import org.jboss.pnc.client.RemoteResourceException;
import org.jboss.pnc.client.SCMRepositoryClient;
import org.jboss.pnc.dto.BuildConfiguration;
import org.jboss.pnc.dto.SCMRepository;
import org.jboss.pnc.dto.requests.CreateAndSyncSCMRequest;
import org.jboss.pnc.restclient.AdvancedSCMRepositoryClient;
import org.jboss.pnc.restclient.AdvancedSCMRepositoryClient.SCMCreationResult;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;

import static org.apache.commons.lang.StringUtils.isNotEmpty;

@Command(
        name = "scm-repository",
        description = "Scm repository",
        subcommands = {
                ScmRepositoryCli.CreateAndSync.class,
                ScmRepositoryCli.Get.class,
                ScmRepositoryCli.List.class,
                ScmRepositoryCli.Update.class,
                ScmRepositoryCli.ListBuildConfigs.class, })
@Slf4j
public class ScmRepositoryCli {

    private static final ClientCreator<AdvancedSCMRepositoryClient> CREATOR = new ClientCreator<>(
            AdvancedSCMRepositoryClient::new);

    @Command(
            name = "create-and-sync",
            description = "Create a repository, and wait for PNC to give us the status of creation",
            footer = Constant.EXAMPLE_TEXT
                    + "$ bacon pnc scm-repository create-and-sync --no-pre-build-sync http://github.com/project-ncl/pnc.git")
    public static class CreateAndSync extends JSONCommandHandler implements Callable<Integer> {
        @Parameters(description = "SCM URL")
        private String scmUrl;

        @Option(names = "--no-pre-build-sync", description = "Disable the pre-build sync of external repo.")
        private boolean noPreBuildSync = false;

        /**
         * Computes a result, or throws an exception if unable to do so.
         *
         * @return computed result
         * @throws Exception if unable to compute a result
         */
        @Override
        public Integer call() throws Exception {
            try (SCMRepositoryClient client = CREATOR.newClient()) {
                RemoteCollection<SCMRepository> existing = client.getAll(scmUrl, null);

                // if already exists, don't try to create!
                if (existing != null && existing.size() > 0) {

                    log.warn("Repository already exists on PNC! No creation needed");
                    ObjectHelper.print(getJsonOutput(), existing.getAll().iterator());
                    return 0;
                }
                CreateAndSyncSCMRequest createAndSyncSCMRequest = CreateAndSyncSCMRequest.builder()
                        .preBuildSyncEnabled(!noPreBuildSync)
                        .scmUrl(scmUrl)
                        .build();

                try (AdvancedSCMRepositoryClient clientAdvanced = CREATOR.newClientAuthenticated()) {
                    CompletableFuture<SCMCreationResult> futureResult = clientAdvanced
                            .createNewAndWait(createAndSyncSCMRequest);
                    if (!futureResult.isDone()) {
                        log.info("Waiting for repository '{}' to be created on PNC...", scmUrl);
                    }
                    final SCMCreationResult result = futureResult.join();
                    if (result.isSuccess()) {
                        ObjectHelper
                                .print(getJsonOutput(), result.getScmRepositoryCreationSuccess().getScmRepository());
                    } else {
                        throw new FatalException(
                                "Failure while creating repository: {}",
                                result.getRepositoryCreationFailure());
                    }
                    return 0;
                }
            }
        }
    }

    @Command(name = "get", description = "Get a repository by its id")
    public static class Get extends AbstractGetSpecificCommand<SCMRepository> {

        @Override
        public SCMRepository getSpecific(String id) throws ClientException {
            try (SCMRepositoryClient client = CREATOR.newClient()) {
                return client.getSpecific(id);
            }
        }
    }

    @Command(
            name = "update",
            description = "Update an SCM Repository",
            footer = Constant.EXAMPLE_TEXT
                    + "bacon pnc scm-repository update 5 --pre-build=false --external-scm=\"http://hello.com/test.git\"")
    public static class Update implements Callable<Integer> {
        @Parameters(description = "SCM Repository Id")
        private String id;

        @Option(
                names = "--external-scm",
                description = "External SCM URL: e.g --external-scm=\"https://github.com/project-ncl/bacon.git\"")
        private String externalScm;

        @Option(names = "--no-external-scm", description = "Specify no external scm")
        private boolean noExternalScmSpecified;

        @Option(names = "--pre-build", description = "Enable / Disable pre-build")
        private Boolean preBuild;

        /**
         * Computes a result, or throws an exception if unable to do so.
         *
         * @return computed result
         * @throws Exception if unable to compute a result
         */
        @Override
        public Integer call() throws Exception {
            try (SCMRepositoryClient client = CREATOR.newClient()) {
                SCMRepository scmRepository = client.getSpecific(id);
                SCMRepository.Builder updated = scmRepository.toBuilder();

                if (preBuild != null) {
                    updated.preBuildSyncEnabled(preBuild);
                }

                if (noExternalScmSpecified && isNotEmpty(externalScm)) {
                    throw new FatalException(
                            "You cannot specify both the 'external-scm' and 'no-external-scm' options at the same time");
                } else if (isNotEmpty(externalScm)) {
                    updated.externalUrl(externalScm);
                } else if (noExternalScmSpecified) {
                    updated.externalUrl(null);
                    log.debug("Since we're removing the external-scm, pre-build set to fals");
                    updated.preBuildSyncEnabled(false);
                }

                log.debug("SCM Repository updated to: {}", updated);

                try (AdvancedSCMRepositoryClient clientAuthenticated = CREATOR.newClientAuthenticated()) {
                    clientAuthenticated.update(id, updated.build());
                    return 0;
                }
            }
        }
    }

    @Command(name = "list", description = "List repositories")
    public static class List extends AbstractListCommand<SCMRepository> {

        @Option(names = "--match-url", description = "Exact URL to search")
        private String matchUrl;

        @Option(names = "--search-url", description = "Part of the URL to search")
        private String searchUrl;

        @Override
        public Collection<SCMRepository> getAll(String sort, String query) throws RemoteResourceException {
            try (SCMRepositoryClient client = CREATOR.newClient()) {
                return client.getAll(matchUrl, searchUrl, Optional.ofNullable(sort), Optional.ofNullable(query))
                        .getAll();
            }
        }
    }

    @Command(name = "list-build-configs", description = "List build configs that use a particular SCM repository")
    public static class ListBuildConfigs extends AbstractListCommand<BuildConfiguration> {

        @Parameters(description = "SCM Repository ID")
        private String scmRepositoryId;

        @Override
        public Collection<BuildConfiguration> getAll(String sort, String query) throws RemoteResourceException {
            try (SCMRepositoryClient client = CREATOR.newClient()) {
                return client.getBuildConfigs(scmRepositoryId, Optional.ofNullable(sort), Optional.ofNullable(query))
                        .getAll();
            }
        }
    }
}
