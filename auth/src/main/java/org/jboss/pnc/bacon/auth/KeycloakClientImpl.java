package org.jboss.pnc.bacon.auth;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Instant;
import java.util.Optional;

import javax.net.ssl.SSLHandshakeException;

import org.jboss.pnc.bacon.auth.model.CacheFile;
import org.jboss.pnc.bacon.auth.model.Credential;
import org.jboss.pnc.bacon.auth.model.KeycloakResponse;
import org.jboss.pnc.bacon.auth.spi.KeycloakClient;
import org.jboss.pnc.bacon.common.exception.FatalException;
import org.keycloak.adapters.installed.KeycloakInstalled;
import org.keycloak.representations.AccessToken;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import kong.unirest.HttpResponse;
import kong.unirest.MultipartBody;
import kong.unirest.Unirest;
import kong.unirest.UnirestException;
import kong.unirest.jackson.JacksonObjectMapper;

/**
 * Authenticates to Keycloak using direct flow!
 */
public class KeycloakClientImpl implements KeycloakClient {

    @java.lang.SuppressWarnings("all")
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(KeycloakClientImpl.class);
    private static final int MAX_RETRIES = 10;

    static {
        // Configure Unirest ObjectMapper
        Unirest.config().setObjectMapper(new JacksonObjectMapper());
    }

    @Override
    public Credential getCredential(String keycloakBaseUrl, String realm, String client, String username)
            throws KeycloakClientException {

        Optional<Credential> cachedCredential = CacheFile.getCredentialFromCacheFile(keycloakBaseUrl, realm, username);

        KeycloakInstalled keycloak = null;

        if (cachedCredential.isPresent()) {

            Credential cred = cachedCredential.get();
            if (cred.isRefreshTokenValid()) {
                Credential refreshed = cred;
                try {
                    if (!cred.isAccessTokenValid()) {
                        log.debug("Refreshing access token");
                        keycloak = new KeycloakInstalled(
                                constructKeycloakSettings(
                                        realm,
                                        keycloakBaseUrl,
                                        client,
                                        false,

                                        cred.getRefreshToken(),
                                        true));
                        keycloak.refreshToken(cred.getRefreshToken());
                        refreshed = tokenToCredential(keycloak, keycloakBaseUrl, client, realm);

                        // write refreshed credentials to cache file
                        CacheFile.writeCredentialToCacheFile(
                                keycloakBaseUrl,
                                realm,
                                keycloak.getToken().getPreferredUsername(),
                                refreshed);
                    }
                } catch (Exception e) {
                    throw new KeycloakClientException(e);
                }

                // we check if the token here (whether from cached or cached+refreshed) is valid again
                if (refreshed.isAccessTokenValid()) {
                    return refreshed;
                } else {
                    /*
                     * This section handles the following case:
                     *
                     * When we refresh an access token, we usually also get a new refresh token. However, the lifetime
                     * of the new refresh token has the same expiry date as the original refresh token.
                     *
                     * Even when we get a new access token with a refresh token, if the particular refresh token
                     * lifetime is less than the normal lifetime of an access token, the new access token will get the
                     * lifetime of the refresh token.
                     *
                     * Those 2 combinations mean that the access token lifetime, even after refreshing it to get new
                     * ones, is the lifetime of a refresh token.
                     *
                     * If the refreshed access token expires in fewer hours than we'd like, we should instead get a new
                     * set of access / refresh token by asking her password again
                     */
                    log.info("Refresh token is close to expiry or has expired. Will request new access token");
                }
            }
        }

        // if we are here, there's either nothing in the cache file, or the refreshed token is not valid. let's get
        // a new one
        if (keycloak == null) {
            keycloak = new KeycloakInstalled(
                    constructKeycloakSettings(realm, keycloakBaseUrl, client, true, null, false));
        }

        try {
            keycloak.loginManual();
            keycloak.refreshToken();

            Credential credential = tokenToCredential(keycloak, keycloakBaseUrl, client, realm);

            CacheFile.writeCredentialToCacheFile(
                    keycloakBaseUrl,
                    realm,
                    keycloak.getToken().getPreferredUsername(),
                    credential);

            return credential;
        } catch (Exception e) {
            throw new FatalException("Failed to login:", e);
        }
    }

