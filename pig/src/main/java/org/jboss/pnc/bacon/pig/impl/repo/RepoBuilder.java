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
import org.jboss.pnc.bacon.pig.impl.config.Config;
import org.jboss.pnc.bacon.pig.impl.config.RepoGenerationData;
import org.jboss.pnc.bacon.pig.impl.utils.GAV;
import org.jboss.pnc.bacon.pig.impl.utils.OSCommandExecutor;
import org.jboss.pnc.bacon.pig.impl.utils.ResourceUtils;
import org.jboss.pnc.bacon.pig.impl.utils.XmlUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * TODO: replace with programmatic maven api to remove OSCommandExecutor?
 * TODO: CON: would mean that users have to stick to the same maven version
 *
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * <br>
 * Date: 7/25/17
 */
public class RepoBuilder {
    private static final Logger log = LoggerFactory.getLogger(RepoBuilder.class);

    private static final String POM_TEMPLATE_LOCATION = "/pom-template.xml";
    private static final String ENCODING = "UTF-8";
    private final Config config;
    private final boolean removeGeneratedM2Dups;
    String topLevelDirectoryName;
    Path configurationDirectory;


    public RepoBuilder(Config config,
                       Path configurationDirectory,
                       boolean removeGeneratedM2Dups) {
        this.config = config;
        topLevelDirectoryName = config.getTopLevelDirectoryPrefix() + "maven-repository";
        this.configurationDirectory = configurationDirectory;
        this.removeGeneratedM2Dups = removeGeneratedM2Dups;
    }


    public File build(File bomFile) {
        try {
            File projectLocation = createProject(bomFile);
            String settingsXml = ResourceUtils
                    .extractToTmpFile("/indy-settings.xml", "settings", ".xml")
                    .getAbsolutePath();
            File m2Repo = buildProjectWithOverriddenM2(projectLocation, settingsXml);
            RepositoryUtils.removeIrrelevantFiles(m2Repo);
            if (removeGeneratedM2Dups) {
              RepositoryUtils.keepOnlyLatestRedHatArtifacts(m2Repo);
            }
          return m2Repo;
        } catch (IOException e) {
            throw new RuntimeException("Unable to build pom", e);
        }
    }

    private File buildProjectWithOverriddenM2(File projectLocation, String settingsXml) {
        log.debug("Building the project in {} with overwritten local repository",
                projectLocation.getAbsolutePath());
        boolean noFailure = true;
        File repoWorkDir = org.jboss.pnc.bacon.pig.impl.utils.FileUtils.mkTempDir("repository");
        File repoParentDir = new File(repoWorkDir, topLevelDirectoryName);
        File repoDir = new File(repoParentDir, RepoDescriptor.MAVEN_REPOSITORY);
        repoDir.mkdirs();
        String baseCmd = "mvn clean package -B -s %s -Dmaven.repo.local=%s";
        String command = String.format(
                baseCmd,
                settingsXml,
                repoDir.getAbsolutePath()
        );
        List<String> output = OSCommandExecutor.runCommandIn(command, projectLocation.toPath());
        if (output.stream().anyMatch(line -> line.contains("BUILD SUCCESS"))) {
            if (config.getFlow().getRepositoryGeneration().getIncludeJavadoc()) {
                log.debug("Running project again to include Javadocs");
                command = String.format(
                        baseCmd + " -Dclassifier=javadoc",
                        settingsXml,
                        repoDir.getAbsolutePath());
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
            return repoParentDir;
        } else {
            log.error("Build failed");
            log.error("Build of the generated project failed");
            log.error("Build log: ");
            log.error(StringUtils.join(output, "\n"));
            throw new RuntimeException("Build of the generated project failed");
        }
    }

    protected File createProject(File bomFile) throws IOException {
        log.debug("Generating a project with all libraries from BOM as dependencies");
        String dependencies = extractRedhatDependencies(bomFile);
        String bomVersion = XmlUtils.extract(bomFile, "/project/version").getContent();

        RepoGenerationData repoGeneration = config.getFlow().getRepositoryGeneration();
        String bomGroupId = repoGeneration.getBomGroupId();
        String artifactId = repoGeneration.getBomArtifactId();

        File projectDirectory = org.jboss.pnc.bacon.pig.impl.utils.FileUtils.mkTempDir("helper-project");
        String pomContent = ResourceUtils.getOverridableResource(POM_TEMPLATE_LOCATION, configurationDirectory)
                .replace("<bom-contents/>", dependencies)
                .replace("<bom-group-id/>", bomGroupId)
                .replace("<bom-artifact-id/>", artifactId)
                .replace("<bom-version/>", bomVersion);


        File pom = new File(projectDirectory, "pom.xml");
        FileUtils.write(pom, pomContent, ENCODING);

        return projectDirectory;
    }

    static String extractRedhatDependencies(File bomFile) {
        Map<String, String> properties = XmlUtils.getProperties(bomFile);

        List<Node> dependencyNodes = XmlUtils.
                listNodes(bomFile, "/project/dependencyManagement/dependencies/dependency");

        return dependencyNodes.stream()
                .map(Element.class::cast)
                .map(element -> GAV.fromXml(element, properties))
                .filter(gav -> gav.getVersion().contains("redhat"))
                .map(GAV::asBomXmlDependency)
                .collect(Collectors.joining("\n"));
    }
}
