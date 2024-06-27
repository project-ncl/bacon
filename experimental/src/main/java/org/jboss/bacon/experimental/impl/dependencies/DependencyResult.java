package org.jboss.bacon.experimental.impl.dependencies;

import lombok.Data;

import java.util.HashSet;
import java.util.Set;

@Data
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
}
