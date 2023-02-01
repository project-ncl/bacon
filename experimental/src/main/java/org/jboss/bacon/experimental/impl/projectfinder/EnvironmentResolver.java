package org.jboss.bacon.experimental.impl.projectfinder;

import lombok.extern.slf4j.Slf4j;
import org.jboss.bacon.experimental.impl.config.BuildConfigGeneratorConfig;
import org.jboss.pnc.bacon.common.exception.FatalException;
import org.jboss.pnc.bacon.pnc.common.ClientCreator;
import org.jboss.pnc.client.EnvironmentClient;
import org.jboss.pnc.client.RemoteCollection;
import org.jboss.pnc.client.RemoteResourceException;
import org.jboss.pnc.dto.Environment;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Slf4j
public class EnvironmentResolver {
    private BuildConfigGeneratorConfig config;

    private final EnvironmentClient environmentClient;

    private final Set<Environment> environments = new HashSet<>();

    private final Set<String> nonDeprecatedNames = new HashSet<>();

    public EnvironmentResolver(BuildConfigGeneratorConfig buildConfigGeneratorConfig) {
        this.config = buildConfigGeneratorConfig;
        environmentClient = new ClientCreator<>(EnvironmentClient::new).newClient();
        init();
    }

    private void init() {
        try {
            RemoteCollection<Environment> all = environmentClient.getAll(Optional.empty(), Optional.empty());
            for (Environment env : all) {
                environments.add(env);
                if (isValidEnvironment(env)) {
                    nonDeprecatedNames.add(env.getName());
                }
            }
            validateDefaultEnvironment();
        } catch (RemoteResourceException e) {
            throw new FatalException("Failed to load PNC environment list.", e);
        }
    }

    private void validateDefaultEnvironment() {
        String defaultEnv = config.getDefaultValues().getEnvironmentName();
        if (!nonDeprecatedNames.contains(defaultEnv)) {
            throw new FatalException(
                    "Could not find environment \"" + defaultEnv
                            + "\" in PNC, update default environment value in config.");
        }
    }

    public boolean isValidName(String name) {
        return nonDeprecatedNames.contains(name);
    }

    public boolean isValidEnvironment(Environment env) {
        return !env.isDeprecated() && !env.isHidden();
    }

}
