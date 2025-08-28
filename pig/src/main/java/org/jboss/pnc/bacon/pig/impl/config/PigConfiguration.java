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
package org.jboss.pnc.bacon.pig.impl.config;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.validation.ConstraintViolation;
import javax.validation.Valid;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import org.jboss.pnc.bacon.common.exception.FatalException;
import org.jboss.pnc.bacon.config.Validate;
import org.jboss.pnc.bacon.pig.impl.utils.AlignmentType;
import org.jboss.pnc.bacon.pig.impl.validation.ListBuildConfigCheck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com <br>
 *         Date: 11/28/17
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PigConfiguration implements Validate {
    private static final Logger log = LoggerFactory.getLogger(PigConfiguration.class);
    @NotNull
    @Valid
    private ProductConfig product;
    @NotBlank
    private String version;
    @NotBlank
    private String milestone;
    @NotBlank
    private String group;
    @NotNull
    private BuildConfig defaultBuildParameters = new BuildConfig();
    @NotEmpty
    @ListBuildConfigCheck
    @Valid
    private List<BuildConfig> builds = new ArrayList<>();
    @NotNull
    @Valid
    private Output outputPrefixes;
    private String outputSuffix;
    @NotNull
    @Valid
    private Flow flow;
    private String majorMinor;
    private String micro;
    private Map<String, Map<String, ?>> addons = new HashMap<>();
    private String releaseStorageUrl;
    private Boolean draft;
    private AlignmentType temporaryBuildAlignmentPreference;
    /**
     * Allow user to override brewTag auto-generated in PNC for a product version
     */
    private String brewTagPrefix;
    private static final Integer maxTries = 256;

    private void init() {
        String[] splittedVersion = version.split("\\.");
        if (splittedVersion.length != 3) {
            throw new RuntimeException(
                    "Version property should be of a form of <number>.<number>.<number>. Found: " + version);
        } else {
            majorMinor = splittedVersion[0] + "." + splittedVersion[1];
            micro = splittedVersion[2];
        }
        builds.forEach(config -> config.setDefaults(defaultBuildParameters));
        builds.forEach(BuildConfig::sanitizebuildScript);
        validate();
    }

    @Override
    public void validate() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        Validator validator = factory.getValidator();
        Set<ConstraintViolation<PigConfiguration>> violations = validator.validate(this);
        if (!violations.isEmpty()) {
            throw new FatalException(
                    "Errors while validating the build-config.yaml:\n"
                            + Validate.<PigConfiguration> prettifyConstraintViolation(violations));
        }
    }

    private void checkForDuplicateConfigNames(List<String> errors) {
        List<String> configNames = builds.stream().map(BuildConfig::getName).collect(toList());
        Set<String> uniqueNames = new HashSet<>(configNames);
        uniqueNames.forEach(configNames::remove);
        configNames
                .forEach(duplicateName -> errors.add("More than one configuration is named \'" + duplicateName + "\'"));
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
        // Look for the variable defs i.e. #!myVariable=1.0.0
        List<String> matches = getAllMatches(contents, "#![a-zA-Z0-9_-]*=.*");
        for (String temp : matches) {
            String[] nameValue = temp.split("=", 2);
            // remove the !# of the name
            variables.put(nameValue[0].substring(2), nameValue[1]);
        }
        return variables;
    }

    private static Map<String, String> convertbuildVarsOverridesStringToMap(String buildVarsOverrides) {
        return Arrays.stream(buildVarsOverrides.split(","))
                .map(String::trim)
                .filter(s -> s.contains("="))
                .map(s -> s.split("="))
                .filter(a -> a.length == 2)
                .collect(toMap(a -> a[0].trim(), a -> a[1].trim()));
    }

    private static Map<String, String> readVariables(String contents, Map<String, String> buildVarsOverrides) {
        Map<String, String> variablesFromFile = readVariablesFromFile(contents);
        // shallow copy
        Map<String, String> variableOverrides = new HashMap<>(buildVarsOverrides);
        Map<String, String> result = new HashMap<>(variablesFromFile);
        variableOverrides.forEach((k, v) -> result.merge(k, v, (v1, v2) -> v2));
        for (String key : result.keySet()) {
            String propertyOverride = System.getProperty(key);
            if (propertyOverride != null) {
                result.put(key, propertyOverride);
            }
        }
        return result;
    }

    public static InputStream preProcess(InputStream buildConfig, Map<String, String> buildVarsOverrides) {
        String contents = "";
        @SuppressWarnings("resource")
        Scanner s = new Scanner(buildConfig);
        // start of string
        s.useDelimiter("\\A");
        if (s.hasNext()) {
            contents = s.next();
        }
        Map<String, String> variables = readVariables(contents, buildVarsOverrides);
        int passes = 0;
        // We also have to take into account variable used inside variables, just keep going over until
        // they have all expanded, maxTries will be hit if one is left over after that many
        List<String> matches;
        while (!(matches = getAllMatches(contents, "\\{\\{.*}}")).isEmpty()) {
            passes++;
            for (Map.Entry<String, String> entry : variables.entrySet()) {
                String name = "\\{\\{[\\s]*" + entry.getKey() + "[\\s]*}}";
                String value = entry.getValue();
                contents = contents.replaceAll(name, value);
            }
            if (passes > maxTries) {
                StringBuilder leftOvers = new StringBuilder();
                Set<String> setString = new HashSet<>(matches);
                for (String temp : setString) {
                    leftOvers.append(temp).append(" ");
                }
                throw new RuntimeException(
                        "No variable definition for [" + leftOvers.substring(0, leftOvers.length() - 1) + "]");
            }
        }
        InputStream stream = new ByteArrayInputStream(contents.getBytes(StandardCharsets.UTF_8));
        return stream;
    }

    public static PigConfiguration load(File buildConfigFile, Map<String, String> buildVarsOverrides) {
        try (InputStream configStream = new FileInputStream(buildConfigFile)) {
            return load(configStream, buildVarsOverrides);
        } catch (IOException e) {
            throw new RuntimeException("Unable to read build config file " + buildConfigFile.getAbsolutePath(), e);
        }
    }

    public static PigConfiguration load(InputStream configStream) {
        return load(configStream, Collections.emptyMap());
    }

    public static PigConfiguration load(InputStream configStream, Map<String, String> buildVarsOverrides) {
        if (buildVarsOverrides == null) {
            buildVarsOverrides = Collections.emptyMap();
        }
        Yaml yaml = new Yaml();
        try (InputStream in = preProcess(configStream, buildVarsOverrides)) {
            PigConfiguration pigConfiguration = yaml.loadAs(in, PigConfiguration.class);
            pigConfiguration.init();
            return pigConfiguration;
        } catch (IOException e) {
            throw new RuntimeException("Unable to load config file", e);
        }
    }

    @JsonIgnore
    public String getTopLevelDirectoryPrefix() {
        String finalSuffix = "";
        // folder name will become <release-dir>-<version>-<stage>-<suffix>-maven-repository
        if (outputSuffix != null && !outputSuffix.isBlank()) {
            finalSuffix = "-" + outputSuffix;
        }
        return String.format("%s-%s.%s%s-", outputPrefixes.getReleaseDir(), version, product.getStage(), finalSuffix);
    }

    @Deprecated
    public String getVersion() {
        return version;
    }

    @Deprecated
    public String getMilestone() {
        return milestone;
    }

    @Deprecated
    public String getMajorMinor() {
        return majorMinor;
    }

    public boolean isDraft() {
        return draft != null && draft;
    }

    public void setDraft(boolean draft) {
        this.draft = draft;
    }

    @java.lang.SuppressWarnings("all")
    public PigConfiguration() {
    }

    @java.lang.SuppressWarnings("all")
    public ProductConfig getProduct() {
        return this.product;
    }

    @java.lang.SuppressWarnings("all")
    public String getGroup() {
        return this.group;
    }

    @java.lang.SuppressWarnings("all")
    public BuildConfig getDefaultBuildParameters() {
        return this.defaultBuildParameters;
    }

    @java.lang.SuppressWarnings("all")
    public List<BuildConfig> getBuilds() {
        return this.builds;
    }

    @java.lang.SuppressWarnings("all")
    public Output getOutputPrefixes() {
        return this.outputPrefixes;
    }

    @java.lang.SuppressWarnings("all")
    public String getOutputSuffix() {
        return this.outputSuffix;
    }

    @java.lang.SuppressWarnings("all")
    public Flow getFlow() {
        return this.flow;
    }

    @java.lang.SuppressWarnings("all")
    public String getMicro() {
        return this.micro;
    }

    @java.lang.SuppressWarnings("all")
    public Map<String, Map<String, ?>> getAddons() {
        return this.addons;
    }

    @java.lang.SuppressWarnings("all")
    public String getReleaseStorageUrl() {
        return this.releaseStorageUrl;
    }

    @java.lang.SuppressWarnings("all")
    public AlignmentType getTemporaryBuildAlignmentPreference() {
        return this.temporaryBuildAlignmentPreference;
    }

    /**
     * Allow user to override brewTag auto-generated in PNC for a product version
     */
    @java.lang.SuppressWarnings("all")
    public String getBrewTagPrefix() {
        return this.brewTagPrefix;
    }

    @java.lang.SuppressWarnings("all")
    public void setProduct(final ProductConfig product) {
        this.product = product;
    }

    @java.lang.SuppressWarnings("all")
    public void setVersion(final String version) {
        this.version = version;
    }

    @java.lang.SuppressWarnings("all")
    public void setMilestone(final String milestone) {
        this.milestone = milestone;
    }

    @java.lang.SuppressWarnings("all")
    public void setGroup(final String group) {
        this.group = group;
    }

    @java.lang.SuppressWarnings("all")
    public void setDefaultBuildParameters(final BuildConfig defaultBuildParameters) {
        this.defaultBuildParameters = defaultBuildParameters;
    }

    @java.lang.SuppressWarnings("all")
    public void setBuilds(final List<BuildConfig> builds) {
        this.builds = builds;
    }

    @java.lang.SuppressWarnings("all")
    public void setOutputPrefixes(final Output outputPrefixes) {
        this.outputPrefixes = outputPrefixes;
    }

    @java.lang.SuppressWarnings("all")
    public void setOutputSuffix(final String outputSuffix) {
        this.outputSuffix = outputSuffix;
    }

    @java.lang.SuppressWarnings("all")
    public void setFlow(final Flow flow) {
        this.flow = flow;
    }

    @java.lang.SuppressWarnings("all")
    public void setMajorMinor(final String majorMinor) {
        this.majorMinor = majorMinor;
    }

    @java.lang.SuppressWarnings("all")
    public void setMicro(final String micro) {
        this.micro = micro;
    }

    @java.lang.SuppressWarnings("all")
    public void setAddons(final Map<String, Map<String, ?>> addons) {
        this.addons = addons;
    }

    @java.lang.SuppressWarnings("all")
    public void setReleaseStorageUrl(final String releaseStorageUrl) {
        this.releaseStorageUrl = releaseStorageUrl;
    }

    @java.lang.SuppressWarnings("all")
    public void setTemporaryBuildAlignmentPreference(final AlignmentType temporaryBuildAlignmentPreference) {
        this.temporaryBuildAlignmentPreference = temporaryBuildAlignmentPreference;
    }

    /**
     * Allow user to override brewTag auto-generated in PNC for a product version
     */
    @java.lang.SuppressWarnings("all")
    public void setBrewTagPrefix(final String brewTagPrefix) {
        this.brewTagPrefix = brewTagPrefix;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this)
            return true;
        if (!(o instanceof PigConfiguration))
            return false;
        final PigConfiguration other = (PigConfiguration) o;
        if (!other.canEqual((java.lang.Object) this))
            return false;
        final java.lang.Object this$draft = this.draft;
        final java.lang.Object other$draft = other.draft;
        if (this$draft == null ? other$draft != null : !this$draft.equals(other$draft))
            return false;
        final java.lang.Object this$product = this.getProduct();
        final java.lang.Object other$product = other.getProduct();
        if (this$product == null ? other$product != null : !this$product.equals(other$product))
            return false;
        final java.lang.Object this$version = this.getVersion();
        final java.lang.Object other$version = other.getVersion();
        if (this$version == null ? other$version != null : !this$version.equals(other$version))
            return false;
        final java.lang.Object this$milestone = this.getMilestone();
        final java.lang.Object other$milestone = other.getMilestone();
        if (this$milestone == null ? other$milestone != null : !this$milestone.equals(other$milestone))
            return false;
        final java.lang.Object this$group = this.getGroup();
        final java.lang.Object other$group = other.getGroup();
        if (this$group == null ? other$group != null : !this$group.equals(other$group))
            return false;
        final java.lang.Object this$defaultBuildParameters = this.getDefaultBuildParameters();
        final java.lang.Object other$defaultBuildParameters = other.getDefaultBuildParameters();
        if (this$defaultBuildParameters == null ? other$defaultBuildParameters != null
                : !this$defaultBuildParameters.equals(other$defaultBuildParameters))
            return false;
        final java.lang.Object this$builds = this.getBuilds();
        final java.lang.Object other$builds = other.getBuilds();
        if (this$builds == null ? other$builds != null : !this$builds.equals(other$builds))
            return false;
        final java.lang.Object this$outputPrefixes = this.getOutputPrefixes();
        final java.lang.Object other$outputPrefixes = other.getOutputPrefixes();
        if (this$outputPrefixes == null ? other$outputPrefixes != null
                : !this$outputPrefixes.equals(other$outputPrefixes))
            return false;
        final java.lang.Object this$outputSuffix = this.getOutputSuffix();
        final java.lang.Object other$outputSuffix = other.getOutputSuffix();
        if (this$outputSuffix == null ? other$outputSuffix != null : !this$outputSuffix.equals(other$outputSuffix))
            return false;
        final java.lang.Object this$flow = this.getFlow();
        final java.lang.Object other$flow = other.getFlow();
        if (this$flow == null ? other$flow != null : !this$flow.equals(other$flow))
            return false;
        final java.lang.Object this$majorMinor = this.getMajorMinor();
        final java.lang.Object other$majorMinor = other.getMajorMinor();
        if (this$majorMinor == null ? other$majorMinor != null : !this$majorMinor.equals(other$majorMinor))
            return false;
        final java.lang.Object this$micro = this.getMicro();
        final java.lang.Object other$micro = other.getMicro();
        if (this$micro == null ? other$micro != null : !this$micro.equals(other$micro))
            return false;
        final java.lang.Object this$addons = this.getAddons();
        final java.lang.Object other$addons = other.getAddons();
        if (this$addons == null ? other$addons != null : !this$addons.equals(other$addons))
            return false;
        final java.lang.Object this$releaseStorageUrl = this.getReleaseStorageUrl();
        final java.lang.Object other$releaseStorageUrl = other.getReleaseStorageUrl();
        if (this$releaseStorageUrl == null ? other$releaseStorageUrl != null
                : !this$releaseStorageUrl.equals(other$releaseStorageUrl))
            return false;
        final java.lang.Object this$temporaryBuildAlignmentPreference = this.getTemporaryBuildAlignmentPreference();
        final java.lang.Object other$temporaryBuildAlignmentPreference = other.getTemporaryBuildAlignmentPreference();
        if (this$temporaryBuildAlignmentPreference == null ? other$temporaryBuildAlignmentPreference != null
                : !this$temporaryBuildAlignmentPreference.equals(other$temporaryBuildAlignmentPreference))
            return false;
        final java.lang.Object this$brewTagPrefix = this.getBrewTagPrefix();
        final java.lang.Object other$brewTagPrefix = other.getBrewTagPrefix();
        if (this$brewTagPrefix == null ? other$brewTagPrefix != null : !this$brewTagPrefix.equals(other$brewTagPrefix))
            return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof PigConfiguration;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $draft = this.draft;
        result = result * PRIME + ($draft == null ? 43 : $draft.hashCode());
        final java.lang.Object $product = this.getProduct();
        result = result * PRIME + ($product == null ? 43 : $product.hashCode());
        final java.lang.Object $version = this.getVersion();
        result = result * PRIME + ($version == null ? 43 : $version.hashCode());
        final java.lang.Object $milestone = this.getMilestone();
        result = result * PRIME + ($milestone == null ? 43 : $milestone.hashCode());
        final java.lang.Object $group = this.getGroup();
        result = result * PRIME + ($group == null ? 43 : $group.hashCode());
        final java.lang.Object $defaultBuildParameters = this.getDefaultBuildParameters();
        result = result * PRIME + ($defaultBuildParameters == null ? 43 : $defaultBuildParameters.hashCode());
        final java.lang.Object $builds = this.getBuilds();
        result = result * PRIME + ($builds == null ? 43 : $builds.hashCode());
        final java.lang.Object $outputPrefixes = this.getOutputPrefixes();
        result = result * PRIME + ($outputPrefixes == null ? 43 : $outputPrefixes.hashCode());
        final java.lang.Object $outputSuffix = this.getOutputSuffix();
        result = result * PRIME + ($outputSuffix == null ? 43 : $outputSuffix.hashCode());
        final java.lang.Object $flow = this.getFlow();
        result = result * PRIME + ($flow == null ? 43 : $flow.hashCode());
        final java.lang.Object $majorMinor = this.getMajorMinor();
        result = result * PRIME + ($majorMinor == null ? 43 : $majorMinor.hashCode());
        final java.lang.Object $micro = this.getMicro();
        result = result * PRIME + ($micro == null ? 43 : $micro.hashCode());
        final java.lang.Object $addons = this.getAddons();
        result = result * PRIME + ($addons == null ? 43 : $addons.hashCode());
        final java.lang.Object $releaseStorageUrl = this.getReleaseStorageUrl();
        result = result * PRIME + ($releaseStorageUrl == null ? 43 : $releaseStorageUrl.hashCode());
        final java.lang.Object $temporaryBuildAlignmentPreference = this.getTemporaryBuildAlignmentPreference();
        result = result * PRIME
                + ($temporaryBuildAlignmentPreference == null ? 43 : $temporaryBuildAlignmentPreference.hashCode());
        final java.lang.Object $brewTagPrefix = this.getBrewTagPrefix();
        result = result * PRIME + ($brewTagPrefix == null ? 43 : $brewTagPrefix.hashCode());
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "PigConfiguration(product=" + this.getProduct() + ", version=" + this.getVersion() + ", milestone="
                + this.getMilestone() + ", group=" + this.getGroup() + ", defaultBuildParameters="
                + this.getDefaultBuildParameters() + ", builds=" + this.getBuilds() + ", outputPrefixes="
                + this.getOutputPrefixes() + ", outputSuffix=" + this.getOutputSuffix() + ", flow=" + this.getFlow()
                + ", majorMinor=" + this.getMajorMinor() + ", micro=" + this.getMicro() + ", addons=" + this.getAddons()
                + ", releaseStorageUrl=" + this.getReleaseStorageUrl() + ", draft=" + this.draft
                + ", temporaryBuildAlignmentPreference=" + this.getTemporaryBuildAlignmentPreference()
                + ", brewTagPrefix=" + this.getBrewTagPrefix() + ")";
    }
}
