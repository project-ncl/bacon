package org.jboss.pnc.bacon.pnc.admin;

import java.util.concurrent.Callable;

import org.jboss.pnc.bacon.common.CustomRestHeaderFilter;
import org.jboss.pnc.bacon.common.TokenAuthenticator;
import org.jboss.pnc.bacon.config.Config;
import org.jboss.pnc.bacon.config.RexConfig;
import org.jboss.pnc.bacon.pnc.client.PncClientHelper;
import org.jboss.pnc.client.Configuration;
import org.jboss.pnc.rex.api.MaintenanceEndpoint;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.jboss.resteasy.plugins.providers.RegisterBuiltin;
import org.jboss.resteasy.spi.ResteasyProviderFactory;

import com.redhat.resilience.otel.OTelCLIHelper;

import io.opentelemetry.api.trace.Span;
import picocli.CommandLine;

@CommandLine.Command(
        name = "rex",
        description = "Rex related tasks",
        subcommands = {
                RexCli.ClearAll.class })
public class RexCli {

    private static ResteasyClientBuilder builder;
    private static String rexUrl;

    @CommandLine.Command(name = "clear-all", description = "[WARNING] This will clear all the tasks in Rex")
    public static class ClearAll implements Callable<Integer> {

        @Override
        public Integer call() throws Exception {
            getMaintenanceEndpointClient().clearAll();
            return 0;
        }
    }

    private static ResteasyWebTarget getClient() {

        if (builder == null) {
            builder = new ResteasyClientBuilder();
            ResteasyProviderFactory factory = ResteasyProviderFactory.getInstance();
            builder.providerFactory(factory);
            ResteasyProviderFactory.setRegisterBuiltinByDefault(true);
            RegisterBuiltin.register(factory);

            RexConfig rexConfig = Config.instance().getActiveProfile().getRex();
            rexUrl = rexConfig.getUrl();
        }
        ResteasyClient resteasyClient = builder.build();
        if (OTelCLIHelper.otelEnabled()) {
            resteasyClient.register(new CustomRestHeaderFilter(Span.current().getSpanContext()));
        }
        return resteasyClient.target(rexUrl);
    }

    private static ResteasyWebTarget getAuthenticatedClient() {

        ResteasyWebTarget target = getClient();
        Configuration pncConfiguration = PncClientHelper.getPncConfiguration();
        target.register(new TokenAuthenticator(pncConfiguration.getBearerTokenSupplier()));
        return target;
    }

    public static MaintenanceEndpoint getMaintenanceEndpointClient() {
        return getAuthenticatedClient().proxy(MaintenanceEndpoint.class);
    }
}
