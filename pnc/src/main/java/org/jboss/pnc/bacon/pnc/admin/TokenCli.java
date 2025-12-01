package org.jboss.pnc.bacon.pnc.admin;

import java.util.concurrent.Callable;

import org.jboss.pnc.bacon.auth.KeycloakClientImpl;
import org.jboss.pnc.bacon.auth.model.KeycloakCredential;
import org.jboss.pnc.bacon.common.cli.JSONCommandHandler;
import org.jboss.pnc.bacon.config.Config;
import org.jboss.pnc.bacon.config.KeycloakConfig;

import picocli.CommandLine;

@CommandLine.Command(
        name = "token",
        description = "Keycloak token commands",
        subcommands = {
                TokenCli.Get.class })
public class TokenCli {

    @CommandLine.Command(name = "get", description = "Returns your keycloak access token.")
    public static class Get extends JSONCommandHandler implements Callable<Integer> {

        @Override
        public Integer call() throws Exception {
            KeycloakClientImpl clientImpl = new KeycloakClientImpl();
            KeycloakConfig config = Config.instance().getActiveProfile().getKeycloak();
            KeycloakCredential keycloakCredential;
            if (config.isServiceAccount()) {
                keycloakCredential = clientImpl.getCredentialServiceAccount(
                        config.getUrl(),
                        config.getRealm(),
                        config.getUsername(),
                        config.getClientSecret());
            } else {
                keycloakCredential = clientImpl
                        .getCredential(config.getUrl(), config.getRealm(), config.getClientId(), config.getUsername());
            }
            System.out.println(keycloakCredential.getAccessToken());
            return 0;
        }
    }

}
