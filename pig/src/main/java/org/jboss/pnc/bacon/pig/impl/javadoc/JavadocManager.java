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
package org.jboss.pnc.bacon.pig.impl.javadoc;

import lombok.Getter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.commonjava.maven.ext.cli.Cli;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.jboss.pnc.bacon.pig.impl.PigContext;
import org.jboss.pnc.bacon.pig.impl.common.DeliverableManager;
import org.jboss.pnc.bacon.pig.impl.config.GenerationData;
import org.jboss.pnc.bacon.pig.impl.config.JavadocGenerationData;
import org.jboss.pnc.bacon.pig.impl.config.JavadocGenerationStrategy;
import org.jboss.pnc.bacon.pig.impl.config.PigConfiguration;
import org.jboss.pnc.bacon.pig.impl.documents.Deliverables;
import org.jboss.pnc.bacon.pig.impl.pnc.ArtifactWrapper;
import org.jboss.pnc.bacon.pig.impl.pnc.PncBuild;
import org.jboss.pnc.bacon.pig.impl.utils.FileUtils;
import org.jboss.pnc.bacon.pig.impl.utils.GAV;
import org.jboss.pnc.bacon.pig.impl.utils.ResourceUtils;
import org.jboss.pnc.bacon.pig.impl.utils.pom.Dependency;
import org.jboss.pnc.bacon.pig.impl.utils.pom.Profile;
import org.jboss.pnc.bacon.pig.impl.utils.pom.Project;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com <br>
 *         Date: 12/5/17
 */
public class JavadocManager extends DeliverableManager<GenerationData<?>, Void> {
    private static final Logger log = LoggerFactory.getLogger(JavadocManager.class);

    private static final String project_gid = "org.jboss.prod";
    private static final String project_aid = "pfg-javadoc-dep-profile-injection";
    private static final String project_version = "1.0.0";

    @Getter
    private final JavadocGenerationData generationData;
    private final boolean tempBuild;

    private String generationProject;
    private List<String> sourceBuilds;
    private String settingsXml;
    private File temporaryDestination;
    private File localRepo;
    private File topLevelDirectory;
    private File archiveFile;
    private String scmRevision;

    public JavadocManager(
            PigConfiguration pigConfiguration,
            String releasePath,
            Deliverables deliverables,
            Map<String, PncBuild> builds) {
        super(pigConfiguration, releasePath, deliverables, builds);
        tempBuild = PigContext.get().isTempBuild();
        generationData = pigConfiguration.getFlow().getJavadocGeneration();
    }

    public void prepare() {
        JavadocGenerationStrategy strategy = generationData.getStrategy();
        switch (strategy) {
            case DOWNLOAD:
                downloadAndRepackage();
                break;
            case GENERATE:
                if (checkRequired()) {
                    generate();
                }
                break;
            case IGNORE:
                log.info("Ignoring javadoc zip generation");
                deliverables.setJavadocZipName(null);
                break;
            default:
                throw new IllegalStateException("Unsupported javadoc generation strategy: " + strategy);
        }
    }

    @Override
    protected void repackage(File contentsDirectory, File targetTopLevelDirectory) {
        Stream.of(contentsDirectory.listFiles()).forEach(file -> FileUtils.copy(file, targetTopLevelDirectory));
    }

    @Override
    protected String getTargetTopLevelDirectoryName() {
        return pigConfiguration.getTopLevelDirectoryPrefix() + "javadoc";
    }

    @Override
    protected Path getTargetZipPath() {
        return Paths.get(releasePath + deliverables.getJavadocZipName());
    }

    private boolean checkRequired() {
        boolean ret = true;
        if (generationData.getSourceArtifact() == null || generationData.getSourceArtifact().isEmpty()) {
            ret = false;
            log.error("Invalid Javadoc generation yaml - 'sourceArtifact' required");
        }
        if (generationData.getGenerationProject() == null || generationData.getGenerationProject().isEmpty()) {
            log.error("Invalid Javadoc generation yaml - 'generationProject' required");
            ret = false;
        }
        return ret;
    }

    private void init() {
        this.temporaryDestination = FileUtils.mkTempDir("javadoc");
        if (this.tempBuild) {
            this.settingsXml = ResourceUtils.extractToTmpFile("/indy-temp-settings.xml", "settings", ".xml")
                    .getAbsolutePath();

        } else {
            this.settingsXml = ResourceUtils.extractToTmpFile("/indy-settings.xml", "settings", ".xml")
                    .getAbsolutePath();
        }
        this.localRepo = new File(temporaryDestination + File.separator + "localRepo");
        this.localRepo.mkdir();
        this.topLevelDirectory = new File(temporaryDestination, getTargetTopLevelDirectoryName());
        this.archiveFile = getTargetZipPath().toFile();

        this.generationProject = generationData.getGenerationProject();
        this.sourceBuilds = generationData.getSourceBuilds();
        if (this.sourceBuilds == null || this.sourceBuilds.isEmpty()) {
            this.sourceBuilds = builds.values().stream().map(PncBuild::getName).collect(Collectors.toList());
        }
        this.scmRevision = generationData.getScmRevision();
    }

