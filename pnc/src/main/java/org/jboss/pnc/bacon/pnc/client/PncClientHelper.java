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

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.jboss.pnc.bacon.auth.DirectKeycloakClientImpl;
import org.jboss.pnc.bacon.auth.KeycloakClientException;
import org.jboss.pnc.bacon.auth.model.Credential;
import org.jboss.pnc.bacon.auth.spi.KeycloakClient;
import org.jboss.pnc.bacon.common.exception.FatalException;
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
@UtilityClass
public class PncClientHelper {
    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

    private boolean bannerChecked = false;

    public Configuration getPncConfiguration(boolean authenticationNeeded) {
        return setup(authenticationNeeded);
    }

    public Configuration getPncConfiguration() {
        return getPncConfiguration(true);
    }

    public Configuration setup(boolean authenticationNeeded) {
        Config config = Config.instance();

        KeycloakConfig keycloakConfig = config.getActiveProfile().getKeycloak();
        String bearerToken = "";

        if (authenticationNeeded) {
            if (keycloakConfig == null) {
                throw new FatalException("Keycloak section is needed in the configuration file!");
            }

            keycloakConfig.validate();
            bearerToken = getBearerToken(keycloakConfig);

            if (bearerToken == null || bearerToken.isEmpty()) {
                throw new FatalException("Credentials don't seem to be valid");
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

            Configuration configuration = Configuration.builder()
                    .protocol(uri.getScheme())
                    .port(port)
                    .host(uri.getHost())
                    .bearerToken(bearerToken)
                    .pageSize(50)
                    .build();

            printBannerIfNecessary(configuration);
            return configuration;

        } catch (URISyntaxException e) {
            throw new FatalException("URI syntax issue", e);
        }
    }

    /**
     * Return null if it couldn't get the authentication token. This generally means that the credentials are not valid
     *
     * @param keycloakConfig the keycloak config
     * @return the token, or null if we couldn't get it
     */
    private String getBearerToken(KeycloakConfig keycloakConfig) {

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
            throw new FatalException("Keycloak authentication failed!", e);
        }
    }

    private void printBannerIfNecessary(Configuration configuration) {

        if (!bannerChecked) {
            try (GenericSettingClient genericSettingClient = new GenericSettingClient(configuration)) {
                String banner = genericSettingClient.getAnnouncementBanner().getBanner();
                if (banner != null && !banner.isEmpty()) {
                    log.warn("***********************");
                    log.warn("Announcement: {}", banner);
                    log.warn("***********************");
                }
            } catch (RemoteResourceException e) {
                log.error(e.getMessage());
            }

            bannerChecked = true;
        }
    }

    /**
     * Format must be: &lt;yyyy&gt;-&lt;MM&gt;-&lt;dd&gt;
     *
     * @param date the date format
     */
    public Instant parseDateFormat(String date) {
        try {
            Date ret = sdf.parse(date);
            return ret.toInstant();
        } catch (ParseException e) {
            throw new FatalException("Date is not in valid format", e);
        }
    }

    public String getTodayDayInYYYYMMDDFormat() {
        return sdf.format(new Date());
    }
}
