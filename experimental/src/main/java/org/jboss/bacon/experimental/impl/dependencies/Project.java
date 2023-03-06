package org.jboss.bacon.experimental.impl.dependencies;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.jboss.da.model.rest.GAV;

import java.util.Set;

@Data
@ToString(exclude = { "dependencies", "depth" })
@EqualsAndHashCode(exclude = { "dependencies", "depth" })
public class Project {
    private String sourceCodeURL;
    private String sourceCodeRevision;
    private Set<GAV> gavs;
    private Set<Project> dependencies;
    private int depth = -1;
    private boolean cutDepenendecy = false;

    public GAV getFirstGAV() {
        return gavs.stream().sorted().findFirst().get();
    }
}
