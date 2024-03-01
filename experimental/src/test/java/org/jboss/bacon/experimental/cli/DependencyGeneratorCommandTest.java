package org.jboss.bacon.experimental.cli;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DependencyGeneratorCommandTest {

    @Test
    void testLoadConfigWithConfigRefs() throws URISyntaxException {
        DependencyGeneratorCommand command = new DependencyGeneratorCommand();
        URL testConfigLoc = getClass().getResource("/config/auto-build-envs.yaml");
        File configFile = Paths.get(testConfigLoc.toURI()).toFile();

        Path configPath = configFile.toPath();
        command.config = configPath;

        var generatedConfig = command.loadConfig();
        var defaults = generatedConfig.getBuildConfigGeneratorConfig().getDefaultValues();

        assertEquals(System.getProperty("user.home"), defaults.getScmRevision());
        assertEquals(System.getenv("PWD"), defaults.getBuildScript());
    }

}
