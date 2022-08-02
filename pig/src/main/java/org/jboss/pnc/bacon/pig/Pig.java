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
package org.jboss.pnc.bacon.pig;

import lombok.extern.slf4j.Slf4j;
import org.jboss.pnc.bacon.common.ObjectHelper;
import org.jboss.pnc.bacon.common.cli.JSONCommandHandler;
import org.jboss.pnc.bacon.common.exception.FatalException;
import org.jboss.pnc.bacon.config.Config;
import org.jboss.pnc.bacon.config.PigConfig;
import org.jboss.pnc.bacon.config.Validate;
import org.jboss.pnc.bacon.pig.impl.PigContext;
import org.jboss.pnc.bacon.pig.impl.config.GroupBuildInfo;
import org.jboss.pnc.bacon.pig.impl.config.PigConfiguration;
import org.jboss.pnc.bacon.pig.impl.out.PigBuildOutput;
import org.jboss.pnc.bacon.pig.impl.out.PigReleaseOutput;
import org.jboss.pnc.bacon.pig.impl.out.PigRunOutput;
import org.jboss.pnc.bacon.pig.impl.pnc.ImportResult;
import org.jboss.pnc.bacon.pig.impl.repo.RepositoryData;
import org.jboss.pnc.bacon.pig.impl.utils.AlignmentType;
import org.jboss.pnc.bacon.pig.impl.utils.FileDownloadUtils;
import org.jboss.pnc.bacon.pnc.common.ParameterChecker;
import org.jboss.pnc.enums.RebuildMode;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.Callable;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com <br>
 *         Date: 12/13/18
 */
@Command(
        name = "pig",
        description = "PiG tool",
        subcommands = {
                Pig.Configure.class,
                Pig.Cancel.class,
                Pig.Build.class,
                Pig.Run.class,
                Pig.GenerateRepository.class,
                Pig.GenerateLicenses.class,
                Pig.GenerateJavadocs.class,
                Pig.GenerateSources.class,
                Pig.GenerateSharedContentAnalysis.class,
                Pig.GenerateDocuments.class,
                Pig.Release.class,
                Pig.PreProcessYaml.class,
                Pig.TriggerAddOns.class,
                PigExport.class })
public class Pig {

    private Pig() {
    }

    public static final String REBUILD_MODE_DESC = "The build mode EXPLICIT_DEPENDENCY_CHECK, IMPLICIT_DEPENDENCY_CHECK, FORCE. Defaults to EXPLICIT";
    public static final String REBUILD_MODE_DEFAULT = "EXPLICIT_DEPENDENCY_CHECK";
    public static final String REBUILD_MODE = "--mode";
    public static final String TEMP_BUILD_TIME_STAMP = "--tempBuildTimeStamp";
    public static final String TEMP_BUILD_TIME_STAMP_DEFAULT = "false";
    public static final String TEMP_BUILD_TIME_STAMP_DESC = "NOT SUPPORTED - If specified, artifacts from temporary builds will have timestamp in versions";

    public static final String REMOVE_M2_DUPLICATES_DESC = "If enabled, only the newest versions of each of the dependencies (groupId:artifactId) "
            + "are kept in the generated repository zip";
    public static final String REMOVE_M2_DUPLICATES = "--removeGeneratedM2Dups";

    public static final String SKIP_BRANCH_CHECK = "--skipBranchCheck";
    public static final String SKIP_BRANCH_CHECK_DEFAULT = "false";
    public static final String SKIP_BRANCH_CHECK_DESC = "If set to true, pig won't try to determine if the branch that is used to build from is modified. "
            + "Branch modification check takes a lot of time, if you use tag, this switch can speed up the build.";

    public abstract static class PigCommand<T> extends JSONCommandHandler implements Callable<Integer> {
        @Parameters(description = "Directory containing the Pig configuration file")
        String configDir;

        @Option(
                names = { "-t", "--tempBuild" },
                defaultValue = "false",
                description = "If specified, PNC will perform temporary builds.")
        boolean tempBuild;

        @Option(
                names = { "--tempBuildAlignment" },
                description = "Alignment preference for temporary build. Options: (default)TEMPORARY, PERSISTENT")
        AlignmentType tempAlign = AlignmentType.TEMPORARY;

        @Option(
                names = "--releaseStorageUrl",
                description = "Location of the release storage, typically on rcm-guest staging. Required for auto-incremented milestones, e.g. DR*")
        protected String releaseStorageUrl;

