/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017 Red Hat, Inc., and individual contributors
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

package org.jboss.pnc.bacon.pig.impl.repo;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.jboss.pnc.bacon.pig.impl.PigContext;
import org.jboss.pnc.bacon.pig.impl.config.PigConfiguration;
import org.jboss.pnc.bacon.pig.impl.config.RepoGenerationData;
import org.jboss.pnc.bacon.pig.impl.pnc.PncBuild;
import org.jboss.pnc.bacon.pig.impl.utils.GAV;
import org.jboss.pnc.bacon.pig.impl.utils.OSCommandExecutor;
import org.jboss.pnc.bacon.pig.impl.utils.ResourceUtils;
import org.jboss.pnc.bacon.pig.impl.utils.XmlUtils;
import org.jboss.pnc.bacon.pig.impl.utils.indy.Indy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.isEmpty;

/**
 * TODO: replace with programmatic maven api to remove OSCommandExecutor? TODO: CON: would mean that users have to stick
 * to the same maven version
 *
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com <br>
 *         Date: 7/25/17
 */
public class RepoBuilder {
    private static final Logger log = LoggerFactory.getLogger(RepoBuilder.class);

    private static final String POM_TEMPLATE_LOCATION = "/pom-template.xml";
    private static final String ENCODING = "UTF-8";
    private final PigConfiguration pigConfiguration;
    private final boolean removeGeneratedM2Dups;
    private final boolean tempBuild;
    private final RepoGenerationData repoGeneration;
    private final Map<String, PncBuild> builds;
    private final String additionalRepo;
    String topLevelDirectoryName;
    Path configurationDirectory;

    public RepoBuilder(
            PigConfiguration pigConfiguration,
            String additionalRepo,
            Path configurationDirectory,
            Map<String, PncBuild> builds,
            boolean removeGeneratedM2Dups) {
        this.pigConfiguration = pigConfiguration;
        this.additionalRepo = additionalRepo;
        this.builds = builds;
        this.configurationDirectory = configurationDirectory;
        this.removeGeneratedM2Dups = removeGeneratedM2Dups;

        repoGeneration = pigConfiguration.getFlow().getRepositoryGeneration();
        tempBuild = PigContext.get().isTempBuild();
        topLevelDirectoryName = pigConfiguration.getTopLevelDirectoryPrefix() + "maven-repository";
    }

    public void build(File bomFile, File repoParentDir, Predicate<GAV> artifactSelector) {
        try {
            createAndBuildProject(bomFile, repoParentDir, artifactSelector);
            RepositoryUtils.removeIrrelevantFiles(repoParentDir);
            if (removeGeneratedM2Dups) {
                RepositoryUtils.keepOnlyLatestRedHatArtifacts(repoParentDir);
            }
        } catch (IOException e) {
            throw new RuntimeException("Unable to build pom", e);
        }
    }

    private void createAndBuildProject(File bomFile, File repoParentDir, Predicate<GAV> artifactSelector)
            throws IOException {
        File projectLocation = createProject(bomFile, artifactSelector);
        String indySettings;
        if (tempBuild) {
            indySettings = "/indy-temp-settings.xml";
        } else {
            indySettings = "/indy-settings.xml";
        }
        String settingsXml = ResourceUtils
                .extractToTmpFileWithFiltering(
                        indySettings,
                        "settings",
                        ".xml",
                        settingsProps(tempBuild, additionalRepo))
                .getAbsolutePath();

        File repoDir = new File(repoParentDir, RepoDescriptor.MAVEN_REPOSITORY);

        buildProjectWithOverriddenM2(projectLocation, repoDir, settingsXml);
    }

    private Properties settingsProps(boolean tempBuild, String additionalRepo) {
        Properties result = new Properties();
        if (tempBuild && additionalRepo != null) {
            String repoDef = "--> <repository>\n" + "          <id>additional</id>\n" + "          <url>"
                    + additionalRepo + "</url>\n" + "        </repository> <!--";
            result.put("ADDITIONAL_REPOS", repoDef);

            String pluginRepoDef = "--> <pluginRepository>\n" + "          <id>additional-plugins</id>\n"
                    + "          <url>" + additionalRepo + "</url>\n" + "        </pluginRepository> <!--";
            result.put("ADDITIONAL_PLUGIN_REPOS", pluginRepoDef);
        }
        result.put("INDY_URL", Indy.getIndyUrl());
        result.put("INDY_TMP_URL", Indy.getIndyTempUrl());
        return result;
    }

