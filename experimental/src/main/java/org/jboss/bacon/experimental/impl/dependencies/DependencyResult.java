package org.jboss.bacon.experimental.impl.dependencies;

import java.util.HashSet;
import java.util.Set;

public class DependencyResult {
    private Set<Project> topLevelProjects;

    public int getCount() {
        HashSet<Project> allProjects = new HashSet<>();
        topLevelProjects.forEach(p -> fillAllProject(allProjects, p));
        return allProjects.size();
    }

    private void fillAllProject(Set<Project> allProjects, Project project) {
        if (!allProjects.contains(project)) {
            allProjects.add(project);
            project.getDependencies().forEach(p -> fillAllProject(allProjects, p));
        }
    }

    @java.lang.SuppressWarnings("all")
    public DependencyResult() {
    }

    @java.lang.SuppressWarnings("all")
    public Set<Project> getTopLevelProjects() {
        return this.topLevelProjects;
    }

    @java.lang.SuppressWarnings("all")
    public void setTopLevelProjects(final Set<Project> topLevelProjects) {
        this.topLevelProjects = topLevelProjects;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this)
            return true;
        if (!(o instanceof DependencyResult))
            return false;
        final DependencyResult other = (DependencyResult) o;
        if (!other.canEqual((java.lang.Object) this))
            return false;
        final java.lang.Object this$topLevelProjects = this.getTopLevelProjects();
        final java.lang.Object other$topLevelProjects = other.getTopLevelProjects();
        if (this$topLevelProjects == null ? other$topLevelProjects != null
                : !this$topLevelProjects.equals(other$topLevelProjects))
            return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof DependencyResult;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $topLevelProjects = this.getTopLevelProjects();
        result = result * PRIME + ($topLevelProjects == null ? 43 : $topLevelProjects.hashCode());
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "DependencyResult(topLevelProjects=" + this.getTopLevelProjects() + ")";
    }
}
