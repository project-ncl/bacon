package org.jboss.pnc.bacon.common;

import java.io.IOException;
import java.util.function.Supplier;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.core.MultivaluedMap;

public class TokenAuthenticator implements ClientRequestFilter {

    private final Supplier<String> tokenSupplier;

    public TokenAuthenticator(Supplier<String> tokenSupplier) {
        this.tokenSupplier = tokenSupplier;
    }

    public void filter(ClientRequestContext requestContext) throws IOException {
        MultivaluedMap<String, Object> headers = requestContext.getHeaders();
        headers.add("Authorization", "Bearer " + this.tokenSupplier.get());
    }
}
