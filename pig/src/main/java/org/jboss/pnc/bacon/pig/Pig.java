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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.GroupCommandDefinition;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Option;
import org.jboss.pnc.bacon.common.cli.AbstractCommand;
import org.jboss.pnc.bacon.pig.impl.MisconfigurationException;
import org.jboss.pnc.bacon.pig.impl.PigContext;
import org.jboss.pnc.bacon.pig.impl.PigFacade;
import org.jboss.pnc.bacon.pig.impl.pnc.ImportResult;
import org.jboss.pnc.bacon.pig.impl.pnc.PncBuild;
import org.jboss.pnc.bacon.pig.impl.repo.RepositoryData;

import java.io.IOException;
import java.util.Map;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * <br>
 * Date: 12/13/18
 */
@GroupCommandDefinition(
        name = "pig",
        description = "PiG tool",
        groupCommands = {
                Pig.Configure.class,
                Pig.Build.class,
                Pig.GenerateRepository.class,
                Pig.GenerateLicenses.class,
                Pig.GenerateJavadocs.class,
                Pig.GenerateSources.class,
                Pig.GenerateSharedContentAnalysis.class,
                Pig.GenerateDocuments.class,
                Pig.GenerateScripts.class,
                Pig.TriggerAddOns.class
        })
public class Pig extends AbstractCommand {

    private static final ObjectMapper jsonMapper = new ObjectMapper();

    public static final String REBUILD_MODE_DESC = "If specified, artifacts from temporary builds will have timestamp in versions";
    public static final String REBUILD_MODE_DEFAULT = "EXPLICIT_DEPENDENCY_CHECK";
    public static final String REBUILD_MODE = "mode";
    public static final String TEMP_BUILD_TIME_STAMP = "tempBuildTimeStamp";
    public static final String TEMP_BUILD_TIME_STAMP_DEFAULT = "false";
    public static final String TEMP_BUILD_TIME_STAMP_DESC = "If specified, artifacts from temporary builds will have timestamp in versions";
    public static final String TEMP_BUILD_DESC = "If specified, PNC will perform temporary builds";
    public static final String TEMP_BUILD_DEFAULT = "false";
    public static final String TEMP_BUILD = "tempBuild";
    public static final char TEMP_BUILD_SHORT = 't';
    public static final String CONFIG_DESC = "location of configuration file";
    public static final String CONFIG_DEFAULT = "build-config.yaml";
    public static final char CONFIG_SHORT = 'c';
    public static final String REMOVE_M2_DUPLICATES_DESC = "If enabled, only the newest versions of each of the dependencies (groupId:artifactId) " +
            "are kept in the generated repository zip";
    public static final String REMOVE_M2_DUPLICATES = "removeGeneratedM2Dups";
    public static final String REMOVE_M2_DUPLICATES_DEFAULT = "false";

    public abstract class PigCommand<T> extends AbstractCommand {
        @Option(shortName = CONFIG_SHORT,
                overrideRequired = true,
                defaultValue = CONFIG_DEFAULT,
                description = CONFIG_DESC)
        String config;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            try {
                PigContext.get().loadConfig(config);
                T result = doExecute();
                jsonMapper.writer().writeValue(System.out, result);
            } catch (MisconfigurationException e) {
                System.err.println(e.getMessage());
                return CommandResult.FAILURE;
            } catch (Exception any) {
                return CommandResult.FAILURE;
            }
            return CommandResult.SUCCESS;
        }

