package org.jboss.bacon.experimental.impl.dependencies;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.jboss.da.model.rest.GAV;

import java.util.Set;

@Data
@EqualsAndHashCode(exclude = { "dependencies", "depth", "cutDependency", "name", "conflictingName" })
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
        return "Project{" + "sourceCodeURL='" + sourceCodeURL + '\'' + ", sourceCodeRevision='" + sourceCodeRevision
                + '\'' + ", gavs=" + gavs + ", name='" + name + '\'' + '}';
    }
}
