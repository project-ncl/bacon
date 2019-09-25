/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.pnc.bacon.pnc.client;

import lombok.extern.slf4j.Slf4j;
import org.jboss.pnc.bacon.auth.DirectKeycloakClientImpl;
import org.jboss.pnc.bacon.auth.KeycloakClientException;
import org.jboss.pnc.bacon.auth.model.Credential;
import org.jboss.pnc.bacon.auth.spi.KeycloakClient;
import org.jboss.pnc.bacon.common.Fail;
import org.jboss.pnc.bacon.config.Config;
import org.jboss.pnc.bacon.config.KeycloakConfig;
import org.jboss.pnc.client.Configuration;

import java.net.URI;
import java.net.URISyntaxException;

@Slf4j
public class PncClientHelper {

    private static Configuration configuration;

    public static Configuration getPncConfiguration() {

        if (configuration == null) {
            setup();
        }
        return configuration;
    }

    public static void setup() {

        Config config = Config.instance();


        KeycloakConfig keycloakConfig = config.getKeycloak();
        String bearerToken = "";

        if (keycloakConfig != null) {
            keycloakConfig.validate();
            bearerToken = getBearerToken(keycloakConfig);

            if (bearerToken == null || bearerToken.isEmpty()) {
                Fail.fail("Credentials don't seem to be valid");
            }
        }

        config.getPnc().validate();
        String url = config.getPnc().getUrl();

        try {
            URI uri = new URI(url);

            configuration = Configuration.builder()
                    .protocol(uri.getScheme())
                    .host(uri.getHost())
                    .bearerToken(bearerToken)
                    .pageSize(50)
                    .build();

        } catch (URISyntaxException e) {
            Fail.fail(e.getMessage());
        }
    }

    /**
     * Return null if it couldn't get the authentication token. This generally means that
     * the credentials are not valid
     * @param keycloakConfig
     * @return
     */
    private static String getBearerToken(KeycloakConfig keycloakConfig) {

        log.debug("Authenticating to keycloak");

        try {

            KeycloakClient client = new DirectKeycloakClientImpl();

            Credential credential;

            if (keycloakConfig.isServiceAccount()) {
                credential = client.getCredential(
                        keycloakConfig.getUrl(),
                        keycloakConfig.getRealm(),
                        keycloakConfig.getUsername(),
                        keycloakConfig.getClientSecret());
            } else {
                credential = client.getCredential(
                        keycloakConfig.getUrl(),
                        keycloakConfig.getRealm(),
                        keycloakConfig.getClientId(),
                        keycloakConfig.getUsername(),
                        keycloakConfig.getPassword());
            }

            return credential.getAccessToken();

        } catch (KeycloakClientException e) {

            log.error("Keycloak authentication failed!");
            Fail.fail(e.getMessage());

            return null;
        }
    }

    /**
     * WARNING: Stops the application if the bearer token is not set
     */
    public static void authRequired() {

        String reason = "Aborted! Please specify credentials in the config file before retrying";

        Fail.failIfNull(getPncConfiguration().getBearerToken(), reason);

        if (getPncConfiguration().getBearerToken().isEmpty()) {
            Fail.fail(reason);
        }
    }
}
