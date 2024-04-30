package org.jboss.bacon.experimental.impl.dependencies;

import lombok.Data;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

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

    /**
     * Remove the provided GAV from the {@link #topLevelProjects} set.
     *
     * @param excludedGavs GAV to be excluded from the topLevelProjects.
     */
    public void removeProjects(String[] excludedGavs) {
        Predicate<Project> pr = a -> (Arrays.stream(excludedGavs).anyMatch(a.getName()::equals));
        topLevelProjects.removeIf(pr);
    }

}
