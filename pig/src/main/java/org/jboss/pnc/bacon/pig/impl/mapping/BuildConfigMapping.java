package org.jboss.pnc.bacon.pig.impl.mapping;

import org.jboss.pnc.bacon.pig.impl.config.BuildConfig;
import org.jboss.pnc.dto.BuildConfiguration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;

public class BuildConfigMapping {

    public static BuildConfig toBuildConfig(BuildConfiguration buildConfiguration) {
        BuildConfig buildConfig = new BuildConfig();
        buildConfig.setName(buildConfiguration.getName());
        buildConfig.setProject(buildConfiguration.getProject().getName());
        buildConfig.setBuildScript(buildConfiguration.getBuildScript());

        if (buildConfiguration.getScmRepository().getExternalUrl() != null) {
            buildConfig.setScmUrl(buildConfiguration.getScmRepository().getExternalUrl());
        } else {
            buildConfig.setScmUrl(buildConfiguration.getScmRepository().getInternalUrl());
        }

        buildConfig.setScmRevision(buildConfiguration.getScmRevision());
        buildConfig.setDescription(buildConfiguration.getDescription());

        // favor systemImageId
        buildConfig.setSystemImageId(buildConfiguration.getEnvironment().getSystemImageId());
        buildConfig.setDependencies(new ArrayList<>(buildConfiguration.getDependencies().keySet()));

        buildConfig.setBrewPullActive(buildConfiguration.getBrewPullActive());
        buildConfig.setBuildType(buildConfiguration.getBuildType().toString());

        setBuildConfigFieldsBasedOnParameters(buildConfiguration, buildConfig);
        return buildConfig;
    }

    static void setBuildConfigFieldsBasedOnParameters(BuildConfiguration buildConfiguration, BuildConfig buildConfig) {

        Map<String, String> parameters = buildConfiguration.getParameters();

        // TODO: could this code be unified with the code in BuildConfig.java?
        if (parameters.containsKey("ALIGNMENT_PARAMETERS")) {
            String[] alignmentParameters = parameters.get("ALIGNMENT_PARAMETERS").split(",");
            buildConfig.setAlignmentParameters(new HashSet<>(Arrays.asList(alignmentParameters)));
        }

        if (parameters.containsKey("BUILDER_POD_MEMORY")) {
            buildConfig.setBuildPodMemory(Double.parseDouble(parameters.get("BUILDER_POD_MEMORY")));
        }

        if (parameters.containsKey("BUILD_CATEGORY")) {
            buildConfig.setBuildCategory(parameters.get("BUILD_CATEGORY"));
        }

        if (parameters.containsKey("PIG_YAML_METADATA")) {
            buildConfig.setPigYamlMetadata(parameters.get("PIG_YAML_METADATA"));
        }

        if (parameters.containsKey("BREW_BUILD_NAME")) {
            buildConfig.setBrewBuildName(parameters.get("BREW_BUILD_NAME"));
        }

        if (parameters.containsKey("EXTRA_REPOSITORIES")) {
            String[] extraRepositories = parameters.get("EXTRA_REPOSITORIES").split("\n");
            buildConfig.setExtraRepositories(new HashSet<>(Arrays.asList(extraRepositories)));
        }
    }
}
