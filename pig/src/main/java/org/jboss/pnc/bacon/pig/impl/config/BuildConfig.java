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

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
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

import javax.validation.constraints.NotBlank;

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

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;

/**
 * DTO representing the build-config field in build-config.yaml
 *
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com <br>
 *         Date: 11/28/17
 */
@Data
public class BuildConfig {
    public static final String BUILD_FORCE = "BUILD_FORCE";

    private static final Logger log = LoggerFactory.getLogger(BuildConfig.class);
    private static final ClientCreator<EnvironmentClient> CREATOR = new ClientCreator<>(EnvironmentClient::new);

    private @NotBlank String name;
    private @NotBlank String project;
    private @NotBlank String buildScript;
    private String scmUrl;
    private String externalScmUrl;
    private @NotBlank String scmRevision;
    private String description;
    private String environmentId;

    /**
     * If environmentId and systemImageId are not specified, use 'environmentName' to find the environmentId
     */
    private String environmentName;

    /**
     * If environmentId is not specified, use 'environmentSystemImageId' to find the environmentId (takes precedence
     * before environmentName)
     */
    private String systemImageId;

    private List<String> dependencies = new ArrayList<>();

    /**
     * build pod memory in GB
     */
    private Double buildPodMemory;
    private String pigYamlMetadata;

    /** deprecated: use alignmentParameters instead */
    private Set<String> customPmeParameters = new TreeSet<>();
    private Set<String> alignmentParameters = new TreeSet<>();

    private Set<String> extraRepositories = new TreeSet<>();
    private Boolean branchModified;
    private @NotBlank String buildType;

    /** deprecated: use brewBuildName instead */
    private String executionRoot;
    private String brewBuildName;

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
                                "Your build-config.yaml '" + f.getName()
                                        + "' field is null instead of empty array [].");
                        f.set(this, defaultValues);
                    }
                } else {
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
                && !isBranchModified(old, skipBranchCheck, temporaryBuild);
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
            log.warn("[Deprecated] Please rename 'customPmeParameters' section to 'alignmentParameters'");
            alignmentParameters = customPmeParameters;
        }
        Map<String, String> result = new HashMap<>();

        Optional<String> oldForceValue = oldConfig == null ? Optional.empty()
                : Optional.ofNullable(oldConfig.getParameters().get(BUILD_FORCE));
        Optional<String> forceValue = forceRebuild ? Optional.of(randomAlphabetic(5)) : oldForceValue;
        forceValue.ifPresent(val -> result.put(BUILD_FORCE, val));

        String dependencyExclusions = String.join(" ", alignmentParameters);
        result.put("ALIGNMENT_PARAMETERS", dependencyExclusions);

        if (buildPodMemory != null) {
            result.put("BUILDER_POD_MEMORY", buildPodMemory.toString());
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
            log.warn("[Deprecated] Please rename 'executionRoot' section to 'brewBuildName'");
            brewBuildName = executionRoot;
        }

        if (brewBuildName != null) {
            result.put("BREW_BUILD_NAME", brewBuildName);
        }

        if (!extraRepositories.isEmpty()) {
            String repositoriesAsString = String.join("\\n", extraRepositories);
            result.put("EXTRA_REPOSITORIES", repositoriesAsString);
        }

        return result;
    }

    public boolean isUpgradableFrom(BuildConfiguration oldConfig) {
        String oldInternalUrl = oldConfig.getScmRepository().getInternalUrl();
        if (scmUrl != null && !StringUtils.equals(scmUrl, oldInternalUrl)) {
            log.error("Scm url for {} changed from {} to {}. Please update it via UI", name, oldInternalUrl, scmUrl);
            return false;
        }
        return true;
    }

    /**
     * Get the environmentId either as defined in the build-config.yaml or find it via environmentName or
     * environmentSystemImageId.
     *
     * @return environmentId
     */
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
}
