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
package org.jboss.pnc.bacon.pig.impl;

import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.jboss.pnc.bacon.common.Constant.PIG_CONTEXT_DIR;
import static org.jboss.pnc.bacon.pig.impl.utils.HashUtils.hashDirectory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Map;

import org.jboss.pnc.bacon.pig.impl.config.PigConfiguration;
import org.jboss.pnc.bacon.pig.impl.documents.Deliverables;
import org.jboss.pnc.bacon.pig.impl.pnc.ImportResult;
import org.jboss.pnc.bacon.pig.impl.pnc.PncBuild;
import org.jboss.pnc.bacon.pig.impl.pnc.PncEntitiesImporter;
import org.jboss.pnc.bacon.pig.impl.repo.RepositoryData;
import org.jboss.pnc.bacon.pig.impl.utils.MilestoneNumberFinder;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * TODO: consider saving the latest reached state to not repeat the steps already performed
 *
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com <br>
 *         Date: 4/1/19
 */
public class PigContext {
    @java.lang.SuppressWarnings("all")
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PigContext.class);
    private static final ObjectMapper jsonMapper;

    static {
        jsonMapper = new ObjectMapper();
        jsonMapper.enable(SerializationFeature.INDENT_OUTPUT);
        jsonMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        jsonMapper.setVisibility(PropertyAccessor.GETTER, JsonAutoDetect.Visibility.NONE);
        jsonMapper.setVisibility(PropertyAccessor.IS_GETTER, JsonAutoDetect.Visibility.NONE);
        jsonMapper.registerModule(new JavaTimeModule());
    }

    private PigConfiguration pigConfiguration; // TODO merge config instead of setting it?
    private ImportResult pncImportResult;
    private Map<String, PncBuild> builds;
    private RepositoryData repositoryData;
    private Deliverables deliverables;
    private String targetPath;
    private String releaseDirName;
    private String releasePath;
    private String extrasPath;
    private String configSha;
    private boolean tempBuild;
    private String contextLocation;
    private String prefix;
    private String fullVersion; // version like 1.3.2.DR7
    private Map<String, Collection<String>> checksums;

    public void initConfig(Path configDir, String targetPath, String releaseStorageUrl, Map<String, String> overrides) {
        File configFile = configDir.resolve("build-config.yaml").toFile();
        if (configFile.exists()) {
            try (FileInputStream configStream = new FileInputStream(configFile)) {
                PigConfiguration loadedConfig = PigConfiguration.load(configStream, overrides);
                setPigConfiguration(loadedConfig, targetPath, releaseStorageUrl);
            } catch (IOException e) {
                throw new RuntimeException("Failed to read config file: " + configFile.getAbsolutePath(), e);
            }
        } else {
            throw new MisconfigurationException(
                    "The provided config file: " + configFile.getAbsolutePath() + " does not exist");
        }
    }

    private void setPigConfiguration(PigConfiguration pigConfiguration, String targetPath, String releaseStorageUrl) {
        this.pigConfiguration = pigConfiguration;
        if (releaseStorageUrl != null) {
            pigConfiguration.setReleaseStorageUrl(releaseStorageUrl);
        }
        this.targetPath = targetPath;
    }

    public void initFullVersion(boolean requireStorageUrl) {
        if (fullVersion != null) {
            return;
        }
        @SuppressWarnings("deprecation")
        String milestone = pigConfiguration.getMilestone();
        @SuppressWarnings("deprecation")
        String version = pigConfiguration.getVersion();
        if (milestone.contains("*") && requireStorageUrl) {
            String url = pigConfiguration.getReleaseStorageUrl();
            if (isEmpty(url)) {
                throw new RuntimeException(
                        "Auto-incremented milestone used but no releaseStorageUrl provided. "
                                + "Please either set the releaseStorageUrl in the build config yaml, by the product version or set the url by adding `--releaseStorageUrl=...` parameter");
            }
            milestone = MilestoneNumberFinder.getFirstUnused(url, version, milestone);
        } else if (milestone.contains("*")) {
            log.info("Loading latest product milestone from PNC.");
            try (PncEntitiesImporter importer = new PncEntitiesImporter()) {
                String milestoneFullVersion = importer.getLatestProductMilestoneFullVersion();
                if (!milestoneFullVersion.startsWith(version)) {
                    throw new RuntimeException(
                            String.format(
                                    "Milestone version retrieved by PNC (%s) differs from in the version part from the version defined in build-config.yaml (%s).",
                                    milestoneFullVersion,
                                    version));
                }
                milestone = milestoneFullVersion.replace(version + '.', "");
            }
        }
        setFullVersion(version + "." + milestone);
    }

    public void configureTargetDirectories() {
        if (deliverables == null) {
            String suffix = "";
            // for e.g, zip will become <releaseFile>-<fullVersion>-<suffix>-maven-repository.zip
            if (pigConfiguration.getOutputSuffix() != null && !pigConfiguration.getOutputSuffix().isBlank()) {
                suffix = "-" + pigConfiguration.getOutputSuffix();
            }
            prefix = String
                    .format("%s-%s%s", pigConfiguration.getOutputPrefixes().getReleaseFile(), fullVersion, suffix);
            deliverables = new Deliverables();
            deliverables.setRepositoryZipName(prefix + "-maven-repository.zip");
            deliverables.setNvrListName(prefix + "-nvr-list.txt");
        }
        String productPrefix = this.pigConfiguration.getProduct().prefix();
        releaseDirName = productPrefix + "-" + fullVersion;
        releasePath = targetPath + File.separator + releaseDirName + File.separator;
        File releaseDirectory = Paths.get(releasePath).toFile();
        if (!releaseDirectory.isDirectory()) {
            releaseDirectory.mkdirs();
        }
        releasePath = releaseDirectory.getAbsolutePath();
        releasePath = releasePath.endsWith(File.separator) ? releasePath : releasePath + File.separator;
        File extrasDirectory = Paths.get(releasePath, "extras").toFile();
        if (!extrasDirectory.isDirectory()) {
            extrasDirectory.mkdirs();
        }
        extrasPath = extrasDirectory.getAbsolutePath();
        extrasPath = extrasPath.endsWith(File.separator) ? extrasPath : extrasPath + File.separator;
    }

    public void storeContext() {
        try (OutputStream output = new FileOutputStream(contextLocation)) {
            jsonMapper.writerFor(PigContext.class).writeValue(output, this);
        } catch (IOException e) {
            throw new RuntimeException("failed to store PigContext", e);
        }
    }

    /*
     * STATICS
     */
    private static PigContext instance;

    public static PigContext get() {
        return instance;
    }

    public static PigContext init(
            boolean clean,
            Path configDir,
            String targetPath,
            String releaseStorageUrl,
            Map<String, String> overrides) {
        instance = readContext(clean, configDir);
        instance.initConfig(configDir, targetPath, releaseStorageUrl, overrides);
        return instance;
    }

    private static PigContext readContext(boolean clean, Path configDir) {
        String sha = hashDirectory(configDir, path -> {
            // normalize to remove any redundancies (unnecessary './' or '../' in the path)
            Path tempPath = path.normalize();
            // we ignore the top-level .bacon/pig-context.json and the content from the top-level target folder
            // if
            // present
            // They shouldn't be part of the hash generation since their content will change constantly but
            // shouldn't
            // contribute to the hash of the directory since their content doesn't affect the integrity of the
            // context
            return tempPath.startsWith(Paths.get(".bacon", "pig-context.json"))
                    || tempPath.startsWith(Paths.get("target"));
        });
        PigContext result;
        String ctxLocationEnv = System.getenv(PIG_CONTEXT_DIR);
        Path contextDir = ctxLocationEnv == null ? Paths.get(".bacon") : Paths.get(ctxLocationEnv);
        Path contextJson = contextDir.resolve("pig-context.json");
        if (!clean && Files.exists(contextJson)) {
            try (InputStream input = Files.newInputStream(contextJson)) {
                result = jsonMapper.readerFor(PigContext.class).readValue(input);
                if (!sha.equals(result.getConfigSha())) {
                    log.info("the configuration has been changed since the last run, using clean pig context");
                    result = new PigContext();
                }
            } catch (IOException e) {
                throw new RuntimeException("failed to read PigContext from " + contextDir.toAbsolutePath(), e);
            }
        } else {
            result = new PigContext();
        }
        if (!Files.exists(contextDir)) {
            try {
                Files.createDirectories(contextDir);
            } catch (IOException e) {
                throw new RuntimeException(
                        "Failed to create a directory to store the pig context: " + contextDir.toAbsolutePath());
            }
        }
        result.setContextLocation(contextJson.toAbsolutePath().toString());
        result.setConfigSha(sha);
        return result;
    }

    // for tests only!
    @Deprecated
    public static void setInstance(PigContext instance) {
        PigContext.instance = instance;
    }

    @java.lang.SuppressWarnings("all")
    public PigContext() {
    }

    @java.lang.SuppressWarnings("all")
    public PigConfiguration getPigConfiguration() {
        return this.pigConfiguration;
    }

    @java.lang.SuppressWarnings("all")
    public ImportResult getPncImportResult() {
        return this.pncImportResult;
    }

    @java.lang.SuppressWarnings("all")
    public Map<String, PncBuild> getBuilds() {
        return this.builds;
    }

    @java.lang.SuppressWarnings("all")
    public RepositoryData getRepositoryData() {
        return this.repositoryData;
    }

    @java.lang.SuppressWarnings("all")
    public Deliverables getDeliverables() {
        return this.deliverables;
    }

    @java.lang.SuppressWarnings("all")
    public String getTargetPath() {
        return this.targetPath;
    }

    @java.lang.SuppressWarnings("all")
    public String getReleaseDirName() {
        return this.releaseDirName;
    }

    @java.lang.SuppressWarnings("all")
    public String getReleasePath() {
        return this.releasePath;
    }

    @java.lang.SuppressWarnings("all")
    public String getExtrasPath() {
        return this.extrasPath;
    }

    @java.lang.SuppressWarnings("all")
    public String getConfigSha() {
        return this.configSha;
    }

    @java.lang.SuppressWarnings("all")
    public boolean isTempBuild() {
        return this.tempBuild;
    }

    @java.lang.SuppressWarnings("all")
    public String getContextLocation() {
        return this.contextLocation;
    }

    @java.lang.SuppressWarnings("all")
    public String getPrefix() {
        return this.prefix;
    }

    @java.lang.SuppressWarnings("all")
    public String getFullVersion() {
        return this.fullVersion;
    }

    @java.lang.SuppressWarnings("all")
    public Map<String, Collection<String>> getChecksums() {
        return this.checksums;
    }

    @java.lang.SuppressWarnings("all")
    public void setPigConfiguration(final PigConfiguration pigConfiguration) {
        this.pigConfiguration = pigConfiguration;
    }

    @java.lang.SuppressWarnings("all")
    public void setPncImportResult(final ImportResult pncImportResult) {
        this.pncImportResult = pncImportResult;
    }

    @java.lang.SuppressWarnings("all")
    public void setBuilds(final Map<String, PncBuild> builds) {
        this.builds = builds;
    }

    @java.lang.SuppressWarnings("all")
    public void setRepositoryData(final RepositoryData repositoryData) {
        this.repositoryData = repositoryData;
    }

    @java.lang.SuppressWarnings("all")
    public void setDeliverables(final Deliverables deliverables) {
        this.deliverables = deliverables;
    }

    @java.lang.SuppressWarnings("all")
    public void setTargetPath(final String targetPath) {
        this.targetPath = targetPath;
    }

    @java.lang.SuppressWarnings("all")
    public void setReleaseDirName(final String releaseDirName) {
        this.releaseDirName = releaseDirName;
    }

    @java.lang.SuppressWarnings("all")
    public void setReleasePath(final String releasePath) {
        this.releasePath = releasePath;
    }

    @java.lang.SuppressWarnings("all")
    public void setExtrasPath(final String extrasPath) {
        this.extrasPath = extrasPath;
    }

    @java.lang.SuppressWarnings("all")
    public void setConfigSha(final String configSha) {
        this.configSha = configSha;
    }

    @java.lang.SuppressWarnings("all")
    public void setTempBuild(final boolean tempBuild) {
        this.tempBuild = tempBuild;
    }

    @java.lang.SuppressWarnings("all")
    public void setContextLocation(final String contextLocation) {
        this.contextLocation = contextLocation;
    }

    @java.lang.SuppressWarnings("all")
    public void setPrefix(final String prefix) {
        this.prefix = prefix;
    }

    @java.lang.SuppressWarnings("all")
    public void setFullVersion(final String fullVersion) {
        this.fullVersion = fullVersion;
    }

    @java.lang.SuppressWarnings("all")
    public void setChecksums(final Map<String, Collection<String>> checksums) {
        this.checksums = checksums;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this)
            return true;
        if (!(o instanceof PigContext))
            return false;
        final PigContext other = (PigContext) o;
        if (!other.canEqual((java.lang.Object) this))
            return false;
        if (this.isTempBuild() != other.isTempBuild())
            return false;
        final java.lang.Object this$pigConfiguration = this.getPigConfiguration();
        final java.lang.Object other$pigConfiguration = other.getPigConfiguration();
        if (this$pigConfiguration == null ? other$pigConfiguration != null
                : !this$pigConfiguration.equals(other$pigConfiguration))
            return false;
        final java.lang.Object this$pncImportResult = this.getPncImportResult();
        final java.lang.Object other$pncImportResult = other.getPncImportResult();
        if (this$pncImportResult == null ? other$pncImportResult != null
                : !this$pncImportResult.equals(other$pncImportResult))
            return false;
        final java.lang.Object this$builds = this.getBuilds();
        final java.lang.Object other$builds = other.getBuilds();
        if (this$builds == null ? other$builds != null : !this$builds.equals(other$builds))
            return false;
        final java.lang.Object this$repositoryData = this.getRepositoryData();
        final java.lang.Object other$repositoryData = other.getRepositoryData();
        if (this$repositoryData == null ? other$repositoryData != null
                : !this$repositoryData.equals(other$repositoryData))
            return false;
        final java.lang.Object this$deliverables = this.getDeliverables();
        final java.lang.Object other$deliverables = other.getDeliverables();
        if (this$deliverables == null ? other$deliverables != null : !this$deliverables.equals(other$deliverables))
            return false;
        final java.lang.Object this$targetPath = this.getTargetPath();
        final java.lang.Object other$targetPath = other.getTargetPath();
        if (this$targetPath == null ? other$targetPath != null : !this$targetPath.equals(other$targetPath))
            return false;
        final java.lang.Object this$releaseDirName = this.getReleaseDirName();
        final java.lang.Object other$releaseDirName = other.getReleaseDirName();
        if (this$releaseDirName == null ? other$releaseDirName != null
                : !this$releaseDirName.equals(other$releaseDirName))
            return false;
        final java.lang.Object this$releasePath = this.getReleasePath();
        final java.lang.Object other$releasePath = other.getReleasePath();
        if (this$releasePath == null ? other$releasePath != null : !this$releasePath.equals(other$releasePath))
            return false;
        final java.lang.Object this$extrasPath = this.getExtrasPath();
        final java.lang.Object other$extrasPath = other.getExtrasPath();
        if (this$extrasPath == null ? other$extrasPath != null : !this$extrasPath.equals(other$extrasPath))
            return false;
        final java.lang.Object this$configSha = this.getConfigSha();
        final java.lang.Object other$configSha = other.getConfigSha();
        if (this$configSha == null ? other$configSha != null : !this$configSha.equals(other$configSha))
            return false;
        final java.lang.Object this$contextLocation = this.getContextLocation();
        final java.lang.Object other$contextLocation = other.getContextLocation();
        if (this$contextLocation == null ? other$contextLocation != null
                : !this$contextLocation.equals(other$contextLocation))
            return false;
        final java.lang.Object this$prefix = this.getPrefix();
        final java.lang.Object other$prefix = other.getPrefix();
        if (this$prefix == null ? other$prefix != null : !this$prefix.equals(other$prefix))
            return false;
        final java.lang.Object this$fullVersion = this.getFullVersion();
        final java.lang.Object other$fullVersion = other.getFullVersion();
        if (this$fullVersion == null ? other$fullVersion != null : !this$fullVersion.equals(other$fullVersion))
            return false;
        final java.lang.Object this$checksums = this.getChecksums();
        final java.lang.Object other$checksums = other.getChecksums();
        if (this$checksums == null ? other$checksums != null : !this$checksums.equals(other$checksums))
            return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof PigContext;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        result = result * PRIME + (this.isTempBuild() ? 79 : 97);
        final java.lang.Object $pigConfiguration = this.getPigConfiguration();
        result = result * PRIME + ($pigConfiguration == null ? 43 : $pigConfiguration.hashCode());
        final java.lang.Object $pncImportResult = this.getPncImportResult();
        result = result * PRIME + ($pncImportResult == null ? 43 : $pncImportResult.hashCode());
        final java.lang.Object $builds = this.getBuilds();
        result = result * PRIME + ($builds == null ? 43 : $builds.hashCode());
        final java.lang.Object $repositoryData = this.getRepositoryData();
        result = result * PRIME + ($repositoryData == null ? 43 : $repositoryData.hashCode());
        final java.lang.Object $deliverables = this.getDeliverables();
        result = result * PRIME + ($deliverables == null ? 43 : $deliverables.hashCode());
        final java.lang.Object $targetPath = this.getTargetPath();
        result = result * PRIME + ($targetPath == null ? 43 : $targetPath.hashCode());
        final java.lang.Object $releaseDirName = this.getReleaseDirName();
        result = result * PRIME + ($releaseDirName == null ? 43 : $releaseDirName.hashCode());
        final java.lang.Object $releasePath = this.getReleasePath();
        result = result * PRIME + ($releasePath == null ? 43 : $releasePath.hashCode());
        final java.lang.Object $extrasPath = this.getExtrasPath();
        result = result * PRIME + ($extrasPath == null ? 43 : $extrasPath.hashCode());
        final java.lang.Object $configSha = this.getConfigSha();
        result = result * PRIME + ($configSha == null ? 43 : $configSha.hashCode());
        final java.lang.Object $contextLocation = this.getContextLocation();
        result = result * PRIME + ($contextLocation == null ? 43 : $contextLocation.hashCode());
        final java.lang.Object $prefix = this.getPrefix();
        result = result * PRIME + ($prefix == null ? 43 : $prefix.hashCode());
        final java.lang.Object $fullVersion = this.getFullVersion();
        result = result * PRIME + ($fullVersion == null ? 43 : $fullVersion.hashCode());
        final java.lang.Object $checksums = this.getChecksums();
        result = result * PRIME + ($checksums == null ? 43 : $checksums.hashCode());
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "PigContext(pigConfiguration=" + this.getPigConfiguration() + ", pncImportResult="
                + this.getPncImportResult() + ", builds=" + this.getBuilds() + ", repositoryData="
                + this.getRepositoryData() + ", deliverables=" + this.getDeliverables() + ", targetPath="
                + this.getTargetPath() + ", releaseDirName=" + this.getReleaseDirName() + ", releasePath="
                + this.getReleasePath() + ", extrasPath=" + this.getExtrasPath() + ", configSha=" + this.getConfigSha()
                + ", tempBuild=" + this.isTempBuild() + ", contextLocation=" + this.getContextLocation() + ", prefix="
                + this.getPrefix() + ", fullVersion=" + this.getFullVersion() + ", checksums=" + this.getChecksums()
                + ")";
    }
}
