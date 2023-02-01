package org.jboss.bacon.experimental.impl.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import javax.validation.constraints.NotNull;

import java.util.Set;

@Data
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class DependencyResolutionConfig {
    @NotNull
    private Set<String> excludeArtifacts = Set.of();
    @NotNull
    private Set<String> includeArtifacts = Set.of();
    @NotNull
    private Set<String> analyzeArtifacts = Set.of();
    private String analyzeBOM;
    private boolean includeOptionalDependencies = true;
}
