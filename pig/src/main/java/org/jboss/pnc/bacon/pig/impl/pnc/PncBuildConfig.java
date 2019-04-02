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
package org.jboss.pnc.bacon.pig.impl.pnc;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;
import org.jboss.pnc.bacon.pig.impl.config.BuildConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * <br>
 * Date: 11/29/17
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@ToString(of = {"id", "name"})
public class PncBuildConfig {
    public static final String BUILD_FORCE = "BUILD_FORCE";

    private static final Logger log = LoggerFactory.getLogger(PncBuildConfig.class);

    private Integer id;
    private String name;
    //Get from repository_configuration return 
    private String scmUrl;
    private String externalScmUrl;
    private Boolean preBuildSync;
    private Integer repositoryConfigId;
    @JsonProperty("repository_configuration")
    public void unpackRepositoryConfiguration(Map<String,?> repositoryConfig)
    {
        repositoryConfigId = (Integer)repositoryConfig.get("id");
        scmUrl = (String)repositoryConfig.get("internal_url");
        externalScmUrl = (String)repositoryConfig.get("external_url");
        preBuildSync = (Boolean)repositoryConfig.get("pre_build_sync_enabled");
    }
    @JsonProperty("scm_revision")
    private String scmRevision;
    @JsonProperty("build_script")
    private String buildScript;
    @JsonProperty("dependency_ids")
    private Set<Integer> dependencyIds = new TreeSet<>();
    @JsonProperty("product_version_id")
    private Integer versionId;
    @JsonIgnore
    private String project;
    @JsonIgnore
    private Integer environmentId;
    @JsonIgnore
    private Set<String> customPmeParameters = new TreeSet<>();
    @JsonIgnore
    private String buildForceParameter;

    @JsonProperty("environment")
    public void unpackEnvironmentId(Map<String, ?> environment) {
        environmentId = (Integer) environment.get("id");
    }

    @JsonProperty("generic_parameters")
    public void unpackDependencyOverrides(Map<String, Object> genericParameters) {
        String pmeParams = (String) genericParameters.get("CUSTOM_PME_PARAMETERS");
        if (pmeParams != null) {
            String[] splitParams = pmeParams.trim().split("\\s+");
            customPmeParameters.addAll(Arrays.asList(splitParams));
        }

        buildForceParameter = (String) genericParameters.getOrDefault(BUILD_FORCE, "");
    }
//
//    @JsonProperty("repository_configuration")
//    public void unpackRepositoryConfiguration(Map<String, ?> repoConfig) {
//        externalScmUrl = (String) repoConfig.get("external_url");
//        scmUrl = (String) repoConfig.get("internal_url");
//    }

    @JsonProperty("project")
    public void unpackProject(Map<String, ?> project) {
        this.project = (String) project.get("name");
    }

    public boolean isUpgradableTo(BuildConfig newConfig) {
        if (newConfig.getScmUrl() != null && !StringUtils.equals(newConfig.getScmUrl(), scmUrl)) {
            log.error("Scm url for {} changed from {} to {}. Please update it via UI", name, scmUrl, newConfig.getScmUrl());
            return false;
        }
        return true;
    }
}
