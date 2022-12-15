package org.jboss.bacon.tempname.impl.projectfinder;

import lombok.Data;

import java.util.Set;

@Data
public class FoundProjects {
    Set<FoundProject> foundProjects;
}
