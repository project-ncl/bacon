package org.jboss.bacon.experimental.impl.projectfinder;

import java.util.HashSet;
import java.util.Set;

public class FoundProjects {
    private Set<FoundProject> foundProjects = new HashSet<>();

    @java.lang.SuppressWarnings("all")
    public FoundProjects() {
    }

    @java.lang.SuppressWarnings("all")
    public Set<FoundProject> getFoundProjects() {
        return this.foundProjects;
    }

    @java.lang.SuppressWarnings("all")
    public void setFoundProjects(final Set<FoundProject> foundProjects) {
        this.foundProjects = foundProjects;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this)
            return true;
        if (!(o instanceof FoundProjects))
            return false;
        final FoundProjects other = (FoundProjects) o;
        if (!other.canEqual((java.lang.Object) this))
            return false;
        final java.lang.Object this$foundProjects = this.getFoundProjects();
        final java.lang.Object other$foundProjects = other.getFoundProjects();
        if (this$foundProjects == null ? other$foundProjects != null : !this$foundProjects.equals(other$foundProjects))
            return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof FoundProjects;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $foundProjects = this.getFoundProjects();
        result = result * PRIME + ($foundProjects == null ? 43 : $foundProjects.hashCode());
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "FoundProjects(foundProjects=" + this.getFoundProjects() + ")";
    }
}
