package org.jboss.bacon.tempname.impl.dependencies;

import lombok.Data;

import java.util.Set;

@Data
public class DependencyResult {
    private Set<Project> topLevelProjects;
    private int count;
}
