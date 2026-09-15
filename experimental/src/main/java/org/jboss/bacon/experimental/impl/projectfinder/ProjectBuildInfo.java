package org.jboss.bacon.experimental.impl.projectfinder;

import org.jboss.pnc.api.enums.BuildType;

import lombok.Data;

@Data
@lombok.Builder
public class ProjectBuildInfo {
    private JdkVersion jdkVersion;
    private BuildType buildType;
    private String buildToolVersion;
    private String buildToolVersionRange;
    private String detectionSource;
}
