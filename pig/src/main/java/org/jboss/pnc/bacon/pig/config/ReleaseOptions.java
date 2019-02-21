// TODO: bring back or remove
///**
// * JBoss, Home of Professional Open Source.
// * Copyright 2017 Red Hat, Inc., and individual contributors
// * as indicated by the @author tags.
// * <p>
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// * <p>
// * http://www.apache.org/licenses/LICENSE-2.0
// * <p>
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//package org.jboss.pnc.bacon.pig.config;
//
//import lombok.RequiredArgsConstructor;
//import org.apache.commons.cli.CommandLine;
//import org.apache.commons.cli.Options;
//import org.jboss.prod.generator.utils.LoggerUtils;
//
//import java.nio.file.Path;
//import java.nio.file.Paths;
//
///**
// * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
// * <br>
// * Date: 11/28/17
// */
//@RequiredArgsConstructor
//public class ReleaseOptions {
//
//    private static final String BUILD_CONFIGURATION = "c";
//    private static final String BUILD_VARS_OVERRIDES = "v";
//    private static final String BUILD_VARS_OVERRIDES_LONG = "buildVarsOverrides";
//    private static final String DESTINATION_DIRECTORY = "d";
//    private static final String REPO_ZIP = "r";
//    private static final String REPO_ZIP_LONG = "repoZip";
//    private static final String VERBOSE = "verbose";
//    private static final String SKIP_DA = "sda";
//    private static final String SKIP_DA_LONG = "skipDa";
//    private static final String SKIP_SHARED_CONTENT = "ssc";
//    private static final String SKIP_SHARED_CONTENT_LONG = "skipSharedContent";
//    private static final String SKIP_NVR_LIST = "snl";
//    private static final String SKIP_NVR_LIST_LONG = "skipNvrList";
//    private static final String RELEASE_STORAGE_URL = "releaseStorageUrl";
//    private static final String SKIP_BUILDS = "skipBuilds";
//    private static final String SKIP_SOURCES = "skipSources"; // TODO: remove?
//    private static final String SKIP_LICENSES = "skipLicenses";
//    private static final String SKIP_JAVADOC = "skipJavadoc";
//    private static final String SKIP_REPO = "skipRepo";
//    private static final String TEMP_BUILD = "tempBuild";
//    private static final String TEMP_BUILD_TS = "tempBuildTimeStamp";
//    private static final String REMOVE_GENERATED_M_2_DUPS = "removeGeneratedM2Dups";
//    public static final String SKIP_PNC_UPDATE = "skipPncUpdate";
//
//    public final String repositoryZipPath;
//    public final Path configurationDirectory;
//    public final String buildVarsOverrides;
//    public final boolean skipDa;
//    public final boolean skipSharedContent;
//    public final boolean skipNvrList;
//    public final boolean verbose;
//    public final String destinationDirectory;
//    public final String releaseStorageUrl;
//    public final boolean skipBuilds;
//    public final boolean skipSources;
//    public final boolean skipLicenses;
//    public final boolean skipJavadoc;
//    public final boolean skipRepo;
//    public final boolean tempBuild;
//    public final boolean tempBuildTS;
//    public final boolean removeGeneratedM2Dups;
//    public final boolean skipPncUpdate;
//
//    public ReleaseOptions(CommandLine commandLine) {
//        configurationDirectory = Paths.get(commandLine.getOptionValue(BUILD_CONFIGURATION));
//        buildVarsOverrides = commandLine.getOptionValue(BUILD_VARS_OVERRIDES, "");
//
//        repositoryZipPath = (String) commandLine.getOptionValue(REPO_ZIP);
//
//        skipDa = commandLine.hasOption(SKIP_DA);
//        skipSharedContent = commandLine.hasOption(SKIP_SHARED_CONTENT);
//        skipNvrList = commandLine.hasOption(SKIP_NVR_LIST);
//        verbose = commandLine.hasOption(VERBOSE);
//        releaseStorageUrl = commandLine.getOptionValue(RELEASE_STORAGE_URL, null);
//        destinationDirectory = commandLine.getOptionValue(DESTINATION_DIRECTORY, "target");
//        skipSources = commandLine.hasOption(SKIP_SOURCES);
//        skipLicenses = commandLine.hasOption(SKIP_LICENSES);
//        skipJavadoc = commandLine.hasOption(SKIP_JAVADOC);
//        skipRepo = commandLine.hasOption(SKIP_REPO);
//        tempBuild = commandLine.hasOption(TEMP_BUILD);
//        tempBuildTS = commandLine.hasOption(TEMP_BUILD_TS);
//        removeGeneratedM2Dups = commandLine.hasOption(REMOVE_GENERATED_M_2_DUPS);
//
//        skipPncUpdate = commandLine.hasOption(SKIP_PNC_UPDATE);
//        skipBuilds = commandLine.hasOption(SKIP_BUILDS);
//        LoggerUtils.setVerboseLogging(verbose);
//    }
//
//    public static void registerOptions(Options options) {
//        options.addRequiredOption(BUILD_CONFIGURATION,
//                "build-configuration",
//                true,
//                "A configuration directory for the build. It should contain build.properties file. " +
//                        "It may additionally contain template files");
//
//        options.addOption(BUILD_VARS_OVERRIDES,
//            BUILD_VARS_OVERRIDES_LONG,
//            true,
//            "[OPTIONAL] A string that contains a comma separated list of build vars (as defined in build-config.yaml) to be overridden. The name and value are separated by the = sign");
//
//
//        options.addOption(DESTINATION_DIRECTORY,
//                "destination",
//                true,
//                "[OPTIONAL] The output directory");
//
//        options.addOption(REPO_ZIP,
//                REPO_ZIP_LONG,
//                true,
//                "FOR TESTING ONLY. RESULTS IN NOT PUTTING REPO ZIP TO THE RELEASE DIR.\n" +
//                        "Repository zip. " +
//                        "Might be used if you have already downloaded repository zip to speed up the process.");
//
//        options.addOption(SKIP_DA,
//                SKIP_DA_LONG,
//                false,
//                "[OPTIONAL] Skip Dependency Analyzer invocation");
//        options.addOption(SKIP_SHARED_CONTENT,
//                SKIP_SHARED_CONTENT_LONG,
//                false,
//                "[OPTIONAL] Skip generating Shared Content Request input");
//        options.addOption(SKIP_NVR_LIST,
//                SKIP_NVR_LIST_LONG,
//                false,
//                "[OPTIONAL] Skip generating NVR list");
//        options.addOption(VERBOSE,
//                VERBOSE,
//                false,
//                "[OPTIONAL] Verbose output");
//        options.addOption(RELEASE_STORAGE_URL,
//                RELEASE_STORAGE_URL,
//                true,
//                "[OPTIONAL] Release storage area url. Required for incremental milestone (e.g. DR*)," +
//                        " ignored otherwise.");
//        options.addOption(SKIP_BUILDS,
//                SKIP_BUILDS,
//                false,
//                "[OPTIONAL] Skip PNC builds. Use when all your built went fine, something failed later " +
//                        "and you want to retry deliverables generation without rebuilding.");
//        options.addOption(SKIP_PNC_UPDATE,
//                SKIP_PNC_UPDATE,
//                false,
//                "[OPTIONAL] Skip updating PNC entities. Use only if you have all entities created properly.");
//        options.addOption(SKIP_SOURCES,
//                SKIP_SOURCES,
//                false,
//                "[OPTIONAL] skip sources generation.");
//        options.addOption(SKIP_LICENSES,
//                SKIP_LICENSES,
//                false,
//                "[OPTIONAL] skip licenses generation.");
//        options.addOption(SKIP_JAVADOC,
//                SKIP_JAVADOC,
//                false,
//                "[OPTIONAL] skip javadoc generation.");
//        options.addOption(SKIP_REPO,
//                SKIP_REPO,
//                false,
//                "[OPTIONAL] skip mavan repository generation.");
//        options.addOption(TEMP_BUILD,
//                TEMP_BUILD,
//                false,
//                "[OPTIONAL] Use PNC temporary builds");
//        options.addOption(TEMP_BUILD_TS,
//                TEMP_BUILD_TS,
//                false,
//                String.format("[OPTIONAL] Add timestamp to temporary build artifacts. %s must be enabled", TEMP_BUILD));
//        options.addOption(REMOVE_GENERATED_M_2_DUPS,
//            REMOVE_GENERATED_M_2_DUPS,
//            false,
//            "[OPTIONAL] Remove possible duplicates in generated Maven repo.");
//    }
//}