    @Override
    public Credential getCredentialServiceAccount(
            String keycloakBaseUrl,
            String realm,
            String serviceAccountUsername,
            String secret) throws KeycloakClientException {

        String keycloakEndpoint = keycloakEndpoint(keycloakBaseUrl, realm);

        try {

            log.debug("Getting token via clientServiceAccountUsername / secret");

            MultipartBody body = Unirest.post(keycloakEndpoint)
                    .field("grant_type", "client_credentials")
                    .field("client_id", serviceAccountUsername)
                    .field("client_secret", secret);

            KeycloakResponse response = getKeycloakResponseWithRetries(body);
            Instant now = Instant.now();

            return Credential.builder()
                    .keycloakBaseUrl(keycloakBaseUrl)
                    .realm(realm)
                    .client(serviceAccountUsername)
                    .accessToken(response.getAccessToken())
                    .accessTokenExpiresIn(now.plusSeconds(response.getExpiresIn()))
                    .refreshToken(response.getRefreshToken())
                    .refreshTokenExpiresIn(now.plusSeconds(response.getRefreshExpiresIn()))
                    .build();

        } catch (Exception e) {
            throw new KeycloakClientException(e);
        }
    }

    /**
     * Tiny helper method that takes in a Unirest body and submits the request, and parses the response as a
     * KeycloakResponse object. This helper method add retries (up to MAX_RETRIES) if the request fails for whatever
     * reason.
     *
     * This method sleeps exponentially between retries to simulate an exponential backoff, starting at 200ms up to 102
     * seconds
     *
     * @param body Unirest body to send request
     * @return The KeycloakResponse object
     * @throws UnirestException when all hope is lost to recover
     */
    private KeycloakResponse getKeycloakResponseWithRetries(MultipartBody body) throws UnirestException {

        int retries = 0;

        while (true) {
            try {
                HttpResponse<KeycloakResponse> postResponse = body.asObject(KeycloakResponse.class);
                return postResponse.getBody();
            } catch (UnirestException e) {
                if (e.getCause().getClass().equals(SSLHandshakeException.class)) {
                    throw new FatalException(
                            "Cannot reach the Keycloak server because of missing TLS certificates",
                            e.getCause());
                }
                retries++;

                if (retries > MAX_RETRIES) {
                    // all hope is lost. Stop retrying
                    throw e;
                } else if (retries == MAX_RETRIES / 2) {
                    // let the user know as she waits
                    log.info("Having difficulty reaching {}. Retrying again...", body.getUrl());
                }
                sleepExponentially(retries);
                log.debug("Retrying to reach: {}", body.getUrl());
            }
        }
    }

