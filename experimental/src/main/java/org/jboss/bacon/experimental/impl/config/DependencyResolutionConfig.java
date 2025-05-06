package org.jboss.bacon.experimental.impl.config;

import java.util.List;
import java.util.Set;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;

/**
 * Configuration for the autobuilder dependency resolution.
 */
@Data
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class DependencyResolutionConfig {

    /**
     * List of toplevel artifacts that should be analyzed and built.
     */
    @NotNull
    private Set<String> analyzeArtifacts = Set.of();

    /**
     * Bom that should be analyzed and which artifacts should be built.
     */
    private String analyzeBOM;

    /**
     * Whether optional dependencies should be included in the analysis.
     */
    private boolean includeOptionalDependencies = true;

    /**
     * List of artifacts that should be excluded from the analysis.
     *
     * The matching artifact and it's dependencies will be omitted from the resulting build tree. The format is
     * G:A:[C:T:]V where every part can be replaced with '*'.
     */
    @NotNull
    private Set<String> excludeArtifacts = Set.of();

    /**
     * If set to true, all -redhat-X artifacts and their dependencies are omitted from the resulting build tree. Note
     * that this option has effect only when {@link #rebuildNonAutoBuilds} is true.
     */
    private boolean excludeProductizedArtifacts = false;

    /**
     * List of artifacts that should not be excluded from the analysis.
     *
     * The format is G:A:[C:T:]V where every part can be replaced with '*'.
     */
    @NotNull
    private Set<String> includeArtifacts = Set.of();

    /**
     * The list of URLs to use for the SCM recipes. The SCM recipes are used for determining SCM coordinates for
     * project.
     */
    private List<String> recipeRepos = List.of();

    /**
     * When enabled, autobuilder will rebuild existing builds, if they are not managed by autobuilder (Build Configs
     * with -AUTOBUILD suffix) and will manage them (create a copy of the existing build's Build Config with -AUTOBUILD
     * suffix). This is useful to make sure whole dependency tree is build correctly.
     */
    private boolean rebuildNonAutoBuilds = false;
}
