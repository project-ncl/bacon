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

import java.util.List;
import java.util.stream.Collectors;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.ProjectBuildingRequest;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.jboss.pnc.bacon.licenses.properties.GeneratorProperties;

/**
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
public class ProjectBuildingRequestFactory {

    private final GeneratorProperties properties;

    private final SnowdropMavenEmbedder maven;

    public ProjectBuildingRequestFactory(GeneratorProperties properties, SnowdropMavenEmbedder maven) {
        this.properties = properties;
        this.maven = maven;
    }

    public ProjectBuildingRequest getProjectBuildingRequest() {
        try {
            DefaultProjectBuildingRequest request = new DefaultProjectBuildingRequest();
            request.setLocalRepository(maven.getLocalRepository());
            request.setRemoteRepositories(getRepositories());
            request.setResolveDependencies(true);
            request.setRepositorySession(maven.buildRepositorySystemSession());
            request.setSystemProperties(System.getProperties());
            request.setProcessPlugins(false);
            request.setValidationLevel(ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL);

            return request;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create project building request", e);
        }
    }

    private List<ArtifactRepository> getRepositories() {
        return properties.getRepositories().entrySet().stream().map(entry -> {
            try {
                return maven.createRepository(entry.getValue(), entry.getKey());
            } catch (ComponentLookupException e) {
                throw new RuntimeException("Failed to initialise repository", e);
            }
        }).collect(Collectors.toList());
    }

}
