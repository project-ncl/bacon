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
package org.jboss.bacon.da;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;

import org.jboss.bacon.da.rest.endpoint.ListingsApi;
import org.jboss.da.listings.model.rest.RestArtifact;
import org.jboss.pnc.bacon.common.ObjectHelper;
import org.jboss.pnc.bacon.common.cli.JSONCommandHandler;
import org.jboss.pnc.bacon.common.exception.FatalException;

import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;

@CommandLine.Command(
        name = "blocklist",
        description = "DA blocklist",
        subcommands = { DABlockListCli.List.class, DABlockListCli.Add.class })
@Slf4j
public class DABlockListCli {

    @CommandLine.Command(name = "list", description = "List the GAVs in the blocklist")
    public static class List extends JSONCommandHandler implements Callable<Integer> {

        @Override
        public Integer call() {

            ListingsApi listingsApi = DaHelper.createListingsApi();
            try {
                Collection<RestArtifact> artifacts = listingsApi.getAllBlackArtifacts();
                ObjectHelper.print(getJsonOutput(), artifacts);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return 0;
        }
    }

    @CommandLine.Command(name = "add", description = "Add GAVs to the blocklist")
    public static class Add extends JSONCommandHandler implements Callable<Integer> {

        @CommandLine.Parameters(description = "groupId:artifactId:version of the artifact to lookup")
        private String[] gavs;

        @Override
        public Integer call() {

            if (gavs == null) {
                throw new FatalException("You didn't specify any GAVs!");
            }

            Set<RestArtifact> gavSet = new HashSet<>();
            for (String gav : gavs) {
                gavSet.add(DaHelper.toRestArtifact(gav));
            }

            ListingsApi listingsApi = DaHelper.createAuthenticatedListingsApi();
            try {
                for (RestArtifact artifact : gavSet) {
                    listingsApi.addBlackArtifact(artifact);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return 0;
        }
    }
}
