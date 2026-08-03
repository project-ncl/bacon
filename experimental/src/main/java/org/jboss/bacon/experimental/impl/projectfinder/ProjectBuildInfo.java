package org.jboss.bacon.experimental.impl.projectfinder;

import org.jboss.pnc.api.enums.BuildType;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProjectBuildInfo {
    private JdkVersion jdkVersion;
    private BuildType buildType;
    private String buildToolVersion;
    private String buildToolVersionRange;
    private String detectionSource;
}
