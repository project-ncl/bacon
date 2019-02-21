package org.jboss.pnc.bacon.auth;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.ObjectMapper;
import com.mashape.unirest.http.Unirest;
import lombok.extern.slf4j.Slf4j;
import org.jboss.pnc.bacon.auth.model.Credential;
import org.jboss.pnc.bacon.auth.model.KeycloakResponse;
import org.jboss.pnc.bacon.auth.spi.KeycloakClient;

import java.io.File;
import java.io.IOException;
import java.util.Date;

/**
 * Authenticates to Keycloak using direct flow!
 */
@Slf4j
public class DirectKeycloakClientImpl implements KeycloakClient {

    private static void setupUnirest() {
        // Only one time
        Unirest.setObjectMapper(new ObjectMapper() {
            private com.fasterxml.jackson.databind.ObjectMapper jacksonObjectMapper
                    = new com.fasterxml.jackson.databind.ObjectMapper();

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

    public DirectKeycloakClientImpl() {
        setupUnirest();
    }

    /**
     * TODO: save current token in a file
     */
    @Override
    public Credential getCredential(String keycloakBaseUrl, String realm, String client,
                                    String username, String password) throws KeycloakClientException {

        String keycloakEndpoint = keycloakEndpoint(keycloakBaseUrl, realm);

        try {

            log.debug("Getting token via username/password");

            HttpResponse<KeycloakResponse> postResponse = Unirest.post(keycloakEndpoint)
                    .field("grant_type", "password")
                    .field("client_id", client)
                    .field("username", username)
                    .field("password", password)
                    .asObject(KeycloakResponse.class);

            KeycloakResponse response = postResponse.getBody();
            Date date = new Date();

            return Credential.builder()
                    .keycloakBaseUrl(keycloakBaseUrl)
                    .realm(realm)
                    .client(client)
                    .username(username)
                    .accessToken(response.getAccessToken())
                    .accessTokenTime(date)
                    .refreshToken(response.getRefreshToken())
                    .refreshTokenTime(date)
                    .build();

        } catch (Exception e) {
            throw new KeycloakClientException(e);
        }
    }

    /**
     * TODO: save current token in a file
     */
    @Override
    public Credential getCredential(String keycloakBaseUrl, String realm,
                                    String serviceAccountUsername, String secret) throws KeycloakClientException {

        String keycloakEndpoint = keycloakEndpoint(keycloakBaseUrl, realm);

        try {

            log.debug("Getting token via clientServiceAccountUsername / secret");

            HttpResponse<KeycloakResponse> postResponse = Unirest.post(keycloakEndpoint)
                    .field("grant_type", "client_credentials")
                    .field("client_id", serviceAccountUsername)
                    .field("client_secret", secret)
                    .asObject(KeycloakResponse.class);

            KeycloakResponse response = postResponse.getBody();
            Date date = new Date();

            return Credential.builder()
                    .keycloakBaseUrl(keycloakBaseUrl)
                    .realm(realm)
                    .client(serviceAccountUsername)
                    .accessToken(response.getAccessToken())
                    .accessTokenTime(date)
                    .refreshToken(response.getRefreshToken())
                    .refreshTokenTime(date)
                    .build();

        } catch (Exception e) {
            throw new KeycloakClientException(e);
        }
    }

    // TODO
    private void writeCredentialToFile(Credential credential, File file) {

    }

    // TODO
    private Credential readCredentialFromFile(File file) {
        return null;

    }

    // TODO
    private boolean needToRefreshAccessToken(Credential credential, long expiry) {
        return false;
    }
}
