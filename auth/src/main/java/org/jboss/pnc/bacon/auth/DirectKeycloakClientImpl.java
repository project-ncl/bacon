package org.jboss.pnc.bacon.auth;

import kong.unirest.HttpResponse;
import kong.unirest.MultipartBody;
import kong.unirest.Unirest;
import kong.unirest.UnirestException;
import kong.unirest.jackson.JacksonObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.jboss.pnc.bacon.auth.model.CacheFile;
import org.jboss.pnc.bacon.auth.model.Credential;
import org.jboss.pnc.bacon.auth.model.KeycloakResponse;
import org.jboss.pnc.bacon.auth.spi.KeycloakClient;
import org.jboss.pnc.bacon.common.exception.FatalException;

import javax.net.ssl.SSLHandshakeException;

import java.io.Console;
import java.time.Instant;
import java.util.Optional;

/**
 * Authenticates to Keycloak using direct flow!
 */
@Slf4j
public class DirectKeycloakClientImpl implements KeycloakClient {

    private static final int MAX_RETRIES = 10;

    static {
        // Configure Unirest ObjectMapper
        Unirest.config().setObjectMapper(new JacksonObjectMapper());
    }

    @Override
    public Credential getCredential(String keycloakBaseUrl, String realm, String client, String username)
            throws KeycloakClientException {

        Optional<Credential> cachedCredential = CacheFile.getCredentialFromCacheFile(keycloakBaseUrl, realm, username);

        if (cachedCredential.isPresent()) {

            Credential cred = cachedCredential.get();
            if (cred.isValid()) {
                log.debug("Using cached credential details");
                Credential refreshed;
                try {
                    refreshed = refreshCredentialIfNeededAndReturnNewCredential(keycloakBaseUrl, realm, username, cred);
                } catch (UnirestException e) {
                    throw new KeycloakClientException(e);
                }

                if (!refreshed.needsNewAccessToken()) {
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

        // if we're here, it means we couldn't use / get the credential in the cached file
        String keycloakEndpoint = keycloakEndpoint(keycloakBaseUrl, realm);

        try {
            log.debug("Getting token via username/password");

            MultipartBody body = Unirest.post(keycloakEndpoint)
                    .field("grant_type", "password")
                    .field("client_id", client)
                    .field("username", username)
                    .field("password", askForPassword());

            KeycloakResponse response = getKeycloakResponseWithRetries(body);
            Instant now = Instant.now();

            Credential credential = Credential.builder()
                    .keycloakBaseUrl(keycloakBaseUrl)
                    .realm(realm)
                    .client(client)
                    .username(username)
                    .accessToken(response.getAccessToken())
                    .accessTokenExpiresIn(now.plusSeconds(response.getExpiresIn()))
                    .refreshToken(response.getRefreshToken())
                    .refreshTokenExpiresIn(now.plusSeconds(response.getRefreshExpiresIn()))
                    .build();

            CacheFile.writeCredentialToCacheFile(keycloakBaseUrl, realm, username, credential);
            return credential;
        } catch (Exception e) {
            throw new KeycloakClientException(e);
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

    private Credential refreshToken(Credential credential) throws UnirestException {
        String keycloakEndpoint = keycloakEndpoint(credential.getKeycloakBaseUrl(), credential.getRealm());
        MultipartBody body = Unirest.post(keycloakEndpoint)
                .field("grant_type", "refresh_token")
                .field("client_id", credential.getClient())
                .field("refresh_token", credential.getRefreshToken());

        KeycloakResponse response = getKeycloakResponseWithRetries(body);
        Instant now = Instant.now();

        return credential.toBuilder()
                .accessToken(response.getAccessToken())
                .accessTokenExpiresIn(now.plusSeconds(response.getExpiresIn()))
                .refreshToken(response.getRefreshToken())
                .refreshTokenExpiresIn(now.plusSeconds(response.getRefreshExpiresIn()))
                .build();
    }

    private static String askForPassword() {

        Console console = System.console();

        if (console == null) {
            throw new FatalException("Couldn't get console instance");
        }
        char[] passwordArray = console.readPassword("Enter your password: ");
        return new String(passwordArray);
    }

    private Credential refreshCredentialIfNeededAndReturnNewCredential(
            String keycloakUrl,
            String realm,
            String username,
            Credential cred) throws UnirestException {

        if (cred.needsNewAccessToken()) {
            // if it needs a refresh
            log.info("Refreshing access token...");
            Credential refreshed = refreshToken(cred);
            CacheFile.writeCredentialToCacheFile(keycloakUrl, realm, username, refreshed);
            return refreshed;
        } else {
            return cred;
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
     *
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
}
