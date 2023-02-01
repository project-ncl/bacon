package org.jboss.bacon.experimental.impl.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@Data
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class GeneratorConfig {
    @Valid
    @NotNull
    private DependencyResolutionConfig dependencyResolutionConfig = new DependencyResolutionConfig();
    @Valid
    @NotNull
    private BuildConfigGeneratorConfig buildConfigGeneratorConfig = new BuildConfigGeneratorConfig();
}
