package org.jboss.pnc.bacon.pig.impl.addons.scanservice.pssaas;

import org.apache.commons.io.IOUtils;
import org.jboss.pnc.bacon.pig.impl.addons.scanservice.PostBuildScanService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;
import javax.ws.rs.client.Entity;

import java.io.BufferedInputStream;
import java.io.IOException;

import static org.apache.commons.io.IOUtils.toInputStream;

public class RedirectAndLog implements ClientResponseFilter {
    private static final Logger log = LoggerFactory.getLogger(PostBuildScanService.class);

    @Override
    public void filter(ClientRequestContext requestContext, ClientResponseContext responseContext) {

        // 302 redirect as underlying Apache Http client does not handle redirects with POST requests (only get)
        if (responseContext.getStatus() == 302) {
            // Make the request again
            requestContext.getClient()
                    .target(responseContext.getLocation())
                    .request()
                    .headers(requestContext.getHeaders())
                    .method(requestContext.getMethod(), Entity.json(requestContext.getEntity()));

        }

        // Log all responses, we don't actually need to handle them
        if (responseContext.hasEntity()) {
            // Read the output stream of the request object, then write back the string value, if we don#t write it back
            // the entity wil be null
            BufferedInputStream stream = new BufferedInputStream(responseContext.getEntityStream());
            String payload = null;
            try {
                payload = IOUtils.toString(stream, "UTF-8");
                log.info("PSSaaS Service Response: " + payload);
                responseContext.setEntityStream(toInputStream(payload, "UTF-8"));
            } catch (IOException e) {
                log.error(e.toString());
            }
        }
    }
}
