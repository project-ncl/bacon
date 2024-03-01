package org.jboss.bacon.experimental.cli;

import org.jboss.bacon.experimental.impl.config.GeneratorConfig;
import org.jboss.pnc.bacon.common.exception.FatalException;
import org.jboss.pnc.bacon.config.Validate;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.TypeDescription;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.AbstractConstruct;
import org.yaml.snakeyaml.env.EnvScalarConstructor;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.Tag;
import picocli.CommandLine;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

        Yaml yaml = new Yaml(new PropertyScalarConstructor(GeneratorConfig.class));
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

    private static class PropertyScalarConstructor extends EnvScalarConstructor {
        public static final Tag PROP_TAG = new Tag("!SYSPROP");
        private static final Pattern PROP_FORMAT = Pattern.compile("^\\$\\{\\s*(?<name>[^}]+)\\s*\\}$");

        private class ConstructProp extends AbstractConstruct {
            public Object construct(Node node) {
                String val = constructScalar((ScalarNode) node);
                Matcher matcher = PROP_FORMAT.matcher(val);
                matcher.matches();
                String name = matcher.group("name");
                return apply(name, null, "", getProperty(name));
            }
        }

        PropertyScalarConstructor(Class<?> binding) {
            super(new TypeDescription(binding), Collections.emptyList(), new LoaderOptions());
            this.yamlConstructors.put(PROP_TAG, new ConstructProp());
        }

        public String getProperty(String key) {
            return System.getProperty(key);
        }
    }
}
