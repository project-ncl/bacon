package org.jboss.pnc.bacon.pig.impl.sources;

import java.util.ArrayList;
import java.util.List;

import org.jboss.pnc.bacon.pig.impl.config.GenerationData;

public class SourcesGenerationData extends GenerationData<SourcesGenerationStrategy> {
    /**
     * Each entry can be a part of an artifact If none are supplied, then it's assumed that sources from all artifacts
     * are to be added
     */
    private List<String> whitelistedArtifacts;
    private List<String> additionalExternalSources;
    private List<String> excludeSourceBuilds;
    private boolean oldBCNaming = false;

    /**
     * Add defaults to avoid having existing configurations having to define a sourceGeneration object in the flow
     * section
     */
    public SourcesGenerationData() {
        this.whitelistedArtifacts = new ArrayList<>();
        this.additionalExternalSources = new ArrayList<>();
        this.excludeSourceBuilds = new ArrayList<>();
        setStrategy(SourcesGenerationStrategy.GENERATE);
    }

    /**
     * Each entry can be a part of an artifact If none are supplied, then it's assumed that sources from all artifacts
     * are to be added
     */
    @java.lang.SuppressWarnings("all")
    public List<String> getWhitelistedArtifacts() {
        return this.whitelistedArtifacts;
    }

    @java.lang.SuppressWarnings("all")
    public List<String> getAdditionalExternalSources() {
        return this.additionalExternalSources;
    }

    @java.lang.SuppressWarnings("all")
    public List<String> getExcludeSourceBuilds() {
        return this.excludeSourceBuilds;
    }

    @java.lang.SuppressWarnings("all")
    public boolean isOldBCNaming() {
        return this.oldBCNaming;
    }

    /**
     * Each entry can be a part of an artifact If none are supplied, then it's assumed that sources from all artifacts
     * are to be added
     */
    @java.lang.SuppressWarnings("all")
    public void setWhitelistedArtifacts(final List<String> whitelistedArtifacts) {
        this.whitelistedArtifacts = whitelistedArtifacts;
    }

    @java.lang.SuppressWarnings("all")
    public void setAdditionalExternalSources(final List<String> additionalExternalSources) {
        this.additionalExternalSources = additionalExternalSources;
    }

    @java.lang.SuppressWarnings("all")
    public void setExcludeSourceBuilds(final List<String> excludeSourceBuilds) {
        this.excludeSourceBuilds = excludeSourceBuilds;
    }

    @java.lang.SuppressWarnings("all")
    public void setOldBCNaming(final boolean oldBCNaming) {
        this.oldBCNaming = oldBCNaming;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this)
            return true;
        if (!(o instanceof SourcesGenerationData))
            return false;
        final SourcesGenerationData other = (SourcesGenerationData) o;
        if (!other.canEqual((java.lang.Object) this))
            return false;
        if (this.isOldBCNaming() != other.isOldBCNaming())
            return false;
        final java.lang.Object this$whitelistedArtifacts = this.getWhitelistedArtifacts();
        final java.lang.Object other$whitelistedArtifacts = other.getWhitelistedArtifacts();
        if (this$whitelistedArtifacts == null ? other$whitelistedArtifacts != null
                : !this$whitelistedArtifacts.equals(other$whitelistedArtifacts))
            return false;
        final java.lang.Object this$additionalExternalSources = this.getAdditionalExternalSources();
        final java.lang.Object other$additionalExternalSources = other.getAdditionalExternalSources();
        if (this$additionalExternalSources == null ? other$additionalExternalSources != null
                : !this$additionalExternalSources.equals(other$additionalExternalSources))
            return false;
        final java.lang.Object this$excludeSourceBuilds = this.getExcludeSourceBuilds();
        final java.lang.Object other$excludeSourceBuilds = other.getExcludeSourceBuilds();
        if (this$excludeSourceBuilds == null ? other$excludeSourceBuilds != null
                : !this$excludeSourceBuilds.equals(other$excludeSourceBuilds))
            return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof SourcesGenerationData;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        result = result * PRIME + (this.isOldBCNaming() ? 79 : 97);
        final java.lang.Object $whitelistedArtifacts = this.getWhitelistedArtifacts();
        result = result * PRIME + ($whitelistedArtifacts == null ? 43 : $whitelistedArtifacts.hashCode());
        final java.lang.Object $additionalExternalSources = this.getAdditionalExternalSources();
        result = result * PRIME + ($additionalExternalSources == null ? 43 : $additionalExternalSources.hashCode());
        final java.lang.Object $excludeSourceBuilds = this.getExcludeSourceBuilds();
        result = result * PRIME + ($excludeSourceBuilds == null ? 43 : $excludeSourceBuilds.hashCode());
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "SourcesGenerationData(whitelistedArtifacts=" + this.getWhitelistedArtifacts()
                + ", additionalExternalSources=" + this.getAdditionalExternalSources() + ", excludeSourceBuilds="
                + this.getExcludeSourceBuilds() + ", oldBCNaming=" + this.isOldBCNaming() + ")";
    }
}
