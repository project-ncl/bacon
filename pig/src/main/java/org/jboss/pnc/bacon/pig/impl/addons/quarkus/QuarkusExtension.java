package org.jboss.pnc.bacon.pig.impl.addons.quarkus;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com <br>
 *         Date: 19/08/2019
 */
public class QuarkusExtension {
    @JsonProperty("artifact")
    private String artifact;

    @java.lang.SuppressWarnings("all")
    public QuarkusExtension() {
    }

    @java.lang.SuppressWarnings("all")
    public String getArtifact() {
        return this.artifact;
    }

    @JsonProperty("artifact")
    @java.lang.SuppressWarnings("all")
    public void setArtifact(final String artifact) {
        this.artifact = artifact;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this)
            return true;
        if (!(o instanceof QuarkusExtension))
            return false;
        final QuarkusExtension other = (QuarkusExtension) o;
        if (!other.canEqual((java.lang.Object) this))
            return false;
        final java.lang.Object this$artifact = this.getArtifact();
        final java.lang.Object other$artifact = other.getArtifact();
        if (this$artifact == null ? other$artifact != null : !this$artifact.equals(other$artifact))
            return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof QuarkusExtension;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $artifact = this.getArtifact();
        result = result * PRIME + ($artifact == null ? 43 : $artifact.hashCode());
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "QuarkusExtension(artifact=" + this.getArtifact() + ")";
    }
}
