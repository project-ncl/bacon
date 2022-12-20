package org.jboss.bacon.tempname.impl.projectfinder;

import lombok.Data;

import java.util.HashSet;
import java.util.Set;

@Data
public class FoundProjects {
    private Set<FoundProject> foundProjects = new HashSet<>();
}
