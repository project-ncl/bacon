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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Scanner;
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

    private final static String availableModes = "Available modes: PERSISTENT, TEMPORARY, TEMPORARY_PREFER_PERSISTENT, SERVICE, SERVICE_TEMPORARY, SERVICE_TEMPORARY_PREFER_PERSISTENT";

    @CommandLine.Command(
            name = "maven",
            description = "Finds best matching versions for given Maven artifact GAVs. In PNC it is used for alignment and ignores blacklisted artifacts.")
    public static class LookupMaven extends JSONCommandHandler implements Callable<Integer> {

        @Deprecated(forRemoval = true)
        @CommandLine.Option(names = "--temporary", description = "Lookup temporary version. Deprecated.", hidden = true)
        private boolean temporary = false;

        @Deprecated(forRemoval = true)
        @CommandLine.Option(
                names = "--managed-service",
                description = "Lookup managed service options. Deprecated.",
                hidden = true)
        private boolean managedService = false;

        @CommandLine.Option(names = "--brew-pull-active", description = "Check for versions also in Brew")
        private boolean brewPullActive = false;

        @CommandLine.Option(
                names = "--lookup-mode",
                description = "Explicitly specified lookup mode to use. Default: PERSISTENT " + availableModes)
        private String lookupMode;

        @CommandLine.Parameters(description = "groupId:artifactId:version of the artifact to lookup")
        private String[] gavs;

        @Override
        public Integer call() {

            if (gavs == null) {
                throw new FatalException("You didn't specify any GAVs!");
            }

            LinkedHashSet<GAV> gavSet = new LinkedHashSet<>();
            for (String gav : gavs) {
                gavSet.add(DaHelper.toGAV(gav));
            }

            MavenLookupRequest request = MavenLookupRequest.builder()
                    .mode(DaHelper.getMode(temporary, managedService, lookupMode))
                    .brewPullActive(brewPullActive)
                    .artifacts(gavSet)
                    .build();

            LookupApi lookupApi = DaHelper.createLookupApi();
            try {
                Set<MavenLookupResult> result = lookupApi.lookupMaven(request);
                List<MavenLookupResult> orderedResult = DaHelper.orderedMavenLookupResult(gavSet, result);
                ObjectHelper.print(getJsonOutput(), orderedResult);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return 0;
        }
    }

    @CommandLine.Command(
            name = "maven-latest",
            description = "Finds latest matching versions for given Maven artifact GAVs. In PNC it is used for version increment, by default looking for versions in Indy and Brew.")
    public static class LookupMavenLatest extends JSONCommandHandler implements Callable<Integer> {

        @Deprecated(forRemoval = true)
        @CommandLine.Option(names = "--temporary", description = "Lookup temporary version. Deprecated.", hidden = true)
        private boolean temporary = false;

        @Deprecated(forRemoval = true)
        @CommandLine.Option(
                names = "--managed-service",
                description = "Lookup managed service options. Deprecated.",
                hidden = true)
        private boolean managedService = false;

        @CommandLine.Option(
                names = "--lookup-mode",
                description = "Explicitly specified lookup mode to use. Default: PERSISTENT " + availableModes)
        private String lookupMode;

        @CommandLine.Parameters(description = "groupId:artifactId:version of the artifact to lookup")
        private String[] gavs;

        @CommandLine.Option(names = "--filename", description = "filename to specify GAVs, one per line")
        private String filename;

        @Override
        public Integer call() {

            if (filename == null && gavs == null) {
                throw new FatalException("You didn't specify any GAVs or file!");
            }

            // Use LinkedHashSet to maintain order of insertion
            LinkedHashSet<GAV> gavSet = new LinkedHashSet<>();
            if (gavs != null) {
                for (String gav : gavs) {
                    gavSet.add(DaHelper.toGAV(gav));
                }
            }

            if (filename != null) {
                try (Scanner scanner = new Scanner(new File(filename))) {
                    while (scanner.hasNextLine()) {
                        String line = scanner.nextLine().trim();
                        // ignore line if it starts with '#', it's a comment
                        if (!line.isEmpty() && !line.strip().startsWith("#")) {
                            gavSet.add(DaHelper.toGAV(line));
                        }
                    }
                } catch (FileNotFoundException e) {
                    throw new FatalException("File " + filename + " does not exist!");
                }
            }

            MavenLatestRequest request = MavenLatestRequest.builder()
                    .mode(DaHelper.getMode(temporary, managedService, lookupMode))
                    .artifacts(gavSet)
                    .build();

            LookupApi lookupApi = DaHelper.createLookupApi();
            try {
                Set<MavenLatestResult> result = lookupApi.lookupMaven(request);
                List<MavenLatestResult> orderedResult = DaHelper.orderedMavenLatestResult(gavSet, result);
                ObjectHelper.print(getJsonOutput(), orderedResult);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return 0;
        }

    }

    @CommandLine.Command(name = "npm", description = "Finds best matching versions for given NPM artifact coordinates")
    public static class LookupNPM extends JSONCommandHandler implements Callable<Integer> {

        @Deprecated(forRemoval = true)
        @CommandLine.Option(names = "--temporary", description = "Lookup temporary version. Deprecated.", hidden = true)
        private boolean temporary = false;

        @Deprecated(forRemoval = true)
        @CommandLine.Option(
                names = "--managed-service",
                description = "Lookup managed service options. Deprecated.",
                hidden = true)
        private boolean managedService = false;

        @CommandLine.Option(names = "--brew-pull-active", description = "Check for versions also in Brew")
        private boolean brewPullActive = false;

        @CommandLine.Option(
                names = "--lookup-mode",
                description = "Explicitly specified lookup mode to use. Default: PERSISTENT " + availableModes)
        private String lookupMode;

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

            LinkedHashSet<NPMPackage> pkgs = new LinkedHashSet<>();
            for (String npmVersion : npmVersions) {
                pkgs.add(DaHelper.toNPMPackage(npmVersion));
            }

            NPMLookupRequest request = NPMLookupRequest.builder()
                    .mode(DaHelper.getMode(temporary, managedService, lookupMode))
                    .packages(pkgs)
                    .build();

            LookupApi lookupApi = DaHelper.createLookupApi();
            try {
                Set<NPMLookupResult> result = lookupApi.lookupNPM(request);
                List<NPMLookupResult> orderedResult = DaHelper.orderedNPMLookupResult(pkgs, result);
                ObjectHelper.print(getJsonOutput(), orderedResult);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return 0;
        }
    }
}
