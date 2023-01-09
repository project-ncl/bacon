package org.jboss.bacon.tempname.impl.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.Set;

@Data
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class DependencyResolutionConfig {
    private Set<String> excludeArtifacts = Set.of();
    private Set<String> includeArtifacts = Set.of();
    private Set<String> analyzeArtifacts = Set.of();
}
