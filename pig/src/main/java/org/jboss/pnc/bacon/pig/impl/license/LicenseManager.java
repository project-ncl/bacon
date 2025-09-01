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
package org.jboss.pnc.bacon.pig.impl.license;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import org.jboss.pnc.bacon.pig.impl.PigContext;
import org.jboss.pnc.bacon.pig.impl.common.DeliverableManager;
import org.jboss.pnc.bacon.pig.impl.config.GenerationData;
import org.jboss.pnc.bacon.pig.impl.config.LicenseGenerationStrategy;
import org.jboss.pnc.bacon.pig.impl.config.PigConfiguration;
import org.jboss.pnc.bacon.pig.impl.config.RepoGenerationData;
import org.jboss.pnc.bacon.pig.impl.documents.Deliverables;
import org.jboss.pnc.bacon.pig.impl.pnc.PncBuild;
import org.jboss.pnc.bacon.pig.impl.repo.RepositoryData;
import org.jboss.pnc.bacon.pig.impl.utils.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com <br>
 *         Date: 12/5/17
 */
public class LicenseManager extends DeliverableManager<GenerationData<?>, Void> {
    private static final Logger log = LoggerFactory.getLogger(LicenseGenerator.class);
    private final RepositoryData repositoryData;
    private final GenerationData<LicenseGenerationStrategy> generationData;
    private final boolean useTempBuilds;
    private final boolean strict;
    private String exceptionsPath;
    private String namesPath;

    public LicenseManager(
            PigConfiguration pigConfiguration,
            String releasePath,
            boolean strict,
            Deliverables deliverables,
            Map<String, PncBuild> builds,
            RepositoryData repositoryData) {
        super(pigConfiguration, releasePath, deliverables, builds);
        this.repositoryData = repositoryData;
        this.strict = strict;
        generationData = pigConfiguration.getFlow().getLicensesGeneration();
        useTempBuilds = PigContext.get().isTempBuild();
        exceptionsPath = pigConfiguration.getFlow().getLicensesGeneration().getLicenseExceptionsPath();
        namesPath = pigConfiguration.getFlow().getLicensesGeneration().getLicenseNamesPath();
    }

    public void prepare() {
        LicenseGenerationStrategy strategy = (generationData == null) ? LicenseGenerationStrategy.IGNORE
                : generationData.getStrategy();
        switch (strategy) {
            case DOWNLOAD:
                downloadAndRepackage();
                break;
            case GENERATE:
                generate();
                break;
            case IGNORE:
                log.info("Ignoring license zip generation");
                break;
            default:
                throw new IllegalStateException("Unsupported repository generation strategy");
        }
    }

    @Override
    protected void repackage(File contentsDirectory, File targetTopLevelDirectory) {
        FileUtils.copy(contentsDirectory, targetTopLevelDirectory);
    }

    @Override
    protected String getTargetTopLevelDirectoryName() {
        return pigConfiguration.getTopLevelDirectoryPrefix() + "licenses";
    }

    @Override
    protected Path getTargetZipPath() {
        return Paths.get(releasePath, deliverables.getLicenseZipName());
    }

    private void generate() {
        log.info("Generating licenses");
        RepoGenerationData repoGen = pigConfiguration.getFlow().getRepositoryGeneration();
        if (repoGen.isIncludeLicenses()) {
            LicenseGenerator.extractLicenses(
                    repositoryData.getRepositoryPath().toFile(),
                    getTargetZipPath().toFile(),
                    getTargetTopLevelDirectoryName());
        } else {
            LicenseGenerator.generateLicenses(
                    repositoryData.getGavs(),
                    getTargetZipPath().toFile(),
                    getTargetTopLevelDirectoryName(),
                    strict,
                    exceptionsPath,
                    namesPath);
        }
    }

    @java.lang.SuppressWarnings("all")
    public GenerationData<LicenseGenerationStrategy> getGenerationData() {
        return this.generationData;
    }
}
