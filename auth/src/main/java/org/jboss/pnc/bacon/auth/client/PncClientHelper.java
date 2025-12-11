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
package org.jboss.pnc.bacon.auth.client;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.function.Supplier;

import org.jboss.pnc.bacon.auth.KeycloakClientImpl;
import org.jboss.pnc.bacon.auth.model.KeycloakCredential;
import org.jboss.pnc.bacon.auth.spi.KeycloakClient;
import org.jboss.pnc.bacon.common.exception.FatalException;
import org.jboss.pnc.bacon.config.Config;
import org.jboss.pnc.bacon.config.KeycloakConfig;
import org.jboss.pnc.client.Configuration;
import org.jboss.pnc.client.GenericSettingClient;
import org.jboss.pnc.client.RemoteResourceException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PncClientHelper {

    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

    private static boolean bannerChecked = false;

    private static PncClientTokenHolder pncClientTokenHolder;

    private static boolean resteasyAvailable = true;

    static {
        // If this component is used externally in Quarkus based applications they will not
        // have access to resteasy clients (which cause a clash).
        try {
            Class.forName("org.jboss.resteasy.client.jaxrs.ClientHttpEngine");
        } catch (ClassNotFoundException e) {
            resteasyAvailable = false;
        }
    }

    public static Configuration getPncConfiguration(boolean authenticationNeeded) {
        return setup(authenticationNeeded);
    }

    public static Configuration getPncConfiguration() {
        return getPncConfiguration(true);
    }

    public static Configuration setup(boolean authenticationNeeded) {
        Config config = Config.instance();

        KeycloakConfig keycloakConfig = config.getActiveProfile().getKeycloak();

        if (authenticationNeeded) {
            if (keycloakConfig == null) {
                throw new FatalException("Keycloak section is needed in the configuration file!");
            }

            keycloakConfig.validate();
        }

        config.getActiveProfile().getPnc().validate();
        String url = config.getActiveProfile().getPnc().getUrl();

        try {
            URI uri = new URI(url);

            Integer port = null;
            if (uri.getPort() != -1) {
                port = uri.getPort();
            }

            Configuration.ConfigurationBuilder builder = Configuration.builder()
                    .protocol(uri.getScheme())
                    .port(port)
                    .host(uri.getHost())
                    .pageSize(50)
                    .addDefaultMdcToHeadersMappings();

            if (authenticationNeeded) {
                if (pncClientTokenHolder == null) {
                    pncClientTokenHolder = new PncClientTokenHolder(() -> getCredential(keycloakConfig));
                }
                builder = builder.bearerTokenSupplier(() -> pncClientTokenHolder.getAccessToken());
            }
            Configuration configuration = builder.build();

            printBannerIfNecessary(configuration);

            return configuration;

        } catch (URISyntaxException e) {
            throw new FatalException("URI syntax issue", e);
        }
    }

    /**
     * Return credential based on keycloak config, making the choice between client secret auth or user auth
     *
     * @param keycloakConfig the keycloak config
     * @return the token, or null if we couldn't get it
     */
    private static KeycloakCredential getCredential(KeycloakConfig keycloakConfig) {

        log.debug("Authenticating to keycloak");

        try {

            KeycloakClient client = new KeycloakClientImpl();

            KeycloakCredential keycloakCredential;

            if (keycloakConfig.isServiceAccount()) {
                keycloakCredential = client.getCredentialServiceAccount(
                        keycloakConfig.getUrl(),
                        keycloakConfig.getRealm(),
                        keycloakConfig.getUsername(),
                        keycloakConfig.getClientSecret());
            } else {
                keycloakCredential = client.getCredential(
                        keycloakConfig.getUrl(),
                        keycloakConfig.getRealm(),
                        keycloakConfig.getClientId(),
                        keycloakConfig.getUsername());

            }

            return keycloakCredential;

        } catch (Exception e) {
            throw new FatalException("Keycloak authentication failed!", e);
        }
    }

    private static void printBannerIfNecessary(Configuration configuration) {
        if (resteasyAvailable && !bannerChecked) {
            try (GenericSettingClient genericSettingClient = new GenericSettingClient(configuration)) {
                String banner = genericSettingClient.getAnnouncementBanner().getBanner();
                if (banner != null && !banner.isEmpty()) {
                    log.warn("***********************");
                    log.warn("Announcement: {}", banner);
                    log.warn("***********************");
                }
            } catch (RemoteResourceException e) {
                log.error("Could not get announcements: " + e);
            }

            bannerChecked = true;
        }
    }

    /**
     * Format must be: &lt;yyyy&gt;-&lt;MM&gt;-&lt;dd&gt;
     *
     * @param date the date format
     */
    public static Instant parseDateFormat(String date) {

        try {
            Date ret = sdf.parse(date);
            return ret.toInstant();
        } catch (ParseException e) {
            throw new FatalException("Date is not in valid format", e);
        }
    }

    public static String getTodayDayInYYYYMMDDFormat() {
        return sdf.format(new Date());
    }

    /**
     * Private static class to contain the logic to automatically get a new access token when necessary
     */
    private static class PncClientTokenHolder {
        KeycloakCredential cached;
        Supplier<KeycloakCredential> credentialSupplier;

        PncClientTokenHolder(Supplier<KeycloakCredential> credentialSupplier) {
            this.credentialSupplier = credentialSupplier;
        }

        public String getAccessToken() {
            if (cached == null || !cached.isAccessTokenValid()) {
                log.debug("Getting or Refreshing access token!");
                cached = credentialSupplier.get();
                if (cached.getAccessToken() == null || cached.getAccessToken().isEmpty()) {
                    throw new FatalException("Credentials don't seem to be valid");
                }
            }

            return cached.getAccessToken();
        }
    }
}
