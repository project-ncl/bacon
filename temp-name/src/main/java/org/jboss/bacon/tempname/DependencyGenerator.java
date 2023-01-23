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
package org.jboss.bacon.tempname;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.jboss.bacon.tempname.impl.config.GeneratorConfig;
import org.jboss.bacon.tempname.impl.dependencies.DependencyResolver;
import org.jboss.bacon.tempname.impl.dependencies.DependencyResult;
import org.jboss.bacon.tempname.impl.generator.BuildConfigGenerator;
import org.jboss.bacon.tempname.impl.projectfinder.FoundProjects;
import org.jboss.bacon.tempname.impl.projectfinder.ProjectFinder;
import org.jboss.pnc.bacon.common.ObjectHelper;
import org.jboss.pnc.bacon.common.cli.JSONCommandHandler;
import org.jboss.pnc.bacon.common.exception.FatalException;
import org.jboss.pnc.bacon.pig.impl.config.BuildConfig;
import org.jboss.pnc.bacon.pig.impl.config.PigConfiguration;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import picocli.CommandLine;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;

@CommandLine.Command(
        name = "dependency-generator",
        description = "Automated dependency Build Config generator",
        subcommands = { DependencyGenerator.Generate.class })
@Slf4j
public class DependencyGenerator {

    @CommandLine.Command(name = "generate", description = "Generates build config")
    public static class Generate extends JSONCommandHandler implements Callable<Integer> {

        @CommandLine.Option(names = { "--project-dir" }, description = "Project directory")
        private Path projectDir;

        @CommandLine.Parameters(description = "Autobuilder configuration file")
        private Path config;

        @Override
        public Integer call() {
            try {
                ObjectHelper.print(getJsonOutput(), generatePigConfig());
            } catch (JsonProcessingException e) {
                throw new FatalException("Caught exception " + e.getMessage(), e);
            }
            return 0;
        }

        private GeneratorConfig loadConfig() {
            if (config == null) {
                throw new FatalException("You need to specify the configuration directory!");
            }

            Yaml yaml = new Yaml(new Constructor(GeneratorConfig.class));
            try (BufferedReader reader = Files.newBufferedReader(config)) {
                return yaml.load(reader);
            } catch (IOException e) {
                throw new FatalException("Unable to load config file", e);
            }
        }

        private PigConfiguration generatePigConfig() {
            // Load config file
            GeneratorConfig config = loadConfig();
            // Analyze dependencies
            DependencyResolver dependencyResolver = new DependencyResolver(config.getDependencyResolutionConfig());
            DependencyResult dependencies;
            if (projectDir != null) {
                dependencies = dependencyResolver.resolve(projectDir);
            } else {
                dependencies = dependencyResolver.resolve();
            }
            log.info("Analyzed project and found {} dependencies", dependencies.getCount());
            // Generate BCs
            // Find pre-existing BC in PNC
            ProjectFinder projectFinder = new ProjectFinder();
            FoundProjects foundProjects = projectFinder.findProjects(dependencies);
            // Use correct strategy to generate BC
            BuildConfigGenerator buildConfigGenerator = new BuildConfigGenerator(
                    config.getBuildConfigGeneratorConfig());
            List<BuildConfig> buildConfigs = buildConfigGenerator.generateConfigs(dependencies, foundProjects);
            // Generate build-config.yaml
            PigConfiguration template = config.getBuildConfigGeneratorConfig().getPigTemplate();
            template.setBuilds(buildConfigs);
            return template;
        }

    }

}
