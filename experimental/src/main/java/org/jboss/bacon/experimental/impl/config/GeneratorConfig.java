package org.jboss.bacon.experimental.impl.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class GeneratorConfig {
    private DependencyResolutionConfig dependencyResolutionConfig = new DependencyResolutionConfig();
    private BuildConfigGeneratorConfig buildConfigGeneratorConfig = new BuildConfigGeneratorConfig();
}
