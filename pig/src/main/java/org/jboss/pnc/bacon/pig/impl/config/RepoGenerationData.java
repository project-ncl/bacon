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

import lombok.Data;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com <br>
 *         Date: 1/17/18
 */
@Data
public class RepoGenerationData extends GenerationData<RepoGenerationStrategy> {
    private List<AdditionalArtifactsFromBuild> additionalArtifacts = new ArrayList<>();

    /**
     * list of groupId:artifactId:packaging:version with <strong>exact</strong> version
     */
    private List<String> externalAdditionalArtifacts = new ArrayList<>();
    private List<String> excludeArtifacts = new ArrayList<>();
    private List<String> sourceBuilds = new ArrayList<>();
    private String bomGroupId;
    private String bomArtifactId;
    private boolean includeJavadoc;
    private boolean includeLicenses;
    private boolean includeMavenMetadata;
    private String buildScript;
    private Set<String> ignored = new HashSet<>();
    private String additionalRepo;
    private List<Map<String, String>> stages;
    private Map<String, String> parameters;
}
