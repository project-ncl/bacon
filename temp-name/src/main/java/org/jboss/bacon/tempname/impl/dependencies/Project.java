package org.jboss.bacon.tempname.impl.dependencies;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.jboss.da.model.rest.GAV;

import java.util.Set;

@Data
@EqualsAndHashCode(exclude = "dependencies")
public class Project {
    private String sourceCodeURL;
    private String sourceCodeRevision;
    private Set<GAV> gavs;
    private Set<Project> dependencies;
}