        @Option(
                names = "--clean",
                defaultValue = "false",
                description = "If enabled, the pig execution will not attempt to continue the previous execution")
        private boolean clean;

        @Option(
                names = "--downloadAttempts",
                defaultValue = "5",
                description = "How many times should attempts to download files (e.g. from Indy to repo zip) be made")
        private int downloadAttempts;

        @Option(
                names = "--skipJsonOutput",
                defaultValue = "false",
                description = "Stop all the build output JSON going to stdout")
        private boolean skipJsonOutput;

        @Option(
                names = "--targetPath",
                defaultValue = "target",
                description = "The directory where the deliverables will be put")
        private String targetPath;

        @Option(
                names = { "-e", "--env" },
                description = "Override the variables in the build-config.yaml. e.g -eVariable1=value1 -e Variable2=value2 --env=Variable3=value3")
        Map<String, String> overrides = Collections.emptyMap();

        /**
         * Computes a result, or throws an exception if unable to do so.
         *
         * @return computed result
         * @throws Exception if unable to compute a result
         */
        @Override
        public Integer call() throws Exception {
            if (configDir == null) {
                throw new FatalException("You need to specify the configuration directory!");
            }
            // validate the PiG config
            PigConfig pig = Config.instance().getActiveProfile().getPig();
            if (pig == null) {
                throw new Validate.ConfigMissingException("Pig configuration missing");
            }
            pig.validate();

            FileDownloadUtils.setAttempts(downloadAttempts);

            PigContext.init(clean || isStartingPoint(), Paths.get(configDir), targetPath, releaseStorageUrl, overrides);
            PigContext.get().setTempBuild(tempBuild);
            if (skipJsonOutput) {
                doExecute();
            } else {
                ObjectHelper.print(getJsonOutput(), doExecute());
            }

            return 0;
        }

        boolean isStartingPoint() {
            return false;
        }

        abstract T doExecute();
    }

    /* System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", "10"); */
    @Command(name = "run", description = "Run all the steps, except the release & pre-process-yaml phases")
    public static class Run extends PigCommand<PigRunOutput> {

        // TODO: it is doable to do this step with build group id only, add this functionality
        // @Option(shortName = 'b',
        // description = "id of the group to build. Exactly one of {config, build-group} has to be provided")
        // private Integer buildGroupId;

        @Option(
                names = TEMP_BUILD_TIME_STAMP,
                defaultValue = TEMP_BUILD_TIME_STAMP_DEFAULT,
                description = TEMP_BUILD_TIME_STAMP_DESC)
        private boolean tempBuildTS;

        @Option(names = REBUILD_MODE, defaultValue = REBUILD_MODE_DEFAULT, description = REBUILD_MODE_DESC)
        private String rebuildMode;

        @Option(
                names = "--skipPncUpdate",
                defaultValue = "false",
                description = "Skip updating PNC entities. Use only if you have all entities created properly.")
        private boolean skipPncUpdate;

        @Option(
                names = "--skipBuilds",
                defaultValue = "false",
                description = "Skip PNC builds. Use when all your builds went fine, something failed later "
                        + "and you want to retry generating deliverables without rebuilding.")
        private boolean skipBuilds;

        @Option(names = "--skipSources", defaultValue = "false", description = "Skip sources generation.")
        private boolean skipSources;

        @Option(names = "--skipJavadoc", defaultValue = "false", description = "Skip Javadoc generation.")
        private boolean skipJavadoc;

        @Option(names = "--skipLicenses", defaultValue = "false", description = "Skip Licenses generation.")
        private boolean skipLicenses;

        @Option(
                names = "--skipSharedContent",
                defaultValue = "false",
                description = "Skip generating shared content request input.")
        private boolean skipSharedContent;

        @Option(names = REMOVE_M2_DUPLICATES, description = REMOVE_M2_DUPLICATES_DESC)
        private boolean removeGeneratedM2Dups;

        @Option(
                names = SKIP_BRANCH_CHECK,
                defaultValue = SKIP_BRANCH_CHECK_DEFAULT,
                description = SKIP_BRANCH_CHECK_DESC)
        private boolean skipBranchCheck;

        @Option(
                names = { "-r", "--repoZip" },
                description = "Repository zip. "
                        + "Might be used if you have already downloaded repository zip to speed up the process.")
        private String repoZipPath;

        @Option(
                names = "--strictLicenseCheck",
                defaultValue = "true",
                description = "if set to true will fail on license zip with missing/invalid entries")
        private boolean strictLicenseCheck;

