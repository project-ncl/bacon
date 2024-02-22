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

package org.jboss.pnc.bacon.licenses;

import io.quarkus.qute.Qute;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.jboss.pnc.bacon.licenses.xml.DependencyElement;
import org.jboss.pnc.bacon.licenses.xml.LicenseElement;
import org.jboss.pnc.bacon.licenses.xml.LicenseSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Class responsible for persisting licenses information to XML and HTML files.
 *
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
public class LicensesFileManager {

    private static final int DOWNLOAD_TIMEOUT = 60_000;

    private static final String CONTENTS_DIR = "contents";
    private static final String LICENSES_QUTE = "licenses.qute";

    private final Logger logger = LoggerFactory.getLogger(LicensesFileManager.class);

    private final CloseableHttpClient httpClient = HttpClientBuilder.create()
            .setDefaultRequestConfig(
                    RequestConfig.custom()
                            .setConnectTimeout(DOWNLOAD_TIMEOUT)
                            .setSocketTimeout(DOWNLOAD_TIMEOUT)
                            .build())
            .build();

    /**
     * Create a licenses.xml file.
     *
     * @param licenseSummary license summary XML element, which should be written to a licenses.xml file.
     * @param directoryPath directory where new file should be stored.
     * @throws LicensesGeneratorException on generation failure
     */
    public void createLicensesXml(LicenseSummary licenseSummary, String directoryPath)
            throws LicensesGeneratorException {
        logger.debug("Generating licenses.xml at {}", directoryPath);
        File file = new File(directoryPath, "licenses.xml");
        try {
            FileUtils.writeStringToFile(file, licenseSummary.toXmlString(), StandardCharsets.UTF_8);
        } catch (JAXBException | IOException e) {
            throw new LicensesGeneratorException("Failed to create licenses.xml", e);
        }
    }

    /**
     * Create a licenses.html file and download copy of each license for offline use.
     *
     * @param licenseSummary license summary XML element, which should be written to a licenses.xml file.
     * @param directoryPath directory where new file should be stored.
     * @throws LicensesGeneratorException on generation failure
     */
    public void createLicensesHtml(LicenseSummary licenseSummary, String directoryPath)
            throws LicensesGeneratorException {
        logger.debug("Generating licenses.html at {}", directoryPath);
        Map<String, String> licenseFiles = downloadLicenseFiles(licenseSummary.getDependencies(), directoryPath);

        File file = new File(directoryPath, "licenses.html");

        try (FileWriter fileOutputStream = new FileWriter(file)) {
            fileOutputStream.write(
                    Qute.fmt(loadTemplate())
                            .data("dependencies", licenseSummary.getDependencies())
                            .data("licenseFiles", licenseFiles)
                            .render());
        } catch (IOException e) {
            throw new LicensesGeneratorException("Failed to create licenses.html", e);
        }
    }

    private static String loadTemplate() {
        final InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(LICENSES_QUTE);
        if (is == null) {
            throw new IllegalStateException("Failed to locate " + LICENSES_QUTE + " on the classpath");
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            StringBuilder sb = new StringBuilder();
            String line = reader.readLine();
            while (line != null) {
                sb.append(line).append(System.lineSeparator());
                line = reader.readLine();
            }
            return sb.toString();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private Map<String, String> downloadLicenseFiles(List<DependencyElement> dependencies, String directoryPath) {
        final File licenseContentsDirectory = new File(directoryPath, CONTENTS_DIR);
        licenseContentsDirectory.mkdirs();
        return dependencies.stream()
                .flatMap(
                        dependency -> dependency.getLicenses()
                                .stream()
                                .map(license -> downloadLicenseFile(dependency, license, licenseContentsDirectory)))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .distinct()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private Optional<Map.Entry<String, String>> downloadLicenseFile(
            DependencyElement dependency,
            LicenseElement license,
            File licenseContentsDirectory) {
        String textUrl = license.getTextUrl();
        if (StringUtils.isBlank(textUrl)) {
            return Optional.empty();
        }
        logger.debug("Downloading license file for {} from {}", dependency.toGavString(), textUrl);
        try {
            String fileName = getLocalLicenseFileName(license);
            File file = new File(licenseContentsDirectory, fileName);
            boolean download = false;
            if (!file.exists()) {
                synchronized (this) {
                    if (!file.exists()) {
                        file.createNewFile();
                        download = true;
                    }
                }
            }
            if (download) {
                try {
                    downloadTo(textUrl, file);
                } catch (IOException e) {
                    if (!textUrl.startsWith("https")) {
                        downloadTo(textUrl.replace("http", "https"), file);
                    } else {
                        throw e;
                    }
                }
            }
            return Optional.of(
                    new AbstractMap.SimpleEntry<>(license.getName(), String.format("%s/%s", CONTENTS_DIR, fileName)));
        } catch (Exception e) {
            logger.warn(
                    "Failed to download license '{}' for '{}' from '{}'",
                    license.getName(),
                    dependency.toGavString(),
                    textUrl,
                    e);
            return Optional.empty();
        }
    }

    // TODO: optimize!
    private void downloadTo(String url, File file) throws IOException {
        HttpGet request = new HttpGet(url);
        HttpResponse response = httpClient.execute(request);
        HttpEntity entity = response.getEntity();
        if (entity != null) {
            try (OutputStream stream = new FileOutputStream(file)) {
                entity.writeTo(stream);
            }
        }
    }

    private String getLocalLicenseFileName(LicenseElement licenseElement) {
        String fileName = licenseElement.getName().replaceAll("[^A-Za-z0-9 ]", "");
        return fileName.replace(" ", "+");
    }

}
