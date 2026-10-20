package org.jboss.pnc.bacon.pnc.common;

import java.util.function.Function;

import org.jboss.pnc.bacon.auth.client.PncClientHelper;
import org.jboss.pnc.client.ClientBase;
import org.jboss.pnc.client.Configuration;

import lombok.extern.slf4j.Slf4j;

/**
 * Helper class to create the PNC CLI client to use in the CLI objects. There are two variants, one authenticating
 * only if the active profile defines credentials, and one always requiring them.
 */
@Slf4j
public class ClientCreator<T extends ClientBase<?>> {
    private final Function<Configuration, T> constructor;

    /**
     * Example: {@code ClientCreator<BuildClient> CREATOR = new ClientCreator<>(BuildClient::new);}
     *
     * @param constructor Constructor of the client
     */
    public ClientCreator(Function<Configuration, T> constructor) {
        this.constructor = constructor;
    }

    /**
     * Get a new PNC Client object.
     *
     * @return PNC Client, authenticated if the active profile defines credentials
     */
    public T newClient() {
        return newClientPrivate(PncClientHelper.credentialsAvailable());
    }

    /**
     * Get a new authenticated PNC Client object.
     *
     * @return Authenticated PNC Client
     */
    public T newClientAuthenticated() {
        return newClientPrivate(true);
    }

    private T newClientPrivate(boolean authenticated) {
        return constructor.apply(PncClientHelper.getPncConfiguration(authenticated));
    }
}
