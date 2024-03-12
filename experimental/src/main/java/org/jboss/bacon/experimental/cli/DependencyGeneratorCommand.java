package org.jboss.bacon.experimental.cli;

import org.jboss.bacon.experimental.impl.config.GeneratorConfig;
import org.jboss.pnc.bacon.common.exception.FatalException;
import org.jboss.pnc.bacon.config.Validate;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import picocli.CommandLine;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

public class DependencyGeneratorCommand extends ExperimentalCommand {
    @CommandLine.Option(names = { "--project-dir" }, description = "Project directory")
    protected Path projectDir;

    @CommandLine.Option(
            names = { "--domino-config" },
            description = "Path to the domino config json. The config is "
                    + "used to initialize the dependency resolution config and then the autobuilder config is applied.")
    protected Path dominoConfig;

    @CommandLine.Parameters(description = "Autobuilder configuration file")
    protected Path config;

    protected GeneratorConfig loadConfig() {
        if (config == null) {
            throw new FatalException("You need to specify the configuration directory!");
        }

        Yaml yaml = new Yaml(new Constructor(GeneratorConfig.class));
        try (BufferedReader reader = Files.newBufferedReader(config)) {
            GeneratorConfig config = yaml.load(reader);
            validate(config);
            return config;
        } catch (IOException e) {
            throw new FatalException("Unable to load config file", e);
        }
    }

    private void validate(GeneratorConfig config) {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();
            Set<ConstraintViolation<GeneratorConfig>> violations = validator.validate(config);

            if (!violations.isEmpty()) {
                throw new FatalException(
                        "Errors while validating the autobuilder config.yaml:\n"
                                + Validate.<GeneratorConfig> prettifyConstraintViolation(violations));
            }
        }
    }
}