        @Option(
                names = "--strictDownloadSource",
                defaultValue = "false",
                description = "If set, any missing source jars will throw an error")
        private boolean strictDownloadSource;

        /**
         * addOns to skip. Specified in CLI by using the flag multiple times: --skipAddon=a --skipAddon=b
         *
         * If the flag is not specified, the defaultValue causes 'skippedAddons' to be an empty array
         */
        @Option(
                names = "--skipAddon",
                defaultValue = "",
                description = "Specify which addon(s) to skip. Can be specified multiple times (e.g --skipAddon=a --skipAddon=b")
        private String[] skippedAddons;

        @Override
        public PigRunOutput doExecute() {

            PigFacade.exitIfSkippedAddonsInvalid(skippedAddons);
            ParameterChecker.checkRebuildModeOption(rebuildMode);
            Path configurationDirectory = Paths.get(configDir);

            GroupBuildInfo groupBuildInfo = PigFacade.run(
                    skipPncUpdate,
                    skipBuilds,
                    skipSources,
                    skipJavadoc,
                    skipLicenses,
                    skipSharedContent,
                    removeGeneratedM2Dups,
                    repoZipPath,
                    tempBuild,
                    tempAlign,
                    tempBuildTS,
                    RebuildMode.valueOf(rebuildMode),
                    skipBranchCheck,
                    strictLicenseCheck,
                    strictDownloadSource,
                    skippedAddons,
                    configurationDirectory);

            PigContext context = PigContext.get();
            return new PigRunOutput(
                    context.getFullVersion(),
                    groupBuildInfo,
                    context.getReleaseDirName(),
                    context.getReleasePath());
        }

        @Override
        boolean isStartingPoint() {
            return true;
        }
    }

    @Command(name = "cancel", description = "Cancel running group build")
    public static class Cancel extends PigCommand<String> {

        @Override
        public String doExecute() {
            return PigFacade.cancel();
        }
    }

    @Command(name = "configure", description = "Configure PNC entities")
    public static class Configure extends PigCommand<ImportResult> {

        @Option(
                names = SKIP_BRANCH_CHECK,
                defaultValue = SKIP_BRANCH_CHECK_DEFAULT,
                description = SKIP_BRANCH_CHECK_DESC)
        private boolean skipBranchCheck;

        @Override
        public ImportResult doExecute() {
            ImportResult importResult = PigFacade.configure(skipBranchCheck, tempBuild);
            PigContext.get().setPncImportResult(importResult);
            PigContext.get().storeContext();
            return importResult;
        }

        @Override
        boolean isStartingPoint() {
            return true;
        }
    }

    @Command(name = "build", description = "Build")
    public static class Build extends PigCommand<PigBuildOutput> {

        // TODO: it is doable to do this step with build group id only, add this functionality
        // @Option(shortName = 'b',
        // description = "id of the group to build. Exactly one of {config, build-group} has to be provided")
        // private Integer buildGroupId;

        @Option(
                names = { TEMP_BUILD_TIME_STAMP },
                defaultValue = TEMP_BUILD_TIME_STAMP_DEFAULT,
                description = TEMP_BUILD_TIME_STAMP_DESC)
        private boolean tempBuildTS;

        @Option(names = REBUILD_MODE, defaultValue = REBUILD_MODE_DEFAULT, description = REBUILD_MODE_DESC)
        private String rebuildMode;

        @Option(names = "--noWait", defaultValue = "true", description = "Start build and don't wait for result.")
        private boolean wait;

        @Override
        public PigBuildOutput doExecute() {

            ParameterChecker.checkRebuildModeOption(rebuildMode);
            GroupBuildInfo groupBuildInfo = PigFacade
                    .build(tempBuild, tempBuildTS, RebuildMode.valueOf(rebuildMode), wait, tempAlign);
            if (wait) {
                PigContext context = PigContext.get();
                context.setBuilds(groupBuildInfo.getBuilds());
                context.storeContext();

                return new PigBuildOutput(
                        "Build started, waited for result.",
                        new PigRunOutput(
                                context.getFullVersion(),
                                groupBuildInfo,
                                context.getReleaseDirName(),
                                context.getReleasePath()));
            }
            return new PigBuildOutput("Builds started, not waiting for result.", null);
        }
    }

