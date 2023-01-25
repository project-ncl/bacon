package org.jboss.bacon.experimental.impl.projectfinder;

import lombok.Data;
import org.jboss.da.model.rest.GAV;
import org.jboss.pnc.dto.BuildConfiguration;
import org.jboss.pnc.dto.BuildConfigurationRevision;

import java.util.Set;

@Data
public class FoundProject {
    private Set<GAV> gavs;
    private BuildConfigurationRevision buildConfigRevision;
    private BuildConfiguration buildConfig;
    /**
     * Did we find existing Build Config?
     */
    private boolean found;
    /**
     * Did the found build produced exact match of the version? Exact match means the same Major.Minor.Micro.Qualifier,
     * but can differ in the -redhat suffix.
     */
    private boolean exactMatch;
    /**
     * Did the found build produced all the GAs?
     */
    private boolean complete;
    /**
     * Is the Build Config Revision the latest revision for that Build Config?
     */
    private boolean latestRevision;
}
