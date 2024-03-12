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

import org.jboss.pnc.bacon.licenses.properties.GeneratorProperties;
import org.jboss.pnc.bacon.licenses.properties.PropertyKeys;

import java.util.Arrays;
import java.util.Objects;
import java.util.Properties;

/**
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
public class LicensesGeneratorApplication {

    public static void main(String... args) throws Exception {
        Properties properties = argsToProperties(args);
        GeneratorProperties generatorProperties = getGeneratorProperties(properties);
        LicensesGenerator licensesGenerator = new LicensesGenerator(generatorProperties);

        String pomPath = properties.getProperty("pom");
        String resultPath = properties.getProperty("destination");

        licensesGenerator.generateLicensesForPom(pomPath, resultPath);
    }

    private static GeneratorProperties getGeneratorProperties(Properties properties) {
        GeneratorProperties generatorProperties;

        if (properties.containsKey("generatorProperties")) {
            generatorProperties = new GeneratorProperties(properties.getProperty("generatorProperties"));
        } else {
            generatorProperties = new GeneratorProperties();
        }

        // Override properties if explicitly provided
        if (properties.containsKey(PropertyKeys.ALIASES_FILE)) {
            generatorProperties.setAliasesFilePath(properties.getProperty(PropertyKeys.ALIASES_FILE));
        }

        if (properties.containsKey(PropertyKeys.EXCEPTIONS_FILE)) {
            generatorProperties.setExceptionsFilePath(properties.getProperty(PropertyKeys.EXCEPTIONS_FILE));
        }

        return generatorProperties;
    }

    private static Properties argsToProperties(String... args) {
        Properties properties = new Properties();
        Arrays.stream(args)
                .map(s -> s.replace("-D", ""))
                .filter(s -> s.contains("="))
                .map(s -> s.split("="))
                .filter(a -> a.length == 2)
                .forEach(a -> properties.put(a[0], a[1]));

        Objects.requireNonNull(properties.getProperty("pom"), "'pom' is required");
        Objects.requireNonNull(properties.getProperty("destination"), "'destination' is required");

        return properties;
    }

}
