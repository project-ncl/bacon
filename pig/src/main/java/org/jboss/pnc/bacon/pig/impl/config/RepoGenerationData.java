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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com <br>
 *         Date: 1/17/18
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
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
        result.externalAdditionalConfigs = merge(
                defaults.externalAdditionalConfigs,
                overrides.externalAdditionalConfigs);
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
    private List<String> externalAdditionalConfigs = new ArrayList<>();
    private List<String> excludeArtifacts = new ArrayList<>();
    private List<String> sourceBuilds = new ArrayList<>();
    private List<String> excludeSourceBuilds = new ArrayList<>();
    private List<String> filterArtifacts = new ArrayList<>();
    private String bomGroupId;
    private String bomArtifactId;
    /**
     * We use {@link Boolean} instead of boolean since we use the same DTO to set the defaults and overrides. We use
     * null to know that the override is not setting a new value for a default. The default value is set in the
     * {@link #isIncludeJavadoc} method
     *
     * We remove the default Getter method from lombok since we are implementing our own getter method
     */
    private Boolean includeJavadoc;
    /**
     * We use {@link Boolean} instead of boolean since we use the same DTO to set the defaults and overrides. We use
     * null to know that the override is not setting a new value for a default. The default value is set in the
     * {@link #isIncludeLicenses} method
     *
     * We remove the default Getter method from lombok since we are implementing our own getter method
     */
    private Boolean includeLicenses;
    /**
     * We use {@link Boolean} instead of boolean since we use the same DTO to set the defaults and overrides. We use
     * null to know that the override is not setting a new value for a default. The default value is set in the
     * {@link #isIncludeMavenMetadata} method
     *
     * We remove the default Getter method from lombok since we are implementing our own getter method
     */
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

    /**
     * We are handwriting this setter object because SnakeYaml doesn't know how to call a setter method for
     * {@link Boolean} but knows how to do it for boolean when parsing a yaml file.
     *
     * @param includeJavadoc boolean to set
     */
    public void setIncludeJavadoc(boolean includeJavadoc) {
        this.includeJavadoc = includeJavadoc;
    }

    /**
     * We are handwriting this setter object because SnakeYaml doesn't know how to call a setter method for
     * {@link Boolean} but knows how to do it for boolean when parsing a yaml file.
     *
     * @param includeLicenses boolean to set
     */
    public void setIncludeLicenses(boolean includeLicenses) {
        this.includeLicenses = includeLicenses;
    }

    /**
     * We are handwriting this setter object because SnakeYaml doesn't know how to call a setter method for
     * {@link Boolean} but knows how to do it for boolean when parsing a yaml file.
     *
     * @param includeMavenMetadata boolean to set
     */
    public void setIncludeMavenMetadata(boolean includeMavenMetadata) {
        this.includeMavenMetadata = includeMavenMetadata;
    }

    @java.lang.SuppressWarnings("all")
    public RepoGenerationData() {
    }

    @java.lang.SuppressWarnings("all")
    public List<AdditionalArtifactsFromBuild> getAdditionalArtifacts() {
        return this.additionalArtifacts;
    }

    /**
     * list of groupId:artifactId:packaging:version with <strong>exact</strong> version
     */
    @java.lang.SuppressWarnings("all")
    public List<String> getExternalAdditionalArtifacts() {
        return this.externalAdditionalArtifacts;
    }

    @java.lang.SuppressWarnings("all")
    public List<String> getExternalAdditionalConfigs() {
        return this.externalAdditionalConfigs;
    }

    @java.lang.SuppressWarnings("all")
    public List<String> getExcludeArtifacts() {
        return this.excludeArtifacts;
    }

    @java.lang.SuppressWarnings("all")
    public List<String> getSourceBuilds() {
        return this.sourceBuilds;
    }

    @java.lang.SuppressWarnings("all")
    public List<String> getExcludeSourceBuilds() {
        return this.excludeSourceBuilds;
    }

    @java.lang.SuppressWarnings("all")
    public List<String> getFilterArtifacts() {
        return this.filterArtifacts;
    }

    @java.lang.SuppressWarnings("all")
    public String getBomGroupId() {
        return this.bomGroupId;
    }

    @java.lang.SuppressWarnings("all")
    public String getBomArtifactId() {
        return this.bomArtifactId;
    }

    @java.lang.SuppressWarnings("all")
    public String getBuildScript() {
        return this.buildScript;
    }

    @java.lang.SuppressWarnings("all")
    public Set<String> getIgnored() {
        return this.ignored;
    }

    @java.lang.SuppressWarnings("all")
    public String getAdditionalRepo() {
        return this.additionalRepo;
    }

    @java.lang.SuppressWarnings("all")
    public List<Map<String, String>> getStages() {
        return this.stages;
    }

    @java.lang.SuppressWarnings("all")
    public Map<String, String> getParameters() {
        return this.parameters;
    }

    @java.lang.SuppressWarnings("all")
    public List<RepoGenerationData> getSteps() {
        return this.steps;
    }

    @java.lang.SuppressWarnings("all")
    public void setAdditionalArtifacts(final List<AdditionalArtifactsFromBuild> additionalArtifacts) {
        this.additionalArtifacts = additionalArtifacts;
    }

    /**
     * list of groupId:artifactId:packaging:version with <strong>exact</strong> version
     */
    @java.lang.SuppressWarnings("all")
    public void setExternalAdditionalArtifacts(final List<String> externalAdditionalArtifacts) {
        this.externalAdditionalArtifacts = externalAdditionalArtifacts;
    }

    @java.lang.SuppressWarnings("all")
    public void setExternalAdditionalConfigs(final List<String> externalAdditionalConfigs) {
        this.externalAdditionalConfigs = externalAdditionalConfigs;
    }

    @java.lang.SuppressWarnings("all")
    public void setExcludeArtifacts(final List<String> excludeArtifacts) {
        this.excludeArtifacts = excludeArtifacts;
    }

    @java.lang.SuppressWarnings("all")
    public void setSourceBuilds(final List<String> sourceBuilds) {
        this.sourceBuilds = sourceBuilds;
    }

    @java.lang.SuppressWarnings("all")
    public void setExcludeSourceBuilds(final List<String> excludeSourceBuilds) {
        this.excludeSourceBuilds = excludeSourceBuilds;
    }

    @java.lang.SuppressWarnings("all")
    public void setFilterArtifacts(final List<String> filterArtifacts) {
        this.filterArtifacts = filterArtifacts;
    }

    @java.lang.SuppressWarnings("all")
    public void setBomGroupId(final String bomGroupId) {
        this.bomGroupId = bomGroupId;
    }

    @java.lang.SuppressWarnings("all")
    public void setBomArtifactId(final String bomArtifactId) {
        this.bomArtifactId = bomArtifactId;
    }

    @java.lang.SuppressWarnings("all")
    public void setBuildScript(final String buildScript) {
        this.buildScript = buildScript;
    }

    @java.lang.SuppressWarnings("all")
    public void setIgnored(final Set<String> ignored) {
        this.ignored = ignored;
    }

    @java.lang.SuppressWarnings("all")
    public void setAdditionalRepo(final String additionalRepo) {
        this.additionalRepo = additionalRepo;
    }

    @java.lang.SuppressWarnings("all")
    public void setStages(final List<Map<String, String>> stages) {
        this.stages = stages;
    }

    @java.lang.SuppressWarnings("all")
    public void setParameters(final Map<String, String> parameters) {
        this.parameters = parameters;
    }

    @java.lang.SuppressWarnings("all")
    public void setSteps(final List<RepoGenerationData> steps) {
        this.steps = steps;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this)
            return true;
        if (!(o instanceof RepoGenerationData))
            return false;
        final RepoGenerationData other = (RepoGenerationData) o;
        if (!other.canEqual((java.lang.Object) this))
            return false;
        final java.lang.Object this$includeJavadoc = this.includeJavadoc;
        final java.lang.Object other$includeJavadoc = other.includeJavadoc;
        if (this$includeJavadoc == null ? other$includeJavadoc != null
                : !this$includeJavadoc.equals(other$includeJavadoc))
            return false;
        final java.lang.Object this$includeLicenses = this.includeLicenses;
        final java.lang.Object other$includeLicenses = other.includeLicenses;
        if (this$includeLicenses == null ? other$includeLicenses != null
                : !this$includeLicenses.equals(other$includeLicenses))
            return false;
        final java.lang.Object this$includeMavenMetadata = this.includeMavenMetadata;
        final java.lang.Object other$includeMavenMetadata = other.includeMavenMetadata;
        if (this$includeMavenMetadata == null ? other$includeMavenMetadata != null
                : !this$includeMavenMetadata.equals(other$includeMavenMetadata))
            return false;
        final java.lang.Object this$additionalArtifacts = this.getAdditionalArtifacts();
        final java.lang.Object other$additionalArtifacts = other.getAdditionalArtifacts();
        if (this$additionalArtifacts == null ? other$additionalArtifacts != null
                : !this$additionalArtifacts.equals(other$additionalArtifacts))
            return false;
        final java.lang.Object this$externalAdditionalArtifacts = this.getExternalAdditionalArtifacts();
        final java.lang.Object other$externalAdditionalArtifacts = other.getExternalAdditionalArtifacts();
        if (this$externalAdditionalArtifacts == null ? other$externalAdditionalArtifacts != null
                : !this$externalAdditionalArtifacts.equals(other$externalAdditionalArtifacts))
            return false;
        final java.lang.Object this$externalAdditionalConfigs = this.getExternalAdditionalConfigs();
        final java.lang.Object other$externalAdditionalConfigs = other.getExternalAdditionalConfigs();
        if (this$externalAdditionalConfigs == null ? other$externalAdditionalConfigs != null
                : !this$externalAdditionalConfigs.equals(other$externalAdditionalConfigs))
            return false;
        final java.lang.Object this$excludeArtifacts = this.getExcludeArtifacts();
        final java.lang.Object other$excludeArtifacts = other.getExcludeArtifacts();
        if (this$excludeArtifacts == null ? other$excludeArtifacts != null
                : !this$excludeArtifacts.equals(other$excludeArtifacts))
            return false;
        final java.lang.Object this$sourceBuilds = this.getSourceBuilds();
        final java.lang.Object other$sourceBuilds = other.getSourceBuilds();
        if (this$sourceBuilds == null ? other$sourceBuilds != null : !this$sourceBuilds.equals(other$sourceBuilds))
            return false;
        final java.lang.Object this$excludeSourceBuilds = this.getExcludeSourceBuilds();
        final java.lang.Object other$excludeSourceBuilds = other.getExcludeSourceBuilds();
        if (this$excludeSourceBuilds == null ? other$excludeSourceBuilds != null
                : !this$excludeSourceBuilds.equals(other$excludeSourceBuilds))
            return false;
        final java.lang.Object this$filterArtifacts = this.getFilterArtifacts();
        final java.lang.Object other$filterArtifacts = other.getFilterArtifacts();
        if (this$filterArtifacts == null ? other$filterArtifacts != null
                : !this$filterArtifacts.equals(other$filterArtifacts))
            return false;
        final java.lang.Object this$bomGroupId = this.getBomGroupId();
        final java.lang.Object other$bomGroupId = other.getBomGroupId();
        if (this$bomGroupId == null ? other$bomGroupId != null : !this$bomGroupId.equals(other$bomGroupId))
            return false;
        final java.lang.Object this$bomArtifactId = this.getBomArtifactId();
        final java.lang.Object other$bomArtifactId = other.getBomArtifactId();
        if (this$bomArtifactId == null ? other$bomArtifactId != null : !this$bomArtifactId.equals(other$bomArtifactId))
            return false;
        final java.lang.Object this$buildScript = this.getBuildScript();
        final java.lang.Object other$buildScript = other.getBuildScript();
        if (this$buildScript == null ? other$buildScript != null : !this$buildScript.equals(other$buildScript))
            return false;
        final java.lang.Object this$ignored = this.getIgnored();
        final java.lang.Object other$ignored = other.getIgnored();
        if (this$ignored == null ? other$ignored != null : !this$ignored.equals(other$ignored))
            return false;
        final java.lang.Object this$additionalRepo = this.getAdditionalRepo();
        final java.lang.Object other$additionalRepo = other.getAdditionalRepo();
        if (this$additionalRepo == null ? other$additionalRepo != null
                : !this$additionalRepo.equals(other$additionalRepo))
            return false;
        final java.lang.Object this$stages = this.getStages();
        final java.lang.Object other$stages = other.getStages();
        if (this$stages == null ? other$stages != null : !this$stages.equals(other$stages))
            return false;
        final java.lang.Object this$parameters = this.getParameters();
        final java.lang.Object other$parameters = other.getParameters();
        if (this$parameters == null ? other$parameters != null : !this$parameters.equals(other$parameters))
            return false;
        final java.lang.Object this$steps = this.getSteps();
        final java.lang.Object other$steps = other.getSteps();
        if (this$steps == null ? other$steps != null : !this$steps.equals(other$steps))
            return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof RepoGenerationData;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $includeJavadoc = this.includeJavadoc;
        result = result * PRIME + ($includeJavadoc == null ? 43 : $includeJavadoc.hashCode());
        final java.lang.Object $includeLicenses = this.includeLicenses;
        result = result * PRIME + ($includeLicenses == null ? 43 : $includeLicenses.hashCode());
        final java.lang.Object $includeMavenMetadata = this.includeMavenMetadata;
        result = result * PRIME + ($includeMavenMetadata == null ? 43 : $includeMavenMetadata.hashCode());
        final java.lang.Object $additionalArtifacts = this.getAdditionalArtifacts();
        result = result * PRIME + ($additionalArtifacts == null ? 43 : $additionalArtifacts.hashCode());
        final java.lang.Object $externalAdditionalArtifacts = this.getExternalAdditionalArtifacts();
        result = result * PRIME + ($externalAdditionalArtifacts == null ? 43 : $externalAdditionalArtifacts.hashCode());
        final java.lang.Object $externalAdditionalConfigs = this.getExternalAdditionalConfigs();
        result = result * PRIME + ($externalAdditionalConfigs == null ? 43 : $externalAdditionalConfigs.hashCode());
        final java.lang.Object $excludeArtifacts = this.getExcludeArtifacts();
        result = result * PRIME + ($excludeArtifacts == null ? 43 : $excludeArtifacts.hashCode());
        final java.lang.Object $sourceBuilds = this.getSourceBuilds();
        result = result * PRIME + ($sourceBuilds == null ? 43 : $sourceBuilds.hashCode());
        final java.lang.Object $excludeSourceBuilds = this.getExcludeSourceBuilds();
        result = result * PRIME + ($excludeSourceBuilds == null ? 43 : $excludeSourceBuilds.hashCode());
        final java.lang.Object $filterArtifacts = this.getFilterArtifacts();
        result = result * PRIME + ($filterArtifacts == null ? 43 : $filterArtifacts.hashCode());
        final java.lang.Object $bomGroupId = this.getBomGroupId();
        result = result * PRIME + ($bomGroupId == null ? 43 : $bomGroupId.hashCode());
        final java.lang.Object $bomArtifactId = this.getBomArtifactId();
        result = result * PRIME + ($bomArtifactId == null ? 43 : $bomArtifactId.hashCode());
        final java.lang.Object $buildScript = this.getBuildScript();
        result = result * PRIME + ($buildScript == null ? 43 : $buildScript.hashCode());
        final java.lang.Object $ignored = this.getIgnored();
        result = result * PRIME + ($ignored == null ? 43 : $ignored.hashCode());
        final java.lang.Object $additionalRepo = this.getAdditionalRepo();
        result = result * PRIME + ($additionalRepo == null ? 43 : $additionalRepo.hashCode());
        final java.lang.Object $stages = this.getStages();
        result = result * PRIME + ($stages == null ? 43 : $stages.hashCode());
        final java.lang.Object $parameters = this.getParameters();
        result = result * PRIME + ($parameters == null ? 43 : $parameters.hashCode());
        final java.lang.Object $steps = this.getSteps();
        result = result * PRIME + ($steps == null ? 43 : $steps.hashCode());
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "RepoGenerationData(additionalArtifacts=" + this.getAdditionalArtifacts()
                + ", externalAdditionalArtifacts=" + this.getExternalAdditionalArtifacts()
                + ", externalAdditionalConfigs=" + this.getExternalAdditionalConfigs() + ", excludeArtifacts="
                + this.getExcludeArtifacts() + ", sourceBuilds=" + this.getSourceBuilds() + ", excludeSourceBuilds="
                + this.getExcludeSourceBuilds() + ", filterArtifacts=" + this.getFilterArtifacts() + ", bomGroupId="
                + this.getBomGroupId() + ", bomArtifactId=" + this.getBomArtifactId() + ", includeJavadoc="
                + this.includeJavadoc + ", includeLicenses=" + this.includeLicenses + ", includeMavenMetadata="
                + this.includeMavenMetadata + ", buildScript=" + this.getBuildScript() + ", ignored="
                + this.getIgnored() + ", additionalRepo=" + this.getAdditionalRepo() + ", stages=" + this.getStages()
                + ", parameters=" + this.getParameters() + ", steps=" + this.getSteps() + ")";
    }
}
