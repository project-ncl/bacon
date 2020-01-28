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

import me.snowdrop.licenses.LicensesGenerator;
import me.snowdrop.licenses.LicensesGeneratorException;
import me.snowdrop.licenses.properties.GeneratorProperties;
import me.snowdrop.licenses.utils.Gav;
import org.jboss.pnc.bacon.pig.impl.utils.FileUtils;
import org.jboss.pnc.bacon.pig.impl.utils.GAV;
import org.jboss.pnc.bacon.pig.impl.utils.ResourceUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com <br>
 *         Date: 8/24/17
 */
public class LicenseGenerator {
    private static final Logger log = LoggerFactory.getLogger(LicenseGenerator.class);

    private LicenseGenerator() {
    }

    public static void generateLicenses(Collection<GAV> gavs, File archiveFile, String topLevelDirectoryName) {
        File temporaryDestination = FileUtils.mkTempDir("licenses");
        File topLevelDirectory = new File(temporaryDestination, topLevelDirectoryName);

        generateLicenses(gavs, topLevelDirectory);
        FileUtils.zip(archiveFile, temporaryDestination, topLevelDirectory);
        log.debug("Generated zip archive {}", archiveFile);
    }

    private static void generateLicenses(Collection<GAV> gavs, File temporaryDestination) {
        try {
            LicensesGenerator generator = new LicensesGenerator(prepareGeneratorProperties());

            generator.generateLicensesForGavs(gavsToLicenseGeneratorGavs(gavs), temporaryDestination.getAbsolutePath());
        } catch (LicensesGeneratorException e) {
            throw new RuntimeException("Failed to generate licenses", e);
        }
    }

    private static List<Gav> gavsToLicenseGeneratorGavs(Collection<GAV> gavs) {
        return gavs.stream().map(gav -> new Gav(gav.getGroupId(), gav.getArtifactId(), gav.getVersion(), gav.getPackaging()))
                .collect(Collectors.toList());
    }

    private static GeneratorProperties prepareGeneratorProperties() {
        File propertiesFile = ResourceUtils.extractToTmpFile("/license-generator.properties", "license-generator",
                ".properties");
        return new GeneratorProperties(propertiesFile.getAbsolutePath());
    }
}
