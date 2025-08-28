package org.jboss.bacon.experimental.impl.dependencies;

import java.util.Set;

import org.jboss.da.model.rest.GAV;

public class Project {
    private String sourceCodeURL;
    private String sourceCodeRevision;
    private Set<GAV> gavs;
    private String name;
    private Set<Project> dependencies;
    private int depth = -1;
    private boolean cutDependency = false;
    private boolean conflictingName = false;

    public GAV getFirstGAV() {
        return gavs.stream().sorted().findFirst().get();
    }

    public String getName() {
        if (name == null) {
            throw new IllegalStateException(
                    "You must run project generator on the projects, to generate names for them first.");
        }
        return name;
    }

    @Override
    public String toString() {
        return "Project{" + "sourceCodeURL=\'" + sourceCodeURL + '\'' + ", sourceCodeRevision=\'" + sourceCodeRevision
                + '\'' + ", gavs=" + gavs + ", name=\'" + name + '\'' + '}';
    }

    @java.lang.SuppressWarnings("all")
    public Project() {
    }

    @java.lang.SuppressWarnings("all")
    public String getSourceCodeURL() {
        return this.sourceCodeURL;
    }

    @java.lang.SuppressWarnings("all")
    public String getSourceCodeRevision() {
        return this.sourceCodeRevision;
    }

    @java.lang.SuppressWarnings("all")
    public Set<GAV> getGavs() {
        return this.gavs;
    }

    @java.lang.SuppressWarnings("all")
    public Set<Project> getDependencies() {
        return this.dependencies;
    }

    @java.lang.SuppressWarnings("all")
    public int getDepth() {
        return this.depth;
    }

    @java.lang.SuppressWarnings("all")
    public boolean isCutDependency() {
        return this.cutDependency;
    }

    @java.lang.SuppressWarnings("all")
    public boolean isConflictingName() {
        return this.conflictingName;
    }

    @java.lang.SuppressWarnings("all")
    public void setSourceCodeURL(final String sourceCodeURL) {
        this.sourceCodeURL = sourceCodeURL;
    }

    @java.lang.SuppressWarnings("all")
    public void setSourceCodeRevision(final String sourceCodeRevision) {
        this.sourceCodeRevision = sourceCodeRevision;
    }

    @java.lang.SuppressWarnings("all")
    public void setGavs(final Set<GAV> gavs) {
        this.gavs = gavs;
    }

    @java.lang.SuppressWarnings("all")
    public void setName(final String name) {
        this.name = name;
    }

    @java.lang.SuppressWarnings("all")
    public void setDependencies(final Set<Project> dependencies) {
        this.dependencies = dependencies;
    }

    @java.lang.SuppressWarnings("all")
    public void setDepth(final int depth) {
        this.depth = depth;
    }

    @java.lang.SuppressWarnings("all")
    public void setCutDependency(final boolean cutDependency) {
        this.cutDependency = cutDependency;
    }

    @java.lang.SuppressWarnings("all")
    public void setConflictingName(final boolean conflictingName) {
        this.conflictingName = conflictingName;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this)
            return true;
        if (!(o instanceof Project))
            return false;
        final Project other = (Project) o;
        if (!other.canEqual((java.lang.Object) this))
            return false;
        final java.lang.Object this$sourceCodeURL = this.getSourceCodeURL();
        final java.lang.Object other$sourceCodeURL = other.getSourceCodeURL();
        if (this$sourceCodeURL == null ? other$sourceCodeURL != null : !this$sourceCodeURL.equals(other$sourceCodeURL))
            return false;
        final java.lang.Object this$sourceCodeRevision = this.getSourceCodeRevision();
        final java.lang.Object other$sourceCodeRevision = other.getSourceCodeRevision();
        if (this$sourceCodeRevision == null ? other$sourceCodeRevision != null
                : !this$sourceCodeRevision.equals(other$sourceCodeRevision))
            return false;
        final java.lang.Object this$gavs = this.getGavs();
        final java.lang.Object other$gavs = other.getGavs();
        if (this$gavs == null ? other$gavs != null : !this$gavs.equals(other$gavs))
            return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof Project;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $sourceCodeURL = this.getSourceCodeURL();
        result = result * PRIME + ($sourceCodeURL == null ? 43 : $sourceCodeURL.hashCode());
        final java.lang.Object $sourceCodeRevision = this.getSourceCodeRevision();
        result = result * PRIME + ($sourceCodeRevision == null ? 43 : $sourceCodeRevision.hashCode());
        final java.lang.Object $gavs = this.getGavs();
        result = result * PRIME + ($gavs == null ? 43 : $gavs.hashCode());
        return result;
    }
}
