package org.jboss.pnc.bacon.pnc.admin;

import java.time.Instant;
import java.util.concurrent.Callable;

import org.jboss.pnc.bacon.common.Constant;
import org.jboss.pnc.bacon.common.ObjectHelper;
import org.jboss.pnc.bacon.common.cli.JSONCommandHandler;
import org.jboss.pnc.bacon.pnc.common.ClientCreator;
import org.jboss.pnc.client.PncStatusClient;
import org.jboss.pnc.dto.PncStatus;

import picocli.CommandLine;

@CommandLine.Command(
        name = "pnc-status",
        description = "PNC status related tasks",
        subcommands = {
                PncStatusCli.SetPncStatus.class,
                PncStatusCli.GetPncStatus.class })
public class PncStatusCli {

    private static final ClientCreator<PncStatusClient> CREATOR = new ClientCreator<>(PncStatusClient::new);

    @CommandLine.Command(
            name = "set",
            description = "This will set the PNC status",
            footer = Constant.EXAMPLE_TEXT + "$ bacon pnc admin pnc-status set \\%n"
                    + "\t--banner=\"Switching to maintenance mode for upcoming migration\" \\%n"
                    + "\t--eta=\"2025-07-30T15:00:00.000Z\" isMaintenanceMode")
    public static class SetPncStatus implements Callable<Integer> {

        @CommandLine.Parameters(description = "Is maintenance mode ON or OFF?", defaultValue = "false")
        private Boolean isMaintenanceMode;

        @CommandLine.Option(names = "--banner", description = "Banner")
        private String banner;

        @CommandLine.Option(names = "--eta", description = "ETA")
        private Instant eta;

        @Override
        public Integer call() throws Exception {
            try (PncStatusClient client = CREATOR.newClientAuthenticated()) {
                client.setPncStatus(
                        PncStatus.builder()
                                .banner(banner)
                                .eta(eta)
                                .isMaintenanceMode(isMaintenanceMode)
                                .build());
                return 0;
            }
        }
    }

    @CommandLine.Command(
            name = "get",
            description = "This will get the PNC status",
            footer = Constant.EXAMPLE_TEXT + "$ bacon pnc admin pnc-status get")
    public static class GetPncStatus extends JSONCommandHandler implements Callable<Integer> {

        @Override
        public Integer call() throws Exception {
            try (PncStatusClient client = CREATOR.newClientAuthenticated()) {
                PncStatus pncStatus = client.getPncStatus();
                ObjectHelper.print(getJsonOutput(), pncStatus);
                return 0;
            }
        }
    }
}
