package org.jboss.pnc.bacon.pnc.common;

import lombok.extern.slf4j.Slf4j;
import org.jboss.pnc.bacon.pnc.client.PncClientHelper;
import org.jboss.pnc.client.ClientBase;
import org.jboss.pnc.client.Configuration;

import java.util.function.Function;

/**
 * Helper class to create the PNC CLI client to use in the CLI objects. There are 2 variants, the unauthenticated one and the
 * authenticated one
 *
 * @param <T> : PNC client to initialize
 */
@Slf4j
public class ClientCreator<T extends ClientBase> {

    private T client;
    private T clientAuthenticated;

    private Function<Configuration, T> constructor;

    /**
     * Example: ClientCreator<BuildClient> CREATOR = new ClientCreator<>(BuildClient::new);
     *
     * @param constructor: constructor of the client
     */
    public ClientCreator(Function<Configuration, T> constructor) {
        this.constructor = constructor;
    }

    /**
     * Get the un-authenticated PNC Client object. If already created, object is cached
     *
     * @return Un-authenticated PNC Client
     */
    public T getClient() {

        if (client == null) {
            client = getClientPrivate(false);
        }
        return client;
    }

    /**
     * Get the authenticated PNC Client object. If already created, object is cached
     *
     * @return Authenticated PNC Client
     */
    public T getClientAuthenticated() {

        if (clientAuthenticated == null) {
            clientAuthenticated = getClientPrivate(true);
        }
        return clientAuthenticated;
    }

    @SuppressWarnings("unchecked")
    private T getClientPrivate(boolean authenticated) {
        return constructor.apply(PncClientHelper.getPncConfiguration(authenticated));
    }
}
