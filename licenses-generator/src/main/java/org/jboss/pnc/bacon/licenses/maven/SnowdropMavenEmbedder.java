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
import java.util.Properties;

import org.apache.maven.DefaultMaven;
import org.apache.maven.Maven;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.DefaultMavenExecutionResult;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequestPopulator;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.plugin.LegacySupport;
import org.apache.maven.repository.RepositorySystem;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.building.DefaultSettingsBuildingRequest;
import org.apache.maven.settings.building.SettingsBuilder;
import org.codehaus.plexus.DefaultContainerConfiguration;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.classworlds.ClassWorld;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.eclipse.aether.RepositorySystemSession;

/**
 * Minimal Maven embedder used by the licenses generator.
 *
 * <p>
 * This class intentionally uses Apache Maven's own embedder/core APIs directly, instead of the Jenkins
 * {@code lib-jenkins-maven-embedder} wrapper. The licenses generator only needs a Plexus container, a legacy local
 * repository, remote repository creation and a repository system session for
 * {@link org.apache.maven.project.ProjectBuilder}.
 * Keeping this small adapter local avoids resolving Jenkins-specific artifacts during the Bacon build.
 * </p>
 *
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
public class SnowdropMavenEmbedder {

    private static final String DEFAULT_LOCAL_REPOSITORY_ID = "local";

    private static final String DEFAULT_REPOSITORY_LAYOUT = "default";

    private static final String DEFAULT_UPDATE_POLICY = ArtifactRepositoryPolicy.UPDATE_POLICY_ALWAYS;

    private static final String DEFAULT_CHECKSUM_POLICY = ArtifactRepositoryPolicy.CHECKSUM_POLICY_WARN;

    private final PlexusContainer plexusContainer;

    private final MavenExecutionRequest mavenExecutionRequest;

    public SnowdropMavenEmbedder() throws Exception {
        this.plexusContainer = createPlexusContainer();
        this.mavenExecutionRequest = createMavenExecutionRequest();
        initialiseLegacySupport();
    }

    public PlexusContainer getPlexusContainer() {
        return plexusContainer;
    }

    public RepositorySystemSession buildRepositorySystemSession() throws Exception {
        DefaultMaven defaultMaven = (DefaultMaven) plexusContainer.lookup(Maven.class);
        return defaultMaven.newRepositorySession(mavenExecutionRequest);
    }

    public ArtifactRepository getLocalRepository() throws ComponentLookupException {
        String localRepositoryPath = System.getProperty("maven.repo.local");

        if (isBlank(localRepositoryPath)) {
            try {
                localRepositoryPath = getSettings().getLocalRepository();
            } catch (Exception e) {
                throw new RuntimeException("Failed to read Maven settings", e);
            }
        }

        if (isBlank(localRepositoryPath)) {
            localRepositoryPath = new File(System.getProperty("user.home"), ".m2/repository").getAbsolutePath();
        }

        return createLocalRepository(localRepositoryPath, DEFAULT_LOCAL_REPOSITORY_ID);
    }

    public ArtifactRepository createLocalRepository(String path, String id) throws ComponentLookupException {
        String url = path.startsWith("file:") ? path : "file://" + path;
        return createRepository(url, id);
    }

    public ArtifactRepository createRepository(String url, String id) throws ComponentLookupException {
        ArtifactRepositoryPolicy releases = new ArtifactRepositoryPolicy(
                true,
                DEFAULT_UPDATE_POLICY,
                DEFAULT_CHECKSUM_POLICY);
        ArtifactRepositoryPolicy snapshots = new ArtifactRepositoryPolicy(
                true,
                DEFAULT_UPDATE_POLICY,
                DEFAULT_CHECKSUM_POLICY);

        RepositorySystem repositorySystem = plexusContainer.lookup(RepositorySystem.class);
        ArtifactRepositoryLayout layout = plexusContainer.lookup(
                ArtifactRepositoryLayout.class,
                DEFAULT_REPOSITORY_LAYOUT);

        return repositorySystem.createArtifactRepository(id, url, layout, snapshots, releases);
    }

    private PlexusContainer createPlexusContainer() throws Exception {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        ClassWorld classWorld = new ClassWorld();
        ClassRealm realm = new ClassRealm(classWorld, "maven", classLoader);

        DefaultContainerConfiguration configuration = new DefaultContainerConfiguration();
        configuration.setRealm(realm);
        configuration.setClassWorld(classWorld);
        configuration.setAutoWiring(false);
        configuration.setClassPathScanning("index");
        configuration.setComponentVisibility("realm");

        return new DefaultPlexusContainer(configuration);
    }

    private MavenExecutionRequest createMavenExecutionRequest() throws Exception {
        MavenExecutionRequest request = new DefaultMavenExecutionRequest();

        Settings settings = getSettings();
        MavenExecutionRequestPopulator populator = plexusContainer.lookup(MavenExecutionRequestPopulator.class);
        populator.populateFromSettings(request, settings);
        populator.populateDefaults(request);

        ArtifactRepository localRepository = getLocalRepository();
        request.setLocalRepository(localRepository);
        request.setLocalRepositoryPath(localRepository.getBasedir());
        request.setOffline(false);
        request.setUpdateSnapshots(false);
        request.setCacheTransferError(true);
        request.setCacheNotFound(true);
        request.setInteractiveMode(false);
        request.setSystemProperties(System.getProperties());
        request.setUserProperties(new Properties());

        request.getProjectBuildingRequest().setProcessPlugins(false);
        request.getProjectBuildingRequest().setValidationLevel(ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL);

        return request;
    }

    private Settings getSettings() throws Exception {
        DefaultSettingsBuildingRequest request = new DefaultSettingsBuildingRequest();
        request.setSystemProperties(System.getProperties());
        request.setUserProperties(new Properties());

        File userSettings = new File(System.getProperty("user.home"), ".m2/settings.xml");
        if (userSettings.isFile()) {
            request.setUserSettingsFile(userSettings);
        }

        String mavenHome = System.getProperty("maven.home");
        if (!isBlank(mavenHome)) {
            File globalSettings = new File(mavenHome, "conf/settings.xml");
            if (globalSettings.isFile()) {
                request.setGlobalSettingsFile(globalSettings);
            }
        }

        return plexusContainer.lookup(SettingsBuilder.class).build(request).getEffectiveSettings();
    }

    private void initialiseLegacySupport() throws Exception {
        RepositorySystemSession repositorySystemSession = buildRepositorySystemSession();
        MavenSession mavenSession = new MavenSession(
                plexusContainer,
                repositorySystemSession,
                mavenExecutionRequest,
                new DefaultMavenExecutionResult());
        plexusContainer.lookup(LegacySupport.class).setSession(mavenSession);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
