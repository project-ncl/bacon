package org.jboss.bacon.experimental.impl.projectfinder;

import java.util.HashSet;
import java.util.Set;

import lombok.Data;

@Data
public class FoundProjects {
    private Set<FoundProject> foundProjects = new HashSet<>();
}
