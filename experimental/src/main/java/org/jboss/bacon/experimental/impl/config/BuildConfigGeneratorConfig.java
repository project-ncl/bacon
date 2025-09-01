package org.jboss.bacon.experimental.impl.config;

import java.util.LinkedHashMap;
import java.util.Set;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.jboss.pnc.bacon.pig.impl.config.PigConfiguration;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Configuration for how the new build configs should be created.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class BuildConfigGeneratorConfig {
    /**
     * Template of the PiG build-config.yaml to use.
     */
    @NotNull
    private PigConfiguration pigTemplate;
    /**
     * SCM URLs which contain one of the string listed will be replaced by the default placeholder scmUrl.
     */
    @NotNull
    private Set<String> scmReplaceWithPlaceholder = Set.of();
    /**
     * SCM URLs will have the listed key substring replaced with the value.
     */
    @NotNull
    private LinkedHashMap<String, String> scmPattern = new LinkedHashMap<>();
    /**
     * SCM URLs containing the listed key are wholly replaced by the value.
     */
    @NotNull
    private LinkedHashMap<String, String> scmMapping = new LinkedHashMap<>();
    /**
     * When generating new or copying and changing existing build config, will add 'false' at the end of the build
     * script to make sure the build fails.
     *
     * This can be helpful to make sure the configs are manually reviewed.
     */
    private boolean failGeneratedBuildScript = true;
    /**
     * Default values to be used when generating build config
     */
    @Valid
    @NotNull
    private DefaultBuildConfigValues defaultValues = new DefaultBuildConfigValues();
    /**
     * If true, uses systemImageId to specify environment (to force use of specific environment) when the environment
     * defined by environmentName would not be found because it was deprecated.
     */
    private boolean allowDeprecatedEnvironments = false;

    @java.lang.SuppressWarnings("all")
    public BuildConfigGeneratorConfig() {
    }

    /**
     * Template of the PiG build-config.yaml to use.
     */
    @java.lang.SuppressWarnings("all")
    public PigConfiguration getPigTemplate() {
        return this.pigTemplate;
    }

    /**
     * SCM URLs which contain one of the string listed will be replaced by the default placeholder scmUrl.
     */
    @java.lang.SuppressWarnings("all")
    public Set<String> getScmReplaceWithPlaceholder() {
        return this.scmReplaceWithPlaceholder;
    }

    /**
     * SCM URLs will have the listed key substring replaced with the value.
     */
    @java.lang.SuppressWarnings("all")
    public LinkedHashMap<String, String> getScmPattern() {
        return this.scmPattern;
    }

    /**
     * SCM URLs containing the listed key are wholly replaced by the value.
     */
    @java.lang.SuppressWarnings("all")
    public LinkedHashMap<String, String> getScmMapping() {
        return this.scmMapping;
    }

    /**
     * When generating new or copying and changing existing build config, will add 'false' at the end of the build
     * script to make sure the build fails.
     *
     * This can be helpful to make sure the configs are manually reviewed.
     */
    @java.lang.SuppressWarnings("all")
    public boolean isFailGeneratedBuildScript() {
        return this.failGeneratedBuildScript;
    }

    /**
     * Default values to be used when generating build config
     */
    @java.lang.SuppressWarnings("all")
    public DefaultBuildConfigValues getDefaultValues() {
        return this.defaultValues;
    }

    /**
     * If true, uses systemImageId to specify environment (to force use of specific environment) when the environment
     * defined by environmentName would not be found because it was deprecated.
     */
    @java.lang.SuppressWarnings("all")
    public boolean isAllowDeprecatedEnvironments() {
        return this.allowDeprecatedEnvironments;
    }

    /**
     * Template of the PiG build-config.yaml to use.
     */
    @java.lang.SuppressWarnings("all")
    public void setPigTemplate(final PigConfiguration pigTemplate) {
        this.pigTemplate = pigTemplate;
    }

    /**
     * SCM URLs which contain one of the string listed will be replaced by the default placeholder scmUrl.
     */
    @java.lang.SuppressWarnings("all")
    public void setScmReplaceWithPlaceholder(final Set<String> scmReplaceWithPlaceholder) {
        this.scmReplaceWithPlaceholder = scmReplaceWithPlaceholder;
    }

    /**
     * SCM URLs will have the listed key substring replaced with the value.
     */
    @java.lang.SuppressWarnings("all")
    public void setScmPattern(final LinkedHashMap<String, String> scmPattern) {
        this.scmPattern = scmPattern;
    }

    /**
     * SCM URLs containing the listed key are wholly replaced by the value.
     */
    @java.lang.SuppressWarnings("all")
    public void setScmMapping(final LinkedHashMap<String, String> scmMapping) {
        this.scmMapping = scmMapping;
    }

    /**
     * When generating new or copying and changing existing build config, will add 'false' at the end of the build
     * script to make sure the build fails.
     *
     * This can be helpful to make sure the configs are manually reviewed.
     */
    @java.lang.SuppressWarnings("all")
    public void setFailGeneratedBuildScript(final boolean failGeneratedBuildScript) {
        this.failGeneratedBuildScript = failGeneratedBuildScript;
    }

    /**
     * Default values to be used when generating build config
     */
    @java.lang.SuppressWarnings("all")
    public void setDefaultValues(final DefaultBuildConfigValues defaultValues) {
        this.defaultValues = defaultValues;
    }

    /**
     * If true, uses systemImageId to specify environment (to force use of specific environment) when the environment
     * defined by environmentName would not be found because it was deprecated.
     */
    @java.lang.SuppressWarnings("all")
    public void setAllowDeprecatedEnvironments(final boolean allowDeprecatedEnvironments) {
        this.allowDeprecatedEnvironments = allowDeprecatedEnvironments;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this)
            return true;
        if (!(o instanceof BuildConfigGeneratorConfig))
            return false;
        final BuildConfigGeneratorConfig other = (BuildConfigGeneratorConfig) o;
        if (!other.canEqual((java.lang.Object) this))
            return false;
        if (this.isFailGeneratedBuildScript() != other.isFailGeneratedBuildScript())
            return false;
        if (this.isAllowDeprecatedEnvironments() != other.isAllowDeprecatedEnvironments())
            return false;
        final java.lang.Object this$pigTemplate = this.getPigTemplate();
        final java.lang.Object other$pigTemplate = other.getPigTemplate();
        if (this$pigTemplate == null ? other$pigTemplate != null : !this$pigTemplate.equals(other$pigTemplate))
            return false;
        final java.lang.Object this$scmReplaceWithPlaceholder = this.getScmReplaceWithPlaceholder();
        final java.lang.Object other$scmReplaceWithPlaceholder = other.getScmReplaceWithPlaceholder();
        if (this$scmReplaceWithPlaceholder == null ? other$scmReplaceWithPlaceholder != null
                : !this$scmReplaceWithPlaceholder.equals(other$scmReplaceWithPlaceholder))
            return false;
        final java.lang.Object this$scmPattern = this.getScmPattern();
        final java.lang.Object other$scmPattern = other.getScmPattern();
        if (this$scmPattern == null ? other$scmPattern != null : !this$scmPattern.equals(other$scmPattern))
            return false;
        final java.lang.Object this$scmMapping = this.getScmMapping();
        final java.lang.Object other$scmMapping = other.getScmMapping();
        if (this$scmMapping == null ? other$scmMapping != null : !this$scmMapping.equals(other$scmMapping))
            return false;
        final java.lang.Object this$defaultValues = this.getDefaultValues();
        final java.lang.Object other$defaultValues = other.getDefaultValues();
        if (this$defaultValues == null ? other$defaultValues != null : !this$defaultValues.equals(other$defaultValues))
            return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof BuildConfigGeneratorConfig;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        result = result * PRIME + (this.isFailGeneratedBuildScript() ? 79 : 97);
        result = result * PRIME + (this.isAllowDeprecatedEnvironments() ? 79 : 97);
        final java.lang.Object $pigTemplate = this.getPigTemplate();
        result = result * PRIME + ($pigTemplate == null ? 43 : $pigTemplate.hashCode());
        final java.lang.Object $scmReplaceWithPlaceholder = this.getScmReplaceWithPlaceholder();
        result = result * PRIME + ($scmReplaceWithPlaceholder == null ? 43 : $scmReplaceWithPlaceholder.hashCode());
        final java.lang.Object $scmPattern = this.getScmPattern();
        result = result * PRIME + ($scmPattern == null ? 43 : $scmPattern.hashCode());
        final java.lang.Object $scmMapping = this.getScmMapping();
        result = result * PRIME + ($scmMapping == null ? 43 : $scmMapping.hashCode());
        final java.lang.Object $defaultValues = this.getDefaultValues();
        result = result * PRIME + ($defaultValues == null ? 43 : $defaultValues.hashCode());
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "BuildConfigGeneratorConfig(pigTemplate=" + this.getPigTemplate() + ", scmReplaceWithPlaceholder="
                + this.getScmReplaceWithPlaceholder() + ", scmPattern=" + this.getScmPattern() + ", scmMapping="
                + this.getScmMapping() + ", failGeneratedBuildScript=" + this.isFailGeneratedBuildScript()
                + ", defaultValues=" + this.getDefaultValues() + ", allowDeprecatedEnvironments="
                + this.isAllowDeprecatedEnvironments() + ")";
    }
}