    private Collection<GAV> findSourceBuilds() {
        Collection<GAV> srcBuilds = builds.values()
                .stream()
                .filter(build -> sourceBuilds.contains(build.getName()))
                .map(PncBuild::getBuiltArtifacts)
                .flatMap(Collection::stream)
                .map(ArtifactWrapper::toGAV)
                .filter(g -> g.getClassifier() != null && g.getClassifier().equals("sources"))
                .collect(Collectors.toList());
        return srcBuilds;
    }

    private boolean cloneProject() {
        Git git = null;
        try {
            log.debug("Cloning " + generationProject + " into " + topLevelDirectory);
            git = Git.cloneRepository().setURI(generationProject).setDirectory(topLevelDirectory).call();
            if (scmRevision != null && !scmRevision.isEmpty()) {
                log.debug("Checkout version " + scmRevision);
                git.checkout().setName(scmRevision).call();
            }
        } catch (GitAPIException e) {
            log.error("Exception occurred while cloning repo - {}", e.getMessage());
            return false;
        }
        return true;
    }

    private boolean addImportBOM(Profile profile) {
        Dependency dep;
        PncBuild build = getBuild(generationData.getImportBom());
        if (build != null) {
            List<ArtifactWrapper> artifacts = build.getBuiltArtifacts();
            if (artifacts != null && !artifacts.isEmpty()) {
                GAV tmp = artifacts.get(0).toGAV();
                // Get the first artifact and create the dep on the pom
                dep = new Dependency();
                dep.setArtifactId(tmp.getArtifactId());
                dep.setGroupId(tmp.getGroupId());
                dep.setVersion(tmp.getVersion());
                dep.setType("pom");
                dep.setScope("import");
                profile.getDependencyManagement().addDependency(dep);
            } else {
                log.error("Error no artifacts in build for 'importBom' {}", generationData.getImportBom());
                return false;
            }
        } else {
            log.error("Error no build found for 'importBom' {}", generationData.getImportBom());
            return false;
        }
        return true;
    }

    private void addSourceBuildsDeps(Profile profile, Collection<GAV> srcBuilds) {
        Dependency dep;
        for (GAV sb : srcBuilds) {
            dep = new Dependency();
            dep.setArtifactId(sb.getArtifactId());
            dep.setGroupId(sb.getGroupId());
            dep.setVersion(sb.getVersion());
            dep.setType("jar");
            dep.setClassifier(sb.getClassifier());
            profile.addDependency(dep);
        }
    }

    private boolean writeProject(Project project) {
        try {
            File dir = new File(
                    localRepo.getPath() + File.separator + project_gid.replace('.', File.separatorChar) + File.separator
                            + project_aid + File.separator + project_version);
            if (!dir.exists()) {
                if (!dir.mkdirs()) {
                    log.error("Error while creating directory {}", dir);
                    return false;
                }
            }
            File file = new File(dir.getPath() + File.separator + project_aid + "-" + project_version + ".pom");
            JAXBContext jaxbContext = JAXBContext.newInstance(Project.class);
            Marshaller jaxbMarshaller = jaxbContext.createMarshaller();

            // output pretty printed
            jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

            jaxbMarshaller.marshal(project, file);
        } catch (JAXBException e) {
            log.error("Error while creating profile pom", e);
            return false;
        }
        return true;
    }

    /**
     * Use PME to insert the profile into the cloned project and run with any extra custom PME parameters in the yaml
     * config
     *
     * @return
     */
    private boolean runPME() {
        PrintStream stdout = System.out;
        PrintStream outStream = null;
        String filePath = temporaryDestination.getPath() + File.separator + "pme-execution.log";
        try {
            outStream = new PrintStream(new File(filePath));
        } catch (Exception e) {
            log.error("Error PME run - cannot open {}", filePath);
            return false;
        }
        log.debug("Running PME to insert the dependencies into the project (see log {})", outStream);
        StringBuilder cmd = new StringBuilder(
                "-f " + topLevelDirectory.getPath() + File.separator + "pom.xml" + " -DprofileInjection=" + project_gid
                        + ":" + project_aid + ":" + project_version + " -s " + settingsXml + " -Dmaven.repo.local="
                        + localRepo + " -t");
        if (generationData.getAlignmentParameters() != null && !generationData.getAlignmentParameters().isEmpty()) {
            for (String parameter : generationData.getAlignmentParameters()) {
                cmd.append(" " + parameter);
            }
        }
        System.setOut(outStream);
        log.info("PME Command run [{}]", cmd.toString());
        if (new Cli().run(cmd.toString().split("\\s+")) != 0) {
            System.setOut(stdout);
            log.error("Error running PME see {}", filePath);
            dumpLog(filePath);
            return false;
        }
        System.setOut(stdout);
        return true;
    }

