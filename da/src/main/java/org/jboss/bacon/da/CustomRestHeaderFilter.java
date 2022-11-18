package org.jboss.bacon.da;

import org.jboss.pnc.bacon.common.OTELHelper;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;

import java.io.IOException;

public class CustomRestHeaderFilter implements ClientRequestFilter {
    private final String trace;

    private final String span;

    public CustomRestHeaderFilter(String trace, String span) {
        this.span = span;
        this.trace = trace;
    }

    @Override
    public void filter(ClientRequestContext requestContext) throws IOException {
        requestContext.getHeaders().add("trace-id", trace);
        requestContext.getHeaders().add("span-id", span);
        requestContext.getHeaders().add("traceparent", OTELHelper.generateTraceParent(trace, span));
    }
}
