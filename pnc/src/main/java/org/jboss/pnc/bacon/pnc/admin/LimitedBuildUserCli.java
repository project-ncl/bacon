package org.jboss.pnc.bacon.pnc.admin;

import java.util.concurrent.Callable;

import org.jboss.pnc.bacon.common.Constant;
import org.jboss.pnc.bacon.common.ObjectHelper;
import org.jboss.pnc.bacon.common.cli.JSONCommandHandler;
import org.jboss.pnc.bacon.pnc.common.ClientCreator;
import org.jboss.pnc.client.GenericSettingClient;

import picocli.CommandLine;

@CommandLine.Command(
        name = "limited-build-user",
        description = "Limit users for builds",
        subcommands = {
                LimitedBuildUserCli.SetLimitedBuildUser.class,
                LimitedBuildUserCli.GetLimitedBuildUser.class,
                LimitedBuildUserCli.RemoveLimitedBuildUser.class
        })
public class LimitedBuildUserCli {

    private static final ClientCreator<GenericSettingClient> CREATOR = new ClientCreator<>(GenericSettingClient::new);

    @CommandLine.Command(
            name = "add",
            description = "Add user to limited-build-users list",
            footer = Constant.EXAMPLE_TEXT + "$ bacon pnc admin limited-build-user add username")
    public static class SetLimitedBuildUser implements Callable<Integer> {

        @CommandLine.Parameters(description = "username to add")
        private String username;

        @Override
        public Integer call() throws Exception {
            try (GenericSettingClient client = CREATOR.newClientAuthenticated()) {
                client.addLimitedBuildUser(username);
                return 0;
            }
        }
    }

    @CommandLine.Command(
            name = "remove",
            description = "Remove user from limited-build-users list",
            footer = Constant.EXAMPLE_TEXT + "$ bacon pnc admin limited-build-user remove username")
    public static class RemoveLimitedBuildUser implements Callable<Integer> {

        @CommandLine.Parameters(description = "username to remove")
        private String username;

        @Override
        public Integer call() throws Exception {
            try (GenericSettingClient client = CREATOR.newClientAuthenticated()) {
                client.removeLimitedBuildUser(username);
                return 0;
            }
        }
    }

    @CommandLine.Command(
            name = "get",
            description = "Get list of limited-build-user",
            footer = Constant.EXAMPLE_TEXT + "$ bacon pnc admin limited-build-user get")
    public static class GetLimitedBuildUser extends JSONCommandHandler implements Callable<Integer> {

        @Override
        public Integer call() throws Exception {
            try (GenericSettingClient client = CREATOR.newClient()) {
                ObjectHelper.print(getJsonOutput(), client.getLimitedBuildUsers());
                return 0;
            }
        }
    }
}
