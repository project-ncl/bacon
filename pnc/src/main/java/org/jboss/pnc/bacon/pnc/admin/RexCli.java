package org.jboss.pnc.bacon.pnc.admin;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

import org.jboss.pnc.bacon.auth.client.PncClientHelper;
import org.jboss.pnc.bacon.common.CustomRestHeaderFilter;
import org.jboss.pnc.bacon.common.TokenAuthenticator;
import org.jboss.pnc.bacon.config.Config;
import org.jboss.pnc.bacon.config.RexConfig;
import org.jboss.pnc.client.Configuration;
import org.jboss.pnc.rex.api.MaintenanceEndpoint;
import org.jboss.pnc.rex.api.TaskEndpoint;
import org.jboss.pnc.rex.dto.TaskDTO;
import org.jboss.pnc.rex.dto.TransitionTimeDTO;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.jboss.resteasy.plugins.providers.RegisterBuiltin;
import org.jboss.resteasy.spi.ResteasyProviderFactory;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.resilience.otel.OTelCLIHelper;

import io.opentelemetry.api.trace.Span;
import picocli.CommandLine;

@CommandLine.Command(
        name = "rex",
        description = "Rex related tasks",
        subcommands = {
                RexCli.ClearAll.class,
                RexCli.Get.class })
public class RexCli {

    private static ResteasyClientBuilder builder;
    private static String rexUrl;

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .findAndRegisterModules();

    @CommandLine.Command(name = "clear-all", description = "[WARNING] This will clear all the tasks in Rex")
    public static class ClearAll implements Callable<Integer> {

        @Override
        public Integer call() throws Exception {
            getMaintenanceEndpointClient().clearAll();
            return 0;
        }
    }

    @CommandLine.Command(name = "get")
    public static class Get implements Callable<Integer> {
        @Override
        public Integer call() throws Exception {

            TaskEndpoint taskEndpoint = getTaskEndpointClient();
            Set<TaskDTO> tasks = taskEndpoint.getAll(null, null);

            List<TaskDTO> builds = tasks.stream().filter(task -> task.getQueue().equals("builds")).toList();
            for (TaskDTO build : builds) {
                BuildAttachment buildAttachment = MAPPER
                        .convertValue(build.getRemoteStart().getAttachment(), BuildAttachment.class);

                System.out.println(build.getName() + ":: " + buildAttachment.buildConfigName());

                List<TaskDTO> dingroguTasks = new java.util.ArrayList<>(
                        tasks.stream()
                                .filter(
                                        task -> task.getQueue().equals("dingrogu-build")
                                                && task.getCorrelationID().equals(buildAttachment.correlationId()))
                                .toList());

                dingroguTasks.sort(new DingroguTaskComparator());
                int maxLengthOfStateName = maxLengthOfStateNames(dingroguTasks);
                for (TaskDTO dingroguTask : dingroguTasks) {
                    System.out.printf(
                            "    [%" + maxLengthOfStateName + "s] %s\n",
                            dingroguTask.getState(),
                            dingroguTask.getName());
                }
            }
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
        ResteasyClient resteasyClient = builder.register(new CustomJacksonConfigurator()).build();
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

    public static TaskEndpoint getTaskEndpointClient() {
        return getAuthenticatedClient().proxy(TaskEndpoint.class);
    }

    private static int maxLengthOfStateNames(List<TaskDTO> tasks) {
        int length = 0;
        for (TaskDTO task : tasks) {
            int stateLength = task.getState().name().length();
            if (stateLength > length) {
                length = stateLength;
            }
        }
        return length;
    }

    private static record BuildAttachment(String buildConfigName, String correlationId) {
    }

    @Provider
    private static class CustomJacksonConfigurator implements ContextResolver<ObjectMapper> {
        @Override
        public ObjectMapper getContext(Class<?> type) {
            return MAPPER;
        }
    }

    private static class DingroguTaskComparator implements Comparator<TaskDTO> {
        @Override
        public int compare(TaskDTO first, TaskDTO second) {

            // we need to show final state before ongoing state
            if (first.getState().isFinal() && !second.getState().isFinal()) {
                return -1;
            } else if (!first.getState().isFinal() && second.getState().isFinal()) {
                return 1;
            } else if (first.getState().isFinal() && second.getState().isFinal()) {
                // if both states are final, then sort based on timestamp, with the earliest timestamp first
                return whoHasEarliestTimestamp(first, second);
            } else {
                // if both states are ongoing, then the one who has the latest timestamp should be shown first in the list
                return whoHasEarliestTimestamp(first, second) * -1;
            }
        }

        /**
         * -1 if first,
         * 1 if second
         * 0 if both have the same
         *
         * @param first
         * @param second
         * @return
         */
        private int whoHasEarliestTimestamp(TaskDTO first, TaskDTO second) {
            List<TransitionTimeDTO> firstTransition = first.getTimestamps();
            List<TransitionTimeDTO> secondTransition = second.getTimestamps();

            if (firstTransition == null || firstTransition.isEmpty()) {
                return -1;
            }

            if (secondTransition == null || secondTransition.isEmpty()) {
                return 1;
            }

            Instant firstTransitionTime = firstTransition.get(firstTransition.size() - 1).time;
            Instant secondTransitionTime = secondTransition.get(secondTransition.size() - 1).time;
            if (firstTransitionTime.isBefore(secondTransitionTime)) {
                return -1;
            } else if (firstTransitionTime.isAfter(secondTransitionTime)) {
                return 1;
            } else {
                return 0;
            }
        }
    }
}
