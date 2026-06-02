package org.jboss.pnc.bacon.common;

import java.io.IOException;
import java.util.function.Supplier;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.core.MultivaluedMap;

public class TokenAuthenticator implements ClientRequestFilter {

    private final Supplier<String> tokenSupplier;
    /**
     * whether it is 'Bearer', 'Basic'
     */
    private final String authScheme;

    /**
     * Initialization
     *
     * @param authScheme whether it is Bearer (OIDC), Basic (LDAP, etc)
     * @param tokenSupplier method for providing the token
     */
    public TokenAuthenticator(String authScheme, Supplier<String> tokenSupplier) {
        this.authScheme = authScheme;
        this.tokenSupplier = tokenSupplier;
    }

    public void filter(ClientRequestContext requestContext) throws IOException {
        MultivaluedMap<String, Object> headers = requestContext.getHeaders();
        headers.add("Authorization", authScheme + " " + this.tokenSupplier.get());
    }
}
