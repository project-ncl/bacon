package org.jboss.bacon.experimental.impl.projectfinder;

import java.util.Set;

import org.jboss.da.model.rest.GAV;
import org.jboss.pnc.dto.BuildConfiguration;
import org.jboss.pnc.dto.BuildConfigurationRevision;

public class FoundProject {
    private Set<GAV> gavs;
    private BuildConfigurationRevision buildConfigRevision;
    private BuildConfiguration buildConfig;
    /**
     * Did we find existing Build Config?
     */
    private boolean found;
    /**
     * Is the found build config created by previous runs of autobuilder?
     */
    private boolean managed;
    /**
     * Did the found build produced exact match of the version? Exact match means the same Major.Minor.Micro.Qualifier,
     * but can differ in the -redhat suffix.
     */
    private boolean exactMatch;
    /**
     * Did the found build produced all the GAs?
     */
    private boolean complete;
    /**
     * Is the Build Config Revision the latest revision for that Build Config?
     */
    private boolean latestRevision;

    @java.lang.SuppressWarnings("all")
    public FoundProject() {
    }

    @java.lang.SuppressWarnings("all")
    public Set<GAV> getGavs() {
        return this.gavs;
    }

    @java.lang.SuppressWarnings("all")
    public BuildConfigurationRevision getBuildConfigRevision() {
        return this.buildConfigRevision;
    }

    @java.lang.SuppressWarnings("all")
    public BuildConfiguration getBuildConfig() {
        return this.buildConfig;
    }

    /**
     * Did we find existing Build Config?
     */
    @java.lang.SuppressWarnings("all")
    public boolean isFound() {
        return this.found;
    }

    /**
     * Is the found build config created by previous runs of autobuilder?
     */
    @java.lang.SuppressWarnings("all")
    public boolean isManaged() {
        return this.managed;
    }

    /**
     * Did the found build produced exact match of the version? Exact match means the same Major.Minor.Micro.Qualifier,
     * but can differ in the -redhat suffix.
     */
    @java.lang.SuppressWarnings("all")
    public boolean isExactMatch() {
        return this.exactMatch;
    }

    /**
     * Did the found build produced all the GAs?
     */
    @java.lang.SuppressWarnings("all")
    public boolean isComplete() {
        return this.complete;
    }

    /**
     * Is the Build Config Revision the latest revision for that Build Config?
     */
    @java.lang.SuppressWarnings("all")
    public boolean isLatestRevision() {
        return this.latestRevision;
    }

    @java.lang.SuppressWarnings("all")
    public void setGavs(final Set<GAV> gavs) {
        this.gavs = gavs;
    }

    @java.lang.SuppressWarnings("all")
    public void setBuildConfigRevision(final BuildConfigurationRevision buildConfigRevision) {
        this.buildConfigRevision = buildConfigRevision;
    }

    @java.lang.SuppressWarnings("all")
    public void setBuildConfig(final BuildConfiguration buildConfig) {
        this.buildConfig = buildConfig;
    }

    /**
     * Did we find existing Build Config?
     */
    @java.lang.SuppressWarnings("all")
    public void setFound(final boolean found) {
        this.found = found;
    }

    /**
     * Is the found build config created by previous runs of autobuilder?
     */
    @java.lang.SuppressWarnings("all")
    public void setManaged(final boolean managed) {
        this.managed = managed;
    }

    /**
     * Did the found build produced exact match of the version? Exact match means the same Major.Minor.Micro.Qualifier,
     * but can differ in the -redhat suffix.
     */
    @java.lang.SuppressWarnings("all")
    public void setExactMatch(final boolean exactMatch) {
        this.exactMatch = exactMatch;
    }

    /**
     * Did the found build produced all the GAs?
     */
    @java.lang.SuppressWarnings("all")
    public void setComplete(final boolean complete) {
        this.complete = complete;
    }

    /**
     * Is the Build Config Revision the latest revision for that Build Config?
     */
    @java.lang.SuppressWarnings("all")
    public void setLatestRevision(final boolean latestRevision) {
        this.latestRevision = latestRevision;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this)
            return true;
        if (!(o instanceof FoundProject))
            return false;
        final FoundProject other = (FoundProject) o;
        if (!other.canEqual((java.lang.Object) this))
            return false;
        if (this.isFound() != other.isFound())
            return false;
        if (this.isManaged() != other.isManaged())
            return false;
        if (this.isExactMatch() != other.isExactMatch())
            return false;
        if (this.isComplete() != other.isComplete())
            return false;
        if (this.isLatestRevision() != other.isLatestRevision())
            return false;
        final java.lang.Object this$gavs = this.getGavs();
        final java.lang.Object other$gavs = other.getGavs();
        if (this$gavs == null ? other$gavs != null : !this$gavs.equals(other$gavs))
            return false;
        final java.lang.Object this$buildConfigRevision = this.getBuildConfigRevision();
        final java.lang.Object other$buildConfigRevision = other.getBuildConfigRevision();
        if (this$buildConfigRevision == null ? other$buildConfigRevision != null
                : !this$buildConfigRevision.equals(other$buildConfigRevision))
            return false;
        final java.lang.Object this$buildConfig = this.getBuildConfig();
        final java.lang.Object other$buildConfig = other.getBuildConfig();
        if (this$buildConfig == null ? other$buildConfig != null : !this$buildConfig.equals(other$buildConfig))
            return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof FoundProject;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        result = result * PRIME + (this.isFound() ? 79 : 97);
        result = result * PRIME + (this.isManaged() ? 79 : 97);
        result = result * PRIME + (this.isExactMatch() ? 79 : 97);
        result = result * PRIME + (this.isComplete() ? 79 : 97);
        result = result * PRIME + (this.isLatestRevision() ? 79 : 97);
        final java.lang.Object $gavs = this.getGavs();
        result = result * PRIME + ($gavs == null ? 43 : $gavs.hashCode());
        final java.lang.Object $buildConfigRevision = this.getBuildConfigRevision();
        result = result * PRIME + ($buildConfigRevision == null ? 43 : $buildConfigRevision.hashCode());
        final java.lang.Object $buildConfig = this.getBuildConfig();
        result = result * PRIME + ($buildConfig == null ? 43 : $buildConfig.hashCode());
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "FoundProject(gavs=" + this.getGavs() + ", buildConfigRevision=" + this.getBuildConfigRevision()
                + ", buildConfig=" + this.getBuildConfig() + ", found=" + this.isFound() + ", managed="
                + this.isManaged() + ", exactMatch=" + this.isExactMatch() + ", complete=" + this.isComplete()
                + ", latestRevision=" + this.isLatestRevision() + ")";
    }
}
