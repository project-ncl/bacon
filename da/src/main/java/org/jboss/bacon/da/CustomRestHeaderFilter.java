package org.jboss.bacon.da;

import io.opentelemetry.api.trace.SpanContext;
import org.jboss.pnc.api.constants.MDCHeaderKeys;
import org.jboss.pnc.common.otel.OtelUtils;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;

import java.io.IOException;

public class CustomRestHeaderFilter implements ClientRequestFilter {

    private final SpanContext spanContext;

    public CustomRestHeaderFilter(SpanContext spanContext) {
        this.spanContext = spanContext;
    }

    @Override
    public void filter(ClientRequestContext requestContext) throws IOException {
        requestContext.getHeaders().add(MDCHeaderKeys.TRACE_ID.getHeaderName(), spanContext.getTraceId());
        requestContext.getHeaders().add(MDCHeaderKeys.SPAN_ID.getHeaderName(), spanContext.getSpanId());
        OtelUtils.createTraceStateHeader(spanContext).forEach((k, v) -> requestContext.getHeaders().add(k, v));
        OtelUtils.createTraceParentHeader(spanContext).forEach((k, v) -> requestContext.getHeaders().add(k, v));
    }
}
