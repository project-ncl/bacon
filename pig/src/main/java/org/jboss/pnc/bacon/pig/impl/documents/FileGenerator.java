/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
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
package org.jboss.pnc.bacon.pig.impl.documents;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Optional;

import freemarker.core.Environment;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.TemplateException;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com <br>
 *         Date: 6/5/17
 */
public class FileGenerator {
    private final Configuration resourceLoadingConfiguration;
    private final Optional<Configuration> fileLoadingConfiguration;
    private final Optional<Path> configurationDir;

    public FileGenerator(Optional<Path> configurationDir) {
        this.configurationDir = configurationDir;

        fileLoadingConfiguration = configurationDir.map(dir -> {
            Configuration config = createConfiguration();
            try {
                config.setDirectoryForTemplateLoading(dir.toFile());
            } catch (IOException e) {
                throw new RuntimeException("Unable to load configuration directory", e);
            }
            return config;
        });

        resourceLoadingConfiguration = createConfiguration();
        resourceLoadingConfiguration.setClassForTemplateLoading(Template.class, "/");
    }

    private Configuration createConfiguration() {
        Configuration configuration = new Configuration(Configuration.VERSION_2_3_23);
        configuration.setObjectWrapper(new DefaultObjectWrapper(Configuration.VERSION_2_3_23));
        configuration.setEncoding(new Locale("en_US"), "UTF-8");
        configuration.setDefaultEncoding("UTF-8");
        return configuration;
    }

    public <DataRootType> void generateFileFromResource(DataRootType data, String resourceName, File outputFile) {
        generateToFile(data, outputFile, resourceName);
    }

    public void generateFiles(String parentDir, String extrasDir, DataRoot data) {
        Paths.get(extrasDir).toFile().mkdirs();

        for (Template template : Template.values()) {
            generateFromTemplate(template.isExtrasPath() ? extrasDir : parentDir, template, data);
        }
    }

    private void generateFromTemplate(String parentDir, Template template, DataRoot data) {
        File output = generateOutputFile(parentDir, template);
        generateToFile(data, output, template.getTemplateName());
    }

    private <DataRootType> void generateToFile(DataRootType data, File output, String templateName) {
        try (FileWriter writer = new FileWriter(output)) {
            Environment env = getTemplate(templateName).createProcessingEnvironment(data, writer);
            env.setOutputEncoding("UTF-8");
            env.setNumberFormat("computer");
            env.process();
        } catch (IOException | TemplateException e) {
            throw new IllegalStateException("Unable to generate content from template " + templateName, e);
        }
    }

    private freemarker.template.Template getTemplate(String templateName) {
        try {
            if (configurationDir.map(d -> d.resolve(templateName).toFile().exists()).orElse(false)) {
                // noinspection OptionalGetWithoutIsPresent
                return fileLoadingConfiguration.get().getTemplate(templateName);
            } else {
                return resourceLoadingConfiguration.getTemplate(templateName);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Unable to generate template for " + templateName, e);
        }
    }

    private static File generateOutputFile(String parentDir, Template template) {
        return new File(parentDir, template.getFilename());
    }
}
