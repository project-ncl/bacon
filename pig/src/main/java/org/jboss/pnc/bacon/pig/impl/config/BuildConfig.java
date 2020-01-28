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
import org.jboss.pnc.bacon.pig.impl.pnc.GitRepoInspector;
import org.jboss.pnc.dto.BuildConfiguration;
import org.jboss.pnc.dto.SCMRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com <br>
 *         Date: 11/28/17
 */
@Data
public class BuildConfig {
    public static final String BUILD_FORCE = "BUILD_FORCE";

    private static final Logger log = LoggerFactory.getLogger(BuildConfig.class);

    private String name;
    private String project;
    private String buildScript;
    private String scmUrl;
    private String externalScmUrl;
    private String scmRevision;
    private String description;
    private String environmentId;
    private List<String> dependencies = new ArrayList<>();
    private Set<String> customPmeParameters = new TreeSet<>();
    private Boolean branchModified;

    public void setDefaults(BuildConfig defaults) {
        try {
            for (Field f : BuildConfig.class.getDeclaredFields()) {
                f.setAccessible(true);
                if (Collection.class.isAssignableFrom(f.getType())) {
                    Collection<?> defaultValues = (Collection<?>) f.get(defaults);
                    // noinspection unchecked
                    ((Collection) f.get(this)).addAll(defaultValues);
                } else {
                    if (f.get(this) == null) {
                        f.set(this, f.get(defaults));
                    }
                }
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Unable to set default values for " + this + " from " + defaults);
        }
    }

    public static Map<String, BuildConfig> mapByName(List<BuildConfig> newConfigs) {
        return newConfigs.stream().collect(toMap(BuildConfig::getName, identity()));
    }

    public void sanitizebuildScript() {
        buildScript = buildScript.trim();
    }

    @JsonIgnore
    public boolean isTheSameAs(BuildConfiguration old) {
        return old != null && StringUtils.equals(name, old.getName()) && StringUtils.equals(project, old.getProject().getName())
                && StringUtils.equals(buildScript, old.getBuildScript())
                && StringUtils.equals(scmRevision, old.getScmRevision()) && environmentId.equals(old.getEnvironment().getId())
                && customPmeParameters.equals(getPmeParameters(old)) && urlsEqual(old.getScmRepository())
                && !isBranchModified(old);
    }

    private Set<String> getPmeParameters(BuildConfiguration old) {
        String parametersAsString = old.getParameters().get("CUSTOM_PME_PARAMETERS");
        return Arrays.stream(parametersAsString.split(",")).collect(Collectors.toSet());
    }

    private synchronized boolean isBranchModified(BuildConfiguration oldVersion) {
        if (branchModified == null) {
            branchModified = GitRepoInspector.isModifiedBranch(oldVersion.getId(),
                    oldVersion.getScmRepository().getInternalUrl(), getScmRevision());
        }
        return branchModified;
    }

    private boolean urlsEqual(SCMRepository repo) {
        return StringUtils.equals(externalScmUrl, repo.getExternalUrl()) || StringUtils.equals(scmUrl, repo.getInternalUrl());
    }

    @JsonIgnore
    public Map<String, String> getGenericParameters(BuildConfiguration oldConfig, boolean forceRebuild) {
        Map<String, String> result = new HashMap<>();

        String oldForceValue = oldConfig == null ? "" : oldConfig.getParameters().getOrDefault(BUILD_FORCE, "");
        String forceValue = forceRebuild ? randomAlphabetic(5) : oldForceValue;

        String dependencyExclusions = String.join(" ", customPmeParameters);

        result.put("CUSTOM_PME_PARAMATERS", dependencyExclusions);
        result.put(BUILD_FORCE, forceValue);

        return result;
    }

    // Work around for NCL-3670
    public String getShortScmURIPath() {
        String scmUrl = getScmUrl() != null ? getScmUrl() : getExternalScmUrl();
        try {
            URI scmUri = new URI(scmUrl);
            return scmUri.getPath();
        } catch (URISyntaxException e) {
            throw new RuntimeException("Invalid scm URI: " + getScmUrl(), e);
        }

    }

    public void validate(List<String> errors) {
        // TODO!
    }

    public boolean matchesRepository(SCMRepository repository) {
        String currentInternalUrl = repository.getInternalUrl();

        // currentInternalUrl cannot be null, if the url from the config is not null, they have to be the same
        if (scmUrl != null) {
            return areSameRepoUrls(scmUrl, currentInternalUrl);
        }

        // if the internal scm url is not provided in the config, check if external scm urls are the same
        return externalScmUrl != null && areSameRepoUrls(repository.getExternalUrl(), externalScmUrl);
    }

    private boolean areSameRepoUrls(String scmUrl1, String scmUrl2) {
        String normalizedUrl1 = normalize(scmUrl1);
        String normalizedUrl2 = normalize(scmUrl2);
        return normalizedUrl1.equals(normalizedUrl2);
    }

    private String normalize(String url) {
        if (!url.endsWith(".git")) {
            url += ".git";
        }

        if (url.startsWith("https")) {
            url = url.replaceFirst("https", "http");
        }

        return url;
    }

    public boolean isUpgradableFrom(BuildConfiguration oldConfig) {
        String oldInternalUrl = oldConfig.getScmRepository().getInternalUrl();
        if (scmUrl != null && !StringUtils.equals(scmUrl, oldInternalUrl)) {
            log.error("Scm url for {} changed from {} to {}. Please update it via UI", name, oldInternalUrl, scmUrl);
            return false;
        }
        return true;
    }
}
