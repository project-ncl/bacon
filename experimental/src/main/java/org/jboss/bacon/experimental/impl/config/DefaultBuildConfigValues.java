package org.jboss.bacon.experimental.impl.config;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import org.jboss.pnc.dto.validation.constraints.SCMUrl;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Default values to be used when generating build config.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class DefaultBuildConfigValues {
    /**
     * Name of the environment to be used when generating new build config. Required.
     */
    @NotNull
    private String environmentName;
    /**
     * Build script to be used when generating new build config.
     */
    @NotEmpty
    private String buildScript = "mvn deploy";
    /**
     * Placeholder SCM URL.
     *
     * To be used when no SCM url was found for the project. We need it to generate valid build-config.
     */
    @SCMUrl(message = "must be valid git url")
    private String scmUrl = "https://github.com/michalszynkiewicz/empty.git";
    /**
     * Placeholder SCM revision to be used when using placeholder URL or when SCM revision was not resolved.
     */
    @NotEmpty
    private String scmRevision = "master";

    @java.lang.SuppressWarnings("all")
    public DefaultBuildConfigValues() {
    }

    /**
     * Name of the environment to be used when generating new build config. Required.
     */
    @java.lang.SuppressWarnings("all")
    public String getEnvironmentName() {
        return this.environmentName;
    }

    /**
     * Build script to be used when generating new build config.
     */
    @java.lang.SuppressWarnings("all")
    public String getBuildScript() {
        return this.buildScript;
    }

    /**
     * Placeholder SCM URL.
     *
     * To be used when no SCM url was found for the project. We need it to generate valid build-config.
     */
    @java.lang.SuppressWarnings("all")
    public String getScmUrl() {
        return this.scmUrl;
    }

    /**
     * Placeholder SCM revision to be used when using placeholder URL or when SCM revision was not resolved.
     */
    @java.lang.SuppressWarnings("all")
    public String getScmRevision() {
        return this.scmRevision;
    }

    /**
     * Name of the environment to be used when generating new build config. Required.
     */
    @java.lang.SuppressWarnings("all")
    public void setEnvironmentName(final String environmentName) {
        this.environmentName = environmentName;
    }

    /**
     * Build script to be used when generating new build config.
     */
    @java.lang.SuppressWarnings("all")
    public void setBuildScript(final String buildScript) {
        this.buildScript = buildScript;
    }

    /**
     * Placeholder SCM URL.
     *
     * To be used when no SCM url was found for the project. We need it to generate valid build-config.
     */
    @java.lang.SuppressWarnings("all")
    public void setScmUrl(final String scmUrl) {
        this.scmUrl = scmUrl;
    }

    /**
     * Placeholder SCM revision to be used when using placeholder URL or when SCM revision was not resolved.
     */
    @java.lang.SuppressWarnings("all")
    public void setScmRevision(final String scmRevision) {
        this.scmRevision = scmRevision;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this)
            return true;
        if (!(o instanceof DefaultBuildConfigValues))
            return false;
        final DefaultBuildConfigValues other = (DefaultBuildConfigValues) o;
        if (!other.canEqual((java.lang.Object) this))
            return false;
        final java.lang.Object this$environmentName = this.getEnvironmentName();
        final java.lang.Object other$environmentName = other.getEnvironmentName();
        if (this$environmentName == null ? other$environmentName != null
                : !this$environmentName.equals(other$environmentName))
            return false;
        final java.lang.Object this$buildScript = this.getBuildScript();
        final java.lang.Object other$buildScript = other.getBuildScript();
        if (this$buildScript == null ? other$buildScript != null : !this$buildScript.equals(other$buildScript))
            return false;
        final java.lang.Object this$scmUrl = this.getScmUrl();
        final java.lang.Object other$scmUrl = other.getScmUrl();
        if (this$scmUrl == null ? other$scmUrl != null : !this$scmUrl.equals(other$scmUrl))
            return false;
        final java.lang.Object this$scmRevision = this.getScmRevision();
        final java.lang.Object other$scmRevision = other.getScmRevision();
        if (this$scmRevision == null ? other$scmRevision != null : !this$scmRevision.equals(other$scmRevision))
            return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof DefaultBuildConfigValues;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $environmentName = this.getEnvironmentName();
        result = result * PRIME + ($environmentName == null ? 43 : $environmentName.hashCode());
        final java.lang.Object $buildScript = this.getBuildScript();
        result = result * PRIME + ($buildScript == null ? 43 : $buildScript.hashCode());
        final java.lang.Object $scmUrl = this.getScmUrl();
        result = result * PRIME + ($scmUrl == null ? 43 : $scmUrl.hashCode());
        final java.lang.Object $scmRevision = this.getScmRevision();
        result = result * PRIME + ($scmRevision == null ? 43 : $scmRevision.hashCode());
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "DefaultBuildConfigValues(environmentName=" + this.getEnvironmentName() + ", buildScript="
                + this.getBuildScript() + ", scmUrl=" + this.getScmUrl() + ", scmRevision=" + this.getScmRevision()
                + ")";
    }
}
