package org.jboss.bacon.experimental.impl.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import org.jboss.pnc.bacon.pig.impl.config.PigConfiguration;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import java.util.LinkedHashMap;
import java.util.Set;

@Data
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class BuildConfigGeneratorConfig {
    @NotNull
    private PigConfiguration pigTemplate;
    @NotNull
    private Set<String> scmReplaceWithPlaceholder = Set.of();
    @NotNull
    private LinkedHashMap<String, String> scmPattern = new LinkedHashMap<>();
    @NotNull
    private LinkedHashMap<String, String> scmMapping = new LinkedHashMap<>();
    private boolean failGeneratedBuildScript = true;
    @Valid
    @NotNull
    private DefaultBuildConfigValues defaultValues = new DefaultBuildConfigValues();
}
