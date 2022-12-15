package org.jboss.bacon.tempname.impl.projectfinder;

import lombok.Data;
import org.jboss.da.model.rest.GAV;
import org.jboss.pnc.dto.BuildConfigurationRevision;

import java.util.Optional;
import java.util.Set;

@Data
public class FoundProject {
    private Set<GAV> gavs; // maybe use different GAV class?
    private Optional<BuildConfigurationRevision> buildConfig;
    private boolean exactMatch;
}
