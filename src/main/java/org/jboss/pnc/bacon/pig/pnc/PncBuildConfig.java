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
package org.jboss.pnc.bacon.pig.pnc;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;
import org.jboss.prod.generator.config.build.BuildConfig;
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
// TODO: replace with PNC rest DTO
public class PncBuildConfig {
    private static final Logger log = LoggerFactory.getLogger(PncBuildConfig.class);

    private Integer id;
    private String name;
    //Get from repository_configuration return 
    private String scmUrl;
    private String externalScmUrl;
    private Boolean preBuildSync;
    private Integer repositoryConfigId;
    @JsonProperty("repositoryConfiguration")
    public void unpackRepositoryConfiguration(Map<String,?> repositoryConfig)
    {
        repositoryConfigId = (Integer) repositoryConfig.get("id");
        scmUrl = (String) repositoryConfig.get("internalUrl");
        externalScmUrl = (String) repositoryConfig.get("externalUrl");
        preBuildSync = (Boolean) repositoryConfig.get("preBuildSyncEnabled");
    }
    private String scmRevision;
    private String buildScript;
    private Set<Integer> dependencyIds = new TreeSet<>();
    private Integer versionId;
    @JsonIgnore
    private String project;
    @JsonIgnore
    private Integer environmentId;
    @JsonIgnore
    private Set<String> customPmeParameters = new TreeSet<>();

    @JsonProperty("environment")
    public void unpackEnvironmentId(Map<String, ?> environment) {
        environmentId = (Integer) environment.get("id");
    }

    @JsonProperty("genericParameters")
    public void unpackDependencyOverrides(Map<String, ?> genericParameters) {
        String pmeParams = (String) genericParameters.get("CUSTOM_PME_PARAMETERS");
        if (pmeParams != null) {
            String[] splitParams = pmeParams.trim().split("\\s+");
            customPmeParameters.addAll(Arrays.asList(splitParams));
        }
    }

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