    @Command(name = "repo", description = "GenerateRepository")
    public static class GenerateRepository extends PigCommand<RepositoryData> {
        @Option(names = REMOVE_M2_DUPLICATES, description = REMOVE_M2_DUPLICATES_DESC)
        private boolean removeGeneratedM2Dups;
        @Option(
                names = "--strictLicenseCheck",
                defaultValue = "true",
                description = "if set to true will fail on license zip with missing/invalid entries")
        private boolean strictLicenseCheck;

        @Option(
                names = "--strictDownloadSource",
                defaultValue = "false",
                description = "If set, any missing source jars will throw an error")
        private boolean strictDownloadSource;

        @Override
        public RepositoryData doExecute() {
            Path configurationDirectory = Paths.get(configDir);
            RepositoryData result = PigFacade.generateRepo(
                    removeGeneratedM2Dups,
                    configurationDirectory,
                    strictLicenseCheck,
                    strictDownloadSource);
            PigContext.get().setRepositoryData(result);
            PigContext.get().storeContext();
            return result;
        }
    }

    @Command(name = "licenses", description = "GenerateLicenses")
    public static class GenerateLicenses extends PigCommand<String> {
        @Option(
                names = "--strictLicenseCheck",
                defaultValue = "true",
                description = "if set to true will fail on license zip with missing/invalid entries")
        private boolean strictLicenseCheck;

        @Override
        public String doExecute() {
            PigFacade.generateLicenses(strictLicenseCheck);
            return "Licenses generated successfully"; // TODO: better output
        }
    }

    @Command(name = "javadocs", description = "GenerateJavadocs")
    public static class GenerateJavadocs extends PigCommand<String> {

        @Override
        public String doExecute() {
            PigFacade.generateJavadoc();
            return "Javadocs generated successfully"; // TODO: better output
        }
    }

    @Command(name = "sources", description = "GenerateSources")
    public static class GenerateSources extends PigCommand<String> {

        @Override
        public String doExecute() {
            PigFacade.generateSources();
            return "Sources gathered successfully"; // TODO: better output
        }
    }

    @Command(name = "shared-content", description = "GenerateSharedContentAnalysis")
    public static class GenerateSharedContentAnalysis extends PigCommand<String> {

        @Override
        public String doExecute() {
            PigFacade.prepareSharedContentAnalysis();
            return "Shared content analysis document prepared successfully"; // TODO: better output
        }
    }

    @Command(name = "docs", description = "GenerateDocuments")
    public static class GenerateDocuments extends PigCommand<String> {

        @Override
        public String doExecute() {
            PigFacade.generateDocuments();
            return "Documents generated successfully"; // TODO: better output
        }
    }

    @Command(name = "addons", description = "Addons")
    public static class TriggerAddOns extends PigCommand<String> {

        /**
         * addOns to skip. Specified in CLI by using the flag multiple times: --skipAddon=a --skipAddon=b
         *
         * If the flag is not specified, the defaultValue causes 'skippedAddons' to be an empty array
         */
        @Option(
                names = "--skipAddon",
                defaultValue = "",
                description = "Specify which addon(s) to skip. Can be specified multiple times (e.g --skipAddon=a --skipAddon=b")
        private String[] skippedAddons;

        @Override
        public String doExecute() {
            PigFacade.exitIfSkippedAddonsInvalid(skippedAddons);
            PigFacade.triggerAddOns(skippedAddons);
            return "Add-ons executed successfully";
        }
    }

    @Command(
            name = "release",
            description = "Push builds to brew, generate the NVR list, "
                    + "close the PNC milestone, generate the upload to candidates script if repository generated")
    public static class Release extends PigCommand<PigReleaseOutput> {

        @Override
        public PigReleaseOutput doExecute() {
            return PigFacade.release();
        }
    }

    @Command(name = "pre-process-yaml", description = "Show the final YAML content with variables injected")
    @Slf4j
    public static class PreProcessYaml extends PigCommand<String> {

        @Override
        public String doExecute() {
            try {
                File configFile = Paths.get(configDir).resolve("build-config.yaml").toFile();
                InputStream preProcessed = PigConfiguration.preProcess(new FileInputStream(configFile), overrides);

                String contents = "";
                Scanner s = new Scanner(preProcessed);
                s.useDelimiter("\\A");

                if (s.hasNext()) {
                    contents = s.next();
                }

                // Yeah we'll violate the normal rules and just print to stdout. The JSON/YAML output has no effect here
                System.out.println(contents);
            } catch (FileNotFoundException e) {
                log.error("Config file {}{}build-config.yaml does not exist", configDir, File.separator);
            }
            return "";
        }
    }
}
