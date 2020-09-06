package org.jboss.pnc.bacon.auth;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.ObjectMapper;
import com.mashape.unirest.http.Unirest;
import lombok.extern.slf4j.Slf4j;
import org.jboss.pnc.bacon.auth.model.CacheFile;
import org.jboss.pnc.bacon.auth.model.Credential;
import org.jboss.pnc.bacon.auth.model.KeycloakResponse;
import org.jboss.pnc.bacon.auth.spi.KeycloakClient;
import org.jboss.pnc.bacon.common.exception.FatalException;

import java.io.Console;
import java.io.IOException;
import java.time.Instant;
import java.util.Optional;

/**
 * Authenticates to Keycloak using direct flow!
 */
@Slf4j
public class DirectKeycloakClientImpl implements KeycloakClient {

    static {
        setupUnirest();
    }

    private static void setupUnirest() {
        // Only one time
        Unirest.setObjectMapper(new ObjectMapper() {
            private com.fasterxml.jackson.databind.ObjectMapper jacksonObjectMapper = new com.fasterxml.jackson.databind.ObjectMapper();

            public <T> T readValue(String value, Class<T> valueType) {
                try {
                    return jacksonObjectMapper.readValue(value, valueType);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            public String writeValue(Object value) {
                try {
                    return jacksonObjectMapper.writeValueAsString(value);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    @Override
    public Credential getCredential(String keycloakBaseUrl, String realm, String client, String username)
            throws KeycloakClientException {

        Optional<Credential> cachedCredential = CacheFile.getCredentialFromCacheFile(keycloakBaseUrl, realm, username);

        if (cachedCredential.isPresent()) {

            Credential cred = cachedCredential.get();
            if (cred.isValid()) {
                log.debug("Using cached credential details");
                Credential refreshed = refreshCredentialIfNeededAndReturnNewCredential(
                        keycloakBaseUrl,
                        realm,
                        username,
                        cred);

                if (!refreshed.needsNewAccessToken()) {
                    return refreshed;
                } else {
                    /*
                     * This section handles the following case:
                     *
                     * When we refresh an access token, we usually also get a new refresh token. However the lifetime of
                     * the new refresh token has the same expiry date as the original refresh token.
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

            HttpResponse<KeycloakResponse> postResponse = Unirest.post(keycloakEndpoint)
                    .field("grant_type", "password")
                    .field("client_id", client)
                    .field("username", username)
                    .field("password", askForPassword())
                    .asObject(KeycloakResponse.class);

            KeycloakResponse response = postResponse.getBody();
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

            HttpResponse<KeycloakResponse> postResponse = Unirest.post(keycloakEndpoint)
                    .field("grant_type", "client_credentials")
                    .field("client_id", serviceAccountUsername)
                    .field("client_secret", secret)
                    .asObject(KeycloakResponse.class);

            KeycloakResponse response = postResponse.getBody();
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

    private Credential refreshToken(Credential credential) {

        try {
            String keycloakEndpoint = keycloakEndpoint(credential.getKeycloakBaseUrl(), credential.getRealm());
            HttpResponse<KeycloakResponse> postResponse = Unirest.post(keycloakEndpoint)
                    .field("grant_type", "refresh_token")
                    .field("client_id", credential.getClient())
                    .field("refresh_token", credential.getRefreshToken())
                    .asObject(KeycloakResponse.class);

            KeycloakResponse response = postResponse.getBody();
            Instant now = Instant.now();

            return credential.toBuilder()
                    .accessToken(response.getAccessToken())
                    .accessTokenExpiresIn(now.plusSeconds(response.getExpiresIn()))
                    .refreshToken(response.getRefreshToken())
                    .refreshTokenExpiresIn(now.plusSeconds(response.getRefreshExpiresIn()))
                    .build();

        } catch (Exception e) {
            log.error(e.getMessage());
            return null;
        }
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
            Credential cred) {

        if (cred.needsNewAccessToken()) {
            // if needs a refresh
            log.info("Refreshing access token...");
            Credential refreshed = refreshToken(cred);
            CacheFile.writeCredentialToCacheFile(keycloakUrl, realm, username, refreshed);
            return refreshed;
        } else {
            return cred;
        }
    }
}