    private void buildProjectWithOverriddenM2(File projectLocation, File repoDir, String settingsXml) {
        log.debug("Building the project in {} with overwritten local repository", projectLocation.getAbsolutePath());
        boolean noFailure = true;
        String cmd = pigConfiguration.getFlow().getRepositoryGeneration().getBuildScript();
        if (cmd == null || cmd.isEmpty()) {
            // Use a default mvn command on project
            cmd = "mvn clean package -B";
        }
        repoDir.mkdirs();
        String baseCmd = cmd + " -s %s -Dmaven.repo.local=%s";
        String command = String.format(baseCmd, settingsXml, repoDir.getAbsolutePath());
        List<String> output = OSCommandExecutor.runCommandIn(command, projectLocation.toPath());
        if (output.stream().anyMatch(line -> line.contains("BUILD SUCCESS"))) {
            if (pigConfiguration.getFlow().getRepositoryGeneration().isIncludeJavadoc()) {
                log.debug("Running project again to include Javadocs");
                command = String.format(baseCmd + " -Dclassifier=javadoc", settingsXml, repoDir.getAbsolutePath());
                output = OSCommandExecutor.runCommandIn(command, projectLocation.toPath());
                if (output.stream().noneMatch(line -> line.contains("BUILD SUCCESS"))) {
                    noFailure = false;
                }
            }
        } else {
            noFailure = false;
        }

        if (noFailure) {
            log.debug("Build finished successfully");
        } else {
            if (log.isErrorEnabled()) {
                log.error("Build failed");
                log.error("Build of the generated project failed");
                log.error("Build log: ");
                log.error(StringUtils.join(output, System.lineSeparator()));
            }

            throw new RuntimeException("Build of the generated project failed");
        }
    }

    protected File createProject(File bomFile, Predicate<GAV> artifactSelector) throws IOException {
        log.debug("Generating a project with all libraries from BOM as dependencies");
        String dependencies = extractRedhatDependencies(bomFile, artifactSelector);
        String bomVersion = XmlUtils.extract(bomFile, "/project/version").getContent();

        if (bomVersion == null) {
            bomVersion = XmlUtils.extract(bomFile, "/project/parent/version").getContent();
        }

        if (bomVersion == null) {
            throw new RuntimeException("Failed to find BOM version in " + bomFile.getAbsolutePath());
        }

        String bomGroupId = repoGeneration.getBomGroupId();
        String artifactId = repoGeneration.getBomArtifactId();

        File projectDirectory = org.jboss.pnc.bacon.pig.impl.utils.FileUtils.mkTempDir("helper-project");
        Path repoTemplate = configurationDirectory.resolve("repo-project");
        if (repoTemplate.toFile().isDirectory()) {
            FileUtils.copyDirectory(repoTemplate.toFile(), projectDirectory);
        }
        String pomContent = ResourceUtils.getOverridableResource(POM_TEMPLATE_LOCATION, projectDirectory.toPath())
                .replace("<bom-contents/>", dependencies)
                .replace("<bom-group-id/>", bomGroupId)
                .replace("<bom-artifact-id/>", artifactId)
                .replace("<bom-version/>", bomVersion);

        Map<String, String> customParameters = repoGeneration.getParameters();
        if (customParameters != null) {
            for (Map.Entry<String, String> param : customParameters.entrySet()) {
                String paramValue = evaluate(param.getValue());
                pomContent = pomContent.replace(String.format("<%s/>", param.getKey()), paramValue);
            }

        }

        File pom = new File(projectDirectory, "pom.xml");
        FileUtils.write(pom, pomContent, ENCODING);

        return projectDirectory;
    }

    private String evaluate(String value) {
        if (isEmpty(value)) {
            return value;
        }

        if (ArtifactVersion.prefix.startsWith(ArtifactVersion.prefix)) {
            return ArtifactVersion.get(value, repoGeneration.getSourceBuild(), builds);
        }

        return value;
    }

    private String extractRedhatDependencies(File bomFile, Predicate<GAV> artifactSelector) {
        Map<String, String> properties = XmlUtils.getProperties(bomFile);

        List<Node> dependencyNodes = XmlUtils
                .listNodes(bomFile, "/project/dependencyManagement/dependencies/dependency");

        return dependencyNodes.stream()
                .map(Element.class::cast)
                .map(element -> GAV.fromXml(element, properties))
                .filter(gav -> gav.getVersion().contains("redhat"))
                .filter(artifactSelector)
                .filter(gav -> !repoGeneration.getIgnored().contains(gav.getArtifactId()))
                .map(GAV::asBomXmlDependency)
                .collect(Collectors.joining("\n"));
    }
}