    /**
     * Sleep starting from 100 milliseconds when retries = 0, and sleeping exponentially as the retries increase
     *
     * The math is millisecondsSleep = 100 * 2^(retries)
     *
     * @param retries number of retries attempted
     */
    private void sleepExponentially(int retries) {

        long amountOfSleep = (long) (100 * Math.pow(2, retries));
        log.debug("Sleeping for {} seconds", String.format("%.1f", amountOfSleep / 1000.0));

        try {
            Thread.sleep(amountOfSleep);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private InputStream constructKeycloakSettings(
            String realm,
            String keycloakBaseUrl,
            String client,
            boolean user,
            String secret,
            boolean refresh) {

        ObjectMapper mapper = new ObjectMapper();

        KeycloakSettings kSettings = new KeycloakSettings();
        kSettings.setRealm(realm);
        kSettings.setAuthServerUrl(keycloakBaseUrl + "/auth");
        kSettings.setSslRequired("none");
        kSettings.setResource(client);
        kSettings.setConfidentialPort("0");

        if (user) {
            kSettings.setPublicClient(user);
        } else {
            Credentials cred = new Credentials(secret);
            kSettings.setCredentials(cred);
        }
        // Enable auth with refresh token
        if (refresh) {
            kSettings.setBasicAuth(true);
        }

        String settings = null;
        try {
            settings = mapper.writeValueAsString(kSettings);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        return new ByteArrayInputStream(settings.getBytes());
    }

    private Credential tokenToCredential(
            KeycloakInstalled keycloak,
            String keycloakBaseUrl,
            String client,
            String realm) {
        Instant now = Instant.now();
        AccessToken token = keycloak.getToken();

        Credential credential = Credential.builder()
                .keycloakBaseUrl(keycloakBaseUrl)
                .accessToken(keycloak.getTokenString())
                .refreshToken(keycloak.getRefreshToken())
                .client(client)
                .realm(realm)
                .username(token.getPreferredUsername())
                .accessTokenExpiresIn(now.plusSeconds(keycloak.getTokenResponse().getExpiresIn()))
                .refreshTokenExpiresIn(now.plusSeconds(keycloak.getTokenResponse().getRefreshExpiresIn()))
                .build();

        return credential;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private class KeycloakSettings {
        private String realm;

        @JsonProperty("auth-server-url")
        private String authServerUrl;
        @JsonProperty("ssl-required")
        private String sslRequired;
        private String resource;
        @JsonProperty("confidential-port")
        private String confidentialPort;
        @JsonProperty("public-client")
        private Boolean publicClient;
        private Credentials credentials;
        @JsonProperty("enable-basic-auth")
        private Boolean basicAuth;

        @java.lang.SuppressWarnings("all")
        public KeycloakSettings() {
        }

        @java.lang.SuppressWarnings("all")
        public String getRealm() {
            return this.realm;
        }

        @java.lang.SuppressWarnings("all")
        public String getAuthServerUrl() {
            return this.authServerUrl;
        }

        @java.lang.SuppressWarnings("all")
        public String getSslRequired() {
            return this.sslRequired;
        }

        @java.lang.SuppressWarnings("all")
        public String getResource() {
            return this.resource;
        }

        @java.lang.SuppressWarnings("all")
        public String getConfidentialPort() {
            return this.confidentialPort;
        }

        @java.lang.SuppressWarnings("all")
        public Boolean getPublicClient() {
            return this.publicClient;
        }

        @java.lang.SuppressWarnings("all")
        public Credentials getCredentials() {
            return this.credentials;
        }

        @java.lang.SuppressWarnings("all")
        public Boolean getBasicAuth() {
            return this.basicAuth;
        }

        @java.lang.SuppressWarnings("all")
        public void setRealm(final String realm) {
            this.realm = realm;
        }

        @JsonProperty("auth-server-url")
        @java.lang.SuppressWarnings("all")
        public void setAuthServerUrl(final String authServerUrl) {
            this.authServerUrl = authServerUrl;
        }

        @JsonProperty("ssl-required")
        @java.lang.SuppressWarnings("all")
        public void setSslRequired(final String sslRequired) {
            this.sslRequired = sslRequired;
        }

        @java.lang.SuppressWarnings("all")
        public void setResource(final String resource) {
            this.resource = resource;
        }

        @JsonProperty("confidential-port")
        @java.lang.SuppressWarnings("all")
        public void setConfidentialPort(final String confidentialPort) {
            this.confidentialPort = confidentialPort;
        }

        @JsonProperty("public-client")
        @java.lang.SuppressWarnings("all")
        public void setPublicClient(final Boolean publicClient) {
            this.publicClient = publicClient;
        }

        @java.lang.SuppressWarnings("all")
        public void setCredentials(final Credentials credentials) {
            this.credentials = credentials;
        }

        @JsonProperty("enable-basic-auth")
        @java.lang.SuppressWarnings("all")
        public void setBasicAuth(final Boolean basicAuth) {
            this.basicAuth = basicAuth;
        }

        @java.lang.Override
        @java.lang.SuppressWarnings("all")
        public boolean equals(final java.lang.Object o) {
            if (o == this)
                return true;
            if (!(o instanceof KeycloakClientImpl.KeycloakSettings))
                return false;
            final KeycloakClientImpl.KeycloakSettings other = (KeycloakClientImpl.KeycloakSettings) o;
            if (!other.canEqual((java.lang.Object) this))
                return false;
            final java.lang.Object this$publicClient = this.getPublicClient();
            final java.lang.Object other$publicClient = other.getPublicClient();
            if (this$publicClient == null ? other$publicClient != null : !this$publicClient.equals(other$publicClient))
                return false;
            final java.lang.Object this$basicAuth = this.getBasicAuth();
            final java.lang.Object other$basicAuth = other.getBasicAuth();
            if (this$basicAuth == null ? other$basicAuth != null : !this$basicAuth.equals(other$basicAuth))
                return false;
            final java.lang.Object this$realm = this.getRealm();
            final java.lang.Object other$realm = other.getRealm();
            if (this$realm == null ? other$realm != null : !this$realm.equals(other$realm))
                return false;
            final java.lang.Object this$authServerUrl = this.getAuthServerUrl();
            final java.lang.Object other$authServerUrl = other.getAuthServerUrl();
            if (this$authServerUrl == null ? other$authServerUrl != null
                    : !this$authServerUrl.equals(other$authServerUrl))
                return false;
            final java.lang.Object this$sslRequired = this.getSslRequired();
            final java.lang.Object other$sslRequired = other.getSslRequired();
            if (this$sslRequired == null ? other$sslRequired != null : !this$sslRequired.equals(other$sslRequired))
                return false;
            final java.lang.Object this$resource = this.getResource();
            final java.lang.Object other$resource = other.getResource();
            if (this$resource == null ? other$resource != null : !this$resource.equals(other$resource))
                return false;
            final java.lang.Object this$confidentialPort = this.getConfidentialPort();
            final java.lang.Object other$confidentialPort = other.getConfidentialPort();
            if (this$confidentialPort == null ? other$confidentialPort != null
                    : !this$confidentialPort.equals(other$confidentialPort))
                return false;
            final java.lang.Object this$credentials = this.getCredentials();
            final java.lang.Object other$credentials = other.getCredentials();
            if (this$credentials == null ? other$credentials != null : !this$credentials.equals(other$credentials))
                return false;
            return true;
        }

        @java.lang.SuppressWarnings("all")
        protected boolean canEqual(final java.lang.Object other) {
            return other instanceof KeycloakClientImpl.KeycloakSettings;
        }

        @java.lang.Override
        @java.lang.SuppressWarnings("all")
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final java.lang.Object $publicClient = this.getPublicClient();
            result = result * PRIME + ($publicClient == null ? 43 : $publicClient.hashCode());
            final java.lang.Object $basicAuth = this.getBasicAuth();
            result = result * PRIME + ($basicAuth == null ? 43 : $basicAuth.hashCode());
            final java.lang.Object $realm = this.getRealm();
            result = result * PRIME + ($realm == null ? 43 : $realm.hashCode());
            final java.lang.Object $authServerUrl = this.getAuthServerUrl();
            result = result * PRIME + ($authServerUrl == null ? 43 : $authServerUrl.hashCode());
            final java.lang.Object $sslRequired = this.getSslRequired();
            result = result * PRIME + ($sslRequired == null ? 43 : $sslRequired.hashCode());
            final java.lang.Object $resource = this.getResource();
            result = result * PRIME + ($resource == null ? 43 : $resource.hashCode());
            final java.lang.Object $confidentialPort = this.getConfidentialPort();
            result = result * PRIME + ($confidentialPort == null ? 43 : $confidentialPort.hashCode());
            final java.lang.Object $credentials = this.getCredentials();
            result = result * PRIME + ($credentials == null ? 43 : $credentials.hashCode());
            return result;
        }

        @java.lang.Override
        @java.lang.SuppressWarnings("all")
        public java.lang.String toString() {
            return "KeycloakClientImpl.KeycloakSettings(realm=" + this.getRealm() + ", authServerUrl="
                    + this.getAuthServerUrl() + ", sslRequired=" + this.getSslRequired() + ", resource="
                    + this.getResource() + ", confidentialPort=" + this.getConfidentialPort() + ", publicClient="
                    + this.getPublicClient() + ", credentials=" + this.getCredentials() + ", basicAuth="
                    + this.getBasicAuth() + ")";
        }
    }

    private class Credentials {
        private String secret;

        @java.lang.SuppressWarnings("all")
        public String getSecret() {
            return this.secret;
        }

        @java.lang.SuppressWarnings("all")
        public void setSecret(final String secret) {
            this.secret = secret;
        }

        @java.lang.Override
        @java.lang.SuppressWarnings("all")
        public boolean equals(final java.lang.Object o) {
            if (o == this)
                return true;
            if (!(o instanceof KeycloakClientImpl.Credentials))
                return false;
            final KeycloakClientImpl.Credentials other = (KeycloakClientImpl.Credentials) o;
            if (!other.canEqual((java.lang.Object) this))
                return false;
            final java.lang.Object this$secret = this.getSecret();
            final java.lang.Object other$secret = other.getSecret();
            if (this$secret == null ? other$secret != null : !this$secret.equals(other$secret))
                return false;
            return true;
        }

        @java.lang.SuppressWarnings("all")
        protected boolean canEqual(final java.lang.Object other) {
            return other instanceof KeycloakClientImpl.Credentials;
        }

        @java.lang.Override
        @java.lang.SuppressWarnings("all")
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final java.lang.Object $secret = this.getSecret();
            result = result * PRIME + ($secret == null ? 43 : $secret.hashCode());
            return result;
        }

        @java.lang.Override
        @java.lang.SuppressWarnings("all")
        public java.lang.String toString() {
            return "KeycloakClientImpl.Credentials(secret=" + this.getSecret() + ")";
        }

        @java.lang.SuppressWarnings("all")
        public Credentials(final String secret) {
            this.secret = secret;
        }
    }
}