        abstract T doExecute() throws Exception;
    }

    /*System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", "10");*/
    @CommandDefinition(name = "run", description = "Run all the steps")
    public class Run extends PigCommand<String> {

//        TODO: it is doable to do this step with build group id only, add this functionality
//        @Option(shortName = 'b',
//                overrideRequired = true,
//                description = "id of the group to build. Exactly one of {config, build-group} has to be provided")
//        private Integer buildGroupId;

        @Option(shortName = TEMP_BUILD_SHORT,
                name = TEMP_BUILD,
                overrideRequired = true,
                defaultValue = TEMP_BUILD_DEFAULT,
                description = TEMP_BUILD_DESC)
        private boolean tempBuild;

        @Option(name = TEMP_BUILD_TIME_STAMP,
                overrideRequired = true,
                defaultValue = TEMP_BUILD_TIME_STAMP_DEFAULT,
                description = TEMP_BUILD_TIME_STAMP_DESC)
        private boolean tempBuildTS;

        @Option(name = REBUILD_MODE,
                overrideRequired = true,
                defaultValue = REBUILD_MODE_DEFAULT,
                description = REBUILD_MODE_DESC)
        private String rebuildMode;

        @Option(name = "skipRepo",
                overrideRequired = true,
                defaultValue = "false",
                description = "Skip maven repository generation")
        private boolean skipRepo;

        @Option(name = "skipPncUpdate",
                overrideRequired = true,
                defaultValue = "false",
                description = "Skip updating PNC entities. Use only if you have all entities created properly.")
        private boolean skipPncUpdate;

        @Option(name = "skipBuilds",
                overrideRequired = true,
                defaultValue = "false",
                description = "Skip PNC builds. Use when all your builds went fine, something failed later " +
                        "and you want to retry generating deliverables without rebuilding.")
        private boolean skipBuilds;

        @Option(name = "skipSources",
                overrideRequired = true,
                defaultValue = "false",
                description = "Skip sources generation.")
        private boolean skipSources;

        @Option(name = "skipJavadoc",
                overrideRequired = true,
                defaultValue = "false",
                description = "Skip Javadoc generation.")
        private boolean skipJavadoc;

        @Option(name = "skipLicenses",
                overrideRequired = true,
                defaultValue = "false",
                description = "Skip Licenses generation.")
        private boolean skipLicenses;

        @Option(name = "skipSharedContent",
                overrideRequired = true,
                defaultValue = "false",
                description = "Skip generating shared content request input.")
        private boolean skipSharedContent;

        @Option(name = REMOVE_M2_DUPLICATES,
                overrideRequired = true,
                defaultValue = REMOVE_M2_DUPLICATES_DEFAULT,
                description = REMOVE_M2_DUPLICATES_DESC)
        private boolean removeGeneratedM2Dups;

        @Option(shortName = 'r',
                name = "repoZip",
                overrideRequired = true,
                description = "Repository zip. " +
                        "Might be used if you have already downloaded repository zip to speed up the process.")
        private String repoZipPath;

        @Override
        public String doExecute() {
            PigContext.get().loadConfig(config);
            return PigFacade.run(skipRepo,
                    skipPncUpdate,
                    skipBuilds,
                    skipSources,
                    skipJavadoc,
                    skipLicenses,
                    skipSharedContent,
                    removeGeneratedM2Dups,
                    repoZipPath,
                    tempBuild,
                    tempBuildTS,
                    rebuildMode
            );
        }
    }


    @CommandDefinition(name = "configure", description = "Configure PNC entities")
    public class Configure extends PigCommand<ImportResult> {

        @Override
        public ImportResult doExecute() {
            ImportResult importResult = PigFacade.importPncEntities();
            PigContext.get().setPncImportResult(importResult);
            return importResult;
        }
    }

    @CommandDefinition(name = "build", description = "Build")
    public class Build extends PigCommand<Map<String, PncBuild>> {

//        TODO: it is doable to do this step with build group id only, add this functionality
//        @Option(shortName = 'b',
//                overrideRequired = true,
//                description = "id of the group to build. Exactly one of {config, build-group} has to be provided")
//        private Integer buildGroupId;

        @Option(shortName = TEMP_BUILD_SHORT,
                name = TEMP_BUILD,
                overrideRequired = true,
                defaultValue = TEMP_BUILD_DEFAULT,
                description = TEMP_BUILD_DESC)
        private boolean tempBuild;

        @Option(name = TEMP_BUILD_TIME_STAMP,
                overrideRequired = true,
                defaultValue = TEMP_BUILD_TIME_STAMP_DEFAULT,
                description = TEMP_BUILD_TIME_STAMP_DESC)
        private boolean tempBuildTS;

        @Option(name = REBUILD_MODE,
                overrideRequired = true,
                defaultValue = REBUILD_MODE_DEFAULT,
                description = REBUILD_MODE_DESC)
        private String rebuildMode;

        @Override
        public Map<String, PncBuild> doExecute() {
            Map<String, PncBuild> builds = PigFacade.build(tempBuild, tempBuildTS, rebuildMode);
            PigContext.get().setBuilds(builds);
            PigContext.get().storeContext();
            return builds;
        }
    }

    @CommandDefinition(name = "repo", description = "GenerateRepository")
    public class GenerateRepository extends PigCommand<RepositoryData> {
        @Option(name = REMOVE_M2_DUPLICATES,
                overrideRequired = true,
                defaultValue = REMOVE_M2_DUPLICATES_DEFAULT,
                description = REMOVE_M2_DUPLICATES_DESC)
        private boolean removeGeneratedM2Dups;


        @Override
        public RepositoryData doExecute() {
            RepositoryData result = PigFacade.generateRepo(removeGeneratedM2Dups);
            PigContext.get().setRepositoryData(result);
            return result;
        }
    }

    @CommandDefinition(name = "licenses", description = "GenerateLicenses")
    public class GenerateLicenses extends PigCommand<String> {

        @Override
        public String doExecute() {
            PigFacade.generateLicenses();
            return "Licenses generated successfully"; //TODO: better output
        }
    }

    @CommandDefinition(name = "javadocs", description = "GenerateJavadocs")
    public class GenerateJavadocs extends PigCommand<String> {

        @Override
        public String doExecute() {
            PigFacade.generateJavadoc();
            return "Javadocs generated successfully"; // TODO: better output
        }
    }

    @CommandDefinition(name = "sources", description = "GenerateSources")
    public class GenerateSources extends PigCommand<String> {

        @Override
        public String doExecute() {
            PigFacade.generateSources();
            return "Sources gathered successfully"; // TODO: better output
        }
    }

    @CommandDefinition(name = "shared-content", description = "GenerateSharedContentAnalysis")
    public class GenerateSharedContentAnalysis extends PigCommand<String> {

        @Override
        public String doExecute() {
            PigFacade.prepareSharedContentAnalysis();
            return "Shared content analysis document prepared successfully"; // TODO: better output
        }
    }

    @CommandDefinition(name = "docs", description = "GenerateDocuments")
    public class GenerateDocuments extends PigCommand<String> {

        @Override
        public String doExecute() {
            PigFacade.generateDocuments();
            return "Documents generated successfully"; // TODO: better output
        }
    }


    @CommandDefinition(name = "scripts", description = "GenerateScripts")
    public class GenerateScripts extends PigCommand<String> {

        @Override
        public String doExecute() {
            PigFacade.generateScripts();
            return "Scripts generated successfully"; // TODO: better output
        }
    }

    @CommandDefinition(name = "addons", description = "Addons")
    public class TriggerAddOns extends PigCommand<String> {

        @Override
        public String doExecute() {
            PigFacade.triggerAddOns();
            return "Add-ons executed successfully";
        }
    }

}
