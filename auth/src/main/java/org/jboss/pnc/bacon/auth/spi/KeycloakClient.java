package org.jboss.pnc.bacon.auth.spi;

import org.jboss.pnc.bacon.auth.KeycloakClientException;
import org.jboss.pnc.bacon.auth.model.Credential;

public interface KeycloakClient {

    /**
     * Authenticate based on username and password
     *
     * @param keycloakBaseUrl
     * @param realm
     * @param client
     * @param username
     * @param password
     *
     * @return Credential object that contains all the information
     */
    Credential getCredential(String keycloakBaseUrl, String realm, String client, String username, String password) throws KeycloakClientException;

    /**
     * Authenticate based on service account and service account secret
     *
     * @param keycloakBaseUrl
     * @param realm
     * @param serviceAccountUsername
     * @param secret
     *
     * @return Credential object that contains all the information
     */
    Credential getCredential(String keycloakBaseUrl, String realm, String serviceAccountUsername, String secret) throws KeycloakClientException;


    default String keycloakEndpoint(String keycloakBaseUrl, String realm) {

        String keycloakUrl = keycloakBaseUrl;

        if (!keycloakBaseUrl.endsWith("/")) {
            keycloakUrl = keycloakBaseUrl + "/";
        }

        StringBuilder builder = new StringBuilder();

        builder.append(keycloakUrl)
                .append("auth/realms/")
                .append(realm)
                .append("/protocol/openid-connect/token");

        return builder.toString();
    }
}
