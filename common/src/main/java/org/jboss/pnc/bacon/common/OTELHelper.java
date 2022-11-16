package org.jboss.pnc.bacon.common;

import com.redhat.resilience.otel.internal.EnvarExtractingPropagator;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@UtilityClass
public class OTELHelper {
    private SpanProcessor processor = null;

    private Span root = null;

    public void startOtel(String serviceName, String commandName, SpanProcessor p) {
        processor = p;
        if (serviceName == null) {
            serviceName = "bacon";
        }
        if (commandName == null) {
            commandName = serviceName;
        }

        Resource resource = Resource.getDefault()
                .merge(Resource.create(Attributes.of(ResourceAttributes.SERVICE_NAME, serviceName)));

        SdkTracerProvider sdkTracerProvider = SdkTracerProvider.builder()
                .addSpanProcessor(processor)
                .setResource(resource)
                .build();

        // NOTE the use of EnvarExtractingPropagator here OpenTelemetrySdk openTelemetry = OpenTelemetrySdk.builder()
        OpenTelemetrySdk openTelemetry = OpenTelemetrySdk.builder()
                .setTracerProvider(sdkTracerProvider)
                .setPropagators(ContextPropagators.create(EnvarExtractingPropagator.getInstance()))
                .buildAndRegisterGlobal();

        Context parentContext = EnvarExtractingPropagator.getInstance().extract(Context.current(), null, null);
        root = openTelemetry.getTracer(serviceName).spanBuilder(commandName).setParent(parentContext).startSpan();

        root.makeCurrent();
        log.debug(
                "Running with traceId {} spanId {}",
                Span.current().getSpanContext().getTraceId(),
                Span.current().getSpanContext().getSpanId());
    }

    public boolean otelEnabled() {
        return processor != null;
    }

    public void stopOtel() {
        if (root != null && processor != null) {
            log.debug("Finishing OTEL instrumentation for {}", root);
            root.end();
            processor.close();
        }
    }

    public String generateTraceParent(String trace, String span) {
        return "00" + '-' + trace + '-' + span + "-01";
    }
}
