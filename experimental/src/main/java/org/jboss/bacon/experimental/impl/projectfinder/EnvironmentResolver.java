package org.jboss.bacon.experimental.impl.projectfinder;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import lombok.extern.slf4j.Slf4j;

import org.jboss.bacon.experimental.impl.config.BuildConfigGeneratorConfig;
import org.jboss.pnc.bacon.common.exception.FatalException;
import org.jboss.pnc.bacon.pnc.common.ClientCreator;
import org.jboss.pnc.client.EnvironmentClient;
import org.jboss.pnc.client.RemoteCollection;
import org.jboss.pnc.client.RemoteResourceException;
import org.jboss.pnc.dto.Environment;

@Slf4j
public class EnvironmentResolver {

    private final Map<String, Environment> environments = new HashMap<>();

    public EnvironmentResolver(BuildConfigGeneratorConfig config) {
        try {
            EnvironmentClient environmentClient = new ClientCreator<>(EnvironmentClient::new).newClient();
            RemoteCollection<Environment> all = environmentClient.getAll(Optional.empty(), Optional.empty());
            for (Environment env : all) {
                environments.put(env.getId(), env);
            }
            validateDefaultEnvironment(config.getDefaultValues().getEnvironmentName());
        } catch (RemoteResourceException e) {
            throw new FatalException("Failed to load PNC environment list.", e);
        }
    }

    private void validateDefaultEnvironment(String defaultEnv) {
        Environment byName = null;
        for (Environment env : environments.values()) {
            if (env.getName().equals(defaultEnv)) {
                if (!env.isDeprecated()) {
                    return;
                }
                byName = env;
            }
        }
        if (byName == null) {
            throw new FatalException(
                    "Could not find environment \"" + defaultEnv
                            + "\" in PNC, update default environment value in config.");
        }
        Environment replacement = resolve(byName);
        String msg = "Default environment \"" + defaultEnv
                + "\" is deprecated, update default environment value in config.";
        if (!replacement.isDeprecated()) {
            msg += " Environment upgrade path suggests upgrading to \"" + replacement.getName() + "\"";
        }
        throw new FatalException(msg);
    }

    public Environment resolve(Environment env) {
        if (!env.isDeprecated()) {
            return env;
        }
        String replacementID = env.getAttributes().get("DEPRECATION_REPLACEMENT");// TODO: replace with constant from
                                                                                  // 2.5
        if (replacementID == null) {
            log.error(
                    "Environment " + env.getName() + " #" + env.getId()
                            + " is deprecated, but does not have a DEPRECATION_REPLACEMENT provided.");// TODO: replace
                                                                                                                                                                     // with constant
                                                                                                                                                                     // from 2.5
            return env;
        }
        Environment replacement = environments.get(replacementID);
        if (replacement == null) {
            log.error(
                    "Environment " + env.getName() + " #" + env.getId()
                            + " is deprecated, but DEPRECATION_REPLACEMENT points to invalid Environment #"
                            + replacementID + ".");// TODO: replace with constant from 2.5
            return env;
        }
        return resolve(replacement);
    }
}
