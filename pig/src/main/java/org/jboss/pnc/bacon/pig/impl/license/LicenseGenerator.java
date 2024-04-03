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

import org.jboss.pnc.bacon.config.Config;
import org.jboss.pnc.bacon.config.PigConfig;
import org.jboss.pnc.bacon.licenses.LicensesGenerator;
import org.jboss.pnc.bacon.licenses.LicensesGeneratorException;
import org.jboss.pnc.bacon.licenses.properties.GeneratorProperties;
import org.jboss.pnc.bacon.licenses.utils.Gav;
import org.jboss.pnc.bacon.pig.impl.PigContext;
import org.jboss.pnc.bacon.pig.impl.utils.FileUtils;
import org.jboss.pnc.bacon.pig.impl.utils.GAV;
import org.jboss.pnc.bacon.pig.impl.utils.ResourceUtils;
import org.jboss.pnc.bacon.pig.impl.utils.XmlUtils;
import org.jboss.pnc.bacon.pig.impl.utils.indy.Indy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com <br>
 *         Date: 8/24/17
 */
public class LicenseGenerator {
    private static final Logger log = LoggerFactory.getLogger(LicenseGenerator.class);

    private LicenseGenerator() {
    }

    public static void generateLicenses(
            Collection<GAV> gavs,
            File archiveFile,
            String topLevelDirectoryName,
            boolean strict,
            String exceptionsPath,
            String namesPath) {
        File temporaryDestination = FileUtils.mkTempDir("licenses");
        File topLevelDirectory = new File(temporaryDestination, topLevelDirectoryName);

        generateLicenses(gavs, topLevelDirectory, PigContext.get().isTempBuild(), strict, exceptionsPath, namesPath);
        FileUtils.zip(archiveFile, temporaryDestination, topLevelDirectory);
        log.debug("Generated zip archive {}", archiveFile);
    }

    public static void generateLicenses(
            Collection<GAV> gavs,
            File licensesDirectory,
            boolean useTempBuilds,
            boolean strict,
            String exceptionsPath,
            String namesPath) {
        try {
            LicensesGenerator generator = new LicensesGenerator(
                    prepareGeneratorProperties(useTempBuilds, exceptionsPath, namesPath));

            generator.generateLicensesForGavs(gavsToLicenseGeneratorGavs(gavs), licensesDirectory.getAbsolutePath());
            // Checking if the URL for licenses are present and are valid
            File xmlFile = new File(licensesDirectory.getAbsolutePath(), "licenses.xml");
            log.info("License directory {}", licensesDirectory.getAbsolutePath());
            boolean isInvalidLicensesPresent = XmlUtils
                    .isValidNodePresent(xmlFile, "//license[not(url)] or //url[not(string(.))]");
            if (isInvalidLicensesPresent) {
                if (log.isErrorEnabled()) {
                    log.error(
                            "There are some invalid licenses in XML file generated. Following are the details of the invalid licenses:");
                    List<Node> nodes = XmlUtils.listNodes(xmlFile, "//license[not(url)]/parent::node()/parent::node()");
                    nodes.forEach(
                            node -> log.error(
                                    "License url missing for {}:{}:{}",
                                    node.getChildNodes().item(1).getTextContent(),
                                    node.getChildNodes().item(3).getTextContent(),
                                    node.getChildNodes().item(5).getTextContent()));
                }

                if (strict) {
                    throw new RuntimeException("Invalid licenses XML file");
                }
            }
        } catch (LicensesGeneratorException e) {
            throw new RuntimeException("Failed to generate licenses", e);
        }
    }

    public static void extractLicenses(File repoZip, File archiveFile, String topLevelDirectoryName) {
        File temporaryDestination = FileUtils.mkTempDir("licenses");
        File topLevelDirectory = new File(temporaryDestination, topLevelDirectoryName);

        try {
            FileUtils.unzip(repoZip, topLevelDirectory, "^[^/]*/licenses/.*");
            try (Stream<Path> stream = Files.list(topLevelDirectory.toPath())) {
                File repoDir = stream.iterator().next().toFile();
                FileUtils.moveDirectoryContents(new File(repoDir, "licenses"), topLevelDirectory);
                org.apache.commons.io.FileUtils.deleteDirectory(repoDir);
                FileUtils.zip(archiveFile, temporaryDestination, topLevelDirectory);
                log.info("Generated zip archive {}", archiveFile);
            }
        } catch (IOException ex) {
            throw new RuntimeException("Failed to extract licenses from zip", ex);
        }

    }

    private static List<Gav> gavsToLicenseGeneratorGavs(Collection<GAV> gavs) {
        return gavs.stream()
                .map(gav -> new Gav(gav.getGroupId(), gav.getArtifactId(), gav.getVersion(), gav.getPackaging()))
                .collect(Collectors.toList());
    }

    private static GeneratorProperties prepareGeneratorProperties(
            boolean useTempBuilds,
            String exceptionsPath,
            String namesPath) {
        Properties props = new Properties();
        PigConfig pig = Config.instance().getActiveProfile().getPig();
        String licenseServiceUrl = pig.getLicenseServiceUrl();
        String licenseServiceProp = "";
        if (licenseServiceUrl != null) {
            licenseServiceProp = String
                    .format("licenseServiceUrl=%s/find-license-check-record-and-license-info", licenseServiceUrl);
        }
        props.setProperty("licenseServiceUrl", licenseServiceProp);
        if (useTempBuilds) {
            props.setProperty("names", "Indy Temp Builds,Indy Static");
            props.setProperty("urls", Indy.getIndyTempUrl() + "," + Indy.getIndyUrl());
        } else {
            props.setProperty("names", "Indy Static");
            props.setProperty("urls", Indy.getIndyUrl());
        }

        File propertiesFile = ResourceUtils.extractToTmpFileWithFiltering(
                "/license-generator.properties",
                "license-generator",
                ".properties",
                props);
        GeneratorProperties genProp = new GeneratorProperties(propertiesFile.getAbsolutePath());
        if (exceptionsPath != null)
            genProp.setExceptionsFilePath(exceptionsPath);
        if (namesPath != null)
            genProp.setAliasesFilePath(namesPath);
        return genProp;
    }
}
