package org.jboss.pnc.bacon.pnc.common;

import lombok.extern.slf4j.Slf4j;
import org.jboss.pnc.bacon.pnc.client.PncClientHelper;
import org.jboss.pnc.client.ClientBase;
import org.jboss.pnc.client.Configuration;

import java.util.function.Function;

/**
 * Helper class to create the PNC CLI client to use in the CLI objects. There are two variants, the unauthenticated one
 * and the authenticated one.
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
     * Get a new unauthenticated PNC Client object.
     *
     * @return Unauthenticated PNC Client
     */
    public T newClient() {
        return newClientPrivate(false);
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
