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

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.jboss.pnc.bacon.pig.impl.config.PigConfiguration;
import org.jboss.pnc.bacon.pig.impl.documents.Deliverables;
import org.jboss.pnc.bacon.pig.impl.pnc.ImportResult;
import org.jboss.pnc.bacon.pig.impl.pnc.PncBuild;
import org.jboss.pnc.bacon.pig.impl.repo.RepositoryData;
import org.jboss.pnc.bacon.pig.impl.utils.MilestoneNumberFinder;

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
import java.util.Optional;

import static org.jboss.pnc.bacon.common.Constant.PIG_CONTEXT_DIR;
import static org.jboss.pnc.bacon.pig.impl.utils.HashUtils.hashDirectory;

/**
 * TODO: consider saving the latest reached state to not repeat the steps already performed
 *
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com <br>
 *         Date: 4/1/19
 */
@Data
@Slf4j
public class PigContext {
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

    private String fullVersion; // version like 1.3.2.DR7

    private Map<String, Collection<String>> checksums;

    public void initConfig(
            Path configDir,
            String targetPath,
            Optional<String> releaseStorageUrl,
            Map<String, String> overrides) {
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

    private void setPigConfiguration(
            PigConfiguration pigConfiguration,
            String targetPath,
            Optional<String> releaseStorageUrl) {
        this.pigConfiguration = pigConfiguration;
        initFullVersion(pigConfiguration, releaseStorageUrl);

        if (deliverables == null) {
            String prefix = String.format("%s-%s", pigConfiguration.getOutputPrefixes().getReleaseFile(), fullVersion);

            deliverables = new Deliverables();

            deliverables.setRepositoryZipName(prefix + "-maven-repository.zip");
            deliverables.setLicenseZipName(prefix + "-license.zip");
            deliverables.setSourceZipName(prefix + "-src.zip");
            deliverables.setJavadocZipName(prefix + "-javadoc.zip");
            deliverables.setNvrListName(prefix + "-nvr-list.txt");
        }

        configureTargetDirectories(pigConfiguration, targetPath);
    }

    private void initFullVersion(PigConfiguration pigConfiguration, Optional<String> releaseStorageUrl) {
        if (fullVersion != null) {
            return;
        }
        @SuppressWarnings("deprecation")
        String milestone = pigConfiguration.getMilestone();
        @SuppressWarnings("deprecation")
        String version = pigConfiguration.getVersion();
        if (milestone.contains("*")) {
            String url = releaseStorageUrl.orElse(pigConfiguration.getReleaseStorageUrl());
            if (url == null) {
                throw new RuntimeException(
                        "Auto-incremented milestone used but no releaseStorageUrl provided. "
                                + "Please either set the releaseStorageUrl in the build config yaml, by the product version or set the url by adding `--releaseStorageUrl=...` parameter");
            }
            milestone = MilestoneNumberFinder.getFirstUnused(url, version, milestone);
        }
        setFullVersion(version + "." + milestone);
    }

    private void configureTargetDirectories(PigConfiguration pigConfiguration, String targetPath) {
        String productPrefix = pigConfiguration.getProduct().prefix();
        this.targetPath = targetPath;
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
            Optional<String> releaseStorageUrl,
            Map<String, String> overrides) {
        instance = readContext(clean, configDir);
        instance.initConfig(configDir, targetPath, releaseStorageUrl, overrides);
        return instance;
    }

    private static PigContext readContext(boolean clean, Path configDir) {
        String sha = hashDirectory(configDir);

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
}
