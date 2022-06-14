package org.jboss.pnc.bacon.pig.impl.addons.scanservice.pssaas;

import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;

import java.net.URI;
import java.util.Map;
import java.util.TreeMap;

public class ScanHelper {

    public ScanInterface client;
    private static Map<String, String> authHeaders = new TreeMap<String, String>();

    public ScanHelper(String pssaasSecretKey, String pssaasSecretValue, URI target) {
        authHeaders.put("PSSC-Secret-Key", pssaasSecretKey);
        authHeaders.put("PSSC-Secret-Value", pssaasSecretValue);
        client = new ResteasyClientBuilder().build()
                .target(target)
                .register(new AddAuthHeadersRequestFilter(authHeaders))
                .register(new RedirectAndLog())
                .proxy(ScanInterface.class);
    }
}
