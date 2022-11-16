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

import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
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

    public static RepoGenerationData merge(RepoGenerationData defaults, RepoGenerationData overrides) {
        if (defaults == null) {
            return overrides;
        }
        if (overrides == null) {
            return defaults;
        }

        if (!overrides.steps.isEmpty()) {
            throw new IllegalArgumentException("Overrides cannot have steps");
        }
        if (overrides.getStrategy() != null && overrides.getStrategy().equals(defaults.getStrategy())) {
            throw new IllegalArgumentException("Overrides cannot have a different strategy than the default one");
        }

        final RepoGenerationData result = new RepoGenerationData();

        result.setSourceBuild(override(defaults.getSourceBuild(), overrides.getSourceBuild()));
        result.setSourceArtifact(override(defaults.getSourceArtifact(), overrides.getSourceArtifact()));
        result.setStrategy(override(defaults.getStrategy(), overrides.getStrategy()));

        result.additionalArtifacts = merge(defaults.additionalArtifacts, overrides.additionalArtifacts);
        result.externalAdditionalArtifacts = merge(
                defaults.externalAdditionalArtifacts,
                overrides.externalAdditionalArtifacts);
        result.excludeArtifacts = merge(defaults.excludeArtifacts, overrides.excludeArtifacts);
        result.sourceBuilds = merge(defaults.sourceBuilds, overrides.sourceBuilds);
        result.excludeSourceBuilds = merge(defaults.excludeSourceBuilds, overrides.excludeSourceBuilds);
        result.filterArtifacts = merge(defaults.filterArtifacts, overrides.filterArtifacts);
        result.bomGroupId = override(defaults.bomGroupId, overrides.bomGroupId);
        result.bomArtifactId = override(defaults.bomArtifactId, overrides.bomArtifactId);
        result.includeJavadoc = override(defaults.includeJavadoc, overrides.includeJavadoc);
        result.includeLicenses = override(defaults.includeLicenses, overrides.includeLicenses);
        result.includeMavenMetadata = override(defaults.includeMavenMetadata, overrides.includeMavenMetadata);
        result.buildScript = override(defaults.buildScript, overrides.buildScript);
        result.ignored = override(defaults.ignored, overrides.ignored);
        result.additionalRepo = override(defaults.additionalRepo, overrides.additionalRepo);
        result.stages = merge(defaults.stages, overrides.stages);
        result.parameters = merge(defaults.parameters, overrides.parameters);

        return result;
    }

    private static <T> List<T> merge(List<T> defaults, List<T> overrides) {
        final List<T> r = new ArrayList<>(defaults.size() + overrides.size());
        r.addAll(defaults);
        r.addAll(overrides);
        return r;
    }

    private static <T> Set<T> override(Set<T> defaults, Set<T> overrides) {
        final Set<T> r = new HashSet<>(defaults.size() + overrides.size());
        r.addAll(defaults);
        r.addAll(overrides);
        return r;
    }

    private static Map<String, String> merge(Map<String, String> defaults, Map<String, String> overrides) {
        final Map<String, String> r = new HashMap<>(defaults.size() + overrides.size());
        r.putAll(defaults);
        for (Map.Entry<String, String> e : overrides.entrySet()) {
            String v = r.get(e.getKey());
            if (v == null || v.isBlank()) {
                r.put(e.getKey(), e.getValue());
            } else {
                r.put(e.getKey(), v + "," + e.getValue());
            }
        }
        return r;
    }

    private static <T> T override(T defaults, T overrides) {
        return overrides == null ? defaults : overrides;
    }

    private List<AdditionalArtifactsFromBuild> additionalArtifacts = new ArrayList<>();

    /**
     * list of groupId:artifactId:packaging:version with <strong>exact</strong> version
     */
    private List<String> externalAdditionalArtifacts = new ArrayList<>();
    private List<String> excludeArtifacts = new ArrayList<>();
    private List<String> sourceBuilds = new ArrayList<>();
    private List<String> excludeSourceBuilds = new ArrayList<>();
    private List<String> filterArtifacts = new ArrayList<>();
    private String bomGroupId;
    private String bomArtifactId;
    @Setter(AccessLevel.NONE)
    private Boolean includeJavadoc;
    @Setter(AccessLevel.NONE)
    private Boolean includeLicenses;
    @Setter(AccessLevel.NONE)
    private Boolean includeMavenMetadata;
    private String buildScript;
    private Set<String> ignored = new HashSet<>();
    private String additionalRepo;
    private List<Map<String, String>> stages = List.of();
    private Map<String, String> parameters = Map.of();
    private List<RepoGenerationData> steps = new ArrayList<>();

    public boolean isIncludeJavadoc() {
        return includeJavadoc == null ? false : includeJavadoc;
    }

    public boolean isIncludeLicenses() {
        return includeLicenses == null ? false : includeLicenses;
    }

    public boolean isIncludeMavenMetadata() {
        return includeMavenMetadata == null ? false : includeMavenMetadata;
    }
}
