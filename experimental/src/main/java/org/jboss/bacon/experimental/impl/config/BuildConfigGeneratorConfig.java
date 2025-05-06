package org.jboss.bacon.experimental.impl.config;

import java.util.LinkedHashMap;
import java.util.Set;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.jboss.pnc.bacon.pig.impl.config.PigConfiguration;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;

/**
 * Configuration for how the new build configs should be created.
 */
@Data
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class BuildConfigGeneratorConfig {
    /**
     * Template of the PiG build-config.yaml to use.
     */
    @NotNull
    private PigConfiguration pigTemplate;

    /**
     * SCM URLs which contain one of the string listed will be replaced by the default placeholder scmUrl.
     */
    @NotNull
    private Set<String> scmReplaceWithPlaceholder = Set.of();

    /**
     * SCM URLs will have the listed key substring replaced with the value.
     */
    @NotNull
    private LinkedHashMap<String, String> scmPattern = new LinkedHashMap<>();

    /**
     * SCM URLs containing the listed key are wholly replaced by the value.
     */
    @NotNull
    private LinkedHashMap<String, String> scmMapping = new LinkedHashMap<>();

    /**
     * When generating new or copying and changing existing build config, will add 'false' at the end of the build
     * script to make sure the build fails.
     *
     * This can be helpful to make sure the configs are manually reviewed.
     */
    private boolean failGeneratedBuildScript = true;

    /**
     * Default values to be used when generating build config
     */
    @Valid
    @NotNull
    private DefaultBuildConfigValues defaultValues = new DefaultBuildConfigValues();

    /**
     * If true, uses systemImageId to specify environment (to force use of specific environment) when the environment
     * defined by environmentName would not be found because it was deprecated.
     */
    private boolean allowDeprecatedEnvironments = false;
}
