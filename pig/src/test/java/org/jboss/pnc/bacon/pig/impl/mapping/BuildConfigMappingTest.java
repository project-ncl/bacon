package org.jboss.pnc.bacon.pig.impl.mapping;

import org.jboss.pnc.bacon.pig.impl.config.BuildConfig;
import org.jboss.pnc.dto.BuildConfiguration;
import org.jeasy.random.EasyRandom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class BuildConfigMappingTest {

    BuildConfig buildConfig;

    @BeforeEach
    void setup() {
        buildConfig = new BuildConfig();
    }

    @Test
    void testAlignmentParametersSet() {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("ALIGNMENT_PARAMETERS", "-Dtest=true -Dme=false");
        BuildConfiguration buildConfiguration = BuildConfiguration.builder().parameters(parameters).build();

        BuildConfigMapping.setBuildConfigFieldsBasedOnParameters(buildConfiguration, buildConfig);
        assertEquals("-Dtest=true -Dme=false", buildConfig.getAlignmentParameters().stream().findFirst().get());

        // make sure no other parameters are set
        assertNull(buildConfig.getBuildPodMemory());
        assertNull(buildConfig.getBuildCategory());
        assertNull(buildConfig.getPigYamlMetadata());
        assertNull(buildConfig.getBrewBuildName());
        assertTrue(buildConfig.getExtraRepositories().size() == 0);
    }

    @Test
    void testBuilderPodMemorySet() {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("BUILDER_POD_MEMORY", "10");
        BuildConfiguration buildConfiguration = BuildConfiguration.builder().parameters(parameters).build();

        BuildConfigMapping.setBuildConfigFieldsBasedOnParameters(buildConfiguration, buildConfig);
        assertTrue(Math.abs(10.0 - buildConfig.getBuildPodMemory()) <= 0.000001);

        // make sure no other parameters are set
        assertTrue(buildConfig.getAlignmentParameters().size() == 0);
        assertNull(buildConfig.getBuildCategory());
        assertNull(buildConfig.getPigYamlMetadata());
        assertNull(buildConfig.getBrewBuildName());
        assertTrue(buildConfig.getExtraRepositories().size() == 0);
    }

    @Test
    void testBuildCategorySet() {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("BUILD_CATEGORY", "SERVICE");
        BuildConfiguration buildConfiguration = BuildConfiguration.builder().parameters(parameters).build();

        BuildConfigMapping.setBuildConfigFieldsBasedOnParameters(buildConfiguration, buildConfig);
        assertEquals("SERVICE", buildConfig.getBuildCategory());

        // make sure no other parameters are set
        assertTrue(buildConfig.getAlignmentParameters().size() == 0);
        assertNull(buildConfig.getBuildPodMemory());
        assertNull(buildConfig.getPigYamlMetadata());
        assertNull(buildConfig.getBrewBuildName());
        assertTrue(buildConfig.getExtraRepositories().size() == 0);
    }

    @Test
    void testPigYamlMetadataSet() {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("PIG_YAML_METADATA", "12345");
        BuildConfiguration buildConfiguration = BuildConfiguration.builder().parameters(parameters).build();

        BuildConfigMapping.setBuildConfigFieldsBasedOnParameters(buildConfiguration, buildConfig);
        assertEquals("12345", buildConfig.getPigYamlMetadata());

        // make sure no other parameters are set
        assertTrue(buildConfig.getAlignmentParameters().size() == 0);
        assertNull(buildConfig.getBuildPodMemory());
        assertNull(buildConfig.getBuildCategory());
        assertNull(buildConfig.getBrewBuildName());
        assertTrue(buildConfig.getExtraRepositories().size() == 0);
    }

    @Test
    void testBrewBuildNameSet() {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("BREW_BUILD_NAME", "testme");
        BuildConfiguration buildConfiguration = BuildConfiguration.builder().parameters(parameters).build();

        BuildConfigMapping.setBuildConfigFieldsBasedOnParameters(buildConfiguration, buildConfig);
        assertEquals("testme", buildConfig.getBrewBuildName());

        // make sure no other parameters are set
        assertTrue(buildConfig.getAlignmentParameters().size() == 0);
        assertNull(buildConfig.getBuildPodMemory());
        assertNull(buildConfig.getBuildCategory());
        assertNull(buildConfig.getPigYamlMetadata());
        assertTrue(buildConfig.getExtraRepositories().size() == 0);
    }

    @Test
    void testExtraRepositoriesSet() {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("EXTRA_REPOSITORIES", "hello\nworld");
        BuildConfiguration buildConfiguration = BuildConfiguration.builder().parameters(parameters).build();

        BuildConfigMapping.setBuildConfigFieldsBasedOnParameters(buildConfiguration, buildConfig);
        assertTrue(buildConfig.getExtraRepositories().contains("hello"));
        assertTrue(buildConfig.getExtraRepositories().contains("world"));
        assertTrue(buildConfig.getExtraRepositories().size() == 2);

        // make sure no other parameters are set
        assertTrue(buildConfig.getAlignmentParameters().size() == 0);
        assertNull(buildConfig.getBuildPodMemory());
        assertNull(buildConfig.getBuildCategory());
        assertNull(buildConfig.getPigYamlMetadata());
        assertNull(buildConfig.getBrewBuildName());
    }

    @Test
    void testCanSetMultipleParameters() {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("ALIGNMENT_PARAMETERS", "-Dtest=true -Dme=false");
        parameters.put("BREW_BUILD_NAME", "testme");
        BuildConfiguration buildConfiguration = BuildConfiguration.builder().parameters(parameters).build();

        BuildConfigMapping.setBuildConfigFieldsBasedOnParameters(buildConfiguration, buildConfig);
        assertEquals("-Dtest=true -Dme=false", buildConfig.getAlignmentParameters().stream().findFirst().get());
        assertEquals("testme", buildConfig.getBrewBuildName());

        // make sure no other parameters are set
        assertNull(buildConfig.getBuildPodMemory());
        assertNull(buildConfig.getBuildCategory());
        assertNull(buildConfig.getPigYamlMetadata());
        assertTrue(buildConfig.getExtraRepositories().size() == 0);
    }

    @Test
    void testEntireMapping() {
        EasyRandom easyRandom = new EasyRandom();
        BuildConfiguration buildConfiguration = easyRandom.nextObject(BuildConfiguration.class);
        BuildConfig bc = BuildConfigMapping.toBuildConfig(buildConfiguration);
        assertEquals(buildConfiguration.getName(), bc.getName());
        assertEquals(buildConfiguration.getProject().getName(), bc.getProject());
        assertEquals(buildConfiguration.getBuildScript(), bc.getBuildScript());
        assertEquals(buildConfiguration.getScmRepository().getExternalUrl(), bc.getScmUrl());
        assertEquals(buildConfiguration.getScmRevision(), bc.getScmRevision());
        assertEquals(buildConfiguration.getDescription(), bc.getDescription());
        assertEquals(buildConfiguration.getEnvironment().getSystemImageId(), bc.getSystemImageId());
        assertEquals(buildConfiguration.getDependencies().keySet(), new HashSet(bc.getDependencies()));
        assertEquals(buildConfiguration.getBrewPullActive(), bc.getBrewPullActive());
        assertEquals(buildConfiguration.getBuildType().toString(), bc.getBuildType());
    }
}
