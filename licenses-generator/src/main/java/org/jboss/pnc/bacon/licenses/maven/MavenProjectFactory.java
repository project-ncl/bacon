/*
 * Copyright 2016-2017 Red Hat, Inc, and individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.pnc.bacon.licenses.maven;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.project.ProjectBuildingResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
public class MavenProjectFactory {

    private final Logger logger = LoggerFactory.getLogger(MavenProjectFactory.class);

    private final ProjectBuilder projectBuilder;

    private final ProjectBuildingRequestFactory projectBuildingRequestFactory;

    public MavenProjectFactory(
            ProjectBuilder projectBuilder,
            ProjectBuildingRequestFactory projectBuildingRequestFactory) {
        this.projectBuilder = projectBuilder;
        this.projectBuildingRequestFactory = projectBuildingRequestFactory;
    }

    public Optional<MavenProject> getMavenProject(Artifact artifact, boolean resolveDependencies) {
        ProjectBuildingRequest request = projectBuildingRequestFactory.getProjectBuildingRequest();
        request.setResolveDependencies(resolveDependencies);

        try {
            ProjectBuildingResult result = projectBuilder.build(artifact, request);
            return Optional.ofNullable(result.getProject());
        } catch (ProjectBuildingException e) {
            logger.warn("Failed to get maven project for " + artifact, e);
            return Optional.empty();
        }
    }

    public List<MavenProject> getMavenProjects(File pom, boolean resolveDependencies) {
        ProjectBuildingRequest request = projectBuildingRequestFactory.getProjectBuildingRequest();
        request.setResolveDependencies(resolveDependencies);

        List<MavenProject> interimMavenProjects;

        try {
            interimMavenProjects = projectBuilder.build(Collections.singletonList(pom), true, request)
                    .parallelStream()
                    .map(ProjectBuildingResult::getProject)
                    .collect(Collectors.toList());
        } catch (ProjectBuildingException e) {
            logger.warn("Failed to get maven project for " + pom.getName(), e);
            return Collections.emptyList();
        }

        if (resolveDependencies) {
            // This is needed as the build above doesn't resolve dependencies
            return interimMavenProjects.parallelStream()
                    .map(p -> resolveMavenProject(p, request))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.toList());
        }

        return interimMavenProjects;
    }

    private Optional<MavenProject> resolveMavenProject(MavenProject mavenProject, ProjectBuildingRequest request) {
        try {
            ProjectBuildingResult result = projectBuilder.build(mavenProject.getFile(), request);
            return Optional.ofNullable(result.getProject());
        } catch (ProjectBuildingException e) {
            logger.warn("Failed to resolve maven project", e);
            return Optional.empty();
        }
    }

}
