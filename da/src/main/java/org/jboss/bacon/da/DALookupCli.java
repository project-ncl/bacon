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

import lombok.extern.slf4j.Slf4j;
import org.jboss.bacon.da.rest.endpoint.LookupApi;
import org.jboss.da.lookup.model.MavenLatestRequest;
import org.jboss.da.lookup.model.MavenLatestResult;
import org.jboss.da.lookup.model.MavenLookupRequest;
import org.jboss.da.lookup.model.MavenLookupResult;
import org.jboss.da.lookup.model.NPMLookupRequest;
import org.jboss.da.lookup.model.NPMLookupResult;
import org.jboss.da.model.rest.GAV;
import org.jboss.da.model.rest.NPMPackage;
import org.jboss.pnc.bacon.common.ObjectHelper;
import org.jboss.pnc.bacon.common.cli.JSONCommandHandler;
import org.jboss.pnc.bacon.common.exception.FatalException;
import picocli.CommandLine;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;

@CommandLine.Command(
        name = "lookup",
        description = "DA Lookup endpoint",
        subcommands = {
                DALookupCli.LookupMaven.class,
                DALookupCli.LookupMavenLatest.class,
                DALookupCli.LookupNPM.class })
@Slf4j
public class DALookupCli {

    @CommandLine.Command(name = "maven", description = "Finds best matching versions for given Maven artifact GAVs")
    public static class LookupMaven extends JSONCommandHandler implements Callable<Integer> {

        @CommandLine.Option(names = "--temporary", description = "Lookup temporary version")
        private boolean temporary = false;

        @CommandLine.Option(names = "--managed-service", description = "Lookup managed service options")
        private boolean managedService = false;

        @CommandLine.Option(names = "--brew-pull-active", description = "Check for versions also in Brew")
        private boolean brewPullActive = false;

        @CommandLine.Parameters(description = "groupId:artifactId:version of the artifact to lookup")
        private String[] gavs;

        @Override
        public Integer call() {

            if (gavs == null) {
                throw new FatalException("You didn't specify any GAVs!");
            }

            Set<GAV> gavSet = new HashSet<>();
            for (String gav : gavs) {
                gavSet.add(DaHelper.toGAV(gav));
            }

            MavenLookupRequest request = MavenLookupRequest.builder()
                    .mode(DaHelper.getMode(temporary, managedService))
                    .brewPullActive(brewPullActive)
                    .artifacts(gavSet)
                    .build();

            LookupApi lookupApi = DaHelper.createLookupApi();
            try {
                Set<MavenLookupResult> result = lookupApi.lookupMaven(request);
                ObjectHelper.print(getJsonOutput(), result);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return 0;
        }
    }

    @CommandLine.Command(
            name = "maven-latest",
            description = "Finds latest matching versions for given Maven artifact GAVs")
    public static class LookupMavenLatest extends JSONCommandHandler implements Callable<Integer> {

        @CommandLine.Option(names = "--temporary", description = "Lookup temporary version")
        private boolean temporary = false;

        @CommandLine.Option(names = "--managed-service", description = "Lookup managed service options")
        private boolean managedService = false;

        @CommandLine.Parameters(description = "groupId:artifactId:version of the artifact to lookup")
        private String[] gavs;

        @Override
        public Integer call() {

            if (gavs == null) {
                throw new FatalException("You didn't specify any GAVs!");
            }

            Set<GAV> gavSet = new HashSet<>();
            for (String gav : gavs) {
                gavSet.add(DaHelper.toGAV(gav));
            }

            MavenLatestRequest request = MavenLatestRequest.builder()
                    .mode(DaHelper.getMode(temporary, managedService))
                    .artifacts(gavSet)
                    .build();

            LookupApi lookupApi = DaHelper.createLookupApi();
            try {
                Set<MavenLatestResult> result = lookupApi.lookupMaven(request);
                ObjectHelper.print(getJsonOutput(), result);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return 0;
        }
    }

    @CommandLine.Command(name = "npm", description = "Finds best matching versions for given NPM artifact coordinates")
    public static class LookupNPM extends JSONCommandHandler implements Callable<Integer> {

        @CommandLine.Option(names = "--temporary", description = "Lookup temporary version")
        private boolean temporary = false;

        @CommandLine.Option(names = "--managed-service", description = "Lookup managed service options")
        private boolean managedService = false;

        @CommandLine.Option(names = "--brew-pull-active", description = "Check for versions also in Brew")
        private boolean brewPullActive = false;

        @CommandLine.Parameters(description = "package:version of the artifact to lookup")
        private String[] npmVersions;

        /**
         * Computes a result, or throws an exception if unable to do so.
         *
         * @return computed result
         */
        @Override
        public Integer call() {

            if (npmVersions == null) {
                throw new FatalException("You didn't specify any npm versions!");
            }

            Set<NPMPackage> pkgs = new HashSet<>();
            for (String npmVersion : npmVersions) {
                pkgs.add(DaHelper.toNPMPackage(npmVersion));
            }

            NPMLookupRequest request = NPMLookupRequest.builder()
                    .mode(DaHelper.getMode(temporary, managedService))
                    .packages(pkgs)
                    .build();

            LookupApi lookupApi = DaHelper.createLookupApi();
            try {
                Set<NPMLookupResult> result = lookupApi.lookupNPM(request);
                ObjectHelper.print(getJsonOutput(), result);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return 0;
        }
    }
}
