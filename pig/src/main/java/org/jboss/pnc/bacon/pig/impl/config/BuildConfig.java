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

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.jboss.pnc.bacon.common.exception.FatalException;
import org.jboss.pnc.bacon.pig.impl.pnc.GitRepoInspector;
import org.jboss.pnc.bacon.pnc.common.ClientCreator;
import org.jboss.pnc.client.EnvironmentClient;
import org.jboss.pnc.client.RemoteResourceException;
import org.jboss.pnc.dto.BuildConfiguration;
import org.jboss.pnc.dto.Environment;
import org.jboss.pnc.dto.SCMRepository;
import org.jboss.pnc.enums.BuildType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * DTO representing the build-config field in build-config.yaml
 *
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com <br>
 *         Date: 11/28/17
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class BuildConfig {
    public static final String BUILD_FORCE = "BUILD_FORCE";
    private static final Logger log = LoggerFactory.getLogger(BuildConfig.class);
    private static final ClientCreator<EnvironmentClient> CREATOR = new ClientCreator<>(EnvironmentClient::new);
    // Name can contain only alpha-numeric characters, hyphens, underscores and periods and cannot start with a hyphen
    @Pattern(regexp = "^[a-zA-Z0-9\\._][a-zA-Z0-9\\._-]*$")
    @NotBlank
    private String name;
    @NotBlank
    private String project;
    @NotBlank
    private String buildScript;
    private String scmUrl;
    private String externalScmUrl;
    @NotBlank
    private String scmRevision;
    private String description;
    @FieldGroup("environment")
    private String environmentId;
    /**
     * If environmentId and systemImageId are not specified, use 'environmentName' to find the environmentId
     */
    @FieldGroup("environment")
    private String environmentName;
    /**
     * If environmentId is not specified, use 'environmentSystemImageId' to find the environmentId (takes precedence
     * before environmentName)
     */
    @FieldGroup("environment")
    private String systemImageId;
    private List<String> dependencies = new ArrayList<>();
    /**
     * build pod memory in GB
     */
    private Double buildPodMemory;
    private String pigYamlMetadata;
    /**
     * deprecated: use alignmentParameters instead
     */
    private Set<String> customPmeParameters = new TreeSet<>();
    private Set<String> alignmentParameters = new TreeSet<>();
    private Map<String, String> parameters = new HashMap<>();
    private Set<String> extraRepositories = new TreeSet<>();
    private Boolean branchModified;
    private Boolean brewPullActive = false;
    @NotBlank
    private String buildType;
    /**
     * deprecated: use brewBuildName instead
     */
    private String executionRoot;
    private String brewBuildName;
    private String buildCategory;

    /**
     * Set the defaults of buildConfig if not explicitly specified
     * <p>
     * If buildType is not specified in the buildConfig or in the defaults, it is set to MVN
     *
     * @param defaults
     */
    @SuppressWarnings("rawtypes")
    public void setDefaults(BuildConfig defaults) {
        try {
            for (Field f : BuildConfig.class.getDeclaredFields()) {
                f.setAccessible(true);
                if (Collection.class.isAssignableFrom(f.getType())) {
                    Collection<?> defaultValues = (Collection<?>) f.get(defaults);
                    // noinspection unchecked
                    if (f.get(this) != null) {
                        ((Collection) f.get(this)).addAll(defaultValues);
                    } else {
                        log.warn(
                                "Your build-config.yaml \'" + f.getName()
                                        + "\' field is null instead of empty array [].");
                        f.set(this, defaultValues);
                    }
                } else {
                    // don't override with defaults if there is any field in a group that has a value
                    if (f.getAnnotation(FieldGroup.class) != null) {
                        String group = f.getAnnotation(FieldGroup.class).value();
                        Collection<Field> fieldsInSameGroup = getFellowFieldsInGroup(group);
                        if (fieldsInSameGroup.stream().anyMatch(this::isNotNull)) {
                            continue;
                        }
                    }
                    if (f.get(this) == null) {
                        f.set(this, f.get(defaults));
                    }
                }
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Unable to set default values for " + this + " from " + defaults);
        }
        // set buildtype to Maven if not specified
        if (buildType == null) {
            buildType = BuildType.MVN.toString();
        }
    }

    private boolean isNotNull(Field field) {
        try {
            return field.get(this) != null;
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Unable to set default values for " + this);
        }
    }

    public Collection<Field> getFellowFieldsInGroup(String group) {
        Collection<Field> fields = new HashSet<>();
        for (Field field : BuildConfig.class.getDeclaredFields()) {
            FieldGroup groupAnnotation = field.getAnnotation(FieldGroup.class);
            if (groupAnnotation != null && group.equals(groupAnnotation.value())) {
                fields.add(field);
            }
        }
        return fields;
    }

    public static Map<String, BuildConfig> mapByName(List<BuildConfig> newConfigs) {
        return newConfigs.stream().collect(toMap(BuildConfig::getName, identity()));
    }

    public void sanitizebuildScript() {
        buildScript = buildScript.trim();
    }

    @JsonIgnore
    public synchronized boolean isBranchModified(
            BuildConfiguration oldVersion,
            boolean skipBranchCheck,
            boolean temporaryBuild) {
        if (skipBranchCheck) {
            return false;
        }
        if (branchModified == null) {
            branchModified = GitRepoInspector.isModifiedBranch(
                    oldVersion.getId(),
                    oldVersion.getScmRepository().getInternalUrl(),
                    getScmRevision(),
                    temporaryBuild);
        }
        return branchModified;
    }

    @JsonIgnore
    public boolean isTheSameAs(BuildConfiguration old, boolean skipBranchCheck, boolean temporaryBuild) {
        return old != null && StringUtils.equals(name, old.getName())
                && StringUtils.equals(project, old.getProject().getName())
                && StringUtils.equals(buildScript, old.getBuildScript())
                && StringUtils.equals(buildType, old.getBuildType().toString())
                && StringUtils.equals(scmRevision, old.getScmRevision())
                && getEnvironmentId().equals(old.getEnvironment().getId())
                && alignmentParameters.equals(getAlignmentParameters(old)) && urlsEqual(old.getScmRepository())
                && parameters.equals(old.getParameters()) && !isBranchModified(old, skipBranchCheck, temporaryBuild)
                && getBrewPullActive() == old.getBrewPullActive();
    }

    private Set<String> getAlignmentParameters(BuildConfiguration old) {
        String parametersAsString = old.getParameters().get("ALIGNMENT_PARAMETERS");
        if (parametersAsString != null) {
            return Arrays.stream(parametersAsString.split(",")).collect(Collectors.toSet());
        } else {
            return new HashSet<>();
        }
    }

    private boolean urlsEqual(SCMRepository repo) {
        return StringUtils.equals(externalScmUrl, repo.getExternalUrl())
                || StringUtils.equals(scmUrl, repo.getInternalUrl());
    }

    @JsonIgnore
    public Map<String, String> getGenericParameters(BuildConfiguration oldConfig, boolean forceRebuild) {
        if (!customPmeParameters.isEmpty() && alignmentParameters.isEmpty()) {
            log.warn("[Deprecated] Please rename \'customPmeParameters\' section to \'alignmentParameters\'");
            alignmentParameters = customPmeParameters;
        }
        Map<String, String> result = new HashMap<>();
        result.putAll(parameters);
        Optional<String> oldForceValue = oldConfig == null ? Optional.empty()
                : Optional.ofNullable(oldConfig.getParameters().get(BUILD_FORCE));
        Optional<String> forceValue = forceRebuild ? Optional.of(randomAlphabetic(5)) : oldForceValue;
        forceValue.ifPresent(val -> result.put(BUILD_FORCE, val));
        String dependencyExclusions = String.join(" ", alignmentParameters);
        result.put("ALIGNMENT_PARAMETERS", dependencyExclusions);
        if (buildPodMemory != null) {
            result.put("BUILDER_POD_MEMORY", buildPodMemory.toString());
        }
        if (buildCategory != null) {
            result.put("BUILD_CATEGORY", buildCategory.toUpperCase());
        }
        if (pigYamlMetadata != null) {
            String metadata = "";
            try {
                metadata = URLDecoder.decode(pigYamlMetadata, StandardCharsets.UTF_8.toString());
            } catch (UnsupportedEncodingException ex) {
                metadata = pigYamlMetadata;
            }
            result.put("PIG_YAML_METADATA", metadata);
        }
        if (executionRoot != null && brewBuildName == null) {
            log.warn("[Deprecated] Please rename \'executionRoot\' section to \'brewBuildName\'");
            brewBuildName = executionRoot;
        }
        if (brewBuildName != null) {
            result.put("BREW_BUILD_NAME", brewBuildName);
        }
        if (!extraRepositories.isEmpty()) {
            String repositoriesAsString = String.join("\n", extraRepositories);
            result.put("EXTRA_REPOSITORIES", repositoriesAsString);
        }
        return result;
    }

    /**
     * Get the environmentId either as defined in the build-config.yaml or find it via environmentName or
     * environmentSystemImageId.
     *
     * @return environmentId
     */
    @JsonIgnore
    public String getEnvironmentId() {
        if (environmentId != null) {
            return environmentId;
        } else if (systemImageId != null || environmentName != null) {
            String query;
            String exceptionMessage;
            if (systemImageId != null) {
                query = "systemImageId==" + systemImageId;
                exceptionMessage = " an environment with systemImageId of: " + systemImageId;
            } else {
                query = "name==\"" + environmentName + "\";deprecated==false";
                exceptionMessage = " an environment with name of: " + environmentName;
            }
            try (EnvironmentClient client = CREATOR.newClient()) {
                Optional<Environment> environment = client.getAll(Optional.empty(), Optional.of(query))
                        .getAll()
                        .stream()
                        .findFirst();
                if (environment.isPresent()) {
                    environmentId = environment.get().getId();
                    return environment.get().getId();
                } else {
                    throw new FatalException("Cannot find the environment in PNC for {}", exceptionMessage);
                }
            } catch (RemoteResourceException e) {
                throw new FatalException("Exception while talking to PNC", e);
            }
        }
        // if we're here, no environmentId could be found
        return null;
    }

    @JsonIgnore
    public String getRawEnvironmentId() {
        return environmentId;
    }

    @java.lang.SuppressWarnings("all")
    public BuildConfig() {
    }

    @java.lang.SuppressWarnings("all")
    public String getName() {
        return this.name;
    }

    @java.lang.SuppressWarnings("all")
    public String getProject() {
        return this.project;
    }

    @java.lang.SuppressWarnings("all")
    public String getBuildScript() {
        return this.buildScript;
    }

    @java.lang.SuppressWarnings("all")
    public String getScmUrl() {
        return this.scmUrl;
    }

    @java.lang.SuppressWarnings("all")
    public String getExternalScmUrl() {
        return this.externalScmUrl;
    }

    @java.lang.SuppressWarnings("all")
    public String getScmRevision() {
        return this.scmRevision;
    }

    @java.lang.SuppressWarnings("all")
    public String getDescription() {
        return this.description;
    }

    /**
     * If environmentId and systemImageId are not specified, use 'environmentName' to find the environmentId
     */
    @java.lang.SuppressWarnings("all")
    public String getEnvironmentName() {
        return this.environmentName;
    }

    /**
     * If environmentId is not specified, use 'environmentSystemImageId' to find the environmentId (takes precedence
     * before environmentName)
     */
    @java.lang.SuppressWarnings("all")
    public String getSystemImageId() {
        return this.systemImageId;
    }

    @java.lang.SuppressWarnings("all")
    public List<String> getDependencies() {
        return this.dependencies;
    }

    /**
     * build pod memory in GB
     */
    @java.lang.SuppressWarnings("all")
    public Double getBuildPodMemory() {
        return this.buildPodMemory;
    }

    @java.lang.SuppressWarnings("all")
    public String getPigYamlMetadata() {
        return this.pigYamlMetadata;
    }

    /**
     * deprecated: use alignmentParameters instead
     */
    @java.lang.SuppressWarnings("all")
    public Set<String> getCustomPmeParameters() {
        return this.customPmeParameters;
    }

    @java.lang.SuppressWarnings("all")
    public Set<String> getAlignmentParameters() {
        return this.alignmentParameters;
    }

    @java.lang.SuppressWarnings("all")
    public Map<String, String> getParameters() {
        return this.parameters;
    }

    @java.lang.SuppressWarnings("all")
    public Set<String> getExtraRepositories() {
        return this.extraRepositories;
    }

    @java.lang.SuppressWarnings("all")
    public Boolean getBranchModified() {
        return this.branchModified;
    }

    @java.lang.SuppressWarnings("all")
    public Boolean getBrewPullActive() {
        return this.brewPullActive;
    }

    @java.lang.SuppressWarnings("all")
    public String getBuildType() {
        return this.buildType;
    }

    /**
     * deprecated: use brewBuildName instead
     */
    @java.lang.SuppressWarnings("all")
    public String getExecutionRoot() {
        return this.executionRoot;
    }

    @java.lang.SuppressWarnings("all")
    public String getBrewBuildName() {
        return this.brewBuildName;
    }

    @java.lang.SuppressWarnings("all")
    public String getBuildCategory() {
        return this.buildCategory;
    }

    @java.lang.SuppressWarnings("all")
    public void setName(final String name) {
        this.name = name;
    }

    @java.lang.SuppressWarnings("all")
    public void setProject(final String project) {
        this.project = project;
    }

    @java.lang.SuppressWarnings("all")
    public void setBuildScript(final String buildScript) {
        this.buildScript = buildScript;
    }

    @java.lang.SuppressWarnings("all")
    public void setScmUrl(final String scmUrl) {
        this.scmUrl = scmUrl;
    }

    @java.lang.SuppressWarnings("all")
    public void setExternalScmUrl(final String externalScmUrl) {
        this.externalScmUrl = externalScmUrl;
    }

    @java.lang.SuppressWarnings("all")
    public void setScmRevision(final String scmRevision) {
        this.scmRevision = scmRevision;
    }

    @java.lang.SuppressWarnings("all")
    public void setDescription(final String description) {
        this.description = description;
    }

    @java.lang.SuppressWarnings("all")
    public void setEnvironmentId(final String environmentId) {
        this.environmentId = environmentId;
    }

    /**
     * If environmentId and systemImageId are not specified, use 'environmentName' to find the environmentId
     */
    @java.lang.SuppressWarnings("all")
    public void setEnvironmentName(final String environmentName) {
        this.environmentName = environmentName;
    }

    /**
     * If environmentId is not specified, use 'environmentSystemImageId' to find the environmentId (takes precedence
     * before environmentName)
     */
    @java.lang.SuppressWarnings("all")
    public void setSystemImageId(final String systemImageId) {
        this.systemImageId = systemImageId;
    }

    @java.lang.SuppressWarnings("all")
    public void setDependencies(final List<String> dependencies) {
        this.dependencies = dependencies;
    }

    /**
     * build pod memory in GB
     */
    @java.lang.SuppressWarnings("all")
    public void setBuildPodMemory(final Double buildPodMemory) {
        this.buildPodMemory = buildPodMemory;
    }

    @java.lang.SuppressWarnings("all")
    public void setPigYamlMetadata(final String pigYamlMetadata) {
        this.pigYamlMetadata = pigYamlMetadata;
    }

    /**
     * deprecated: use alignmentParameters instead
     */
    @java.lang.SuppressWarnings("all")
    public void setCustomPmeParameters(final Set<String> customPmeParameters) {
        this.customPmeParameters = customPmeParameters;
    }

    @java.lang.SuppressWarnings("all")
    public void setAlignmentParameters(final Set<String> alignmentParameters) {
        this.alignmentParameters = alignmentParameters;
    }

    @java.lang.SuppressWarnings("all")
    public void setParameters(final Map<String, String> parameters) {
        this.parameters = parameters;
    }

    @java.lang.SuppressWarnings("all")
    public void setExtraRepositories(final Set<String> extraRepositories) {
        this.extraRepositories = extraRepositories;
    }

    @java.lang.SuppressWarnings("all")
    public void setBranchModified(final Boolean branchModified) {
        this.branchModified = branchModified;
    }

    @java.lang.SuppressWarnings("all")
    public void setBrewPullActive(final Boolean brewPullActive) {
        this.brewPullActive = brewPullActive;
    }

    @java.lang.SuppressWarnings("all")
    public void setBuildType(final String buildType) {
        this.buildType = buildType;
    }

    /**
     * deprecated: use brewBuildName instead
     */
    @java.lang.SuppressWarnings("all")
    public void setExecutionRoot(final String executionRoot) {
        this.executionRoot = executionRoot;
    }

    @java.lang.SuppressWarnings("all")
    public void setBrewBuildName(final String brewBuildName) {
        this.brewBuildName = brewBuildName;
    }

    @java.lang.SuppressWarnings("all")
    public void setBuildCategory(final String buildCategory) {
        this.buildCategory = buildCategory;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this)
            return true;
        if (!(o instanceof BuildConfig))
            return false;
        final BuildConfig other = (BuildConfig) o;
        if (!other.canEqual((java.lang.Object) this))
            return false;
        final java.lang.Object this$buildPodMemory = this.getBuildPodMemory();
        final java.lang.Object other$buildPodMemory = other.getBuildPodMemory();
        if (this$buildPodMemory == null ? other$buildPodMemory != null
                : !this$buildPodMemory.equals(other$buildPodMemory))
            return false;
        final java.lang.Object this$branchModified = this.getBranchModified();
        final java.lang.Object other$branchModified = other.getBranchModified();
        if (this$branchModified == null ? other$branchModified != null
                : !this$branchModified.equals(other$branchModified))
            return false;
        final java.lang.Object this$brewPullActive = this.getBrewPullActive();
        final java.lang.Object other$brewPullActive = other.getBrewPullActive();
        if (this$brewPullActive == null ? other$brewPullActive != null
                : !this$brewPullActive.equals(other$brewPullActive))
            return false;
        final java.lang.Object this$name = this.getName();
        final java.lang.Object other$name = other.getName();
        if (this$name == null ? other$name != null : !this$name.equals(other$name))
            return false;
        final java.lang.Object this$project = this.getProject();
        final java.lang.Object other$project = other.getProject();
        if (this$project == null ? other$project != null : !this$project.equals(other$project))
            return false;
        final java.lang.Object this$buildScript = this.getBuildScript();
        final java.lang.Object other$buildScript = other.getBuildScript();
        if (this$buildScript == null ? other$buildScript != null : !this$buildScript.equals(other$buildScript))
            return false;
        final java.lang.Object this$scmUrl = this.getScmUrl();
        final java.lang.Object other$scmUrl = other.getScmUrl();
        if (this$scmUrl == null ? other$scmUrl != null : !this$scmUrl.equals(other$scmUrl))
            return false;
        final java.lang.Object this$externalScmUrl = this.getExternalScmUrl();
        final java.lang.Object other$externalScmUrl = other.getExternalScmUrl();
        if (this$externalScmUrl == null ? other$externalScmUrl != null
                : !this$externalScmUrl.equals(other$externalScmUrl))
            return false;
        final java.lang.Object this$scmRevision = this.getScmRevision();
        final java.lang.Object other$scmRevision = other.getScmRevision();
        if (this$scmRevision == null ? other$scmRevision != null : !this$scmRevision.equals(other$scmRevision))
            return false;
        final java.lang.Object this$description = this.getDescription();
        final java.lang.Object other$description = other.getDescription();
        if (this$description == null ? other$description != null : !this$description.equals(other$description))
            return false;
        final java.lang.Object this$environmentId = this.getEnvironmentId();
        final java.lang.Object other$environmentId = other.getEnvironmentId();
        if (this$environmentId == null ? other$environmentId != null : !this$environmentId.equals(other$environmentId))
            return false;
        final java.lang.Object this$environmentName = this.getEnvironmentName();
        final java.lang.Object other$environmentName = other.getEnvironmentName();
        if (this$environmentName == null ? other$environmentName != null
                : !this$environmentName.equals(other$environmentName))
            return false;
        final java.lang.Object this$systemImageId = this.getSystemImageId();
        final java.lang.Object other$systemImageId = other.getSystemImageId();
        if (this$systemImageId == null ? other$systemImageId != null : !this$systemImageId.equals(other$systemImageId))
            return false;
        final java.lang.Object this$dependencies = this.getDependencies();
        final java.lang.Object other$dependencies = other.getDependencies();
        if (this$dependencies == null ? other$dependencies != null : !this$dependencies.equals(other$dependencies))
            return false;
        final java.lang.Object this$pigYamlMetadata = this.getPigYamlMetadata();
        final java.lang.Object other$pigYamlMetadata = other.getPigYamlMetadata();
        if (this$pigYamlMetadata == null ? other$pigYamlMetadata != null
                : !this$pigYamlMetadata.equals(other$pigYamlMetadata))
            return false;
        final java.lang.Object this$customPmeParameters = this.getCustomPmeParameters();
        final java.lang.Object other$customPmeParameters = other.getCustomPmeParameters();
        if (this$customPmeParameters == null ? other$customPmeParameters != null
                : !this$customPmeParameters.equals(other$customPmeParameters))
            return false;
        final java.lang.Object this$alignmentParameters = this.getAlignmentParameters();
        final java.lang.Object other$alignmentParameters = other.getAlignmentParameters();
        if (this$alignmentParameters == null ? other$alignmentParameters != null
                : !this$alignmentParameters.equals(other$alignmentParameters))
            return false;
        final java.lang.Object this$parameters = this.getParameters();
        final java.lang.Object other$parameters = other.getParameters();
        if (this$parameters == null ? other$parameters != null : !this$parameters.equals(other$parameters))
            return false;
        final java.lang.Object this$extraRepositories = this.getExtraRepositories();
        final java.lang.Object other$extraRepositories = other.getExtraRepositories();
        if (this$extraRepositories == null ? other$extraRepositories != null
                : !this$extraRepositories.equals(other$extraRepositories))
            return false;
        final java.lang.Object this$buildType = this.getBuildType();
        final java.lang.Object other$buildType = other.getBuildType();
        if (this$buildType == null ? other$buildType != null : !this$buildType.equals(other$buildType))
            return false;
        final java.lang.Object this$executionRoot = this.getExecutionRoot();
        final java.lang.Object other$executionRoot = other.getExecutionRoot();
        if (this$executionRoot == null ? other$executionRoot != null : !this$executionRoot.equals(other$executionRoot))
            return false;
        final java.lang.Object this$brewBuildName = this.getBrewBuildName();
        final java.lang.Object other$brewBuildName = other.getBrewBuildName();
        if (this$brewBuildName == null ? other$brewBuildName != null : !this$brewBuildName.equals(other$brewBuildName))
            return false;
        final java.lang.Object this$buildCategory = this.getBuildCategory();
        final java.lang.Object other$buildCategory = other.getBuildCategory();
        if (this$buildCategory == null ? other$buildCategory != null : !this$buildCategory.equals(other$buildCategory))
            return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof BuildConfig;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $buildPodMemory = this.getBuildPodMemory();
        result = result * PRIME + ($buildPodMemory == null ? 43 : $buildPodMemory.hashCode());
        final java.lang.Object $branchModified = this.getBranchModified();
        result = result * PRIME + ($branchModified == null ? 43 : $branchModified.hashCode());
        final java.lang.Object $brewPullActive = this.getBrewPullActive();
        result = result * PRIME + ($brewPullActive == null ? 43 : $brewPullActive.hashCode());
        final java.lang.Object $name = this.getName();
        result = result * PRIME + ($name == null ? 43 : $name.hashCode());
        final java.lang.Object $project = this.getProject();
        result = result * PRIME + ($project == null ? 43 : $project.hashCode());
        final java.lang.Object $buildScript = this.getBuildScript();
        result = result * PRIME + ($buildScript == null ? 43 : $buildScript.hashCode());
        final java.lang.Object $scmUrl = this.getScmUrl();
        result = result * PRIME + ($scmUrl == null ? 43 : $scmUrl.hashCode());
        final java.lang.Object $externalScmUrl = this.getExternalScmUrl();
        result = result * PRIME + ($externalScmUrl == null ? 43 : $externalScmUrl.hashCode());
        final java.lang.Object $scmRevision = this.getScmRevision();
        result = result * PRIME + ($scmRevision == null ? 43 : $scmRevision.hashCode());
        final java.lang.Object $description = this.getDescription();
        result = result * PRIME + ($description == null ? 43 : $description.hashCode());
        final java.lang.Object $environmentId = this.getEnvironmentId();
        result = result * PRIME + ($environmentId == null ? 43 : $environmentId.hashCode());
        final java.lang.Object $environmentName = this.getEnvironmentName();
        result = result * PRIME + ($environmentName == null ? 43 : $environmentName.hashCode());
        final java.lang.Object $systemImageId = this.getSystemImageId();
        result = result * PRIME + ($systemImageId == null ? 43 : $systemImageId.hashCode());
        final java.lang.Object $dependencies = this.getDependencies();
        result = result * PRIME + ($dependencies == null ? 43 : $dependencies.hashCode());
        final java.lang.Object $pigYamlMetadata = this.getPigYamlMetadata();
        result = result * PRIME + ($pigYamlMetadata == null ? 43 : $pigYamlMetadata.hashCode());
        final java.lang.Object $customPmeParameters = this.getCustomPmeParameters();
        result = result * PRIME + ($customPmeParameters == null ? 43 : $customPmeParameters.hashCode());
        final java.lang.Object $alignmentParameters = this.getAlignmentParameters();
        result = result * PRIME + ($alignmentParameters == null ? 43 : $alignmentParameters.hashCode());
        final java.lang.Object $parameters = this.getParameters();
        result = result * PRIME + ($parameters == null ? 43 : $parameters.hashCode());
        final java.lang.Object $extraRepositories = this.getExtraRepositories();
        result = result * PRIME + ($extraRepositories == null ? 43 : $extraRepositories.hashCode());
        final java.lang.Object $buildType = this.getBuildType();
        result = result * PRIME + ($buildType == null ? 43 : $buildType.hashCode());
        final java.lang.Object $executionRoot = this.getExecutionRoot();
        result = result * PRIME + ($executionRoot == null ? 43 : $executionRoot.hashCode());
        final java.lang.Object $brewBuildName = this.getBrewBuildName();
        result = result * PRIME + ($brewBuildName == null ? 43 : $brewBuildName.hashCode());
        final java.lang.Object $buildCategory = this.getBuildCategory();
        result = result * PRIME + ($buildCategory == null ? 43 : $buildCategory.hashCode());
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "BuildConfig(name=" + this.getName() + ", project=" + this.getProject() + ", buildScript="
                + this.getBuildScript() + ", scmUrl=" + this.getScmUrl() + ", externalScmUrl="
                + this.getExternalScmUrl() + ", scmRevision=" + this.getScmRevision() + ", description="
                + this.getDescription() + ", environmentId=" + this.getEnvironmentId() + ", environmentName="
                + this.getEnvironmentName() + ", systemImageId=" + this.getSystemImageId() + ", dependencies="
                + this.getDependencies() + ", buildPodMemory=" + this.getBuildPodMemory() + ", pigYamlMetadata="
                + this.getPigYamlMetadata() + ", customPmeParameters=" + this.getCustomPmeParameters()
                + ", alignmentParameters=" + this.getAlignmentParameters() + ", parameters=" + this.getParameters()
                + ", extraRepositories=" + this.getExtraRepositories() + ", branchModified=" + this.getBranchModified()
                + ", brewPullActive=" + this.getBrewPullActive() + ", buildType=" + this.getBuildType()
                + ", executionRoot=" + this.getExecutionRoot() + ", brewBuildName=" + this.getBrewBuildName()
                + ", buildCategory=" + this.getBuildCategory() + ")";
    }
}
