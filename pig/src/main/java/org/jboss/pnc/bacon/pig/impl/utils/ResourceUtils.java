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

package org.jboss.pnc.bacon.pig.impl.utils;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Properties;

import static org.apache.commons.io.FileUtils.writeStringToFile;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * <br>
 * Date: 7/14/17
 */
public class ResourceUtils {

    private static final Logger log = LoggerFactory.getLogger(ResourceUtils.class);

    public static final String ENCODING = "UTF-8";

    public static File extractToTmpFile(String resource, String prefix, String suffix) {
        try {
            File tempFile = File.createTempFile(prefix, suffix);
            return extractToFile(resource, tempFile);
        } catch (IOException e) {
            throw new RuntimeException("Unable to extract resource: '" + resource + "' to file", e);
        }
    }

    public static File extractToFile(String resource, File targetFile) {
        try {
            String resourceAsString = getResourceAsString(resource);
            FileUtils.write(targetFile, resourceAsString, ENCODING);
            return targetFile;
        } catch (IOException e) {
            throw new RuntimeException("Unable to extract resource: '" + resource + "' to file", e);
        }
    }

    public static void copyResourceWithFiltering(String sourceFileName,
                                                 String targetFileName,
                                                 File targetDirectory,
                                                 Properties properties,
                                                 Path configurationDirectory) {
        String text = getOverridableResource(sourceFileName, configurationDirectory);
        text = PropertyUtils.replaceProperties(text, properties);
        File targetFile = new File(targetDirectory, targetFileName);
        try {
            writeStringToFile(targetFile, text, ENCODING);
        } catch (IOException e) {
            throw new RuntimeException("Unable to copy resource " + sourceFileName +
                    " to " + targetDirectory.getAbsolutePath());
        }
    }

    public static void copyOverridableResource(String sourceFileName,
                                               String targetFileName,
                                               File targetDirectory,
                                               Path configurationDirectory) {
        String text = getOverridableResource(sourceFileName, configurationDirectory);
        File targetFile = new File(targetDirectory, targetFileName);
        try {
            writeStringToFile(targetFile, text, ENCODING);
        } catch (IOException e) {
            throw new RuntimeException("Unable to copy resource " + sourceFileName +
                    " to " + targetDirectory.getAbsolutePath());
        }
    }

    /**
     * Get file contents either from configurationDirectory or resources.
     * If a file called `fileName` is defined in the `configurationDirectory`, than its contents will be returned,
     * otherwise a resource from jar file with the same name will be used as a source
     *
     * @param fileName               name of the file to read
     * @param configurationDirectory directory to check for the files
     * @return file content read to string
     */
    public static String getOverridableResource(String fileName, Path configurationDirectory) {
        Optional<String> maybeResult = getResourceFromDirectory(fileName, configurationDirectory);
        return maybeResult.orElseGet(() -> getResourceAsString(fileName));
    }

    public static Optional<String> getResourceFromDirectory(String fileName, Path configurationDirectory) {
        File file = new File(configurationDirectory.toFile(), fileName);
        if (file.exists()) {
            try {
                return Optional.of(FileUtils.readFileToString(file, ENCODING));
            } catch (IOException e) {
                log.error("Unable to read file: {}", file.getAbsolutePath(), e);
            }
        }
        return Optional.empty();
    }

    private static String getResourceAsString(String resource) {
        InputStream resourceStream = ResourceUtils.class.getResourceAsStream(resource);
        if (resourceStream == null) {
            throw new IllegalArgumentException("Resource " + resource + " not found");
        }
        try {
            return IOUtils.toString(resourceStream, ENCODING);
        } catch (IOException e) {
            throw new IllegalArgumentException("Unable to read resource: " + resource);
        }
    }

    private ResourceUtils() {
    }
}
