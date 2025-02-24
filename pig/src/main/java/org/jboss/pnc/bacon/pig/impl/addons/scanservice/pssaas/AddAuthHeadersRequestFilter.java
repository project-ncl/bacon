package org.jboss.pnc.bacon.pig.impl.addons.scanservice.pssaas;

import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;

public class AddAuthHeadersRequestFilter implements ClientRequestFilter {
    private Map<String, String> headers = new TreeMap<String, String>();

    public AddAuthHeadersRequestFilter(Map<String, String> headers) {
        this.headers = headers;
    }

    public void addAuthHeader(String key, String value) {
        this.headers.put(key, value);
    }

    @Override
    public void filter(ClientRequestContext requestContext) throws IOException {
        for (Map.Entry<String, String> entry : this.headers.entrySet()) {
            requestContext.getHeaders().add(entry.getKey(), entry.getValue());
        }
    }
}
