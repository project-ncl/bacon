package org.jboss.bacon.tempname.impl.projectfinder;

import lombok.Data;
import org.jboss.da.model.rest.GAV;
import org.jboss.pnc.dto.BuildConfiguration;

import java.util.Set;

@Data
public class FoundProject {
    private Set<GAV> gavs; // maybe use different GAV class?
    private BuildConfiguration buildConfig;
    private boolean exactMatch;
}
