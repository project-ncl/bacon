package org.jboss.bacon.experimental.impl.config;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class GeneratorConfig {
    @Valid
    @NotNull
    private DependencyResolutionConfig dependencyResolutionConfig = new DependencyResolutionConfig();
    @Valid
    @NotNull
    private BuildConfigGeneratorConfig buildConfigGeneratorConfig = new BuildConfigGeneratorConfig();

    @java.lang.SuppressWarnings("all")
    public GeneratorConfig() {
    }

    @java.lang.SuppressWarnings("all")
    public DependencyResolutionConfig getDependencyResolutionConfig() {
        return this.dependencyResolutionConfig;
    }

    @java.lang.SuppressWarnings("all")
    public BuildConfigGeneratorConfig getBuildConfigGeneratorConfig() {
        return this.buildConfigGeneratorConfig;
    }

    @java.lang.SuppressWarnings("all")
    public void setDependencyResolutionConfig(final DependencyResolutionConfig dependencyResolutionConfig) {
        this.dependencyResolutionConfig = dependencyResolutionConfig;
    }

    @java.lang.SuppressWarnings("all")
    public void setBuildConfigGeneratorConfig(final BuildConfigGeneratorConfig buildConfigGeneratorConfig) {
        this.buildConfigGeneratorConfig = buildConfigGeneratorConfig;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this)
            return true;
        if (!(o instanceof GeneratorConfig))
            return false;
        final GeneratorConfig other = (GeneratorConfig) o;
        if (!other.canEqual((java.lang.Object) this))
            return false;
        final java.lang.Object this$dependencyResolutionConfig = this.getDependencyResolutionConfig();
        final java.lang.Object other$dependencyResolutionConfig = other.getDependencyResolutionConfig();
        if (this$dependencyResolutionConfig == null ? other$dependencyResolutionConfig != null
                : !this$dependencyResolutionConfig.equals(other$dependencyResolutionConfig))
            return false;
        final java.lang.Object this$buildConfigGeneratorConfig = this.getBuildConfigGeneratorConfig();
        final java.lang.Object other$buildConfigGeneratorConfig = other.getBuildConfigGeneratorConfig();
        if (this$buildConfigGeneratorConfig == null ? other$buildConfigGeneratorConfig != null
                : !this$buildConfigGeneratorConfig.equals(other$buildConfigGeneratorConfig))
            return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof GeneratorConfig;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $dependencyResolutionConfig = this.getDependencyResolutionConfig();
        result = result * PRIME + ($dependencyResolutionConfig == null ? 43 : $dependencyResolutionConfig.hashCode());
        final java.lang.Object $buildConfigGeneratorConfig = this.getBuildConfigGeneratorConfig();
        result = result * PRIME + ($buildConfigGeneratorConfig == null ? 43 : $buildConfigGeneratorConfig.hashCode());
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "GeneratorConfig(dependencyResolutionConfig=" + this.getDependencyResolutionConfig()
                + ", buildConfigGeneratorConfig=" + this.getBuildConfigGeneratorConfig() + ")";
    }
}
