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
import org.jboss.pnc.client.GenericSettingClient;
import org.jboss.pnc.client.RemoteResourceException;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;

@Slf4j
public class PncClientHelper {

    private static Configuration configuration;
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

    public static Configuration getPncConfiguration(boolean authenticationNeeded) {
        if (configuration == null) {
            setup(authenticationNeeded);
        }
        return configuration;
    }

    public static Configuration getPncConfiguration() {
        return getPncConfiguration(true);
    }

    public static void setup(boolean authenticationNeeded) {
        Config config = null;
        try {
            config = Config.instance();
        } catch (Exception e) {
            Fail.fail(e.getMessage());
        }

        KeycloakConfig keycloakConfig = config.getActiveProfile().getKeycloak();
        String bearerToken = "";

        if (authenticationNeeded && keycloakConfig == null) {
            Fail.fail("Keycloak section is needed in the configuration file!");
        }

        if (authenticationNeeded && keycloakConfig != null) {
            keycloakConfig.validate();
            bearerToken = getBearerToken(keycloakConfig);

            if (bearerToken == null || bearerToken.isEmpty()) {
                Fail.fail("Credentials don't seem to be valid");
            }
        }

        config.getActiveProfile().getPnc().validate();
        String url = config.getActiveProfile().getPnc().getUrl();

        try {
            URI uri = new URI(url);

            Integer port = null;
            if (uri.getPort() != -1) {
                port = uri.getPort();
            }

            configuration = Configuration.builder()
                    .protocol(uri.getScheme())
                    .port(port)
                    .host(uri.getHost())
                    .bearerToken(bearerToken)
                    .pageSize(50)
                    .build();

            GenericSettingClient genericSettingClient = new GenericSettingClient(configuration);
            try {
                String banner = genericSettingClient.getAnnouncementBanner().getBanner();
                if (banner != null && !banner.isEmpty()) {
                    log.warn("***********************");
                    log.warn("Announcement: {}", banner);
                    log.warn("***********************");
                }
            } catch (RemoteResourceException e) {
                log.error(e.getMessage());
            }

        } catch (URISyntaxException e) {
            Fail.fail(e.getMessage());
        }
    }

    /**
     * Return null if it couldn't get the authentication token. This generally means that the credentials are not valid
     * 
     * @param keycloakConfig
     * @return
     */
    private static String getBearerToken(KeycloakConfig keycloakConfig) {

        log.debug("Authenticating to keycloak");

        try {

            KeycloakClient client = new DirectKeycloakClientImpl();

            Credential credential;

            if (keycloakConfig.isServiceAccount()) {
                credential = client.getCredentialServiceAccount(
                        keycloakConfig.getUrl(),
                        keycloakConfig.getRealm(),
                        keycloakConfig.getUsername(),
                        keycloakConfig.getClientSecret());
            } else {
                credential = client.getCredential(
                        keycloakConfig.getUrl(),
                        keycloakConfig.getRealm(),
                        keycloakConfig.getClientId(),
                        keycloakConfig.getUsername());
            }

            return credential.getAccessToken();

        } catch (KeycloakClientException e) {

            log.error("Keycloak authentication failed!");
            Fail.fail(e.getMessage());

            return null;
        }
    }

    /**
     * Format must be: <yyyy>-<mm>-
     * <dd>
     * 
     * @param date
     */
    public static Instant parseDateFormat(String date) {

        try {
            Date ret = sdf.parse(date);
            return ret.toInstant();
        } catch (ParseException e) {
            Fail.fail("Date is not in valid format");
            return null;
        }
    }

    public static String getTodayDayInYYYYMMDDFormat() {
        return sdf.format(new Date());
    }
}
