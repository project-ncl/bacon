/**
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
package org.jboss.pnc.bacon.pig.config.build;

import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang3.StringUtils.join;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * <br>
 * Date: 11/28/17
 */
@Data
public class Config {
    private static final Logger log = LoggerFactory.getLogger(Config.class);

    private Product product;
    private String version;
    private String milestone;
    private String group;
    private BuildConfig defaultBuildParameters = new BuildConfig();
    private List<BuildConfig> builds = new ArrayList<>();
    private Output outputPrefixes;
    private Flow flow;
    private String majorMinor;
    private String micro;
    private Map<String, Map<String, ?>> addons = new HashMap<>();
    private static final Integer maxTries = 256;

    private void init() {
        String[] splittedVersion = version.split("\\.");
        if (splittedVersion.length != 3) {
            throw new RuntimeException("Version property should be of a form of <number>.<number>.<number>. Found: " + version);
        } else {
            majorMinor = splittedVersion[0] + "." + splittedVersion[1];
            micro = splittedVersion[2];
        }

        builds.forEach(config -> config.setDefaults(defaultBuildParameters));
        builds.forEach(BuildConfig::sanitizebuildScript);
        List<String> errors = validate();
        if (!errors.isEmpty()) {
            throw new RuntimeException("The build configuration file is invalid. Errors:\n" + join(errors, "\n"));
        }
    }

    private List<String> validate() {
        List<String> errors = new ArrayList<>();
        checkForDuplicateConfigNames(errors);
        builds.forEach(b -> b.validate(errors));
        // TODO!
        return errors;
    }

    private void checkForDuplicateConfigNames(List<String> errors) {
        List<String> configNames = builds.stream().map(BuildConfig::getName).collect(toList());
        Set<String> uniqueNames = new HashSet<>(configNames);

        uniqueNames.forEach(configNames::remove);

        configNames.forEach(
                duplicateName -> errors.add("More than one configuration is named '" + duplicateName + "'")
        );
    }

    private static List<String> getAllMatches(String text, String regex) {
        List<String> matches = new ArrayList<>();
        Matcher m = Pattern.compile(regex).matcher(text);
        while (m.find()) {
            matches.add(m.group());
        }
        return matches;
    }

    private static HashMap<String, String> readVariablesFromFile(String contents) {
        HashMap<String, String> variables = new HashMap<>();
        //Look for the variable defs i.e. #!myVariable=1.0.0
        List<String> matches = getAllMatches(contents, "#![a-zA-Z0-9_-]*=.*");
        for (String temp : matches) {
            String[] nameValue = temp.split("=", 2);
            //remove the !# of the name
            variables.put(nameValue[0].substring(2), nameValue[1]);
        }
        return variables;
    }

    private static Map<String, String> convertbuildVarsOverridesStringToMap(String buildVarsOverrides) {
        return Arrays
                .stream(buildVarsOverrides.split(","))
                .map(String::trim)
                .filter(s -> s.contains("="))
                .map(s -> s.split("="))
                .filter(a -> a.length == 2)
                .collect(toMap(
                        (a) -> a[0].trim(),
                        (a) -> a[1].trim()
                ));
    }

    private static Map<String, String> readVariables(String contents, String buildVarsOverrides) {
        Map<String, String> variablesFromFile = readVariablesFromFile(contents);
        Map<String, String> variableOverrides = convertbuildVarsOverridesStringToMap(buildVarsOverrides);

        Map<String, String> result = new HashMap<>(variablesFromFile);
        variableOverrides.forEach((k, v) -> result.merge(k, v, (v1, v2) -> v2));
        return result;
    }

    public static InputStream preProcess(InputStream buildConfig, String buildVarsOverrides) {
        String contents = "";
        @SuppressWarnings("resource")
        Scanner s = new Scanner(buildConfig);

        s.useDelimiter("\\A");

        if (s.hasNext()) {
            contents = s.next();
        }

        Map<String, String> variables = readVariables(contents, buildVarsOverrides);
        Integer passes = 0;
        // We also have to take into account variable used inside variables, just keep going over until
        // they have all expanded, maxTries will be hit if one is left over after that many
        List<String> matches;

        while (!(matches = getAllMatches(contents, "\\{\\{.*}}")).isEmpty()) {
            passes++;

            for (Map.Entry<String, String> entry : variables.entrySet()) {
                String name = "\\{\\{" + entry.getKey() + "}}";
                String value = entry.getValue();
                contents = contents.replaceAll(name, value);
            }

            if (passes > maxTries) {
                StringBuilder leftOvers = new StringBuilder("");
                Set<String> setString = new HashSet<String>(matches);

                for (String temp : setString) {
                    leftOvers.append(temp).append(" ");
                }

                throw new RuntimeException("No variable definition for [" + leftOvers.substring(0, leftOvers.length() - 1) + "]");
            }
        }

        InputStream stream = new ByteArrayInputStream(contents.getBytes(StandardCharsets.UTF_8));

        return stream;
    }

    public static Config load(File buildConfigFile, String buildVarsOverrides) {
        try (InputStream configStream = new FileInputStream(buildConfigFile)) {
            return load(configStream, buildVarsOverrides);
        } catch (IOException e) {
            throw new RuntimeException("Unable to read build config file " + buildConfigFile.getAbsolutePath(), e);
        }
    }

    public static Config load(InputStream configStream) {
        return load(configStream, "");
    }

    public static Config load(InputStream configStream, String buildVarsOverrides) {
        if (buildVarsOverrides == null) {
            buildVarsOverrides = "";
        }

        Yaml yaml = new Yaml(new Constructor(Config.class));

        try (InputStream in = preProcess(configStream, buildVarsOverrides)) {
            Config config = (Config) yaml.load(in);
            config.init();
            return config;
        } catch (IOException e) {
            throw new RuntimeException("Unable to load config file", e);
        }
    }

    public String getTopLevelDirectoryPrefix() {
        return String.format("%s-%s.%s-", outputPrefixes.getReleaseDir(), version, product.getStage());
    }
}
