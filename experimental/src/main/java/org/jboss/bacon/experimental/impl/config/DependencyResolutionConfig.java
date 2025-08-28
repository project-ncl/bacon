package org.jboss.bacon.experimental.impl.config;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Configuration for the autobuilder dependency resolution.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class DependencyResolutionConfig {
    /**
     * List of toplevel artifacts that should be analyzed and built.
     */
    @NotNull
    private Set<String> analyzeArtifacts = Set.of();
    /**
     * Bom that should be analyzed and which artifacts should be built.
     */
    private String analyzeBOM;
    /**
     * Whether optional dependencies should be included in the analysis.
     */
    private boolean includeOptionalDependencies = true;
    /**
     * List of artifacts that should be excluded from the analysis.
     *
     * The matching artifact and it's dependencies will be omitted from the resulting build tree. The format is
     * G:A:[C:T:]V where every part can be replaced with '*'.
     */
    @NotNull
    private Set<String> excludeArtifacts = Set.of();
    /**
     * If set to true, all -redhat-X artifacts and their dependencies are omitted from the resulting build tree. Note
     * that this option has effect only when {@link #rebuildNonAutoBuilds} is true.
     */
    private boolean excludeProductizedArtifacts = false;
    /**
     * List of artifacts that should not be excluded from the analysis.
     *
     * The format is G:A:[C:T:]V where every part can be replaced with '*'.
     */
    @NotNull
    private Set<String> includeArtifacts = Set.of();
    /**
     * The list of URLs to use for the SCM recipes. The SCM recipes are used for determining SCM coordinates for
     * project.
     */
    private List<String> recipeRepos = List.of();
    /**
     * When enabled, autobuilder will rebuild existing builds, if they are not managed by autobuilder (Build Configs
     * with -AUTOBUILD suffix) and will manage them (create a copy of the existing build's Build Config with -AUTOBUILD
     * suffix). This is useful to make sure whole dependency tree is build correctly.
     */
    private boolean rebuildNonAutoBuilds = false;
    /**
     * Path to the maven settings.xml file to use instead of the system one.
     */
    private Path mavenSettings;

    @java.lang.SuppressWarnings("all")
    public DependencyResolutionConfig() {
    }

    /**
     * List of toplevel artifacts that should be analyzed and built.
     */
    @java.lang.SuppressWarnings("all")
    public Set<String> getAnalyzeArtifacts() {
        return this.analyzeArtifacts;
    }

    /**
     * Bom that should be analyzed and which artifacts should be built.
     */
    @java.lang.SuppressWarnings("all")
    public String getAnalyzeBOM() {
        return this.analyzeBOM;
    }

    /**
     * Whether optional dependencies should be included in the analysis.
     */
    @java.lang.SuppressWarnings("all")
    public boolean isIncludeOptionalDependencies() {
        return this.includeOptionalDependencies;
    }

    /**
     * List of artifacts that should be excluded from the analysis.
     *
     * The matching artifact and it's dependencies will be omitted from the resulting build tree. The format is
     * G:A:[C:T:]V where every part can be replaced with '*'.
     */
    @java.lang.SuppressWarnings("all")
    public Set<String> getExcludeArtifacts() {
        return this.excludeArtifacts;
    }

    /**
     * If set to true, all -redhat-X artifacts and their dependencies are omitted from the resulting build tree. Note
     * that this option has effect only when {@link #rebuildNonAutoBuilds} is true.
     */
    @java.lang.SuppressWarnings("all")
    public boolean isExcludeProductizedArtifacts() {
        return this.excludeProductizedArtifacts;
    }

    /**
     * List of artifacts that should not be excluded from the analysis.
     *
     * The format is G:A:[C:T:]V where every part can be replaced with '*'.
     */
    @java.lang.SuppressWarnings("all")
    public Set<String> getIncludeArtifacts() {
        return this.includeArtifacts;
    }

    /**
     * The list of URLs to use for the SCM recipes. The SCM recipes are used for determining SCM coordinates for
     * project.
     */
    @java.lang.SuppressWarnings("all")
    public List<String> getRecipeRepos() {
        return this.recipeRepos;
    }

    /**
     * When enabled, autobuilder will rebuild existing builds, if they are not managed by autobuilder (Build Configs
     * with -AUTOBUILD suffix) and will manage them (create a copy of the existing build's Build Config with -AUTOBUILD
     * suffix). This is useful to make sure whole dependency tree is build correctly.
     */
    @java.lang.SuppressWarnings("all")
    public boolean isRebuildNonAutoBuilds() {
        return this.rebuildNonAutoBuilds;
    }

    /**
     * Path to the maven settings.xml file to use instead of the system one.
     */
    @java.lang.SuppressWarnings("all")
    public Path getMavenSettings() {
        return this.mavenSettings;
    }

    /**
     * List of toplevel artifacts that should be analyzed and built.
     */
    @java.lang.SuppressWarnings("all")
    public void setAnalyzeArtifacts(final Set<String> analyzeArtifacts) {
        this.analyzeArtifacts = analyzeArtifacts;
    }

    /**
     * Bom that should be analyzed and which artifacts should be built.
     */
    @java.lang.SuppressWarnings("all")
    public void setAnalyzeBOM(final String analyzeBOM) {
        this.analyzeBOM = analyzeBOM;
    }

    /**
     * Whether optional dependencies should be included in the analysis.
     */
    @java.lang.SuppressWarnings("all")
    public void setIncludeOptionalDependencies(final boolean includeOptionalDependencies) {
        this.includeOptionalDependencies = includeOptionalDependencies;
    }

    /**
     * List of artifacts that should be excluded from the analysis.
     *
     * The matching artifact and it's dependencies will be omitted from the resulting build tree. The format is
     * G:A:[C:T:]V where every part can be replaced with '*'.
     */
    @java.lang.SuppressWarnings("all")
    public void setExcludeArtifacts(final Set<String> excludeArtifacts) {
        this.excludeArtifacts = excludeArtifacts;
    }

    /**
     * If set to true, all -redhat-X artifacts and their dependencies are omitted from the resulting build tree. Note
     * that this option has effect only when {@link #rebuildNonAutoBuilds} is true.
     */
    @java.lang.SuppressWarnings("all")
    public void setExcludeProductizedArtifacts(final boolean excludeProductizedArtifacts) {
        this.excludeProductizedArtifacts = excludeProductizedArtifacts;
    }

    /**
     * List of artifacts that should not be excluded from the analysis.
     *
     * The format is G:A:[C:T:]V where every part can be replaced with '*'.
     */
    @java.lang.SuppressWarnings("all")
    public void setIncludeArtifacts(final Set<String> includeArtifacts) {
        this.includeArtifacts = includeArtifacts;
    }

    /**
     * The list of URLs to use for the SCM recipes. The SCM recipes are used for determining SCM coordinates for
     * project.
     */
    @java.lang.SuppressWarnings("all")
    public void setRecipeRepos(final List<String> recipeRepos) {
        this.recipeRepos = recipeRepos;
    }

    /**
     * When enabled, autobuilder will rebuild existing builds, if they are not managed by autobuilder (Build Configs
     * with -AUTOBUILD suffix) and will manage them (create a copy of the existing build's Build Config with -AUTOBUILD
     * suffix). This is useful to make sure whole dependency tree is build correctly.
     */
    @java.lang.SuppressWarnings("all")
    public void setRebuildNonAutoBuilds(final boolean rebuildNonAutoBuilds) {
        this.rebuildNonAutoBuilds = rebuildNonAutoBuilds;
    }

    /**
     * Path to the maven settings.xml file to use instead of the system one.
     */
    @java.lang.SuppressWarnings("all")
    public void setMavenSettings(final Path mavenSettings) {
        this.mavenSettings = mavenSettings;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this)
            return true;
        if (!(o instanceof DependencyResolutionConfig))
            return false;
        final DependencyResolutionConfig other = (DependencyResolutionConfig) o;
        if (!other.canEqual((java.lang.Object) this))
            return false;
        if (this.isIncludeOptionalDependencies() != other.isIncludeOptionalDependencies())
            return false;
        if (this.isExcludeProductizedArtifacts() != other.isExcludeProductizedArtifacts())
            return false;
        if (this.isRebuildNonAutoBuilds() != other.isRebuildNonAutoBuilds())
            return false;
        final java.lang.Object this$analyzeArtifacts = this.getAnalyzeArtifacts();
        final java.lang.Object other$analyzeArtifacts = other.getAnalyzeArtifacts();
        if (this$analyzeArtifacts == null ? other$analyzeArtifacts != null
                : !this$analyzeArtifacts.equals(other$analyzeArtifacts))
            return false;
        final java.lang.Object this$analyzeBOM = this.getAnalyzeBOM();
        final java.lang.Object other$analyzeBOM = other.getAnalyzeBOM();
        if (this$analyzeBOM == null ? other$analyzeBOM != null : !this$analyzeBOM.equals(other$analyzeBOM))
            return false;
        final java.lang.Object this$excludeArtifacts = this.getExcludeArtifacts();
        final java.lang.Object other$excludeArtifacts = other.getExcludeArtifacts();
        if (this$excludeArtifacts == null ? other$excludeArtifacts != null
                : !this$excludeArtifacts.equals(other$excludeArtifacts))
            return false;
        final java.lang.Object this$includeArtifacts = this.getIncludeArtifacts();
        final java.lang.Object other$includeArtifacts = other.getIncludeArtifacts();
        if (this$includeArtifacts == null ? other$includeArtifacts != null
                : !this$includeArtifacts.equals(other$includeArtifacts))
            return false;
        final java.lang.Object this$recipeRepos = this.getRecipeRepos();
        final java.lang.Object other$recipeRepos = other.getRecipeRepos();
        if (this$recipeRepos == null ? other$recipeRepos != null : !this$recipeRepos.equals(other$recipeRepos))
            return false;
        final java.lang.Object this$mavenSettings = this.getMavenSettings();
        final java.lang.Object other$mavenSettings = other.getMavenSettings();
        if (this$mavenSettings == null ? other$mavenSettings != null : !this$mavenSettings.equals(other$mavenSettings))
            return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof DependencyResolutionConfig;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        result = result * PRIME + (this.isIncludeOptionalDependencies() ? 79 : 97);
        result = result * PRIME + (this.isExcludeProductizedArtifacts() ? 79 : 97);
        result = result * PRIME + (this.isRebuildNonAutoBuilds() ? 79 : 97);
        final java.lang.Object $analyzeArtifacts = this.getAnalyzeArtifacts();
        result = result * PRIME + ($analyzeArtifacts == null ? 43 : $analyzeArtifacts.hashCode());
        final java.lang.Object $analyzeBOM = this.getAnalyzeBOM();
        result = result * PRIME + ($analyzeBOM == null ? 43 : $analyzeBOM.hashCode());
        final java.lang.Object $excludeArtifacts = this.getExcludeArtifacts();
        result = result * PRIME + ($excludeArtifacts == null ? 43 : $excludeArtifacts.hashCode());
        final java.lang.Object $includeArtifacts = this.getIncludeArtifacts();
        result = result * PRIME + ($includeArtifacts == null ? 43 : $includeArtifacts.hashCode());
        final java.lang.Object $recipeRepos = this.getRecipeRepos();
        result = result * PRIME + ($recipeRepos == null ? 43 : $recipeRepos.hashCode());
        final java.lang.Object $mavenSettings = this.getMavenSettings();
        result = result * PRIME + ($mavenSettings == null ? 43 : $mavenSettings.hashCode());
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "DependencyResolutionConfig(analyzeArtifacts=" + this.getAnalyzeArtifacts() + ", analyzeBOM="
                + this.getAnalyzeBOM() + ", includeOptionalDependencies=" + this.isIncludeOptionalDependencies()
                + ", excludeArtifacts=" + this.getExcludeArtifacts() + ", excludeProductizedArtifacts="
                + this.isExcludeProductizedArtifacts() + ", includeArtifacts=" + this.getIncludeArtifacts()
                + ", recipeRepos=" + this.getRecipeRepos() + ", rebuildNonAutoBuilds=" + this.isRebuildNonAutoBuilds()
                + ", mavenSettings=" + this.getMavenSettings() + ")";
    }
}
