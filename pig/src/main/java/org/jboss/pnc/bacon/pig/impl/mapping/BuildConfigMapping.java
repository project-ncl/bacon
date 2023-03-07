package org.jboss.pnc.bacon.pig.impl.mapping;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jboss.pnc.bacon.pig.impl.config.BuildConfig;
import org.jboss.pnc.dto.BuildConfiguration;
import org.jboss.pnc.dto.BuildConfigurationRevision;
import org.jboss.pnc.dto.Environment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;

public class BuildConfigMapping {

    public static BuildConfig toBuildConfig(BuildConfiguration buildConfiguration, GeneratorOptions options) {
        BuildConfig buildConfig = new BuildConfig();

        buildConfig.setName(options.getNameOverride().orElseGet(buildConfiguration::getName));
        buildConfig.setProject(buildConfiguration.getProject().getName());
        buildConfig.setBuildScript(buildConfiguration.getBuildScript());

        if (buildConfiguration.getScmRepository().getExternalUrl() != null) {
            buildConfig.setScmUrl(buildConfiguration.getScmRepository().getExternalUrl());
        } else {
            buildConfig.setScmUrl(buildConfiguration.getScmRepository().getInternalUrl());
        }

        buildConfig.setScmRevision(buildConfiguration.getScmRevision());
        buildConfig.setDescription(buildConfiguration.getDescription());

        setEnvironment(buildConfig, buildConfiguration.getEnvironment(), options);
        buildConfig.setDependencies(new ArrayList<>(buildConfiguration.getDependencies().keySet()));

        buildConfig.setBrewPullActive(buildConfiguration.getBrewPullActive());
        buildConfig.setBuildType(buildConfiguration.getBuildType().toString());

        setBuildConfigFieldsBasedOnParameters(buildConfig, buildConfiguration.getParameters());
        return buildConfig;
    }

    public static BuildConfig toBuildConfig(
            BuildConfiguration buildConfiguration,
            BuildConfigurationRevision revision,
            GeneratorOptions options) {
        BuildConfig buildConfig = new BuildConfig();
        buildConfig.setName(options.getNameOverride().orElseGet(revision::getName));
        buildConfig.setProject(revision.getProject().getName());
        buildConfig.setBuildScript(revision.getBuildScript());

        if (revision.getScmRepository().getExternalUrl() != null) {
            buildConfig.setScmUrl(revision.getScmRepository().getExternalUrl());
        } else {
            buildConfig.setScmUrl(revision.getScmRepository().getInternalUrl());
        }

        buildConfig.setScmRevision(revision.getScmRevision());
        buildConfig.setDescription(buildConfiguration.getDescription());

        setEnvironment(buildConfig, revision.getEnvironment(), options);
        buildConfig.setDependencies(new ArrayList<>(buildConfiguration.getDependencies().keySet()));

        buildConfig.setBrewPullActive(buildConfiguration.getBrewPullActive());
        buildConfig.setBuildType(revision.getBuildType().toString());

        setBuildConfigFieldsBasedOnParameters(buildConfig, revision.getParameters());
        return buildConfig;
    }

    private static void setEnvironment(BuildConfig buildConfig, Environment environment, GeneratorOptions options) {
        // favor systemImageId
        if (options.useEnvironmentName) {
            buildConfig.setEnvironmentName(environment.getName());
        } else {
            buildConfig.setSystemImageId(environment.getSystemImageId());
        }
    }

    static void setBuildConfigFieldsBasedOnParameters(BuildConfig buildConfig, Map<String, String> parameters) {

        // TODO: could this code be unified with the code in BuildConfig.java?
        if (parameters.containsKey("ALIGNMENT_PARAMETERS")) {
            String alignmentParameters = parameters.get("ALIGNMENT_PARAMETERS");
            if (!alignmentParameters.isBlank()) {
                buildConfig.setAlignmentParameters(Collections.singleton(alignmentParameters));
            }
            parameters.remove("ALIGNMENT_PARAMETERS");
        }

        if (parameters.containsKey("BUILDER_POD_MEMORY")) {
            buildConfig.setBuildPodMemory(Double.parseDouble(parameters.get("BUILDER_POD_MEMORY")));
            parameters.remove("BUILDER_POD_MEMORY");
        }

        if (parameters.containsKey("BUILD_CATEGORY")) {
            buildConfig.setBuildCategory(parameters.get("BUILD_CATEGORY"));
            parameters.remove("BUILD_CATEGORY");
        }

        if (parameters.containsKey("PIG_YAML_METADATA")) {
            buildConfig.setPigYamlMetadata(parameters.get("PIG_YAML_METADATA"));
            parameters.remove("PIG_YAML_METADATA");
        }

        if (parameters.containsKey("BREW_BUILD_NAME")) {
            buildConfig.setBrewBuildName(parameters.get("BREW_BUILD_NAME"));
            parameters.remove("BREW_BUILD_NAME");
        }

        if (parameters.containsKey("EXTRA_REPOSITORIES")) {
            String[] extraRepositories = parameters.get("EXTRA_REPOSITORIES").split("\n");
            buildConfig.setExtraRepositories(new HashSet<>(Arrays.asList(extraRepositories)));
            parameters.remove("EXTRA_REPOSITORIES");
        }
        if (!parameters.isEmpty()) {
            buildConfig.setParameters(parameters);
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class GeneratorOptions {
        private boolean useEnvironmentName = false;
        private Optional<String> nameOverride = Optional.empty();
    }
}
