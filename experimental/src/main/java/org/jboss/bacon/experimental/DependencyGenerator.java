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
package org.jboss.bacon.experimental;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jboss.bacon.experimental.cli.DependencyGeneratorCommand;
import org.jboss.bacon.experimental.impl.config.GeneratorConfig;
import org.jboss.bacon.experimental.impl.dependencies.DependencyResolver;
import org.jboss.bacon.experimental.impl.dependencies.DependencyResult;
import org.jboss.bacon.experimental.impl.dependencies.Project;
import org.jboss.bacon.experimental.impl.generator.BuildConfigGenerator;
import org.jboss.bacon.experimental.impl.projectfinder.FoundProjects;
import org.jboss.bacon.experimental.impl.projectfinder.ProjectFinder;
import org.jboss.pnc.bacon.common.ObjectHelper;
import org.jboss.pnc.bacon.common.exception.FatalException;
import org.jboss.pnc.bacon.pig.impl.config.BuildConfig;
import org.jboss.pnc.bacon.pig.impl.config.PigConfiguration;
import picocli.CommandLine;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

@CommandLine.Command(
        name = "dependency-generator",
        description = "Automated dependency Build Config generator",
        subcommands = { DependencyGenerator.Generate.class, DependencyGenerator.DepTree.class })
@Slf4j
public class DependencyGenerator {

    @CommandLine.Command(name = "dependency-tree", description = "Outputs the analyzed dependency tree")
    public static class DepTree extends DependencyGeneratorCommand {

        @Override
        public void run() {
            GeneratorConfig config = loadConfig();
            DependencyResolver dependencyResolver = new DependencyResolver(config.getDependencyResolutionConfig());
            DependencyResult dependencies;
            dependencies = dependencyResolver.resolve(projectDir, dominoConfig);
            print(dependencies);
        }

        private void print(DependencyResult dependencies) {
            Set<Project> entered = new HashSet<>();
            Stack<StackElem> stack = new Stack<>();
            for (Project topLevelProject : dependencies.getTopLevelProjects()) {
                stack.push(new StackElem(topLevelProject, "# "));
            }
            while (!stack.isEmpty()) {
                StackElem stackElem = stack.pop();
                System.out.println(stackElem.prefix + stackElem.project.getFirstGAV());
                if (!entered.contains(stackElem.project)) {
                    entered.add(stackElem.project);
                    for (Project dependency : stackElem.project.getDependencies()) {
                        stack.push(new StackElem(dependency, stackElem.prefix + " "));
                    }
                }
            }
        }

        @AllArgsConstructor
        private static class StackElem {
            Project project;
            String prefix;
        }
    }

    @CommandLine.Command(name = "generate", description = "Generates build config")
    public static class Generate extends DependencyGeneratorCommand {

        @Override
        public void run() {
            try {
                ObjectHelper.print(getJsonOutput(), generatePigConfig());
            } catch (JsonProcessingException e) {
                throw new FatalException("Caught exception " + e.getMessage(), e);
            }
        }

        private PigConfiguration generatePigConfig() {
            // Load config file
            GeneratorConfig config = loadConfig();
            // Initialize working classes
            DependencyResolver dependencyResolver = new DependencyResolver(config.getDependencyResolutionConfig());
            ProjectFinder projectFinder = new ProjectFinder();
            BuildConfigGenerator buildConfigGenerator = new BuildConfigGenerator(
                    config.getBuildConfigGeneratorConfig());
            // Analyze dependencies
            DependencyResult dependencies;
            dependencies = dependencyResolver.resolve(projectDir, dominoConfig);
            log.info("Analyzed project and found {} dependencies", dependencies.getCount());
            // Generate BCs
            // Find pre-existing BC in PNC
            FoundProjects foundProjects = projectFinder.findProjects(dependencies);
            // Use correct strategy to generate BC
            List<BuildConfig> buildConfigs = buildConfigGenerator.generateConfigs(dependencies, foundProjects);
            // Generate build-config.yaml
            PigConfiguration template = config.getBuildConfigGeneratorConfig().getPigTemplate();
            template.setBuilds(buildConfigs);
            return template;
        }

    }

}