    /**
     * Create and deploy a profile with the dependencies from the GAV list add the importBom if specifies
     *
     * @param srcBuilds
     * @return
     */
    private boolean manipulateProject(Collection<GAV> srcBuilds) {
        Project project = new Project();
        project.setVersion(project_version);
        project.setArtifactId(project_aid);
        project.setGroupId(project_gid);
        Profile profile = new Profile();
        profile.setId("pfg-redhat-javadoc");
        project.getProfiles().addProfile(profile);
        addSourceBuildsDeps(profile, srcBuilds);

        if (!addImportBOM(profile)) {
            return false;
        } else {
            if (!writeProject(project)) {
                return false;
            } else {
                return runPME();
            }
        }
    }

    private void dumpLog(String filePath) {
        try {
            System.out.println(new String(Files.readAllBytes(Paths.get(filePath))));
        } catch (IOException e) {
            log.error("Unable to dump log {}", filePath, e);
        }
    }

    private boolean executeMavenBuild() {
        log.debug("Executing Javadoc generation maven project");
        String command = generationData.getBuildScript();
        Process process = null;
        if (command == null || command.isEmpty()) {
            // Use a default mvn command on project
            command = "mvn package -B";
        }
        // Add to the basic command the specific's needed to do the build locally
        File mavenRun = new File(temporaryDestination.getPath() + File.separator + "mvn-execution.log");
        command = command + " -Dmaven.repo.local=" + localRepo + " -s " + settingsXml + " -Ppfg-redhat-javadoc";
        log.debug("Running Javadoc project (see log {}) with [{}]", mavenRun, command);
        ProcessBuilder builder = new ProcessBuilder(command.split("\\s+")).directory(topLevelDirectory)
                .redirectOutput(mavenRun);
        try {
            process = builder.start();
            process.waitFor();
            if (process.exitValue() != 0) {
                log.error("Error while running Javadoc generation project [{}]", process.exitValue());
                dumpLog(mavenRun.getAbsolutePath());
                return false;
            }
        } catch (IOException e) {
            log.error("Unable to start build Javadoc generation project", e);
            dumpLog(mavenRun.getAbsolutePath());
            return false;
        } catch (InterruptedException e) {
            log.error("Javadoc generation build was Interrupted", e);
            dumpLog(mavenRun.getAbsolutePath());
            return false;
        }
        return true;
    }

    private void generate() {
        init();
        log.info("Generating Javadoc in {}", topLevelDirectory);
        // Lookup the builds listed and that have a -sources artifact, if
        // non provided then all builds in the build-config.yaml will be included
        Collection<GAV> srcBuilds = findSourceBuilds();
        if (srcBuilds != null) {
            // Clone the generation project
            if (cloneProject()) {
                // Add the source dependencies from the list gathered above
                if (manipulateProject(srcBuilds)) {
                    // Run the maven project to actually generate the javadoc zip
                    if (executeMavenBuild()) {
                        // Archive the generated source artifact to the release archive name
                        Pattern pattern = Pattern.compile(generationData.getSourceArtifact(), Pattern.CASE_INSENSITIVE);
                        List<File> files = (List<File>) org.apache.commons.io.FileUtils
                                .listFiles(temporaryDestination, TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE);
                        List<File> found = new ArrayList<>();
                        for (File file : files) {
                            if (pattern.matcher(file.getName()).matches()) {
                                found.add(file);
                            }
                        }

                        if (found.size() == 0) {
                            log.error("No generated files with pattern [{}] found", generationData.getSourceArtifact());
                            return;
                        }
                        if (found.size() > 1) {
                            log.error("More than 1 match for generated artifact found");
                            for (File file : found) {
                                log.error("  - {}", file.getName());
                            }
                            return;
                        }
                        try {
                            org.apache.commons.io.FileUtils.copyFile(found.get(0), archiveFile);
                        } catch (IOException e) {
                            log.error("Error wile copying [{}] copied to [{}]", found.get(0), archiveFile, e);
                        }
                        log.debug("Generated file [{}] copied to [{}]", found.get(0), archiveFile);
                    }
                }
            }
        }
    }
}
