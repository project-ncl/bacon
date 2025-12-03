package org.jboss.pnc.bacon.auth.spi;

import org.jboss.pnc.bacon.auth.OidcClientException;
import org.jboss.pnc.bacon.auth.model.OidcCredential;

/**
 * A generic oidc client interface where we can get a token from any Oidc server
 */
public interface OidcClient {
    /**
     * Authenticate based on username and password
     *
     * @param authServerUrl
     * @param client
     *
     * @return Credential object that contains all the information
     */
    OidcCredential getCredential(String authServerUrl, String client)
            throws OidcClientException;

    /**
     * Authenticate based on service account and service account secret
     *
     * @param authServerUrl
     * @param serviceAccountUsername
     * @param secret
     *
     * @return Credential object that contains all the information
     */
    OidcCredential getCredentialServiceAccount(
            String authServerUrl,
            String serviceAccountUsername,
            String secret) throws OidcClientException;
}
